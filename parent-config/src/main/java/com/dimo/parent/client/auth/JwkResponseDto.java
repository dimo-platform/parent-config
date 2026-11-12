package com.dimo.parent.client.auth;

public record JwkResponseDto(
        String kty,
        String kid,
        String alg,
        String use,
        String n,
        String e
) {
}