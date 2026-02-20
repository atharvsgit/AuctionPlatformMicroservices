package com.example.api_gateway.filter;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouteValidator {

    public static final List<String> openApiEndpoints = List.of("/api/users/register", "/api/users/login","/eureka");

    public Predicate<ServerHttpRequest> isSecured =
            req -> openApiEndpoints
            .stream()
            .noneMatch(uri -> req.getURI().getPath().contains(uri));
}
