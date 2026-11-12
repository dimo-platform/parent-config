package com.dimo.parent.security.authorization.action;



public record ActionRuleDto(
        String actionId,
        String applicationCode,
        String service,
        String titleFa,
        String titleEn,
        boolean productEligible,
        String requiredPermission,
        String denialCode) {}
