# auction-service

`auction-service` owns auction records and the current auction price. It creates auctions, returns auction state to other services, updates current bid values, closes expired auctions on a schedule, and publishes auction-ended events to Kafka.

## Runtime Role

- Listens on port `8082`.
- Registers with `eureka-server`.
- Imports database, Kafka, Redis, and Eureka settings from `config-server`.
- Stores auctions in PostgreSQL table `auctions_schema.auctions`.
- Uses optimistic locking through a JPA `@Version` field.
- Caches the all-auctions query using Spring Cache.
- Publishes `auction-events` to Kafka when scheduled closing detects expired auctions.

## Local Classes

### `AuctionServiceApplication`

Spring Boot entry class.

Enabled features:

- `@EnableFeignClients`: enables OpenFeign clients if this service adds any.
- `@EnableScheduling`: enables `AuctionClosingJob`.
- `@EnableCaching`: enables cache annotations in `AuctionService`.

It also sets the JVM default timezone to `Asia/Kolkata`.

### `AuctionController`

REST controller for `/api/auctions`.

Endpoints:

- `POST /api/auctions`: creates an auction. Reads `X-User-Id` from the gateway and sets it as `sellerId`.
- `GET /api/auctions`: returns all auctions.
- `GET /api/auctions/{id}`: returns one auction by id.
- `PUT /api/auctions/{id}/bid?newPrice=...`: updates the current auction price after a successful bid.

The bid update endpoint is used by `bidding-service` through OpenFeign.

### `AuctionService`

Business logic for auction creation, lookup, list caching, and price updates.

`createAuction(Auction auction)`:

1. Sets status to `ACTIVE`.
2. Sets `currentBid` to `startingPrice`.
3. Saves the auction.
4. Evicts the `auctions` cache because the list changed.

`getAllAuctions()`:

1. Reads all auctions from `AuctionRepo`.
2. Caches the result under cache name `auctions`.

`getAuctionById(Long id)`:

1. Calls `AuctionRepo.findById(...)`.
2. Throws a runtime exception if the auction does not exist.

`updateAuctionPrice(Long auctionId, BigDecimal newBidAmt)`:

1. Loads the auction.
2. Rejects amounts that are not higher than `currentBid`.
3. Updates `currentBid`.
4. Saves the auction.
5. Converts optimistic locking conflicts into a user-facing retry message.
6. Evicts the `auctions` cache because the price changed.

### `AuctionRepo`

Spring Data repository interface for `Auction` entities. It extends `JpaRepository<Auction, Long>`.

Custom finder:

- `findByEndTimeBeforeAndStatus(LocalDateTime time, String status)`: used by `AuctionClosingJob` to find active auctions whose end time has passed.

### `Auction`

JPA entity mapped to `auctions_schema.auctions`.

Fields:

- `id`: database-generated primary key.
- `version`: optimistic locking version. JPA increments this to detect concurrent updates.
- `title`: auction title.
- `description`: auction description.
- `startingPrice`: initial price.
- `currentBid`: current highest bid amount.
- `status`: currently `ACTIVE` or `CLOSED`.
- `endTime`: scheduled auction end.
- `sellerId`: id of the user who created the auction.

The entity implements `Serializable`, which is useful for cache serialization scenarios.

### `AuctionClosingJob`

Scheduled background component that closes expired auctions.

Every 6 seconds:

1. Reads current time.
2. Calls `AuctionRepo.findByEndTimeBeforeAndStatus(now, "ACTIVE")`.
3. Marks each expired auction as `CLOSED`.
4. Saves the auction.
5. Creates an `AuctionEndedEvent`.
6. Sends that event to Kafka topic `auction-events`.

The method is annotated with `@Transactional`, so the database work in one run participates in a transaction.

### `AuctionEndedEvent`

Kafka event DTO published when an auction closes.

Fields:

- `auctionId`
- `sellerId`

`notification-service` has a matching DTO and configures Kafka JSON type mapping so it can deserialize this event into its own class.

## How Classes Interact

Auction creation:

1. `AuctionController.createAuction(...)` receives an `Auction` request body and `X-User-Id`.
2. It sets `sellerId`.
3. `AuctionService.createAuction(...)` initializes status and current price.
4. `AuctionRepo.save(...)` persists the auction.

Bid price update:

1. `bidding-service` calls `PUT /api/auctions/{id}/bid`.
2. `AuctionController.updateAuctionPrice(...)` passes the id and amount to `AuctionService`.
3. `AuctionService` validates the new price and saves the update.
4. JPA optimistic locking helps detect concurrent writes.

Auction closing:

1. `AuctionClosingJob` runs on schedule.
2. It asks `AuctionRepo` for expired active auctions.
3. It closes and saves each auction.
4. It publishes `AuctionEndedEvent` through `KafkaTemplate`.
5. `notification-service` consumes the Kafka event.

## External APIs And Framework Types Used

- `JpaRepository`: generated persistence operations and finder method support. Official API: [JpaRepository](https://docs.spring.io/spring-data/jpa/docs/current/api/org/springframework/data/jpa/repository/JpaRepository.html).
- JPA annotations such as `@Entity`, `@Table`, `@Id`, `@GeneratedValue`, and `@Version`: map entities and enable optimistic locking. Official API: [Jakarta Persistence API](https://jakarta.ee/specifications/persistence/3.2/apidocs/).
- `OptimisticLockingFailureException`: Spring exception for optimistic locking conflicts. Official API: [OptimisticLockingFailureException](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/dao/OptimisticLockingFailureException.html).
- `@Scheduled` and `@EnableScheduling`: Spring scheduling support for recurring jobs. Official docs: [Task Execution and Scheduling](https://docs.spring.io/spring-framework/reference/integration/scheduling.html).
- `@Transactional`: transaction boundary annotation. Official docs: [Declarative Transaction Management](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html).
- `KafkaTemplate`: Spring Kafka producer helper used to send events. Official API: [KafkaTemplate](https://docs.spring.io/spring-kafka/api/org/springframework/kafka/core/KafkaTemplate.html).
- `@Cacheable`, `@CacheEvict`, and `@EnableCaching`: Spring cache abstraction for method-level caching. Official docs: [Spring Boot Caching](https://docs.spring.io/spring-boot/reference/io/caching.html).
- Lombok annotations such as `@Data`, `@NoArgsConstructor`, and `@AllArgsConstructor`: generate boilerplate code at compile time. Official docs: [Project Lombok Features](https://projectlombok.org/features/).

## Configuration

Local `application.yml` imports config from `config-server`.

Central config includes:

- PostgreSQL datasource.
- Hibernate schema generation.
- Redis host and port.
- Kafka producer JSON serialization.
- Eureka registration.

