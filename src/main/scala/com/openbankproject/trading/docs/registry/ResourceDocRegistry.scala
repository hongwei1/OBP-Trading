package com.openbankproject.trading.docs.registry

import com.openbankproject.trading.docs.model.ResourceDoc
import scala.collection.concurrent.TrieMap

object ResourceDocRegistry {
  // Use existential type for storage
  private[this] val byPartialFunctionName: TrieMap[String, ResourceDoc[F] forSome { type F[_] }] = TrieMap.empty

  def register[F[_]](doc: ResourceDoc[F]): Unit = {
    byPartialFunctionName.put(doc.partialFunctionName, doc)
    ()
  }

  def registerAll(docs: Iterable[ResourceDoc[F] forSome { type F[_] }]): Unit = docs.foreach(register(_))

  def get(partialFunctionName: String): Option[ResourceDoc[F] forSome { type F[_] }] = 
    byPartialFunctionName.get(partialFunctionName)

  def all: Vector[ResourceDoc[F] forSome { type F[_] }] = 
    byPartialFunctionName.values.toVector.sortBy(_.partialFunctionName)

  def clear(): Unit = byPartialFunctionName.clear()
}


