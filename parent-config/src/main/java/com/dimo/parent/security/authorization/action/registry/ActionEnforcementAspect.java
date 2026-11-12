package com.dimo.parent.security.authorization.action.registry;

import com.dimo.parent.security.authorization.action.ActionAuthorizer;
import com.dimo.parent.security.authorization.domain.AuthenticatedUser;
import com.dimo.parent.utils.CustomExceptionUtil;
import java.lang.reflect.Method;
import java.util.Set;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Enforces {@code @Action} declarations on controllers through the existing {@link ActionAuthorizer}
 * (authority snapshot path), before the controller body runs.
 */
@Aspect
public class ActionEnforcementAspect {

    private final ActionAuthorizer authorizer;
    private final ActionRequirementResolver resolver;


    public ActionEnforcementAspect(ActionAuthorizer authorizer, ActionRequirementResolver resolver) {
        this.authorizer = authorizer;
        this.resolver = resolver;
    }

    @Around("@within(org.springframework.web.bind.annotation.RestController)"
            + " || @within(org.springframework.stereotype.Controller)")
    public Object enforce(ProceedingJoinPoint joinPoint) throws Throwable {
        Method declared = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Method method = AopUtils.getMostSpecificMethod(declared, joinPoint.getTarget().getClass());
        Set<String> keys = resolver.resolve(method, joinPoint.getArgs());
        if (!keys.isEmpty()) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
                throw CustomExceptionUtil.withError("error.authorization.action.denied");
            }
            for (String key : keys) {
                authorizer.require(user.getId(), key);
            }
        }
        return joinPoint.proceed();
    }
}
