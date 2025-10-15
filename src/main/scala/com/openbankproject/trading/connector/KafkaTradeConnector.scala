package com.openbankproject.trading.connector

import cats.effect.kernel.Async
import cats.effect.kernel.Ref
import cats.syntax.all._
import com.openbankproject.trading.model._
import java.time.Instant

/**
 * Minimal Kafka-based TradeConnector placeholder.
 */
class KafkaTradeConnector[F[_]: Async](bootstrapServers: String, topic: String, state: Ref[F, Map[TradeId, Trade]]) extends TradeConnector[F] {
  override def getConnectorInfo: ConnectorInfo = ConnectorInfo(
    name = "Kafka Trade Connector",
    version = "0.1",
    description = s"Kafka topic=$topic (in-memory storage)",
    supportedOperations = List(Connector.RECORD_TRADE, Connector.GET_TRADE, "getUserTrades", "getTradesBySymbol", "getRecentTrades", "getTradingVolume", "getTradingStats"),
    configuration = Map("bootstrapServers" -> bootstrapServers, "topic" -> topic)
  )

  override def healthCheck = Async[F].pure(ConnectorStatus(true, 0L, Instant.now()))

  override def recordTrade(trade: Trade) = state.update(_ + (trade.tradeId -> trade)).as(Right(trade): Either[ConnectorError, Trade])
  override def getTrade(tradeId: TradeId) = state.get.map(m => Right(m.get(tradeId)): Either[ConnectorError, Option[Trade]])
  override def getUserTrades(userId: UserId, limit: Option[Int]) = state.get.map { m =>
    val trades = m.values.filter(t => t.buyerId == userId || t.sellerId == userId).toList.sortBy(_.executedAt)(Ordering[Instant].reverse)
    Right(limit.fold(trades)(n => trades.take(n))): Either[ConnectorError, List[Trade]]
  }
  override def getTradesBySymbol(symbol: TradingSymbol, limit: Option[Int]) = state.get.map { m =>
    val trades = m.values.filter(_.symbol == symbol).toList.sortBy(_.executedAt)(Ordering[Instant].reverse)
    Right(limit.fold(trades)(n => trades.take(n))): Either[ConnectorError, List[Trade]]
  }
  override def getRecentTrades(symbol: TradingSymbol, since: Option[Instant], limit: Option[Int]) = state.get.map { m =>
    val filtered = m.values.filter(t => t.symbol == symbol && since.forall(ts => !t.executedAt.isBefore(ts))).toList.sortBy(_.executedAt)(Ordering[Instant].reverse)
    Right(limit.fold(filtered)(n => filtered.take(n))): Either[ConnectorError, List[Trade]]
  }
  override def getTradingVolume(symbol: TradingSymbol, since: Instant) = state.get.map { m =>
    val trades = m.values.filter(t => t.symbol == symbol && !t.executedAt.isBefore(since)).toList
    val volume = Quantity(trades.map(_.quantity.value).sum)
    val amount = Amount(trades.map(_.amount.value).sum)
    Right(TradingVolume(symbol, volume, amount, trades.length, s"since-${since.toEpochMilli}", Instant.now())): Either[ConnectorError, TradingVolume]
  }
  override def getTradingStats(userId: UserId) = state.get.map { m =>
    val trades = m.values.filter(t => t.buyerId == userId || t.sellerId == userId).toList
    val totalVolume = Amount(trades.map(_.amount.value).sum)
    val stats = UserTradingStats(userId, trades.length, totalVolume, Amount(0), Amount(0), None, None, Amount(0), None, BigDecimal(0), Instant.now().minusSeconds(86400), Instant.now())
    Right(stats): Either[ConnectorError, UserTradingStats]
  }
}

object KafkaTradeConnector {
  def inMemory[F[_]: Async](bootstrapServers: String, topic: String): F[KafkaTradeConnector[F]] =
    Ref.of[F, Map[TradeId, Trade]](Map.empty).map(ref => new KafkaTradeConnector[F](bootstrapServers, topic, ref))
}



