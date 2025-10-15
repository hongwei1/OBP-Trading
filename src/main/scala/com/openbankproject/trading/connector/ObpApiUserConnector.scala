package com.openbankproject.trading.connector

import cats.effect.kernel.Async
import cats.effect.kernel.Ref
import cats.syntax.all._
import com.openbankproject.trading.model._
import java.time.Instant

/**
 * Minimal OBP-API UserConnector placeholder (in-memory store for users/accounts/profile).
 */
class ObpApiUserConnector[F[_]: Async](baseUrl: String, clientId: String, clientSecret: String, state: Ref[F, Map[UserId, (UserSummary, List[AccountSummary], Option[TradingProfile])]]) extends UserConnector[F] {
  override def getConnectorInfo: ConnectorInfo = ConnectorInfo(
    name = "OBP API User Connector",
    version = "0.1",
    description = s"OBP API at $baseUrl (in-memory store)",
    supportedOperations = List("getUser", "getUserByEmail", "getUserAccounts", "getUserTradingProfile", "updateTradingProfile", "canUserTrade", "getUserTradingPermissions"),
    configuration = Map("baseUrl" -> baseUrl, "clientId" -> clientId)
  )

  override def healthCheck = Async[F].pure(ConnectorStatus(true, 0L, Instant.now()))

  override def getUser(userId: UserId) = state.get.map { m => Right(m.get(userId).map(_._1)): Either[ConnectorError, Option[UserSummary]] }
  override def getUserByEmail(email: String) = state.get.map { m => Right(m.values.map(_._1).find(_.email.contains(email))): Either[ConnectorError, Option[UserSummary]] }
  override def getUserAccounts(userId: UserId) = state.get.map { m => Right(m.get(userId).map(_._2).getOrElse(Nil)): Either[ConnectorError, List[AccountSummary]] }
  override def getUserTradingProfile(userId: UserId) = state.get.map { m => Right(m.get(userId).flatMap(_._3)): Either[ConnectorError, Option[TradingProfile]] }
  override def updateTradingProfile(userId: UserId, profile: TradingProfile) = state.modify { m =>
    m.get(userId) match {
      case Some((u, accs, _)) => (m.updated(userId, (u, accs, Some(profile.copy(updatedAt = Instant.now())))), Right(profile))
      case None => (m, Left(NotFoundError(s"User $userId not found")))
    }
  }
  override def canUserTrade(userId: UserId, symbol: TradingSymbol) = state.get.map { m =>
    val ok = m.get(userId)
      .flatMap { tup => tup._3 }
      .exists { profile =>
        profile.permissions.exists { perm =>
          perm.canTrade(OfferType.Buy) || perm.canTrade(OfferType.Sell)
        }
      }
    Right(ok): Either[ConnectorError, Boolean]
  }
  override def getUserTradingPermissions(userId: UserId) = state.get.map { m =>
    val perms = m.get(userId).flatMap(_._3).map(_.permissions).getOrElse(Nil)
    Right(perms): Either[ConnectorError, List[TradingPermission]]
  }
}

object ObpApiUserConnector {
  def inMemory[F[_]: Async](baseUrl: String, clientId: String, clientSecret: String): F[ObpApiUserConnector[F]] =
    Ref.of[F, Map[UserId, (UserSummary, List[AccountSummary], Option[TradingProfile])]](Map.empty).map(ref => new ObpApiUserConnector[F](baseUrl, clientId, clientSecret, ref))
}


