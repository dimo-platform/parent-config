package com.dimo.parent.security.authorization.action;

import java.time.Instant;
import java.util.List;

public record EffectiveCapabilitiesDto(
        long policyRevision,
        long authorityRevision,
        Instant validUntil,
        List<ActionCapabilityDto> actions) {}
