package com.openbankproject.trading.service

import cats.effect.kernel.Async
import cats.effect.Ref
import cats.syntax.all._
import com.openbankproject.trading.http._

import java.time.Instant
import java.util.UUID

final class InMemoryOrderService[F[_]: Async] private (
  state: Ref[F, Map[String, InMemoryOrderService.StoredOrder]]
) extends OrderService[F] {

  import InMemoryOrderService.StoredOrder

  def createOrder(req: CreateOrderRequest): F[Either[ErrorResponse, CreateOrderResponse]] = {
    val id = UUID.randomUUID().toString
    val now = Instant.now()
    val stored = StoredOrder(
      orderId = id,
      side = req.side,
      price = req.price,
      quantity = req.quantity,
      remaining = req.quantity,
      status = "active",
      ownerAccountId = req.accountId,
      createdAt = now,
      expiresAt = None
    )
    state.update(_ + (id -> stored)) *> Async[F].pure(Right(CreateOrderResponse(id, stored.status, stored.remaining)))
  }

  def cancelOrder(orderId: String): F[Either[ErrorResponse, CancelOrderResponse]] = {
    state.modify { m =>
      m.get(orderId) match {
        case Some(o) =>
          val updated = o.copy(status = "cancelled")
          (m.updated(orderId, updated), Right(CancelOrderResponse(orderId, updated.status)))
        case None =>
          (m, Left(ErrorResponse("not_found", s"Order $orderId not found")))
      }
    }
  }

  def getOrder(orderId: String): F[Either[ErrorResponse, OrderView]] = {
    state.get.map { m =>
      m.get(orderId) match {
        case Some(o) =>
          Right(OrderView(
            orderId = o.orderId,
            side = o.side,
            price = o.price,
            quantity = o.quantity,
            remaining = o.remaining,
            status = o.status,
            ownerAccountId = o.ownerAccountId,
            createdAt = o.createdAt,
            expiresAt = o.expiresAt
          ))
        case None => Left(ErrorResponse("not_found", s"Order $orderId not found"))
      }
    }
  }
}

object InMemoryOrderService {
  private final case class StoredOrder(
    orderId: String,
    side: String,
    price: BigDecimal,
    quantity: BigDecimal,
    remaining: BigDecimal,
    status: String,
    ownerAccountId: String,
    createdAt: Instant,
    expiresAt: Option[Instant]
  )

  def create[F[_]: Async](): F[InMemoryOrderService[F]] =
    Ref.of[F, Map[String, StoredOrder]](Map.empty).map(ref => new InMemoryOrderService[F](ref))
}


