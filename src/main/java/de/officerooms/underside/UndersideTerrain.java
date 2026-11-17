package de.officerooms.underside;

import de.officerooms.block.ModBlocks;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 0.19.0: the Underside. An endless meadow (floor around y 40–42) under an upside-down meadow (ceiling bottom around
 * y 53–55) with hanging trees. Pure functions of x/z, so the boss, the arrival code and the generator agree.
 */
public final class UndersideTerrain {

    public static final int FLOOR = 40;
    public static final int CEIL = 53;
    public static final int HEIGHT = 128;
    static final int TREE_CELL = 10;
    static final int HANG_CELL = 12;

    private UndersideTerrain() {
    }

    static double hash(long x, long z, long salt) {
        long h = x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL ^ salt * 0x165667B19E3779F9L;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        h *= 0x94D049BB133111EBL;
        h ^= h >>> 32;
        return (h >>> 11) * 0x1.0p-53;
    }

    private static double noise(double x, double z, int salt) {
        int xi = (int) Math.floor(x);
        int zi = (int) Math.floor(z);
        double fx = x - xi;
        double fz = z - zi;
        double u = fx * fx * (3 - 2 * fx);
        double v = fz * fz * (3 - 2 * fz);
        double a = hash(xi, zi, salt), b = hash(xi + 1, zi, salt), c = hash(xi, zi + 1, salt), d = hash(xi + 1, zi + 1, salt);
        return (a + (b - a) * u) * (1 - v) + (c + (d - c) * u) * v;
    }

    /** Y of the top grass block of the meadow (0–2 blocks of relief). */
    public static int floor(int x, int z) {
        double n = noise(x / 15.0, z / 15.0, 1) * 0.7 + noise(x / 6.0, z / 6.0, 2) * 0.3;
        return FLOOR + Math.min(2, (int) Math.floor(n * 3));
    }

    /** Y of the lowest block of the upside-down meadow. */
    public static int ceil(int x, int z) {
        double n = noise(x / 15.0, z / 15.0, 3) * 0.7 + noise(x / 6.0, z / 6.0, 4) * 0.3;
        return CEIL + Math.min(2, (int) Math.floor(n * 3));
    }

    // --- trees -----------------------------------------------------------------------------------------------
    /** {x, z, height} of the standing tree in cell (cx, cz), or null. */
    static int[] tree(int cx, int cz) {
        if (hash(cx, cz, 5) > 0.42) {
            return null;
        }
        return new int[]{cx * TREE_CELL + 2 + (int) (hash(cx, cz, 6) * 6), cz * TREE_CELL + 2 + (int) (hash(cx, cz, 7) * 6), 4 + (hash(cx, cz, 8) < 0.5 ? 0 : 1)};
    }

    /** {x, z, length} of the hanging tree in cell (cx, cz), or null. */
    static int[] hang(int cx, int cz) {
        if (hash(cx, cz, 9) > 0.38) {
            return null;
        }
        return new int[]{cx * HANG_CELL + 3 + (int) (hash(cx, cz, 10) * 6), cz * HANG_CELL + 3 + (int) (hash(cx, cz, 11) * 6), 3 + (hash(cx, cz, 12) < 0.5 ? 0 : 1)};
    }

    /** True if a hanging tree touches this column (arrival avoids those). */
    public static boolean hangingAt(int x, int z) {
        int cx = Math.floorDiv(x, HANG_CELL), cz = Math.floorDiv(z, HANG_CELL);
        for (int i = cx - 1; i <= cx + 1; i++) {
            for (int k = cz - 1; k <= cz + 1; k++) {
                int[] t = hang(i, k);
                if (t != null && Math.abs(t[0] - x) <= 2 && Math.abs(t[1] - z) <= 2) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Fills one column (index = y). */
    public static void column(int x, int z, BlockState[] col) {
        BlockState air = Blocks.AIR.defaultBlockState();
        java.util.Arrays.fill(col, air);
        int f = floor(x, z);
        int c = ceil(x, z);
        BlockState stone = Blocks.STONE.defaultBlockState(), dirt = Blocks.DIRT.defaultBlockState(), bed = Blocks.BEDROCK.defaultBlockState();
        col[0] = bed;
        for (int y = 1; y < f - 3; y++) {
            col[y] = stone;
        }
        for (int y = f - 3; y < f; y++) {
            col[y] = dirt;
        }
        col[f] = Blocks.GRASS_BLOCK.defaultBlockState();
        col[c] = ModBlocks.UNDERSIDE_GRASS.get().defaultBlockState();
        for (int y = c + 1; y <= c + 3; y++) {
            col[y] = dirt;
        }
        for (int y = c + 4; y < HEIGHT - 2; y++) {
            col[y] = stone;
        }
        col[HEIGHT - 2] = bed;
        col[HEIGHT - 1] = bed;
        BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        BlockState log = Blocks.OAK_LOG.defaultBlockState();
        boolean busyFloor = false, busyCeil = false;
        int cx = Math.floorDiv(x, TREE_CELL), cz = Math.floorDiv(z, TREE_CELL);
        for (int i = cx - 1; i <= cx + 1; i++) {
            for (int k = cz - 1; k <= cz + 1; k++) {
                int[] t = tree(i, k);
                if (t == null) {
                    continue;
                }
                int dx = Math.abs(t[0] - x), dz = Math.abs(t[1] - z);
                if (dx > 2 || dz > 2) {
                    continue;
                }
                busyFloor = true;
                int base = floor(t[0], t[1]) + 1;
                int top = base + t[2] - 1;
                for (int y = top - 1; y <= top + 1; y++) {
                    int r = y == top + 1 ? 1 : 2;
                    if (dx <= r && dz <= r && !(dx == 2 && dz == 2) && y < c && col[y] == air) {
                        col[y] = leaves;
                    }
                }
                if (dx == 0 && dz == 0) {
                    for (int y = base; y <= top; y++) {
                        col[y] = log;
                    }
                }
            }
        }
        cx = Math.floorDiv(x, HANG_CELL);
        cz = Math.floorDiv(z, HANG_CELL);
        for (int i = cx - 1; i <= cx + 1; i++) {
            for (int k = cz - 1; k <= cz + 1; k++) {
                int[] t = hang(i, k);
                if (t == null) {
                    continue;
                }
                int dx = Math.abs(t[0] - x), dz = Math.abs(t[1] - z);
                if (dx > 2 || dz > 2) {
                    continue;
                }
                busyCeil = true;
                int base = ceil(t[0], t[1]) - 1;
                int bottom = base - t[2] + 1;
                for (int y = bottom - 1; y <= bottom + 1; y++) {
                    int r = y == bottom - 1 ? 1 : 2;
                    if (dx <= r && dz <= r && !(dx == 2 && dz == 2) && y > f + 1 && col[y] == air) {
                        col[y] = leaves;
                    }
                }
                if (dx == 0 && dz == 0) {
                    for (int y = bottom; y <= base; y++) {
                        col[y] = log;
                    }
                }
            }
        }
        double h = hash(x, z, 13);
        if (!busyFloor && col[f + 1] == air) {
            if (h < 0.03) {
                col[f + 1] = (hash(x, z, 14) < 0.5 ? Blocks.DANDELION : Blocks.POPPY).defaultBlockState();
            } else if (h < 0.28) {
                col[f + 1] = Blocks.GRASS.defaultBlockState();
            }
        }
        if (!busyCeil && col[c - 1] == air && hash(x, z, 15) < 0.07) {
            col[c - 1] = Blocks.HANGING_ROOTS.defaultBlockState();
        }
    }
}
