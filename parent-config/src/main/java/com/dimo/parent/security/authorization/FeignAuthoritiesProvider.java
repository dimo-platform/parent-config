package com.dimo.parent.security.authorization;

import com.dimo.parent.security.authorization.action.AuthoritySnapshotDto;
import lombok.RequiredArgsConstructor;
import com.dimo.parent.client.authorization.AuthoritiesResponseDto;
import com.dimo.parent.client.authorization.AuthorizationClient;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Default {@link AuthoritiesProvider}: calls the auth service internal
 * authorities endpoint through OpenFeign. Used by services that do not own the
 * role/permission tables.
 */
@RequiredArgsConstructor
public class FeignAuthoritiesProvider implements AuthoritiesProvider {

    private static final String ROLE_PREFIX = "ROLE_";

    private final AuthorizationClient authorizationClient;


    @Override
    public AuthoritySnapshotDto resolveSnapshot(UUID userId) {
        return authorizationClient.getAuthoritySnapshot(userId);
    }

    @Override
    public Set<String> resolveAuthorities(UUID userId) {

        AuthoritiesResponseDto response = authorizationClient.getAuthorities(userId);

        Set<String> authorities = new HashSet<>();

        if (response.roles() != null) {
            response.roles().forEach(role -> authorities.add(ROLE_PREFIX + role));
        }

        if (response.permissions() != null) {
            authorities.addAll(response.permissions());
        }

        return authorities;

    }

}
