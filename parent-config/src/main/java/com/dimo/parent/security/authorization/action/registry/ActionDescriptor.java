package com.dimo.parent.security.authorization.action.registry;

/**
 * Registration payload of one {@code @Action}. The trailing fields were added in 1.1.0; a payload
 * from an older client leaves them null, which normalizes to the 1.0.0 behaviour.
 */
public record ActionDescriptor(
        String key,
        String title,
        String titleFa,
        String group,
        boolean free,
        String denialCode,
        String permission,
        String service,
        Boolean productEligible,
        String defaultPermission,
        Boolean managedPermission) {

    public ActionDescriptor {
        productEligible = productEligible == null || productEligible;
        managedPermission = managedPermission == null || managedPermission;
    }

    /** 1.0.0-shaped descriptor: managed permission, product eligible, service of the registrar. */
    public ActionDescriptor(
            String key, String title, String titleFa, String group, boolean free, String denialCode,
            String permission) {
        this(key, title, titleFa, group, free, denialCode, permission, null, true, null, true);
    }
}
