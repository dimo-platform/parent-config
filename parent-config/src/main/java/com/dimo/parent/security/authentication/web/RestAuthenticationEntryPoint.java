package com.dimo.parent.security.authentication.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 401 entry point of the security domain: answers unauthenticated requests
 * with the standard JSON envelope and a {@code WWW-Authenticate: Bearer}
 * challenge.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String SECURITY_UNAUTHENTICATED_CODE = "error.security.unauthenticated";

    // handlers
    private final SecurityResponseWriter responseWriter;


    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");

        responseWriter.write(request, response, HttpStatus.UNAUTHORIZED, SECURITY_UNAUTHENTICATED_CODE);

    }

}
