package com.dimo.parent.security.authorization.action;

import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.dimo.parent.security.authorization.action.factory.AuthorizationTestFactory.*;

class SnapshotLeaseCacheTest {

    @Test
    void get_capsLeaseAtVipDeadlineAndNeverRenewsAnUpstreamLease() {
        // Arrange
        Clock clock = mock(Clock.class);
        AtomicReference<Instant> now = new AtomicReference<>(NOW);
        when(clock.instant()).thenAnswer(call -> now.get());
        AtomicLong elapsed = new AtomicLong();
        SnapshotLeaseCache<String, AuthoritySnapshotDto> cache = new SnapshotLeaseCache<>(clock, elapsed::get, 30000, 2);
        AuthoritySnapshotDto snapshot = authority(1, NOW, NOW.plusSeconds(1), Set.of("CUSTOM_READ"));
        assertThat(cache.get("user", () -> snapshot)).isSameAs(snapshot);
        now.set(NOW.plusSeconds(1));
        elapsed.set(1_000_000_000L);

        // Act / Assert
        assertThatThrownBy(() -> cache.get("user", () -> snapshot)).isInstanceOf(AuthorizationUnavailableException.class);
    }

    @Test
    void get_discardsReorderedResponsesAfterExpiryOrInvalidation() {
        // Arrange
        Clock clock = Clock.fixed(NOW, java.time.ZoneOffset.UTC);
        SnapshotLeaseCache<String, AuthoritySnapshotDto> cache = new SnapshotLeaseCache<>(clock, () -> 0L, 5000, 2);
        cache.get("user", () -> authority(3, NOW, NOW.plusSeconds(5), Set.of()));
        cache.invalidate("user");

        // Act / Assert
        assertThatThrownBy(() -> cache.get("user", () -> authority(2, NOW, NOW.plusSeconds(5), Set.of("OLD"))))
                .isInstanceOf(AuthorizationUnavailableException.class);
    }

    @Test
    void get_boundsStorageAndAvoidsRepeatedSourceCallsForWarmEntries() {
        // Arrange
        SnapshotLeaseCache<Integer, AuthoritySnapshotDto> cache = new SnapshotLeaseCache<>(
                Clock.fixed(NOW, java.time.ZoneOffset.UTC), () -> 0L, 5000, 3);
        AtomicInteger loads = new AtomicInteger();

        // Act
        for (int index = 0; index < 10000; index++) {
            cache.get(1, () -> {
                loads.incrementAndGet();
                return authority(1, NOW, NOW.plusSeconds(5), Set.of());
            });
        }
        for (int index = 2; index < 10; index++) {
            cache.get(index, () -> authority(1, NOW, NOW.plusSeconds(5), Set.of()));
        }

        // Assert
        assertThat(loads.get()).isEqualTo(1);
        assertThat(cache.size()).isLessThanOrEqualTo(3);
    }

    @Test
    void get_rejectsResponseThatExpiredDuringSlowFetchAndNeverUsesStaleAllowOnFailure() {
        // Arrange
        AtomicLong elapsed = new AtomicLong();
        SnapshotLeaseCache<String, AuthoritySnapshotDto> cache = new SnapshotLeaseCache<>(
                Clock.fixed(NOW, java.time.ZoneOffset.UTC), elapsed::get, 5000, 3);

        // Act / Assert
        assertThatThrownBy(() -> cache.get("user", () -> {
            elapsed.set(5_000_000_000L);
            return authority(1, NOW, NOW.plusSeconds(5), Set.of("ALLOW"));
        })).isInstanceOf(AuthorizationUnavailableException.class);
        assertThatThrownBy(() -> cache.get("user", () -> { throw new IllegalStateException("Offline"); }))
                .isInstanceOf(AuthorizationUnavailableException.class);
    }
}
