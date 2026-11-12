package com.dimo.parent.security.authorization.action;

import com.dimo.parent.security.authorization.AuthoritiesService;
import com.dimo.parent.utils.CustomExceptionUtil;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class DefaultActionAuthorizer implements ActionAuthorizer {

    private final ActionPolicyProvider provider;
    private final AuthoritiesService authorities;
    private final Clock clock;
    private final SnapshotLeaseCache<String, ActionPolicySnapshotDto> policies;


    public DefaultActionAuthorizer(ActionPolicyProvider provider, AuthoritiesService authorities, Clock clock) {
        this.provider = provider;
        this.clock = clock;
        this.authorities = authorities;
        this.policies = new SnapshotLeaseCache<>(clock, System::nanoTime, 5000, 1);
    }

    @Override
    public void require(UUID userId, String actionId) {
        ActionPolicySnapshotDto policy = getPolicy();
        ActionRuleDto rule = getRule(policy, actionId);
        AuthoritySnapshotDto current = getVersionedAuthorities(userId);
        validateDecision(policy, current);
        if (!isPermitted(rule, current)) {
            throw CustomExceptionUtil.withError(rule.denialCode() == null
                    ? "error.authorization.action.denied" : rule.denialCode());
        }
    }

    @Override
    public boolean isAllowed(UUID userId, String actionId) {
        ActionPolicySnapshotDto policy = getPolicy();
        ActionRuleDto rule = getRule(policy, actionId);
        AuthoritySnapshotDto current = getVersionedAuthorities(userId);
        validateDecision(policy, current);
        return isPermitted(rule, current);
    }

    @Override
    public EffectiveCapabilitiesDto getCapabilities(UUID userId) {
        ActionPolicySnapshotDto policy = getPolicy();
        AuthoritySnapshotDto current = getVersionedAuthorities(userId);
        validateDecision(policy, current);
        List<ActionCapabilityDto> capabilities = new ArrayList<>();
        for (ActionRuleDto rule : policy.actions()) {
            capabilities.add(new ActionCapabilityDto(rule.actionId(), isPermitted(rule, current), rule.requiredPermission()));
        }
        Instant deadline = policy.validUntil().isBefore(current.validUntil()) ? policy.validUntil() : current.validUntil();
        return new EffectiveCapabilitiesDto(policy.revision(), current.revision(), deadline, List.copyOf(capabilities));
    }

    private ActionPolicySnapshotDto getPolicy() {
        return policies.get("dimo", () -> {
            ActionPolicySnapshotDto policy = provider.getPolicySnapshot();
            if (policy == null || policy.revision() < 1 || policy.actions() == null) {
                throw new AuthorizationUnavailableException("Missing registered action policy");
            }
            Set<String> ids = new HashSet<>();
            for (ActionRuleDto rule : policy.actions()) {
                if (rule == null || rule.actionId() == null || !"dimo".equals(rule.applicationCode())
                        || !ids.add(rule.actionId())) {
                    throw new AuthorizationUnavailableException("Invalid registered action policy");
                }
            }
            return new ActionPolicySnapshotDto(policy.revision(), policy.issuedAt(), policy.validUntil(),
                    List.copyOf(policy.actions()));
        });
    }

    private ActionRuleDto getRule(ActionPolicySnapshotDto policy, String actionId) {
        for (ActionRuleDto rule : policy.actions()) {
            if (rule.actionId().equals(actionId)) {
                return rule;
            }
        }
        throw new AuthorizationUnavailableException("Action is not registered: " + actionId);
    }

    private AuthoritySnapshotDto getVersionedAuthorities(UUID userId) {
        AuthoritySnapshotDto snapshot = authorities.getSnapshot(userId);
        if (snapshot.revision() < 1) {
            throw new AuthorizationUnavailableException("Versioned authorities are required for action enforcement");
        }
        return snapshot;
    }

    private void validateDecision(ActionPolicySnapshotDto policy, AuthoritySnapshotDto authority) {
        Instant now = clock.instant();
        if (!now.isBefore(policy.validUntil()) || !now.isBefore(authority.validUntil())) {
            throw new AuthorizationUnavailableException("Authorization expired while resolving decision");
        }
    }

    private boolean isPermitted(ActionRuleDto rule, AuthoritySnapshotDto current) {
        return current.roles().contains("SUPER_ADMIN") || rule.requiredPermission() == null
                || current.permissions().contains(rule.requiredPermission());
    }
}
