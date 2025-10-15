package com.openbankproject.trading.connector

import cats.effect.Sync
import cats.syntax.all._
import com.openbankproject.trading.settlement.{OnChainTx, OnChainTxState}

import java.util.UUID

/** Minimal fake escrow connector that immediately returns a CONFIRMED tx. */
class FakeEthereumEscrowConnector[F[_]: Sync] extends EthereumEscrowConnector[F] {

  override def release(fromEscrowAddress: String,
                       toRecipient: String,
                       amount: BigDecimal,
                       tradeId: String,
                       idempotencyKey: String): F[OnChainTx] =
    Sync[F].pure {
      OnChainTx(
        txId = s"tx-${UUID.nameUUIDFromBytes((tradeId+idempotencyKey).getBytes).toString}",
        network = "ethereum",
        function = "RELEASE",
        from = fromEscrowAddress,
        to = toRecipient,
        amount = amount,
        confirmations = 12,
        requiredConfirmations = 12,
        state = OnChainTxState.Confirmed,
        hash = UUID.randomUUID().toString.replaceAll("-", ""),
        nonce = 1L,
        error = None
      )
    }

  override def waitConfirmations(tx: OnChainTx, requiredConfirmations: Int): F[Unit] = Sync[F].unit
}


