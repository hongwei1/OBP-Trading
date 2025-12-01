package com.openbankproject.trading.http

import cats.effect.{IO, IOApp}
import com.openbankproject.trading.service._
import org.http4s.server.Router
import org.http4s.implicits._
import org.http4s.ember.server.EmberServerBuilder
import com.comcast.ip4s._
import com.typesafe.config.ConfigFactory

/**
  * Minimal http4s server that mounts Routes.api with placeholder services.
  * Replace the placeholder services with real implementations when ready.
  */
object HttpServerMain extends IOApp.Simple {

  private val orderServiceIO: IO[OrderService] = InMemoryOrderService.create()
  private val offerServiceIO: IO[OfferService] = InMemoryOfferService.create()

  private val matchService: MatchService = new MatchService {
    def createMatch(req: MatchRequest) = IO.pure(Left(ErrorResponse(ErrorCodes.NOT_IMPLEMENTED, "createMatch not implemented")))
  }

  private val settlementService: SettlementService = new SettlementService {
    def settle(req: SettlementRequest) = IO.pure(Left(ErrorResponse(ErrorCodes.NOT_IMPLEMENTED, "settle not implemented")))
    def getTrade(tradeId: String) = IO.pure(Left(ErrorResponse(ErrorCodes.NOT_IMPLEMENTED, "getTrade not implemented")))
  }

  private val fundsService: FundsService = new FundsService {
    def notifyDeposit(req: DepositNotification) = IO.pure(Left(ErrorResponse(ErrorCodes.NOT_IMPLEMENTED, "notifyDeposit not implemented")))
    def requestWithdrawal(req: WithdrawalRequest) = IO.pure(Left(ErrorResponse(ErrorCodes.NOT_IMPLEMENTED, "requestWithdrawal not implemented")))
  }

  override def run: IO[Unit] =
    for {
      config <- IO(ConfigFactory.load())
      serverHost = config.getString("server.host")
      serverPort = config.getString("server.port").toInt
      offerService <- offerServiceIO
      orderService <- orderServiceIO
      apiRoutes = Routes.api(orderService, offerService, matchService, settlementService, fundsService)
      httpApp   = Router("/" -> apiRoutes).orNotFound
      host <- IO.fromOption(Host.fromString(serverHost))(new IllegalArgumentException(s"Invalid host: $serverHost"))
      port <- IO.fromOption(Port.fromInt(serverPort))(new IllegalArgumentException(s"Invalid port: $serverPort"))
      _ <- EmberServerBuilder
        .default[IO]
        .withHost(host)
        .withPort(port)
        .withHttpApp(httpApp)
        .build
        .useForever
    } yield ()
}


