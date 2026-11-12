package com.dimo.parent.security.authorization.action;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** An expired/unavailable snapshot must never be treated as a confirmed unrestricted action. */
public class AuthorizationUnavailableException extends ResponseStatusException {

    public AuthorizationUnavailableException(String reason) {
        super(HttpStatus.SERVICE_UNAVAILABLE, reason);
    }

    public AuthorizationUnavailableException(String reason, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, reason, cause);
    }
}
