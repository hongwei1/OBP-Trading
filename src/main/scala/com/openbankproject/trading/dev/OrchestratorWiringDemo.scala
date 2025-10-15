// package com.openbankproject.trading.dev

// import cats.effect.{IO, IOApp}
// import com.openbankproject.trading.connector.{FakeEthereumEscrowConnector, FakeObpPaymentsConnector}
// import com.openbankproject.trading.model._
// import com.openbankproject.trading.settlement.{DefaultSettlementOrchestrator, InMemoryIdempotencyStore}

// import java.time.Instant

// /**
//   * Minimal wiring to exercise DefaultSettlementOrchestrator with fake connectors.
//   * This is for local development only; it does not perform real payments or on-chain actions.
//   */
// object OrchestratorWiringDemo extends IOApp.Simple {

//   override def run: IO[Unit] = {
//     val payments = new FakeObpPaymentsConnector[IO]
//     val escrow   = new FakeEthereumEscrowConnector[IO]
//     val store    = new InMemoryIdempotencyStore[IO]
//     val orch     = new DefaultSettlementOrchestrator[IO](payments, escrow, store)

//     // Build a dummy trade for EUR<->OGCR
//     val buyOffer = Offer(
//       offerId = OfferId("buy-offer-1"),
//       userId = UserId("buyer-1"),
//       user_id = UserId("buyer-1"),
//       consent_id = ConsentId("consent-1"),
//       accountId = AccountId("buyer-fiat-account"),
//       bankId = BankId("bank-1"),
//       symbol = TradingSymbol("OGCR-EUR"),
//       offerType = OfferType.Buy,
//       price = Price(BigDecimal(25)),
//       originalQuantity = Quantity(BigDecimal(10)),
//       remainingQuantity = Quantity(BigDecimal(0)),
//       status = OfferStatus.Filled,
//       createdAt = Instant.now(),
//       updatedAt = Instant.now(),
//       expiresAt = Instant.now().plusSeconds(3600)
//     )

//     val sellOffer = buyOffer.copy(
//       offerId = OfferId("sell-offer-1"),
//       userId = UserId("seller-1"),
//       user_id = UserId("seller-1"),
//       accountId = AccountId("seller-token-account"),
//       offerType = OfferType.Sell
//     )

//     val trade = Trade.create(
//       createdByUserId = UserId("system"),
//       consentId = ConsentId("system-consent"),
//       symbol = TradingSymbol("OGCR-EUR"),
//       buyOffer = buyOffer,
//       sellOffer = sellOffer,
//       tradePrice = Price(BigDecimal(25)),
//       tradeQuantity = Quantity(BigDecimal(10))
//     ).copy(
//       metadata = Map(
//         "buyer_chain_address" -> "0xBuyerAddress",
//         "seller_escrow_address" -> "0xSellerEscrow"
//       )
//     )

//     for {
//       res <- orch.settle(trade, requiredConfirmations = 3)
//       _ <- res match {
//         case Left(err) => IO.println(s"Settlement failed: ${err.getMessage}")
//         case Right(t)  => IO.println(s"Settlement success: tradeId=${t.tradeId.value} status=${t.status}")
//       }
//     } yield ()
//   }
// }


