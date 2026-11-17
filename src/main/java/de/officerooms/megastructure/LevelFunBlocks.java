package de.officerooms.megastructure;

import de.officerooms.block.CeilingLightBlock;
import de.officerooms.block.DecorBlock;
import de.officerooms.block.ModBlocks;
import de.officerooms.room.OfficePlanner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.registries.RegistryObject;

/** 0.16.0: block states of Level Fun and its decoration (gift chests). */
public final class LevelFunBlocks {

    private LevelFunBlocks() {
    }

    private static BlockState d(RegistryObject<Block> b, Direction f) {
        return b.get().defaultBlockState().setValue(DecorBlock.FACING, f);
    }

    public static BlockState state(LevelFun.Part part) {
        BlockState strip = ModBlocks.CEILING_STRIP.get().defaultBlockState().setValue(CeilingLightBlock.AXIS, Direction.Axis.X);
        return switch (part) {
            case AIR, OFFICE -> Blocks.AIR.defaultBlockState();
            case WALL -> ModBlocks.FUN_WALLPAPER.get().defaultBlockState();
            case CARPET -> ModBlocks.PLAY_CARPET.get().defaultBlockState();
            case CEILING -> ModBlocks.MAZE_CEILING.get().defaultBlockState();
            case LIGHT -> strip;
            case LIGHT_OFF -> strip.setValue(CeilingLightBlock.LIT, false);
            case CONFETTI -> d(ModBlocks.CONFETTI, Direction.NORTH);
            case TABLE -> d(ModBlocks.PARTY_TABLE, Direction.NORTH);
            case CAKE -> Blocks.CAKE.defaultBlockState();
            case BALLOON_0 -> d(ModBlocks.BALLOON_RED, Direction.NORTH);
            case BALLOON_1 -> d(ModBlocks.BALLOON_BLUE, Direction.NORTH);
            case BALLOON_2 -> d(ModBlocks.BALLOON_GREEN, Direction.NORTH);
            case BALLOON_3 -> d(ModBlocks.BALLOON_PINK, Direction.NORTH);
            case BALLOON_4 -> d(ModBlocks.BALLOON_PURPLE, Direction.NORTH);
            case STREAMER -> d(ModBlocks.STREAMER, Direction.NORTH);
            case GIFT -> d(ModBlocks.GIFT_BOX, Direction.NORTH);
            case SMILE_E -> d(ModBlocks.SMILE_GRAFFITI, Direction.EAST);
            case SMILE_S -> d(ModBlocks.SMILE_GRAFFITI, Direction.SOUTH);
            case INFL_RED -> ModBlocks.INFLATABLE_RED.get().defaultBlockState();
            case INFL_BLUE -> ModBlocks.INFLATABLE_BLUE.get().defaultBlockState();
            case INFL_YELLOW -> ModBlocks.INFLATABLE_YELLOW.get().defaultBlockState();
            case STAIR_S -> Blocks.BIRCH_STAIRS.defaultBlockState().setValue(net.minecraft.world.level.block.StairBlock.FACING, Direction.SOUTH);
            case STAIR_E -> Blocks.BIRCH_STAIRS.defaultBlockState().setValue(net.minecraft.world.level.block.StairBlock.FACING, Direction.EAST);
            case STAIR_FILL -> ModBlocks.FUN_WALLPAPER.get().defaultBlockState();
            case RAIL -> Blocks.BIRCH_FENCE.defaultBlockState();
            case BALL_R -> Blocks.RED_WOOL.defaultBlockState();
            case BALL_B -> Blocks.LIGHT_BLUE_WOOL.defaultBlockState();
            case BALL_Y -> Blocks.YELLOW_WOOL.defaultBlockState();
            case BALL_G -> Blocks.LIME_WOOL.defaultBlockState();
            case DISCO_A -> Blocks.MAGENTA_CONCRETE.defaultBlockState();
            case DISCO_B -> Blocks.CYAN_CONCRETE.defaultBlockState();
            case JUKEBOX -> Blocks.JUKEBOX.defaultBlockState();
        };
    }

    /** 0.19.0: the first gift cell (scan order) holds the Level Fun artifact. */
    static boolean artifactCell(LevelFun.Site s, int cx, int cz) {
        for (int i = 1; i <= 15; i++) {
            for (int k = 0; k <= 14; k++) {
                if (s.cellType(i, k) == LevelFun.GIFTS) {
                    return i == cx && k == cz;
                }
            }
        }
        return false;
    }

    public static void decorate(OfficePlanner p, LevelFun.Site s, WorldGenLevel level, ChunkAccess chunk, int baseX, int baseZ) {
        int x0 = chunk.getPos().getMinBlockX();
        int z0 = chunk.getPos().getMinBlockZ();
        for (int cx = Math.max(0, (x0 - baseX) / LevelFun.C - 1); cx <= Math.min(15, (x0 - baseX + 15) / LevelFun.C); cx++) {
            for (int cz = Math.max(0, (z0 - baseZ) / LevelFun.C - 1); cz <= Math.min(15, (z0 - baseZ + 15) / LevelFun.C); cz++) {
                for (int lv = 0; lv < s.storeys - 1; lv++) {               // 0.36.0: connect the railings around stairwells
                    if (s.cellType(lv, cx, cz) != LevelFun.STAIRS) {
                        continue;
                    }
                    for (int ax = 1; ax < LevelFun.C; ax++) {
                        for (int az = 1; az < LevelFun.C; az++) {
                            int x = baseX + cx * LevelFun.C + ax;
                            int z = baseZ + cz * LevelFun.C + az;
                            if (MegaDecorator.chunkHas(chunk, x, z) && s.stair(lv, cx, cz, ax, az, 7) == LevelFun.Part.RAIL) {
                                BlockPos pos = new BlockPos(x, s.yBase + lv * 6 + 7, z);
                                level.setBlock(pos, net.minecraft.world.level.block.Block.updateFromNeighbourShapes(level.getBlockState(pos), level, pos), 2);
                            }
                        }
                    }
                }
                for (int lv = 0; lv < s.storeys; lv++) {
                    if (s.cellType(lv, cx, cz) != LevelFun.GIFTS || (lv > 0 && s.cellType(lv - 1, cx, cz) == LevelFun.STAIRS)) {
                        continue;
                    }
                    int x = baseX + cx * LevelFun.C + 2;
                    int z = baseZ + cz * LevelFun.C + 7;
                    if (MegaDecorator.chunkHas(chunk, x, z) && x - baseX > 2 && z - baseZ > 2 && x - baseX < 157 && z - baseZ < 157) {
                        MegaDecorator.lootChest(p, level, new BlockPos(x, s.yBase + lv * 6 + 1, z), Direction.NORTH,
                                lv == 0 && artifactCell(s, cx, cz) ? "level_fun_gift_artifact" : "level_fun_gift");
                    }
                }
            }
        }
    }
}
