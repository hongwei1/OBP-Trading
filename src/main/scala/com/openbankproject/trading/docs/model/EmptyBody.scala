package com.openbankproject.trading.docs.model

/**
  * Marker for empty request/response body in ResourceDoc examples.
  */
case object EmptyBody extends Product with Serializable {
  override def productArity: Int = 0
  override def productElement(n: Int): Any = throw new IndexOutOfBoundsException(n.toString)
  override def canEqual(that: Any): Boolean = that.isInstanceOf[EmptyBody.type]
}

