package com.dimo.parent.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import com.dimo.parent.logging.definition.LoggingProperties;
import com.dimo.parent.logging.definition.LoggingUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

@Aspect
public class LoggingAspect {

    @Autowired
    private LoggingProperties properties;


    @Around("@annotation(com.dimo.parent.logging.LogDebug) || @within(com.dimo.parent.logging.LogDebug)")
    public Object debug(ProceedingJoinPoint joinPoint) throws Throwable {

        if (!properties.logLevel().equalsIgnoreCase("debug")) {
            return joinPoint.proceed();
        }

        LogDebug annotation = getAnnotation(joinPoint, LogDebug.class);

        String className = joinPoint.getTarget()
                .getClass()
                .getSimpleName();

        String methodName = joinPoint.getSignature()
                .getName();

        String message = annotation.message().isBlank()
                ? "Executing method"
                : annotation.message();

        Object data = annotation.logArgs()
                ? getArguments(joinPoint)
                : null;

        LoggingUtil.debug(
                properties.name(),
                className,
                methodName,
                message,
                getUserId(),
                data
        );

        Object result = joinPoint.proceed();

        if (annotation.logResult()) {

            LoggingUtil.debug(
                    properties.name(),
                    className,
                    methodName,
                    "Method completed",
                    getUserId(),
                    result
            );
        }

        return result;
    }


    @Around("@annotation(com.dimo.parent.logging.LogError) || @within(com.dimo.parent.logging.LogError)")
    public Object error(ProceedingJoinPoint joinPoint) throws Throwable {

        if (!properties.logLevel().equalsIgnoreCase("error")) {
            return joinPoint.proceed();
        }

        LogError annotation = getAnnotation(joinPoint, LogError.class);

        String className = joinPoint.getTarget()
                .getClass()
                .getSimpleName();

        String methodName = joinPoint.getSignature()
                .getName();

        String message = annotation.message().isBlank()
                ? "Method execution failed"
                : annotation.message();

        Object data = annotation.logArgs()
                ? getArguments(joinPoint)
                : null;

        try {

            return joinPoint.proceed();

        } catch (Throwable throwable) {

            LoggingUtil.error(
                    properties.name(),
                    className,
                    methodName,
                    message,
                    getUserId(),
                    data,
                    throwable
            );

            throw throwable;
        }
    }


    private Object[] getArguments(ProceedingJoinPoint joinPoint) {
        return joinPoint.getArgs();
    }


    private String getUserId() {
        // TODO: Extract from SecurityContext
        return null;
    }


    private <T extends Annotation> T getAnnotation(ProceedingJoinPoint joinPoint, Class<T> annotationClass) {

        MethodSignature signature =
                (MethodSignature) joinPoint.getSignature();

        Method method = signature.getMethod();

        T annotation = method.getAnnotation(annotationClass);
        if (annotation != null) {
            return annotation;
        }

        return joinPoint.getTarget().getClass().getAnnotation(annotationClass);
    }
}
