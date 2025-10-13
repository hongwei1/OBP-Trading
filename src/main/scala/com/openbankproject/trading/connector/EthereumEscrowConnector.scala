package com.openbankproject.trading.connector

import com.openbankproject.trading.settlement.{OnChainTx, OnChainTxState}

/** Abstraction of escrow interactions used by the orchestrator. */
trait EthereumEscrowConnector[F[_]] {

  /**
   * Submit a release transaction from escrow to recipient.
   * Idempotent by (tradeId,idempotencyKey) at the implementation side.
   */
  def release(fromEscrowAddress: String,
              toRecipient: String,
              amount: BigDecimal,
              tradeId: String,
              idempotencyKey: String): F[OnChainTx]

  /** Blocks until the given tx reaches required confirmations or fails (implementation may poll). */
  def waitConfirmations(tx: OnChainTx, requiredConfirmations: Int): F[Unit]
}


