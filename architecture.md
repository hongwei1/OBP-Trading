# OBP-Trading Architecture

## Overview

OBP-Trading is a Scala-based trading engine built for the Open Bank Project ecosystem. It focuses on offer management, real-time market data, and seamless integration with OBP services.

## Technology Stack

### HTTP Layer
- **http4s-ember-server** - Fast, lightweight HTTP server
- **http4s-dsl** - Functional route DSL for API endpoints  
- **http4s-circe** - JSON support and serialization

### OBP Integration
- **obp-commons** - Shared models and utilities from OBP ecosystem
- **lift-json** - JSON compatibility with existing OBP patterns
- **lift-mapper** - Database patterns and ORM (if needed for compatibility)

### Functional Programming
- **cats-effect** - Effect system for pure functional programming
- **cats-core** - Core functional abstractions and type classes

### Data Layer
- **slick** - Functional relational mapping for database queries
- **circe** - High-performance JSON processing and codec derivation

### Supporting Libraries
- **postgresql** - Primary database driver
- **redis** - Caching and real-time data storage
- **jwt-scala** - JWT token handling for authentication
- **refined** - Compile-time validation and type refinement
- **logback** - Logging framework
- **scalatest** - Testing framework

## Architecture Principles

### 1. Functional First
- Pure functions with referential transparency
- Effect management through cats-effect IO
- Immutable data structures throughout

### 2. Type Safety
- Leverages Scala's type system for compile-time safety
- Refined types for domain validation
- JSON codec derivation with circe

### 3. OBP Ecosystem Integration
- Uses obp-commons for shared models (User, Bank, Account, etc.)
- Compatible with OBP authentication patterns
- Follows OBP API conventions and error handling

### 4. Performance & Scalability
- Non-blocking HTTP server with http4s-ember
- Functional stream processing for real-time data
- Redis caching for hot data access
- Connection pooling for database operations

## System Components

### Core Services
- **Offer Management** - Create, update, cancel trading offers
- **Order Book** - Real-time market depth aggregation
- **Market Data** - Price feeds, trading statistics
- **Authentication** - JWT validation with OBP-OIDC integration
- **User Management** - Profile, preferences, trading history
- **Connector Layer** - Pluggable storage and messaging backends

### Data Storage
- **PostgreSQL** - Persistent storage for offers, trades, users
- **Redis** - Hot data cache, real-time market data, sessions

### Data Storage
- **PostgreSQL** - Persistent storage for trades, audit logs, users
- **Redis** - Default storage for offers, hot data cache, sessions

### External Integrations
- **OBP-API-II** - Account access, transaction execution
- **OBP-OIDC** - Authentication and authorization
- **OBP-Commons** - Shared data models and utilities

## Connector Architecture

### Overview
The connector layer provides pluggable backends for different data storage and messaging patterns, similar to OBP-API but simplified for trading data.

### Supported Connectors

#### Offer Storage Connectors
- **redis** (default) - Fast in-memory storage for active offers
- **rabbitmq** - Send offers to RabbitMQ queues for processing
- **kafka** - Publish offers to Kafka topics for streaming
- **postgres** - Store offers directly in PostgreSQL

#### Trade Storage Connectors  
- **postgres** (default) - Persistent storage for executed trades
- **kafka** - Stream trade events to Kafka topics
- **rabbitmq** - Send trade notifications to RabbitMQ

### Configuration
```hocon
connector {
  offers = "redis"        # redis, rabbitmq, kafka, postgres
  trades = "postgres"     # postgres, kafka, rabbitmq
  
  redis {
    host = "localhost"
    port = 6379
    database = 0
  }
  
  rabbitmq {
    host = "localhost"
    port = 5672
    username = "guest"
    password = "guest"
    offers_exchange = "trading.offers"
    trades_exchange = "trading.trades"
  }
  
  kafka {
    bootstrap_servers = "localhost:9092"
    offers_topic = "trading-offers"
    trades_topic = "trading-trades"
  }
  
  postgres {
    url = "jdbc:postgresql://localhost:5432/obp_trading"
    username = "obp_trading"
    password = "password"
  }
}
```

## Design Patterns

### 1. Connector Pattern
```scala
trait OfferConnector[F[_]] {
  def createOffer(offer: Offer): F[Offer]
  def updateOffer(offer: Offer): F[Offer]
  def getOffer(id: OfferId): F[Option[Offer]]
  def getUserOffers(userId: UserId): F[List[Offer]]
  def cancelOffer(id: OfferId): F[Unit]
}

trait TradeConnector[F[_]] {
  def recordTrade(trade: Trade): F[Trade]
  def getTradeHistory(userId: UserId, limit: Int): F[List[Trade]]
  def getTradeById(id: TradeId): F[Option[Trade]]
}
```

### 2. Connector Factory
```scala
object ConnectorFactory {
  def createOfferConnector[F[_]: Async](config: ConnectorConfig): OfferConnector[F] = {
    config.offerConnector match {
      case "redis" => new RedisOfferConnector[F]
      case "rabbitmq" => new RabbitMQOfferConnector[F]
      case "kafka" => new KafkaOfferConnector[F] 
      case "postgres" => new PostgresOfferConnector[F]
    }
  }
  
  def createTradeConnector[F[_]: Async](config: ConnectorConfig): TradeConnector[F] = {
    config.tradeConnector match {
      case "postgres" => new PostgresTradeConnector[F]
      case "kafka" => new KafkaTradeConnector[F]
      case "rabbitmq" => new RabbitMQTradeConnector[F]
    }
  }
}
```

### 3. Repository Pattern
```scala
trait OfferRepository[F[_]] {
  def create(offer: Offer): F[Offer]
  def findById(id: OfferId): F[Option[Offer]]
  def findByUser(userId: UserId): F[List[Offer]]
}

// Repository delegates to connector
class OfferRepositoryImpl[F[_]](connector: OfferConnector[F]) extends OfferRepository[F] {
  def create(offer: Offer): F[Offer] = connector.createOffer(offer)
  def findById(id: OfferId): F[Option[Offer]] = connector.getOffer(id)
  def findByUser(userId: UserId): F[List[Offer]] = connector.getUserOffers(userId)
}
```

### 4. Service Layer
```scala
trait OfferService[F[_]] {
  def createOffer(request: CreateOfferRequest): F[Either[TradingError, Offer]]
  def cancelOffer(offerId: OfferId, userId: UserId): F[Either[TradingError, Unit]]
}
```

### 5. HTTP Routes
```scala
object OfferRoutes {
  def routes[F[_]: Async](service: OfferService[F]): HttpRoutes[F] = {
    HttpRoutes.of[F] {
      case req @ POST -> Root / "offers" => 
        // Handle offer creation
    }
  }
}
```

## Error Handling

### Functional Error Types
```scala
sealed trait TradingError extends Exception
case class InsufficientFunds(required: Amount, available: Amount) extends TradingError
case class InvalidTradingPair(symbol: String) extends TradingError
case class OfferNotFound(id: OfferId) extends TradingError
```

### HTTP Error Mapping
```scala
def mapToHttpError[F[_]: Applicative](error: TradingError): F[Response[F]] = {
  error match {
    case InsufficientFunds(_, _) => BadRequest("Insufficient funds")
    case InvalidTradingPair(_) => BadRequest("Invalid trading pair")
    case OfferNotFound(_) => NotFound("Offer not found")
  }
}
```

## Configuration

### Application Configuration
- **Typesafe Config** for environment-specific settings
- **PureConfig** for type-safe configuration loading
- Environment variable overrides for deployment

### Database Configuration
- Connection pooling with HikariCP
- Migration management with Flyway
- Read/write splitting capability

## Security

### Authentication Flow
1. Client obtains JWT from OBP-OIDC
2. JWT included in Authorization header
3. Trading service validates JWT with OBP-OIDC JWKS
4. User permissions extracted from JWT claims
5. Role-based access control applied

### Data Protection
- SQL injection prevention through parameterized queries
- Input validation with refined types
- Rate limiting per user/endpoint
- Audit logging for all trading operations

## Deployment

### Containerization
- Docker images with OpenJDK base
- Multi-stage builds for optimized images
- Health checks and readiness probes

### Environment Support
- Development, staging, production configurations
- Database migrations on startup
- Graceful shutdown handling

## Monitoring & Observability

### Metrics
- HTTP request metrics (latency, throughput, errors)
- Database connection pool metrics
- Trading-specific metrics (offers created, trades executed)

### Logging
- Structured logging with logback
- Request correlation IDs
- Performance metrics logging

### Health Checks
- Database connectivity
- Redis connectivity  
- OBP service dependencies

### Connector Implementation Examples

#### Redis Offer Connector
```scala
class RedisOfferConnector[F[_]: Async](redis: RedisCommands[F, String, String]) 
  extends OfferConnector[F] {
  
  def createOffer(offer: Offer): F[Offer] = {
    for {
      json <- Sync[F].delay(offer.asJson.noSpaces)
      _ <- redis.set(s"offer:${offer.id}", json)
      _ <- redis.zadd(s"user:${offer.userId}:offers", offer.createdAt.toEpochMilli, offer.id.toString)
    } yield offer
  }
}
```

#### RabbitMQ Offer Connector  
```scala
class RabbitMQOfferConnector[F[_]: Async](channel: Channel) 
  extends OfferConnector[F] {
  
  def createOffer(offer: Offer): F[Offer] = {
    for {
      json <- Sync[F].delay(offer.asJson.noSpaces)
      _ <- Sync[F].delay {
        channel.basicPublish(
          "trading.offers",
          s"offer.${offer.symbol}",
          null,
          json.getBytes()
        )
      }
    } yield offer
  }
}
```

#### Kafka Offer Connector
```scala
class KafkaOfferConnector[F[_]: Async](producer: KafkaProducer[String, String]) 
  extends OfferConnector[F] {
  
  def createOffer(offer: Offer): F[Offer] = {
    for {
      json <- Sync[F].delay(offer.asJson.noSpaces)
      record = new ProducerRecord("trading-offers", offer.id.toString, json)
      _ <- Async[F].fromFuture(Sync[F].delay(producer.send(record)))
    } yield offer
  }
}
```

### Connector Switching Benefits

1. **Development Flexibility**
   - Use Redis for fast local development
   - Switch to PostgreSQL for data persistence testing
   - Use message queues for integration testing

2. **Deployment Options**
   - High-frequency trading: Redis for offers, Kafka for trades
   - Regulatory compliance: PostgreSQL for both
   - Event-driven architecture: RabbitMQ/Kafka for all data

3. **Performance Tuning**
   - Hot data in Redis, cold data in PostgreSQL
   - Real-time streams via Kafka
   - Async processing via RabbitMQ

### Future Enhancements

### Planned Features
- WebSocket real-time feeds
- Advanced order types (stop-loss, limit orders)  
- Market making capabilities
- Multi-currency trading support
- Advanced analytics and reporting

### Additional Connectors
- **ElasticSearch** - Full-text search and analytics
- **MongoDB** - Document-based offer storage
- **AWS SQS/SNS** - Cloud-based messaging
- **Apache Pulsar** - Advanced streaming platform

### Scalability Improvements
- Event sourcing for audit trails
- CQRS for read/write optimization  
- Distributed caching with Redis Cluster
- Microservice decomposition
- Connector load balancing and failover

---

# Architecture Update — Market Role with Per‑Trade On‑Chain Release (MVP: Limit Orders)

This section aligns OBP‑Trading with the “Market” component from the design diagrams and locks the following decisions:

- Only limit orders in MVP
- Per‑trade on‑chain release to the buyer address, after fiat capture, with N confirmations
- Settlement follows TCC/Saga at business layer: preAuth → captureFiat → on‑chain release → finalize; on failure → compensate

## Domain Model (Conceptual)

- User: userId, kycStatus
- Account
  - FiatAccount: accountId, currency=EUR, available, holding
  - TokenAccount: accountId, currency=OGCR, available, holding, custodialHint(address/escrowSubId)
- Order: orderId, side(BUY|SELL), price, quantity, remaining, status, ownerAccountId, createdAt
- Trade: tradeId, buyOrderId, sellOrderId, price, quantity, status, paymentAuthId?, onChainTxId?, executedAt, settledAt?
- Holding: accountId, asset(EUR/OGCR), amount, reason(order|preauth), state(CREATED|PARTIALLY_RELEASED|RELEASED)
- PaymentAuth: authId, buyerFiatAccountId, amountFiat, state(PREAUTH|CAPTURED|RELEASED), idempotencyKey
- OnChainTx: txId, network, function(release/withdraw), from, to, amount, confirmations, state(PENDING|CONFIRMED|FAILED)

## Modules & Responsibilities

- OrderBook & Matching
  - Price‑level FIFO; partial fills; generates Trade(tradeId)
  - Locks: moves seller OGCR from Available→Holding; buyer EUR from Available→Holding

- Settlement Orchestrator
  - Implements TCC/Saga with idempotency; state machine for Trade
  - Steps: preAuthorizeFiat → captureFiat → releaseTokenOnChain → finalize
  - Compensation: releaseFiatPreauth, revert token holds (if release failed), mark trade FAILED

- EthereumEscrow Connector
  - release(fromSeller, toBuyerAddress, amount, tradeId)
  - Poll N confirmations, reorg‑safe checks, gas strategy (maxFeePerGas, priorityFee)
  - Emits chain.release.requested / chain.release.confirmed / chain.release.failed

- OBPPayments Connector
  - preAuth(buyerFiatAccountId, amountFiat, idempotencyKey)
  - capture(authId) and release(authId)
  - Maps to OBP‑API endpoints and error model; propagates idempotency keys

- Balance Service
  - Maintains Available/Holding for EUR & OGCR; enforces invariants
  - OGCR: Available + Holding ≤ Custodial (escrow mirror)

- Event Bus & Outbox
  - Topics: order.*, trade.*, fiat.*, chain.*
  - Outbox table ensures at‑least‑once with idempotency keys: orderId, tradeId, paymentAuthId, txHash

- Reconciliation
  - L1: Escrow on‑chain total == Σ user(OGCR Available + Holding)
  - L2: Internal ledgers == OBP account balances (EUR/OGCR)
  - L3: Trust Account balance == Σ user(EUR Available + Holding) and matches CBS statements

- Security & Compliance
  - KYC/AML gate for order/withdrawal; sanctions screening
  - Rate limit per user; audit logs; WORM export pipeline

## Settlement Flow (Per‑Trade On‑Chain Release)

1) Match: OrderBook creates Trade(tradeId) with qty Q, price P
2) Try / Prepare:
   - Ensure seller OGCR Holding ≥ Q; buyer EUR Holding ≥ Q*P
3) Confirm step 1 — Fiat capture:
   - OBPPayments.capture(authId) → Mark buyer EUR Holding − amount, seller EUR Available + amount
4) Confirm step 2 — On‑chain release:
   - EthereumEscrow.release(sellerEscrow, buyerAddress, Q, tradeId)
   - Wait N confirmations; update OnChainTx → CONFIRMED
   - Move seller OGCR Holding −Q; buyer OGCR Available +Q
5) Finalize Trade → DONE, emit trade.settled

Failure/Compensation:
- If capture fails: release preAuth, revert any temp holds → Trade FAILED
- If chain release fails (or stuck): auto‑retry with backoff; after TTL: ops alert; if unrecoverable, initiate fiat refund or create compensating trade per policy
- Reorg: if confirmations drop below N (chain reorg), temporarily freeze affected balances, re‑validate, re‑emit events

## API Surface (Conceptual)

- POST /market/orders { side, price, quantity, accountId }
- DELETE /market/orders/{orderId}
- POST /market/matches { orderId, counterOrderId, amount, price } → tradeId
- POST /market/settlements { tradeId }  // idempotent step‑up of TCC
- POST /market/deposits  // watcher intake for on‑chain deposits to escrow
- POST /market/withdrawals { accountId, amount, address }  // chain withdraw
- GET /market/orders/{id}, GET /market/trades/{id}

Notes:
- External clients call Orders; Matches/Settlements may be internal (or admin‑guarded) depending on deployment

## Events & Idempotency

- order.opened/updated/canceled
- trade.created/settled/failed
- fiat.preauthorized/captured/released
- chain.release.requested/confirmed/failed

Every event carries: eventId, idempotencyKey, occurredAt, traceId, actor
Idempotency keys: orderId, tradeId, paymentAuthId, onChainTxHash

## Configuration & Operations

- market.perTradeRelease=true
- chain.network=ethereum; chain.confirmations=N (e.g., 12 mainnet, 3 testnet)
- settlement.timeouts: preauthTTL, captureTTL, releaseTTL
- gas.maxFeePerGas, gas.priorityFee, gas.limitSafetyFactor
- reorg.maxDepth, reorg.freezePolicy

## Failure Modes & Policies

- Payments: capture timeout → release preAuth; alert; rate limit intake
- Chain: nonce too low/gas price too low → dynamic bump; stuck mempool → replacement policy
- Reorg: if tx dropped, re‑submit; if conflicting state observed, freeze and manual runbook
- Idempotency: all mutating endpoints require Idempotency‑Key header

## Reconciliation & Alerts

- Daily L1/L2/L3 checks; discrepancy thresholds with pager alerts
- On discrepancy: freeze related orders/withdrawals; generate incident ticket with evidence bundle (events, postings, txs)

## MVP Boundaries & Risks / Trade‑offs

In‑scope
- Limit orders; per‑trade on‑chain release; EUR+OGCR only; manual KYC gate; daily reconciliation

Out‑of‑scope (future)
- Market orders; advanced order types; batch settlement; multi‑asset routing; cross‑chain bridges

Risks & Trade‑offs
- Gas & latency overhead for per‑trade release; operational complexity on reorgs
- Strong auditability and non‑repudiation benefits; simpler user mental model

## Detailed Specifications (fulfilling the plan To‑dos)

### A. Domain Types (fields)

- User
  - userId: String
  - kycStatus: Pending|Approved|Rejected
  - createdAt: Instant

- FiatAccount (EUR)
  - accountId: String, userId: String, currency: "EUR"
  - available: BigDecimal, holding: BigDecimal
  - metadata: Map[String,String]

- TokenAccount (OGCR)
  - accountId: String, userId: String, currency: "OGCR"
  - available: BigDecimal, holding: BigDecimal
  - chainAddress: String, escrowSubId: Option[String]
  - metadata: Map[String,String]

- Order
  - orderId: String, side: BUY|SELL, price: BigDecimal, quantity: BigDecimal
  - remaining: BigDecimal, status: NEW|OPEN|PARTIALLY_FILLED|FILLED|CANCELED|EXPIRED
  - ownerAccountId: String, createdAt: Instant, expiresAt: Option[Instant]
  - idempotencyKey: String

- Trade
  - tradeId: String, buyOrderId: String, sellOrderId: String
  - price: BigDecimal, quantity: BigDecimal
  - status: INIT|TRY_AUTH_FIAT|TOKEN_INTERNAL_MOVE|CAPTURE_FIAT|ONCHAIN_RELEASE|DONE|FAILED
  - paymentAuthId: Option[String], onChainTxId: Option[String]
  - executedAt: Instant, settledAt: Option[Instant]

- Holding
  - accountId: String, asset: EUR|OGCR
  - amount: BigDecimal, reason: ORDER|PREAUTH
  - state: CREATED|PARTIALLY_RELEASED|RELEASED
  - relatedId: orderId or authId

- PaymentAuth
  - authId: String, buyerFiatAccountId: String, amountFiat: BigDecimal
  - state: PREAUTH|CAPTURED|RELEASED
  - idempotencyKey: String, createdAt: Instant, updatedAt: Instant

- OnChainTx
  - txId: String, network: String (ethereum)
  - function: RELEASE|WITHDRAW, from: String, to: String, amount: BigDecimal
  - confirmations: Int, requiredConfirmations: Int
  - state: PENDING|CONFIRMED|FAILED
  - hash: String, nonce: Long, gasUsed: Option[Long], error: Option[String]

### B. API Surface (requests/responses — conceptual)

- POST /market/orders
  - Request: { side, price, quantity, accountId, idempotencyKey }
  - Response: { orderId, status, remaining }

- DELETE /market/orders/{orderId}
  - Response: { orderId, status: CANCELED }

- POST /market/matches
  - Request: { orderId, counterOrderId, amount, price }
  - Response: { tradeId, status: INIT }

- POST /market/settlements
  - Request: { tradeId, step? } // step optional; server advances idempotently
  - Response: { tradeId, status }

- POST /market/deposits
  - Request: { txHash, from, to, amount, confirmations }
  - Response: { credited: Boolean, externalId: txHash }

- POST /market/withdrawals
  - Request: { accountId, amount, address, idempotencyKey }
  - Response: { onChainTxId, state }

- GET /market/orders/{id} → { ...Order }
- GET /market/trades/{id} → { ...Trade, paymentAuth?, onChainTx? }

Notes:
- All mutating requests require Idempotency‑Key header.

### C. Events, Idempotency & Outbox

Topics and sample payload keys:
- order.opened { orderId, side, price, quantity, ownerAccountId, traceId }
- order.updated/canceled { orderId, status, remaining, traceId }
- trade.created { tradeId, buyOrderId, sellOrderId, qty, price, traceId }
- fiat.preauthorized/captured/released { authId, tradeId, amount, traceId }
- chain.release.requested/confirmed/failed { tradeId, txHash, from, to, amount, confirmations, traceId }

Outbox table (concept):
- id(UUID), topic, key(idempotencyKey), payload(JSON), status(PENDING|SENT|FAILED), attempts(Int), nextAttemptAt(Instant), createdAt
- Retry policy: exponential backoff (e.g., 1s, 5s, 30s, 5m, 30m), maxAttempts=20; DLQ after maxAttempts

Idempotency keys:
- orderId, tradeId, paymentAuthId, txHash; server dedups on key within TTL window

### D. Reconciliation (L1/L2/L3) & Freeze Policy

Schedule:
- Daily at 02:00 UTC; ad‑hoc on incident

Checks:
- L1: sum(OGCR Available + Holding) == escrow on‑chain total (± buffer)
- L2: internal ledgers (EUR/OGCR) == OBP accounts snapshot
- L3: trust account balance == sum(EUR Available + Holding) and equals CBS statement

On discrepancy:
- if |delta| > threshold: freeze withdrawals and new orders for involved accounts; raise P1 alert; create incident report with evidence (events, postings, txs)

### E. Security & Compliance

- KYC/AML gating: registration → KYC Approved before order/withdrawal
- Sanctions screening on user and destination addresses (withdrawals)
- Rate limits: default 60 req/min/user; settlement steps 10/min/trade
- Audit: append‑only logs with traceId, subjectId, actor, ip, userAgent; WORM export daily

### F. Configuration (defaults)

```hocon
market.perTradeRelease = true
chain.network = "ethereum"
chain.confirmations = 12         # 3 on testnet
settlement.timeout.preauth = 10m
settlement.timeout.capture = 5m
settlement.timeout.release = 30m
gas.maxFeePerGas = "auto"        # or numeric gwei
gas.priorityFee = "auto"
gas.limitSafetyFactor = 1.2
reorg.maxDepth = 3
reorg.freezePolicy = "freeze-affected-accounts"
```

### G. Failure Modes → Compensation Mapping

- preAuth timeout → cancel trade, release holds, notify user
- capture failed → release preAuth, revert holds, trade FAILED
- chain tx stuck → bump gas & retry; after TTL, alert + manual runbook
- chain tx failed/reverted → refund/counter‑post per policy; trade FAILED
- reorg reduces confirmations < N → temporary freeze, revalidate, re‑emit

### H. MVP Constraints & Non‑Goals (explicit)

In‑scope: limit orders; per‑trade release; single asset pair (EUR↔OGCR); manual KYC; daily reconciliation; idempotent APIs

Out‑of‑scope: market orders; batch releases; multi‑asset routing; cross‑chain bridges; automated KYC providers; intraday full auto‑recon
