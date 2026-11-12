package com.dimo.parent.security.authorization.action;

import java.time.Instant;
import java.util.List;

public record ActionPolicySnapshotDto(
        long revision,
        Instant issuedAt,
        Instant validUntil,
        List<ActionRuleDto> actions) implements AuthorizationSnapshot {}
