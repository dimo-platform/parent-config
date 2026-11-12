package com.dimo.parent.security.authorization.autoconfigure;

import com.dimo.parent.security.authorization.AuthoritiesService;
import com.dimo.parent.security.authorization.action.DefaultActionAuthorizer;
import com.dimo.parent.security.authorization.action.ActionAuthorizer;
import com.dimo.parent.security.authorization.action.ActionPolicyProvider;
import com.dimo.parent.security.authorization.action.registry.ActionCatalogRegistration;
import com.dimo.parent.security.authorization.action.registry.ActionCatalogScanner;
import com.dimo.parent.security.authorization.action.registry.ActionEnforcementAspect;
import com.dimo.parent.security.authorization.action.registry.ActionRegistrar;
import com.dimo.parent.client.authorization.ActionRegistryClient;
import com.dimo.parent.security.authorization.action.registry.ActionRequirementResolver;
import com.dimo.parent.security.authorization.action.registry.FeignActionRegistrar;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import java.time.Clock;
import com.dimo.parent.client.autoconfigure.CommonClientsAutoConfiguration;
import com.dimo.parent.client.authorization.AuthorizationClient;
import com.dimo.parent.security.authorization.AuthoritiesProvider;
import com.dimo.parent.security.authorization.FeignAuthoritiesProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Provides the default {@link AuthoritiesProvider} backed by the auth service.
 * Services owning the role/permission tables (the user service) declare their
 * own provider bean, which backs this default off.
 */
@AutoConfiguration
@AutoConfigureAfter(CommonClientsAutoConfiguration.class)
public class CommonAuthorizationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AuthoritiesProvider.class)
    public AuthoritiesProvider feignAuthoritiesProvider(AuthorizationClient authorizationClient) {
        return new FeignAuthoritiesProvider(authorizationClient);
    }

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock authorizationClock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnMissingBean(ActionPolicyProvider.class)
    public ActionPolicyProvider actionPolicyProvider(
            AuthorizationClient client) {
        return client::getPolicySnapshot;
    }

    @Bean
    @ConditionalOnMissingBean(ActionAuthorizer.class)
    public ActionAuthorizer actionAuthorizer(
            ActionPolicyProvider provider,
            AuthoritiesService authorities, Clock clock) {
        return new DefaultActionAuthorizer(provider, authorities, clock);
    }

    @Bean
    @ConditionalOnMissingBean(ActionRequirementResolver.class)
    public ActionRequirementResolver actionRequirementResolver() {
        return new ActionRequirementResolver();
    }

    @Bean
    @ConditionalOnMissingBean(ActionEnforcementAspect.class)
    public ActionEnforcementAspect actionEnforcementAspect(
            ActionAuthorizer authorizer,
            ActionRequirementResolver resolver) {
        return new ActionEnforcementAspect(authorizer, resolver);
    }

    @Bean
    @ConditionalOnMissingBean(ActionCatalogScanner.class)
    public ActionCatalogScanner actionCatalogScanner(ApplicationContext context) {
        return new ActionCatalogScanner(context);
    }

    @Bean
    @ConditionalOnMissingBean(ActionRegistrar.class)
    public ActionRegistrar actionRegistrar(
            ActionRegistryClient client,
            @Value("${user.action-registry-key:}")
            String serviceKey) {
        return new FeignActionRegistrar(client, serviceKey);
    }

    @Bean
    @ConditionalOnProperty(name = "authorization.action-registry.auto-register", havingValue = "true", matchIfMissing = true)
    public ActionCatalogRegistration actionCatalogRegistration(
            ActionCatalogScanner scanner,
            ActionRegistrar registrar,
            @Value("${spring.application.name:unknown}")
            String serviceName) {
        return new ActionCatalogRegistration(scanner, registrar, serviceName);
    }
}
