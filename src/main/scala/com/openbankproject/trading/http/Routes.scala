package com.openbankproject.trading.http

import cats.effect.IO
import cats.syntax.all._
import com.github.andyglow.jsonschema.AsCirce._
import json.{Json => SchemaJson, _}
import json.schema.Version.Draft07
import com.openbankproject.trading.docs.model.{EmptyBody, ImplementedByJson, ResourceDoc, ResourceDocJson, ResourceDocMeta, ResourceDocsJson}
import com.openbankproject.trading.docs.registry.ResourceDocRegistry
import com.openbankproject.trading.service._
import io.circe.generic.auto._
import io.circe.parser.parse
import io.circe.syntax._
import io.circe.Json
import org.http4s._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.io._

import java.time.Instant
import java.util.UUID
import scala.util.Try

/** Aggregated HTTP routes (interfaces only, no concrete wiring). */
object Routes {
  // ===== Named partial functions for OBP Offer endpoints =====
  def createOfferPF(offer: OfferService): OBPEndpoint = {
    {
      // POST /obp/v7.0.0/banks/BANK_ID/accounts/ACCOUNT_ID/views/VIEW_ID/trading/offers
      case req @ POST -> Root / "obp" / "v7.0.0" / "banks" / bankId / "accounts" / accountId / "views" / viewId / "trading" / "offers" =>
        // Minimal body mapping based on trading-api-endpoints.md
        case class CreateOfferReq(
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
          parse(raw).leftMap(_.getMessage).flatMap(_.as[CreateOfferReq].leftMap(_.getMessage)) match {
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

  // Helper case classes for ResourceDoc examples
  private final case class ObpOfferExecutionExample(
    execution_id: String,
    executed_amount: String,
    executed_price: String,
    executed_at: String,
    counterpart_offer_id: String
  )
  private implicit val obpOfferExecutionSchema: Schema[ObpOfferExecutionExample] =
    SchemaJson.schema[ObpOfferExecutionExample]
  private implicit val obpOfferDetailsSchema: Schema[ObpOfferDetailsExample] =
    SchemaJson.schema[ObpOfferDetailsExample]
  private implicit val obpOfferAccountSchema: Schema[ObpOfferAccountInfoExample] =
    SchemaJson.schema[ObpOfferAccountInfoExample]
  private implicit val obpOfferResponseSchema: Schema[ObpOfferResponseExample] =
    SchemaJson.schema[ObpOfferResponseExample]
  private final case class ObpOfferDetailsExample(
    offer_type: String,
    asset_code: String,
    asset_amount: String,
    filled_amount: String,
    remaining_amount: String,
    price_currency: String,
    price_amount: String,
    expiry_datetime: String,
    minimum_fill: String
  )
  private final case class ObpOfferAccountInfoExample(
    bank_id: String,
    account_id: String,
    view_id: String
  )
  private final case class ObpOfferResponseExample(
    offer_id: String,
    status: String,
    created_at: String,
    updated_at: String,
    offer_details: ObpOfferDetailsExample,
    account_info: ObpOfferAccountInfoExample,
    executions: List[ObpOfferExecutionExample]
  )
  private val draft07 = Draft07("http://json-schema.org/draft-07/schema#")
  private def schemaOf[T: Schema]: Json =
    SchemaJson.schema[T].asCirce(draft07)
  private def schemaFromProduct(p: Option[Product]): Option[Json] =
    p.collect {
      case _: ObpOfferResponseExample => schemaOf[ObpOfferResponseExample]
    }
  // ResourceDoc for: GET /obp/v7.0.0/.../trading/offers/{OFFER_ID}
  def getOfferDoc(offer: OfferService): ResourceDoc = ResourceDoc(
    partialFunction = getOfferPF(offer),
    implementedInApiVersion = "OBPv7.0.0",
    partialFunctionName = "getOfferPF",
    requestVerb = "GET",
    requestUrl = "/obp/v7.0.0/banks/BANK_ID/accounts/ACCOUNT_ID/views/VIEW_ID/trading/offers/OFFER_ID",
    summary = "Get trading offer by id",
    description = "Returns the trading offer details by id for the given bank/account/view.",
    exampleRequestBody = None,
    successResponseBody = ObpOfferResponseExample(
      offer_id = "offer_789",
      status = "active",
      created_at = "2024-01-15T10:30:00Z",
      updated_at = "2024-01-15T10:30:00Z",
      offer_details = ObpOfferDetailsExample(
        offer_type = "buy",
        asset_code = "BTC",
        asset_amount = "1.5",
        filled_amount = "0.3",
        remaining_amount = "1.2",
        price_currency = "USD",
        price_amount = "45000.00",
        expiry_datetime = "2024-12-31T23:59:59Z",
        minimum_fill = "0.1"
      ),
      account_info = ObpOfferAccountInfoExample(
        bank_id = "BANK_ID",
        account_id = "ACCOUNT_ID",
        view_id = "VIEW_ID"
      ),
      executions = List(
        ObpOfferExecutionExample(
          execution_id = "exec_123",
          executed_amount = "0.3",
          executed_price = "45000.00",
          executed_at = "2024-01-15T11:00:00Z",
          counterpart_offer_id = "offer_456"
        )
      )
    ),
    errorResponseBodies = List("not_found", "bad_request"),
    tags = List("trading", "offer"),
    roles = None,
    isFeatured = false,
    specialInstructions = None,
    specifiedUrl = "/banks/BANK_ID/accounts/ACCOUNT_ID/views/VIEW_ID/trading/offers/OFFER_ID",
    createdByBankId = None
  )

  def getOfferPF(offer: OfferService): OBPEndpoint = {
    {
      case GET -> Root / "obp" / "v7.0.0" / "banks" / bankId / "accounts" / accountId / "views" / viewId / "trading" / "offers" / offerId =>
        offer.getOffer(offerId).flatMap {
          case Right(v)  => Ok(v)
          case Left(err) => NotFound(err)
        }
    }
  }

  def cancelOfferPF(offer: OfferService): OBPEndpoint = {
    {
      // DELETE /obp/v7.0.0/banks/BANK_ID/accounts/ACCOUNT_ID/views/VIEW_ID/trading/offers/OFFER_ID
      case DELETE -> Root / "obp" / "v7.0.0" / "banks" / bankId / "accounts" / accountId / "views" / viewId / "trading" / "offers" / offerId =>
        offer.cancelOffer(offerId).flatMap {
          case Right(ok) => Ok(ok)
          case Left(err) => BadRequest(err)
        }
    }
  }
  private def productToJson(p: Product): Json = p match {
    case EmptyBody => Json.Null
    case resp: ObpOfferResponseExample => resp.asJson
    case other => Json.fromString(other.toString)
  }

  private def toResourceDocJson(doc: ResourceDoc): ResourceDocJson =
    ResourceDocJson(
      operation_id = doc.implementedInApiVersion + "-" + doc.partialFunctionName,
      implemented_by = ImplementedByJson(doc.implementedInApiVersion, doc.partialFunctionName),
      request_verb = doc.requestVerb,
      request_url = doc.requestUrl,
      summary = doc.summary,
      description = doc.description,
      description_markdown = doc.description,
      example_request_body = doc.exampleRequestBody.map(productToJson),
      success_response_body = productToJson(doc.successResponseBody),
      error_response_bodies = doc.errorResponseBodies,
      tags = doc.tags,
      typed_request_body = schemaFromProduct(doc.exampleRequestBody),
      typed_success_response_body = schemaFromProduct(Some(doc.successResponseBody)),
      roles = doc.roles,
      is_featured = doc.isFeatured,
      special_instructions = doc.specialInstructions,
      specified_url = doc.specifiedUrl,
      connector_methods = Nil,
      created_by_bank_id = doc.createdByBankId
    )

  private def normalizeVersion(apiVersion: String): String = {
    val upper = apiVersion.toUpperCase
    if (upper.startsWith("OBP")) upper else s"OBP$upper"
  }

  def getResourceDocsPF: OBPEndpoint = {
    {
      case GET -> Root / "obp" / "v7.0.0" / "resource-docs" / apiVersion / "obp" =>
        val normalized = normalizeVersion(apiVersion)
        val docs = ResourceDocRegistry.all
          .filter(_.implementedInApiVersion.equalsIgnoreCase(normalized))
          .map(toResourceDocJson)
          .toList
        val meta = ResourceDocMeta(response_date = Instant.now(), count = docs.size)
        val payload = ResourceDocsJson(resource_docs = docs, meta = Some(meta))
        Ok(payload.asJson)
    }
  }

  def api(
    order: OrderService,
    offer: OfferService,
    matcher: MatchService,
    settlement: SettlementService,
    funds: FundsService
  ): HttpRoutes[IO] = {
    ResourceDocRegistry.register(getOfferDoc(offer))
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
    val offerPF =
      createOfferPF(offer)
        .orElse(getOfferPF(offer))
        .orElse(cancelOfferPF(offer))
        .orElse(getResourceDocsPF)
    val offerRoutes: HttpRoutes[IO] = HttpRoutes.of[IO](offerPF)
    marketRoutes <+> offerRoutes
  }
}


