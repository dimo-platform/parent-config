package com.dimo.parent.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BusinessEventAspectTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();


    @Test
    void count_incrementsSuccessWhenMethodReturns() {
        Sender sender = proxy(new Sender());

        sender.deliver(false);

        assertThat(registry.get("dimo.otp").tag("outcome", "success").tag("channel", "sms").counter().count())
                .isEqualTo(1.0);
    }

    @Test
    void count_incrementsFailureAndRethrowsWhenMethodThrows() {
        Sender sender = proxy(new Sender());

        assertThatThrownBy(() -> sender.deliver(true)).isInstanceOf(IllegalStateException.class);

        assertThat(registry.get("dimo.otp").tag("outcome", "failure").counter().count()).isEqualTo(1.0);
        assertThat(registry.find("dimo.otp").tag("outcome", "success").counter()).isNull();
    }

    @Test
    void count_usesNoneChannelWhenNotDeclared() {
        Sender sender = proxy(new Sender());

        sender.signUp();

        assertThat(registry.get("dimo.signups").tag("channel", "none").counter().count()).isEqualTo(1.0);
    }

    private Sender proxy(Sender target) {
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAspect(new BusinessEventAspect(registry));
        return factory.getProxy();
    }


    static class Sender {

        @CountBusinessEvent(value = BusinessEventType.OTP, channel = "sms")
        public void deliver(boolean fail) {
            if (fail) {
                throw new IllegalStateException("provider down");
            }
        }

        @CountBusinessEvent(BusinessEventType.SIGNUP)
        public void signUp() {
        }
    }
}
