package com.dimo.parent.security.authorization.action.registry;

import java.util.List;

public record ActionRegistrationRequestDto(
        String service,
        List<ActionDescriptor> actions) {}
