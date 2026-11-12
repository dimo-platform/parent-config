package com.dimo.parent.security.authentication.config;

import lombok.RequiredArgsConstructor;

import com.dimo.parent.observability.MonitoringEndpointAccess;
import com.dimo.parent.security.authentication.filter.JwtAuthenticationFilter;
import com.dimo.parent.security.authentication.service.JwtDecoderProvider;
import com.dimo.parent.security.authentication.web.RestAccessDeniedHandler;
import com.dimo.parent.security.authentication.web.RestAuthenticationEntryPoint;
import com.dimo.parent.security.authentication.web.SecurityResponseWriter;
import com.dimo.parent.security.authorization.AuthoritiesService;
import com.dimo.parent.security.authorization.OwnershipPermissionEvaluator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

/**
 * Shared Spring Security wiring of the microservices. Every request is authenticated from a JWT
 * bearer token validated against the JWK exposed by the auth service; there is no HTTP Basic mode.
 * Method security enforces the {@code <resource>_<action>} permissions, including ownership rules.
 */
@Configuration
@EnableWebSecurity
// @PreAuthorize permission gates on the controllers (authorization domain)
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /// public endpoints
    private static final String[] PUBLIC_POST_ENDPOINTS = {
        "/api/v1/users", "/api/v1/profiles", "/api/v1/auth/token",
        "/api/v1/auth/refresh", "/api/v1/auth/email/challenges", "/api/v1/auth/email/token",
        "/api/v1/auth/password-reset", "/api/v1/auth/phone/challenges", "/api/v1/auth/phone/token",
        "/api/v1/auth/phone/password-reset/verify", "/api/v1/auth/phone/password-reset"
    };

    /// service-to-service endpoints (used by other microservices over OpenFeign)
    private static final String[] INTERNAL_ENDPOINTS = {"/internal/v1/**"};

    /// API documentation endpoints (Swagger UI and the OpenAPI JSON)
    private static final String[] PUBLIC_DOCUMENTATION_ENDPOINTS = {
        "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/docs", "/docs/**"
    };

    // services
    private final JwtDecoderProvider jwtDecoderProvider;
    private final AuthoritiesService authoritiesService;

    // handlers
    private final SecurityResponseWriter responseWriter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Bean
    public AuthenticationTrustResolver authenticationTrustResolver() {
        return new AuthenticationTrustResolverImpl();
    }

    /// ownership-aware permission evaluator used by @PreAuthorize hasPermission(...) expressions
    @Bean
    public PermissionEvaluator permissionEvaluator() {
        return new OwnershipPermissionEvaluator();
    }

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            PermissionEvaluator permissionEvaluator) {
        DefaultMethodSecurityExpressionHandler handler =
                new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(permissionEvaluator);
        return handler;
    }

    /// single stateless filter chain shared by every service
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, AuthenticationTrustResolver authenticationTrustResolver)
            throws Exception {

        // built here (not as a bean) so the servlet container does not register it outside the
        // chain
        JwtAuthenticationFilter jwtAuthenticationFilter =
                new JwtAuthenticationFilter(
                        jwtDecoderProvider,
                        authoritiesService,
                        responseWriter,
                        authenticationTrustResolver);

        http.csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        registry ->
                                registry.requestMatchers(HttpMethod.POST, PUBLIC_POST_ENDPOINTS)
                                        .permitAll()
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/api/v1/community-documents/*",
                                                "/api/v1/auth/methods",
                                                "/api/v1/chat-backgrounds",
                                                "/api/v1/chat-backgrounds/*/content",
                                                "/api/v1/chat-backgrounds/*/thumbnail",
                                                "/api/v1/locations/countries",
                                                "/api/v1/locations/countries/*/cities",
                                                "/api/v1/interests",
                                                "/api/v1/interests/categories")
                                        .permitAll()
                                        .requestMatchers(HttpMethod.GET, "/actuator/health")
                                        .permitAll()
                                        // Metrics: direct loopback/private callers only, never via a proxy.
                                        .requestMatchers(HttpMethod.GET, "/actuator/prometheus")
                                        .access(new MonitoringEndpointAccess())
                                        .requestMatchers(INTERNAL_ENDPOINTS)
                                        .permitAll()
                                        .requestMatchers(PUBLIC_DOCUMENTATION_ENDPOINTS)
                                        .permitAll()
                                        // WebSocket authentication is performed by the handshake
                                        // interceptor.
                                        .requestMatchers("/ws", "/ws/**")
                                        .permitAll()
                                        .requestMatchers("/error")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .exceptionHandling(
                        handling ->
                                handling.authenticationEntryPoint(authenticationEntryPoint)
                                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtAuthenticationFilter, AuthorizationFilter.class);

        return http.build();
    }
}
