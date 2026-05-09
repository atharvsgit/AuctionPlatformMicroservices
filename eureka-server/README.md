# eureka-server

`eureka-server` is the service registry for the system. Other services register themselves here so the gateway and OpenFeign clients can resolve logical service names instead of hard-coded host and port combinations.

## Runtime Role

- Listens on port `8761`.
- Does not register itself as a Eureka client.
- Does not fetch another registry.
- Provides discovery for:
  - `api-gateway`
  - `config-server`
  - `user-service`
  - `auction-service`
  - `bidding-service`
  - `notification-service`

## Local Classes

### `EurekaServerApplication`

The Spring Boot entry class for service discovery. It is annotated with `@EnableEurekaServer`, which enables the Netflix Eureka server inside this application.

The class itself only starts the Spring Boot runtime. Registration, registry storage, heartbeat handling, and discovery endpoints are provided by Spring Cloud Netflix.

## How It Interacts With The System

1. `eureka-server` starts first in Docker Compose.
2. `config-server` starts and registers itself with Eureka.
3. The gateway and domain services start and register with Eureka.
4. `api-gateway` route URIs like `lb://USER-SERVICE` are resolved through discovery and Spring Cloud LoadBalancer.
5. `bidding-service` uses `@FeignClient(name = "AUCTION-SERVICE")`; that logical name is also resolved through Eureka.

## External APIs And Framework Types Used

- `@EnableEurekaServer`: Spring Cloud Netflix annotation that turns a Spring Boot app into a Eureka registry server. Official docs: [Spring Cloud Netflix](https://docs.spring.io/spring-cloud-netflix/docs/current/reference/html/).
- Eureka server and client model: services register, renew leases through heartbeats, and clients discover service instances by application name. Official docs: [Service Discovery: Eureka Clients](https://docs.spring.io/spring-cloud-netflix/docs/current/reference/html/).
- `@SpringBootApplication`: Spring Boot convenience annotation for bootstrapping, auto-configuration, and component scanning. Official docs: [Spring Boot Using the @SpringBootApplication Annotation](https://docs.spring.io/spring-boot/reference/using/using-the-springbootapplication-annotation.html).

## Configuration

`application.yml` sets:

- `server.port: 8761`
- `spring.application.name: eureka-server`
- `eureka.instance.hostname: eureka-server`
- `eureka.client.register-with-eureka: false`
- `eureka.client.fetch-registry: false`

Those two false values are important because this service is the registry itself, not a normal service that needs to register with another registry.

