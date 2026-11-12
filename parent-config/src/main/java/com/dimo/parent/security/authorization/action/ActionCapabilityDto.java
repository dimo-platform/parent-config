package com.dimo.parent.security.authorization.action;



public record ActionCapabilityDto(
        String actionId,
        boolean allowed,
        String requiredPermission) {}
