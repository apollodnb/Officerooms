package de.officerooms.megastructure;

import de.officerooms.block.CeilingLightBlock;
import de.officerooms.block.DecorBlock;
import de.officerooms.block.ModBlocks;
import de.officerooms.block.RoomDecorBlocks;
import de.officerooms.furnish.Decor;
import de.officerooms.block.SecurityDoorBlock;
import de.officerooms.core.Hashing;
import de.officerooms.core.OfficeRoomsIds;
import de.officerooms.room.OfficePlanner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.registries.RegistryObject;

/** 0.14.0: block states of the Hotel and its decoration in the FEATURES stage (signs, chests, bell, vault). */
public final class HotelBlocks {

    private HotelBlocks() {
    }

    private static BlockState d(RegistryObject<Block> b, Direction f) {
        return b.get().defaultBlockState().setValue(DecorBlock.FACING, f);
    }

    private static BlockState door(Direction f, boolean open, boolean upper) {
        return Blocks.DARK_OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, f).setValue(DoorBlock.OPEN, open)
                .setValue(DoorBlock.HALF, upper ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER);
    }

    /** 0.28.0: a locked room ("do not disturb"): an iron door without a button, break it or find another way. */
    private static BlockState iron(Direction f, boolean upper) {
        return Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, f)
                .setValue(DoorBlock.HALF, upper ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER);
    }

    private static BlockState stairs(Block b, Direction f) {
        return b.defaultBlockState().setValue(StairBlock.FACING, f);
    }

    private static final Direction[] DIRS = {Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH};

    /** 0.36.0: the groups of four facings (E, S, W, N) of the labyrinth hotel. */
    private static BlockState directional(Hotel.Part base, Direction f) {
        return switch (base) {
            case STAIR_0 -> stairs(Blocks.DARK_OAK_STAIRS, f);
            case BED_0 -> d(ModBlocks.HOTEL_BED, f);
            case BEDF_0 -> d(ModBlocks.HOTEL_BED_FOOT, f);
            case NIGHT_0 -> d(ModBlocks.HOTEL_NIGHTSTAND, f);
            case DRESS_0 -> d(ModBlocks.HOTEL_DRESSER, f);
            case TV_0 -> d(ModBlocks.CRT_MONITOR, f);
            case STV_0 -> RoomDecorBlocks.decor(Decor.STATIC_TV, f);
            case BTV_0 -> ModBlocks.monitor(f, true);
            case PHONE_0 -> RoomDecorBlocks.decor(Decor.HOTEL_PHONE, f);
            case SOFA_0 -> stairs(Blocks.CRIMSON_STAIRS, f.getOpposite());
            case SCONCE_0 -> RoomDecorBlocks.decor(Decor.HOTEL_SCONCE, f);
            case CART_0 -> RoomDecorBlocks.decor(Decor.HOUSEKEEPING_CART, f);
            case ICE_0 -> RoomDecorBlocks.decor(Decor.ICE_MACHINE, f);
            case TOILET_0 -> stairs(Blocks.QUARTZ_STAIRS, f);
            case CHAIR_0 -> stairs(Blocks.DARK_OAK_STAIRS, f);
            default -> Blocks.AIR.defaultBlockState();
        };
    }

    public static BlockState state(Hotel.Part part) {
        Hotel.Part base = Hotel.dirBase(part);
        if (base != null) {
            return directional(base, DIRS[part.ordinal() - base.ordinal()]);
        }
        return switch (part) {
            case AIR, OFFICE -> Blocks.AIR.defaultBlockState();
            case RING, WALLPAPER -> ModBlocks.HOTEL_WALLPAPER.get().defaultBlockState();
            case CEILING -> ModBlocks.HOTEL_CEILING.get().defaultBlockState();
            case CARPET -> ModBlocks.HOTEL_CARPET.get().defaultBlockState();
            case MARBLE -> ModBlocks.HOTEL_MARBLE.get().defaultBlockState();
            case WAINSCOT -> ModBlocks.HOTEL_WAINSCOT.get().defaultBlockState();
            case LAMP -> d(ModBlocks.HOTEL_LAMP, Direction.NORTH);
            case LAMP_BROKEN -> d(ModBlocks.HOTEL_LAMP_BROKEN, Direction.NORTH);
            case DOOR_A_LO -> door(Direction.SOUTH, false, false);
            case DOOR_A_HI -> door(Direction.SOUTH, false, true);
            case DOOR_A_OPEN_LO -> door(Direction.SOUTH, true, false);
            case DOOR_A_OPEN_HI -> door(Direction.SOUTH, true, true);
            case DOOR_B_LO -> door(Direction.NORTH, false, false);
            case DOOR_B_HI -> door(Direction.NORTH, false, true);
            case DOOR_B_OPEN_LO -> door(Direction.NORTH, true, false);
            case DOOR_B_OPEN_HI -> door(Direction.NORTH, true, true);
            case BED_A -> d(ModBlocks.HOTEL_BED, Direction.NORTH);
            case BED_B -> d(ModBlocks.HOTEL_BED, Direction.SOUTH);
            case BEDF_A -> d(ModBlocks.HOTEL_BED_FOOT, Direction.NORTH);
            case BEDF_B -> d(ModBlocks.HOTEL_BED_FOOT, Direction.SOUTH);
            case NIGHT_A -> d(ModBlocks.HOTEL_NIGHTSTAND, Direction.SOUTH);
            case NIGHT_B -> d(ModBlocks.HOTEL_NIGHTSTAND, Direction.NORTH);
            case DRESSER -> d(ModBlocks.HOTEL_DRESSER, Direction.WEST);
            case TV -> d(ModBlocks.CRT_MONITOR, Direction.WEST);
            case STAIR -> stairs(Blocks.DARK_OAK_STAIRS, Direction.SOUTH);
            case STAIR_SUPPORT -> Blocks.DARK_OAK_PLANKS.defaultBlockState();
            case RAILING -> Blocks.DARK_OAK_FENCE.defaultBlockState();
            case COLUMN -> Blocks.QUARTZ_PILLAR.defaultBlockState();
            case RECEPTION -> d(ModBlocks.HOTEL_RECEPTION_DESK, Direction.SOUTH);
            case SOFA_S -> stairs(Blocks.CRIMSON_STAIRS, Direction.NORTH);
            case SOFA_N -> stairs(Blocks.CRIMSON_STAIRS, Direction.SOUTH);
            case PLANT -> Blocks.POTTED_DEAD_BUSH.defaultBlockState();
            case RUG -> Blocks.RED_CARPET.defaultBlockState();
            case B_FLOOR -> Blocks.GRAY_CONCRETE.defaultBlockState();
            case B_CEIL -> Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
            case B_PILLAR -> Blocks.POLISHED_ANDESITE.defaultBlockState();
            case PIPE -> ModBlocks.PIPE.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
            case B_LIGHT -> ModBlocks.CEILING_PANEL.get().defaultBlockState().setValue(CeilingLightBlock.AXIS, Direction.Axis.X);
            case CRATE -> Blocks.STRIPPED_SPRUCE_WOOD.defaultBlockState();
            case LAUNDRY -> Blocks.WHITE_WOOL.defaultBlockState();
            case SECURITY -> ModBlocks.SECURITY_WALL.get().defaultBlockState();
            case CARPET2 -> ModBlocks.HOTEL_CARPET_TEAL.get().defaultBlockState();
            case PHONE_A -> RoomDecorBlocks.decor(Decor.HOTEL_PHONE, Direction.SOUTH);
            case PHONE_B -> RoomDecorBlocks.decor(Decor.HOTEL_PHONE, Direction.NORTH);
            case STATIC_TV -> RoomDecorBlocks.decor(Decor.STATIC_TV, Direction.WEST);
            case BROKEN_TV -> ModBlocks.monitor(Direction.WEST, true);
            case MAGAZINES -> RoomDecorBlocks.decor(Decor.MAGAZINES, Direction.NORTH);
            case COBWEB -> Blocks.COBWEB.defaultBlockState();
            case BARRICADE -> Blocks.DARK_OAK_PLANKS.defaultBlockState();
            case BARRICADE_TOP -> Blocks.DARK_OAK_FENCE.defaultBlockState();
            case SCONCE_S -> RoomDecorBlocks.decor(Decor.HOTEL_SCONCE, Direction.SOUTH);
            case SCONCE_N -> RoomDecorBlocks.decor(Decor.HOTEL_SCONCE, Direction.NORTH);
            case ICE_S -> RoomDecorBlocks.decor(Decor.ICE_MACHINE, Direction.SOUTH);
            case CART_N -> RoomDecorBlocks.decor(Decor.HOUSEKEEPING_CART, Direction.NORTH);
            case IRON_A_LO -> iron(Direction.SOUTH, false);
            case IRON_A_HI -> iron(Direction.SOUTH, true);
            case IRON_B_LO -> iron(Direction.NORTH, false);
            case IRON_B_HI -> iron(Direction.NORTH, true);
            case DOOR_X_LO -> door(Direction.EAST, false, false);
            case DOOR_X_HI -> door(Direction.EAST, false, true);
            case DOOR_X_OPEN_LO -> door(Direction.EAST, true, false);
            case DOOR_X_OPEN_HI -> door(Direction.EAST, true, true);
            case IRON_X_LO -> iron(Direction.EAST, false);
            case IRON_X_HI -> iron(Direction.EAST, true);
            case TILE -> Blocks.SMOOTH_QUARTZ.defaultBlockState();
            case TUB -> Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 2);
            case SINK -> Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState().setValue(net.minecraft.world.level.block.SlabBlock.TYPE,
                    net.minecraft.world.level.block.state.properties.SlabType.TOP);
            case SHELF -> Blocks.BOOKSHELF.defaultBlockState();
            case TABLE -> Blocks.DARK_OAK_FENCE.defaultBlockState();
            case TABLE_TOP -> Blocks.DARK_OAK_PRESSURE_PLATE.defaultBlockState();
            case WOOD_FLOOR -> Blocks.DARK_OAK_PLANKS.defaultBlockState();
            case PLANT2 -> Blocks.POTTED_FERN.defaultBlockState();
            case WARDROBE -> Blocks.STRIPPED_DARK_OAK_WOOD.defaultBlockState();
            default -> Blocks.AIR.defaultBlockState();
        };
    }

    /** 0.36.0: room numbers over the doors (outside) and loot chests in the labyrinth hotel. */
    private static void labyrinth(OfficePlanner p, Hotel.Site s, WorldGenLevel level, ChunkAccess chunk, int baseX, int baseZ) {
        // railings around the stairwells: connect the fence posts
        for (int lv = 1; lv < s.storeys - 1; lv++) {
            for (int[] u : s.floor(lv).up) {
                int ox = baseX + HotelLayout.ORG + u[0] * HotelLayout.CS;
                int oz = baseZ + HotelLayout.ORG + u[1] * HotelLayout.CS;
                for (int a = 1; a < HotelLayout.CS; a++) {
                    for (int b = 1; b < HotelLayout.CS; b++) {
                        if (MegaDecorator.chunkHas(chunk, ox + a, oz + b) && Hotel.Site.lStair(a, b, 7, u[2]) == Hotel.Part.RAILING) {
                            BlockPos pos = new BlockPos(ox + a, s.yBase + lv * 6 + 7, oz + b);
                            level.setBlock(pos, Block.updateFromNeighbourShapes(level.getBlockState(pos), level, pos), 2);
                        }
                    }
                }
            }
        }
        for (int lv = 2; lv < s.storeys; lv++) {
            HotelLayout f = s.floor(lv);
            int y = s.yBase + lv * 6 + 1;
            int idx = 0;
            for (int cz = 0; cz < HotelLayout.NC; cz++) {
                for (int cx = 0; cx < HotelLayout.NC; cx++) {
                    if (f.type[cx][cz] != HotelLayout.ROOM) {
                        continue;
                    }
                    idx++;
                    int dd = f.door[cx][cz];
                    int[] door = s.roomPos(f, cx, cz, 5, 0);
                    int sx = baseX + door[0] + HotelLayout.DX[dd];
                    int sz = baseZ + door[1] + HotelLayout.DZ[dd];
                    if (MegaDecorator.chunkHas(chunk, sx, sz)) {
                        boolean dnd = f.kind[cx][cz] == HotelLayout.Kind.BARRICADED;
                        String name = switch (f.kind[cx][cz]) {
                            case LINEN -> "LINEN";
                            case BATH -> "BATH";
                            case LOUNGE -> "LOUNGE";
                            case DINING -> "DINING";
                            default -> "ROOM " + (lv * 100 + idx);
                        };
                        MegaDecorator.sign(level, new BlockPos(sx, y + 2, sz), DIRS[dd], "", name, dnd ? "DO NOT" : "", dnd ? "DISTURB" : "");
                    }
                    if (s.roomChest(lv, cx, cz)) {
                        int[] c = s.roomPos(f, cx, cz, 1, 9);
                        if (MegaDecorator.chunkHas(chunk, baseX + c[0], baseZ + c[1])) {
                            MegaDecorator.lootChest(p, level, new BlockPos(baseX + c[0], y, baseZ + c[1]), DIRS[dd], "hotel_room");
                        }
                    }
                }
            }
        }
    }

    // --- decoration ------------------------------------------------------------------------------------------

    private static void container(OfficePlanner p, WorldGenLevel level, BlockPos pos, BlockState st, String table) {
        level.setBlock(pos, st, 2);
        if (level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity be) {
            be.setLootTable(new ResourceLocation(OfficeRoomsIds.MOD_ID, "chests/" + table), p.seed ^ pos.asLong());
        }
    }

    public static void decorate(OfficePlanner p, Hotel.Site s, WorldGenLevel level, ChunkAccess chunk, int baseX, int baseZ) {
        if (s.modern) {
            labyrinth(p, s, level, chunk, baseX, baseZ);
            MegaFurniture.swap(level, chunk, baseX, baseZ, s.yBase, s.h, (lx, off, lz) -> MegaFurniture.hotel(s, lx, off, lz));
        }
        // guest floors: room numbers over the doors and loot chests in some rooms
        for (int lv = 2; lv <= 3 && !s.modern; lv++) {
            int y = s.yBase + lv * 6 + 1;
            int idx = 0;
            for (int row = 0; row < Hotel.ROWS; row++) {
                for (int band = 0; band < 2; band++) {
                    for (int col = 0; col < Hotel.COLS; col++) {
                        int dx = Hotel.doorX(col);
                        if (dx < 0) {
                            continue;
                        }
                        idx++;
                        int dz = Hotel.doorZ(row, band);
                        int sx = baseX + dx;
                        int sz = baseZ + dz + (band == 0 ? 1 : -1);
                        Hotel.RoomKind kind = s.roomKind(lv, row, col, band);
                        if (MegaDecorator.chunkHas(chunk, sx, sz)) {
                            boolean dnd = kind == Hotel.RoomKind.BARRICADED;
                            MegaDecorator.sign(level, new BlockPos(sx, y + 2, sz), band == 0 ? Direction.SOUTH : Direction.NORTH,
                                    "", "ROOM " + ((lv - 1) * 1000 + idx), dnd ? "DO NOT" : "", dnd ? "DISTURB" : "");
                        }
                        if (s.chestRoom(lv, row, col, band) || kind == Hotel.RoomKind.BARRICADED) {
                            int cx = baseX + Hotel.X0 + col * Hotel.CW + 1;
                            int cz = baseZ + Hotel.Z0 + row * Hotel.MZ + (band == 0 ? 6 : Hotel.MZ - 7);
                            if (MegaDecorator.chunkHas(chunk, cx, cz)) {
                                MegaDecorator.lootChest(p, level, new BlockPos(cx, y, cz), band == 0 ? Direction.NORTH : Direction.SOUTH, "hotel_room");
                            }
                        }
                    }
                }
            }
        }
        // lobby: reception bell, sign and the reception chest
        int ly = s.yBase + 7;
        int rc = Hotel.reception();
        if (MegaDecorator.chunkHas(chunk, baseX + rc, baseZ + rc)) {
            level.setBlock(new BlockPos(baseX + rc, ly + 1, baseZ + rc), Blocks.BELL.defaultBlockState(), 2);
        }
        if (MegaDecorator.chunkHas(chunk, baseX + 75, baseZ + rc)) {
            level.setBlock(new BlockPos(baseX + 75, ly + 1, baseZ + rc), ModBlocks.monitor(Direction.SOUTH, false), 2);
        }
        if (MegaDecorator.chunkHas(chunk, baseX + 70, baseZ + 74)) {
            MegaDecorator.lootChest(p, level, new BlockPos(baseX + 70, ly, baseZ + 74), Direction.NORTH, "hotel_reception");
        }
        if (MegaDecorator.chunkHas(chunk, baseX + 72, baseZ + 75)) {
            MegaDecorator.sign(level, new BlockPos(baseX + 72, ly + 2, baseZ + 75), Direction.NORTH, "", "RECEPTION", "Please ring", "the bell");
        }
        // basement vault
        int vy = s.yBase + 1;
        int vx = baseX + s.vx;
        int vz = baseZ + s.vz;
        int doorX = vx + Hotel.VS / 2;
        int doorZ = vz + Hotel.VS - 1;
        if (MegaDecorator.chunkHas(chunk, doorX, doorZ)) {
            BlockState lower = ModBlocks.SECURITY_DOOR.get().defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH)
                    .setValue(SecurityDoorBlock.LEVEL, s.vaultLevel);
            level.setBlock(new BlockPos(doorX, vy, doorZ), lower, 2);
            level.setBlock(new BlockPos(doorX, vy + 1, doorZ), lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 2);
        }
        if (MegaDecorator.chunkHas(chunk, doorX + 1, doorZ + 1)) {
            MegaDecorator.sign(level, new BlockPos(doorX + 1, vy + 1, doorZ + 1), Direction.SOUTH, "HOTEL VAULT", "Staff only", "LEVEL " + s.vaultLevel, "Access card");
        }
        for (int[] c : new int[][]{{2, 1}, {6, 1}}) {
            if (MegaDecorator.chunkHas(chunk, vx + c[0], vz + c[1])) {
                MegaDecorator.lootChest(p, level, new BlockPos(vx + c[0], vy, vz + c[1]), Direction.SOUTH, c[0] == 2 ? "hotel_vault_artifact" : "hotel_vault");
            }
        }
        for (int k = 3; k <= 5; k++) {
            if (MegaDecorator.chunkHas(chunk, vx + 1, vz + k)) {
                container(p, level, new BlockPos(vx + 1, vy, vz + k), Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.EAST), "hotel_safe_box");
            }
        }
        if (MegaDecorator.chunkHas(chunk, vx + 7, vz + 4) && Hashing.unit(Hashing.hash(p.seed, s.rx, s.rz, 0x9420, 0)) < 0.5) {
            level.setBlock(new BlockPos(vx + 7, vy, vz + 4), d(ModBlocks.RESEARCH_TABLE, Direction.WEST), 2);
        }
    }
}
