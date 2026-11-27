package de.officerooms.special;

import de.officerooms.block.ChalkMarkBlock;
import de.officerooms.block.ModBlocks;
import de.officerooms.block.SecurityDoorBlock;
import de.officerooms.core.Hashing;
import de.officerooms.core.OfficeRoomsIds;
import de.officerooms.megastructure.MegaDecorator;
import de.officerooms.room.Extras;
import de.officerooms.room.OfficePlanner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * 0.11.0: furnishes the hidden rooms ({@link Extras.Hideout}). Interior cells (ix 1–5, iz 1–4), the doorway in
 * the middle of the south wall. Everything is placed per chunk in the FEATURES stage.
 */
public final class Hideouts {

    private Hideouts() {
    }

    private record Ctx(WorldGenLevel level, ChunkAccess chunk, int bx, int by, int bz, RandomSource rng, long seed) {
        BlockPos at(int ix, int dy, int iz) {
            return new BlockPos(bx + ix, by + dy, bz + iz);
        }

        boolean mine(BlockPos p) {
            return (p.getX() >> 4) == chunk.getPos().x && (p.getZ() >> 4) == chunk.getPos().z;
        }

        void set(int ix, int dy, int iz, BlockState st) {
            BlockPos p = at(ix, dy, iz);
            if (mine(p)) {
                level.setBlock(p, st, 2);
            }
        }

        void container(int ix, int iz, BlockState st, String table) {
            BlockPos p = at(ix, 0, iz);
            if (!mine(p)) {
                return;
            }
            level.setBlock(p, st, 2);
            if (level.getBlockEntity(p) instanceof RandomizableContainerBlockEntity be) {
                be.setLootTable(ResourceLocation.fromNamespaceAndPath(OfficeRoomsIds.MOD_ID, "chests/" + table), seed ^ p.asLong());
            }
        }

        void lectern(int ix, int iz, Direction facing, Stories.Book book) {
            BlockPos p = at(ix, 0, iz);
            if (!mine(p)) {
                return;
            }
            level.setBlock(p, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, facing).setValue(LecternBlock.HAS_BOOK, true), 2);
            if (level.getBlockEntity(p) instanceof LecternBlockEntity le) {
                le.setBook(SpecialDecorator.bookStack(book));
            }
        }
    }

    public static void decorate(OfficePlanner p, Extras.Hideout h, WorldGenLevel level, ChunkAccess chunk, int baseX, int baseZ) {
        int minX = baseX + h.x0() - 1;
        int maxX = baseX + h.x0() + h.w();
        int minZ = baseZ + h.z0() - 1;
        int maxZ = baseZ + h.z0() + h.d();
        int cx = chunk.getPos().getMinBlockX();
        int cz = chunk.getPos().getMinBlockZ();
        if (maxX < cx || minX > cx + 15 || maxZ < cz || minZ > cz + 15) {
            return;
        }
        long seed = Hashing.hash(p.seed, baseX + h.x0(), baseZ + h.z0(), h.s(), 0x9304);
        Ctx c = new Ctx(level, chunk, baseX + h.x0(), p.floorY(h.s()) + 1, baseZ + h.z0(), RandomSource.create(seed), p.seed);
        door(c, h);
        dust(c, h);
        if (h.empty()) {
            return;
        }
        Stories.Book[] notes = HideoutNotes.BY_TYPE[h.type()];
        Stories.Book note = notes[(int) Math.floorMod(seed, notes.length)];
        switch (h.type()) {
            case 0 -> maintenance(c, note);
            case 1 -> supply(c, note);
            case 2 -> breakRoom(c, note);
            case 3 -> security(c, note);
            case 4 -> lab(c, note);
            default -> concealed(c, note);
        }
    }

    // --- entrance ------------------------------------------------------------------------------------

    private static void door(Ctx c, Extras.Hideout h) {
        int ix = h.w() / 2;
        int iz = h.d() - 1;
        switch (h.type()) {
            case 2 -> {                                        // break room: a plain door, left ajar
                BlockState d = Blocks.SPRUCE_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH)
                        .setValue(DoorBlock.OPEN, c.rng.nextBoolean());
                c.set(ix, 0, iz, d);
                c.set(ix, 1, iz, d.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
            }
            case 3 -> {                                        // security office: iron door, opened from inside
                BlockState d = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH).setValue(DoorBlock.OPEN, true);
                c.set(ix, 0, iz, d);
                c.set(ix, 1, iz, d.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
            }
            case 4 -> {                                        // lab: security door level 1 or 2
                int lvl = c.rng.nextInt(10) < 6 ? 1 : 2;
                BlockState d = ModBlocks.SECURITY_DOOR.get().defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH)
                        .setValue(SecurityDoorBlock.LEVEL, lvl);
                c.set(ix, 0, iz, d);
                c.set(ix, 1, iz, d.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
                BlockPos sp = c.at(ix + 1, 1, iz + 1);
                if (c.mine(sp)) {
                    MegaDecorator.sign(c.level, sp, Direction.SOUTH, "LABORATORY", "AUTHORISED", "STAFF ONLY", "Level " + lvl);
                }
            }
            default -> {                                       // hidden: a loose panel that looks like the wall
                c.set(ix, 0, iz, ModBlocks.LOOSE_PANEL.get().defaultBlockState());
                c.set(ix, 1, iz, ModBlocks.LOOSE_PANEL.get().defaultBlockState());
            }
        }
    }

    private static void dust(Ctx c, Extras.Hideout h) {
        for (int k = 0; k < 3; k++) {
            int ix = 1 + c.rng.nextInt(h.w() - 2);
            int iz = 1 + c.rng.nextInt(h.d() - 2);
            int dy = c.rng.nextBoolean() ? 3 : 0;
            if (dy == 3 || (iz < h.d() - 2 && (ix == 1 || ix == h.w() - 2 || iz == 1))) {
                c.set(ix, dy, iz, Blocks.COBWEB.defaultBlockState());
            }
        }
    }

    private static BlockState chestFacing(Direction d) {
        return Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, d);
    }

    // --- rooms ---------------------------------------------------------------------------------------

    private static void maintenance(Ctx c, Stories.Book note) {
        for (int ix = 1; ix <= 5; ix++) {                     // pipes along the back wall
            c.set(ix, 2, 1, Blocks.IRON_BARS.defaultBlockState());
        }
        c.set(1, 0, 1, Blocks.CHAIN.defaultBlockState());
        c.set(1, 1, 1, Blocks.CHAIN.defaultBlockState());
        c.set(5, 1, 2, Blocks.REDSTONE_LAMP.defaultBlockState());   // dead fuse box
        c.set(5, 0, 2, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.WALL).setValue(LeverBlock.FACING, Direction.WEST));
        c.container(2, 1, chestFacing(Direction.SOUTH), "hideout_maintenance");
        c.set(4, 0, 1, Blocks.CHIPPED_ANVIL.defaultBlockState());
        c.container(5, 4, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), "hideout_maintenance");
        c.lectern(1, 3, Direction.EAST, note);
    }

    private static void supply(Ctx c, Stories.Book note) {
        for (int ix = 1; ix <= 5; ix++) {
            c.container(ix, 1, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.SOUTH), ix % 2 == 0 ? "hideout_supply" : "hideout_supply_common");
            c.set(ix, 1, 1, Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        }
        c.set(1, 0, 2, Blocks.BARREL.defaultBlockState());
        c.container(5, 2, chestFacing(Direction.WEST), "hideout_supply");
        c.lectern(1, 3, Direction.EAST, note);
    }

    private static void breakRoom(Ctx c, Stories.Book note) {
        c.set(3, 0, 2, Blocks.DARK_OAK_FENCE.defaultBlockState());   // table
        c.set(3, 1, 2, Blocks.DARK_OAK_PRESSURE_PLATE.defaultBlockState());
        c.set(2, 0, 2, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST));
        c.set(4, 0, 2, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
        c.set(3, 0, 1, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
        c.set(1, 0, 1, Blocks.IRON_BLOCK.defaultBlockState());          // fridge
        c.container(1, 2, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.EAST), "hideout_breakroom");
        c.set(5, 0, 1, Blocks.SMOOTH_STONE.defaultBlockState());         // counter with a coffee machine
        c.set(5, 1, 1, Blocks.BREWING_STAND.defaultBlockState());
        c.set(5, 0, 2, Blocks.SMOOTH_STONE.defaultBlockState());
        c.set(5, 1, 2, Blocks.POTTED_DEAD_BUSH.defaultBlockState());
        c.set(2, 1, 2, Blocks.CANDLE.defaultBlockState());
        c.lectern(5, 4, Direction.WEST, note);
        BlockPos sp = c.at(3, 1, 0);
        if (c.mine(sp)) {
            MegaDecorator.sign(c.level, sp.south(), Direction.SOUTH, "HAPPY", "BIRTHDAY", "DAVE", "");
        }
    }

    private static BlockState decor(net.neoforged.neoforge.registries.DeferredHolder<Block> b, Direction facing) {
        return b.get().defaultBlockState().setValue(de.officerooms.block.DecorBlock.FACING, facing);
    }

    private static BlockState chair(Direction back) {
        return Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, back);
    }

    private static BlockState topSlab() {
        return Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
    }

    /** 0.12.0: 9 x 6 surveillance centre. Interior ix 1-9, iz 1-6, door at ix 5 in the south wall. */
    private static void security(Ctx c, Stories.Book note) {
        for (int ix = 2; ix <= 8; ix++) {                               // the CCTV wall over a row of consoles
            c.set(ix, 0, 1, decor(ModBlocks.CONTROL_PANEL, Direction.SOUTH));
            c.set(ix, 1, 1, decor(ModBlocks.CCTV_BANK, Direction.SOUTH));
            c.set(ix, 2, 1, ix % 2 == 0 ? decor(ModBlocks.CCTV_BANK, Direction.SOUTH) : decor(ModBlocks.CRT_MONITOR, Direction.SOUTH));
        }
        c.set(1, 0, 1, decor(ModBlocks.SERVER_RACK, Direction.SOUTH));
        c.set(9, 0, 1, decor(ModBlocks.SERVER_RACK, Direction.SOUTH));
        c.set(3, 0, 2, chair(Direction.SOUTH));
        c.set(7, 0, 2, chair(Direction.SOUTH));
        for (int iz = 3; iz <= 5; iz++) {                               // server racks on the west wall
            c.set(1, 0, iz, decor(ModBlocks.SERVER_RACK, Direction.EAST));
            c.set(1, 1, iz, decor(ModBlocks.SERVER_RACK, Direction.EAST));
        }
        for (int ix = 4; ix <= 6; ix++) {                               // supervisor desk
            c.set(ix, 0, 4, topSlab());
        }
        c.set(4, 1, 4, ModBlocks.monitor(Direction.SOUTH, c.rng.nextInt(6) == 0));
        c.set(6, 1, 4, Blocks.LANTERN.defaultBlockState());
        c.set(5, 0, 5, chair(Direction.SOUTH));
        c.set(9, 0, 3, decor(ModBlocks.LAB_CABINET, Direction.WEST));    // key cabinet and the card chest
        c.set(9, 1, 3, decor(ModBlocks.LAB_CABINET, Direction.WEST));
        c.container(9, 4, chestFacing(Direction.WEST), "hideout_security");
        c.set(9, 1, 5, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.WALL).setValue(LeverBlock.FACING, Direction.WEST));
        c.lectern(8, 6, Direction.WEST, note);
        c.set(1, 3, 6, Blocks.OBSERVER.defaultBlockState().setValue(net.minecraft.world.level.block.ObserverBlock.FACING, Direction.SOUTH));
        c.set(5, 3, 3, ModBlocks.CEILING_PANEL.get().defaultBlockState().setValue(de.officerooms.block.CeilingLightBlock.LIT, false));
    }

    /** 0.12.0: 9 x 6 abandoned research lab. Interior ix 1-9, iz 1-6, door at ix 5 in the south wall. */
    private static void lab(Ctx c, Stories.Book note) {
        for (int ix = 1; ix <= 4; ix++) {                               // north bench with instruments
            c.set(ix, 0, 1, Blocks.WHITE_CONCRETE.defaultBlockState());
        }
        c.set(1, 1, 1, decor(ModBlocks.MICROSCOPE, Direction.SOUTH));
        c.set(2, 1, 1, decor(ModBlocks.SPECIMEN_JAR, Direction.SOUTH));
        c.set(3, 1, 1, Blocks.BREWING_STAND.defaultBlockState());
        c.set(4, 1, 1, decor(ModBlocks.SPECIMEN_JAR, Direction.SOUTH));
        for (int ix = 6; ix <= 8; ix++) {                               // three containment tanks, one broken
            c.set(ix, 0, 1, decor(ModBlocks.CONTAINMENT_TANK, Direction.SOUTH));
            c.set(ix, 1, 1, ix != 7 ? decor(ModBlocks.CONTAINMENT_TANK, Direction.SOUTH) : Blocks.IRON_BARS.defaultBlockState());
        }
        c.set(7, 0, 2, Blocks.SCULK.defaultBlockState());               // ... and what leaked out of it
        c.set(6, 0, 3, Blocks.SCULK_VEIN.defaultBlockState());
        c.set(9, 0, 1, decor(ModBlocks.LAB_CABINET, Direction.SOUTH));
        c.set(9, 1, 1, decor(ModBlocks.LAB_CABINET, Direction.SOUTH));
        c.set(9, 0, 2, decor(ModBlocks.LAB_CABINET, Direction.WEST));
        c.set(9, 0, 3, decor(ModBlocks.SERVER_RACK, Direction.WEST));    // computer corner
        c.set(9, 0, 4, decor(ModBlocks.CONTROL_PANEL, Direction.WEST));
        c.set(9, 1, 4, decor(ModBlocks.CRT_MONITOR, Direction.WEST));
        c.set(8, 0, 4, chair(Direction.EAST));
        c.set(4, 0, 4, decor(ModBlocks.EXPERIMENT_MACHINE, Direction.SOUTH)); // the experiment, fenced in with chains
        for (int[] q : new int[][]{{3, 3}, {4, 3}, {5, 3}, {3, 4}, {5, 4}, {3, 5}, {5, 5}}) {
            c.set(q[0], 0, q[1], Blocks.CHAIN.defaultBlockState());
            c.set(q[0], 1, q[1], Blocks.CHAIN.defaultBlockState());
        }
        c.set(4, 0, 5, Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, 8));
        c.set(1, 0, 3, Blocks.WHITE_CONCRETE.defaultBlockState());      // west bench: notes and the file chest
        c.set(1, 1, 3, ModBlocks.monitor(Direction.EAST, c.rng.nextInt(6) == 0));
        c.set(1, 0, 4, Blocks.WHITE_CONCRETE.defaultBlockState());
        c.set(1, 1, 4, decor(ModBlocks.SPECIMEN_JAR, Direction.EAST));
        c.container(1, 5, chestFacing(Direction.EAST), "hideout_lab");
        c.lectern(1, 6, Direction.EAST, note);
        if (c.rng.nextInt(2) == 0) {                                     // 0.13.0: half of the labs still have their Research Table
            c.set(3, 0, 6, decor(ModBlocks.RESEARCH_TABLE, Direction.NORTH));
        }
        c.set(8, 0, 6, Blocks.CAULDRON.defaultBlockState());
        c.set(9, 0, 6, Blocks.SLIME_BLOCK.defaultBlockState());
        c.set(2, 3, 3, ModBlocks.CEILING_STRIP.get().defaultBlockState());
        c.set(7, 3, 4, Blocks.SOUL_LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
    }

    private static void concealed(Ctx c, Stories.Book note) {
        BlockState mark = ModBlocks.CHALK_MARK.get().defaultBlockState().setValue(ChalkMarkBlock.MARK, ChalkMarkBlock.Mark.CROSS);
        for (int ix = 1; ix <= 5; ix++) {                               // the back wall is covered in marks
            for (int dy = 0; dy <= 2; dy++) {
                if (c.rng.nextInt(4) != 0) {
                    c.set(ix, dy, 1, mark.setValue(ChalkMarkBlock.FACING, Direction.SOUTH));
                }
            }
        }
        for (int iz = 2; iz <= 3; iz++) {
            for (int dy = 0; dy <= 2; dy++) {
                if (c.rng.nextInt(3) != 0) {
                    c.set(1, dy, iz, mark.setValue(ChalkMarkBlock.FACING, Direction.EAST));
                }
            }
        }
        c.set(5, 0, 2, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST)
                .setValue(StairBlock.HALF, Half.BOTTOM));                 // a chair facing the marks
        c.set(3, 0, 3, Blocks.REDSTONE_WIRE.defaultBlockState());
        c.set(3, 0, 2, Blocks.REDSTONE_WIRE.defaultBlockState());
        c.set(4, 0, 3, Blocks.REDSTONE_WIRE.defaultBlockState());
        c.container(5, 4, chestFacing(Direction.WEST), "hideout_concealed");
        c.lectern(2, 4, Direction.NORTH, note);
    }
}
