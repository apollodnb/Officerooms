package de.officerooms.room;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Kleiner, threadsicherer LRU-Cache. Jeder Schlüssel wird genau einmal berechnet; andere Threads,
 * die gleichzeitig denselben Schlüssel brauchen, warten auf dasselbe Ergebnis. Die Berechnung
 * selbst läuft außerhalb der Sperre.
 */
public final class PlanCache<T> {

    private final int capacity;
    private final LinkedHashMap<Long, CompletableFuture<T>> map;

    public PlanCache(int capacity) {
        this.capacity = capacity;
        this.map = new LinkedHashMap<>(capacity * 2, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, CompletableFuture<T>> eldest) {
                return size() > PlanCache.this.capacity && eldest.getValue().isDone();
            }
        };
    }

    /**
     * Bijektive Durchmischung (MurmurHash3-fmix64): Schlüssel bleiben eindeutig, aber benachbarte
     * Regionen ((rx << 32) ^ rz) landen nicht mehr alle im selben Hash-Bucket (Long.hashCode = rx ^ rz).
     */
    private static long mix(long k) {
        k ^= k >>> 33;
        k *= 0xff51afd7ed558ccdL;
        k ^= k >>> 33;
        k *= 0xc4ceb9fe1a85ec53L;
        k ^= k >>> 33;
        return k;
    }

    public T get(long rawKey, Supplier<T> compute) {
        long key = mix(rawKey);
        CompletableFuture<T> f;
        boolean owner = false;
        synchronized (map) {
            f = map.get(key);
            if (f == null) {
                f = new CompletableFuture<>();
                map.put(key, f);
                owner = true;
            }
        }
        if (owner) {
            try {
                f.complete(compute.get());
            } catch (Throwable t) {
                synchronized (map) {
                    map.remove(key);
                }
                f.completeExceptionally(t);
            }
        }
        return f.join();
    }

    int size() {
        synchronized (map) {
            return map.size();
        }
    }
}
