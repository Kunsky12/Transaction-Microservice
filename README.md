# Transaction-Microservice

A Spring Boot microservice for **com.mekheainteractive** that handles in-game currency transfers for **Kun Khmer Mobile**. It processes peer-to-peer transactions between players, ensures idempotency, persists a double-entry ledger to PostgreSQL, caches state in Redis, publishes events to Kafka, and pushes real-time notifications to recipients via the WebSocket server.

---

## How It Works

```
Game Client
    │
    │  POST /api/transfer  { receiverId, amount, currency, idempotencyKey, ... }
    │  Authorization: Bearer <JWT>
    ▼
TransactionController
    │
    ├─► Validate JWT → extract senderId
    ├─► Reject if senderId == receiverId or amount <= 0
    ▼
LedgerService
    │
    ├─► Acquire Redis distributed lock  (key: "lock:<senderId>", TTL: 5s)
    │       └─ Reject if lock already held (concurrent transfer in progress)
    │
    ├─► Idempotency check (DB lookup by senderId + idempotencyKey)
    │       └─ If found → return existing TransactionDTO immediately
    │
    ├─► Fetch sender balance from PlayFab  (VirtualCurrency: "RP")
    │       └─ Reject if balance < amount
    │
    ├─► Debit sender in PlayFab  (/SubtractUserVirtualCurrency)
    │
    ├─► Save PENDING ledger rows (debit + credit entries)
    │
    ├─► Credit receiver in PlayFab  (/AddUserVirtualCurrency)
    │       └─ On failure → refund sender, mark both rows FAILED
    │
    ├─► Mark both ledger rows SUCCESS
    │
    ├─► Publish notification (async thread → POST ${WEBSOCKET_URL})
    │       └─ Uses internal service JWT (role: service)
    │
    └─► Release Redis lock
            │
            ▼
    Return TransactionDTO to client
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 4.0.2 |
| Language | Java 17 |
| HTTP Client | Spring `RestClient` + `RestTemplate` |
| Auth | JJWT 0.13.0 (HS256) |
| ORM | Spring Data JPA + Hibernate |
| Database | PostgreSQL |
| Cache / Idempotency | Redis (Jedis 5.0.1) |
| Messaging | Apache Kafka |
| Notifications | Telegram Bot API 6.9.7.1 |
| Utilities | Lombok |

---

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL instance
- Redis instance
- Kafka broker

### Installation

```bash
git clone https://github.com/your-org/Transaction-Microservice.git
cd Transaction-Microservice
mvn install
```

### Environment Variables

| Variable | Description |
|---|---|
| `PLAYFAB_TITLE_ID` | PlayFab title ID |
| `PLAYFAB_SECRET_KEY` | PlayFab server secret key |
| `JWT_SECRET_KEY` | Shared JWT signing secret (must match `authenticator-service`) |
| `DB_URL` | PostgreSQL JDBC URL (e.g. `jdbc:postgresql://localhost:5432/transactions`) |
| `DB_USER` | PostgreSQL username |
| `DB_PASSWORD` | PostgreSQL password |
| `REDIS_HOST` | Redis host (default: `redis`) |
| `REDIS_PORT` | Redis port (default: `6379`) |
| `WEBSOCKET_URL` | Base URL of the Websocket notification endpoint |
| `KAFKA_BROKERS` | Kafka bootstrap servers (default: `kafka:9092`) |

### Running

```bash
mvn spring-boot:run
```

Or build and run the JAR:

```bash
mvn package
java -jar target/Transaction-Microservice-0.0.1-SNAPSHOT.jar
```

The service starts on **port 8080**.

---

## API Reference

### `POST /api/transfer`

Transfers in-game currency from the authenticated player to another player.

**Access:** Requires a valid JWT issued by `authenticator-service`

**Headers:**
```
Authorization: Bearer <JWT>
```

**Request body:**
```json
{
  "idempotencyKey": "unique-client-key",
  "receiverId":     "receiver-playfab-id",
  "amount":         100,
  "currency":       "RP",
  "facebookId":     "optional-facebook-id",
  "senderName":     "Hero123"
}
```

**Response — `TransactionDTO`:**
```json
{
  "referenceId":     "txn-reference-id",
  "facebookId":      "optional-facebook-id",
  "senderName":      "Hero123",
  "amount":          100,
  "currency":        "RP",
  "transactionDate": "2025-01-01T12:00:00",
  "status":          "SUCCESS"
}
```

**Validation:**
- `amount` must be a positive integer
- `receiverId` and `currency` are required
- `senderId` is extracted from the JWT — never trusted from the request body

---

## Transaction Entity

Each transfer creates **two ledger rows** in the `transaction_entity` table — one for the sender (debit) and one for the receiver (credit).

| Column | Type | Description |
|---|---|---|
| `id` | BIGINT | Auto-increment primary key |
| `transactionId` | UUID | Unique transaction identifier |
| `referenceId` | VARCHAR | Human-readable reference |
| `idempotencyKey` | VARCHAR | Client-supplied key for safe retries |
| `senderId` | VARCHAR | Sender's PlayFab ID |
| `receiverId` | VARCHAR | Receiver's PlayFab ID |
| `senderName` | VARCHAR | Sender's display name |
| `facebookId` | VARCHAR | Optional Facebook ID |
| `amount` | INT | Negative = debit, positive = credit |
| `type` | ENUM | `SENDER` or `RECEIVER` |
| `balanceAfter` | INT | Player's balance after transaction |
| `currency` | VARCHAR | Currency code |
| `status` | VARCHAR | Transaction status |
| `transactionDate` | TIMESTAMP | Auto-set on persist |

> **Unique constraint:** `(senderId, idempotencyKey)` — prevents duplicate transactions on retry.

---

## Idempotency

Clients must supply a unique `idempotencyKey` per transfer attempt. Before any PlayFab calls are made, `LedgerService` queries the database for an existing row matching `(senderId, idempotencyKey)`. If found, the original `TransactionDTO` is returned immediately — no money moves, no duplicate ledger entries.

The unique constraint on `(senderId, idempotencyKey)` at the database level provides a second safety net against race conditions.

---

## Distributed Locking (Redis)

To prevent concurrent transfers from the same sender (e.g. double-tap), `LedgerService` acquires a Redis lock before processing:

- **Key:** `lock:<senderId>`
- **TTL:** 5 seconds (auto-released on expiry)
- **Implementation:** `SET NX EX` via `StringRedisTemplate.setIfAbsent()`
- If the lock cannot be acquired, the request is rejected immediately with an error

The lock is always released in a `finally` block regardless of success or failure.

---

## Compensation / Rollback

If the sender debit succeeds but the receiver credit fails, the service automatically refunds the sender (`/AddUserVirtualCurrency`) and marks both ledger rows as `FAILED` before throwing. This ensures no funds are lost due to a partial failure.

---

## JWT — Service Token

In addition to validating player JWTs, this service generates its own **internal service token** for authenticating outbound calls to the Websocket `/notify` endpoint:

```
sub:  "notification-service"
role: "service"
exp:  issued-at + 12 hours
alg:  HS256
```

---

## Notifications

After a successful transfer, `EventPublisherImpl` fires an **async thread** (so notification failures never affect the transfer result) that POSTs to `${WEBSOCKET_URL}` — the Websocket `/notify` endpoint:

```json
{
  "receiverId":      "playfab-id",
  "facebookId":      "...",
  "senderName":      "Hero123",
  "referenceId":     "uuid",
  "transactionDate": "2025-01-01T12:00:00",
  "currency":        "RP",
  "amount":          100,
  "message":         "You received 100 RP from Hero123"
}
```

The request is authenticated with a short-lived internal service JWT (`role: service`, 12h expiry). Timeouts are set to 3s connect / 5s read to avoid blocking.

---

## PlayFab Virtual Currency

`PlayFabWalletService` wraps three PlayFab Server API calls:

| Method | PlayFab endpoint | Purpose |
|---|---|---|
| `getSenderCurrency()` | `/GetUserInventory` | Fetch sender's `RP` balance |
| `subtractCurrency()` | `/SubtractUserVirtualCurrency` | Debit sender |
| `addCurrency()` | `/AddUserVirtualCurrency` | Credit receiver |

All calls use `X-SecretKey` header authentication and return a `CurrencyResult` wrapper with `success` flag and updated `balance`.

---

## Project Structure

```
src/main/java/com/mekheainteractive/Transaction_Microservice/
├── Auth/
│   └── JwtAuth.java                      # JWT validation, extraction & service token generation
├── Config/
│   └── KafkaConfig.java                  # Kafka producer factory & template
├── Controller/
│   └── TransactionController.java        # POST /api/transfer
├── DTO/
│   └── TransactionDTO.java               # Transfer response shape
├── Entity/
│   └── TransactionEntity.java            # JPA double-entry ledger entity
├── Event/
│   ├── EventPublisher.java               # Interface
│   └── EventPublisherImpl.java           # Async HTTP notification to Websocket
├── Repository/
│   └── LedgerRepository.java             # JPA repository + idempotency query
└── Service/
    ├── LedgerService.java                # Core transfer orchestration
    ├── LockService.java                  # Redis distributed lock (SET NX EX 5s)
    └── PlayFabWalletService.java         # PlayFab virtual currency operations
```

---

## Related Services

| Service | Role |
|---|---|
| `authenticator-service` | Issues JWTs consumed by this service |
| `Websocket` | Receives `POST /notify` calls to push transaction alerts to online players |

---

## License

ISC
