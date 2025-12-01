package com.openbankproject.trading.docs.registry

import com.openbankproject.trading.docs.model.ResourceDoc
import scala.collection.concurrent.TrieMap

object ResourceDocRegistry {
  private[this] val byPartialFunctionName: TrieMap[String, ResourceDoc] = TrieMap.empty

  def register(doc: ResourceDoc): Unit = {
    byPartialFunctionName.put(doc.partialFunctionName, doc)
    ()
  }

  def registerAll(docs: Iterable[ResourceDoc]): Unit = docs.foreach(register)

  def get(partialFunctionName: String): Option[ResourceDoc] = 
    byPartialFunctionName.get(partialFunctionName)

  def all: Vector[ResourceDoc] = 
    byPartialFunctionName.values.toVector.sortBy(_.partialFunctionName)

  def clear(): Unit = byPartialFunctionName.clear()
}


