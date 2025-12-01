package com.openbankproject.trading.docs.model

import org.http4s.{Request, Response}
import cats.effect.IO
import com.openbankproject.trading.http.OBPEndpoint
import io.circe.Json

/**
  * ResourceDoc aligned with OBP-API structure.
  * Simplified version without Lift dependencies.
  *
  * @param partialFunction The actual partial function implementing this endpoint
  * @param implementedInApiVersion API version (e.g., "v7.0.0")
  * @param partialFunctionName Name of the partial function (e.g., "getOfferPF")
  * @param requestVerb HTTP method (GET, POST, PUT, DELETE, etc.)
  * @param requestUrl URL pattern with path parameters
  * @param summary Short description of the endpoint
  * @param description Detailed description
  * @param exampleRequestBody Example request body as case class (use EmptyBody if no body)
  * @param successResponseBody Example success response as case class
  * @param errorResponseBodies List of possible error messages
  * @param tags Tags for categorization
  * @param roles Required roles (None means public)
  */
final case class ResourceDoc(
  partialFunction: OBPEndpoint,
  implementedInApiVersion: String,
  partialFunctionName: String,
  requestVerb: String,
  requestUrl: String,
  summary: String,
  description: String,
  exampleRequestBody: Product,
  successResponseBody: Product,
  errorResponseBodies: List[String],
  tags: List[String],
  roles: Option[List[String]] = None,
  isFeatured: Boolean = false,
  specialInstructions: Option[String] = None,
  specifiedUrl: Option[String] = None,
  createdByBankId: Option[String] = None
) {
  require(partialFunctionName.trim.nonEmpty, "partialFunctionName must be non-empty")
  require(requestUrl.trim.nonEmpty, "requestUrl must be non-empty")
  require(requestVerb.trim.nonEmpty, "requestVerb must be non-empty")
}

/**
  * Used to describe where an API call is implemented, similar to OBP's ImplementedByJson.
  */
final case class ImplementedByJson(
  version: String,  // Short hand for version, e.g. "v7_0_0"
  function: String  // The partial function name, e.g. "getOfferPF"
)

/**
  * Export-friendly ResourceDocJson, mirroring OBP-API structure for docs/export.
  */
final case class ResourceDocJson(
  operation_id: String,
  implemented_by: ImplementedByJson,
  request_url: String,
  summary: String,
  description: String,
  description_markdown: String,
  example_request_body: Json,
  success_response_body: Json,
  error_response_bodies: List[String],
  tags: List[String],
  typed_request_body: Json,
  typed_success_response_body: Json,
  roles: Option[List[String]] = None,
  is_featured: Boolean = false,
  special_instructions: Option[String] = None,
  specified_url: Option[String] = None,
  connector_methods: List[String] = Nil,
  created_by_bank_id: Option[String] = None
)

object ResourceDocJson {
  import io.circe.generic.semiauto._
  import io.circe.{Encoder, Decoder}

  implicit val implementedByJsonEncoder: Encoder[ImplementedByJson] =
    deriveEncoder[ImplementedByJson]
  implicit val implementedByJsonDecoder: Decoder[ImplementedByJson] =
    deriveDecoder[ImplementedByJson]

  implicit val resourceDocJsonEncoder: Encoder[ResourceDocJson] =
    deriveEncoder[ResourceDocJson]
  implicit val resourceDocJsonDecoder: Decoder[ResourceDocJson] =
    deriveDecoder[ResourceDocJson]
}
