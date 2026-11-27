package de.officerooms.megastructure;

import de.officerooms.core.Hashing;
import de.officerooms.room.OfficePlanner;
import de.officerooms.room.Room;

/**
 * 0.8.0: Shared region assignment for all megastructures. A region holds at most one megastructure.
 *
 * <p>The Poolrooms keep their own hash stream (unchanged since 0.6.0). Regions without Poolrooms roll
 * a second hash: below {@code chance} they become a Neighbourhood, between {@code chance} and
 * {@code 2 × chance} they become The Maze. All three use the same chance
 * ({@code poolrooms_region_chance}, default 0.07 since 0.8.3), so each megastructure is exactly as rare as the
 * Poolrooms.</p>
 */
public final class Megastructures {

    public enum Kind {
        NONE, POOLROOMS, NEIGHBOURHOOD, MAZE, HOTEL, INDUSTRIAL, LEVEL_FUN, CINEMA, MALL
    }

    private Megastructures() {
    }

    /** Cheap region kind from the hashes alone (does not build anything). */
    public static Kind kindOf(OfficePlanner p, int rx, int rz) {
        if (p.regionBlocks != 160 || p.phys != 6) {
            return Kind.NONE;
        }
        double c = p.cfg.special().poolroomsRegionChance();
        if (Hashing.unit(Hashing.hash(p.seed, rx, rz, 0x9001, 0)) < c) {
            return Kind.POOLROOMS;
        }
        double u = Hashing.unit(Hashing.hash(p.seed, rx, rz, 0x9101, 0));
        if (u < c) {
            return Kind.NEIGHBOURHOOD;
        }
        if (u < 2 * c) {
            return Kind.MAZE;
        }
        if (u < 3 * c) {
            return Kind.HOTEL;                // 0.14.0
        }
        if (u < 4 * c) {
            return Kind.INDUSTRIAL;           // 0.15.0
        }
        if (u < 5 * c) {
            return Kind.LEVEL_FUN;            // 0.16.0
        }
        if (u < 6 * c && p.cfg.modern()) {
            return Kind.CINEMA;               // 0.31.0, new worlds only
        }
        if (u < 7 * c && p.cfg.v3()) {
            return Kind.MALL;                 // 0.43.0, worlds from 0.43.0 on
        }
        return Kind.NONE;
    }

    /** 0.41.0 (/locate markers): entrance point {x, y, z} of the megastructure of this kind in region (rx, rz), or null if the region has none. */
    public static int[] anchor(OfficePlanner p, Megastructures.Kind kind, int rx, int rz) {
        if (kind == Kind.NONE || kindOf(p, rx, rz) != kind) {
            return null;
        }
        int bx = rx * p.regionBlocks;
        int bz = rz * p.regionBlocks;
        switch (kind) {
            case POOLROOMS -> {
                var s = p.pools.site(rx, rz);
                return s == null ? null : new int[]{bx + s.hallX, s.yBase + de.officerooms.megastructure.Poolrooms.FLOOR + 1, bz + s.hallZ};
            }
            case NEIGHBOURHOOD -> {
                var s = p.hood.site(rx, rz);
                return s == null ? null : new int[]{bx + 80, s.yBase + 1, bz + 80};
            }
            case MAZE -> {
                var s = p.maze.site(rx, rz);
                return s == null ? null : new int[]{bx + 80, s.yBase + 1, bz + 80};
            }
            case HOTEL -> {
                var s = p.hotel.site(rx, rz);
                return s == null ? null : new int[]{bx + 72, s.yBase + 7, bz + 68};
            }
            case INDUSTRIAL -> {
                var s = p.industrial.site(rx, rz);
                return s == null ? null : new int[]{bx + s.halls[0][0] * 16 + 4, s.yBase + 1, bz + s.halls[0][1] * 16 + 4};
            }
            case LEVEL_FUN -> {
                var s = p.fun.site(rx, rz);
                return s == null ? null : new int[]{bx + 80, s.yBase + 1, bz + 80};
            }
            case CINEMA -> {
                var s = p.cinema.site(rx, rz);
                return s == null ? null : new int[]{bx + 80, s.yBase + 1, bz + 84};
            }
            case MALL -> {
                var s = p.mall.site(rx, rz);
                return s == null ? null : new int[]{bx + 80, s.yBase + 1, bz + 100};
            }
            default -> {
                return null;
            }
        }
    }


    /** Storey range shared by all megastructures: away from the dark exit storeys and the roof. */
    static int[] storeyRange(OfficePlanner p, int storeys) {
        int lo = Math.max(p.cfg.special().darkBottomStoreys() + 4, 8);
        int hi = p.storeys - storeys - 8;
        return new int[]{lo, hi};
    }

    /** Whether a room overlaps the storey band [s0, s0 + n - 1]. */
    static boolean overlapsBand(Room r, int s0, int n) {
        int b = r.kind == Room.Kind.ATRIUM && r.atrium != null ? r.atrium.s0 : r.bottom;
        int t = r.kind == Room.Kind.ATRIUM && r.atrium != null ? r.atrium.top() : r.top();
        return t >= s0 && b <= s0 + n - 1;
    }
}
