package com.dimo.parent.security.authorization.action.registry;

public record ActionDescriptor(
        String key,
        String title,
        String titleFa,
        String group,
        boolean free,
        String denialCode,
        String permission) {}
