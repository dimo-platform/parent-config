package com.dimo.parent.observability;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
@RequiredArgsConstructor
public class BusinessEventAspect {

    static final String OUTCOME_TAG = "outcome";
    static final String CHANNEL_TAG = "channel";
    static final String SUCCESS = "success";
    static final String FAILURE = "failure";

    private final MeterRegistry registry;


    @Around("@annotation(countBusinessEvent)")
    public Object count(ProceedingJoinPoint joinPoint, CountBusinessEvent countBusinessEvent) throws Throwable {
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable throwable) {
            increment(countBusinessEvent, FAILURE);
            throw throwable;
        }
        increment(countBusinessEvent, SUCCESS);
        return result;
    }

    private void increment(CountBusinessEvent event, String outcome) {
        try {
            BusinessEventType type = event.value();
            registry.counter(
                            type.getMeterName(),
                            OUTCOME_TAG, outcome,
                            CHANNEL_TAG, event.channel().isBlank() ? "none" : event.channel())
                    .increment();
        } catch (RuntimeException ignored) {
            // Metrics must never break business logic.
        }
    }
}
