package de.officerooms.megastructure;

import static de.officerooms.megastructure.Mall.*;

import de.officerooms.OfficeRooms;
import de.officerooms.core.OfficeRoomsIds;
import de.officerooms.compat.OfficeCraftCompat;
import de.officerooms.core.Hashing;
import de.officerooms.compat.FurnitureBuilder;
import de.officerooms.room.OfficePlanner;
import de.officerooms.special.Stories;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.registries.RegistryObject;

/**
 * 0.43.0: furnishing of the Dead Mall: shop interiors, the food court, kiosks, benches, signs, mannequins and loot.
 * Furniture from Refurbished Furniture / OfficeCraft where installed, own blocks or vanilla otherwise. The plan is
 * built once per mall (region-local coordinates) and applied chunk by chunk.
 */
public final class MallDecorator {

    private MallDecorator() {
    }

    record Mannequin(float yaw, int color, boolean arms) {
    }

    record Op(int lx, int y, int lz, BlockState state, String loot, String rf, String oc, Direction front, Object extra) {
    }

    private static final Map<Long, List<Op>> PLANS = new ConcurrentHashMap<>();

    static final Direction N = Direction.NORTH, S = Direction.SOUTH, E = Direction.EAST, Wd = Direction.WEST;

    public static void decorate(OfficePlanner pl, Mall.Site s, WorldGenLevel level, ChunkAccess chunk) {
        long key = Hashing.hash(s.seed, s.rx, s.rz, 0xA1D0, 0);
        List<Op> plan = PLANS.get(key);
        if (plan == null) {
            plan = build(s);
            if (PLANS.size() > 6) {
                PLANS.clear();
            }
            PLANS.put(key, plan);
        }
        int baseX = s.rx * pl.regionBlocks;
        int baseZ = s.rz * pl.regionBlocks;
        int cx = chunk.getPos().x;
        int cz = chunk.getPos().z;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (Op op : plan) {
            int x = baseX + op.lx();
            int z = baseZ + op.lz();
            if ((x >> 4) != cx || (z >> 4) != cz) {
                continue;
            }
            m.set(x, op.y(), z);
            try {
                apply(level, s, m, op);
            } catch (RuntimeException ex) {
                OfficeRooms.LOGGER.warn("Dead Mall: could not place {} at {}: {}", op, m, ex.toString());
            }
        }
    }

    static void apply(WorldGenLevel level, Mall.Site s, BlockPos.MutableBlockPos m, Op op) {
        BlockState cur = level.getBlockState(m);
        if (!(cur.isAir() || cur.canBeReplaced()) && op.extra() != Boolean.TRUE) {
            return;
        }
        if (op.oc() != null && OfficeCraftCompat.isAvailable()) {
            List<BlockPos> cells = OfficeCraftCompat.cells(op.oc(), op.front());
            boolean ok = !cells.isEmpty();
            for (BlockPos c : cells) {
                BlockState here = level.getBlockState(m.offset(c));
                ok &= c.getY() >= 0 && c.getY() <= 1 && (here.isAir() || here.canBeReplaced());
            }
            if (ok && OfficeCraftCompat.place(level, m.immutable(), op.oc(), op.front(), false, FurnitureBuilder.FLAGS)) {
                return;
            }
        }
        if (op.rf() != null) {
            BlockState r = FurnitureBuilder.refurbished(op.rf(), op.front());
            if (r != null) {
                level.setBlock(m, r, FurnitureBuilder.FLAGS);
                return;
            }
        }
        if (op.extra() instanceof Mannequin mq) {
            ArmorStand a = EntityType.ARMOR_STAND.create(level.getLevel());
            if (a != null) {
                CompoundTag t = new CompoundTag();
                t.putBoolean("NoBasePlate", false);
                t.putBoolean("ShowArms", mq.arms());
                a.readAdditionalSaveData(t);
                a.moveTo(m.getX() + 0.5, m.getY(), m.getZ() + 0.5, mq.yaw(), 0);
                if (mq.color() >= 0) {
                    a.setItemSlot(EquipmentSlot.CHEST, dyed(Items.LEATHER_CHESTPLATE, mq.color()));
                    a.setItemSlot(EquipmentSlot.LEGS, dyed(Items.LEATHER_LEGGINGS, (mq.color() * 7 + 0x303030) & 0xFFFFFF));
                }
                a.addTag(OfficeRoomsIds.MOD_ID + ".mannequin");
                level.addFreshEntity(a);
            }
            return;
        }
        if (op.state() == null) {
            return;
        }
        level.setBlock(m, op.state(), 2);
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
    }

    private static ItemStack dyed(net.minecraft.world.item.Item item, int color) {
        ItemStack st = new ItemStack(item);
        if (item instanceof DyeableLeatherItem d) {
            d.setColor(st, color);
        }
        return st;
    }

    // --- plan ----------------------------------------------------------------------------------------------------------

    static final class P {
        final Mall.Site s;
        final List<Op> ops = new ArrayList<>();
        Hashing.Rng rng;

        P(Mall.Site s) {
            this.s = s;
        }

        void put(int lx, int y, int lz, BlockState st) {
            if (lx > 2 && lz > 2 && lx < 158 && lz < 158) {
                ops.add(new Op(lx, y, lz, st, null, null, null, null, null));
            }
        }

        void force(int lx, int y, int lz, BlockState st) {
            ops.add(new Op(lx, y, lz, st, null, null, null, null, Boolean.TRUE));
        }

        void decor(int lx, int y, int lz, RegistryObject<Block> b, Direction front) {
            put(lx, y, lz, MallBlocks.f(b, front));
        }

        void loot(int lx, int y, int lz, Direction f, String table) {
            ops.add(new Op(lx, y, lz, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, f), table, null, null, null, null));
        }

        void rf(int lx, int y, int lz, String id, Direction front, BlockState fallback) {
            ops.add(new Op(lx, y, lz, fallback, null, id, null, front, null));
        }

        void oc(int lx, int y, int lz, String id, Direction front, BlockState fallback) {
            ops.add(new Op(lx, y, lz, fallback, null, null, id, front, null));
        }

        void mannequin(int lx, int y, int lz, Direction faces, int color) {
            ops.add(new Op(lx, y, lz, null, null, null, null, null, new Mannequin(faces.toYRot(), color, rng.nextDouble() < 0.5)));
        }

        void book(int lx, int y, int lz, Direction f, Stories.Book b) {
            ops.add(new Op(lx, y, lz, Blocks.LECTERN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, f)
                    .setValue(BlockStateProperties.HAS_BOOK, true), null, null, null, null, b));
        }

        /** Letters reading left to right for someone looking at the face; centred on (lx, lz). */
        void text(String t, int lx, int y, int lz, Direction face, double missing) {
            Direction step = face.getCounterClockWise();
            int start = -(t.length() - 1) / 2;
            for (int k = 0; k < t.length(); k++) {
                char c = t.charAt(k);
                if (c == ' ' || rng.nextDouble() < missing) {
                    continue;
                }
                int o = start + k;
                put(lx + step.getStepX() * o, y, lz + step.getStepZ() * o, CinemaBlocks.LETTER.get().defaultBlockState()
                        .setValue(de.officerooms.block.DecorBlock.FACING, face).setValue(CinemaBlocks.Letter.GLYPH, CinemaBlocks.Letter.glyph(c)));
            }
        }
    }

    static int ux(int q, int a) {
        return (q & 1) == 0 ? C0 - 1 - a : C1 + 1 + a;
    }

    static int uz(int q, int b) {
        return (q & 2) == 0 ? C0 - 1 - b : C1 + 1 + b;
    }

    /** Front of a unit: towards its concourse. */
    static Direction front(int q, int strip) {
        return strip == EW ? ((q & 2) == 0 ? S : N) : ((q & 1) == 0 ? E : Wd);
    }

    /** Direction of increasing w along a unit. */
    static Direction along(int q, int strip) {
        return strip == EW ? ((q & 1) == 0 ? Wd : E) : ((q & 2) == 0 ? N : S);
    }

    /** One shop unit, with helpers in its own coordinates (d from the front, w along). */
    static final class U {
        final P p;
        final int f, q, strip, idx, width, kind, y;
        final Direction fr, al;

        U(P p, int f, int q, int strip, int idx) {
            this.p = p;
            this.f = f;
            this.q = q;
            this.strip = strip;
            this.idx = idx;
            this.width = strip == EW ? 11 : 12;
            this.kind = p.s.unitKind(f, q, strip, idx);
            this.y = p.s.yBase + f * FH + 1;
            this.fr = front(q, strip);
            this.al = along(q, strip);
        }

        int lx(int d, int w) {
            return strip == EW ? ux(q, idx * 12 + w) : ux(q, d);
        }

        int lz(int d, int w) {
            return strip == EW ? uz(q, d) : uz(q, 21 + idx * 13 + w);
        }

        void put(int d, int w, int o, BlockState st) {
            p.put(lx(d, w), y + o, lz(d, w), st);
        }

        void decor(int d, int w, int o, RegistryObject<Block> b, Direction face) {
            p.decor(lx(d, w), y + o, lz(d, w), b, face);
        }

        void rf(int d, int w, int o, String id, Direction face, BlockState fallback) {
            p.rf(lx(d, w), y + o, lz(d, w), id, face, fallback);
        }

        void oc(int d, int w, int o, String id, Direction face, BlockState fallback) {
            p.oc(lx(d, w), y + o, lz(d, w), id, face, fallback);
        }

        void loot(int d, int w, String table) {
            p.loot(lx(d, w), y, lz(d, w), fr.getOpposite(), table);
        }

        void mannequin(int d, int w, Direction faces) {
            p.mannequin(lx(d, w), y, lz(d, w), faces, p.rng.nextInt(0xFFFFFF));
        }
    }

    static List<Op> build(Mall.Site s) {
        P p = new P(s);
        p.rng = new Hashing.Rng(Hashing.hash(s.seed, s.rx, s.rz, 0xA1D1, 0));
        for (int f = 0; f < FLOORS; f++) {
            for (int q = 0; q < 4; q++) {
                for (int idx = 0; idx < 5; idx++) {
                    unit(p, new U(p, f, q, EW, idx));
                }
                for (int idx = 0; idx < 3; idx++) {
                    unit(p, new U(p, f, q, NS, idx));
                }
                department(p, f, q);
                service(p, f, q);
            }
            concourseDecor(p, f);
        }
        foodCourt(p);
        entrance(p);
        daylight(p);
        return p.ops;
    }

    // --- shop units ----------------------------------------------------------------------------------------------------

    static final String[][] NAMES = {
            {}, {}, {"MODA", "THREADS", "LOOKS", "STYLE CO", "DENIM", "BOUTIQUE"}, {"SOLE", "STEPS", "SHOES", "FOOTWEAR"},
            {"VIDEO", "TV HIFI", "SOUND", "ELECTRO"}, {"BOOKS", "PAGES", "READ MORE"}, {"TOYS", "FUN TIME", "PLAY"}, {"PETS", "AQUARIUM"},
            {"SALON", "HAIR"}, {}, {"PIZZA", "WOK", "BURGER", "TACOS", "PRETZEL", "SUBS", "GYROS", "NOODLES", "DONUTS", "COFFEE", "JUICE", "CHICKEN"},
            {"MALL OFFICE"}, {"SECURITY"}};

    static String name(U u) {
        int k = u.kind;
        if (k == SHUTTER || k == DARK) {
            k = 2 + u.p.rng.nextInt(7);
        }
        String[] n = NAMES[k];
        return n.length == 0 ? null : n[u.p.rng.nextInt(n.length)];
    }

    static void unit(P p, U u) {
        int fl = (u.width + 1) / 2;
        if (u.kind != LEASE) {
            String n = name(u);
            if (n != null) {
                double missing = u.kind == SHUTTER ? 0.25 : u.kind == DARK ? 0.12 : 0.04;
                p.text(n, u.lx(-1, fl), u.y + 5, u.lz(-1, fl), u.fr, missing);
            }
        }
        Direction in = u.fr.getOpposite();
        Direction al = u.al;
        int wd = u.width;
        switch (u.kind) {
            case CLOTHES -> {
                for (int d : new int[]{3, 6, 9}) {
                    for (int w = 2; w <= wd - 1; w++) {
                        if (w < fl - 1 || w > fl + 1) {
                            u.decor(d, w, 0, MallBlocks.CLOTHES_RACK, u.fr);
                        }
                    }
                }
                u.mannequin(1, 2, u.fr);
                u.mannequin(1, wd - 1, u.fr);
                if (p.rng.nextDouble() < 0.4) {
                    u.mannequin(7, fl, in);                    // one stands in the aisle, facing away
                }
                counter(u, 11);
                u.decor(5, 1, 1, MallBlocks.MIRROR, al);
            }
            case SHOES -> {
                for (int d = 2; d <= 11; d++) {
                    u.decor(d, 1, 0, MallBlocks.SHOE_SHELF, al);
                    u.decor(d, wd, 0, MallBlocks.SHOE_SHELF, al.getOpposite());
                    u.decor(d, 1, 1, MallBlocks.SHOE_SHELF, al);
                    u.decor(d, wd, 1, MallBlocks.SHOE_SHELF, al.getOpposite());
                }
                u.decor(5, fl, 0, MallBlocks.BENCH, u.fr);
                u.decor(8, fl, 0, MallBlocks.BENCH, in);
                counter(u, 11);
            }
            case ELECTRONICS -> {
                for (int d : new int[]{4, 8}) {
                    for (int w = 2; w <= wd - 1; w++) {
                        if (w < fl - 1 || w > fl + 1) {
                            u.decor(d, w, 0, MallBlocks.COUNTER, u.fr);
                            u.rf(d, w, 1, "television", u.fr, MallBlocks.f(p.rng.nextDouble() < 0.6 ? MallBlocks.TV : MallBlocks.TV_OFF, u.fr));
                        }
                    }
                }
                for (int w = 1; w <= wd; w++) {                  // a wall of televisions, most of them showing static
                    for (int o = 0; o <= 2; o++) {
                        if (w != wd - 1 || o > 0) {
                            u.decor(12, w, o, p.rng.nextDouble() < 0.7 ? MallBlocks.TV : MallBlocks.TV_OFF, u.fr);
                        }
                    }
                }
                u.decor(10, 2, 0, MallBlocks.COUNTER, u.fr);
                u.decor(10, 2, 1, MallBlocks.REGISTER, u.fr);
            }
            case BOOKS -> {
                for (int d = 2; d <= 12; d++) {
                    for (int o = 0; o <= 2; o++) {
                        BlockState shelf = p.rng.nextDouble() < 0.35
                                ? Blocks.CHISELED_BOOKSHELF.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, al)
                                : Blocks.BOOKSHELF.defaultBlockState();
                        u.put(d, 1, o, shelf);
                        u.put(d, wd, o, shelf.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                                ? shelf.setValue(BlockStateProperties.HORIZONTAL_FACING, al.getOpposite()) : shelf);
                    }
                }
                u.rf(6, fl, 0, "oak_table", u.fr, MallBlocks.f(MallBlocks.COUNTER, u.fr));
                u.rf(6, fl - 1, 0, "oak_chair", al, MallBlocks.f(MallBlocks.FOOD_CHAIR, al));
                if (p.rng.nextDouble() < 0.5) {
                    p.book(u.lx(9, fl), u.y, u.lz(9, fl), u.fr, Stories.ALL[p.rng.nextInt(Stories.ALL.length)]);
                }
                counter(u, 11);
            }
            case TOYS -> {
                for (int d = 3; d <= 10; d++) {
                    u.decor(d, 3, 0, MallBlocks.SHELF, al.getOpposite());
                    u.decor(d, wd - 2, 0, MallBlocks.SHELF, al);
                }
                u.decor(2, fl, 0, MallBlocks.KIDDIE_RIDE, u.fr);
                counter(u, 11);
                u.loot(15, 2, "mall_toys");
            }
            case PETS -> {
                for (int w = 1; w <= wd; w++) {
                    if (w != wd - 1) {
                        u.decor(12, w, 0, MallBlocks.PET_TANK, u.fr);
                        u.decor(12, w, 1, MallBlocks.PET_TANK, u.fr);
                    }
                }
                for (int d = 3; d <= 9; d++) {
                    u.put(d, 1, 0, Blocks.IRON_BARS.defaultBlockState());
                    u.put(d, 1, 1, Blocks.IRON_BARS.defaultBlockState());
                    u.put(d, wd, 0, Blocks.IRON_BARS.defaultBlockState());
                }
                counter(u, 10);
            }
            case SALON -> {
                for (int d = 3; d <= 10; d += 2) {
                    u.decor(d, 1, 1, MallBlocks.MIRROR, al);
                    u.decor(d, 2, 0, MallBlocks.SALON_CHAIR, al.getOpposite());
                    u.rf(d, wd, 0, "white_basin", al.getOpposite(), Blocks.CAULDRON.defaultBlockState());
                }
                counter(u, 11);
            }
            case DARK -> {
                if (p.rng.nextDouble() < 0.6) {
                    u.mannequin(8, fl, u.fr);                    // standing in the dark, looking out
                }
                if (p.rng.nextDouble() < 0.5) {
                    u.decor(4, 3, 0, MallBlocks.CLOTHES_RACK, u.fr);
                }
            }
            case SHUTTER -> {
                for (int d = 3; d <= 9; d += 3) {
                    if (p.rng.nextDouble() < 0.5) {
                        u.decor(d, 2 + p.rng.nextInt(wd - 3), 0, MallBlocks.SHELF, u.fr);
                    }
                }
            }
            case LEASE -> u.decor(1, fl, 1, MallBlocks.SIGN_LEASE, u.fr);
            case FOOD -> food(u);
            case MANAGEMENT -> management(u);
            case SECURITY -> security(u);
            default -> {
            }
        }
        // stockroom behind the shop
        if (u.kind != FOOD) {
            for (int w = 1; w <= wd - 2; w++) {
                double r = p.rng.nextDouble();
                if (r < 0.45) {
                    u.rf(16, w, 0, "spruce_crate", in, Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));
                } else if (r < 0.55) {
                    u.decor(16, w, 0, MallBlocks.CART, al);
                }
            }
            if (p.rng.nextDouble() < 0.4) {
                u.loot(14, 1, u.kind == ELECTRONICS ? "mall_electronics" : "mall_stock");
            }
        }
    }

    /** Sales counter with a till, near the back of the shop floor. */
    static void counter(U u, int d) {
        for (int w = 1; w <= 3; w++) {
            u.decor(d, w, 0, MallBlocks.COUNTER, u.fr);
        }
        u.decor(d, 2, 1, MallBlocks.REGISTER, u.fr);
        if (u.p.rng.nextDouble() < 0.35) {
            u.loot(d + 1, 2, "mall_shop");
        }
    }

    static void food(U u) {
        P p = u.p;
        int wd = u.width;
        for (int w = 1; w <= wd - 1; w++) {
            u.decor(2, w, 0, MallBlocks.FOOD_COUNTER, u.fr);
        }
        u.decor(2, 3, 1, MallBlocks.REGISTER, u.fr);
        Direction in = u.fr.getOpposite();
        u.decor(7, 2, 0, MallBlocks.FRYER, u.fr);
        u.decor(7, 3, 0, MallBlocks.FRYER, u.fr);
        u.rf(7, 5, 0, "light_fridge", u.fr, MallBlocks.f(MallBlocks.COUNTER, u.fr));
        u.rf(7, 6, 0, "white_kitchen_cabinetry", u.fr, MallBlocks.f(MallBlocks.COUNTER, u.fr));
        u.rf(7, 7, 0, "white_kitchen_sink", u.fr, Blocks.CAULDRON.defaultBlockState());
        u.rf(8, 7, 0, "light_microwave", in, null);
        for (int w = 2; w <= wd - 2; w += 3) {
            u.decor(16, w, 2, MallBlocks.MENU, u.fr);
        }
        if (p.rng.nextDouble() < 0.5) {
            u.loot(15, 2, "mall_food");
        }
        if (p.rng.nextDouble() < 0.3) {
            u.rf(15, wd - 2, 0, "light_freezer", in, Blocks.BARREL.defaultBlockState());
        }
    }

    static final Stories.Book MEMO = new Stories.Book("mall_memo", "To All Tenants", "Mall Management", new String[]{
            "TO ALL TENANTS\n\nThe mall will remain open during renovation.\n\nOpening hours are unchanged: we are always open.",
            "PARKING\n\nThe parking garage is closed. There is no parking garage.\n\nPlease use the main entrance.",
            "THE MAIN ENTRANCE\n\nDo not open the main entrance doors. It is night outside.\n\nIt has been night outside for some time.",
            "MAINTENANCE\n\nTenants report something moving above the ceiling tiles.\n\nMaintenance has been informed. Maintenance has not come back.",
            "LOST AND FOUND\n\nA child was found near the fountain. If your child is missing, please come to the mall office.\n\nPlease come soon.",
            "INVENTORY\n\nThe department stores are closed for inventory. Staff use the service corridors.\n\nThe red pin from the directory was put away upstairs, in a top floor stockroom. Please do not ask for it."});

    static void management(U u) {
        P p = u.p;
        for (int d : new int[]{4, 8}) {
            for (int w = 2; w <= 4; w++) {
                u.rf(d, w, 0, "oak_desk", u.fr, MallBlocks.f(MallBlocks.COUNTER, u.fr));
            }
            u.oc(d + 1, 3, 0, "chair_black", u.fr, Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, u.fr.getOpposite()));
            u.oc(d, u.width - 1, 0, "printer", u.fr, MallBlocks.f(MallBlocks.REGISTER, u.fr));
        }
        u.oc(11, u.width - 3, 0, "water_cooler", u.fr, null);
        u.oc(11, u.width - 5, 0, "coffee_machine", u.fr, null);
        for (int d = 2; d <= 10; d += 2) {
            u.rf(d, 1, 0, "oak_drawer", u.al, MallBlocks.f(MallBlocks.SHELF, u.al));
        }
        p.book(u.lx(6, 7), u.y, u.lz(6, 7), u.fr, MEMO);
        u.loot(15, 2, "mall_office");
    }

    static void security(U u) {
        for (int w = 1; w <= u.width; w++) {
            if (w != u.width - 1) {
                u.decor(12, w, 0, MallBlocks.COUNTER, u.fr.getOpposite());
                u.decor(12, w, 1, MallBlocks.TV, u.fr.getOpposite());
                u.decor(12, w, 2, w % 2 == 0 ? MallBlocks.TV : MallBlocks.TV_OFF, u.fr.getOpposite());
            }
        }
        u.oc(10, 4, 0, "chair_gray", u.fr.getOpposite(), Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, u.fr));
        u.oc(10, 8, 0, "chair_gray", u.fr.getOpposite(), null);
        u.loot(15, 2, "mall_security");
        u.oc(3, 2, 0, "water_cooler", u.al, null);
    }

    // --- department stores (closed, reached only through the service corridors) -------------------------------------

    static void department(P p, int f, int q) {
        int y = p.s.yBase + f * FH + 1;
        for (int a = 24; a <= 57; a += 3) {
            for (int b = 24; b <= 57; b += 4) {
                if ((a - 22) % 10 == 5 || (b - 22) % 10 == 5 || a >= 36 && a <= 44 && b >= 36 && b <= 44) {
                    continue;
                }
                double r = p.rng.nextDouble();
                if (r < 0.55) {
                    p.decor(ux(q, a), y, uz(q, b), MallBlocks.CLOTHES_RACK, (q & 2) == 0 ? N : S);
                } else if (r < 0.62) {
                    p.mannequin(ux(q, a), y, uz(q, b), DIRS[p.rng.nextInt(4)], p.rng.nextInt(0xFFFFFF));
                }
            }
        }
        // cosmetics counters in the middle, a lounge in a corner
        for (int k = 37; k <= 43; k++) {
            for (int[] c : new int[][]{{k, 37}, {k, 43}, {37, k}, {43, k}}) {
                p.decor(ux(q, c[0]), y, uz(q, c[1]), MallBlocks.COUNTER, N);
            }
        }
        p.mannequin(ux(q, 40), y, uz(q, 40), DIRS[p.rng.nextInt(4)], -1);
        p.rf(ux(q, 56), y, uz(q, 25), "red_sofa", (q & 2) == 0 ? N : S, MallBlocks.f(MallBlocks.BENCH, (q & 2) == 0 ? N : S));
        p.rf(ux(q, 57), y, uz(q, 25), "red_sofa", (q & 2) == 0 ? N : S, MallBlocks.f(MallBlocks.BENCH, (q & 2) == 0 ? N : S));
        int pinQ = (int) (p.s.h(0xA1E0, 0, 0) * 4);
        for (int k = 0; k < 2; k++) {
            boolean pin = f == FLOORS - 1 && q == pinQ && k == 1;     // 0.43.0: the You Are Here pin (Underside Key)
            p.loot(ux(q, 30 + k * 20), y, uz(q, 58), (q & 2) == 0 ? S : N, pin ? "mall_dept_artifact" : "mall_dept");
        }
        p.text("CLOSED", ux(q, 22), y + 4, uz(q, 30), (q & 1) == 0 ? Wd : E, 0.0);
    }

    static final Direction[] DIRS = {N, E, S, Wd};

    static void service(P p, int f, int q) {
        int y = p.s.yBase + f * FH + 1;
        // the passage door from the concourse
        Direction out = (q & 1) == 0 ? E : Wd;
        p.decor(ux(q, -1), y + 3, uz(q, 19), MallBlocks.EMPLOYEES, out);
        for (int a = 4; a <= 58; a += 5) {
            double r = p.rng.nextDouble();
            int b = 18 + p.rng.nextInt(2) * 2;
            if (r < 0.2) {
                p.rf(ux(q, a), y, uz(q, b), "spruce_crate", N, Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));
            } else if (r < 0.28) {
                p.decor(ux(q, a), y, uz(q, b), MallBlocks.CART, (q & 1) == 0 ? Wd : E);
            } else if (r < 0.32) {
                p.put(ux(q, a), y, uz(q, b), Blocks.CAULDRON.defaultBlockState());
            }
        }
        for (int b = 24; b <= 58; b += 6) {
            if (p.rng.nextDouble() < 0.25) {
                int a = 18 + p.rng.nextInt(2) * 2;
                p.rf(ux(q, a), y, uz(q, b), "spruce_crate", N, Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));
            }
        }
    }

    // --- concourse -----------------------------------------------------------------------------------------------------

    static boolean freeFloor(int lx, int lz, int f) {
        if (!concourse(lx, lz) || pillar(lx, lz) || planter(lx, lz) || escalator(lx, f * FH + 1, lz) != null || escalator(lx, f * FH + 2, lz) != null) {
            return false;
        }
        if (f > 0 && (isVoid(lx, lz) || railCell(lx, lz))) {
            return false;
        }
        int dx = lx - 80;
        int dz = lz - 80;
        return dx * dx + dz * dz > 64 && lx > 3 && lz > 3 && lx < 157 && lz < 156;
    }

    static void concourseDecor(P p, int f) {
        int y = p.s.yBase + f * FH + 1;
        int ceil = p.s.yBase + f * FH + FH - 2;
        // ceiling speakers under the galleries
        for (int k = 6; k < 157; k += 12) {
            for (int side : new int[]{67, 93}) {
                if (!isVoid(k, side) && !pillar(k, side)) {
                    p.decor(k, ceil, side, MallBlocks.SPEAKER, N);
                }
                if (!isVoid(side, k) && !pillar(side, k) && (k < C0 || k > C1)) {
                    p.decor(side, ceil, k, MallBlocks.SPEAKER, E);
                }
            }
        }
        if (f == 0) {
            // benches and bins at the planters
            for (int cx : new int[]{20, 50, 110, 140}) {
                for (int dx = -1; dx <= 1; dx++) {
                    p.decor(cx + dx, y, 78, MallBlocks.BENCH, N);
                    p.decor(cx + dx, y, 82, MallBlocks.BENCH, S);
                }
                p.decor(cx - 3, y, 80, CinemaBlocks.TRASH, E);
            }
            for (int cz : new int[]{110, 130}) {
                for (int dz = -1; dz <= 1; dz++) {
                    p.decor(78, y, cz + dz, MallBlocks.BENCH, Wd);
                    p.decor(82, y, cz + dz, MallBlocks.BENCH, E);
                }
            }
            // kiosks in the east-west concourse
            for (int kx : new int[]{40, 120}) {
                p.decor(kx, y, 80, MallBlocks.KIOSK, N);
                p.decor(kx + 1, y, 80, MallBlocks.KIOSK, N);
                p.decor(kx, y, 81, MallBlocks.KIOSK, S);
                p.decor(kx + 1, y, 81, MallBlocks.KIOSK, S);
                p.loot(kx, y, 79, N, "mall_kiosk");
            }
            // "out of order" at the foot of the escalators
            p.decor(30, y, 76, MallBlocks.OUT_OF_ORDER, N);
            p.decor(130, y, 76, MallBlocks.OUT_OF_ORDER, N);
            // directories
            p.decor(58, y, 70, MallBlocks.DIRECTORY, S);
            p.decor(58, y + 1, 70, MallBlocks.DIRECTORY_TOP, S);
            p.decor(102, y, 90, MallBlocks.DIRECTORY, N);
            p.decor(102, y + 1, 90, MallBlocks.DIRECTORY_TOP, N);
            p.decor(86, y, 146, MallBlocks.DIRECTORY, S);
            p.decor(86, y + 1, 146, MallBlocks.DIRECTORY_TOP, S);
            // a cart in the dry fountain
            p.decor(84, y, 81, MallBlocks.CART, Wd);
        } else {
            p.decor(8, y, 66, MallBlocks.DIRECTORY, S);
            p.decor(8, y + 1, 66, MallBlocks.DIRECTORY_TOP, S);
            p.decor(152, y, 94, MallBlocks.DIRECTORY, N);
            p.decor(152, y + 1, 94, MallBlocks.DIRECTORY_TOP, N);
            // benches on the galleries, facing the void
            for (int k = 14; k < 150; k += 18) {
                if (freeFloor(k, 70, f)) {
                    p.decor(k, y, 70, MallBlocks.BENCH, S);
                }
                if (freeFloor(k, 90, f)) {
                    p.decor(k, y, 90, MallBlocks.BENCH, N);
                }
            }
        }
        // wet floor signs, carts, bins
        for (int k = 0; k < 14; k++) {
            int lx = 4 + p.rng.nextInt(152);
            int lz = p.rng.nextDouble() < 0.5 ? C0 + p.rng.nextInt(33) : 4 + p.rng.nextInt(152);
            if (lz >= C0 && lz <= C1 || lx >= C0 && lx <= C1) {
                if (freeFloor(lx, lz, f) && !(f == 0 && lx >= C0 && lx <= C1 && lz < 60)) {
                    RegistryObject<Block> b = k % 3 == 0 ? MallBlocks.CART : k % 3 == 1 ? MallBlocks.WET_FLOOR : CinemaBlocks.TRASH;
                    p.decor(lx, y, lz, b, DIRS[p.rng.nextInt(4)]);
                }
            }
        }
        // somebody is standing at the railing
        for (int k = 0; k < 2; k++) {
            int lx = 12 + p.rng.nextInt(136);
            int lz = p.rng.nextDouble() < 0.5 ? 70 : 90;
            if (f > 0 && freeFloor(lx, lz, f)) {
                p.mannequin(lx, y, lz, lz == 70 ? S : N, -1);
            }
        }
    }

    static void foodCourt(P p) {
        int y = p.s.yBase + 1;
        for (int lx = 68; lx <= 93; lx += 5) {
            for (int lz = 8; lz <= 56; lz += 6) {
                if (!freeFloor(lx, lz, 0) || p.rng.nextDouble() < 0.18) {
                    continue;
                }
                p.rf(lx, y, lz, "oak_table", N, MallBlocks.f(MallBlocks.FOOD_TABLE, N));
                Direction[] ds = {N, S, E, Wd};
                for (Direction d : ds) {
                    if (p.rng.nextDouble() < 0.8) {
                        int cx = lx + d.getStepX();
                        int cz = lz + d.getStepZ();
                        Direction faces = d.getOpposite();
                        if (p.rng.nextDouble() < 0.15) {
                            faces = DIRS[p.rng.nextInt(4)];             // pushed back, never pushed in again
                        }
                        p.rf(cx, y, cz, "oak_chair", faces, MallBlocks.f(MallBlocks.FOOD_CHAIR, faces));
                    }
                }
            }
        }
        for (int lz = 10; lz <= 54; lz += 11) {
            p.decor(66, y, lz, CinemaBlocks.TRASH, E);
            p.decor(94, y, lz, CinemaBlocks.TRASH, Wd);
        }
        p.decor(80, y, 59, MallBlocks.KIDDIE_RIDE, S);
        p.mannequin(78 + p.rng.nextInt(5), y, 30 + p.rng.nextInt(10), S, -1);   // a customer, waiting
        p.text("FOOD COURT", 80, y + 5, 3, S, 0.0);
    }

    /** Grey daylight from the skylights reaches down into the atriums (invisible light blocks, as with OfficeCraft lamps). */
    static void daylight(P p) {
        BlockState[] lv = new BlockState[16];
        for (int k = 0; k < 16; k++) {
            lv[k] = Blocks.LIGHT.defaultBlockState().setValue(net.minecraft.world.level.block.LightBlock.LEVEL, k);
        }
        for (int lx = 4; lx < 157; lx += 5) {
            for (int lz = 4; lz < 157; lz += 5) {
                if (!isVoid(lx, lz) || escalator(lx, 13, lz) != null || escalator(lx, 25, lz) != null) {
                    continue;
                }
                p.put(lx, p.s.yBase + 29, lz, lv[14]);
                p.put(lx, p.s.yBase + 17, lz, lv[14]);
                p.put(lx, p.s.yBase + 5, lz, lv[14]);
            }
        }
        // shop lamps: their light reaches the shelves
        for (int f = 0; f < FLOORS; f++) {
            for (int lx = 3; lx < 157; lx++) {
                for (int lz = 3; lz < 157; lz++) {
                    if (!concourse(lx, lz) && p.s.part(lx, f * FH + 6, lz, false) == Part.LIGHT) {
                        p.put(lx, p.s.yBase + f * FH + 4, lz, lv[12]);
                    }
                }
            }
        }
        // the lamps hang high under the gallery ceilings: their light reaches the floor through a hidden light below each
        for (int f = 0; f < FLOORS; f++) {
            for (int lx = 3; lx < 157; lx += 6) {
                for (int lz = 3; lz < 157; lz += 6) {
                    if (concourse(lx, lz) && !isVoid(lx, lz) && p.s.part(lx, f * FH + FH - 1, lz, false) == Part.LIGHT) {
                        p.put(lx, p.s.yBase + f * FH + 6, lz, lv[14]);
                    }
                }
            }
        }
    }

    static void entrance(P p) {
        int y = p.s.yBase + 1;
        p.text("MERIDIAN GALLERIA", 80, y + 6, 156, N, 0.0);
        p.text("EXIT", 80, y + 4, 156, N, 0.0);
        p.mannequin(80, y, 154, S, -1);                          // looking out into the night
        p.decor(76, y, 152, MallBlocks.WET_FLOOR, N);
    }
}
