package com.openbankproject.trading.docs.model

import org.http4s.{Request, Response}
import cats.effect.IO
import com.openbankproject.trading.http.OBPEndpoint

/**
  * ResourceDoc aligned with OBP-API structure.
  * Simplified version without Lift dependencies.
  *
  * @param partialFunction The actual partial function implementing this endpoint
  * @param implementedInApiVersion API version (e.g., "v7.0.0")
  * @param partialFunctionName Name of the partial function (e.g., "obpGetOfferPF")
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
