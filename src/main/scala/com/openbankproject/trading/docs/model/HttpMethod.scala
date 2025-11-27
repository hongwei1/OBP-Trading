package com.openbankproject.trading.docs.model

sealed trait HttpMethod extends Product with Serializable { def name: String }

object HttpMethod {
  case object GET extends HttpMethod { val name: String = "GET" }
  case object POST extends HttpMethod { val name: String = "POST" }
  case object PUT extends HttpMethod { val name: String = "PUT" }
  case object DELETE extends HttpMethod { val name: String = "DELETE" }
  case object PATCH extends HttpMethod { val name: String = "PATCH" }
  case object HEAD extends HttpMethod { val name: String = "HEAD" }
  case object OPTIONS extends HttpMethod { val name: String = "OPTIONS" }
}


