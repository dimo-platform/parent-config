package com.dimo.parent.client.auth;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(
        name = "user-service",
        contextId = "jwtClient",
        url = "${client.auth.service.url:http://localhost:8090}"
)
public interface JwtClient {

    @GetMapping("/internal/v1/auth/jwk")
    JwkResponseDto getJwk();

    @GetMapping("/internal/v1/auth/users/{userId}/token-state")
    TokenStateDto getTokenState(@org.springframework.web.bind.annotation.PathVariable("userId") java.util.UUID userId);
    @GetMapping("/internal/v1/auth/users/{userId}/token-state")
    TokenStateDto getSessionTokenState(
            @org.springframework.web.bind.annotation.PathVariable("userId")
            java.util.UUID userId,
            @org.springframework.web.bind.annotation.RequestParam("sessionId")
            java.util.UUID sessionId);
}
