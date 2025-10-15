/**
 * Copyright (c) TESOBE 2025. All rights reserved.
 * Licensed under AGPL-3.0. See the project root for license information.
 */

package com.openbankproject.trading.settlement

import cats.effect.kernel.Async
import cats.syntax.all._
import com.openbankproject.trading.model._
import com.openbankproject.trading.settlement.SettlementError
import com.openbankproject.trading.settlement.PaymentAuth
import com.openbankproject.trading.settlement.OnChainTx
import com.openbankproject.trading.settlement.OnChainTxState
import com.openbankproject.trading.connector.{EthereumEscrowConnector, ObpPaymentsConnector}

/**
 * Orchestrates business-level TCC settlement for a Trade with per-trade on-chain release.
 * Sequence: preAuth (buyer fiat) -> captureFiat -> on-chain release (seller -> buyer) -> finalize.
 * All operations MUST be idempotent. Use IdempotencyStore with keys: tradeId, paymentAuthId, txHash.
 */
trait SettlementOrchestrator[F[_]] {

  /** Idempotently advances settlement for a trade. */
  def settle(trade: Trade, requiredConfirmations: Int): F[Either[SettlementError, Trade]]

  /** Runs only the pre-authorization step (idempotent). Useful for staged flows. */
  def preAuthorize(trade: Trade, idempotencyKey: String): F[Either[SettlementError, PaymentAuth]]

  /** Captures a previously created payment authorization (idempotent). */
  def capture(auth: PaymentAuth, idempotencyKey: String): F[Either[SettlementError, Unit]]

  /** Performs the on-chain release from seller escrow to buyer address (idempotent). */
  def releaseOnChain(
    trade: Trade,
    fromEscrowAddress: String,
    toBuyerAddress: String,
    amount: BigDecimal,
    requiredConfirmations: Int,
    idempotencyKey: String
  ): F[Either[SettlementError, OnChainTx]]
}

/**
 * Minimal reference wiring for an orchestrator implementation. This is a placeholder
 * to guide concrete implementations and tests; it avoids any business decisions.
 */
class DefaultSettlementOrchestrator[F[_]: Async](
  payments: ObpPaymentsConnector[F],
  escrow: EthereumEscrowConnector[F],
  store: IdempotencyStore[F]
) extends SettlementOrchestrator[F] {

  override def preAuthorize(trade: Trade, idempotencyKey: String): F[Either[SettlementError, PaymentAuth]] = {
    val amountFiat = trade.amount.value // EUR amount = price * qty (as modeled)
    val buyerFiatAccount = trade.buyerAccountId.value
    store.checkAndPut(s"preauth:${idempotencyKey}").ifM(
      Async[F].pure(Left(SettlementError.DuplicateOperation("preauth", idempotencyKey))),
      payments.preAuthorize(buyerFiatAccount, amountFiat, idempotencyKey)
    )
  }

  override def capture(auth: PaymentAuth, idempotencyKey: String): F[Either[SettlementError, Unit]] =
    store.checkAndPut(s"capture:${idempotencyKey}").ifM(
      Async[F].pure(Left(SettlementError.DuplicateOperation("capture", idempotencyKey))),
      payments.capture(auth.authId, idempotencyKey)
    )

  override def releaseOnChain(
    trade: Trade,
    fromEscrowAddress: String,
    toBuyerAddress: String,
    amount: BigDecimal,
    requiredConfirmations: Int,
    idempotencyKey: String
  ): F[Either[SettlementError, OnChainTx]] =
    store.checkAndPut(s"release:${idempotencyKey}").ifM(
      Async[F].pure(Left(SettlementError.DuplicateOperation("release", idempotencyKey))),
      for {
        submitted <- escrow.release(fromEscrowAddress, toBuyerAddress, amount, trade.tradeId.value, idempotencyKey)
        _ <- escrow.waitConfirmations(submitted, requiredConfirmations)
      } yield Right(submitted.copy(requiredConfirmations = requiredConfirmations, state = OnChainTxState.Confirmed))
    )

  override def settle(trade: Trade, requiredConfirmations: Int): F[Either[SettlementError, Trade]] = {
    val idem = trade.tradeId.value
    val buyerFiatAccount = trade.buyerAccountId.value
    val amountFiat = trade.amount.value
    val buyerAddress = trade.metadata.getOrElse("buyer_chain_address", "")
    val sellerEscrow = trade.metadata.getOrElse("seller_escrow_address", "")

    if (buyerAddress.isEmpty || sellerEscrow.isEmpty)
      Async[F].pure(Left(SettlementError.MissingAddresses))
    else for {
      authEither <- preAuthorize(trade, s"$idem:preauth")
      result <- authEither match {
        case Left(e) => Async[F].pure(Left(e))
        case Right(auth) =>
          for {
            cap <- capture(auth, s"$idem:capture")
            res <- (cap match {
              case Left(e) => Async[F].pure(Left(e): Either[SettlementError, Trade])
              case Right(_) =>
                releaseOnChain(
                  trade,
                  sellerEscrow,
                  buyerAddress,
                  trade.quantity.value,
                  requiredConfirmations,
                  s"$idem:release"
                ).map(_.map(_ => trade.settle))
            })
          } yield res
      }
    } yield result
  }
}


