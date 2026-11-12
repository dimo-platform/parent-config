package com.dimo.parent.security.authorization.action;

import java.util.UUID;

/** Optional action permission only. Owning services must retain mandatory and fixed checks. */
public interface ActionAuthorizer {

    void require(UUID userId, String actionId);

    boolean isAllowed(UUID userId, String actionId);

    EffectiveCapabilitiesDto getCapabilities(UUID userId);
}
