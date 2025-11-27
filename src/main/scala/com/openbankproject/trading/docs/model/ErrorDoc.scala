package com.openbankproject.trading.docs.model

final case class ErrorDoc(code: String, httpStatus: Int, message: Option[String] = None)


