package com.openbankproject.trading.connector

import cats.effect.Sync
import cats.syntax.all._
import com.openbankproject.trading.settlement.{AuthState, PaymentAuth, SettlementError}

import java.time.Instant
import java.util.UUID

/**
  * Minimal fake implementation for local development and wiring tests.
  * Always succeeds and returns deterministic objects.
  */
class FakeObpPaymentsConnector[F[_]: Sync] extends ObpPaymentsConnector[F] {
  override def preAuthorize(buyerFiatAccountId: String, amountFiat: BigDecimal, idempotencyKey: String): F[Either[SettlementError, PaymentAuth]] =
    Sync[F].pure {
      Right(
        PaymentAuth(
          authId = s"auth-${UUID.nameUUIDFromBytes(idempotencyKey.getBytes).toString}",
          buyerFiatAccountId = buyerFiatAccountId,
          amountFiat = amountFiat,
          state = AuthState.Preauthorized,
          idempotencyKey = idempotencyKey,
          createdAt = Instant.now(),
          updatedAt = Instant.now()
        )
      )
    }

  override def capture(authorizationId: String, idempotencyKey: String): F[Either[SettlementError, Unit]] =
    Sync[F].pure(Right(()))

  override def release(authorizationId: String, idempotencyKey: String): F[Either[SettlementError, Unit]] =
    Sync[F].pure(Right(()))
}


