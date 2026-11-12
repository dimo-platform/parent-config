package com.dimo.parent.security.authorization;

import com.dimo.parent.security.authorization.domain.AuthenticatedUser;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.io.Serializable;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Permission evaluator following the {@code <RESOURCE>_<ACTION>} convention.
 * <ul>
 *     <li>{@code <RESOURCE>_<ACTION>_ALL} — the user may act on any target.</li>
 *     <li>{@code <RESOURCE>_<ACTION>} — the user may act only on targets they own.</li>
 * </ul>
 * Ownership is established by comparing the target id with the current
 * principal id (the JWT subject), so a user holding {@code USER_UPDATE} can
 * update only his own record while {@code USER_UPDATE_ALL} can update anyone.
 */
public class OwnershipPermissionEvaluator implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication authentication,
                                 Object targetDomainObject,
                                 Object permission) {
        return false;
    }

    @Override
    public boolean hasPermission(Authentication authentication,
                                 Serializable targetId,
                                 String targetType,
                                 Object permission) {

        if (authentication == null || targetId == null || targetType == null || permission == null) {
            return false;
        }

        String resource = targetType;
        String action = permission.toString();

        Set<String> authorities = authorities(authentication);

        if (authorities.contains(resource + "_" + action + "_ALL")) {
            return true;
        }

        if (!authorities.contains(resource + "_" + action)) {
            return false;
        }

        return owns(authentication, targetId);

    }


    private Set<String> authorities(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }


    private boolean owns(Authentication authentication, Object targetId) {

        Object principal = authentication.getPrincipal();

        String ownerId = principal instanceof AuthenticatedUser user
                ? user.getId().toString()
                : authentication.getName();

        return ownerId != null && ownerId.equals(targetId.toString());

    }

}
