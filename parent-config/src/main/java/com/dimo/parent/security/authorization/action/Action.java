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

    /** English title. Either this or {@link #titleEn()} must be set. */
    String title() default "";

    /** English title; takes precedence over {@link #title()} when both are set. */
    String titleEn() default "";

    String titleFa();

    /** Grouping label for apps and management, e.g. {@code discovery}. */
    String group() default "";

    /** Free actions are bound to the USER role on first registration only. */
    boolean free() default false;

    /** Optional error code returned on denial; defaults to the generic action-denied error. */
    String denialCode() default "";

    /**
     * Optional backing permission name. Defaults to the key; only set it to keep the name of a
     * permission that already exists and is referenced by VIP plans.
     */
    String permission() default "";

    /**
     * Owning service name. Defaults to the registering application's own name; set it only when
     * the catalogue must attribute the action to a different service.
     */
    String service() default "";

    /** Whether the action may be sold through VIP plans and products. */
    boolean productEligible() default true;

    /**
     * Name of an existing permission to link when the action has none yet. Only used with
     * {@code managedPermission = false}.
     */
    String defaultPermission() default "";

    /**
     * {@code true} (default): the registry creates the backing permission, binds free actions to
     * the USER role and retires them when no longer declared. {@code false}: the registry only
     * stores the catalogue entry; permission bindings stay administered in the user service and
     * entries are never retired by registration.
     */
    boolean managedPermission() default true;
}
