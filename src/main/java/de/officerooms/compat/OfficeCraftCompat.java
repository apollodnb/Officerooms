package de.officerooms.compat;

import de.officerooms.block.OfficeFurniture;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;

/**
 * Office furniture for the layouts. Up to 0.43.0 this bridged to the optional GameFantom OfficeCraft mod; since 0.44.0
 * the mod has its own office furniture ({@link OfficeFurniture}) and this class only keeps the old entry points, so
 * every layout keeps working unchanged. OfficeCraft is no longer used.
 */
public final class OfficeCraftCompat {

    private OfficeCraftCompat() {
    }

    /** Always true: the furniture is part of the mod. */
    public static boolean isAvailable() {
        return true;
    }

    /** Kept for the setup call; nothing to warm up any more. */
    public static void warmUpCaches() {
    }

    /** Occupied cells relative to the master block (including 0,0,0) for facing. */
    public static List<BlockPos> cells(String id, Direction facing) {
        return OfficeFurniture.cells(id, facing);
    }

    public static boolean place(WorldGenLevel level, BlockPos master, String id, Direction facing, boolean lit) {
        return place(level, master, id, facing, lit, 2);
    }

    public static boolean place(WorldGenLevel level, BlockPos master, String id, Direction facing, boolean lit, int flags) {
        return OfficeFurniture.place(level, master, id, facing, flags);
    }
}
