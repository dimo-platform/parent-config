package com.dimo.parent.client.authorization;

import com.dimo.parent.security.authorization.action.AuthoritySnapshotDto;
import com.dimo.parent.security.authorization.action.ActionPolicySnapshotDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
        name = "user-service",
        contextId = "authorizationClient",
        url = "${client.auth.service.url:http://localhost:8090}"
)
public interface AuthorizationClient {

    @GetMapping("/internal/v1/authorization/policy")
    ActionPolicySnapshotDto getPolicySnapshot();

    @GetMapping("/internal/v1/authorization/users/{userId}/snapshot")
    AuthoritySnapshotDto getAuthoritySnapshot(@PathVariable UUID userId);

    @GetMapping("/internal/v1/authorities/users/{userId}")
    AuthoritiesResponseDto getAuthorities(@PathVariable UUID userId);
}
