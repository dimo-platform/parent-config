package com.dimo.parent.security.authorization;

import com.dimo.parent.security.authorization.action.AuthoritySnapshotDto;
import java.util.HashSet;
import java.util.Objects;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * SPI used by the shared security layer to obtain the authorities of a user.
 * <p>
 * Services that own the role/permission tables provide a local implementation;
 * the default implementation calls the auth service over OpenFeign so every
 * microservice resolves the very same authority model per request.
 */
public interface AuthoritiesProvider {

    /**
     * Resolves the final authority strings of a user (role authorities such as
     * {@code ROLE_admin} plus permissions such as {@code user_read}).
     *
     * @param userId the stable user id (JWT subject)
     * @return the authorities, never {@code null}
     */
    Set<String> resolveAuthorities(UUID userId);

    /** Legacy revision zero is usable for fixed authorities, never for configurable actions. */
    default AuthoritySnapshotDto resolveSnapshot(UUID userId) {
        Instant issuedAt = Instant.now();
        Set<String> resolved = Objects.requireNonNull(resolveAuthorities(userId));
        Set<String> roles = new HashSet<>();
        Set<String> permissions = new HashSet<>();
        for (String value : resolved) {
            if (value.startsWith("ROLE_")) {
                roles.add(value.substring(5));
            } else {
                permissions.add(value);
            }
        }
        return new AuthoritySnapshotDto(
                userId, 0, issuedAt, issuedAt.plusSeconds(5), Set.copyOf(roles), Set.copyOf(permissions));
    }
}
