package com.dimo.parent.security.authorization.action.registry;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

/**
 * Scans this service's {@code @Action} declarations and registers them once the application is
 * ready. A failed attempt is retried in the background so a service never fails to start because
 * the user service is briefly unavailable; until it succeeds unregistered actions fail closed.
 */
public class ActionCatalogRegistration implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(ActionCatalogRegistration.class);
    private static final int MAXIMUM_ATTEMPTS = 60;
    private static final long RETRY_DELAY_MILLIS = 5000;

    private final ActionCatalogScanner scanner;
    private final ActionRegistrar registrar;
    private final String serviceName;


    public ActionCatalogRegistration(ActionCatalogScanner scanner, ActionRegistrar registrar, String serviceName) {
        this.scanner = scanner;
        this.registrar = registrar;
        this.serviceName = serviceName;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        List<ActionDescriptor> actions = scanner.scan();
        if (actions.isEmpty()) {
            return;
        }
        registrar.verifyConfigured();
        if (attempt(actions)) {
            return;
        }
        Thread retry = new Thread(() -> retryUntilRegistered(actions), "action-catalog-registration");
        retry.setDaemon(true);
        retry.start();
    }

    public boolean attempt(List<ActionDescriptor> actions) {
        try {
            ActionRegistrationResultDto result =
                    registrar.register(new ActionRegistrationRequestDto(serviceName, actions));
            LOG.info("Registered {} actions for {} (retired: {})", result.registered(), serviceName, result.retired());
            return true;
        } catch (RuntimeException failure) {
            LOG.warn("Action catalog registration for {} failed: {}", serviceName, failure.getMessage());
            return false;
        }
    }

    private void retryUntilRegistered(List<ActionDescriptor> actions) {
        for (int attempt = 1; attempt < MAXIMUM_ATTEMPTS; attempt++) {
            try {
                Thread.sleep(RETRY_DELAY_MILLIS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
            if (attempt(actions)) {
                return;
            }
        }
        LOG.error("Giving up registering the action catalog for {}", serviceName);
    }
}
