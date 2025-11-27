package com.openbankproject.trading.docs.model

final case class ResourceDoc(
  operationId: String,
  method: HttpMethod,
  path: String,
  summary: String,
  description: String,
  roles: RequiredRole = RequiredRole.Public,
  tags: Set[String] = Set.empty,
  requestExample: Option[String] = None,
  responseExample: Option[String] = None,
  errorResponses: List[ErrorDoc] = Nil
) {
  require(operationId.trim.nonEmpty, "operationId must be non-empty")
  require(path.trim.nonEmpty, "path must be non-empty")
}


