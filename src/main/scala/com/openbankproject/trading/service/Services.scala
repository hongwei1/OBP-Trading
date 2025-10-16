package com.openbankproject.trading.service

import com.openbankproject.trading.http._

trait OrderService[F[_]] {
  def createOrder(req: CreateOrderRequest): F[Either[ErrorResponse, CreateOrderResponse]]
  def cancelOrder(orderId: String): F[Either[ErrorResponse, CancelOrderResponse]]
  def getOrder(orderId: String): F[Either[ErrorResponse, OrderView]]
}

trait OfferService[F[_]] {
  def createOffer(req: CreateOfferRequest): F[Either[ErrorResponse, CreateOfferResponse]]
  def cancelOffer(offerId: String): F[Either[ErrorResponse, CancelOfferResponse]]
  def getOffer(offerId: String): F[Either[ErrorResponse, OfferView]]
}

trait MatchService[F[_]] {
  def createMatch(req: MatchRequest): F[Either[ErrorResponse, CreateMatchResponse]]
}

trait SettlementService[F[_]] {
  def settle(req: SettlementRequest): F[Either[ErrorResponse, SettlementResponse]]
  def getTrade(tradeId: String): F[Either[ErrorResponse, TradeView]]
}

trait FundsService[F[_]] {
  def notifyDeposit(req: DepositNotification): F[Either[ErrorResponse, DepositResponse]]
  def requestWithdrawal(req: WithdrawalRequest): F[Either[ErrorResponse, WithdrawalResponse]]
}


