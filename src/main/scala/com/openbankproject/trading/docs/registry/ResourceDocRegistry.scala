package com.openbankproject.trading.docs.registry

import com.openbankproject.trading.docs.model.ResourceDoc
import scala.collection.concurrent.TrieMap

object ResourceDocRegistry {
  private[this] val byOperationId: TrieMap[String, ResourceDoc] = TrieMap.empty

  def register(doc: ResourceDoc): Unit = {
    byOperationId.put(doc.operationId, doc)
    ()
  }

  def registerAll(docs: Iterable[ResourceDoc]): Unit = docs.foreach(register)

  def get(operationId: String): Option[ResourceDoc] = byOperationId.get(operationId)

  def all: Vector[ResourceDoc] = byOperationId.values.toVector.sortBy(_.operationId)

  def clear(): Unit = byOperationId.clear()
}


