package com.openbankproject.trading.http

import org.http4s._
import org.http4s.dsl.Http4sDsl
import cats.effect.Async
import cats.syntax.all._
import com.openbankproject.trading.service._

/** Aggregated HTTP routes (interfaces only, no concrete wiring). */
object Routes {
  def api[F[_]: Async](
    order: OrderService[F],
    matcher: MatchService[F],
    settlement: SettlementService[F],
    funds: FundsService[F]
  ): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}; import dsl._

    HttpRoutes.of[F] {
      // POST /market/orders
      case req @ POST -> Root / "market" / "orders" =>
        Async[F].pure(Response[F](status = Status.NotImplemented))

      // DELETE /market/orders/{id}
      case DELETE -> Root / "market" / "orders" / orderId =>
        Async[F].pure(Response[F](status = Status.NotImplemented))

      // GET /market/orders/{id}
      case GET -> Root / "market" / "orders" / orderId =>
        Async[F].pure(Response[F](status = Status.NotImplemented))

      // POST /market/matches
      case req @ POST -> Root / "market" / "matches" =>
        Async[F].pure(Response[F](status = Status.NotImplemented))

      // POST /market/settlements
      case req @ POST -> Root / "market" / "settlements" =>
        Async[F].pure(Response[F](status = Status.NotImplemented))

      // GET /market/trades/{id}
      case GET -> Root / "market" / "trades" / tradeId =>
        Async[F].pure(Response[F](status = Status.NotImplemented))

      // POST /market/deposits
      case req @ POST -> Root / "market" / "deposits" =>
        Async[F].pure(Response[F](status = Status.NotImplemented))

      // POST /market/withdrawals
      case req @ POST -> Root / "market" / "withdrawals" =>
        Async[F].pure(Response[F](status = Status.NotImplemented))
    }
  }
}


