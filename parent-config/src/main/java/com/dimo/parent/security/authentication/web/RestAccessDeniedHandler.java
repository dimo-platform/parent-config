package com.dimo.parent.security.authentication.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 403 denied handler of the security domain: answers authenticated-but-not
 * allowed requests with the standard JSON envelope, no stack traces.
 */
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private static final String SECURITY_FORBIDDEN_CODE = "error.security.forbidden";

    // handlers
    private final SecurityResponseWriter responseWriter;


    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        responseWriter.write(request, response, HttpStatus.FORBIDDEN, SECURITY_FORBIDDEN_CODE);

    }

}
