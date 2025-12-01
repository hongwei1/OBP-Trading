package com.openbankproject.trading.service

import com.openbankproject.trading.http._
import cats.effect.IO

trait OrderService {
  def createOrder(req: CreateOrderRequest): IO[Either[ErrorResponse, CreateOrderResponse]]
  def cancelOrder(orderId: String): IO[Either[ErrorResponse, CancelOrderResponse]]
  def getOrder(orderId: String): IO[Either[ErrorResponse, OrderView]]
}

trait OfferService {
  def createOffer(req: CreateOfferRequest): IO[Either[ErrorResponse, CreateOfferResponse]]
  def cancelOffer(offerId: String): IO[Either[ErrorResponse, CancelOfferResponse]]
  def getOffer(offerId: String): IO[Either[ErrorResponse, OfferView]]
}

trait MatchService {
  def createMatch(req: MatchRequest): IO[Either[ErrorResponse, CreateMatchResponse]]
}

trait SettlementService {
  def settle(req: SettlementRequest): IO[Either[ErrorResponse, SettlementResponse]]
  def getTrade(tradeId: String): IO[Either[ErrorResponse, TradeView]]
}

trait FundsService {
  def notifyDeposit(req: DepositNotification): IO[Either[ErrorResponse, DepositResponse]]
  def requestWithdrawal(req: WithdrawalRequest): IO[Either[ErrorResponse, WithdrawalResponse]]
}


