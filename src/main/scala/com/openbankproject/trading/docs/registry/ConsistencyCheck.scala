package com.openbankproject.trading.docs.registry

import com.openbankproject.trading.docs.model.ResourceDoc

object ConsistencyCheck {
  def findDuplicateImplementations(docs: Seq[ResourceDoc]): Map[String, Int] =
    docs.groupBy(_.partialFunctionName).view.mapValues(_.size).filter(_._2 > 1).toMap
}


