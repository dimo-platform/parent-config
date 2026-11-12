package com.dimo.parent.client.authorization;

import java.util.Set;

/**
 * Role and permission names of a user as exposed by the auth service internal
 * endpoint.
 */
public record AuthoritiesResponseDto(
        Set<String> roles,
        Set<String> permissions
) {
}
