package com.dimo.parent.client.authorization;

import com.dimo.parent.security.authorization.action.registry.ActionRegistrationRequestDto;
import com.dimo.parent.security.authorization.action.registry.ActionRegistrationResultDto;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "user-service",
        contextId = "actionRegistryClient",
        url = "${client.auth.service.url:http://localhost:8090}"
)
public interface ActionRegistryClient {

    String SERVICE_KEY_HEADER = "X-Action-Registry-Key";

    @PostMapping("/internal/v1/authorization/actions/registrations")
    ActionRegistrationResultDto register(
            @RequestHeader(SERVICE_KEY_HEADER)
            String serviceKey,
            @RequestBody
            ActionRegistrationRequestDto request);
}
