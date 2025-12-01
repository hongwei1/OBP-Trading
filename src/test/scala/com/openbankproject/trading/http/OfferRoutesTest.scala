package com.openbankproject.trading.http

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.openbankproject.trading.service._
import io.circe.syntax._
import io.circe.generic.auto._
import org.http4s._
import org.http4s.Method._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.implicits._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class OfferRoutesTest extends AnyFunSuite with Matchers {

  private def app(offerService: OfferService) = {
    val orderStub: OrderService = new OrderService {
      def createOrder(req: CreateOrderRequest) = IO.pure(Left(ErrorResponse("not_implemented", "order create not used in this test")))
      def cancelOrder(orderId: String) = IO.pure(Left(ErrorResponse("not_implemented", "order cancel not used in this test")))
      def getOrder(orderId: String) = IO.pure(Left(ErrorResponse("not_found", s"order $orderId not found")))
    }
    val matchStub: MatchService = new MatchService {
      def createMatch(req: MatchRequest) = IO.pure(Left(ErrorResponse("not_implemented", "match not used in this test")))
    }
    val settlementStub: SettlementService = new SettlementService {
      def settle(req: SettlementRequest) = IO.pure(Left(ErrorResponse("not_implemented", "settle not used in this test")))
      def getTrade(tradeId: String) = IO.pure(Left(ErrorResponse("not_implemented", "getTrade not used in this test")))
    }
    val fundsStub: FundsService = new FundsService {
      def notifyDeposit(req: DepositNotification) = IO.pure(Left(ErrorResponse("not_implemented", "notifyDeposit not used in this test")))
      def requestWithdrawal(req: WithdrawalRequest) = IO.pure(Left(ErrorResponse("not_implemented", "requestWithdrawal not used in this test")))
    }
    Routes.api(orderStub, offerService, matchStub, settlementStub, fundsStub).orNotFound
  }

  test("create -> get -> cancel offer happy path") {
    val service = InMemoryOfferService.create().unsafeRunSync()
    val httpApp = app(service)

    val bankId = "bank-1"
    val accountId = "acc-1"
    val viewId = "owner"

    val createJson = Map(
      "offer_type" -> "BUY",
      "asset_code" -> "BTC",
      "asset_amount" -> "1.5",
      "price_currency" -> "USD",
      "price_amount" -> "45000.00",
      "settlement_account_id" -> accountId
    ).asJson

    val createReq = Request[IO](
      method = POST,
      uri = uri"/obp/v7.0.0/banks/" / bankId / "accounts" / accountId / "views" / viewId / "trading" / "offers"
    ).withEntity(createJson)

    val createResp = httpApp.run(createReq).unsafeRunSync()
    createResp.status shouldBe Status.Created
    val created = createResp.as[CreateOfferResponse].unsafeRunSync()
    created.status shouldBe "active"
    created.remaining shouldBe BigDecimal("1.5")

    val getReq = Request[IO](
      method = GET,
      uri = uri"/obp/v7.0.0/banks/" / bankId / "accounts" / accountId / "views" / viewId / "trading" / "offers" / created.offerId
    )
    val getResp = httpApp.run(getReq).unsafeRunSync()
    getResp.status shouldBe Status.Ok
    val view = getResp.as[OfferView].unsafeRunSync()
    view.offerId shouldBe created.offerId
    view.offerType shouldBe "BUY"

    val delReq = Request[IO](
      method = DELETE,
      uri = uri"/obp/v7.0.0/banks/" / bankId / "accounts" / accountId / "views" / viewId / "trading" / "offers" / created.offerId
    )
    val delResp = httpApp.run(delReq).unsafeRunSync()
    delResp.status shouldBe Status.Ok
    val cancelled = delResp.as[CancelOfferResponse].unsafeRunSync()
    cancelled.offerId shouldBe created.offerId
    cancelled.status shouldBe "cancelled"
  }

  test("invalid JSON returns 400") {
    val service = InMemoryOfferService.create().unsafeRunSync()
    val httpApp = app(service)

    val req = Request[IO](
      method = POST,
      uri = uri"/obp/v7.0.0/banks/b1/accounts/a1/views/v1/trading/offers"
    ).withEntity("{" ) // malformed JSON

    val resp = httpApp.run(req).unsafeRunSync()
    resp.status shouldBe Status.BadRequest
  }

  test("get non-existing offer returns 404") {
    val service = InMemoryOfferService.create().unsafeRunSync()
    val httpApp = app(service)

    val req = Request[IO](
      method = GET,
      uri = uri"/obp/v7.0.0/banks/b1/accounts/a1/views/v1/trading/offers/does-not-exist"
    )

    val resp = httpApp.run(req).unsafeRunSync()
    resp.status shouldBe Status.NotFound
  }
}


