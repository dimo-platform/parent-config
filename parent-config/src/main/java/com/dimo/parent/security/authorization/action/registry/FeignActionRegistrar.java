package com.dimo.parent.security.authorization.action.registry;

import com.dimo.parent.client.authorization.ActionRegistryClient;

/** Registers the catalog with the user service using the shared registry service key. */
public class FeignActionRegistrar implements ActionRegistrar {

    private static final int MINIMUM_KEY_LENGTH = 32;

    private final ActionRegistryClient client;
    private final String serviceKey;


    public FeignActionRegistrar(ActionRegistryClient client, String serviceKey) {
        this.client = client;
        this.serviceKey = serviceKey;
    }

    @Override
    public void verifyConfigured() {
        if (serviceKey == null || serviceKey.length() < MINIMUM_KEY_LENGTH) {
            throw new IllegalStateException("Secret user.action-registry-key is missing or shorter than "
                    + MINIMUM_KEY_LENGTH + " characters; this service declares @Action and cannot register");
        }
    }

    @Override
    public ActionRegistrationResultDto register(ActionRegistrationRequestDto request) {
        return client.register(serviceKey, request);
    }
}
