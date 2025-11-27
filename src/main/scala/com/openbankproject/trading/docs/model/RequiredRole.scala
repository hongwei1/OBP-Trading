package com.openbankproject.trading.docs.model

sealed trait RequiredRole extends Product with Serializable

object RequiredRole {
  case object Public extends RequiredRole {
    override def toString: String = "Public"
  }
}


