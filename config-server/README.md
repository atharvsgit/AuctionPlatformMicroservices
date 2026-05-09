# config-server

`config-server` centralizes service configuration for the microservice system. It uses Spring Cloud Config Server in `native` profile mode, meaning it serves YAML files from this repository instead of a remote Git-backed config repository.

## Runtime Role

- Listens on port `8888`.
- Registers with `eureka-server`.
- Serves config from `config-server/src/main/resources/configs/`.
- Allows each service to keep only a small bootstrap-style `application.yml` locally and load its effective settings at startup.

## Local Classes And Config Files

### `ConfigServerApplication`

The Spring Boot entry class for the config server. It is annotated with `@EnableConfigServer`, which turns the application into a Spring Cloud Config Server.

Its `main` method starts the Spring Boot application. The actual config serving behavior is provided by Spring Cloud Config Server after the annotation is enabled.

### `application.yml`

Runtime settings for the config server itself:

- Runs on port `8888`.
- Uses application name `config-server`.
- Activates the `native` profile.
- Points native config search to `classpath:/configs/`.
- Registers with Eureka at `http://eureka-server:8761/eureka/`.

### `configs/api-gateway.yml`

Central configuration for gateway routing, Redis rate limiting, Eureka discovery, and JWT secret binding.

Important sections:

- `spring.data.redis`: tells the gateway where Redis lives.
- `spring.cloud.gateway.routes`: defines routes for `/api/users/**`, `/api/auctions/**`, and `/api/bids/**`.
- `filters`: attaches `AuthenticationFilter` and `RequestRateLimiter` to each route.
- `jwt.secret`: injected from `JWT_SECRET`.

### `configs/user-service.yml`

Central configuration for user persistence, Kafka bootstrap server, Eureka registration, and JWT token creation settings.

Important sections:

- `spring.datasource`: PostgreSQL connection.
- `spring.jpa`: schema generation and SQL logging.
- `spring.kafka`: Kafka bootstrap server.
- `jwt.secret` and `jwt.expiration`: token signing and expiry values.

### `configs/auction-service.yml`

Central configuration for auction persistence, Redis-backed caching, Kafka producer serialization, and Eureka registration.

Important sections:

- `spring.datasource`: PostgreSQL connection.
- `spring.data.redis`: Redis host used by cache infrastructure.
- `spring.kafka.producer`: JSON serialization for events.
- `eureka.client.service-url.defaultZone`: registry address.

### `configs/bidding-service.yml`

Central configuration for bid persistence, Kafka producer behavior, Eureka registration, Redis host, and Resilience4j circuit breaker settings.

Important sections:

- `spring.datasource`: PostgreSQL connection.
- `spring.kafka.producer.value-serializer`: JSON event serialization.
- `resilience4j.circuitbreaker.instances.auctionServiceBreaker`: failure threshold and open-state timing for calls to `auction-service`.

### `configs/notification-service.yml`

Central configuration for Kafka event consumption, JSON type mapping, mail delivery, and Eureka registration.

Important sections:

- `spring.kafka.consumer`: consumer group, offset behavior, deserializer, and trusted packages.
- `spring.kafka.consumer.properties.spring.json.type.mapping`: maps producer event class names into notification-service DTO classes.
- `spring.mail`: Gmail SMTP settings sourced from environment variables.

## How Pieces Interact

1. A service starts and reads its local `application.yml`.
2. The local file imports `optional:configserver:http://config-server:8888`.
3. Spring Cloud Config Client requests configuration using the service's `spring.application.name`.
4. `config-server` resolves the matching YAML file from `classpath:/configs/`.
5. The service starts with central settings for database, Kafka, Eureka, Redis, JWT, or mail as needed.

## External APIs And Framework Types Used

- `@EnableConfigServer`: Spring Cloud annotation that enables Config Server behavior in a Spring Boot app. Official docs: [Spring Cloud Config Server](https://docs.spring.io/spring-cloud-config/reference/server.html).
- Spring Cloud Config native backend: serves local/classpath files instead of a Git repository. Official docs: [Spring Cloud Config](https://docs.spring.io/spring-cloud-config/reference/index.html).
- Eureka client configuration: lets the config server register itself with service discovery. Official docs: [Spring Cloud Netflix Eureka Clients](https://docs.spring.io/spring-cloud-netflix/docs/current/reference/html/).

## Configuration Ownership

This service owns the current central YAML files. If a service's runtime value appears surprising, check this module first, because most local service `application.yml` files only import from the config server.

