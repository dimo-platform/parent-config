package com.dimo.parent.security.authorization.action;

import java.time.Instant;

public interface AuthorizationSnapshot {

    long revision();

    Instant issuedAt();

    Instant validUntil();
}
