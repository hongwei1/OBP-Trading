package com.openbankproject.trading.settlement

sealed trait SettlementError extends Throwable { self: Throwable => }

object SettlementError {
  final case object MissingAddresses extends Exception("Missing buyer or seller escrow address") with SettlementError
  final case class DuplicateOperation(op: String, key: String) extends Exception(s"Duplicate $op for key=$key") with SettlementError
  final case class PaymentFailure(msg: String) extends Exception(msg) with SettlementError
  final case class ChainFailure(msg: String) extends Exception(msg) with SettlementError
}


