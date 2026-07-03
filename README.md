# SnipURL

A globally distributed URL shortener uses a scalable architecture with distributed ID generation, partitioned storage,
cached redirects, and an in-memory layer to protect the backend from excessive traffic.

## Project layout

This is a Gradle multi-project build with four modules:

- **[`url-service`](url-service)** – Spring Boot service that exposes the REST API for creating and resolving short URLs.

- **[`analytics-service`](analytics-service)** – Spring Boot service that consumes click events from Kafka and updates URL click statistics.

- **[`common-events`](common-events)** – Shared Java library containing the Kafka event model (for example, `ClickEvent`) used by both backend services.

- **[`frontend`](frontend)** – React + Vite web application for creating and managing short URLs.

`url-service` and `analytics-service` communicate only through Kafka (the `click-metrics`
topic, typed by `common-events`' `ClickEvent`) and by both reading/writing MongoDB's
`short_urls` collection — they share no code beyond that event type, and each owns its own
Mongo entity mapping for exactly the fields it touches.

## Architecture

| Concern | Implementation |
|---|---|
| **ID generation** | [`IdGenerator`](url-service/src/main/java/com/shortener/urlservice/service/IdGenerator.java) — a Twitter Snowflake-style 64-bit ID generated locally per node (timestamp + datacenter id + worker id + sequence). No network round-trip and no single-writer bottleneck. |
| **Storage** | MongoDB, sharded 3 ways on a hashed `short_code` key (see [`init-shards-and-sharding.js`](mongo/init-shards-and-sharding.js)) — actually partitioned, not just conceptually. See [`ShortUrl`](url-service/src/main/java/com/shortener/urlservice/entity/ShortUrl.java) / [`ShortUrlRepo`](url-service/src/main/java/com/shortener/urlservice/repo/ShortUrlRepo.java). |
| **Read cache** | Redis, in front of Mongo, keyed by short code (see [`UrlShortenerService`](url-service/src/main/java/com/shortener/urlservice/service/UrlShortenerService.java)). |
| **Guard layer** | [`ShortCodeBloomFilter`](url-service/src/main/java/com/shortener/urlservice/service/ShortCodeBloomFilter.java) — an in-memory Bloom filter checked before Redis/Mongo on every redirect. Unknown codes are rejected immediately, protecting the backend from random-guessing/scraping traffic. Bulk-loaded at boot ([`BloomFilterStartupRunner`](url-service/src/main/java/com/shortener/urlservice/config/BloomFilterStartupRunner.java)), periodically rebuilt ([`BloomFilterRebuildScheduler`](url-service/src/main/java/com/shortener/urlservice/service/BloomFilterRebuildScheduler.java)), and kept converged across nodes in near-real-time via a Kafka broadcast ([`BloomFilterSyncConsumerService`](url-service/src/main/java/com/shortener/urlservice/service/BloomFilterSyncConsumerService.java)) — each node's filter is otherwise purely local memory, so without this a code created on one node would 404 on every other node for up to an hour. |
| **Edge caching** | Redirect responses carry a `Cache-Control` header so a CDN can serve hot links without ever reaching origin. |
| **Click analytics** | `url-service` redirects publish a [`ClickEvent`](common-events/src/main/java/com/shortener/events/ClickEvent.java) to Kafka; `analytics-service`'s [`ClickMetricsConsumerService`](analytics-service/src/main/java/com/shortener/analytics/service/ClickMetricsConsumerService.java) consumes it and atomically increments `clickCount` in Mongo. |

## Tech stack

- **Backend**: Java 21, Spring Boot 4.1, Spring Data MongoDB, Spring Data Redis, Spring Kafka, Guava (Bloom filter)
- **Storage**: MongoDB, Redis, Kafka (all via `docker-compose.yml`)
- **Frontend**: React + Vite (`frontend/`)

## Prerequisites

- JDK 21
- Docker (for MongoDB, Redis, Kafka)
- Node.js (for the frontend, optional)

## Running locally

1. Start the backing services:

   ```bash
   docker-compose up -d mongo-config mongo-shard1 mongo-shard2 mongo-shard3 mongo-router mongo-setup redis kafka-1 kafka-2 kafka-3
   ```

   This starts a 3-shard MongoDB cluster (`localhost:27017`, routed through `mongo-router` —
   see [MongoDB sharded cluster](#mongodb-sharded-cluster) below), Redis (`localhost:6379`),
   and a 3-broker Kafka cluster (`localhost:9092`/`9093`/`9094`).

2. Run url-service (serves the API on `http://localhost:8080`):

   ```bash
   ./gradlew :url-service:bootRun
   ```

3. (Optional, needed for click counts to update) Run analytics-service in another terminal:

   ```bash
   ./gradlew :analytics-service:bootRun
   ```

4. (Optional) Run the frontend:

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

## API

### `POST /api/v1/shorten`

Creates a short link.

**Params** (form/query): `longUrl` (required), `expiresAtValue` (optional, epoch seconds)

```bash
curl -X POST "http://localhost:8080/api/v1/shorten?longUrl=https://example.com"
```

```json
{"shortCode":"Edq2dAh1XM","targetUrl":"https://example.com","clickCount":0,"expiresAt":null}
```

### `GET /api/v1/{shortCode}`

Redirects to the target URL.

```bash
curl -i http://localhost:8080/api/v1/Edq2dAh1XM
```

```
HTTP/1.1 302
Location: https://example.com
Cache-Control: max-age=3600, public
```

Unknown or expired codes return `404`.

## Running multiple nodes

`docker-compose.yml` defines three containerized `url-service` nodes (`url-service-1`,
`url-service-2`, `url-service-3`) and three `analytics-service` replicas
(`deploy.replicas: 3` — Compose auto-names them, e.g. `demo-analytics-service-1/2/3`; run
`docker compose ps` to see the exact names), alongside the sharded Mongo cluster / Redis /
Kafka, to actually exercise the distributed-ID design, MongoDB shard distribution, and Kafka
consumer-group load balancing with more than one instance of each service:

```bash
docker-compose up -d --build
```

This builds both service images (see `url-service/Dockerfile` and
`analytics-service/Dockerfile`) and starts `url-service` nodes on `localhost:8081`, `:8082`,
and `:8083`, each with a distinct Snowflake identity but sharing the same sharded
Mongo / Redis / Kafka cluster:

| Node | Port | Datacenter id | Worker id |
|---|---|---|---|
| `url-service-1` | 8081 | 0 | 0 |
| `url-service-2` | 8082 | 0 | 1 |
| `url-service-3` | 8083 | 1 | 0 |

Kafka itself also runs as a 3-broker cluster (`kafka-1/2/3`, `localhost:9092`/`9093`/`9094`),
and `click-metrics` is replicated across all 3 brokers (`.replicas(3)` in the same bean) so
the topic survives the loss of any single broker.

### MongoDB sharded cluster

`docker-compose.yml` runs a real (if minimally-redundant) sharded MongoDB cluster rather
than a single `mongod`:

| Service | Role |
|---|---|
| `mongo-config` | Config server (1-node replica set `configReplSet`) — holds cluster/chunk metadata. |
| `mongo-shard1/2/3` | The 3 shards, each its own 1-node replica set (`shard1ReplSet`/etc — MongoDB requires every shard to be a replica set, even a single-member one). |
| `mongo-router` | `mongos` — the only component the app or `localhost:27017` tools ever talk to; routes each query to the right shard(s). |
| `mongo-setup` | One-shot job: adds the 3 shards, enables sharding on `appdb`, and shards `short_urls` on a **hashed** `short_code` key with a uniqueness constraint. |


## Testing

```bash
./gradlew test
```
