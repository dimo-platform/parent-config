package com.dimo.parent.security.authorization.action;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a lockable feature. On a controller method it gates the whole endpoint; on a request
 * parameter, DTO field or record component it gates only requests where that value is present.
 * Every service scans its own annotations at startup and registers them with the user service.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD, ElementType.RECORD_COMPONENT})
public @interface Action {

    /** Stable identifier, e.g. {@code DISCOVERY_NEARBY}. Must never change once released. */
    String key();

    String title();

    String titleFa();

    /** Grouping label for apps and management, e.g. {@code discovery}. */
    String group();

    /** Free actions are bound to the USER role on first registration only. */
    boolean free();

    /** Optional error code returned on denial; defaults to the generic action-denied error. */
    String denialCode() default "";

    /**
     * Optional backing permission name. Defaults to the key; only set it to keep the name of a
     * permission that already exists and is referenced by VIP plans.
     */
    String permission() default "";
}
