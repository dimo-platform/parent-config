package com.dimo.parent.security.authorization.action;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record AuthoritySnapshotDto(
        UUID userId,
        long revision,
        Instant issuedAt,
        Instant validUntil,
        Set<String> roles,
        Set<String> permissions) implements AuthorizationSnapshot {}
