package com.dimo.parent.security.authentication.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64URL;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.dimo.parent.client.auth.JwkResponseDto;
import com.dimo.parent.client.auth.JwtClient;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

/**
 * Builds (and lazily caches) the {@link JwtDecoder} used to validate incoming bearer tokens. The
 * public key is obtained from the auth service through {@link JwtClient}, so every microservice
 * validates tokens against the very same key material without sharing the private key.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtDecoderProvider {

    private final JwtClient jwtClient;

    private volatile JwtDecoder jwtDecoder;

    /**
     * Returns the cached decoder, fetching the JWK from the auth service on the first successful
     * call.
     */
    public JwtDecoder getDecoder() {

        JwtDecoder decoder = this.jwtDecoder;

        if (decoder == null) {

            synchronized (this) {
                decoder = this.jwtDecoder;

                if (decoder == null) {
                    decoder = buildDecoder();
                    this.jwtDecoder = decoder;
                }
            }
        }

        return decoder;
    }

    /**
     * Drops the cached decoder so the next call re-reads the JWK from the auth service (for example
     * after key rotation).
     */
    public void refresh() {
        this.jwtDecoder = null;
    }

    private JwtDecoder buildDecoder() {

        JwkResponseDto jwk = jwtClient.getJwk();

        try {

            RSAKey rsaKey =
                    new RSAKey.Builder(new Base64URL(jwk.n()), new Base64URL(jwk.e()))
                            .keyID(jwk.kid())
                            .algorithm(JWSAlgorithm.RS256)
                            .keyUse(KeyUse.SIGNATURE)
                            .build();

            log.debug("JWT decoder built from auth service JWK, kid '{}'", jwk.kid());

            JwtDecoder signatureDecoder =
                    NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
            return token -> {
                org.springframework.security.oauth2.jwt.Jwt jwt = signatureDecoder.decode(token);
                java.util.UUID userId = java.util.UUID.fromString(jwt.getSubject());
                Object rawSessionId = jwt.getClaims().get("sid");
                com.dimo.parent.client.auth.TokenStateDto state;
                if (!jwt.getClaims().containsKey("sid")) {
                    state = jwtClient.getTokenState(userId);
                } else {
                    if (!(rawSessionId instanceof String sid) || sid.length() != 36) {
                        throw new org.springframework.security.oauth2.jwt.JwtException("Malformed session identity");
                    }
                    state = jwtClient.getSessionTokenState(userId, java.util.UUID.fromString(sid));
                }
                Number version = jwt.getClaim("credentialsVersion");
                long tokenVersion = version == null ? 0 : version.longValue();
                if (state == null || !state.active() || tokenVersion != state.credentialsVersion())
                    throw new org.springframework.security.oauth2.jwt.JwtException("Token revoked");
                return jwt;
            };

        } catch (JOSEException failure) {
            throw new IllegalStateException(
                    "Unable to build JWT decoder from the auth service JWK", failure);
        }
    }
}
