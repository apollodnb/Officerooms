package de.officerooms.megastructure;

import de.officerooms.block.DecorBlock;
import de.officerooms.block.ModBlocks;
import de.officerooms.room.OfficePlanner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.registries.RegistryObject;

/** 0.15.0: block states of the Industrial Complex and its decoration (chests, signs). */
public final class IndustrialBlocks {

    private IndustrialBlocks() {
    }

    private static BlockState d(RegistryObject<Block> b, Direction f) {
        return b.get().defaultBlockState().setValue(DecorBlock.FACING, f);
    }

    private static BlockState door(Direction f, boolean open, boolean upper) {
        return ModBlocks.GRATE_DOOR.get().defaultBlockState().setValue(DoorBlock.FACING, f).setValue(DoorBlock.OPEN, open)
                .setValue(DoorBlock.HALF, upper ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER);
    }

    private static BlockState pipe(Direction.Axis a) {
        return ModBlocks.PIPE.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, a);
    }

    public static BlockState state(Industrial.Part part) {
        return switch (part) {
            case AIR, OFFICE -> Blocks.AIR.defaultBlockState();
            case WALL -> ModBlocks.INDUSTRIAL_WALL.get().defaultBlockState();
            case FLOOR -> ModBlocks.INDUSTRIAL_FLOOR.get().defaultBlockState();
            case CEIL -> ModBlocks.INDUSTRIAL_CEILING.get().defaultBlockState();
            case GRATE -> ModBlocks.GRATE.get().defaultBlockState();
            case RAIL -> Blocks.IRON_BARS.defaultBlockState();
            case PIPE_X -> pipe(Direction.Axis.X);
            case PIPE_Y -> pipe(Direction.Axis.Y);
            case PIPE_Z -> pipe(Direction.Axis.Z);
            case LAMP -> d(ModBlocks.CAGE_LAMP, Direction.NORTH);
            case LAMP_BROKEN -> d(ModBlocks.CAGE_LAMP_BROKEN, Direction.NORTH);
            case DOOR_X_LO -> door(Direction.EAST, false, false);
            case DOOR_X_HI -> door(Direction.EAST, false, true);
            case DOOR_X_OPEN_LO -> door(Direction.EAST, true, false);
            case DOOR_X_OPEN_HI -> door(Direction.EAST, true, true);
            case DOOR_Z_LO -> door(Direction.SOUTH, false, false);
            case DOOR_Z_HI -> door(Direction.SOUTH, false, true);
            case DOOR_Z_OPEN_LO -> door(Direction.SOUTH, true, false);
            case DOOR_Z_OPEN_HI -> door(Direction.SOUTH, true, true);
            case STAIR -> Blocks.POLISHED_ANDESITE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
            case SUPPORT -> Blocks.POLISHED_ANDESITE.defaultBlockState();
            case CASING -> ModBlocks.GENERATOR_CASING.get().defaultBlockState();
            case CORE -> ModBlocks.GENERATOR_CORE.get().defaultBlockState();
            case CORE_DEAD -> ModBlocks.GENERATOR_CORE_DEAD.get().defaultBlockState();
            case PANEL -> d(ModBlocks.CONTROL_PANEL, Direction.SOUTH);
            case CCTV -> d(ModBlocks.CCTV_BANK, Direction.SOUTH);
            case CHAIR -> Blocks.POLISHED_BLACKSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);
            case LOCKER -> d(ModBlocks.LAB_CABINET, Direction.WEST);
            case CRATE -> Blocks.STRIPPED_SPRUCE_WOOD.defaultBlockState();
            case TANK -> Blocks.WEATHERED_CUT_COPPER.defaultBlockState();
            case TANK_TOP -> Blocks.WAXED_EXPOSED_CUT_COPPER.defaultBlockState();
            case FURNACE -> Blocks.BLAST_FURNACE.defaultBlockState().setValue(net.minecraft.world.level.block.AbstractFurnaceBlock.FACING, Direction.SOUTH)
                    .setValue(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT, true);
            case VAT -> Blocks.LAVA_CAULDRON.defaultBlockState();
            case COAL -> Blocks.COAL_BLOCK.defaultBlockState();
            case LOCKER_E -> d(ModBlocks.LAB_CABINET, Direction.EAST);
            case BENCH -> Blocks.POLISHED_BLACKSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST);
        };
    }

    public static void decorate(OfficePlanner p, Industrial.Site s, WorldGenLevel level, ChunkAccess chunk, int baseX, int baseZ) {
        int cx0 = (chunk.getPos().getMinBlockX() - baseX) / Industrial.C;
        int cz0 = (chunk.getPos().getMinBlockZ() - baseZ) / Industrial.C;
        for (int lv = 0; lv < s.storeys; lv++) {
            int y = s.yBase + lv * 6 + 1;
            int t = s.type(lv, cx0, cz0);
            int ox = baseX + cx0 * Industrial.C;
            int oz = baseZ + cz0 * Industrial.C;
            if (t == Industrial.CONTROL && MegaDecorator.chunkHas(chunk, ox + 14, oz + 2)) {
                MegaDecorator.lootChest(p, level, new BlockPos(ox + 14, y, oz + 2), Direction.WEST, "industrial_control");
            }
            if (t == Industrial.CONTROL && MegaDecorator.chunkHas(chunk, ox + 7, oz + 1)) {
                level.setBlock(new BlockPos(ox + 7, y + 1, oz + 1), ModBlocks.monitor(Direction.SOUTH, s.h(0x9560 + lv, cx0, cz0) < 0.17), 2);
            }
            if (t == Industrial.STORAGE && s.h(0x9550 + lv, cx0, cz0) < 0.6 && MegaDecorator.chunkHas(chunk, ox + 2, oz + 14)) {
                MegaDecorator.lootChest(p, level, new BlockPos(ox + 2, y, oz + 14), Direction.EAST, "industrial_storage");
            }
        }
        for (int i = 0; i < s.halls.length; i++) {
            int hx = baseX + s.halls[i][0] * Industrial.C;
            int hz = baseZ + s.halls[i][1] * Industrial.C;
            if (MegaDecorator.chunkHas(chunk, hx + 4, hz + 4)) {
                MegaDecorator.lootChest(p, level, new BlockPos(hx + 4, s.yBase + 1, hz + 4), Direction.SOUTH, i == 0 ? "industrial_generator_artifact" : "industrial_generator");
            }
            if (MegaDecorator.chunkHas(chunk, hx + 15, hz + 1)) {
                MegaDecorator.sign(level, new BlockPos(hx + 15, s.yBase + 3, hz + 1), Direction.SOUTH,
                        "GENERATOR HALL", i == 0 ? "A" : "B", "Hearing protection", "required");
            }
        }
    }
}
