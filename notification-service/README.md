# notification-service

`notification-service` consumes Kafka events from the auction and bidding flows and sends email notifications. It does not own user or auction state yet; the current implementation uses configured mail credentials and hard-coded recipient addresses while the event contracts carry the ids needed for future lookup.

## Runtime Role

- Listens on port `8084`.
- Registers with `eureka-server`.
- Imports Kafka, mail, and Eureka settings from `config-server`.
- Consumes Kafka topics:
  - `bid-events`
  - `auction-events`
- Sends email through Gmail SMTP using Spring Mail.

## Local Classes

### `NotificationServiceApplication`

Spring Boot entry class. It starts the notification service and enables component scanning for consumers, DTOs, and mail services.

### `NotificationConsumer`

Kafka consumer service.

`consumeBidEvent(BidPlacedEvent e)`:

1. Listens to Kafka topic `bid-events`.
2. Receives a deserialized `BidPlacedEvent`.
3. Logs the seller id.
4. Calls `EmailService.sendBidNotification(...)`.
5. Catches and logs email failures so the listener method does not crash silently.

`consumeAuctionEndedEvent(AuctionEndedEvent e)`:

1. Listens to Kafka topic `auction-events`.
2. Receives a deserialized `AuctionEndedEvent`.
3. Logs the auction id.
4. Calls `EmailService.sendAuctionEndedEmail(...)`.
5. Catches and logs email failures.

Current limitation: recipient emails are hard-coded as `atharvcodes@gmail.com`. The event carries seller/bidder ids, but this service does not yet call `user-service` to resolve real email addresses.

### `EmailService`

Mail delivery service built on Spring's `JavaMailSender`.

`sendBidNotification(String toEmail, Long auctionId, BigDecimal amt)`:

1. Creates a `SimpleMailMessage`.
2. Sets sender from `spring.mail.username`.
3. Sets recipient, subject, and message body.
4. Sends the email through `JavaMailSender`.

`sendAuctionEndedEmail(String sellerEmail, String winnerEmail, Long auctionId)`:

1. Builds and sends an email to the seller.
2. Builds and sends an email to the winner.
3. Logs success.

### `BidPlacedEvent`

Notification-side copy of the bid event contract.

Fields:

- `auctionId`
- `bidderId`
- `sellerId`
- `amount`

Kafka JSON type mapping maps producer class `com.example.bidding_service.dto.BidPlacedEvent` into this local class.

### `AuctionEndedEvent`

Notification-side copy of the auction-ended event contract.

Fields:

- `auctionId`
- `sellerId`

Kafka JSON type mapping maps producer class `com.example.auction_service.dto.AuctionEndedEvent` into this local class.

## How Classes Interact

Bid notification:

1. `bidding-service` publishes `BidPlacedEvent` to `bid-events`.
2. Kafka stores the event.
3. `NotificationConsumer.consumeBidEvent(...)` receives it.
4. The configured JSON deserializer maps the producer DTO class into notification-service's `BidPlacedEvent`.
5. `NotificationConsumer` calls `EmailService.sendBidNotification(...)`.
6. `EmailService` uses `JavaMailSender` to send the email.

Auction-ended notification:

1. `auction-service` publishes `AuctionEndedEvent` to `auction-events`.
2. `NotificationConsumer.consumeAuctionEndedEvent(...)` receives it.
3. The JSON type mapping converts it to notification-service's `AuctionEndedEvent`.
4. `NotificationConsumer` calls `EmailService.sendAuctionEndedEmail(...)`.
5. `EmailService` sends seller and winner emails.

## External APIs And Framework Types Used

- `@KafkaListener`: Spring Kafka annotation that turns methods into Kafka message listeners. Official docs: [Receiving Messages with @KafkaListener](https://docs.spring.io/spring-kafka/reference/kafka/receiving-messages/listener-annotation.html).
- `ErrorHandlingDeserializer` and `JsonDeserializer`: Spring Kafka deserialization support used in config to safely deserialize JSON events. Official docs: [Spring Kafka Serialization, Deserialization, and Message Conversion](https://docs.spring.io/spring-kafka/reference/kafka/serdes.html).
- `JavaMailSender`: Spring Mail interface for sending email. Official API: [JavaMailSender](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/mail/javamail/JavaMailSender.html).
- `SimpleMailMessage`: simple text email message class. Official API: [SimpleMailMessage](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/mail/SimpleMailMessage.html).
- `@Value`: Spring annotation used to inject `spring.mail.username`. Official docs: [Using @Value](https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/value-annotations.html).
- Spring stereotypes such as `@Service`: register classes as injectable beans. Official docs: [ClassPath Scanning and Managed Components](https://docs.spring.io/spring-framework/reference/core/beans/classpath-scanning.html).

## Configuration

Local `application.yml` imports config from `config-server`.

Central config includes:

- Kafka bootstrap server.
- Consumer group `notification-group-v5`.
- Latest-offset startup behavior.
- JSON deserializer and producer-to-consumer type mappings.
- Gmail SMTP host, port, auth, STARTTLS, and credentials.
- Eureka registration.

