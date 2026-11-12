package com.dimo.parent.security.authorization.action.factory;

import com.dimo.parent.security.authorization.action.*;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class AuthorizationTestFactory {

    public static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000011");
    public static final Instant NOW = Instant.parse("2026-10-26T10:00:00Z");

    private AuthorizationTestFactory() {}

    public static AuthoritySnapshotDto authority(long revision, Instant issuedAt, Instant until, Set<String> permissions) {
        return new AuthoritySnapshotDto(USER_ID, revision, issuedAt, until, Set.of("USER"), Set.copyOf(permissions));
    }

    public static ActionPolicySnapshotDto policy(long revision, Instant now, String permission) {
        return new ActionPolicySnapshotDto(revision, now, now.plusSeconds(5), List.of(
                new ActionRuleDto("chat.message.create", "dimo", "chat", "پیام", "Message", true, permission, null)));
    }
}
