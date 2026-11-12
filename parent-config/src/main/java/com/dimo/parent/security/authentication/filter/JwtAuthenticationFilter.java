package com.dimo.parent.security.authentication.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.dimo.parent.security.authentication.service.JwtDecoderProvider;
import com.dimo.parent.security.authentication.web.SecurityResponseWriter;
import com.dimo.parent.security.authorization.AuthoritiesService;
import com.dimo.parent.security.authorization.domain.AuthenticatedUser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Authenticates requests carrying a JWT bearer token. The token is validated
 * against the JWK exposed by the auth service; the user's roles and
 * permissions are then reloaded per request (never taken from the token) and
 * turned into the authorities of the security context.
 * <p>
 * An invalid or expired token is rejected with 401 immediately, while requests
 * without a bearer header are passed through untouched so the public endpoints
 * and the entry point keep working.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USERNAME_CLAIM = "username";
    private static final String TOKEN_INVALID_CODE = "error.security.token.invalid";

    // services
    private final JwtDecoderProvider jwtDecoderProvider;
    private final AuthoritiesService authoritiesService;

    // handlers
    private final SecurityResponseWriter responseWriter;
    private final AuthenticationTrustResolver authenticationTrustResolver;


    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // These public proof exchanges cannot depend on an expired access token sent by a client.
        return "POST".equals(request.getMethod()) && java.util.Set.of("/api/v1/auth/token",
                "/api/v1/auth/refresh", "/api/v1/auth/email/challenges", "/api/v1/auth/email/token",
                "/api/v1/auth/password-reset").contains(request.getServletPath().isEmpty()
                        ? request.getRequestURI() : request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        Authentication currentAuthentication = SecurityContextHolder.getContext().getAuthentication();

        boolean isAnonymous = authenticationTrustResolver.isAnonymous(currentAuthentication);

        if (authorizationHeader != null && !authorizationHeader.isBlank()
                && (SecurityContextHolder.getContext().getAuthentication() == null || isAnonymous)) {

            if (doesNotStartWithPrefix(authorizationHeader, BEARER_PREFIX)) {
                filterChain.doFilter(request, response);
                return;
            }

            String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();

            try {

                Jwt jwt = jwtDecoderProvider.getDecoder().decode(token);

                Authentication authentication = authenticate(jwt);

                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (JwtException | IllegalArgumentException failure) {

                log.debug("Bearer token rejected: {}", failure.getMessage());

                reject(request, response);

                return;

            } catch (RuntimeException failure) {

                // the authorities could not be resolved (auth service unavailable): never a 500
                log.warn("Unable to resolve authorities for the request: {}", failure.getMessage());

                reject(request, response);

                return;

            }

        }

        filterChain.doFilter(request, response);

    }


    private Authentication authenticate(Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());
        String username = jwt.getClaimAsString(USERNAME_CLAIM);

        List<GrantedAuthority> authorities = authoritiesService.getAuthorities(userId).stream()
                .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                .toList();

        AuthenticatedUser principal = new AuthenticatedUser(userId, username, authorities);

        return UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities);

    }


    private void reject(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SecurityContextHolder.clearContext();
        responseWriter.write(request, response, HttpStatus.UNAUTHORIZED, TOKEN_INVALID_CODE);
    }


    private boolean doesNotStartWithPrefix(String value, String prefix) {
        return !value.regionMatches(true, 0, prefix, 0, prefix.length());
    }

}
