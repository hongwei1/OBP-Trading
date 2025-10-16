package com.openbankproject.trading.http

import com.openbankproject.trading.model._
import java.time.Instant

// Request models
final case class CreateOrderRequest(
  side: String,                 // "BUY" | "SELL"
  price: BigDecimal,
  quantity: BigDecimal,
  accountId: String,
  idempotencyKey: String
)

// Offer-specific request/response models
final case class CreateOfferRequest(
  offerType: String,            // "BUY" | "SELL"
  price: BigDecimal,
  quantity: BigDecimal,
  accountId: String,
  idempotencyKey: String
)

final case class MatchRequest(
  orderId: String,
  counterOrderId: String,
  amount: BigDecimal,
  price: BigDecimal
)

final case class SettlementRequest(
  tradeId: String,
  step: Option[String] = None   // optional step hint
)

final case class DepositNotification(
  txHash: String,
  from: String,
  to: String,
  amount: BigDecimal,
  confirmations: Int
)

final case class WithdrawalRequest(
  accountId: String,
  amount: BigDecimal,
  address: String,
  idempotencyKey: String
)

// Response models
final case class CreateOrderResponse(orderId: String, status: String, remaining: BigDecimal)
final case class CancelOrderResponse(orderId: String, status: String)
final case class CreateMatchResponse(tradeId: String, status: String)
final case class SettlementResponse(tradeId: String, status: String)
final case class DepositResponse(credited: Boolean, externalId: String)
final case class WithdrawalResponse(onChainTxId: String, state: String)

final case class CreateOfferResponse(offerId: String, status: String, remaining: BigDecimal)
final case class CancelOfferResponse(offerId: String, status: String)

// Views
final case class OrderView(
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

final case class OfferView(
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

final case class TradeView(
  tradeId: String,
  buyOrderId: String,
  sellOrderId: String,
  price: BigDecimal,
  quantity: BigDecimal,
  status: String,
  executedAt: Instant,
  settledAt: Option[Instant]
)

// Errors
final case class ErrorResponse(code: String, message: String, traceId: Option[String] = None)


