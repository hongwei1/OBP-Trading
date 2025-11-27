package com.openbankproject.trading.http

import org.http4s._
import org.http4s.dsl.Http4sDsl
import cats.effect.Async
import cats.syntax.all._
import com.openbankproject.trading.service._
import org.http4s.circe.CirceEntityCodec._
import io.circe.generic.auto._
import io.circe.parser.parse
import java.util.UUID
import scala.util.Try
import com.openbankproject.trading.docs.model.{ResourceDoc, HttpMethod, RequiredRole, ErrorDoc}
import com.openbankproject.trading.docs.registry.ResourceDocRegistry

/** Aggregated HTTP routes (interfaces only, no concrete wiring). */
object Routes {
  def api[F[_]: Async](
    order: OrderService[F],
    offer: OfferService[F],
    matcher: MatchService[F],
    settlement: SettlementService[F],
    funds: FundsService[F]
  ): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}; import dsl._

    // ResourceDoc for: GET /obp/v7.0.0/.../trading/offers/{OFFER_ID}
    val getObpOfferDoc: ResourceDoc = ResourceDoc(
      operationId = "getObpOffer",
      method = HttpMethod.GET,
      path = "/obp/v7.0.0/banks/{BANK_ID}/accounts/{ACCOUNT_ID}/views/{VIEW_ID}/trading/offers/{OFFER_ID}",
      summary = "Get OBP trading offer by id",
      description = "Returns the trading offer details by id for the given bank/account/view.",
      roles = RequiredRole.Public,
      tags = Set("trading", "offer"),
      requestExample = None,
      responseExample = Some(
        """{
          |  "offerId": "OFFER-123",
          |  "offerType": "BUY",
          |  "price": 100.50,
          |  "quantity": 2.0,
          |  "remaining": 0.0,  
          |  "status": "FILLED",
          |  "ownerAccountId": "ACC-001",
          |  "createdAt": "2025-11-03T10:20:30Z",
          |  "expiresAt": null
          |}""".stripMargin
      ),
      errorResponses = List(
        ErrorDoc(code = "not_found", httpStatus = 404, message = Some("Offer not found")),
        ErrorDoc(code = "bad_request", httpStatus = 400, message = Some("Invalid parameters"))
      )
    )
    ResourceDocRegistry.register(getObpOfferDoc)

    HttpRoutes.of[F] {
      // ========== Minimal market endpoints (internal shape) ==========
      // POST /market/orders
      case req @ POST -> Root / "market" / "orders" =>
        req.attemptAs[CreateOrderRequest].value.flatMap {
          case Left(df) => BadRequest(ErrorResponse("bad_request", Option(df.getMessage).getOrElse(df.toString)))
          case Right(payload) =>
            order.createOrder(payload).flatMap {
              case Right(ok)  => Created(ok)
              case Left(err)  => BadRequest(err)
            }
        }

      // DELETE /market/orders/{id}
      case DELETE -> Root / "market" / "orders" / orderId =>
        order.cancelOrder(orderId).flatMap {
          case Right(ok) => Ok(ok)
          case Left(err) => BadRequest(err)
        }

      // GET /market/orders/{id}
      case GET -> Root / "market" / "orders" / orderId =>
        order.getOrder(orderId).flatMap {
          case Right(v)  => Ok(v)
          case Left(err) => NotFound(err)
        }

      // POST /market/matches
      case req @ POST -> Root / "market" / "matches" =>
        Async[F].pure(Response[F](status = Status.NotImplemented))

      // POST /market/settlements
      case req @ POST -> Root / "market" / "settlements" =>
        Async[F].pure(Response[F](status = Status.NotImplemented))

      // GET /market/trades/{id}
      case GET -> Root / "market" / "trades" / tradeId =>
        Async[F].pure(Response[F](status = Status.NotImplemented))

      // POST /market/deposits
      case req @ POST -> Root / "market" / "deposits" =>
        Async[F].pure(Response[F](status = Status.NotImplemented))

      // POST /market/withdrawals
      case req @ POST -> Root / "market" / "withdrawals" =>
        Async[F].pure(Response[F](status = Status.NotImplemented))

      // ========== OBP-style Offer endpoints (map to OfferService) ==========
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
            case Left(msg) => BadRequest(ErrorResponse("bad_request", s"Invalid JSON: $msg"))
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

      // GET /obp/v7.0.0/banks/BANK_ID/accounts/ACCOUNT_ID/views/VIEW_ID/trading/offers/OFFER_ID
      case GET -> Root / "obp" / "v7.0.0" / "banks" / bankId / "accounts" / accountId / "views" / viewId / "trading" / "offers" / offerId =>
        offer.getOffer(offerId).flatMap {
          case Right(v)  => Ok(v)
          case Left(err) => NotFound(err)
        }

      // DELETE /obp/v7.0.0/banks/BANK_ID/accounts/ACCOUNT_ID/views/VIEW_ID/trading/offers/OFFER_ID
      case DELETE -> Root / "obp" / "v7.0.0" / "banks" / bankId / "accounts" / accountId / "views" / viewId / "trading" / "offers" / offerId =>
        offer.cancelOffer(offerId).flatMap {
          case Right(ok) => Ok(ok)
          case Left(err) => BadRequest(err)
        }
    }
  }
}


