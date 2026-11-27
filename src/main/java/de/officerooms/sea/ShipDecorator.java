package de.officerooms.sea;

import static de.officerooms.sea.Ship.*;

import de.officerooms.OfficeRooms;
import de.officerooms.compat.FurnitureBuilder;
import de.officerooms.compat.OfficeCraftCompat;
import de.officerooms.core.Hashing;
import de.officerooms.core.OfficeRoomsIds;
import de.officerooms.special.Stories;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * 0.42.0: furniture and loot of the SS Fun =). Uses Refurbished Furniture and OfficeCraft where they are installed (with
 * vanilla stand-ins otherwise) and the ship's own blocks. Every room gets a fixed plan (from its seed); each chunk places
 * the part of the plan that lies inside it.
 */
public final class ShipDecorator {

    private ShipDecorator() {
    }

    /** One placement, in local ship coordinates; facings are local too (mirrored when the ship faces west). */
    record Op(int lx, int y, int lz, BlockState state, String loot, String rf, String oc, Direction front, Object extra) {
    }

    private static final Map<Long, List<Op>> PLANS = new ConcurrentHashMap<>();

    static void decorate(WorldGenLevel level, ChunkAccess chunk, Ship s) {
        int cx = chunk.getPos().x, cz = chunk.getPos().z;
        long key = Hashing.hash(s.seed, s.gx, s.gz);
        List<Op> plan = PLANS.get(key);
        if (plan == null) {
            plan = build(s);
            if (PLANS.size() > 6) {
                PLANS.clear();
            }
            PLANS.put(key, plan);
        }
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (Op op : plan) {
            int x = s.wx(op.lx()), z = s.wz(op.lz());
            if ((x >> 4) != cx || (z >> 4) != cz) {
                continue;
            }
            m.set(x, op.y(), z);
            try {
                apply(level, s, m, op);
            } catch (RuntimeException ex) {
                OfficeRooms.LOGGER.warn("SS Fun =): could not place {} at {}: {}", op, m, ex.toString());
            }
        }
    }

    // --- applying ----------------------------------------------------------------------------------------------------

    static Direction w(Ship s, Direction d) {
        return d == null ? null : ShipShape.w(s, d);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static BlockState mirror(Ship s, BlockState st) {
        if (!s.flip) {
            return st;
        }
        for (var p : st.getProperties()) {
            if (p instanceof net.minecraft.world.level.block.state.properties.DirectionProperty dp) {
                Direction d = (Direction) st.getValue(dp);
                if (d.getAxis() == Direction.Axis.X) {
                    st = st.setValue(dp, d.getOpposite());
                }
            }
        }
        if (st.hasProperty(BlockStateProperties.EAST) && st.hasProperty(BlockStateProperties.WEST)) {
            boolean e = st.getValue(BlockStateProperties.EAST);
            st = st.setValue(BlockStateProperties.EAST, st.getValue(BlockStateProperties.WEST)).setValue(BlockStateProperties.WEST, e);
        }
        return st;
    }

    static void apply(WorldGenLevel level, Ship s, BlockPos.MutableBlockPos m, Op op) {
        BlockState cur = level.getBlockState(m);
        if (!(cur.isAir() || cur.is(Blocks.WATER) || cur.canBeReplaced()) && op.extra() != Boolean.TRUE) {
            return;
        }
        Direction front = w(s, op.front());
        if (op.oc() != null && OfficeCraftCompat.isAvailable()) {
            List<BlockPos> cells = OfficeCraftCompat.cells(op.oc(), front);
            boolean ok = !cells.isEmpty();
            for (BlockPos c : cells) {
                BlockState here = level.getBlockState(m.offset(c));
                ok &= c.getY() >= 0 && c.getY() <= 1 && (here.isAir() || here.canBeReplaced());
            }
            if (ok && OfficeCraftCompat.place(level, m.immutable(), op.oc(), front, false, FurnitureBuilder.FLAGS)) {
                return;
            }
        }
        if (op.rf() != null) {
            BlockState r = FurnitureBuilder.refurbished(op.rf(), front);
            if (r != null) {
                level.setBlock(m, r, FurnitureBuilder.FLAGS);
                return;
            }
        }
        if (op.state() == null) {
            return;
        }
        BlockState st = mirror(s, op.state());
        level.setBlock(m, st, 2);
        if (op.loot() != null) {
            BlockEntity be = level.getBlockEntity(m);
            if (be instanceof RandomizableContainerBlockEntity rc) {
                rc.setLootTable(new ResourceLocation(OfficeRoomsIds.MOD_ID, "chests/" + op.loot()), Hashing.hash(s.seed, m.getX(), m.getY(), m.getZ()));
            }
        }
        if (op.extra() instanceof Stories.Book book) {
            BlockEntity be = level.getBlockEntity(m);
            if (be instanceof net.minecraft.world.level.block.entity.LecternBlockEntity lec) {
                lec.setBook(de.officerooms.special.SpecialDecorator.bookStack(book));
            }
        }
        if (op.extra() instanceof float[] guest) {             // a motionless figure (armour stand)
            ArmorStand a = EntityType.ARMOR_STAND.create(level.getLevel());
            if (a != null) {
                level.setBlock(m, Blocks.AIR.defaultBlockState(), 2);
                a.moveTo(m.getX() + 0.5, m.getY(), m.getZ() + 0.5, s.flip ? 360 - guest[0] : guest[0], 0);
                a.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LEATHER_CHESTPLATE));
                a.addTag(OfficeRoomsIds.MOD_ID + ".guest");
                level.addFreshEntity(a);
            }
        }
    }

    // --- building the plan -------------------------------------------------------------------------------------------

    static final class P {
        final Ship s;
        final List<Op> ops = new ArrayList<>();
        Hashing.Rng rng;
        /** The room being furnished: ordinary placements outside its cells are dropped. */
        Room cur;

        boolean inside(int lx, int lz) {
            return cur == null || s.room(cur.d, lx, lz) == cur;
        }

        void add(Op op) {
            if (op.extra() == Boolean.TRUE || inside(op.lx(), op.lz())) {
                ops.add(op);
            }
        }

        P(Ship s) {
            this.s = s;
        }

        void put(int lx, int y, int lz, BlockState st) {
            add(new Op(lx, y, lz, st, null, null, null, null, null));
        }

        void force(int lx, int y, int lz, BlockState st) {
            add(new Op(lx, y, lz, st, null, null, null, null, Boolean.TRUE));
        }

        void loot(int lx, int y, int lz, Direction f, String table) {
            add(new Op(lx, y, lz, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, f), table, null, null, null, null));
        }

        void barrel(int lx, int y, int lz, String table) {
            add(new Op(lx, y, lz, Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP), table, null, null, null, null));
        }

        /** Refurbished piece (usable side towards front) or the fallback. */
        void rf(int lx, int y, int lz, String id, Direction front, BlockState fallback) {
            add(new Op(lx, y, lz, fallback, null, id, null, front, null));
        }

        /** OfficeCraft piece (usable side towards front) or the fallback. */
        void oc(int lx, int y, int lz, String id, Direction front, BlockState fallback) {
            add(new Op(lx, y, lz, fallback, null, null, id, front, null));
        }

        void decor(int lx, int y, int lz, net.minecraftforge.registries.RegistryObject<Block> b, Direction front) {
            put(lx, y, lz, ShipBlocks.f(b, front));
        }

        /** Bed: head at (hx, hz), foot one step against dir. */
        void bed(int hx, int y, int hz, Direction dir, Block bed) {
            BlockState head = bed.defaultBlockState().setValue(BedBlock.FACING, dir).setValue(BedBlock.PART, BedPart.HEAD);
            put(hx, y, hz, head);
            put(hx - dir.getStepX(), y, hz - dir.getStepZ(), head.setValue(BedBlock.PART, BedPart.FOOT));
        }

        void book(int lx, int y, int lz, Direction f, Stories.Book b) {
            add(new Op(lx, y, lz, Blocks.LECTERN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, f)
                    .setValue(BlockStateProperties.HAS_BOOK, true), null, null, null, null, b));
        }

        void guest(int lx, int y, int lz, float yaw) {
            add(new Op(lx, y, lz, Blocks.AIR.defaultBlockState(), null, null, null, null, new float[]{yaw}));
        }

        boolean free(Room r, int lx, int lz) {
            if (!r.contains(lx, lz) || s.room(r.d, lx, lz) != r || s.code(r.d, lx, lz) != RM) {
                return false;
            }
            if (r.dx >= 0 && Math.abs(lx - r.dx) + Math.abs(lz - (r.dz - r.dirZ)) <= 0) {
                return false;                                   // keep the cell inside the door clear
            }
            for (Op op : ops) {
                if (op.lx() == lx && op.lz() == lz && op.y() == fy(r.d) + 1) {
                    return false;
                }
            }
            return true;
        }
    }

    static final Direction N = Direction.NORTH, S = Direction.SOUTH, E = Direction.EAST, Wd = Direction.WEST;

    static BlockState st(String id) {
        return ShipShape.id(id);
    }

    static List<Op> build(Ship s) {
        P p = new P(s);
        for (Room r : s.rooms) {
            p.rng = new Hashing.Rng(r.seed);
            p.cur = r;
            int y = fy(r.d) + 1;
            try {
                room(p, r, y);
            } catch (RuntimeException ex) {
                OfficeRooms.LOGGER.warn("SS Fun =): room {} failed: {}", r.kind, ex.toString());
            }
        }
        p.cur = null;
        extras(p);
        return p.ops;
    }

    /** Direction from the door into the room, and the back wall z. */
    static Direction inward(Room r) {
        return r.dirZ > 0 ? N : S;
    }

    static int backZ(Room r) {
        return r.dirZ > 0 ? r.z0 : r.z1;
    }

    static int frontZ(Room r) {
        return r.dirZ > 0 ? r.z1 : r.z0;
    }

    static void room(P p, Room r, int y) {
        Hashing.Rng g = p.rng;
        Ship s = p.s;
        switch (r.kind) {
            case CABIN, TWIN, MESSY, FLOODED, DARK, STATIC, PARTY, LUGGAGE, BARRICADED -> cabin(p, r, y);
            case UPSIDE -> {
                int bz = backZ(r);
                p.rf(r.x0, y + 2, bz, "oak_table", N, Blocks.OAK_PLANKS.defaultBlockState());
                p.put(r.x0 + 1, y + 2, bz, Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.HALF, Half.TOP).setValue(StairBlock.FACING, E));
                p.put(r.x1, y + 2, (r.z0 + r.z1) / 2, Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.HALF, Half.TOP).setValue(StairBlock.FACING, Wd));
                p.put(r.x1, y + 2, bz, Blocks.LANTERN.defaultBlockState());
                p.rf(r.x0, y, (r.z0 + r.z1) / 2, "oak_chair", inward(r), Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, inward(r)));
            }
            case STRIPPED -> p.rf(r.x0 + r.w() / 2, y, backZ(r), "oak_chair", inward(r).getOpposite(),
                    Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, inward(r)));
            case SUITE -> suite(p, r, y);
            case CREW_CABIN -> {
                int bz = backZ(r);
                Direction in = inward(r);
                for (int k = 0; k < Math.min(2, r.w() / 2); k++) {
                    p.bed(r.x0 + k * 2, y, bz, in, Blocks.GRAY_BED);
                }
                p.rf(r.x1, y, bz, "light_gray_kitchen_storage_cabinet", in.getOpposite(), Blocks.IRON_BLOCK.defaultBlockState());
                p.rf(r.x1, y + 1, bz, "light_gray_kitchen_storage_cabinet", in.getOpposite(), Blocks.AIR.defaultBlockState());
                p.rf(r.x1, y, bz - in.getStepZ() * 2, "dark_oak_desk", Wd, Blocks.SPRUCE_PLANKS.defaultBlockState());
                if (g.chance(0.6)) {
                    p.loot(r.x1 - 1, y, frontZ(r) == r.dz ? bz - in.getStepZ() * 2 : frontZ(r), in.getOpposite(), "ship_crew");
                }
                if (g.chance(0.4)) {
                    p.decor(r.x0, y + 1, frontZ(r), ShipBlocks.POSTER, in);
                }
            }
            case STORE -> {
                for (int lx = r.x0; lx <= r.x1; lx++) {
                    for (int lz = r.z0; lz <= r.z1; lz++) {
                        if (!p.free(r, lx, lz) || (lx == r.dx) || ((lx + lz) % 3 == 0)) {
                            continue;
                        }
                        double u = g.nextDouble();
                        if (u < 0.25) {
                            p.barrel(lx, y, lz, "ship_stores");
                        } else if (u < 0.55) {
                            p.rf(lx, y, lz, "spruce_crate", N, Blocks.BARREL.defaultBlockState());
                            if (g.chance(0.5)) {
                                p.rf(lx, y + 1, lz, "spruce_crate", N, Blocks.BARREL.defaultBlockState());
                            }
                        } else if (u < 0.62) {
                            p.loot(lx, y, lz, N, "ship_stores");
                        }
                    }
                }
            }
            case LOCKERS -> {
                for (int lx = r.x0; lx <= r.x1; lx++) {
                    if (lx == r.dx) {
                        continue;
                    }
                    int bz = backZ(r);
                    p.rf(lx, y, bz, "light_gray_kitchen_storage_cabinet", inward(r).getOpposite(), Blocks.IRON_TRAPDOOR.defaultBlockState());
                    p.rf(lx, y + 1, bz, "light_gray_kitchen_storage_cabinet", inward(r).getOpposite(), Blocks.IRON_TRAPDOOR.defaultBlockState());
                }
                p.rf(r.x0 + r.w() / 2, y, (r.z0 + r.z1) / 2, "gray_stool", N, Blocks.SPRUCE_SLAB.defaultBlockState());
                if (g.chance(0.5)) {
                    p.loot(r.x0, y, frontZ(r), inward(r), "ship_crew");
                }
            }
            case PIPES, PUMP -> {
                for (int lx = r.x0; lx <= r.x1; lx++) {
                    for (int lz : new int[]{r.z0, r.z1}) {
                        if (p.free(r, lx, lz) && lx != r.dx) {
                            for (int k = 0; k < 3; k++) {
                                p.force(lx, y + k, lz, ShipShape.b(ShipBlocks.PIPES));
                            }
                        }
                    }
                }
                if (r.kind == Kind.PUMP) {
                    for (int lx = r.x0 + 2; lx <= r.x1 - 2; lx += 3) {
                        p.force(lx, y, C, ShipShape.b(ShipBlocks.ENGINE));
                        p.force(lx, y + 1, C, ShipShape.b(ShipBlocks.ENGINE_TOP));
                        p.put(lx, y + 2, C, Blocks.CHAIN.defaultBlockState());
                    }
                }
                p.put(r.x0 + 1, y + 1, r.z0 + 1, Blocks.LEVER.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE,
                        net.minecraft.world.level.block.state.properties.AttachFace.FLOOR));
            }
            case LINEN -> {
                for (int lx = r.x0; lx <= r.x1; lx++) {
                    int bz = backZ(r);
                    if (lx == r.dx) {
                        continue;
                    }
                    p.put(lx, y, bz, Blocks.WHITE_WOOL.defaultBlockState());
                    if (g.chance(0.6)) {
                        p.put(lx, y + 1, bz, Blocks.WHITE_WOOL.defaultBlockState());
                    }
                }
                p.decor(r.x1, y, frontZ(r), ShipBlocks.WASHER, inward(r));
            }
            case TRASH -> {
                p.put(r.x0, y, backZ(r), Blocks.COMPOSTER.defaultBlockState());
                p.put(r.x0 + 1, y, backZ(r), Blocks.CAULDRON.defaultBlockState());
                p.barrel(r.x1, y, backZ(r), "ship_stores");
                p.put(r.x1, y, frontZ(r), Blocks.COBWEB.defaultBlockState());
                p.decor(r.x0 + 1, y, (r.z0 + r.z1) / 2, ShipBlocks.SUITCASE, E);
            }
            case WORKSHOP -> workshop(p, r, y);
            case EMBARK -> embark(p, r, y);
            case PURSER -> purser(p, r, y);
            case SHOPS -> shops(p, r, y);
            case CASINO -> casino(p, r, y);
            case BAR, LOUNGE -> lounge(p, r, y, r.kind == Kind.BAR);
            case THEATRE -> theatre(p, r, y);
            case DINING -> dining(p, r, y);
            case BUFFET -> buffet(p, r, y);
            case KIDS -> kids(p, r, y);
            case LIBRARY -> library(p, r, y);
            case SPA -> spa(p, r, y);
            case GAMES -> games(p, r, y);
            case CAPTAIN -> captain(p, r, y);
            case BRIDGE -> bridge(p, r, y);
            case ENGINE -> engine(p, r, y);
            case LAUNDRY -> laundry(p, r, y);
            case CREW_MESS -> mess(p, r, y);
            case GALLEY -> galley(p, r, y);
            case CREW_OFFICE -> crewOffice(p, r, y);
            case MEDICAL -> medical(p, r, y);
            case COLD_ROOM -> cold(p, r, y);
            case ATRIUM -> atrium(p, r, y);
            default -> {
            }
        }
    }

    // --- cabins ------------------------------------------------------------------------------------------------------

    static final Block[] BEDS = {Blocks.BLUE_BED, Blocks.LIGHT_BLUE_BED, Blocks.WHITE_BED, Blocks.CYAN_BED};

    static void cabin(P p, Room r, int y) {
        Hashing.Rng g = p.rng;
        Direction in = inward(r), out = in.getOpposite();
        int bz = backZ(r);
        Block bedB = BEDS[g.nextInt(BEDS.length)];
        int bx = r.dx == r.x0 ? r.x1 : r.x0;                    // bed away from the door column
        if (r.kind == Kind.TWIN && r.w() >= 4) {
            p.bed(r.x0, y, bz, in, bedB);
            p.bed(r.x1, y, bz, in, bedB);
            p.rf((r.x0 + r.x1) / 2, y, bz, "oak_drawer", out, Blocks.SPRUCE_TRAPDOOR.defaultBlockState());
        } else if (r.kind != Kind.LUGGAGE) {
            p.bed(bx, y, bz, in, bedB);
            int nx = bx == r.x0 ? bx + 1 : bx - 1;
            p.rf(nx, y, bz, "oak_drawer", out, Blocks.SPRUCE_TRAPDOOR.defaultBlockState());
            if (g.chance(0.5)) {
                p.rf(nx, y + 1, bz, "white_lamp", out, Blocks.AIR.defaultBlockState());
            }
        }
        // the wall at the door end: wardrobe, desk or TV
        int fz = frontZ(r);
        int wx = bx;
        if (r.dep() >= 5) {
            p.rf(wx, y, fz, "oak_storage_cabinet", in, Blocks.SPRUCE_PLANKS.defaultBlockState());
            if (r.kind == Kind.STATIC) {
                p.force(wx, y + 1, fz, de.officerooms.megastructure.CinemaBlocks.SCREEN_STATIC.get().defaultBlockState());
            } else if (g.chance(0.45)) {
                p.rf(wx, y + 1, fz, "television", in, Blocks.AIR.defaultBlockState());
            }
            int mz = (r.z0 + r.z1) / 2;
            if (r.dep() >= 6 && g.chance(0.6)) {
                p.rf(wx, y, mz, "oak_desk", bx == r.x0 ? E : Wd, Blocks.OAK_PLANKS.defaultBlockState());
                p.rf(bx == r.x0 ? wx + 1 : wx - 1, y, mz, "oak_chair", bx == r.x0 ? Wd : E,
                        Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, bx == r.x0 ? E : Wd));
            }
        }
        // the little bathroom in the other corner by the door (only in roomy cabins)
        if (r.w() >= 4 && r.dep() >= 6 && r.kind != Kind.LUGGAGE) {
            int tx = bx == r.x0 ? r.x1 : r.x0;
            if (tx != r.dx) {
                p.rf(tx, y, fz - in.getStepZ() * -0 + in.getStepZ() * 1, "white_toilet", bx == r.x0 ? Wd : E, Blocks.QUARTZ_SLAB.defaultBlockState());
                p.rf(tx, y, fz + in.getStepZ() * 2, "white_basin", bx == r.x0 ? Wd : E, Blocks.QUARTZ_SLAB.defaultBlockState());
            }
        }
        if (g.chance(r.kind == Kind.BARRICADED ? 1.0 : r.kind == Kind.MESSY ? 0.7 : 0.35)) {
            int lz = bz - in.getStepZ() * -2;
            lz = Math.max(r.z0, Math.min(r.z1, bz + in.getStepZ() * 2));
            p.loot(bx, y, lz, out == N || out == S ? (bx == r.x0 ? E : Wd) : out, r.kind == Kind.BARRICADED ? "ship_cabin_good" : "ship_cabin");
        }
        switch (r.kind) {
            case MESSY, LUGGAGE -> {
                int n = r.kind == Kind.LUGGAGE ? 7 : 3;
                for (int k = 0; k < n; k++) {
                    int lx = r.x0 + g.nextInt(r.w()), lz = r.z0 + g.nextInt(r.dep());
                    if (p.free(r, lx, lz) && lx != r.dx) {
                        p.decor(lx, y, lz, ShipBlocks.SUITCASE, g.chance(0.5) ? E : S);
                    }
                }
                if (r.kind == Kind.MESSY) {
                    p.put(r.x1, y + 2, r.z0, Blocks.COBWEB.defaultBlockState());
                }
            }
            case FLOODED -> {
                for (int k = 0; k < 3; k++) {
                    int lx = r.x0 + g.nextInt(r.w()), lz = r.z0 + g.nextInt(r.dep());
                    if (p.free(r, lx, lz)) {
                        p.force(lx, y, lz, Blocks.SEAGRASS.defaultBlockState());
                    }
                }
            }
            case DARK -> {
                int gx = bx == r.x0 ? r.x1 : r.x0;
                p.guest(gx, y, bz, inward(r) == N ? 0F : 180F);   // facing the back wall
            }
            case BARRICADED -> {
                if (r.dx >= 0) {
                    int iz = r.dz - r.dirZ;
                    p.force(r.dx, y, iz, Blocks.BARREL.defaultBlockState());
                    p.rf(r.dx, y + 1, iz, "spruce_crate", N, Blocks.BARREL.defaultBlockState());
                }
            }
            case PARTY -> {
                String[] bl = {"balloon_red", "balloon_blue", "balloon_pink", "balloon_green", "balloon_purple"};
                for (int k = 0; k < 4; k++) {
                    int lx = r.x0 + g.nextInt(r.w()), lz = r.z0 + g.nextInt(r.dep());
                    p.put(lx, y + 2, lz, st(bl[g.nextInt(bl.length)]));
                }
                p.put((r.x0 + r.x1) / 2, y, (r.z0 + r.z1) / 2, st("gift_box"));
                p.decor(bx == r.x0 ? r.x1 : r.x0, y + 1, bz, ShipBlocks.POSTER, in.getOpposite());
            }
            default -> {
                if (g.chance(0.25)) {
                    p.decor(bx == r.x0 ? r.x1 : r.x0, y + 1, bz, ShipBlocks.POSTER, out);
                }
            }
        }
    }

    static void suite(P p, Room r, int y) {
        Hashing.Rng g = p.rng;
        Direction in = inward(r);
        int bz = backZ(r);
        p.bed(r.x0 + 1, y, bz, in, Blocks.WHITE_BED);
        p.bed(r.x0 + 2, y, bz, in, Blocks.WHITE_BED);
        p.rf(r.x0, y, bz, "oak_drawer", in.getOpposite(), Blocks.SPRUCE_TRAPDOOR.defaultBlockState());
        p.rf(r.x0 + 3, y, bz, "oak_drawer", in.getOpposite(), Blocks.SPRUCE_TRAPDOOR.defaultBlockState());
        p.rf(r.x1, y, bz, "light_blue_sofa", Wd, Blocks.LIGHT_BLUE_WOOL.defaultBlockState());
        p.rf(r.x1, y, bz - in.getStepZ() * -1 + in.getStepZ(), "light_blue_sofa", Wd, Blocks.LIGHT_BLUE_WOOL.defaultBlockState());
        int mz = (r.z0 + r.z1) / 2;
        p.rf(r.x1 - 2, y, mz, "oak_table", N, Blocks.OAK_PLANKS.defaultBlockState());
        p.rf(r.x1 - 3, y, mz, "oak_chair", E, Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Wd));
        p.rf(r.x1, y, frontZ(r), "white_bath", Wd, Blocks.QUARTZ_SLAB.defaultBlockState());
        p.rf(r.x0, y, frontZ(r), "oak_storage_cabinet", in, Blocks.SPRUCE_PLANKS.defaultBlockState());
        if (g.chance(0.6)) {
            p.loot(r.x0 + 1, y, frontZ(r), in, "ship_cabin_good");
        }
        p.put(r.x0 + 3, y, frontZ(r), Blocks.POTTED_FERN.defaultBlockState());
    }

    // --- venues ------------------------------------------------------------------------------------------------------

    /** Cells of a venue that are part of the room on its floor, in a grid. */
    static boolean cell(P p, Room r, int lx, int lz) {
        return p.free(r, lx, lz);
    }

    static void embark(P p, Room r, int y) {
        int cx = (r.x0 + r.x1) / 2;
        for (int lx = cx - 4; lx <= cx + 4; lx++) {             // reception counter across the hall
            p.rf(lx, y, C - 7, "dark_oak_kitchen_cabinetry", S, Blocks.DARK_OAK_PLANKS.defaultBlockState());
        }
        p.oc(cx - 2, y, C - 8, "chair_black", S, Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, N));
        p.oc(cx + 2, y, C - 8, "chair_black", S, Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, N));
        p.rf(cx, y + 1, C - 7, "computer", S, Blocks.AIR.defaultBlockState());
        p.book(cx + 3, y, C - 6, S, WELCOME);
        for (int lx : new int[]{r.x0 + 3, r.x1 - 3}) {          // brass columns
            for (int lz : new int[]{C - 4, C + 4}) {
                for (int k = 0; k < 3; k++) {
                    p.put(lx, y + k, lz, ShipShape.b(ShipBlocks.BRASS_COLUMN));
                }
            }
        }
        for (int lx = r.x0 + 1; lx <= r.x1 - 1; lx += 6) {      // sofas facing the windows
            p.rf(lx, y, C + 14, "red_sofa", S, Blocks.RED_WOOL.defaultBlockState());
            p.rf(lx + 1, y, C + 14, "red_sofa", S, Blocks.RED_WOOL.defaultBlockState());
            p.put(lx + 3, y, C + 16, Blocks.POTTED_BAMBOO.defaultBlockState());
        }
        for (int k = 0; k < 5; k++) {
            p.decor(r.x0 + 2 + k, y, C + 6 + (k % 2), ShipBlocks.SUITCASE, k % 2 == 0 ? E : S);
        }
        p.put(r.x0 + 1, y, C - 1, Blocks.POTTED_FERN.defaultBlockState());
        p.put(r.x1 - 1, y, C + 1, Blocks.POTTED_FERN.defaultBlockState());
        p.loot(cx + 4, y, C - 8, S, "ship_purser");
    }

    static void purser(P p, Room r, int y) {
        for (int lx = r.x0 + 1; lx <= r.x1 - 1; lx++) {
            p.rf(lx, y, C - 3, "white_kitchen_cabinetry", S, Blocks.SMOOTH_QUARTZ.defaultBlockState());
        }
        for (int lx = r.x0 + 2; lx <= r.x1 - 2; lx += 4) {
            p.rf(lx, y, C - 5, "oak_desk", S, Blocks.OAK_PLANKS.defaultBlockState());
            p.oc(lx, y, C - 4, "chair_gray", N, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, S));
            p.rf(lx, y + 1, C - 5, "computer", S, Blocks.AIR.defaultBlockState());
        }
        p.oc(r.x0, y, C - 6, "printer", E, Blocks.SMOOTH_STONE.defaultBlockState());
        p.oc(r.x1, y, C - 6, "water_cooler", Wd, Blocks.GLASS.defaultBlockState());
        for (int lx = r.x0; lx <= r.x1; lx += 3) {
            p.barrel(lx, y, C + 6, null);
        }
        p.loot(r.x0 + 1, y, C - 6, S, "ship_purser");
    }

    static void shops(P p, Room r, int y) {
        Hashing.Rng g = p.rng;
        for (int lx = r.x0 + 1; lx <= r.x1 - 1; lx++) {
            for (int lz : new int[]{C - 6, C + 6}) {
                boolean shutter = (lx - r.x0) % 6 == 0;
                if (shutter) {
                    p.put(lx, y, lz, Blocks.IRON_BARS.defaultBlockState());
                    p.put(lx, y + 1, lz, Blocks.IRON_BARS.defaultBlockState());
                } else {
                    p.rf(lx, y, lz, "oak_storage_cabinet", lz < C ? S : N, Blocks.BOOKSHELF.defaultBlockState());
                    p.put(lx, y + 1, lz, g.chance(0.5) ? Blocks.GLASS.defaultBlockState() : Blocks.DECORATED_POT.defaultBlockState());
                }
            }
        }
        for (int lx = r.x0 + 3; lx <= r.x1 - 3; lx += 5) {
            p.put(lx, y, C - 2, Blocks.GLASS.defaultBlockState());
            p.put(lx, y, C + 2, Blocks.GLASS.defaultBlockState());
            p.decor(lx + 1, y, C, ShipBlocks.SUITCASE, E);
        }
        p.loot(r.x1 - 1, y, C, Wd, "ship_purser");
    }

    static void casino(P p, Room r, int y) {
        Hashing.Rng g = p.rng;
        for (int lx = r.x0 + 2; lx <= r.x1 - 2; lx++) {
            for (int lz : new int[]{C - 12, C - 11, C + 11, C + 12}) {
                if ((lx - r.x0) % 5 == 0 || !p.free(r, lx, lz)) {
                    continue;
                }
                Direction f = lz == C - 12 || lz == C + 11 ? (lz < C ? S : N) : (lz < C ? N : S);
                if (lz == C - 11 || lz == C + 12) {
                    f = f.getOpposite();
                }
                p.decor(lx, y, lz, ShipBlocks.SLOT_MACHINE, lz < C ? (lz == C - 12 ? S : N) : (lz == C + 12 ? N : S));
                p.decor(lx, y + 1, lz, ShipBlocks.SLOT_MACHINE_TOP, lz < C ? (lz == C - 12 ? S : N) : (lz == C + 12 ? N : S));
            }
        }
        for (int lx = r.x0 + 4; lx <= r.x1 - 4; lx += 7) {      // card tables
            for (int lz : new int[]{C - 5, C + 4}) {
                p.rf(lx, y, lz, "dark_oak_table", N, Blocks.DARK_OAK_PLANKS.defaultBlockState());
                p.rf(lx + 1, y, lz, "dark_oak_table", N, Blocks.DARK_OAK_PLANKS.defaultBlockState());
                p.put(lx, y + 1, lz, Blocks.GREEN_CARPET.defaultBlockState());
                p.put(lx + 1, y + 1, lz, Blocks.GREEN_CARPET.defaultBlockState());
                p.rf(lx - 1, y, lz, "dark_oak_chair", E, Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Wd));
                p.rf(lx + 2, y, lz, "dark_oak_chair", Wd, Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, E));
                if (g.chance(0.5)) {
                    p.rf(lx, y, lz + 1, "dark_oak_chair", N, Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, S));
                }
            }
        }
        int cx = r.x1 - 2;                                      // the cashier's cage
        for (int lz = C - 2; lz <= C + 2; lz++) {
            p.put(cx, y, lz, Blocks.IRON_BARS.defaultBlockState());
            p.put(cx, y + 1, lz, Blocks.IRON_BARS.defaultBlockState());
        }
        p.loot(cx + 1, y, C, Wd, "ship_casino");
        p.loot(cx + 1, y, C + 1, Wd, "ship_casino");
    }

    static void lounge(P p, Room r, int y, boolean bar) {
        Hashing.Rng g = p.rng;
        int bz = C - 5;
        if (bar) {
            for (int lx = r.x0 + 2; lx <= r.x1 - 2; lx++) {
                p.rf(lx, y, bz, "dark_oak_kitchen_cabinetry", S, Blocks.DARK_OAK_PLANKS.defaultBlockState());
                if ((lx - r.x0) % 2 == 0) {
                    p.rf(lx, y, bz + 1, "black_stool", N, Blocks.DARK_OAK_FENCE.defaultBlockState());
                }
                p.rf(lx, y, bz - 1, "dark_oak_kitchen_storage_cabinet", S, Blocks.DARK_OAK_PLANKS.defaultBlockState());
                p.put(lx, y + 1, bz - 1, g.chance(0.5) ? Blocks.FLOWER_POT.defaultBlockState() : Blocks.GLASS_PANE.defaultBlockState());
            }
            p.barrel(r.x0 + 1, y, bz - 1, "ship_galley");
        }
        for (int lx = r.x0 + 2; lx <= r.x1 - 3; lx += 5) {
            int lz = bar ? C + 3 : C - 3 + (lx % 2) * 6;
            String sofa = g.chance(0.5) ? "red_sofa" : "brown_sofa";
            p.rf(lx, y, lz - 1, sofa, S, Blocks.RED_WOOL.defaultBlockState());
            p.rf(lx + 1, y, lz - 1, sofa, S, Blocks.RED_WOOL.defaultBlockState());
            p.rf(lx, y, lz + 1, sofa, N, Blocks.RED_WOOL.defaultBlockState());
            p.rf(lx + 1, y, lz + 1, sofa, N, Blocks.RED_WOOL.defaultBlockState());
            p.rf(lx, y, lz, "dark_oak_table", N, Blocks.DARK_OAK_SLAB.defaultBlockState());
            p.put(lx + 2, y, lz, Blocks.POTTED_FERN.defaultBlockState());
        }
        if (!bar) {
            p.put(r.x1 - 1, y, C, Blocks.JUKEBOX.defaultBlockState());
            p.put(r.x1 - 1, y, C + 1, Blocks.NOTE_BLOCK.defaultBlockState());
        }
    }

    static void theatre(P p, Room r, int y) {
        if (r.d != 6) {
            return;
        }
        int stage0 = r.x1 - 6;
        for (int lx = stage0; lx <= r.x1; lx++) {               // the stage, with curtains at the back
            for (int lz = r.z0; lz <= r.z1; lz++) {
                if (!p.free(r, lx, lz)) {
                    continue;
                }
                p.put(lx, y, lz, Blocks.DARK_OAK_PLANKS.defaultBlockState());
                if (lx >= r.x1 - 1) {
                    for (int k = 1; k < 7; k++) {
                        p.put(lx, y + k, lz, ShipShape.b(ShipBlocks.CURTAIN));
                    }
                }
            }
        }
        for (int lz = r.z0; lz <= r.z1; lz += 4) {
            p.put(stage0, y + 1, lz, ShipShape.b(ShipBlocks.LIGHT));   // footlights
        }
        p.decor(stage0 + 2, y + 1, C, ShipBlocks.POSTER, Wd);
        Hashing.Rng g = p.rng;
        for (int lx = r.x0 + 2; lx < stage0 - 2; lx += 2) {      // rows of seats facing the stage
            for (int lz = r.z0 + 1; lz <= r.z1 - 1; lz++) {
                if (Math.abs(lz - C) <= 1 || !p.free(r, lx, lz)) {
                    continue;                                   // centre aisle
                }
                boolean broken = g.chance(0.1);
                p.put(lx, y, lz, (broken ? de.officerooms.megastructure.CinemaBlocks.SEAT_BROKEN : de.officerooms.megastructure.CinemaBlocks.SEAT)
                        .get().defaultBlockState().setValue(de.officerooms.block.DecorBlock.FACING, E));
            }
        }
        // one guest is still watching
        p.guest(r.x0 + 6, y, C + 4, 270F);
    }

    static void dining(P p, Room r, int y) {
        Hashing.Rng g = p.rng;
        for (int lx = r.x0 + 3; lx <= r.x1 - 3; lx += 5) {
            for (int lz = r.z0 + 3; lz <= r.z1 - 3; lz += 5) {
                if (!p.free(r, lx, lz)) {
                    continue;
                }
                p.rf(lx, y, lz, "dark_oak_table", N, Blocks.DARK_OAK_PLANKS.defaultBlockState());
                p.put(lx, y + 1, lz, Blocks.WHITE_CARPET.defaultBlockState());
                if (g.chance(0.3)) {
                    p.put(lx, y + 1, lz, Blocks.CANDLE.defaultBlockState());
                }
                p.rf(lx - 1, y, lz, "dark_oak_chair", E, Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Wd));
                p.rf(lx + 1, y, lz, "dark_oak_chair", Wd, Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, E));
                p.rf(lx, y, lz - 1, "dark_oak_chair", S, Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, N));
                if (g.chance(0.7)) {
                    p.rf(lx, y, lz + 1, "dark_oak_chair", N, Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, S));
                }
                p.put(lx, y + 2, lz, Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true));
            }
        }
        p.guest(r.x0 + 3, y, C + 1, 90F);
    }

    static void buffet(P p, Room r, int y) {
        for (int lx = r.x0 + 4; lx <= r.x1 - 4; lx += 12) {
            for (int k = 0; k < 6; k++) {
                p.decor(lx + k, y, C - 1, ShipBlocks.BUFFET, N);
                p.decor(lx + k, y, C + 1, ShipBlocks.BUFFET, S);
            }
            p.put(lx - 1, y, C, Blocks.CAULDRON.defaultBlockState());
        }
        for (int lx = r.x0 + 2; lx <= r.x1 - 2; lx += 4) {
            for (int lz : new int[]{C - 9, C - 13, C + 9, C + 13}) {
                if (!p.free(r, lx, lz)) {
                    continue;
                }
                p.rf(lx, y, lz, "birch_table", N, Blocks.BIRCH_PLANKS.defaultBlockState());
                p.rf(lx - 1, y, lz, "birch_chair", E, Blocks.BIRCH_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Wd));
                p.rf(lx + 1, y, lz, "birch_chair", Wd, Blocks.BIRCH_STAIRS.defaultBlockState().setValue(StairBlock.FACING, E));
                if (p.rng.chance(0.25)) {
                    p.put(lx, y + 1, lz, Blocks.CAKE.defaultBlockState().setValue(BlockStateProperties.BITES, 1 + p.rng.nextInt(5)));
                }
            }
        }
        p.loot(r.x0 + 1, y, C, E, "ship_galley");
    }

    static void kids(P p, Room r, int y) {
        Hashing.Rng g = p.rng;
        int px = r.x0 + 3;                                      // the ball pit
        Block[] balls = {Blocks.RED_WOOL, Blocks.BLUE_WOOL, Blocks.YELLOW_WOOL, Blocks.LIME_WOOL};
        for (int lx = px; lx < px + 7; lx++) {
            for (int lz = C - 4; lz <= C + 4; lz++) {
                boolean rim = lx == px || lx == px + 6 || lz == C - 4 || lz == C + 4;
                p.put(lx, y, lz, rim ? Blocks.WHITE_CONCRETE.defaultBlockState() : balls[g.nextInt(balls.length)].defaultBlockState());
            }
        }
        for (int lx = r.x0 + 12; lx <= r.x1 - 3; lx += 4) {
            for (int lz : new int[]{C - 8, C + 8}) {
                p.put(lx, y, lz, st("party_table"));
                p.rf(lx - 1, y, lz, "light_blue_stool", N, Blocks.LIGHT_BLUE_WOOL.defaultBlockState());
                p.rf(lx + 1, y, lz, "pink_stool", N, Blocks.PINK_WOOL.defaultBlockState());
            }
        }
        String[] bl = {"balloon_red", "balloon_blue", "balloon_pink", "balloon_green", "balloon_purple"};
        for (int k = 0; k < 12; k++) {
            p.put(r.x0 + 1 + g.nextInt(r.w() - 2), y + 2, C - 12 + g.nextInt(25), st(bl[g.nextInt(bl.length)]));
        }
        for (int k = 0; k < 4; k++) {
            p.put(r.x1 - 2, y, C - 6 + k * 4, st("gift_box"));
        }
        // the big =) on the back wall and FUN in marquee letters above it
        int wz = r.z0;
        for (int k = 0; k < 2; k++) {
            for (int row = 0; row < 2; row++) {
                int lx = r.x0 + 10 + k;
                boolean lower = p.s.wx(lx) < p.s.wx(r.x0 + 10 + (1 - k));
                int col = lower ? 0 : 1;                        // seen from inside the room (from +z)
                p.force(lx, y + 1 + (1 - row), wz - 1, ShipShape.b(ShipBlocks.SMILE).setValue(ShipBlocks.Variant.VARIANT, col + 2 * row));
            }
        }
        String fun = "FUN";
        for (int k = 0; k < 3; k++) {
            p.put(r.x0 + 15 + k, y + 2, wz, de.officerooms.megastructure.CinemaBlocks.LETTER.get().defaultBlockState()
                    .setValue(de.officerooms.block.DecorBlock.FACING, S)
                    .setValue(de.officerooms.megastructure.CinemaBlocks.Letter.GLYPH, de.officerooms.megastructure.CinemaBlocks.Letter.glyph(fun.charAt(k))));
        }
        for (int lx = r.x0 + 2; lx <= r.x1 - 2; lx += 7) {
            p.decor(lx, y + 1, r.z1, ShipBlocks.POSTER, N);
        }
        p.book(r.x1 - 1, y, C, Wd, KIDS_RULES);
        p.loot(r.x1 - 1, y, C + 2, Wd, "ship_kids");
    }

    static void library(P p, Room r, int y) {
        for (int lx = r.x0; lx <= r.x1; lx++) {
            for (int lz : new int[]{r.z0, r.z1}) {
                if (p.free(r, lx, lz) && (lx - r.x0) % 6 != 3) {
                    for (int k = 0; k < 3; k++) {
                        p.put(lx, y + k, lz, Blocks.BOOKSHELF.defaultBlockState());
                    }
                }
            }
        }
        for (int lx = r.x0 + 3; lx <= r.x1 - 3; lx += 5) {
            p.rf(lx, y, C, "dark_oak_table", N, Blocks.DARK_OAK_PLANKS.defaultBlockState());
            p.rf(lx, y, C - 1, "dark_oak_chair", S, Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, N));
            p.rf(lx, y, C + 1, "dark_oak_chair", N, Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, S));
        }
        p.book(r.x0 + 1, y, C - 3, E, LOG);
        p.book(r.x1 - 1, y, C + 3, Wd, WELCOME);
    }

    static void spa(P p, Room r, int y) {
        int cx = (r.x0 + r.x1) / 2;
        for (int lx = cx - 5; lx <= cx + 5; lx++) {
            for (int lz = C - 3; lz <= C + 3; lz++) {
                boolean rim = lx == cx - 5 || lx == cx + 5 || Math.abs(lz - C) == 3;
                if (rim) {
                    p.put(lx, y, lz, ShipShape.b(ShipBlocks.POOL_TILE));
                } else {
                    p.force(lx, y - 1, lz, Blocks.WATER.defaultBlockState());
                    p.put(lx, y, lz, Blocks.WATER.defaultBlockState());
                }
            }
        }
        for (int lx = r.x0 + 2; lx <= r.x1 - 2; lx += 3) {
            p.decor(lx, y, C - 8, ShipBlocks.DECK_CHAIR, S);
            p.decor(lx, y, C + 8, ShipBlocks.DECK_CHAIR, N);
        }
        // the sauna: a small wooden box in a corner
        int sx = r.x0 + 1, sz = r.z0 + 1;
        for (int lx = sx; lx < sx + 5; lx++) {
            for (int lz = sz; lz < sz + 4; lz++) {
                boolean wall = lx == sx || lx == sx + 4 || lz == sz || lz == sz + 3;
                for (int k = 0; k < 3; k++) {
                    p.force(lx, y + k, lz, wall ? Blocks.SPRUCE_PLANKS.defaultBlockState() : Blocks.AIR.defaultBlockState());
                }
            }
        }
        p.force(sx + 2, y, sz + 3, Blocks.SPRUCE_DOOR.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, S));
        p.force(sx + 2, y + 1, sz + 3, Blocks.SPRUCE_DOOR.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, S)
                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
        p.force(sx + 1, y, sz + 1, Blocks.SPRUCE_SLAB.defaultBlockState());
        p.force(sx + 3, y, sz + 1, Blocks.MAGMA_BLOCK.defaultBlockState());
    }

    static void games(P p, Room r, int y) {
        for (int lx = r.x0 + 1; lx <= r.x1 - 1; lx += 2) {
            p.decor(lx, y, C - 6, de.officerooms.block.CuriosityBlocks.ARCADE, S);
            p.decor(lx, y + 1, C - 6, de.officerooms.block.CuriosityBlocks.ARCADE_TOP, S);
            p.decor(lx, y, C + 6, de.officerooms.block.CuriosityBlocks.ARCADE, N);
            p.decor(lx, y + 1, C + 6, de.officerooms.block.CuriosityBlocks.ARCADE_TOP, N);
        }
        for (int lx = r.x0 + 4; lx <= r.x1 - 4; lx += 6) {
            p.rf(lx, y, C, "white_kitchen_cabinetry", N, Blocks.SMOOTH_QUARTZ.defaultBlockState());
            p.rf(lx + 1, y, C, "white_kitchen_cabinetry", N, Blocks.SMOOTH_QUARTZ.defaultBlockState());
        }
        p.loot(r.x1 - 1, y, C + 2, Wd, "ship_kids");
    }

    static void captain(P p, Room r, int y) {
        int cx = (r.x0 + r.x1) / 2;
        p.rf(cx, y, C + 4, "dark_oak_desk", N, Blocks.DARK_OAK_PLANKS.defaultBlockState());
        p.rf(cx + 1, y, C + 4, "dark_oak_desk", N, Blocks.DARK_OAK_PLANKS.defaultBlockState());
        p.oc(cx, y, C + 5, "chair_black", N, Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, S));
        p.put(cx + 1, y + 1, C + 4, Blocks.CANDLE.defaultBlockState());
        for (int lx = r.x0; lx <= r.x1; lx++) {
            if (p.free(r, lx, r.z1)) {
                p.put(lx, y, r.z1, Blocks.BOOKSHELF.defaultBlockState());
                p.put(lx, y + 1, r.z1, Blocks.BOOKSHELF.defaultBlockState());
            }
        }
        p.put(cx - 3, y, C + 4, Blocks.CARTOGRAPHY_TABLE.defaultBlockState());
        p.bed(r.x0 + 1, y, r.z0, N, Blocks.BLUE_BED);
        p.loot(cx + 3, y, C + 6, N, "ship_captain");
        p.book(cx - 2, y, C + 6, N, LOG);
    }

    static void bridge(P p, Room r, int y) {
        for (int lz = r.z0 + 1; lz <= r.z1 - 1; lz++) {
            if (p.free(r, r.x1, lz)) {
                p.decor(r.x1, y, lz, ShipBlocks.CONSOLE, Wd);
            }
            if (Math.abs(lz - C) % 4 == 1 && p.free(r, r.x1 - 1, lz)) {
                p.oc(r.x1 - 1, y, lz, "chair_black", E, Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Wd));
            }
        }
        int cx = (r.x0 + r.x1) / 2;
        p.put(cx, y, C, Blocks.CARTOGRAPHY_TABLE.defaultBlockState());
        p.put(cx, y, C + 1, Blocks.LEVER.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE,
                net.minecraft.world.level.block.state.properties.AttachFace.FLOOR));
        p.oc(cx - 2, y, C - 4, "printer", S, Blocks.SMOOTH_STONE.defaultBlockState());
        p.loot(r.x0 + 1, y, C - 6, S, "ship_captain");
    }

    static void engine(P p, Room r, int y) {
        for (int k = 0; k < 3; k++) {
            int ex = ENG0 + 8 + k * 18;
            for (int s : new int[]{-1, 1}) {
                for (int lx = ex; lx < ex + 12; lx++) {
                    for (int e = 3; e <= 7; e++) {
                        int lz = C + s * e;
                        int top = e == 3 || e == 7 ? 2 : 3;
                        for (int q = 0; q < top; q++) {
                            p.force(lx, y + q, lz, ShipShape.b(ShipBlocks.ENGINE));
                        }
                        p.force(lx, y + top, lz, ShipShape.b(ShipBlocks.ENGINE_TOP));
                    }
                }
                for (int q = 4; q < 13; q++) {                  // exhaust pipes up to the top of the hall
                    p.force(ex + 3, y + q, C + s * 5, ShipShape.b(ShipBlocks.PIPES));
                    p.force(ex + 8, y + q, C + s * 5, ShipShape.b(ShipBlocks.PIPES));
                }
            }
        }
        for (int lx = ENG0 + 2; lx <= ENG1 - 2; lx += 9) {
            p.decor(lx, y, C - 10, ShipBlocks.CONSOLE, S);
        }
        p.loot(ENG0 + 1, y, C + 10, N, "ship_stores");
    }

    static void laundry(P p, Room r, int y) {
        for (int lx = r.x0 + 1; lx <= r.x1 - 1; lx++) {
            p.decor(lx, y, C - 6, ShipBlocks.WASHER, S);
            p.decor(lx, y + 1, C - 6, ShipBlocks.WASHER, S);
            if (lx % 3 != 0) {
                p.decor(lx, y, C + 6, ShipBlocks.WASHER, N);
            }
        }
        for (int lx = r.x0 + 3; lx <= r.x1 - 3; lx += 4) {
            p.put(lx, y, C - 3, Blocks.WHITE_WOOL.defaultBlockState());
            p.put(lx, y + 1, C - 3, Blocks.WHITE_WOOL.defaultBlockState());
            p.put(lx + 1, y, C + 3, Blocks.LIGHT_GRAY_WOOL.defaultBlockState());
        }
        p.loot(r.x1 - 1, y, C + 4, Wd, "ship_crew");
    }

    static void mess(P p, Room r, int y) {
        for (int lx = r.x0 + 2; lx <= r.x1 - 2; lx++) {
            for (int lz : new int[]{C - 4, C + 4}) {
                p.rf(lx, y, lz, "oak_table", N, Blocks.OAK_PLANKS.defaultBlockState());
                if (lx % 2 == 0) {
                    p.rf(lx, y, lz - 1, "gray_stool", N, Blocks.SPRUCE_SLAB.defaultBlockState());
                    p.rf(lx, y, lz + 1, "gray_stool", N, Blocks.SPRUCE_SLAB.defaultBlockState());
                }
            }
        }
        p.decor(r.x1 - 1, y, C, de.officerooms.block.CuriosityBlocks.VENDING, Wd);
        p.decor(r.x1 - 1, y + 1, C, de.officerooms.block.CuriosityBlocks.VENDING_TOP, Wd);
        p.loot(r.x0 + 1, y, C, E, "ship_galley");
    }

    static void galley(P p, Room r, int y) {
        String[] row = {"light_stove", "white_kitchen_cabinetry", "white_kitchen_sink", "white_kitchen_cabinetry", "light_fridge", "light_freezer"};
        for (int lx = r.x0 + 1; lx <= r.x1 - 1; lx++) {
            String id = row[(lx - r.x0) % row.length];
            p.rf(lx, y, C - 6, id, S, Blocks.SMOOTH_STONE.defaultBlockState());
            if (lx % 4 != 0) {
                p.rf(lx, y, C - 1, "white_kitchen_cabinetry", N, Blocks.SMOOTH_STONE.defaultBlockState());
                p.rf(lx, y, C + 1, "white_kitchen_cabinetry", S, Blocks.SMOOTH_STONE.defaultBlockState());
            }
        }
        for (int lx = r.x0 + 2; lx <= r.x1 - 2; lx += 6) {
            p.put(lx, y + 1, C - 1, Blocks.CAULDRON.defaultBlockState());
            p.loot(lx, y, C + 6, N, "ship_galley");
        }
    }

    static void crewOffice(P p, Room r, int y) {
        for (int lx = r.x0 + 1; lx <= r.x1 - 1; lx += 3) {
            for (int lz : new int[]{C - 4, C + 4}) {
                p.rf(lx, y, lz, "oak_desk", lz < C ? S : N, Blocks.OAK_PLANKS.defaultBlockState());
                p.rf(lx, y + 1, lz, "computer", lz < C ? S : N, Blocks.AIR.defaultBlockState());
                p.oc(lx, y, lz < C ? lz + 1 : lz - 1, p.rng.chance(0.5) ? "chair_gray" : "chair_black", lz < C ? N : S,
                        Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, lz < C ? S : N));
            }
        }
        p.oc(r.x0, y, C, "water_cooler", E, Blocks.GLASS.defaultBlockState());
        p.oc(r.x1, y, C, "printer", Wd, Blocks.SMOOTH_STONE.defaultBlockState());
        p.oc(r.x1, y, C + 2, "coffee_machine", Wd, Blocks.SMOOTH_STONE.defaultBlockState());
        p.barrel(r.x0, y, C + 6, "ship_crew");
        p.book(r.x0 + 2, y, C, E, CREW_NOTICE);
    }

    static void medical(P p, Room r, int y) {
        for (int lx = r.x0 + 1; lx <= r.x1 - 1; lx += 3) {
            p.bed(lx, y, C - 6, N, Blocks.WHITE_BED);
        }
        p.put(r.x1 - 1, y, C + 5, Blocks.BREWING_STAND.defaultBlockState());
        p.put(r.x1 - 2, y, C + 5, Blocks.CAULDRON.defaultBlockState());
        p.loot(r.x0 + 1, y, C + 5, N, "ship_medical");
        p.guest(r.x0 + 4, y, C + 2, 0F);
    }

    static void cold(P p, Room r, int y) {
        for (int lx = r.x0; lx <= r.x1; lx++) {
            for (int lz : new int[]{r.z0, r.z1}) {
                if (p.free(r, lx, lz)) {
                    p.put(lx, y, lz, Blocks.PACKED_ICE.defaultBlockState());
                    p.put(lx, y + 1, lz, Blocks.ICE.defaultBlockState());
                }
            }
        }
        for (int lx = r.x0 + 2; lx <= r.x1 - 2; lx += 3) {
            p.put(lx, y + 2, C, Blocks.CHAIN.defaultBlockState());
            p.put(lx, y + 1, C, Blocks.CHAIN.defaultBlockState());
        }
        p.barrel(r.x0 + 1, y, C + 1, "ship_galley");
    }

    static void atrium(P p, Room r, int y) {
        if (r.d != 5) {
            return;
        }
        int cx = (ATR0 + ATR1) / 2;
        for (int lx = cx - 3; lx <= cx + 3; lx++) {             // fountain
            for (int lz = C - 3; lz <= C + 3; lz++) {
                boolean rim = Math.abs(lx - cx) == 3 || Math.abs(lz - C) == 3;
                p.put(lx, y, lz, rim ? Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState() : Blocks.WATER.defaultBlockState());
            }
        }
        p.put(cx, y + 1, C, ShipShape.b(ShipBlocks.BRASS_COLUMN));
        for (int lx : new int[]{ATR0 + 2, ATR1 - 2}) {          // columns through all four decks
            for (int s : new int[]{-1, 1}) {
                for (int yy = y; yy < fy(8) + 4; yy++) {
                    p.force(lx, yy, C + s * 5, ShipShape.b(ShipBlocks.BRASS_COLUMN));
                }
            }
        }
        for (int lx = ATR0 + 4; lx <= ATR1 - 4; lx += 4) {
            p.rf(lx, y, C - 5, "light_blue_sofa", S, Blocks.LIGHT_BLUE_WOOL.defaultBlockState());
            p.rf(lx, y, C + 5, "light_blue_sofa", N, Blocks.LIGHT_BLUE_WOOL.defaultBlockState());
        }
        p.put(ATR0 + 3, y, C, Blocks.POTTED_BAMBOO.defaultBlockState());
        p.put(ATR1 - 3, y, C, Blocks.POTTED_BAMBOO.defaultBlockState());
    }

    static void workshop(P p, Room r, int y) {
        int bz = r.dx >= 0 ? backZ(r) : r.z0;
        p.put(r.x0, y, bz, Blocks.ANVIL.defaultBlockState());
        p.put(r.x0 + 1, y, bz, Blocks.SMITHING_TABLE.defaultBlockState());
        p.put(r.x0 + 2, y, bz, Blocks.GRINDSTONE.defaultBlockState());
        p.put(r.x1, y, bz, Blocks.CRAFTING_TABLE.defaultBlockState());
        p.loot(r.x1, y, (r.z0 + r.z1) / 2, Wd, "ship_crew");
        for (int lx = r.x0; lx <= r.x1; lx += 2) {
            p.put(lx, y + 2, bz, ShipShape.b(ShipBlocks.PIPES));
        }
    }

    // --- ship-wide extras --------------------------------------------------------------------------------------------

    static void extras(P p) {
        Ship s = p.s;
        // lifeboat release levers on the promenade wall
        for (int[] lb : s.lifeboats) {
            int lx = lb[0] + 4, side = lb[1];
            int in = s.inner(10, lx);
            int lz = C + side * (in - 5);
            if (s.hasFlight(lx, lz) || s.code(10, lx, lz) != PR) {
                continue;
            }
            p.put(lx, fy(10) + 2, lz, ShipBlocks.f(ShipBlocks.LIFEBOAT_RELEASE, side > 0 ? S : N));
            p.put(lx + 1, fy(10) + 1, lz, ShipBlocks.f(ShipBlocks.LIFE_RING, side > 0 ? S : N));
        }
        // deck chairs on the sun deck, and a bar by the pool
        for (int lx = 64; lx <= SUP1 - 10; lx += 3) {
            if (lx >= 36 && lx <= 56 || lx >= 96 && lx <= 122) {
                continue;
            }
            for (int side : new int[]{-1, 1}) {
                int in = s.inner(10, lx);
                int lz = C + side * (in - 3);
                if (s.code(11, lx, lz) == TOP && !s.hasFlight(lx, lz)) {
                    p.decor(lx, fy(11) + 1, lz, ShipBlocks.DECK_CHAIR, side > 0 ? N : S);
                }
            }
        }
        for (int lx = 124; lx <= 130; lx++) {
            p.rf(lx, fy(11) + 1, C - 6, "white_kitchen_cabinetry", S, Blocks.SMOOTH_QUARTZ.defaultBlockState());
            p.rf(lx, fy(11) + 1, C - 4, "white_stool", N, Blocks.BIRCH_FENCE.defaultBlockState());
        }
        // sofas in the window lounges beside the stair cores
        for (int cx : CORES) {
            for (int d = 5; d <= 9; d++) {
                for (int side : new int[]{-1, 1}) {
                    int lz = C + side * 12;
                    if (s.code(d, cx + 4, lz) == LO && s.code(d, cx + 5, lz) == LO) {
                        p.rf(cx + 4, fy(d) + 1, lz, "red_sofa", side > 0 ? S : N, Blocks.RED_WOOL.defaultBlockState());
                        p.rf(cx + 5, fy(d) + 1, lz, "red_sofa", side > 0 ? S : N, Blocks.RED_WOOL.defaultBlockState());
                        p.put(cx + 7, fy(d) + 1, lz, Blocks.POTTED_FERN.defaultBlockState());
                    }
                }
            }
        }
        // a flag at the stern
        p.put(3, fy(10) + 1, C, Blocks.WHITE_BANNER.defaultBlockState());
    }

    // --- books -------------------------------------------------------------------------------------------------------

    static final Stories.Book WELCOME = new Stories.Book("ship_welcome", "Welcome Aboard!", "SS Fun =) Guest Services", new String[]{
            "WELCOME ABOARD\nthe SS Fun =)!\n\nYour cabin number is printed on your boarding pass. Please do not lose your boarding pass.\n\nThere is only one boarding pass.",
            "LIFE ON BOARD\n\nMeals are served on Deck 7 and Deck 9 at all hours. The buffet is always full.\n\nNobody has seen the kitchen staff. Please do not look for the kitchen staff.",
            "SAFETY\n\nIn an emergency, proceed to your lifeboat station on Deck 10.\n\nThe stairs to Deck 10 are closed for maintenance. One staircase is not.",
            "ENTERTAINMENT\n\nThe Kids' Club on Deck 8 is open day and night. Our mascot loves visitors!\n\nIf you hear squeaking in the ceiling, stay where you are. It is only saying hello."});
    static final Stories.Book LOG = new Stories.Book("ship_log", "Captain's Log", "The Captain", new String[]{
            "Day 1.\nSea calm. Fog thick. 2,400 passengers boarded through gate 26. Weather report: fog.",
            "Day 9.\nWe have not reached a port. The charts show only water. The compass points at the funnel.\n\nThe passengers say the ship is getting longer.",
            "Day 31.\nThe crew are gone. All of them. Their cabins are made up as if nobody slept there.\n\nThe mascot was in the bridge tonight. It waved at me through the glass.",
            "Day ?\nIf you read this: do not stay below Deck 8. The lifeboats still work. Pull the red handle on the promenade.\n\nDo not take the mascot with you."});
    static final Stories.Book KIDS_RULES = new Stories.Book("ship_kids", "Kids' Club Rules", "Your Friend =)", new String[]{
            "KIDS' CLUB RULES\n\n1. Always smile =)\n2. No running in the hallways\n3. Hugs are free!",
            "4. Never go into the ceiling\n5. Never tell a grown-up where I am\n6. Stay for one more game\n\n=)"});
    static final Stories.Book CREW_NOTICE = new Stories.Book("ship_crew", "Crew Notice", "Chief Purser", new String[]{
            "NOTICE TO ALL CREW\n\nVents on decks 5 to 9 are to remain CLOSED. Do not open them to retrieve lost items.\n\nWet carpets are to be reported, not dried.",
            "The mascot costume is no longer stored in the Kids' Club. We do not know where it is stored.\n\nIf you see it walking, it is not one of us."});
}
