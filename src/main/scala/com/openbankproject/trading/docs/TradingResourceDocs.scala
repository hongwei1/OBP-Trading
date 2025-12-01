package com.openbankproject.trading.docs

import com.openbankproject.trading.docs.model.ResourceDoc
import com.openbankproject.trading.docs.registry.ResourceDocRegistry

object TradingResourceDocs {

  private val docs: Seq[ResourceDoc] = Seq.empty

  def registerAll(): Unit = ResourceDocRegistry.registerAll(docs)
}


