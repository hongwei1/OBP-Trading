package com.openbankproject.trading.matching

import com.openbankproject.trading.model._
import java.time.Instant

/** Lightweight order representation for matching layer (decoupled from transport). */
final case class LimitOrder(
  orderId: String,
  side: Side,                 // Buy | Sell
  price: BigDecimal,
  quantity: BigDecimal,
  remaining: BigDecimal,
  ownerAccountId: String,
  createdAt: Instant
)

sealed trait Side
object Side {
  case object Buy extends Side
  case object Sell extends Side
}

/** A single fill result between a resting and an incoming order. */
final case class Fill(
  tradeId: String,
  buyOrderId: String,
  sellOrderId: String,
  price: BigDecimal,
  quantity: BigDecimal,
  executedAt: Instant
)

/** Outcome of matching an incoming order against the book. */
final case class MatchOutcome(
  fills: List[Fill],
  remainingQuantity: BigDecimal
)

/** Read/write access to the order book storage (price levels FIFO). */
trait OrderBookRepository[F[_]] {
  def add(order: LimitOrder): F[Unit]
  def cancel(orderId: String): F[Boolean]
  def get(orderId: String): F[Option[LimitOrder]]
  def bestBid: F[Option[BigDecimal]]
  def bestAsk: F[Option[BigDecimal]]
  def snapshot(symbol: TradingSymbol): F[OrderBook]   // reuse domain view
}

/** Limit-order matching engine (price-time priority). */
trait MatchingEngine[F[_]] {
  /** Place a new limit order onto the book and (optionally) execute immediate matches. */
  def place(order: LimitOrder): F[MatchOutcome]

  /** Cancel an existing order. Returns true if canceled. */
  def cancel(orderId: String): F[Boolean]
}


