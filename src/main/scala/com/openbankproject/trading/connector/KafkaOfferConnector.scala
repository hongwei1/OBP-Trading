package com.openbankproject.trading.connector

import cats.effect.kernel.Async
import cats.effect.kernel.Ref
import cats.syntax.all._
import com.openbankproject.trading.model._
import java.time.Instant

/**
 * Minimal Kafka-based OfferConnector placeholder.
 * Uses in-memory Ref to keep build green; produce/consume can be added later.
 */
class KafkaOfferConnector[F[_]: Async](bootstrapServers: String, topic: String, state: Ref[F, Map[OfferId, Offer]]) extends OfferConnector[F] {
  override def getConnectorInfo: ConnectorInfo = ConnectorInfo(
    name = "Kafka Offer Connector",
    version = "0.1",
    description = s"Kafka topic=$topic (in-memory storage)",
    supportedOperations = List(
      Connector.CREATE_OFFER, Connector.UPDATE_OFFER, Connector.CANCEL_OFFER, Connector.GET_OFFER,
      "getUserOffers", "getActiveOffers", "getOffersBySymbol", "buildOrderBook", "getMarketDepth"
    ),
    configuration = Map("bootstrapServers" -> bootstrapServers, "topic" -> topic)
  )

  override def healthCheck = Async[F].pure(ConnectorStatus(true, 0L, Instant.now()))

  override def createOffer(offer: Offer) = state.update(_ + (offer.offerId -> offer)).as(Right(offer): Either[ConnectorError, Offer])
  override def updateOffer(offer: Offer) = state.modify { m => if (m.contains(offer.offerId)) (m.updated(offer.offerId, offer), Right(offer)) else (m, Left(NotFoundError(s"Offer ${offer.offerId.value} not found"))) }
  override def getOffer(offerId: OfferId) = state.get.map(m => Right(m.get(offerId)): Either[ConnectorError, Option[Offer]])
  override def cancelOffer(offerId: OfferId, userId: UserId) = state.modify { m =>
    m.get(offerId) match {
      case Some(o) if o.userId == userId => (m.updated(offerId, o.copy(status = OfferStatus.Cancelled, updatedAt = Instant.now())), Right(()))
      case Some(_) => (m, Left(PermissionError(s"User $userId cannot cancel offer $offerId")))
      case None => (m, Left(NotFoundError(s"Offer $offerId not found")))
    }
  }
  override def getUserOffers(userId: UserId, limit: Option[Int]) = state.get.map { m =>
    val offers = m.values.filter(_.userId == userId).toList.sortBy(_.createdAt)(Ordering[Instant].reverse)
    Right(limit.fold(offers)(n => offers.take(n))): Either[ConnectorError, List[Offer]]
  }
  override def getActiveOffers(symbol: TradingSymbol, limit: Option[Int]) = state.get.map { m =>
    val offers = m.values.filter(o => o.symbol == symbol && o.status == OfferStatus.Active).toList
    Right(limit.fold(offers)(n => offers.take(n))): Either[ConnectorError, List[Offer]]
  }
  override def getOffersBySymbol(symbol: TradingSymbol, limit: Option[Int]) = getActiveOffers(symbol, limit)
  override def buildOrderBook(symbol: TradingSymbol, depth: Int) = state.get.map { m =>
    val active = m.values.filter(o => o.symbol == symbol && o.status == OfferStatus.Active).toList
    val bids = active.filter(_.offerType == OfferType.Buy).groupBy(_.price).map { case (p, xs) =>
      OrderBookLevel(p, Quantity(xs.map(_.remainingQuantity.value).sum), xs.length)
    }.toList.sortBy(-_.price.value).take(depth)
    val asks = active.filter(_.offerType == OfferType.Sell).groupBy(_.price).map { case (p, xs) =>
      OrderBookLevel(p, Quantity(xs.map(_.remainingQuantity.value).sum), xs.length)
    }.toList.sortBy(_.price.value).take(depth)
    Right(OrderBook(symbol, bids, asks, Instant.now(), System.currentTimeMillis())): Either[ConnectorError, OrderBook]
  }
  override def getMarketDepth(symbol: TradingSymbol) = state.get.map { m =>
    val active = m.values.filter(o => o.symbol == symbol && o.status == OfferStatus.Active).toList
    val bidCount = active.count(_.offerType == OfferType.Buy)
    val askCount = active.count(_.offerType == OfferType.Sell)
    val bestBid = active.filter(_.offerType == OfferType.Buy).sortBy(_.price.value).reverse.headOption.map(_.price)
    val bestAsk = active.filter(_.offerType == OfferType.Sell).sortBy(_.price.value).headOption.map(_.price)
    val spread = for { b <- bestBid; a <- bestAsk } yield a - b
    Right(MarketDepth(symbol, bidCount, askCount, bestBid, bestAsk, spread, Instant.now())): Either[ConnectorError, MarketDepth]
  }
  override def expireOffers(before: Instant) = state.modify { m =>
    val (expired, kept) = m.partition { case (_, o) => o.expiresAt.isBefore(before) }
    (kept, Right(expired.size): Either[ConnectorError, Int])
  }
  override def cleanupExpiredOffers() = expireOffers(Instant.now()).map(_.map(_ => ()))
}

object KafkaOfferConnector {
  def inMemory[F[_]: Async](bootstrapServers: String, topic: String): F[KafkaOfferConnector[F]] =
    Ref.of[F, Map[OfferId, Offer]](Map.empty).map(ref => new KafkaOfferConnector[F](bootstrapServers, topic, ref))
}


