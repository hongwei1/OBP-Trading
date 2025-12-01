package com.openbankproject.trading.docs

import com.openbankproject.trading.docs.model.ResourceDoc
import com.openbankproject.trading.docs.registry.ResourceDocRegistry

/**
  * Registers ResourceDocs for OBP-Trading endpoints (framework-agnostic).
  */
object TradingResourceDocs {

  private val docs: Seq[ResourceDoc[F] forSome { type F[_] }] = Seq.empty

  def registerAll(): Unit = ResourceDocRegistry.registerAll(docs)
}


