package de.officerooms.core;

/**
 * Deterministische Hash- und Zufallshilfen.
 *
 * <p>Die gesamte Weltgenerierung leitet jeden Zufall aus {@code hash(weltSeed, ...)} ab.
 * Dadurch berechnet jeder Chunk – egal in welcher Reihenfolge und in welchem Thread – exakt
 * dieselbe Raumplanung wie seine Nachbarn.</p>
 */
public final class Hashing {

    private static final long GOLDEN = 0x9E3779B97F4A7C15L;

    private Hashing() {
    }

    /** SplitMix64-Finalizer: gute Bitverteilung, sehr schnell. */
    public static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    public static long hash(long seed, long a) {
        return mix(mix(seed) ^ (a * GOLDEN));
    }

    public static long hash(long seed, long a, long b) {
        return mix(hash(seed, a) ^ (b * GOLDEN));
    }

    public static long hash(long seed, long a, long b, long c) {
        return mix(hash(seed, a, b) ^ (c * GOLDEN));
    }

    public static long hash(long seed, long a, long b, long c, long d) {
        return mix(hash(seed, a, b, c) ^ (d * GOLDEN));
    }

    /** Hash als Zahl in [0, 1). */
    public static double unit(long h) {
        return (h >>> 11) * 0x1.0p-53;
    }

    /** Kleiner, deterministischer Zufallsgenerator (SplitMix64). Nicht threadsicher, pro Aufgabe neu anlegen. */
    public static final class Rng {
        private long state;

        public Rng(long seed) {
            this.state = seed;
        }

        public long nextLong() {
            state += GOLDEN;
            return mix(state);
        }

        public int nextInt(int bound) {
            if (bound <= 1) {
                return 0;
            }
            return (int) Long.remainderUnsigned(nextLong(), bound);
        }

        /** Ganzzahl in [min, max] (beide inklusive). */
        public int range(int min, int max) {
            if (max <= min) {
                return min;
            }
            return min + nextInt(max - min + 1);
        }

        public double nextDouble() {
            return unit(nextLong());
        }

        public boolean chance(double p) {
            return p > 0 && nextDouble() < p;
        }
    }
}
