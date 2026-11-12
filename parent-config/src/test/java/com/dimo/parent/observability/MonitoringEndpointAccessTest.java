package com.dimo.parent.observability;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class MonitoringEndpointAccessTest {

    private final MonitoringEndpointAccess access = new MonitoringEndpointAccess();


    @Test
    void isInternal_allowsLoopbackAndPrivateNetworkCallers() {
        assertThat(access.isInternal(requestFrom("127.0.0.1"))).isTrue();
        assertThat(access.isInternal(requestFrom("172.18.0.5"))).isTrue();
        assertThat(access.isInternal(requestFrom("10.8.0.2"))).isTrue();
    }

    @Test
    void isInternal_rejectsPublicCallers() {
        assertThat(access.isInternal(requestFrom("203.0.113.9"))).isFalse();
    }

    @Test
    void isInternal_rejectsProxiedRequestsEvenFromLoopback() {
        MockHttpServletRequest request = requestFrom("127.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.9");

        assertThat(access.isInternal(request)).isFalse();
    }

    private MockHttpServletRequest requestFrom(String address) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(address);
        return request;
    }
}
