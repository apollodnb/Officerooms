package de.officerooms.megastructure;

import de.officerooms.compat.FurnitureBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * 0.36.1: swaps the vanilla stand-ins for furniture in the Hotel and the Neighbourhood (new worlds) for Refurbished
 * Furniture pieces when that mod is installed. Runs in the FEATURES stage, so block entities (drawers, cabinets,
 * crates) are created properly.
 */
final class MegaFurniture {

    static final Direction[] DIRS = {Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH};
    static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private MegaFurniture() {
    }

    interface Mapper {
        /** Replacement at region-local (lx, off, lz), or null to keep the generated block. */
        BlockState map(int lx, int off, int lz);
    }

    /** Refurbished piece whose usable side points to {@code front}, or null when the mod or the block is missing. */
    static BlockState rf(String id, Direction front) {
        return FurnitureBuilder.refurbished(id, front);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static BlockState with(BlockState s, String name, String value) {
        if (s == null) {
            return null;
        }
        Property prop = s.getBlock().getStateDefinition().getProperty(name);
        if (prop != null) {
            java.util.Optional v = prop.getValue(value);
            if (v.isPresent()) {
                return s.setValue(prop, (Comparable) v.get());
            }
        }
        return s;
    }

    static void swap(WorldGenLevel level, ChunkAccess chunk, int baseX, int baseZ, int yBase, int h, Mapper m) {
        if (!FurnitureBuilder.refurbishedAvailable()) {
            return;
        }
        int x0 = chunk.getPos().getMinBlockX();
        int z0 = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int lx = x0 + dx - baseX;
                int lz = z0 + dz - baseZ;
                if (lx <= 2 || lz <= 2 || lx >= 158 || lz >= 158) {
                    continue;
                }
                for (int off = 0; off < h; off++) {
                    BlockState s = m.map(lx, off, lz);
                    if (s == null) {
                        continue;
                    }
                    pos.set(x0 + dx, yBase + off, z0 + dz);
                    level.setBlock(pos, s, 2);
                    if (s.getBlock() instanceof net.minecraft.world.level.block.FenceBlock
                            || s.getBlock().getStateDefinition().getProperty("north") != null) {
                        level.setBlock(pos, Block.updateFromNeighbourShapes(s, level, pos), 2);
                    }
                }
            }
        }
    }

    // --- Hotel -----------------------------------------------------------------------------------------------

    static BlockState hotel(Hotel.Site s, int lx, int off, int lz) {
        Hotel.Part p = s.part(lx, off, lz, false);
        Hotel.Part base = Hotel.dirBase(p);
        int lv = off / 6;
        if (base != null) {
            Direction d = DIRS[p.ordinal() - base.ordinal()];
            return switch (base) {
                case SOFA_0 -> rf(lv >= 2 && lv % 2 == 1 ? "light_blue_sofa" : "red_sofa", d);
                case CHAIR_0 -> rf("dark_oak_chair", d.getOpposite());   // stair FACING = backrest side
                case TOILET_0 -> rf("white_toilet", d.getOpposite());
                default -> null;
            };
        }
        switch (p) {
            case SOFA_S -> {
                return rf("red_sofa", Direction.SOUTH);
            }
            case SOFA_N -> {
                return rf("red_sofa", Direction.NORTH);
            }
            case TABLE -> {
                return rf("dark_oak_table", Direction.NORTH);
            }
            case TABLE_TOP -> {
                return rf("dark_oak_table", Direction.NORTH) != null ? AIR : null;
            }
            case CRATE -> {
                return rf("spruce_crate", Direction.NORTH);
            }
            default -> {
            }
        }
        if (lv < 2 || (p != Hotel.Part.SINK && p != Hotel.Part.TUB && p != Hotel.Part.WARDROBE && p != Hotel.Part.SHELF)) {
            return null;
        }
        int[] c = s.cellOf(lx, lz);
        if (c == null) {
            return null;
        }
        HotelLayout f = s.floor(lv);
        int dd = f.door[c[0]][c[1]];
        int side = (dd + 1) % 4;
        Direction toDoor = DIRS[dd];
        Direction plusU = DIRS[side];
        return switch (p) {
            case SINK -> rf("white_basin", plusU.getOpposite());
            case WARDROBE -> rf("dark_oak_storage_cabinet", plusU);
            case SHELF -> f.kind[c[0]][c[1]] == HotelLayout.Kind.LINEN ? rf("dark_oak_storage_cabinet", toDoor) : null;
            case TUB -> {
                // two blocks along u (u = 2 and 3): bottom at u = 2, head at u = 3
                int u = 5 + (c[2] - 5) * HotelLayout.DX[side] + (c[3] - 5) * HotelLayout.DZ[side];
                yield with(rf("white_bath", plusU.getOpposite()), "type", u == 3 ? "head" : "bottom");
            }
            default -> null;
        };
    }

    // --- Neighbourhood ---------------------------------------------------------------------------------------

    static BlockState hood(Neighbourhood.Site s, int lx, int off, int lz) {
        Neighbourhood.Part p = s.part(lx, off, lz, false);
        return switch (p) {
            case SOFA -> s.inPark(lx, lz) ? null : rf("gray_sofa", Direction.SOUTH);
            case TABLE_LEG -> rf("oak_table", Direction.NORTH);
            case TABLE_TOP -> rf("oak_table", Direction.NORTH) != null ? AIR : null;
            case LAMP -> rf("white_lamp", Direction.NORTH);
            case LANTERN -> s.inPark(lx, lz) ? null : with(rf("white_lamp", Direction.NORTH), "powered", "true");
            case TV -> rf("television", Direction.NORTH);
            case TV_STAND -> rf("oak_drawer", Direction.NORTH);
            case WARDROBE -> rf("oak_storage_cabinet", Direction.WEST);
            case POST -> s.mailboxPost(lx, off, lz) ? rf("oak_mail_box", Direction.NORTH) : null;
            case MAILBOX -> rf("oak_mail_box", Direction.NORTH) != null ? AIR : null;
            case FENCE -> rf("spruce_lattice_fence", Direction.NORTH);
            default -> null;
        };
    }
}
