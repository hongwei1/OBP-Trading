package com.openbankproject.trading.connector

import com.openbankproject.trading.settlement.{PaymentAuth, SettlementError}

/** OBP-API based payments connector abstraction. */
trait ObpPaymentsConnector[F[_]] {
  /** Pre-authorize buyer fiat funds. */
  def preAuthorize(buyerFiatAccountId: String, amountFiat: BigDecimal, idempotencyKey: String): F[Either[SettlementError, PaymentAuth]]

  /** Capture previously created authorization. */
  def capture(authorizationId: String, idempotencyKey: String): F[Either[SettlementError, Unit]]

  /** Release a pre-authorization (used for compensation). */
  def release(authorizationId: String, idempotencyKey: String): F[Either[SettlementError, Unit]]
}

