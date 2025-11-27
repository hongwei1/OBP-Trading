package com.openbankproject.trading.docs

import com.openbankproject.trading.docs.model.{ErrorDoc, HttpMethod, RequiredRole, ResourceDoc}
import com.openbankproject.trading.docs.registry.ResourceDocRegistry

/**
  * Registers ResourceDocs for OBP-Trading endpoints (framework-agnostic).
  */
object TradingResourceDocs {

  private val docs = Seq.empty[ResourceDoc]

  def registerAll(): Unit = ResourceDocRegistry.registerAll(docs)
}


