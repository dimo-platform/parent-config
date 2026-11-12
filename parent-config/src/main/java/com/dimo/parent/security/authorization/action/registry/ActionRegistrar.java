package com.dimo.parent.security.authorization.action.registry;

/** Sends a service catalog to the owner of the action registry. */
public interface ActionRegistrar {

    /** Fails startup when the registrar cannot work (e.g. missing secret); never register with an empty key. */
    default void verifyConfigured() {}

    ActionRegistrationResultDto register(ActionRegistrationRequestDto request);
}
