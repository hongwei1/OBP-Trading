package com.openbankproject.trading.settlement

import cats.effect.Sync

import java.util.concurrent.ConcurrentHashMap

/** In-memory implementation for development and tests.
  * Semantics: returns true if key already present (duplicate), otherwise stores and returns false.
  */
class InMemoryIdempotencyStore[F[_]: Sync] extends IdempotencyStore[F] {
  private val keys = new ConcurrentHashMap[String, java.lang.Boolean]()

  override def checkAndPut(key: String): F[Boolean] = Sync[F].delay {
    val prev = keys.putIfAbsent(key, java.lang.Boolean.TRUE)
    prev != null // true => duplicate, false => stored now
  }
}


