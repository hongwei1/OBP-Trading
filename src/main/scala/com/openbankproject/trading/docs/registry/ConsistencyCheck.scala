package com.openbankproject.trading.docs.registry

import com.openbankproject.trading.docs.model.ResourceDoc

object ConsistencyCheck {
  def findDuplicateOperationIds(docs: Seq[ResourceDoc]): Map[String, Int] =
    docs.groupBy(_.operationId).view.mapValues(_.size).filter(_._2 > 1).toMap
}


