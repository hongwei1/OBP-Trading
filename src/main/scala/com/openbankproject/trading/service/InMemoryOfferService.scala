package com.openbankproject.trading.service

import cats.effect.kernel.Async
import cats.effect.Ref
import cats.syntax.all._
import com.openbankproject.trading.http._
import com.openbankproject.trading.http.ErrorCodes

import java.time.Instant
import java.util.UUID

final class InMemoryOfferService[F[_]: Async] private (
  state: Ref[F, Map[String, InMemoryOfferService.StoredOffer]]
) extends OfferService[F] {

  import InMemoryOfferService.StoredOffer

  def createOffer(req: CreateOfferRequest): F[Either[ErrorResponse, CreateOfferResponse]] = {
    val id = UUID.randomUUID().toString
    val now = Instant.now()
    val stored = StoredOffer(
      offerId = id,
      offerType = req.offerType,
      price = req.price,
      quantity = req.quantity,
      remaining = req.quantity,
      status = "active",
      ownerAccountId = req.accountId,
      createdAt = now,
      expiresAt = None
    )
    state.update(_ + (id -> stored)) *> Async[F].pure(Right(CreateOfferResponse(id, stored.status, stored.remaining)))
  }

  def cancelOffer(offerId: String): F[Either[ErrorResponse, CancelOfferResponse]] = {
    state.modify { m =>
      m.get(offerId) match {
        case Some(o) =>
          val updated = o.copy(status = "cancelled")
          (m.updated(offerId, updated), Right(CancelOfferResponse(offerId, updated.status)))
        case None =>
          (m, Left(ErrorResponse(ErrorCodes.NOT_FOUND, s"Offer $offerId not found")))
      }
    }
  }

  def getOffer(offerId: String): F[Either[ErrorResponse, OfferView]] = {
    state.get.map { m =>
      m.get(offerId) match {
        case Some(o) =>
          Right(OfferView(
            offerId = o.offerId,
            offerType = o.offerType,
            price = o.price,
            quantity = o.quantity,
            remaining = o.remaining,
            status = o.status,
            ownerAccountId = o.ownerAccountId,
            createdAt = o.createdAt,
            expiresAt = o.expiresAt
          ))
        case None => Left(ErrorResponse(ErrorCodes.NOT_FOUND, s"Offer $offerId not found"))
      }
    }
  }
}

object InMemoryOfferService {
  private final case class StoredOffer(
    offerId: String,
    offerType: String,
    price: BigDecimal,
    quantity: BigDecimal,
    remaining: BigDecimal,
    status: String,
    ownerAccountId: String,
    createdAt: Instant,
    expiresAt: Option[Instant]
  )

  def create[F[_]: Async](): F[InMemoryOfferService[F]] =
    Ref.of[F, Map[String, StoredOffer]](Map.empty).map(ref => new InMemoryOfferService[F](ref))
}


