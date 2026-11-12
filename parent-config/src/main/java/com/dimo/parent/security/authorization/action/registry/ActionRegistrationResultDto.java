package com.dimo.parent.security.authorization.action.registry;

import java.util.List;

public record ActionRegistrationResultDto(
        int registered,
        List<String> retired) {}
