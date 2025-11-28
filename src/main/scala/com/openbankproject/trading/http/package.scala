package com.openbankproject.trading

import org.http4s.{Request, Response}

/**
 * Package object for HTTP-related constants, type aliases, and utilities.
 * 
 * Best practices:
 * - Type aliases for common patterns
 * - API constants (versions, limits, defaults)
 * - Common implicit conversions
 * - Small utility functions
 */
package object http {
  
  // ========== Type Aliases ==========
  /** Type alias for HTTP endpoint partial functions */
  type OBPEndpoint[F[_]] = PartialFunction[Request[F], F[Response[F]]]
  
  // ========== API Constants ==========
  /** Current OBP API version */
  val OBP_API_VERSION: String = "v7.0.0"
  
  /** Default pagination page size */
  val DEFAULT_PAGE_SIZE: Int = 50
  
  /** Maximum pagination page size */
  val MAX_PAGE_SIZE: Int = 500
  
  /** Default request timeout in seconds */
  val DEFAULT_TIMEOUT_SECONDS: Int = 30
  
  // ========== HTTP Headers ==========
  /** Custom header for request tracing */
  val HEADER_REQUEST_ID: String = "X-Request-ID"
  
  /** Custom header for API version */
  val HEADER_API_VERSION: String = "X-OBP-API-Version"
  
  // ========== Error Codes ==========
  /** Standard error code constants */
  object ErrorCodes {
    val BAD_REQUEST = "bad_request"
    val NOT_FOUND = "not_found"
    val UNAUTHORIZED = "unauthorized"
    val FORBIDDEN = "forbidden"
    val INTERNAL_ERROR = "internal_error"
    val NOT_IMPLEMENTED = "not_implemented"
    val INVALID_JSON = "invalid_json"
    val VALIDATION_ERROR = "validation_error"
  }
}

