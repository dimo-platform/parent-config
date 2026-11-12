package com.dimo.parent.security.authentication.service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.Test;
import com.dimo.parent.client.auth.JwkResponseDto;
import com.dimo.parent.client.auth.JwtClient;
import com.dimo.parent.client.auth.TokenStateDto;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtDecoderSessionTest {

    private static final UUID USER = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID SESSION = UUID.fromString("00000000-0000-0000-0000-000000000012");

    @Test
    void decode_checksCurrentFamilyAndPreservesLegacyTokenFallback() throws Exception {
        // Arrange
        RSAKey key = key();
        JwtClient client = client(key);
        when(client.getTokenState(USER)).thenReturn(new TokenStateDto(3, true));
        when(client.getSessionTokenState(USER, SESSION)).thenReturn(new TokenStateDto(3, true));
        JwtDecoderProvider provider = new JwtDecoderProvider(client);

        // Act / Assert
        assertThat(provider.getDecoder().decode(token(key, SESSION.toString())).getSubject()).isEqualTo(USER.toString());
        assertThat(provider.getDecoder().decode(token(key, null)).getSubject()).isEqualTo(USER.toString());
        verify(client).getTokenState(USER);
        verify(client).getSessionTokenState(USER, SESSION);
        when(client.getSessionTokenState(USER, SESSION)).thenReturn(new TokenStateDto(3, false));
        assertThatThrownBy(() -> provider.getDecoder().decode(token(key, SESSION.toString()))).isInstanceOf(JwtException.class);
    }

    @Test
    void decode_rejectsMalformedSessionClaimsBeforeRemoteStateLookup() throws Exception {
        // Arrange
        RSAKey key = key();
        JwtClient client = client(key);
        JwtDecoderProvider provider = new JwtDecoderProvider(client);

        // Act / Assert
        for (Object malformed : List.of(42, "not-a-session", List.of(SESSION.toString()))) {
            assertThatThrownBy(() -> provider.getDecoder().decode(token(key, malformed))).isInstanceOf(JwtException.class);
        }
        verify(client, never()).getTokenState(any());
        verify(client, never()).getSessionTokenState(any(), any());
    }

    private JwtClient client(RSAKey key) {
        JwtClient client = mock(JwtClient.class);
        when(client.getJwk()).thenReturn(new JwkResponseDto("RSA", "test", "RS256", "sig",
                key.getModulus().toString(), key.getPublicExponent().toString()));
        return client;
    }

    private RSAKey key() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        return new RSAKey.Builder((RSAPublicKey) pair.getPublic()).privateKey((RSAPrivateKey) pair.getPrivate())
                .keyID("test").keyUse(KeyUse.SIGNATURE).algorithm(JWSAlgorithm.RS256).build();
    }

    private String token(RSAKey key, Object sessionId) {
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder().subject(USER.toString()).issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60)).claim("credentialsVersion", 3);
        if (sessionId != null) {
            claims.claim("sid", sessionId);
        }
        JwtEncoder encoder = new NimbusJwtEncoder((selector, context) -> List.of(key));
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims.build())).getTokenValue();
    }
}
