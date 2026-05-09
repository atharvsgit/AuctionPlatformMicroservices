# bidding-service

`bidding-service` owns bid placement and bid history. It validates bids against live auction state, stores accepted bids, asks `auction-service` to update the current auction price, and publishes bid events for notifications.

## Runtime Role

- Listens on port `8083`.
- Registers with `eureka-server`.
- Imports database, Kafka, Redis, Eureka, and Resilience4j settings from `config-server`.
- Stores bids in PostgreSQL table `bids_schema.bids`.
- Calls `auction-service` through OpenFeign using the logical service name `AUCTION-SERVICE`.
- Publishes `bid-events` to Kafka.
- Uses a Resilience4j circuit breaker around auction-service calls.

## Local Classes

### `BiddingServiceApplication`

Spring Boot entry class. It enables OpenFeign clients through `@EnableFeignClients` and sets the JVM default timezone to `UTC`.

### `BidController`

REST controller for `/api/bids`.

Endpoints:

- `POST /api/bids`: accepts a `Bid` request body and `X-User-Id` header. Calls `BidService.placeBid(...)`. Returns `201 Created` with the saved bid or `400 Bad Request` with an error message.
- `GET /api/bids/auction/{auctionId}`: returns bids for an auction sorted by highest amount first.

### `BidService`

Business logic for validating and placing bids.

`placeBid(Long auctionId, Long bidderId, BigDecimal amount)`:

1. Calls `AuctionClient.getAuctionById(...)` to fetch current auction state.
2. Rejects the bid if the auction is not `ACTIVE`.
3. Rejects the bid if `endTime` has passed.
4. Rejects seller self-bidding.
5. Rejects amounts less than or equal to the current bid.
6. Creates a `Bid`.
7. Saves it through `BidRepo`.
8. Calls `AuctionClient.updateAuctionPrice(...)`.
9. Creates `BidPlacedEvent`.
10. Sends the event to Kafka topic `bid-events`.
11. Returns the saved bid.

The method is wrapped in a Resilience4j circuit breaker named `auctionServiceBreaker`. If calls to `auction-service` fail enough times, the circuit opens and `auctionServiceFallback(...)` responds with a temporary-unavailability error.

`auctionServiceFallback(...)`:

- Logs the underlying failure.
- Throws a runtime exception with a user-facing retry message.

`getBidsForAuction(Long auctionId)`:

- Delegates to `BidRepo.findByAuctionIdOrderByAmountDesc(...)`.

### `BidRepo`

Spring Data repository interface for `Bid` entities. It extends `JpaRepository<Bid, Long>`.

Custom finder:

- `findByAuctionIdOrderByAmountDesc(Long auctionId)`: returns bid history for one auction with highest bids first.

### `Bid`

JPA entity mapped to `bids_schema.bids`.

Fields:

- `id`: database-generated primary key.
- `auctionId`: auction being bid on.
- `bidderId`: user id from `X-User-Id`.
- `amount`: bid amount.
- `bidTime`: automatically set before insert.

Lifecycle hook:

- `@PrePersist onCreate()`: sets `bidTime = LocalDateTime.now()` before the entity is inserted.

### `AuctionClient`

OpenFeign client interface for calling `auction-service`.

Methods:

- `getAuctionById(Long id)`: maps to `GET /api/auctions/{id}`.
- `updateAuctionPrice(Long id, BigDecimal newPrice)`: maps to `PUT /api/auctions/{id}/bid?newPrice=...`.

Because it is annotated with `@FeignClient(name = "AUCTION-SERVICE")`, the physical service instance is resolved through Eureka.

### `AuctionDto`

DTO representing the auction fields that bidding needs to validate a bid.

Fields:

- `id`
- `currentBid`
- `sellerId`
- `status`
- `endTime`

This DTO intentionally mirrors only part of `auction-service`'s `Auction` entity. That keeps bidding coupled to the API contract it needs, not every auction database field.

### `BidPlacedEvent`

Kafka event DTO published after an accepted bid.

Fields:

- `auctionId`
- `bidderId`
- `sellerId`
- `amount`

`notification-service` has a matching DTO and configures Kafka JSON type mapping for deserialization.

## How Classes Interact

Bid placement:

1. `BidController.placeBid(...)` receives a `Bid` request and `X-User-Id`.
2. It calls `BidService.placeBid(...)`.
3. `BidService` asks `AuctionClient` for current auction state.
4. `AuctionClient` performs a load-balanced HTTP call to `auction-service`.
5. `BidService` validates business rules.
6. `BidRepo.save(...)` persists the bid.
7. `AuctionClient.updateAuctionPrice(...)` updates the auction current price.
8. `KafkaTemplate.send(...)` publishes `BidPlacedEvent`.
9. `NotificationConsumer` in `notification-service` later receives the event.

Bid history:

1. `BidController.getBidsForAuction(...)` receives an auction id.
2. `BidService.getBidsForAuction(...)` delegates to `BidRepo`.
3. Spring Data builds the query from the repository method name.

## External APIs And Framework Types Used

- `@FeignClient` and `@EnableFeignClients`: Spring Cloud OpenFeign support for declarative HTTP clients. Official docs: [Spring Cloud OpenFeign](https://docs.spring.io/spring-cloud-openfeign/reference/spring-cloud-openfeign.html).
- `@CircuitBreaker`: Resilience4j annotation that routes protected method failures through circuit breaker state and fallback handling. Official API: [Resilience4j CircuitBreaker annotation](https://javadoc.io/doc/io.github.resilience4j/resilience4j-annotations/latest/io/github/resilience4j/circuitbreaker/annotation/CircuitBreaker.html).
- `JpaRepository`: generated persistence operations and query methods. Official API: [JpaRepository](https://docs.spring.io/spring-data/jpa/docs/current/api/org/springframework/data/jpa/repository/JpaRepository.html).
- JPA annotations such as `@Entity`, `@Table`, `@Id`, `@GeneratedValue`, and `@PrePersist`: map bid data and run lifecycle hooks. Official API: [Jakarta Persistence API](https://jakarta.ee/specifications/persistence/3.2/apidocs/).
- `KafkaTemplate`: Spring Kafka producer helper used to send `BidPlacedEvent`. Official API: [KafkaTemplate](https://docs.spring.io/spring-kafka/api/org/springframework/kafka/core/KafkaTemplate.html).
- Spring MVC annotations such as `@RestController`, `@RequestMapping`, `@PostMapping`, `@GetMapping`, `@RequestHeader`, and `@PathVariable`: expose HTTP endpoints and bind request data. Official docs: [Spring Web MVC Annotated Controllers](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller.html).
- Lombok annotations are imported in some classes and used on `Bid`; Lombok generates boilerplate methods during compilation. Official docs: [Project Lombok Features](https://projectlombok.org/features/).

## Configuration

Local `application.yml` imports config from `config-server`.

Central config includes:

- PostgreSQL datasource.
- Kafka producer JSON serialization.
- Eureka registration.
- Resilience4j circuit breaker settings for `auctionServiceBreaker`.
- Redis host, currently available for infrastructure consistency.

