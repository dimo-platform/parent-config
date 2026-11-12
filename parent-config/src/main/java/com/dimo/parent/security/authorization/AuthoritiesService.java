package com.dimo.parent.security.authorization;

import com.dimo.parent.security.authorization.action.AuthoritySnapshotDto;
import com.dimo.parent.security.authorization.action.AuthorizationUnavailableException;
import com.dimo.parent.security.authorization.action.SnapshotLeaseCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.Clock;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthoritiesService {

    private final AuthoritiesProvider provider;
    private final SnapshotLeaseCache<UUID, AuthoritySnapshotDto> cache;


    @Autowired
    public AuthoritiesService(AuthoritiesProvider provider, Clock clock,
            @Value("${security.authorities.cache-ttl-ms:5000}") long ttlMillis) {
        this.provider = provider;
        this.cache = new SnapshotLeaseCache<>(clock, System::nanoTime, ttlMillis, 10000);
    }

    public AuthoritiesService(AuthoritiesProvider provider) {
        this(provider, Clock.systemUTC(), 5000);
    }

    public AuthoritySnapshotDto getSnapshot(UUID userId) {
        return cache.get(userId, () -> {
            AuthoritySnapshotDto snapshot = provider.resolveSnapshot(userId);
            if (snapshot == null || !userId.equals(snapshot.userId())
                    || snapshot.roles() == null || snapshot.permissions() == null
                    || snapshot.permissions().stream().anyMatch(value -> value.startsWith("ROLE_"))) {
                throw new AuthorizationUnavailableException("Invalid authority identity or permissions");
            }
            return new AuthoritySnapshotDto(snapshot.userId(), snapshot.revision(), snapshot.issuedAt(),
                    snapshot.validUntil(), Set.copyOf(snapshot.roles()), Set.copyOf(snapshot.permissions()));
        });
    }

    public Set<String> getAuthorities(UUID userId) {
        AuthoritySnapshotDto snapshot = getSnapshot(userId);
        Set<String> result = new HashSet<>(snapshot.permissions());
        for (String role : snapshot.roles()) {
            result.add("ROLE_" + role);
        }
        return Set.copyOf(result);
    }

    public void invalidate(UUID userId) {
        cache.invalidate(userId);
    }

    public void invalidateAll() {
        cache.invalidateAll();
    }
}
