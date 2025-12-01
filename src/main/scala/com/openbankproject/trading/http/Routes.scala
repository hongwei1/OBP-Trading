package com.openbankproject.trading.http

import org.http4s._
import org.http4s.dsl.io._
import cats.syntax.all._
import com.openbankproject.trading.service._
import org.http4s.circe.CirceEntityCodec._
import io.circe.generic.auto._
import io.circe.parser.parse
import java.util.UUID
import scala.util.Try
import com.openbankproject.trading.docs.model.{ResourceDoc, EmptyBody}
import cats.effect.IO

/** Aggregated HTTP routes (interfaces only, no concrete wiring). */
object Routes {
  // ===== Named partial functions for OBP Offer endpoints =====
  def obpCreateOfferPF(offer: OfferService): OBPEndpoint = {
    {
      // POST /obp/v7.0.0/banks/BANK_ID/accounts/ACCOUNT_ID/views/VIEW_ID/trading/offers
      case req @ POST -> Root / "obp" / "v7.0.0" / "banks" / bankId / "accounts" / accountId / "views" / viewId / "trading" / "offers" =>
        // Minimal body mapping based on trading-api-endpoints.md
        case class ObpCreateOfferReq(
          offer_type: String,
          asset_code: String,
          asset_amount: String,
          price_currency: String,
          price_amount: String,
          expiry_datetime: Option[String],
          minimum_fill: Option[String],
          settlement_account_id: String
        )
        req.bodyText.compile.string.flatMap { raw =>
          parse(raw).leftMap(_.getMessage).flatMap(_.as[ObpCreateOfferReq].leftMap(_.getMessage)) match {
            case Left(msg) => BadRequest(ErrorResponse(ErrorCodes.INVALID_JSON, s"Invalid JSON: $msg"))
            case Right(obp) =>
              val side  = obp.offer_type.toUpperCase match {
                case "BUY"  => "BUY"
                case "SELL" => "SELL"
                case other   => other
              }
              val qty   = Try(BigDecimal(obp.asset_amount)).getOrElse(BigDecimal(0))
              val price = Try(BigDecimal(obp.price_amount)).getOrElse(BigDecimal(0))
              val acct  = Option(obp.settlement_account_id).filter(_.nonEmpty).getOrElse(accountId)
              val idKey = s"obp-${UUID.randomUUID().toString}"
              val req0  = CreateOfferRequest(
                offerType = side,
                price = price,
                quantity = qty,
                accountId = acct,
                idempotencyKey = idKey
              )
              offer.createOffer(req0).flatMap {
                case Right(ok)  => Created(ok)
                case Left(err)  => BadRequest(err)
              }
          }
        }
    }
  }

  // ===== Named partial functions for Minimal Market endpoints =====
  def postMarketOrdersPF(order: OrderService): OBPEndpoint = {
    {
      // POST /market/orders
      case req @ POST -> Root / "market" / "orders" =>
        req.attemptAs[CreateOrderRequest].value.flatMap {
          case Left(df) => BadRequest(ErrorResponse(ErrorCodes.BAD_REQUEST, Option(df.getMessage).getOrElse(df.toString)))
          case Right(payload) =>
            order.createOrder(payload).flatMap {
              case Right(ok)  => Created(ok)
              case Left(err)  => BadRequest(err)
            }
        }
    }
  }

  def deleteMarketOrderPF(order: OrderService): OBPEndpoint = {
    {
      // DELETE /market/orders/{id}
      case DELETE -> Root / "market" / "orders" / orderId =>
        order.cancelOrder(orderId).flatMap {
          case Right(ok) => Ok(ok)
          case Left(err) => BadRequest(err)
        }
    }
  }

  def getMarketOrderPF(order: OrderService): OBPEndpoint = {
    {
      // GET /market/orders/{id}
      case GET -> Root / "market" / "orders" / orderId =>
        order.getOrder(orderId).flatMap {
          case Right(v)  => Ok(v)
          case Left(err) => NotFound(err)
        }
    }
  }

  def postMarketMatchesPF(matcher: MatchService): OBPEndpoint = {
    {
      // POST /market/matches
      case req @ POST -> Root / "market" / "matches" =>
        IO.pure(Response[IO](status = Status.NotImplemented))
    }
  }

  def postMarketSettlementsPF(settlement: SettlementService): OBPEndpoint = {
    {
      // POST /market/settlements
      case req @ POST -> Root / "market" / "settlements" =>
        IO.pure(Response[IO](status = Status.NotImplemented))
    }
  }

  def getMarketTradePF(settlement: SettlementService): OBPEndpoint = {
    {
      // GET /market/trades/{id}
      case GET -> Root / "market" / "trades" / tradeId =>
        IO.pure(Response[IO](status = Status.NotImplemented))
    }
  }

  def postMarketDepositsPF(funds: FundsService): OBPEndpoint = {
    {
      // POST /market/deposits
      case req @ POST -> Root / "market" / "deposits" =>
        IO.pure(Response[IO](status = Status.NotImplemented))
    }
  }

  def postMarketWithdrawalsPF(funds: FundsService): OBPEndpoint = {
    {
      // POST /market/withdrawals
      case req @ POST -> Root / "market" / "withdrawals" =>
        IO.pure(Response[IO](status = Status.NotImplemented))
    }
  }

  // ResourceDoc for: GET /obp/v7.0.0/.../trading/offers/{OFFER_ID}
  def getObpOfferDoc(offer: OfferService): ResourceDoc = ResourceDoc(
    partialFunction = obpGetOfferPF(offer),
    implementedInApiVersion = "v7.0.0",
    partialFunctionName = "obpGetOfferPF",
    requestVerb = "GET",
    requestUrl = "/obp/v7.0.0/banks/{BANK_ID}/accounts/{ACCOUNT_ID}/views/{VIEW_ID}/trading/offers/{OFFER_ID}",
    summary = "Get OBP trading offer by id",
    description = "Returns the trading offer details by id for the given bank/account/view.",
    exampleRequestBody = EmptyBody,
    successResponseBody = EmptyBody,
    errorResponseBodies = List("not_found", "bad_request"),
    tags = List("trading", "offer"),
    roles = None,
    isFeatured = false,
    specialInstructions = None,
    specifiedUrl = None,
    createdByBankId = None
  )

  def obpGetOfferPF(offer: OfferService): OBPEndpoint = {
    {
      case GET -> Root / "obp" / "v7.0.0" / "banks" / bankId / "accounts" / accountId / "views" / viewId / "trading" / "offers" / offerId =>
        offer.getOffer(offerId).flatMap {
          case Right(v)  => Ok(v)
          case Left(err) => NotFound(err)
        }
    }
  }

  def obpCancelOfferPF(offer: OfferService): OBPEndpoint = {
    {
      // DELETE /obp/v7.0.0/banks/BANK_ID/accounts/ACCOUNT_ID/views/VIEW_ID/trading/offers/OFFER_ID
      case DELETE -> Root / "obp" / "v7.0.0" / "banks" / bankId / "accounts" / accountId / "views" / viewId / "trading" / "offers" / offerId =>
        offer.cancelOffer(offerId).flatMap {
          case Right(ok) => Ok(ok)
          case Left(err) => BadRequest(err)
        }
    }
  }
  def api(
    order: OrderService,
    offer: OfferService,
    matcher: MatchService,
    settlement: SettlementService,
    funds: FundsService
  ): HttpRoutes[IO] = {
    val marketPF =
      postMarketOrdersPF(order)
        .orElse(deleteMarketOrderPF(order))
        .orElse(getMarketOrderPF(order))
        .orElse(postMarketMatchesPF(matcher))
        .orElse(postMarketSettlementsPF(settlement))
        .orElse(getMarketTradePF(settlement))
        .orElse(postMarketDepositsPF(funds))
        .orElse(postMarketWithdrawalsPF(funds))
    val marketRoutes: HttpRoutes[IO] = HttpRoutes.of[IO](marketPF)
    val obpOfferPF =
      obpCreateOfferPF(offer)
        .orElse(obpGetOfferPF(offer))
        .orElse(obpCancelOfferPF(offer))
    val obpOfferRoutes: HttpRoutes[IO] = HttpRoutes.of[IO](obpOfferPF)
    marketRoutes <+> obpOfferRoutes
  }
}


