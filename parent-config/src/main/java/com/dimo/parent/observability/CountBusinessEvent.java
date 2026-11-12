package com.dimo.parent.observability;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Counts one business event per call of a Spring bean method: `outcome=success` when the
/// method returns, `outcome=failure` when it throws. Only the event, outcome and the optional
/// low-cardinality channel become tags; arguments and results are never inspected.
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CountBusinessEvent {

    BusinessEventType value();

    /// Low-cardinality channel such as `sms`, `email` or `password`; empty means none.
    String channel() default "";
}
