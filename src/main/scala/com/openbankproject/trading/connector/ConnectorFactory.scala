/**
 * Copyright (c) TESOBE 2025. All rights reserved.
 * 
 * This file is part of the Open Bank Project.
 * 
 * Licensed under the GNU Affero General Public License v3.0 (AGPL-3.0).
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.html
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 */

package com.openbankproject.trading.connector

import cats.effect.kernel.Async
import cats.syntax.all._
import com.typesafe.config.Config
import dev.profunktor.redis4cats.Redis
import slick.jdbc.JdbcBackend.Database
import dev.profunktor.redis4cats.effect.Log.Stdout._

/**
 * Factory for creating connector instances based on configuration
 * Supports Redis, PostgreSQL, RabbitMQ, and Kafka connectors
 */
class DefaultConnectorFactory[F[_]: Async] extends ConnectorFactory[F] {

  /**
   * Creates an offer connector based on configuration
   */
  def createOfferConnector(config: ConnectorConfig): F[Either[ConnectorError, OfferConnector[F]]] = {
    config.connectorType.toLowerCase match {
      case Connector.REDIS =>
        createRedisOfferConnector(config)
      case Connector.POSTGRES =>
        createPostgresOfferConnector(config)
      case Connector.KAFKA =>
        for {
          bs <- Async[F].fromEither(Connector.getRequiredProperty(config, "bootstrap_servers"))
          topic = Connector.getProperty(config, "offers_topic", "trading-offers")
          c <- KafkaOfferConnector.inMemory[F](bs, topic).map(r => Right(r): Either[ConnectorError, OfferConnector[F]])
        } yield c
      case Connector.RABBITMQ =>
        for {
          host <- Async[F].fromEither(Connector.getRequiredProperty(config, "host"))
          port <- Async[F].fromEither(parsePort(Connector.getProperty(config, "port", "5672")))
          user <- Async[F].fromEither(Connector.getRequiredProperty(config, "username"))
          pass <- Async[F].fromEither(Connector.getRequiredProperty(config, "password"))
          exchange = Connector.getProperty(config, "offers_exchange", "trading.offers")
          c <- RabbitMQOfferConnector.inMemory[F](host, port, user, pass, exchange).map(r => Right(r): Either[ConnectorError, OfferConnector[F]])
        } yield c
      // All supported cases are handled above
      case unknown =>
        Async[F].pure(Left(ConfigurationError(s"Unknown offer connector type: $unknown")))
    }
  }

  /**
   * Creates a trade connector based on configuration
   */
  def createTradeConnector(config: ConnectorConfig): F[Either[ConnectorError, TradeConnector[F]]] = {
    config.connectorType.toLowerCase match {
      case Connector.POSTGRES =>
        createPostgresTradeConnector(config)
      case Connector.KAFKA =>
        for {
          bs <- Async[F].fromEither(Connector.getRequiredProperty(config, "bootstrap_servers"))
          topic = Connector.getProperty(config, "trades_topic", "trading-trades")
          c <- KafkaTradeConnector.inMemory[F](bs, topic).map(r => Right(r): Either[ConnectorError, TradeConnector[F]])
        } yield c
      case Connector.RABBITMQ =>
        for {
          host <- Async[F].fromEither(Connector.getRequiredProperty(config, "host"))
          port <- Async[F].fromEither(parsePort(Connector.getProperty(config, "port", "5672")))
          user <- Async[F].fromEither(Connector.getRequiredProperty(config, "username"))
          pass <- Async[F].fromEither(Connector.getRequiredProperty(config, "password"))
          exchange = Connector.getProperty(config, "trades_exchange", "trading.trades")
          c <- RabbitMQTradeConnector.inMemory[F](host, port, user, pass, exchange).map(r => Right(r): Either[ConnectorError, TradeConnector[F]])
        } yield c
      case _ => Async[F].pure(Left(ConfigurationError(s"Trade connector '${config.connectorType}' not supported in this build")))
    }
  }

  /**
   * Creates a user connector based on configuration
   */
  def createUserConnector(config: ConnectorConfig): F[Either[ConnectorError, UserConnector[F]]] = {
    config.connectorType.toLowerCase match {
      case "obp-api" =>
        for {
          base <- Async[F].fromEither(Connector.getRequiredProperty(config, "base_url"))
          cid <- Async[F].fromEither(Connector.getRequiredProperty(config, "client_id"))
          csec <- Async[F].fromEither(Connector.getRequiredProperty(config, "client_secret"))
          c <- ObpApiUserConnector.inMemory[F](base, cid, csec).map(r => Right(r): Either[ConnectorError, UserConnector[F]])
        } yield c
      case _ => Async[F].pure(Left(ConfigurationError(s"User connector '${config.connectorType}' not supported in this build")))
    }
  }

  // Redis connector factory methods
  private def createRedisOfferConnector(config: ConnectorConfig): F[Either[ConnectorError, OfferConnector[F]]] = {
    for {
      host <- Async[F].fromEither(Connector.getRequiredProperty(config, "host"))
      port <- Async[F].fromEither(parsePort(config.properties.getOrElse("port", "6379")))
      database = config.properties.getOrElse("database", "0").toInt
      password = config.properties.get("password")
      
      redisUri = buildRedisUri(host, port, database, password)
      
      result <- Redis[F].utf8(redisUri).use { redis =>
        val connector = new RedisOfferConnector[F](redis)
        connector.healthCheck.map { status =>
          if (status.healthy) Right(connector)
          else Left(ConnectionError("Redis connection unhealthy"))
        }
      }.handleError(error => Left(ConnectionError(s"Failed to create Redis connector: ${error.getMessage}", Some(error))))
      
    } yield result
  }

  private def createPostgresOfferConnector(config: ConnectorConfig): F[Either[ConnectorError, OfferConnector[F]]] = {
    Connector.getRequiredProperty(config, "url") match {
      case Left(err) => Async[F].pure(Left(err))
      case Right(url) =>
        val user = Connector.getProperty(config, "username", "")
        val pass = Connector.getProperty(config, "password", "")
        for {
          db <- Async[F].delay(Database.forURL(url, user, pass))
          conn <- PostgresOfferConnector.inMemory[F](db).map(c => Right(c): Either[ConnectorError, OfferConnector[F]])
        } yield conn
    }
  }

  private def createPostgresTradeConnector(config: ConnectorConfig): F[Either[ConnectorError, TradeConnector[F]]] = {
    Connector.getRequiredProperty(config, "url") match {
      case Left(err) => Async[F].pure(Left(err))
      case Right(url) =>
        val user = Connector.getProperty(config, "username", "")
        val pass = Connector.getProperty(config, "password", "")
        for {
          db <- Async[F].delay(Database.forURL(url, user, pass))
          conn <- PostgresTradeConnector.inMemory[F](db).map(c => Right(c): Either[ConnectorError, TradeConnector[F]])
        } yield conn
    }
  }

  // Helper methods
  private def parsePort(portStr: String): Either[ConnectorError, Int] = {
    try {
      Right(portStr.toInt)
    } catch {
      case _: NumberFormatException => 
        Left(ConfigurationError(s"Invalid port number: $portStr"))
    }
  }

  private def buildRedisUri(host: String, port: Int, database: Int, password: Option[String]): String = {
    val auth = password.map(p => s":$p@").getOrElse("")
    s"redis://$auth$host:$port/$database"
  }
}

object ConnectorFactories {
  
  /**
   * Creates a connector factory instance
   */
  def default[F[_]: Async]: ConnectorFactory[F] = new DefaultConnectorFactory[F]
  
  /**
   * Creates connector configuration from Typesafe Config
   */
  def configFromTypesafeConfig(config: Config, section: String): Either[ConnectorError, ConnectorConfig] = {
    try {
      val sectionConfig = config.getConfig(section)
      val connectorType = sectionConfig.getString("type")
      
      import scala.jdk.CollectionConverters._
      val properties = sectionConfig.entrySet().asScala.map { entry =>
        entry.getKey -> entry.getValue.unwrapped().toString
      }.toMap
      
      Right(ConnectorConfig(connectorType, properties))
      
    } catch {
      case ex: Exception => 
        Left(ConfigurationError(s"Failed to parse configuration section '$section': ${ex.getMessage}"))
    }
  }
  
  /**
   * Creates default configurations for common setups
   */
  object Defaults {
    
    def redisOfferConnector(host: String = "localhost", port: Int = 6379, database: Int = 0): ConnectorConfig = {
      ConnectorConfig(
        connectorType = Connector.REDIS,
        properties = Map(
          "host" -> host,
          "port" -> port.toString,
          "database" -> database.toString
        )
      )
    }
  }
}