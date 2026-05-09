# api-gateway

`api-gateway` is the public HTTP entry point for the system. It uses Spring Cloud Gateway to route requests to backend services, validates JWT bearer tokens for protected paths, forwards authenticated user metadata through headers, and applies Redis-backed request rate limiting.

## Runtime Role

- Listens on port `9090`.
- Imports route and security-related config from `config-server`.
- Registers with `eureka-server` and routes to logical service names such as `USER-SERVICE`, `AUCTION-SERVICE`, and `BIDDING-SERVICE`.
- Uses Redis as the backing store for Spring Cloud Gateway `RequestRateLimiter`.
- Uses the same `JWT_SECRET` as `user-service` so tokens issued during login can be validated at the gateway.

## Local Classes

### `ApiGatewayApplication`

The Spring Boot entry class. Its `main` method starts the gateway application and enables component scanning for the gateway package. It does not contain routing logic directly; that comes from Spring Cloud Gateway configuration and custom beans.

### `AuthenticationFilter`

Custom gateway filter registered as a Spring component. It extends Spring Cloud Gateway's `AbstractGatewayFilterFactory<AuthenticationFilter.Config>`, which lets the YAML route config reference it by name as `AuthenticationFilter`.

How it works:

1. Receives every request that passes through a route where the filter is configured.
2. Asks `RouteValidator` whether the current path is secured.
3. If the route is open, it lets the request continue unchanged.
4. If the route is secured, it requires an `Authorization` header.
5. Strips the `Bearer ` prefix from the header value.
6. Calls `JwtUtil.validateToken(...)`.
7. Reads claims from the JWT, especially `userId` and `role`.
8. Mutates the outgoing request to add `X-User-Id` and `X-User-Role`.
9. If validation fails, returns `401 Unauthorized` and stops the request.

The downstream services depend on this behavior. For example, `auction-service` trusts `X-User-Id` as the seller id, and `bidding-service` trusts it as the bidder id.

### `AuthenticationFilter.Config`

Empty nested config class required by `AbstractGatewayFilterFactory`. It exists so Spring Cloud Gateway can instantiate the filter using its standard filter factory contract, even though this filter does not currently accept per-route arguments.

### `JwtUtil`

Gateway-side JWT helper. It is responsible for:

- Reading `jwt.secret` from configuration.
- Base64-decoding the shared secret.
- Building an HMAC signing key.
- Validating signed JWT claims.
- Returning parsed JWT claims to `AuthenticationFilter`.

This class does not create tokens; token creation happens in `user-service`. The gateway only validates and extracts claims.

### `RouteValidator`

Small component that decides which paths need authentication. It keeps an allow-list named `openApiEndpoints`:

- `/api/users/register`
- `/api/users/login`
- `/eureka`

Its `isSecured` predicate returns `true` when the request path does not contain one of those open endpoint fragments.

### `RateLimiterConfig`

Spring configuration class that provides the `ipKeyResolver` bean. Gateway route YAML references this bean through SpEL as `#{@ipKeyResolver}`. It resolves each request's remote IP address and uses that value as the rate-limit key.

## How Classes Interact

1. The client sends a request to `api-gateway`.
2. Spring Cloud Gateway matches the request path against routes from `config-server/src/main/resources/configs/api-gateway.yml`.
3. For `/api/users/**`, `/api/auctions/**`, and `/api/bids/**`, Gateway applies `AuthenticationFilter` and `RequestRateLimiter`.
4. `AuthenticationFilter` asks `RouteValidator` whether the path is secured.
5. For secured paths, `AuthenticationFilter` calls `JwtUtil`.
6. On success, `AuthenticationFilter` injects user headers and continues the filter chain.
7. `RequestRateLimiter` uses `ipKeyResolver` from `RateLimiterConfig` and Redis to enforce limits.
8. Gateway forwards the request to the matching backend service through Eureka and Spring Cloud LoadBalancer.

## External APIs And Framework Types Used

- `AbstractGatewayFilterFactory` and `GatewayFilter`: Spring Cloud Gateway extension points for building named route filters. Official docs: [Spring Cloud Gateway filter factories](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/gatewayfilter-factories.html).
- `RequestRateLimiter` and `KeyResolver`: Gateway rate limiting support. The key resolver extracts the identity used for rate limiting. Official docs: [RequestRateLimiter GatewayFilter Factory](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/gatewayfilter-factories/requestratelimiter-factory.html).
- `ServerHttpRequest`: Reactive HTTP request abstraction used by Gateway to mutate headers before forwarding. Official API: [ServerHttpRequest](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/http/server/reactive/ServerHttpRequest.html).
- `Mono`: Reactor's async single-value publisher, returned by the `KeyResolver`. Official API: [Reactor Mono](https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Mono.html).
- `Jwts`, `Claims`, `Decoders`, and `Keys`: JJWT APIs used to parse and verify JWTs. Official project docs: [JJWT](https://github.com/jwtk/jjwt).
- `@Component`, `@Configuration`, and `@Bean`: Spring annotations that register custom classes and factory methods as application beans. Official docs: [Spring Framework Core Beans](https://docs.spring.io/spring-framework/reference/core/beans.html).

## Configuration

The local `application.yml` sets:

- `server.port: 9090`
- `spring.application.name: api-gateway`
- `spring.config.import: optional:configserver:http://config-server:8888`

The central `config-server/src/main/resources/configs/api-gateway.yml` defines:

- Gateway routes for users, auctions, and bids.
- Redis host and port.
- Rate limiter replenish rate and burst capacity.
- Eureka service registry address.
- `jwt.secret` from `JWT_SECRET`.

