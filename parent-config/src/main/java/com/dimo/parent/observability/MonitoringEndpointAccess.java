package com.dimo.parent.observability;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.function.Supplier;

import org.springframework.security.core.Authentication;

/// Allows the Prometheus endpoint only for direct loopback or private-network callers that did not
/// pass through a proxy. Nginx already answers 404 for /actuator; this is the second lock.
public class MonitoringEndpointAccess implements AuthorizationManager<RequestAuthorizationContext> {

    private static final String FORWARDED_FOR = "X-Forwarded-For";
    private static final String FORWARDED = "Forwarded";


    @Override
    public AuthorizationDecision check(
            Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        return new AuthorizationDecision(isInternal(context.getRequest()));
    }

    boolean isInternal(HttpServletRequest request) {
        if (request.getHeader(FORWARDED_FOR) != null || request.getHeader(FORWARDED) != null) {
            return false;
        }
        String remote = request.getRemoteAddr();
        if (remote == null || remote.isBlank()) {
            return false;
        }
        try {
            InetAddress address = InetAddress.getByName(remote);
            return address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress();
        } catch (UnknownHostException invalid) {
            return false;
        }
    }
}
