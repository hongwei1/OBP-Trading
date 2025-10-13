package com.openbankproject.trading.settlement

import java.time.Instant

/** Lightweight types used by the orchestrator and connectors. */
final case class PaymentAuth(
  authId: String,
  buyerFiatAccountId: String,
  amountFiat: BigDecimal,
  state: AuthState,
  idempotencyKey: String,
  createdAt: Instant,
  updatedAt: Instant
)

sealed trait AuthState
object AuthState {
  case object Preauthorized extends AuthState
  case object Captured extends AuthState
  case object Released extends AuthState
}

final case class OnChainTx(
  txId: String,
  network: String,
  function: String, // RELEASE | WITHDRAW
  from: String,
  to: String,
  amount: BigDecimal,
  confirmations: Int,
  requiredConfirmations: Int,
  state: OnChainTxState,
  hash: String,
  nonce: Long,
  error: Option[String]
)

sealed trait OnChainTxState
object OnChainTxState {
  case object Pending extends OnChainTxState
  case object Confirmed extends OnChainTxState
  case object Failed extends OnChainTxState
}

/** Simple idempotency store abstraction. */
trait IdempotencyStore[F[_]] {
  /** Returns true if the key already exists (duplicate), or stores it and returns false otherwise. */
  def checkAndPut(key: String): F[Boolean]
}


