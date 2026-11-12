package com.dimo.parent.security.authorization.action;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/** Preserves source deadlines; bounded storage and striped refresh prevent stampedes. */
public final class SnapshotLeaseCache<K, V extends AuthorizationSnapshot> {

    private final Clock clock;
    private final LongSupplier ticker;
    private final long maximumLeaseMillis;
    private final int maximumEntries;
    private final Object[] locks = new Object[256];
    private final ConcurrentMap<K, Entry<V>> entries = new ConcurrentHashMap<>();


    public SnapshotLeaseCache(Clock clock, LongSupplier ticker, long maximumLeaseMillis, int maximumEntries) {
        this.clock = clock;
        this.ticker = ticker;
        this.maximumLeaseMillis = Math.max(0, Math.min(5000, maximumLeaseMillis));
        this.maximumEntries = Math.max(1, maximumEntries);
        for (int index = 0; index < locks.length; index++) {
            locks[index] = new Object();
        }
    }

    public V get(K key, Supplier<V> loader) {
        Entry<V> cached = entries.get(key);
        if (isFresh(cached)) {
            return cached.snapshot();
        }
        Object lock = locks[Math.floorMod(key.hashCode(), locks.length)];
        synchronized (lock) {
            cached = entries.get(key);
            if (isFresh(cached)) {
                return cached.snapshot();
            }
            long started = ticker.getAsLong();
            V loaded;
            try {
                loaded = loader.get();
            } catch (AuthorizationUnavailableException failure) {
                throw failure;
            } catch (RuntimeException failure) {
                throw new AuthorizationUnavailableException("Authorization source unavailable", failure);
            }
            validate(loaded, cached);
            long leaseNanos = Duration.between(loaded.issuedAt(), loaded.validUntil()).toNanos();
            long sourceDeadline = started + Math.min(leaseNanos, 5_000_000_000L);
            if (ticker.getAsLong() >= sourceDeadline) {
                throw new AuthorizationUnavailableException("Authorization response expired in transit");
            }
            if (maximumLeaseMillis > 0) {
                long localDeadline = started + Math.min(leaseNanos, maximumLeaseMillis * 1_000_000L);
                synchronized (entries) {
                    if (entries.size() >= maximumEntries && !entries.containsKey(key)) {
                        java.util.Iterator<K> keys = entries.keySet().iterator();
                        if (keys.hasNext()) {
                            entries.remove(keys.next());
                        }
                    }
                    entries.put(key, new Entry<>(loaded, localDeadline));
                }
            }
            return loaded;
        }
    }

    public void invalidate(K key) {
        entries.computeIfPresent(key, (ignored, entry) -> new Entry<>(entry.snapshot(), Long.MIN_VALUE));
    }

    public void invalidateAll() {
        entries.replaceAll((key, entry) -> new Entry<>(entry.snapshot(), Long.MIN_VALUE));
    }

    public int size() {
        return entries.size();
    }

    private boolean isFresh(Entry<V> entry) {
        return entry != null && ticker.getAsLong() < entry.deadlineNanos()
                && clock.instant().isBefore(entry.snapshot().validUntil())
                && !entry.snapshot().issuedAt().isAfter(clock.instant().plusSeconds(1));
    }

    private void validate(V snapshot, Entry<V> previous) {
        Instant now = clock.instant();
        if (snapshot == null || snapshot.issuedAt() == null || snapshot.validUntil() == null
                || snapshot.revision() < 0 || !snapshot.issuedAt().isBefore(snapshot.validUntil())
                || Duration.between(snapshot.issuedAt(), snapshot.validUntil()).compareTo(Duration.ofSeconds(5)) > 0
                || !now.isBefore(snapshot.validUntil()) || snapshot.issuedAt().isAfter(now.plusSeconds(1))) {
            throw new AuthorizationUnavailableException("Invalid or expired authorization snapshot");
        }
        if (previous != null && (snapshot.revision() < previous.snapshot().revision()
                || (snapshot.revision() == previous.snapshot().revision()
                && snapshot.issuedAt().isBefore(previous.snapshot().issuedAt())))) {
            throw new AuthorizationUnavailableException("Reordered authorization snapshot");
        }
    }

    private record Entry<V>(V snapshot, long deadlineNanos) {}
}
