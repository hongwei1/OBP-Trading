package com.openbankproject.trading.service

import cats.effect.IO
import cats.effect.Ref
import cats.syntax.all._
import com.openbankproject.trading.http._
import com.openbankproject.trading.http.ErrorCodes

import java.time.Instant
import java.util.UUID

final class InMemoryOrderService private (
  state: Ref[IO, Map[String, InMemoryOrderService.StoredOrder]]
) extends OrderService {

  import InMemoryOrderService.StoredOrder

  def createOrder(req: CreateOrderRequest): IO[Either[ErrorResponse, CreateOrderResponse]] = {
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
    state.update(_ + (id -> stored)) *> IO.pure(Right(CreateOrderResponse(id, stored.status, stored.remaining)))
  }

  def cancelOrder(orderId: String): IO[Either[ErrorResponse, CancelOrderResponse]] = {
    state.modify { m =>
      m.get(orderId) match {
        case Some(o) =>
          val updated = o.copy(status = "cancelled")
          (m.updated(orderId, updated), Right(CancelOrderResponse(orderId, updated.status)))
        case None =>
          (m, Left(ErrorResponse(ErrorCodes.NOT_FOUND, s"Order $orderId not found")))
      }
    }
  }

  def getOrder(orderId: String): IO[Either[ErrorResponse, OrderView]] = {
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
        case None => Left(ErrorResponse(ErrorCodes.NOT_FOUND, s"Order $orderId not found"))
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

  def create(): IO[InMemoryOrderService] =
    Ref.of[IO, Map[String, StoredOrder]](Map.empty).map(ref => new InMemoryOrderService(ref))
}


