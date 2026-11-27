package de.officerooms.special;

import de.officerooms.OfficeRooms;
import de.officerooms.block.CeilingLightBlock;
import de.officerooms.core.Hashing;
import de.officerooms.core.Hashing.Rng;
import de.officerooms.core.OfficeRoomsIds;
import de.officerooms.entity.HeadlessWarden;
import de.officerooms.entity.ModEntities;
import de.officerooms.furnish.FurnishingPlanner;
import de.officerooms.furnish.Placement;
import de.officerooms.furnish.RoomFurnisher;
import de.officerooms.room.OfficePlanner;
import de.officerooms.room.PlanCache;
import de.officerooms.room.RegionLayout;
import de.officerooms.room.Room;
import de.officerooms.room.connection.Openings;
import de.officerooms.room.structure.BlockKind;
import de.officerooms.room.structure.BlockSampler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Baut die Spezialräume (Phase 4) bei der Weltgenerierung: Wasser, Pflanzen, Sculk, Dorf.
 *
 * <p>Pro Raum wird einmal ein deterministischer Bauplan berechnet (Blöcke und Wesen in
 * Weltkoordinaten) und gecacht. Jeder Chunk setzt daraus nur die Blöcke und Wesen, die in ihm liegen.
 * Damit entsteht alles genau einmal, auch über Chunkgrenzen hinweg, und die Wesen (Dorfbewohner,
 * Eisengolem, der Headless Warden des Sculk-Raums) erscheinen genau einmal pro Raum.</p>
 *
 * <p>Wasser- und Dorfräume werden abgedichtet: Alle Durchgänge bis auf einen werden mit Quarz
 * geschlossen, in den verbleibenden kommt eine Eisentür (Dorf: Knöpfe innen und außen; Wasser: Hebel
 * außen). Wasser kann eine Tür nicht durchfließen, Dorfbewohner können Eisentüren nicht öffnen.</p>
 */
public final class SpecialDecorator {

    private static final int[] DX = {1, 0, -1, 0};
    private static final int[] DZ = {0, 1, 0, -1};
    private static final Direction[] DIR = {Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH};

    /** Blockänderung. {@code onlyFree}: nur setzen, wenn dort noch Luft oder Teppich ist. */
    public record Op(int x, int y, int z, BlockState state, boolean onlyFree, ResourceLocation loot, Object extra) {
        public Op(int x, int y, int z, BlockState state, boolean onlyFree, ResourceLocation loot) {
            this(x, y, z, state, onlyFree, loot, null);
        }
    }

    /** OfficeCraft-Möbel (eine Spalte, höchstens 2 hoch); fällt sonst auf {@link Op#state()} zurück. */
    public record OcPlace(String id, Direction facing) {
    }

    public enum Kind { VILLAGER, IRON_GOLEM, HEADLESS_WARDEN, FROG, CAT }

    public record Spawn(Kind kind, double x, double y, double z, float yaw, VillagerProfession profession) {
    }

    public record Plan(SpecialType type, List<Op> ops, List<Spawn> spawns) {
    }

    private final OfficePlanner p;
    private final BlockSampler sampler;
    private final FurnishingPlanner fp;
    private final PlanCache<Plan> cache = new PlanCache<>(4096);

    public SpecialDecorator(OfficePlanner p, BlockSampler sampler, FurnishingPlanner fp) {
        this.p = p;
        this.sampler = sampler;
        this.fp = fp;
    }

    // -------------------------------------------------------------------------------------------
    // Chunk
    // -------------------------------------------------------------------------------------------

    public void decorate(WorldGenLevel level, ChunkAccess chunk) {
        int cx = chunk.getPos().x;
        int cz = chunk.getPos().z;
        int x0 = (cx << 4) - p.unit;
        int z0 = (cz << 4) - p.unit;
        int x1 = (cx << 4) + 15 + p.unit;
        int z1 = (cz << 4) + 15 + p.unit;
        // Performance: Spezialräume je Region einmal sammeln (mit Rechteck), dann nur Überschneidung prüfen
        int rxa = p.regionOf(x0);
        int rxb = p.regionOf(x1);
        int rza = p.regionOf(z0);
        int rzb = p.regionOf(z1);
        for (int rx = rxa; rx <= rxb; rx++) {
            for (int rz = rza; rz <= rzb; rz++) {
                for (int[] e : specialsOf(rx, rz)) {
                    // e = {Raumindex, minX, minZ, maxX, maxZ} in Weltkoordinaten (inkl. Wände)
                    if (e[3] < x0 || e[1] > x1 || e[4] < z0 || e[2] > z1) {
                        continue;
                    }
                    Room r = p.layout(rx, rz).rooms.get(e[0]);
                    Plan plan = plan(rx, rz, r);
                    apply(level, plan, cx, cz);
                }
            }
        }
    }

    private final PlanCache<List<int[]>> regionSpecials = new PlanCache<>(256);

    private List<int[]> specialsOf(int rx, int rz) {
        long key = ((long) rx << 32) ^ (rz & 0xFFFFFFFFL);
        return regionSpecials.get(key, () -> {
            RegionLayout l = p.layout(rx, rz);
            List<int[]> out = new ArrayList<>();
            for (Room r : l.rooms) {
                SpecialType st = SpecialRooms.of(p, rx, rz, r);
                if (st == SpecialType.NONE || st.isMegastructure()) {
                    continue;
                }
                int[] b = l.bounds(r, r.bottom);
                int bx = rx * p.regionBlocks;
                int bz = rz * p.regionBlocks;
                out.add(new int[]{r.index, bx + b[0] * p.unit, bz + b[1] * p.unit, bx + (b[2] + 1) * p.unit, bz + (b[3] + 1) * p.unit});
            }
            return out;
        });
    }

    public Plan plan(int rx, int rz, Room r) {
        long key = (((long) rx & 0xFFFFF) << 44) | (((long) rz & 0xFFFFF) << 24) | (r.index & 0xFFFFFF);
        return cache.get(key, () -> new Builder(rx, rz, r).build());
    }

    private void apply(WorldGenLevel level, Plan plan, int cx, int cz) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (Op op : plan.ops()) {
            if ((op.x() >> 4) != cx || (op.z() >> 4) != cz) {
                continue;
            }
            m.set(op.x(), op.y(), op.z());
            BlockState cur = level.getBlockState(m);
            BlockState st = op.state();
            if (st.is(Blocks.WATER) && cur.getBlock() instanceof CeilingLightBlock) {
                level.setBlock(m, cur.setValue(CeilingLightBlock.WATERLOGGED, true), 2);
                continue;
            }
            if (op.onlyFree() && !(cur.isAir() || cur.is(Blocks.GRAY_CARPET))) {
                continue;
            }
            if (op.extra() instanceof OcPlace oc && placeOfficeCraft(level, m, oc)) {
                continue;
            }
            level.setBlock(m, st, 2);
            try {
                applyExtras(level, m, op);
            } catch (RuntimeException ex) {
                // ein einzelner Block-Entity-Fehler darf den Rest des Chunks nicht abbrechen
                OfficeRooms.LOGGER.warn("Special room block {} {} could not be filled: {}", m, st, ex.toString());
            }
        }
        applySpawns(level, plan, cx, cz);
    }

    /** OfficeCraft-Möbel setzen, wenn der Mod da ist und das Möbel nur diese Spalte belegt. */
    private static boolean placeOfficeCraft(WorldGenLevel level, BlockPos pos, OcPlace oc) {
        if (!de.officerooms.compat.OfficeCraftCompat.isAvailable()) {
            return false;
        }
        List<BlockPos> cells = de.officerooms.compat.OfficeCraftCompat.cells(oc.id(), oc.facing());
        if (cells.isEmpty()) {
            return false;
        }
        for (BlockPos c : cells) {
            if (c.getX() != 0 || c.getZ() != 0 || c.getY() < 0 || c.getY() > 1) {
                return false;
            }
            BlockState here = level.getBlockState(pos.offset(c));
            if (!(here.isAir() || here.is(Blocks.GRAY_CARPET) || here.canBeReplaced())) {
                return false;
            }
        }
        return de.officerooms.compat.OfficeCraftCompat.place(level, pos.immutable(), oc.id(), oc.facing(), false,
                de.officerooms.compat.FurnitureBuilder.FLAGS);
    }

    private void applyExtras(WorldGenLevel level, BlockPos.MutableBlockPos m, Op op) {
        {
            if (op.loot() != null) {
                BlockEntity be = level.getBlockEntity(m);
                if (be instanceof RandomizableContainerBlockEntity rc) {
                    rc.setLootTable(op.loot(), Hashing.hash(p.seed, op.x(), op.y(), op.z()));
                }
            }
            if (op.extra() instanceof net.minecraft.world.item.ItemStack food) {
                BlockEntity be = level.getBlockEntity(m);
                if (be instanceof net.minecraft.world.level.block.entity.CampfireBlockEntity fire) {
                    // Während der Generierung hat der Block-Entity noch keine Welt: placeFood() würde abstürzen,
                    // daher direkt über NBT befüllen
                    net.minecraft.nbt.CompoundTag tag = fire.saveWithoutMetadata();
                    net.minecraft.nbt.ListTag items = new net.minecraft.nbt.ListTag();
                    net.minecraft.nbt.CompoundTag slot = food.copy().save(new net.minecraft.nbt.CompoundTag());
                    slot.putByte("Slot", (byte) 0);
                    items.add(slot);
                    tag.put("Items", items);
                    tag.putIntArray("CookingTimes", new int[]{0, 0, 0, 0});
                    tag.putIntArray("CookingTotalTimes", new int[]{600, 0, 0, 0});
                    fire.load(tag);
                }
            }
            if (op.extra() instanceof Stories.Book book) {
                BlockEntity be = level.getBlockEntity(m);
                if (be instanceof net.minecraft.world.level.block.entity.LecternBlockEntity lectern) {
                    lectern.setBook(bookStack(book));
                }
            } else if (op.extra() instanceof String text) {
                BlockEntity be = level.getBlockEntity(m);
                if (be instanceof net.minecraft.world.level.block.entity.SignBlockEntity sign) {
                    net.minecraft.world.level.block.entity.SignText t = new net.minecraft.world.level.block.entity.SignText()
                            .setColor(net.minecraft.world.item.DyeColor.LIME).setHasGlowingText(true)
                            .setMessage(1, net.minecraft.network.chat.Component.literal(text));
                    // ohne Welt (Generierung) nicht über setText(), sondern über NBT
                    net.minecraft.nbt.CompoundTag tag = sign.saveWithoutMetadata();
                    net.minecraft.world.level.block.entity.SignText.DIRECT_CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, t)
                            .result().ifPresent(n -> {
                                tag.put("front_text", n);
                                tag.put("back_text", n.copy());
                            });
                    sign.load(tag);
                }
            }
        }
    }

    private void applySpawns(WorldGenLevel level, Plan plan, int cx, int cz) {
        for (Spawn sp : plan.spawns()) {
            if (((int) Math.floor(sp.x()) >> 4) != cx || ((int) Math.floor(sp.z()) >> 4) != cz) {
                continue;
            }
            try {
                spawn(level, sp);
            } catch (RuntimeException e) {
                OfficeRooms.LOGGER.error("Special room: creature {} could not be created", sp.kind(), e);
            }
        }
    }

    /** Beschriebenes Buch (englischer Überlebendenbericht) als Item. */
    public static net.minecraft.world.item.ItemStack bookStack(Stories.Book book) {
        net.minecraft.world.item.ItemStack st = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WRITTEN_BOOK);
        net.minecraft.nbt.CompoundTag tag = st.getOrCreateTag();
        tag.putString("title", book.title());
        tag.putString("author", book.author());
        tag.putInt("generation", 2);
        tag.putBoolean("resolved", true);
        net.minecraft.nbt.ListTag pages = new net.minecraft.nbt.ListTag();
        for (String page : book.pages()) {
            pages.add(net.minecraft.nbt.StringTag.valueOf(net.minecraft.network.chat.Component.Serializer.toJson(
                    net.minecraft.network.chat.Component.literal(page))));
        }
        tag.put("pages", pages);
        return st;
    }

    private static void spawn(WorldGenLevel level, Spawn sp) {
        BlockPos pos = BlockPos.containing(sp.x(), sp.y(), sp.z());
        switch (sp.kind()) {
            case VILLAGER -> {
                Villager v = EntityType.VILLAGER.create(level.getLevel());
                if (v == null) {
                    return;
                }
                v.moveTo(sp.x(), sp.y(), sp.z(), sp.yaw(), 0F);
                v.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null, null);
                v.setVillagerData(new VillagerData(VillagerType.PLAINS, sp.profession(), 1));
                // Erfahrung > 0: der Beruf bleibt dauerhaft, auch wenn die Arbeitsstelle kurz nicht gefunden wird.
                v.setVillagerXp(1);
                v.setPersistenceRequired();
                v.addTag(OfficeRoomsIds.MOD_ID + ".outpost");
                level.addFreshEntityWithPassengers(v);
            }
            case IRON_GOLEM -> {
                IronGolem g = EntityType.IRON_GOLEM.create(level.getLevel());
                if (g == null) {
                    return;
                }
                g.moveTo(sp.x(), sp.y(), sp.z(), sp.yaw(), 0F);
                g.setPersistenceRequired();
                g.addTag(OfficeRoomsIds.MOD_ID + ".outpost");
                level.addFreshEntityWithPassengers(g);
            }
            case FROG, CAT -> {
                net.minecraft.world.entity.Mob m = (sp.kind() == Kind.FROG ? EntityType.FROG : EntityType.CAT).create(level.getLevel());
                if (m == null) {
                    return;
                }
                m.moveTo(sp.x(), sp.y(), sp.z(), sp.yaw(), 0F);
                m.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null, null);
                m.setPersistenceRequired();
                m.addTag(OfficeRoomsIds.MOD_ID + ".special");
                level.addFreshEntityWithPassengers(m);
            }
            case HEADLESS_WARDEN -> {
                HeadlessWarden w = de.officerooms.core.OfficeRoomsConfig.mobsAllowed(level.getLevel())
                        ? ModEntities.HEADLESS_WARDEN.get().create(level.getLevel()) : null;   // 0.41.2
                if (w == null) {
                    return;
                }
                w.moveTo(sp.x(), sp.y(), sp.z(), sp.yaw(), 0F);
                w.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null, null);
                w.setPersistenceRequired();
                w.addTag(HeadlessWarden.SCULK_TAG);
                level.addFreshEntityWithPassengers(w);
            }
        }
    }

    // -------------------------------------------------------------------------------------------
    // Bauplan eines Raums
    // -------------------------------------------------------------------------------------------

    private final class Builder {
        final int rx;
        final int rz;
        final Room r;
        final SpecialType type;
        final RoomFurnisher.Geometry g;
        final Rng rng;
        final List<Op> ops = new ArrayList<>();
        final List<Spawn> spawns = new ArrayList<>();
        /** Felder, die schon belegt sind (geplante Möbel, eigene Objekte). */
        final boolean[] blocked;
        /** Lichtpositionen (x, z) unter der Decke. */
        final Set<Long> lights = new HashSet<>();
        final int floorY;
        final int topY;
        final List<double[]> blobs = new ArrayList<>();
        /** Wandlinien-Positionen der eigenen (verschlossenen) Öffnungen. */
        final Set<Long> ownLine = new HashSet<>();

        Builder(int rx, int rz, Room r) {
            this.rx = rx;
            this.rz = rz;
            this.r = r;
            this.type = SpecialRooms.of(p, rx, rz, r);
            this.g = RoomFurnisher.geometry(p, rx, rz, r);
            this.rng = new Rng(Hashing.hash(p.seed, rx, rz, 0x5ED, r.index));
            this.blocked = new boolean[g.gw() * g.gd()];
            this.floorY = g.floorY();
            this.topY = g.topY();
            for (Placement pl : fp.room(rx, rz, r).placements()) {
                if (pl.item().isLight()) {
                    lights.add(xz(pl.x(), pl.z()));
                    for (int[] c : RoomFurnisher.footprint(pl.item(), pl.facing())) {
                        lights.add(xz(pl.x() + c[0], pl.z() + c[1]));
                    }
                    continue;
                }
                for (int[] c : RoomFurnisher.footprint(pl.item(), pl.facing())) {
                    int bx = g.bx(pl.x() + c[0]);
                    int bz = g.bz(pl.z() + c[1]);
                    if (g.in(bx, bz)) {
                        blocked[bz * g.gw() + bx] = true;
                    }
                }
            }
        }

        Plan build() {
            switch (type) {
                case WATER_OFFICE -> water();
                case SCULK_OFFICE -> sculk();
                case LUSH_CAVE -> lush();
                case VILLAGER_OUTPOST -> outpost();
                case EXIT_ROOM -> exitRoom();
                case ABANDONED_CAMP -> camp();
                case OASIS -> oasis();
                case COLD_STORAGE -> coldStorage();
                case LAST_MEETING -> lastMeeting();
                case MAIL_ROOM -> mailRoom();
                case RAIN_ROOM -> rainRoom();
                case CANTEEN -> canteen();
                case ARCADE -> arcade();
                case DEPARTURE_GATE -> gate();
                default -> {
                }
            }
            return new Plan(type, List.copyOf(ops), List.copyOf(spawns));
        }

        // --- 0.29.0 curiosities (new worlds) -------------------------------------------------------

        BlockState piece(int idx, Direction front) {
            return de.officerooms.block.RoomDecorBlocks.decor(idx, front);
        }

        Direction into(int side) {
            return DIR[(side + 2) % 4];
        }

        /** Places up to n wall objects (two blocks tall: lower, upper) facing into the room. Returns the wall tiles used. */
        List<int[]> wallPairs(int n, java.util.function.Function<Direction, BlockState> lower, java.util.function.Function<Direction, BlockState> upper) {
            List<int[]> used = new ArrayList<>();
            for (int[] w : shuffledWalls()) {
                if (used.size() >= n) {
                    break;
                }
                if (wallSideCount(w[0], w[1]) > 1 || !claim(w)) {
                    continue;
                }
                Direction f = into(w[2]);
                setTile(w[0], floorY, w[1], lower.apply(f));
                if (upper != null) {
                    setTile(w[0], floorY + 1, w[1], upper.apply(f));
                }
                used.add(w);
            }
            return used;
        }

        /** Cold storage: frozen floor, snow, ice stacked along the walls, freezer barrels with food. */
        void coldStorage() {
            for (int[] t : tiles()) {
                int x = g.wx(t[0]);
                int z = g.wz(t[1]);
                double v = rng.nextDouble();
                if (v < 0.45) {
                    set(x, floorY - 1, z, Blocks.PACKED_ICE.defaultBlockState());
                } else if (v < 0.65) {
                    set(x, floorY - 1, z, Blocks.SNOW_BLOCK.defaultBlockState());
                }
                if (free(t[0], t[1]) && rng.nextDouble() < 0.22) {
                    set(x, floorY, z, Blocks.SNOW.defaultBlockState());
                }
            }
            int freezers = 2 + rng.nextInt(2);
            int built = 0;
            for (int[] w : shuffledWalls()) {
                if (built >= freezers + 4) {
                    break;
                }
                if (!claim(w)) {
                    continue;
                }
                int x = g.wx(w[0]);
                int z = g.wz(w[1]);
                if (built < freezers) {
                    loot(x, floorY, z, Blocks.BARREL.defaultBlockState().setValue(net.minecraft.world.level.block.BarrelBlock.FACING, Direction.UP), "cold_storage");
                    set(x, floorY + 1, z, Blocks.PACKED_ICE.defaultBlockState());
                } else {
                    set(x, floorY, z, Blocks.BLUE_ICE.defaultBlockState());
                    set(x, floorY + 1, z, Blocks.PACKED_ICE.defaultBlockState());
                    if (rng.chance(0.5) && floorY + 2 < topY) {
                        set(x, floorY + 2, z, Blocks.ICE.defaultBlockState());
                    }
                }
                built++;
            }
        }

        /**
         * The last meeting: conference table, chairs and board come from the ordinary meeting room furniture
         * (RoomFurnisher). Here: projector and screen still on, the minutes on a lectern, a chest.
         */
        void lastMeeting() {
            boolean alongX = g.gw() >= g.gd();
            int mid = (alongX ? g.gd() : g.gw()) / 2;
            int[] end = alongX ? new int[]{g.gw() - 3, mid} : new int[]{mid, g.gd() - 3};
            // projector on the ceiling, screen on the short wall beyond the table end
            {
                setTile(end[0], topY, end[1], piece(de.officerooms.furnish.Decor.PROJECTOR, alongX ? Direction.EAST : Direction.SOUTH));
                int sx = alongX ? g.gw() - 1 : mid;
                int sz = alongX ? mid : g.gd() - 1;
                if (g.in(sx, sz) && wallSide(sx, sz) >= 0 && placeable(sx, sz) && floorY + 2 <= topY) {
                    Direction f = alongX ? Direction.WEST : Direction.NORTH;
                    setTile(sx, floorY + 1, sz, piece(de.officerooms.furnish.Decor.SCREEN, f));
                    setTile(sx, floorY + 2, sz, piece(de.officerooms.furnish.Decor.SCREEN_TOP, f));
                }
            }
            // the minutes on a lectern and the chest of whoever left last
            boolean lectern = false;
            boolean chest = false;
            for (int[] w : shuffledWalls()) {
                if (lectern && chest) {
                    break;
                }
                if (!claim(w)) {
                    continue;
                }
                if (!lectern) {
                    ops.add(new Op(g.wx(w[0]), floorY, g.wz(w[1]),
                            facing(Blocks.LECTERN, into(w[2])).setValue(net.minecraft.world.level.block.LecternBlock.HAS_BOOK, true), false, null, MINUTES));
                    lectern = true;
                } else {
                    loot(g.wx(w[0]), floorY, g.wz(w[1]), facing(Blocks.CHEST, into(w[2])), "last_meeting");
                    chest = true;
                }
            }
        }

        /** Mail room: shelves of parcels along the walls (some can be opened), a sorting table, a phone that rings. */
        void mailRoom() {
            int shelves = Math.min(12, 4 + g.gw() * g.gd() / 20);
            int k = 0;
            for (int[] w : wallPairs(shelves, f -> Blocks.BARREL.defaultBlockState().setValue(net.minecraft.world.level.block.BarrelBlock.FACING, f),
                    f -> de.officerooms.block.CuriosityBlocks.f(de.officerooms.block.CuriosityBlocks.PARCELS, f))) {
                if (rng.chance(0.55)) {
                    loot(g.wx(w[0]), floorY, g.wz(w[1]), Blocks.BARREL.defaultBlockState()
                            .setValue(net.minecraft.world.level.block.BarrelBlock.FACING, into(w[2])), "mail_parcel");
                }
                if (k++ == 1) {
                    setTile(w[0], floorY + 1, w[1], piece(de.officerooms.furnish.Decor.HOTEL_PHONE, into(w[2])));
                }
            }
            // sorting table in the middle with loose parcels
            int cx = g.gw() / 2;
            int cz = g.gd() / 2;
            for (int dx = -1; dx <= 1; dx++) {
                if (claim(new int[]{cx + dx, cz})) {
                    setTile(cx + dx, floorY, cz, Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
                    if (rng.chance(0.6)) {
                        setTile(cx + dx, floorY + 1, cz, de.officerooms.block.CuriosityBlocks.f(de.officerooms.block.CuriosityBlocks.PARCELS, DIR[rng.nextInt(4)]));
                    }
                }
            }
        }

        /** Rain room: it rains from the ceiling; puddles and mud, and one dry desk in the middle. */
        void rainRoom() {
            int cx = g.gw() / 2;
            int cz = g.gd() / 2;
            for (int[] t : tiles()) {
                int x = g.wx(t[0]);
                int z = g.wz(t[1]);
                boolean dry = Math.abs(t[0] - cx) <= 1 && Math.abs(t[1] - cz) <= 1;
                if (!dry && (t[0] + t[1]) % 2 == 0) {
                    setFree(x, topY, z, de.officerooms.block.CuriosityBlocks.RAIN.get().defaultBlockState());
                }
                if (dry) {
                    continue;
                }
                double v = rng.nextDouble();
                if (v < 0.12 && !g.keep(t[0], t[1]) && free(t[0], t[1])) {
                    set(x, floorY - 1, z, Blocks.WATER.defaultBlockState());          // puddle level with the floor
                } else if (v < 0.4) {
                    set(x, floorY - 1, z, Blocks.MUD.defaultBlockState());
                } else if (v < 0.5 && free(t[0], t[1])) {
                    set(x, floorY, z, Blocks.MOSS_CARPET.defaultBlockState());
                }
            }
            if (claim(new int[]{cx, cz})) {
                setTile(cx, floorY, cz, Blocks.DARK_OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
                setTile(cx, floorY + 1, cz, piece(de.officerooms.furnish.Decor.DESK_LAMP, Direction.SOUTH));
            }
            if (claim(new int[]{cx + 1, cz})) {
                loot(g.wx(cx + 1), floorY, g.wz(cz), facing(Blocks.CHEST, Direction.SOUTH), "rain_room");
            }
        }

        /** Canteen: vending machines, a serving counter, tables and chairs. */
        void canteen() {
            wallPairs(2 + rng.nextInt(2), f -> de.officerooms.block.CuriosityBlocks.f(de.officerooms.block.CuriosityBlocks.VENDING, f),
                    f -> de.officerooms.block.CuriosityBlocks.f(de.officerooms.block.CuriosityBlocks.VENDING_TOP, f));
            // counter: a short row along one wall
            int counter = 0;
            for (int[] w : shuffledWalls()) {
                if (counter >= 4) {
                    break;
                }
                if (wallSideCount(w[0], w[1]) > 1 || !claim(w)) {
                    continue;
                }
                setTile(w[0], floorY, w[1], Blocks.SMOOTH_QUARTZ.defaultBlockState());
                if (counter == 0) {
                    setTile(w[0], floorY + 1, w[1], piece(de.officerooms.furnish.Decor.SERVICE_BELL, into(w[2])));
                }
                counter++;
            }
            // tables (Refurbished if present, otherwise simple vanilla tables with chairs)
            int sets = Math.max(1, g.gw() * g.gd() / 40);
            for (int k = 0; k < sets; k++) {
                if (diningSet("oak", false, true, floorY) == null) {
                    for (int t = 0; t < 20; t++) {
                        int bx = 2 + rng.nextInt(Math.max(1, g.gw() - 4));
                        int bz = 2 + rng.nextInt(Math.max(1, g.gd() - 4));
                        if (claim(new int[]{bx, bz})) {
                            setTile(bx, floorY, bz, Blocks.OAK_FENCE.defaultBlockState());
                            setTile(bx, floorY + 1, bz, Blocks.OAK_PRESSURE_PLATE.defaultBlockState());
                            for (int d = 0; d < 4; d += 2) {
                                if (claim(new int[]{bx + DX[d], bz + DZ[d]})) {
                                    setTile(bx + DX[d], floorY, bz + DZ[d], Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, DIR[d]));
                                }
                            }
                            break;
                        }
                    }
                }
            }
            for (int[] w : shuffledWalls()) {
                if (claim(w)) {
                    loot(g.wx(w[0]), floorY, g.wz(w[1]), facing(Blocks.CHEST, into(w[2])), "canteen");
                    break;
                }
            }
        }

        /**
         * 0.42.0: departure gate. Terminal carpet, rows of seats, a desk, a board that says the ship is boarding, a window
         * full of fog and the gate itself (right-click to board).
         */
        void gate() {
            for (int[] t : tiles()) {
                set(g.wx(t[0]), floorY - 1, g.wz(t[1]), de.officerooms.sea.ShipBlocks.TERMINAL_CARPET.get().defaultBlockState());
            }
            wallPairs(1, f -> de.officerooms.sea.ShipBlocks.f(de.officerooms.sea.ShipBlocks.BOARDING_GATE, f),
                    f -> de.officerooms.sea.ShipBlocks.f(de.officerooms.sea.ShipBlocks.BOARDING_GATE_TOP, f));
            // the departure board: three panels side by side on one wall
            boolean board = false;
            for (int[] w : shuffledWalls()) {
                if (board) {
                    break;
                }
                for (int[] step : new int[][]{{1, 0}, {0, 1}}) {
                    int[] w2 = {w[0] + step[0], w[1] + step[1], w[2]}, w3 = {w[0] + 2 * step[0], w[1] + 2 * step[1], w[2]};
                    if (wallSide(w2[0], w2[1]) == w[2] && wallSide(w3[0], w3[1]) == w[2] && claim(w, w2, w3)) {
                        Direction f = into(w[2]);
                        int[][] ws = {w, w2, w3};
                        // panel 0 is on the viewer's left
                        boolean leftFirst = f == Direction.SOUTH || f == Direction.WEST ? step[0] + step[1] > 0 == (f == Direction.WEST) : step[0] + step[1] < 0;
                        for (int k = 0; k < 3; k++) {
                            int part = (f == Direction.NORTH || f == Direction.EAST) ? 2 - k : k;
                            setTile(ws[k][0], floorY + 1, ws[k][1], de.officerooms.sea.ShipBlocks.DEPARTURE_BOARD.get().defaultBlockState()
                                    .setValue(de.officerooms.block.DecorBlock.FACING, f).setValue(de.officerooms.sea.ShipBlocks.Board.PART, part));
                        }
                        setTile(w2[0], floorY, w2[1], de.officerooms.sea.ShipBlocks.f(de.officerooms.sea.ShipBlocks.GATE_DESK, f));
                        board = true;
                        break;
                    }
                }
            }
            // fog in the windows: set into the wall itself, along the longest straight run
            int windows = 0;
            for (int[] w : shuffledWalls()) {
                if (windows >= Math.max(4, g.gw() + g.gd())) {
                    break;
                }
                if (wallSideCount(w[0], w[1]) > 1) {
                    continue;
                }
                Direction out = into(w[2]).getOpposite();
                int x = g.wx(w[0]) + out.getStepX(), z = g.wz(w[1]) + out.getStepZ();
                if ((w[2] % 2) != 0) {
                    continue;                                   // only the walls on one axis: a long window band
                }
                for (int k = 0; k < 2; k++) {
                    set(x, floorY + k, z, de.officerooms.sea.ShipBlocks.TERMINAL_WINDOW.get().defaultBlockState());
                }
                windows++;
            }
            // rows of seats, back to back
            for (int[] t : tiles()) {
                if (free(t[0], t[1]) && t[0] >= 2 && t[0] < g.gw() - 2 && t[1] >= 2 && t[1] < g.gd() - 2 && t[1] % 3 != 0
                        && t[0] != g.gw() / 2 && rng.chance(0.85)) {
                    setTile(t[0], floorY, t[1], de.officerooms.sea.ShipBlocks.f(de.officerooms.sea.ShipBlocks.GATE_SEATS,
                            t[1] % 3 == 1 ? Direction.NORTH : Direction.SOUTH));
                }
            }
            for (int[] w : shuffledWalls()) {
                if (claim(w)) {
                    loot(g.wx(w[0]), floorY, g.wz(w[1]), facing(Blocks.CHEST, into(w[2])), "departure_gate");
                    break;
                }
            }
        }

        /** Arcade: patterned carpet, rows of humming cabinets with lit screens, a prize chest. No music. */
        void arcade() {
            for (int[] t : tiles()) {
                set(g.wx(t[0]), floorY, g.wz(t[1]), de.officerooms.block.CuriosityBlocks.ARCADE_CARPET.get().defaultBlockState());
            }
            wallPairs(Math.min(10, 4 + g.gw() * g.gd() / 25), f -> de.officerooms.block.CuriosityBlocks.f(de.officerooms.block.CuriosityBlocks.ARCADE, f),
                    f -> de.officerooms.block.CuriosityBlocks.f(de.officerooms.block.CuriosityBlocks.ARCADE_TOP, f));
            for (int[] w : shuffledWalls()) {
                if (claim(w)) {
                    loot(g.wx(w[0]), floorY, g.wz(w[1]), facing(Blocks.CHEST, into(w[2])), "arcade_prizes");
                    break;
                }
            }
        }

        static final Stories.Book MINUTES = new Stories.Book("last_meeting", "Minutes", "R. Vance", new String[]{
                "MINUTES\n\nAttendees: 11\nAbsent: 0\n\n1. Floor plan update\nThe architects confirm the building is the same size as last quarter. Nobody could say which quarter.",
                "2. Elevators\nComplaint noted: nobody arrives on the floor they asked for. Facilities say this is within tolerance.\n\n3. Lights\nIt is darker than it was. Agreed not to mention it.",
                "4. AOB\nSomeone keeps knocking on the projector screen from behind.\n\nThe meeting was not adjourned.",
                "(the last page is blank except for the time, written again and again:\n\n4:17\n4:17\n4:17)"});

        // --- Hilfen -------------------------------------------------------------------------

        long xz(int x, int z) {
            return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
        }

        void set(int x, int y, int z, BlockState s) {
            ops.add(new Op(x, y, z, s, false, null));
        }

        void setFree(int x, int y, int z, BlockState s) {
            ops.add(new Op(x, y, z, s, true, null));
        }

        /** Refurbished-Furniture-Block (Nutzerseite zeigt nach front) oder Ersatz, wenn der Mod fehlt. */
        BlockState rf(String id, Direction front, BlockState fallback) {
            BlockState s = de.officerooms.compat.FurnitureBuilder.refurbished(id, front);
            return s != null ? s : fallback;
        }

        boolean hasRf() {
            return de.officerooms.compat.FurnitureBuilder.refurbishedAvailable();
        }

        /** OfficeCraft-Möbel (eine Spalte) mit Vanilla-Ersatz. */
        void office(int x, int y, int z, String id, Direction facing, BlockState fallback) {
            ops.add(new Op(x, y, z, fallback, false, null, new OcPlace(id, facing)));
        }

        void loot(int x, int y, int z, BlockState s, String table) {
            ops.add(new Op(x, y, z, s, false, ResourceLocation.fromNamespaceAndPath(OfficeRoomsIds.MOD_ID, "chests/" + table)));
        }

        String wood(boolean camp) {
            String[] w = camp ? new String[]{"spruce", "dark_oak", "oak"} : new String[]{"oak", "spruce", "birch"};
            return w[rng.nextInt(w.length)];
        }

        static final String[] COLORS = {"red", "blue", "green", "orange", "yellow", "light_blue", "white", "brown", "gray"};

        String color() {
            return COLORS[rng.nextInt(COLORS.length)];
        }

        /**
         * Tisch mit Stühlen (Refurbished Furniture): 1 Tisch, 2–3 Stühle, die zum Tisch schauen. Ohne den Mod
         * wird nichts gebaut. stools = Hocker statt Stühlen. Liefert den Tisch oder null.
         */
        int[] diningSet(String wood, boolean stools, boolean dishes, int y) {
            if (!hasRf()) {
                return null;
            }
            List<int[]> spots = tiles();
            for (int i = spots.size() - 1; i > 0; i--) {
                int j = rng.nextInt(i + 1);
                int[] tmp = spots.get(i);
                spots.set(i, spots.get(j));
                spots.set(j, tmp);
            }
            for (int[] t : spots) {
                if (wallSide(t[0], t[1]) >= 0) {
                    continue;
                }
                int d0 = rng.nextInt(4);
                List<int[]> seats = new ArrayList<>();
                List<Integer> dirs = new ArrayList<>();
                for (int k = 0; k < 4 && seats.size() < 3; k++) {
                    int d = (d0 + k) % 4;
                    int[] c = {t[0] + DX[d], t[1] + DZ[d]};
                    if (placeable(c[0], c[1]) && (seats.size() < 2 || rng.chance(0.5))) {
                        seats.add(c);
                        dirs.add(d);
                    }
                }
                if (seats.size() < 2) {
                    continue;
                }
                List<int[]> all = new ArrayList<>(seats);
                all.add(t);
                if (!claim(all.toArray(new int[0][]))) {
                    continue;
                }
                setTile(t[0], y, t[1], rf(wood + "_table", Direction.NORTH, Blocks.AIR.defaultBlockState()));
                for (int k = 0; k < seats.size(); k++) {
                    Direction toTable = DIR[(dirs.get(k) + 2) % 4];
                    int[] c = seats.get(k);
                    setTile(c[0], y, c[1], stools ? rf(color() + "_stool", toTable, Blocks.AIR.defaultBlockState())
                            : rf(wood + "_chair", toTable, Blocks.AIR.defaultBlockState()));
                }
                if (dishes) {
                    double v = rng.nextDouble();
                    String top = v < 0.5 ? "plate" : v < 0.75 ? wood + "_cutting_board" : "frying_pan";
                    setTile(t[0], y + 1, t[1], rf(top, DIR[rng.nextInt(4)], Blocks.AIR.defaultBlockState()));
                }
                return t;
            }
            return null;
        }

        void setTile(int bx, int y, int bz, BlockState s) {
            set(g.wx(bx), y, g.wz(bz), s);
        }

        boolean free(int bx, int bz) {
            return g.in(bx, bz) && !blocked[bz * g.gw() + bx];
        }

        boolean placeable(int bx, int bz) {
            return free(bx, bz) && !g.keep(bx, bz) && !g.noFurniture(bx, bz);
        }

        void block(int bx, int bz) {
            blocked[bz * g.gw() + bx] = true;
        }

        void unblock(int bx, int bz) {
            blocked[bz * g.gw() + bx] = false;
        }

        /** Alle Freihalteflächen vor Türen sind noch miteinander verbunden (Breitensuche). */
        boolean connected() {
            int n = g.gw() * g.gd();
            boolean[] seen = new boolean[n];
            ArrayDeque<int[]> q = new ArrayDeque<>();
            int keeps = 0;
            for (int bz = 0; bz < g.gd(); bz++) {
                for (int bx = 0; bx < g.gw(); bx++) {
                    if (g.keep(bx, bz) && free(bx, bz)) {
                        keeps++;
                        if (q.isEmpty()) {
                            q.add(new int[]{bx, bz});
                            seen[bz * g.gw() + bx] = true;
                        }
                    }
                }
            }
            int reached = 0;
            while (!q.isEmpty()) {
                int[] c = q.poll();
                if (g.keep(c[0], c[1]) && free(c[0], c[1])) {
                    reached++;
                }
                for (int d = 0; d < 4; d++) {
                    int nx = c[0] + DX[d];
                    int nz = c[1] + DZ[d];
                    if (free(nx, nz) && !seen[nz * g.gw() + nx]) {
                        seen[nz * g.gw() + nx] = true;
                        q.add(new int[]{nx, nz});
                    }
                }
            }
            return reached == keeps;
        }

        /** Belegt Felder, wenn sie frei sind und die Wege zu allen Türen erhalten bleiben. */
        boolean claim(int[]... tiles) {
            for (int[] t : tiles) {
                if (!placeable(t[0], t[1])) {
                    return false;
                }
            }
            for (int[] t : tiles) {
                block(t[0], t[1]);
            }
            if (!connected()) {
                for (int[] t : tiles) {
                    unblock(t[0], t[1]);
                }
                return false;
            }
            return true;
        }

        /** Seite (0..3), auf der das Feld an eine Wand grenzt, oder -1. */
        int wallSide(int bx, int bz) {
            for (int d = 0; d < 4; d++) {
                if (!g.in(bx + DX[d], bz + DZ[d])) {
                    return d;
                }
            }
            return -1;
        }

        List<int[]> tiles() {
            List<int[]> t = new ArrayList<>();
            for (int bz = 0; bz < g.gd(); bz++) {
                for (int bx = 0; bx < g.gw(); bx++) {
                    if (g.in(bx, bz)) {
                        t.add(new int[]{bx, bz});
                    }
                }
            }
            return t;
        }

        /** Organische Verteilung: Summe weicher Flecken um zufällige Keimpunkte, 0..1. */
        void seedBlobs(int count, double radius) {
            for (int i = 0; i < count; i++) {
                blobs.add(new double[]{rng.nextDouble() * g.gw(), rng.nextDouble() * g.gd(), radius * (0.6 + 0.8 * rng.nextDouble())});
            }
        }

        double blob(double bx, double bz) {
            double v = 0;
            for (double[] b : blobs) {
                double dx = bx - b[0];
                double dz = bz - b[1];
                v = Math.max(v, Math.exp(-(dx * dx + dz * dz) / (2 * b[2] * b[2])));
            }
            return v;
        }

        boolean airLike(int x, int y, int z) {
            return BlockKind.isAirLike(sampler.kind(x, y, z));
        }

        // --- Abdichten ------------------------------------------------------------------------

        /**
         * Schließt alle Öffnungen des Raums mit Quarz, bis auf eine Eisentür. Liefert die Tür
         * {x, z, Seite, Linienposition} oder null.
         */
        int[] seal(boolean buttons) {
            int unit = g.unit();
            int lineTop = floorY + p.pitch - 2;
            List<int[]> doors = new ArrayList<>();
            for (int[] o : g.openings()) {
                int t = Openings.type(o[3]);
                if (t == Openings.DOOR || t == Openings.OPEN) {
                    doors.add(o);
                }
            }
            doors.sort((a, b) -> a[2] != b[2] ? Integer.compare(a[2], b[2]) : a[1] != b[1] ? Integer.compare(a[1], b[1]) : Integer.compare(a[0], b[0]));
            ownLine.clear();
            for (int[] o : g.openings()) {
                if (Openings.type(o[3]) != Openings.WINDOW) {
                    for (int pos = 0; pos <= unit; pos++) {
                        int[] w = linePos(o[0], o[1], o[2], pos);
                        ownLine.add(xz(w[0], w[1]));
                    }
                }
            }
            // Tür wählen: Bedienelemente dürfen nicht vor einer anderen Öffnung stehen
            int[] door = null;
            int doorPos = -1;
            int ctrlPos = -1;
            int start = doors.isEmpty() ? 0 : rng.nextInt(doors.size());
            for (int i = 0; i < doors.size() && door == null; i++) {
                int[] cand = doors.get((start + i) % doors.size());
                int dp = Openings.type(cand[3]) == Openings.OPEN ? unit / 2 : Openings.start(cand[3]);
                for (int wp : new int[]{dp + 1, dp - 1}) {
                    if (wp < 1 || wp > unit - 1) {
                        continue;
                    }
                    int[] w = linePos(cand[0], cand[1], cand[2], wp);
                    Direction out = DIR[cand[2]];
                    Direction in = DIR[(cand[2] + 2) % 4];
                    if (controlSpotOk(w[0] + out.getStepX(), floorY + 1, w[1] + out.getStepZ())
                            && (!buttons || controlSpotOk(w[0] + in.getStepX(), floorY + 1, w[1] + in.getStepZ()))) {
                        door = cand;
                        doorPos = dp;
                        ctrlPos = wp;
                        break;
                    }
                }
            }
            // Zweiter Durchgang (0.7.0): dieselbe Prüfung (nie vor einer fremden Öffnung), aber an jeder
            // Wandposition der Türlinie statt nur direkt neben der Tür.
            for (int i = 0; i < doors.size() && door == null; i++) {
                int[] cand = doors.get((start + i) % doors.size());
                int dp = Openings.type(cand[3]) == Openings.OPEN ? unit / 2 : Openings.start(cand[3]);
                Direction out = DIR[cand[2]];
                Direction in = DIR[(cand[2] + 2) % 4];
                for (int k = 2; k < unit && door == null; k++) {
                    for (int wp : new int[]{dp + k, dp - k}) {
                        if (wp < 1 || wp > unit - 1) {
                            continue;
                        }
                        int[] w = linePos(cand[0], cand[1], cand[2], wp);
                        if (controlSpotOk(w[0] + out.getStepX(), floorY + 1, w[1] + out.getStepZ())
                                && (!buttons || controlSpotOk(w[0] + in.getStepX(), floorY + 1, w[1] + in.getStepZ()))) {
                            door = cand;
                            doorPos = dp;
                            ctrlPos = wp;
                            break;
                        }
                    }
                }
            }
            if (door == null && !doors.isEmpty()) {
                door = doors.get(start);
                doorPos = Openings.type(door[3]) == Openings.OPEN ? unit / 2 : Openings.start(door[3]);
                OfficeRooms.LOGGER.debug("Special room {}: no free spot for the door control, using a hand-operated door", r);
            }
            int[] result = null;
            for (int[] o : g.openings()) {
                int t = Openings.type(o[3]);
                if (t == Openings.WINDOW) {
                    continue;
                }
                for (int pos = 0; pos <= unit; pos++) {
                    int[] w = linePos(o[0], o[1], o[2], pos);
                    for (int y = floorY; y <= lineTop; y++) {
                        if (!airLike(w[0], y, w[1])) {
                            continue;
                        }
                        boolean isDoor = o == door && pos == doorPos && y <= floorY + 1;
                        if (!isDoor) {
                            set(w[0], y, w[1], Blocks.QUARTZ_BLOCK.defaultBlockState());
                        }
                    }
                }
            }
            if (door == null) {
                return null;
            }
            int side = door[2];
            int[] d = linePos(door[0], door[1], side, doorPos);
            Direction inward = DIR[(side + 2) % 4];
            Direction outward = DIR[side];
            // Ohne Platz für Hebel/Knopf: Holztür statt Eisentür, damit der Raum nie dauerhaft verschlossen ist
            BlockState lower = (ctrlPos < 0 ? Blocks.SPRUCE_DOOR : Blocks.IRON_DOOR).defaultBlockState().setValue(DoorBlock.FACING, inward)
                    .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER).setValue(DoorBlock.HINGE, DoorHingeSide.LEFT)
                    .setValue(DoorBlock.OPEN, false).setValue(DoorBlock.POWERED, false);
            set(d[0], floorY, d[1], lower);
            set(d[0], floorY + 1, d[1], lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
            // Bedienelement an dem Wandblock neben der Tür (Höhe der oberen Türhälfte)
            if (ctrlPos < 0) {
                return new int[]{d[0], d[1], side, doorPos};
            }
            int[] w = linePos(door[0], door[1], side, ctrlPos);
            int ox = w[0] + outward.getStepX();
            int oz = w[1] + outward.getStepZ();
            int ix = w[0] + inward.getStepX();
            int iz = w[1] + inward.getStepZ();
            int y = floorY + 1;
            if (buttons) {
                BlockState b = Blocks.STONE_BUTTON.defaultBlockState().setValue(ButtonBlock.FACE, AttachFace.WALL);
                if (airLike(ox, y, oz)) {
                    setFree(ox, y, oz, b.setValue(ButtonBlock.FACING, outward));
                }
                if (airLike(ix, y, iz)) {
                    setFree(ix, y, iz, b.setValue(ButtonBlock.FACING, inward));
                }
            } else if (airLike(ox, y, oz)) {
                setFree(ox, y, oz, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.WALL)
                        .setValue(LeverBlock.FACING, outward).setValue(LeverBlock.POWERED, false));
            }
            result = new int[]{d[0], d[1], side, doorPos};
            return result;
        }

        /**
         * Platz für Knopf/Hebel: Luft, und kein waagrechter Nachbar ist eine Öffnung in einer Wandlinie
         * (sonst stünde das Bedienelement im Durchgang eines anderen Raums).
         */
        boolean controlSpotOk(int x, int y, int z) {
            if (!airLike(x, y, z)) {
                return false;
            }
            for (int d = 0; d < 4; d++) {
                int nx = x + DX[d];
                int nz = z + DZ[d];
                boolean line = Math.floorMod(nx, p.unit) == 0 || Math.floorMod(nz, p.unit) == 0;
                if (line && !ownLine.contains(xz(nx, nz)) && (airLike(nx, y, nz) || airLike(nx, y - 1, nz))) {
                    return false;
                }
            }
            return true;
        }

        /** Weltposition (x, z) eines Punkts auf der Wandlinie einer Zellkante. */
        int[] linePos(int ux, int uz, int side, int pos) {
            int unit = g.unit();
            int lx;
            int lz;
            switch (side) {
                case 0 -> {
                    lx = (ux + 1) * unit;
                    lz = uz * unit + pos;
                }
                case 2 -> {
                    lx = ux * unit;
                    lz = uz * unit + pos;
                }
                case 1 -> {
                    lx = ux * unit + pos;
                    lz = (uz + 1) * unit;
                }
                default -> {
                    lx = ux * unit + pos;
                    lz = uz * unit;
                }
            }
            return new int[]{g.baseX() + lx, g.baseZ() + lz};
        }

        // --- Wasser ---------------------------------------------------------------------------

        void water() {
            seal(false);
            BlockState w = Blocks.WATER.defaultBlockState();
            for (int[] t : tiles()) {
                int x = g.wx(t[0]);
                int z = g.wz(t[1]);
                for (int y = floorY; y <= topY; y++) {
                    if (airLike(x, y, z)) {
                        set(x, y, z, w);
                    }
                }
            }
        }

        // --- Sculk ----------------------------------------------------------------------------

        void sculk() {
            seedBlobs(Math.max(2, g.gw() * g.gd() / 60), 4.5);
            BlockState sculkBlock = Blocks.SCULK.defaultBlockState();
            List<int[]> sculkFloor = new ArrayList<>();
            for (int[] t : tiles()) {
                double b = blob(t[0], t[1]);
                int x = g.wx(t[0]);
                int z = g.wz(t[1]);
                if (rng.nextDouble() < 0.35 + 0.65 * b) {
                    set(x, floorY - 1, z, sculkBlock);
                    if (!blocked[t[1] * g.gw() + t[0]]) {
                        set(x, floorY, z, Blocks.AIR.defaultBlockState());
                    }
                    sculkFloor.add(t);
                }
                // Decke
                if (rng.nextDouble() < 0.25 + 0.6 * b) {
                    set(x, topY + 1, z, sculkBlock);
                } else if (rng.nextDouble() < 0.35 * b + 0.1) {
                    setFree(x, topY, z, vein(Direction.UP));
                }
                // Wände
                int side = wallSide(t[0], t[1]);
                if (side >= 0) {
                    int wx = x + DX[side];
                    int wz = z + DZ[side];
                    for (int y = floorY; y <= topY; y++) {
                        int k = sampler.kind(wx, y, wz);
                        if ((k == BlockKind.WALL || k == BlockKind.PILLAR) && rng.nextDouble() < 0.2 + 0.7 * b) {
                            set(wx, y, wz, sculkBlock);
                        } else if (y >= floorY + 2 && y < topY && (k == BlockKind.WALL || k == BlockKind.PILLAR)
                                && rng.nextDouble() < 0.45 * b) {
                            setFree(x, y, z, vein(DIR[side]));
                        }
                    }
                }
            }
            // Sensoren und ein Katalysator auf dem Sculk-Boden
            int sensors = 2 + rng.nextInt(3);
            boolean catalyst = false;
            for (int i = 0; i < 40 && (sensors > 0 || !catalyst) && !sculkFloor.isEmpty(); i++) {
                int[] t = sculkFloor.get(rng.nextInt(sculkFloor.size()));
                if (!claim(t)) {
                    continue;
                }
                if (!catalyst) {
                    setTile(t[0], floorY, t[1], Blocks.SCULK_CATALYST.defaultBlockState());
                    catalyst = true;
                } else {
                    setTile(t[0], floorY, t[1], Blocks.SCULK_SENSOR.defaultBlockState());
                    sensors--;
                }
            }
            // Spezialtruhe: an einer Wand, möglichst weit von den Türen
            int[] chest = farthestWallTile();
            if (chest != null && claim(new int[]{chest[0], chest[1]})) {
                Direction facing = DIR[(chest[2] + 2) % 4];
                ops.add(new Op(g.wx(chest[0]), floorY, g.wz(chest[1]),
                        Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), false,
                        ResourceLocation.fromNamespaceAndPath(OfficeRoomsIds.MOD_ID, "chests/sculk_special")));
                set(g.wx(chest[0]), floorY - 1, g.wz(chest[1]), sculkBlock);
            }
            // Genau ein Headless Warden: auf einem Feld mit freiem 3×3-Umfeld, Sculk darunter
            int[] spot = openSpot(chest);
            if (spot != null) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int x = g.wx(spot[0] + dx);
                        int z = g.wz(spot[1] + dz);
                        set(x, floorY - 1, z, sculkBlock);
                        set(x, floorY, z, Blocks.AIR.defaultBlockState());
                        for (int y = floorY + 1; y <= floorY + 2; y++) {
                            set(x, y, z, Blocks.AIR.defaultBlockState());
                        }
                    }
                }
                spawns.add(new Spawn(Kind.HEADLESS_WARDEN, g.wx(spot[0]) + 0.5, floorY, g.wz(spot[1]) + 0.5,
                        rng.nextInt(360), null));
            } else if ((spot = anyFreeTile()) != null) {
                // 0.23.1: small or crowded sculk rooms: any free tile (the Warden fits a 1x1 column)
                set(g.wx(spot[0]), floorY - 1, g.wz(spot[1]), sculkBlock);
                spawns.add(new Spawn(Kind.HEADLESS_WARDEN, g.wx(spot[0]) + 0.5, floorY, g.wz(spot[1]) + 0.5,
                        rng.nextInt(360), null));
            } else {
                OfficeRooms.LOGGER.debug("Sculk room {} has no free spot for the Warden", r);
            }
        }

        BlockState vein(Direction face) {
            BlockState v = Blocks.SCULK_VEIN.defaultBlockState();
            for (Direction d : Direction.values()) {
                v = v.setValue(MultifaceBlock.getFaceProperty(d), d == face);
            }
            return v;
        }

        /** Wandfeld (bx, bz, Wandseite) mit größtem Abstand zu den Türen. */
        int[] farthestWallTile() {
            int[] dist = doorDistances();
            int[] best = null;
            int bestD = -1;
            for (int[] t : tiles()) {
                int side = wallSide(t[0], t[1]);
                if (side < 0 || !placeable(t[0], t[1])) {
                    continue;
                }
                int d = dist[t[1] * g.gw() + t[0]];
                if (d > bestD) {
                    bestD = d;
                    best = new int[]{t[0], t[1], side};
                }
            }
            return best;
        }

        int[] doorDistances() {
            int n = g.gw() * g.gd();
            int[] dist = new int[n];
            java.util.Arrays.fill(dist, Integer.MAX_VALUE / 2);
            ArrayDeque<int[]> q = new ArrayDeque<>();
            for (int[] t : tiles()) {
                if (g.keep(t[0], t[1])) {
                    dist[t[1] * g.gw() + t[0]] = 0;
                    q.add(t);
                }
            }
            while (!q.isEmpty()) {
                int[] c = q.poll();
                int dc = dist[c[1] * g.gw() + c[0]];
                for (int d = 0; d < 4; d++) {
                    int nx = c[0] + DX[d];
                    int nz = c[1] + DZ[d];
                    if (g.in(nx, nz) && dist[nz * g.gw() + nx] > dc + 1) {
                        dist[nz * g.gw() + nx] = dc + 1;
                        q.add(new int[]{nx, nz});
                    }
                }
            }
            return dist;
        }

        /** Feld mit freiem 3×3-Umfeld (keine Möbel), bevorzugt nahe {@code near}. */
        int[] anyFreeTile() {
            List<int[]> all = new ArrayList<>();
            for (int[] t : tiles()) {
                if (free(t[0], t[1])) {
                    all.add(t);
                }
            }
            return all.isEmpty() ? null : all.get(rng.nextInt(all.size()));
        }

        int[] openSpot(int[] near) {
            int[] best = null;
            double bestD = Double.MAX_VALUE;
            for (int[] t : tiles()) {
                boolean ok = true;
                for (int dz = -1; dz <= 1 && ok; dz++) {
                    for (int dx = -1; dx <= 1 && ok; dx++) {
                        ok = free(t[0] + dx, t[1] + dz);
                    }
                }
                if (!ok) {
                    continue;
                }
                double d = near == null ? rng.nextDouble() : Math.hypot(t[0] - near[0], t[1] - near[1]) + rng.nextDouble();
                if (near != null && d < 2.5) {
                    continue;
                }
                if (d < bestD) {
                    bestD = d;
                    best = t;
                }
            }
            return best;
        }

        // --- Lush Cave ------------------------------------------------------------------------

        void lush() {
            seedBlobs(Math.max(3, g.gw() * g.gd() / 40), 5.5);
            BlockState moss = Blocks.MOSS_BLOCK.defaultBlockState();
            List<int[]> mossTiles = new ArrayList<>();
            // Bäume (einzige natürliche Holzquelle der Dimension)
            List<int[]> trees = new ArrayList<>();
            int wantTrees = Math.max(2, g.gw() * g.gd() / 50);
            int clear = topY - floorY + 1;
            for (int i = 0; i < 200 && trees.size() < wantTrees; i++) {
                int bx = 2 + rng.nextInt(Math.max(1, g.gw() - 4));
                int bz = 2 + rng.nextInt(Math.max(1, g.gd() - 4));
                boolean ok = placeable(bx, bz) && clear >= 7;
                for (int[] t : trees) {
                    ok &= Math.abs(t[0] - bx) + Math.abs(t[1] - bz) >= 5;
                }
                for (int dz = -2; dz <= 2 && ok; dz++) {
                    for (int dx = -2; dx <= 2 && ok; dx++) {
                        ok = g.in(bx + dx, bz + dz);
                    }
                }
                if (ok && claim(new int[]{bx, bz})) {
                    trees.add(new int[]{bx, bz});
                }
            }
            // Überwucherte Schreibtische (Vanilla-Blöcke, teils mit Moos bedeckt)
            int desks = 1 + Math.min(3, g.gw() * g.gd() / 90);
            for (int i = 0; i < 80 && desks > 0; i++) {
                int bx = 1 + rng.nextInt(Math.max(1, g.gw() - 2));
                int bz = 1 + rng.nextInt(Math.max(1, g.gd() - 2));
                boolean alongX = rng.chance(0.5);
                int[] a = {bx, bz};
                int[] b = alongX ? new int[]{bx + 1, bz} : new int[]{bx, bz + 1};
                int[] c = alongX ? new int[]{bx - 1, bz} : new int[]{bx, bz - 1};
                if (!claim(a, b, c)) {
                    continue;
                }
                for (int[] t : new int[][]{a, b, c}) {
                    setTile(t[0], floorY, t[1], Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
                    if (rng.chance(0.55)) {
                        setTile(t[0], floorY + 1, t[1], Blocks.MOSS_CARPET.defaultBlockState());
                    } else if (rng.chance(0.25)) {
                        setTile(t[0], floorY + 1, t[1], Blocks.POTTED_AZALEA.defaultBlockState());
                    }
                }
                // umgekippter Stuhl daneben
                int[] ch = alongX ? new int[]{bx, bz + 1} : new int[]{bx + 1, bz};
                if (claim(ch)) {
                    setTile(ch[0], floorY, ch[1], Blocks.MOSSY_STONE_BRICK_STAIRS.defaultBlockState()
                            .setValue(StairBlock.FACING, DIR[rng.nextInt(4)]));
                }
                desks--;
            }
            // Truhe mit Lush-Loot (u. a. Holz)
            int[] chest = farthestWallTile();
            if (chest != null && claim(new int[]{chest[0], chest[1]})) {
                ops.add(new Op(g.wx(chest[0]), floorY, g.wz(chest[1]),
                        Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, DIR[(chest[2] + 2) % 4]), false,
                        ResourceLocation.fromNamespaceAndPath(OfficeRoomsIds.MOD_ID, "chests/lush_cave")));
            }
            // Boden und Bodenbewuchs
            for (int[] t : tiles()) {
                double b = blob(t[0], t[1]);
                int x = g.wx(t[0]);
                int z = g.wz(t[1]);
                boolean isBlocked = blocked[t[1] * g.gw() + t[0]];
                if (rng.nextDouble() < 0.45 + 0.55 * b) {
                    set(x, floorY - 1, z, moss);
                    mossTiles.add(t);
                    if (!isBlocked) {
                        double v = rng.nextDouble();
                        BlockState top;
                        if (v < 0.35) {
                            top = Blocks.MOSS_CARPET.defaultBlockState();
                        } else if (v < 0.47) {
                            top = Blocks.GRASS.defaultBlockState();
                        } else if (v < 0.52) {
                            top = Blocks.FERN.defaultBlockState();
                        } else if (v < 0.55 && !g.keep(t[0], t[1]) && claim(t)) {
                            top = rng.chance(0.5) ? Blocks.AZALEA.defaultBlockState() : Blocks.FLOWERING_AZALEA.defaultBlockState();
                        } else {
                            top = Blocks.AIR.defaultBlockState();
                        }
                        set(x, floorY, z, top);
                    }
                }
                // Decke: Moos, Leuchtbeeren-Ranken, Sporenblüten
                if (rng.nextDouble() < 0.3 + 0.5 * b) {
                    set(x, topY + 1, z, moss);
                }
                double v = rng.nextDouble();
                if (v < 0.10 + 0.12 * b) {
                    int len = 1 + rng.nextInt(Math.max(1, Math.min(6, clear - 3)));
                    for (int i = 0; i < len; i++) {
                        int y = topY - i;
                        if (y < floorY + 2) {
                            break;
                        }
                        boolean last = i == len - 1 || y - 1 < floorY + 2;
                        boolean berries = rng.chance(0.35);
                        BlockState s = last
                                ? Blocks.CAVE_VINES.defaultBlockState().setValue(CaveVines.BERRIES, berries)
                                .setValue(BlockStateProperties.AGE_25, 25)
                                : Blocks.CAVE_VINES_PLANT.defaultBlockState().setValue(CaveVines.BERRIES, berries);
                        setFree(x, y, z, s);
                        if (last) {
                            break;
                        }
                    }
                } else if (v < 0.13 + 0.12 * b) {
                    setFree(x, topY, z, Blocks.SPORE_BLOSSOM.defaultBlockState());
                }
                // Wände: Moos unten, Ranken
                int side = wallSide(t[0], t[1]);
                if (side >= 0) {
                    int wx = x + DX[side];
                    int wz = z + DZ[side];
                    for (int y = floorY; y <= topY; y++) {
                        int k = sampler.kind(wx, y, wz);
                        if (k != BlockKind.WALL && k != BlockKind.PILLAR) {
                            continue;
                        }
                        double h = (y - floorY) / (double) Math.max(1, clear);
                        if (rng.nextDouble() < (0.65 - h) * (0.4 + b)) {
                            set(wx, y, wz, moss);
                        } else if (y >= floorY + 2 && rng.nextDouble() < 0.25 + 0.3 * b) {
                            setFree(x, y, z, Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(DIR[side]), true));
                        }
                    }
                }
            }
            // Bäume zuletzt (Stamm und Krone überschreiben Bewuchs)
            for (int[] t : trees) {
                tree(t[0], t[1], clear);
            }
            // 1–2 Frösche auf freien Moosfeldern (dauerhaft, keine weiteren Spawns)
            int frogs = 1 + rng.nextInt(2);
            for (int i = 0; i < 60 && frogs > 0 && !mossTiles.isEmpty(); i++) {
                int[] t = mossTiles.get(rng.nextInt(mossTiles.size()));
                if (!free(t[0], t[1]) || g.keep(t[0], t[1])) {
                    continue;
                }
                spawns.add(new Spawn(Kind.FROG, g.wx(t[0]) + 0.5, floorY, g.wz(t[1]) + 0.5, rng.nextInt(360), null));
                frogs--;
            }
        }

        void tree(int bx, int bz, int clear) {
            int h = Math.min(3 + rng.nextInt(3), clear - 4);
            h = Math.max(3, h);
            int x = g.wx(bx);
            int z = g.wz(bz);
            set(x, floorY - 1, z, Blocks.ROOTED_DIRT.defaultBlockState());
            for (int i = 0; i < h; i++) {
                set(x, floorY + i, z, Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y));
            }
            int top = floorY + h - 1;
            for (int dy = 0; dy <= 2; dy++) {
                int y = top + dy;
                if (y > topY) {
                    break;
                }
                int rad = dy == 2 ? 1 : 2;
                for (int dz = -rad; dz <= rad; dz++) {
                    for (int dx = -rad; dx <= rad; dx++) {
                        if (dx == 0 && dz == 0 && dy < 1) {
                            continue;
                        }
                        if (Math.abs(dx) == rad && Math.abs(dz) == rad && rng.chance(0.6)) {
                            continue;
                        }
                        if (!g.in(bx + dx, bz + dz)) {
                            continue;
                        }
                        BlockState leaves = (rng.chance(0.3) ? Blocks.FLOWERING_AZALEA_LEAVES : Blocks.AZALEA_LEAVES)
                                .defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
                        setFree(g.wx(bx + dx), y, g.wz(bz + dz), leaves);
                    }
                }
            }
        }


        // --- Gemeinsame Lager-Dekoration (Outposts und verlassene Camps) ------------------------

        BlockState facing(Block b, Direction d) {
            BlockState st = b.defaultBlockState();
            if (st.hasProperty(HorizontalDirectionalBlock.FACING)) {
                st = st.setValue(HorizontalDirectionalBlock.FACING, d);
            } else if (st.hasProperty(BlockStateProperties.FACING)) {
                st = st.setValue(BlockStateProperties.FACING, d);
            }
            return st;
        }

        /** Freies Feld mit 3×3-Umfeld möglichst nahe an der Raummitte (zufällig verschoben). */
        int[] centerSpot() {
            int cx = g.gw() / 2 + rng.nextInt(5) - 2;
            int cz = g.gd() / 2 + rng.nextInt(5) - 2;
            int[] best = null;
            double bd = Double.MAX_VALUE;
            for (int[] t : tiles()) {
                boolean ok = true;
                for (int dz = -2; dz <= 2 && ok; dz++) {
                    for (int dx = -2; dx <= 2 && ok; dx++) {
                        ok = placeable(t[0] + dx, t[1] + dz);
                    }
                }
                if (!ok) {
                    continue;
                }
                double d = Math.hypot(t[0] - cx, t[1] - cz);
                if (d < bd) {
                    bd = d;
                    best = t;
                }
            }
            return best;
        }

        /** Feuerstelle mit Baumstamm-Bänken. lit = brennt noch, food = Essen auf dem Feuer. */
        int[] firePit(boolean lit, boolean food) {
            return firePit(lit, food, false);
        }

        int[] firePit(boolean lit, boolean food, boolean officeChairs) {
            int[] c = centerSpot();
            if (c == null || !claim(c)) {
                return null;
            }
            BlockState fire = Blocks.CAMPFIRE.defaultBlockState().setValue(net.minecraft.world.level.block.CampfireBlock.LIT, lit)
                    .setValue(net.minecraft.world.level.block.CampfireBlock.SIGNAL_FIRE, false);
            Object extra = food ? new net.minecraft.world.item.ItemStack(rng.chance(0.5) ? net.minecraft.world.item.Items.BEEF
                    : net.minecraft.world.item.Items.POTATO) : null;
            ops.add(new Op(g.wx(c[0]), floorY, g.wz(c[1]), fire, false, null, extra));
            int benches = 2 + rng.nextInt(2);
            int start = rng.nextInt(4);
            for (int i = 0; i < benches; i++) {
                int d = (start + i) % 4;
                int[] b = {c[0] + DX[d] * 2, c[1] + DZ[d] * 2};
                if (claim(b)) {
                    Direction.Axis ax = d % 2 == 0 ? Direction.Axis.Z : Direction.Axis.X;
                    Block log = rng.chance(0.3) ? Blocks.STRIPPED_OAK_LOG : rng.chance(0.4) ? Blocks.SPRUCE_LOG : Blocks.OAK_LOG;
                    BlockState bench = log.defaultBlockState().setValue(RotatedPillarBlock.AXIS, ax);
                    if (officeChairs && rng.chance(0.5)) {
                        // aus den Büros zusammengetragener Bürostuhl, zur Feuerstelle gedreht
                        office(g.wx(b[0]), floorY, g.wz(b[1]), rng.chance(0.5) ? "chair_gray" : "chair_black", DIR[(d + 2) % 4], bench);
                    } else {
                        setTile(b[0], floorY, b[1], bench);
                    }
                }
            }
            return c;
        }

        /** Wandfelder in zufälliger Reihenfolge {bx, bz, Seite}. */
        List<int[]> shuffledWalls() {
            List<int[]> walls = new ArrayList<>();
            for (int[] t : tiles()) {
                int side = wallSide(t[0], t[1]);
                if (side >= 0 && placeable(t[0], t[1])) {
                    walls.add(new int[]{t[0], t[1], side});
                }
            }
            for (int i = walls.size() - 1; i > 0; i--) {
                int j = rng.nextInt(i + 1);
                int[] tmp = walls.get(i);
                walls.set(i, walls.get(j));
                walls.set(j, tmp);
            }
            return walls;
        }

        /**
         * Improvisiertes Lager: Feuerstelle, Fässer, Heuballen, Holzstapel, Laternenpfosten, Werkbank,
         * Truhe. Unregelmäßig verteilt (zufällige Wandfelder), Wege bleiben frei.
         */
        void survivalDecor(boolean abandoned) {
            firePit(!abandoned || rng.chance(0.5), abandoned && rng.chance(0.6), !abandoned);
            String wood = wood(abandoned);
            // Esstisch mit Stühlen (Außenposten immer, verlassene Lager oft; dort auch umgestoßen = Hocker)
            if (!abandoned || rng.chance(0.6)) {
                diningSet(wood, abandoned && rng.chance(0.5), true, floorY);
            }
            List<int[]> walls = shuffledWalls();
            int n = 0;
            int want = 5 + rng.nextInt(5);
            String[] kinds = abandoned
                    ? new String[]{"crate", "hay", "logs", "lantern", "crafting", "jar", "chest", "grill", "barrel", "cooler", "logs", "cauldron"}
                    : new String[]{"barrel", "crate", "hay", "logs", "lantern", "workbench", "barrel2", "chest", "jar", "grill", "watercooler",
                    "smoker", "cooler", "bin", "cauldron"};
            int k = rng.nextInt(kinds.length);
            for (int[] w : walls) {
                if (n >= want) {
                    break;
                }
                if (!claim(w)) {
                    continue;
                }
                Direction in = DIR[(w[2] + 2) % 4];
                int x = g.wx(w[0]);
                int z = g.wz(w[1]);
                String kind = kinds[k++ % kinds.length];
                switch (kind) {
                    case "barrel" -> set(x, floorY, z, facing(Blocks.BARREL, Direction.UP));
                    case "barrel2" -> {
                        set(x, floorY, z, facing(Blocks.BARREL, Direction.UP));
                        set(x, floorY + 1, z, facing(Blocks.BARREL, in));
                    }
                    case "hay" -> {
                        set(x, floorY, z, Blocks.HAY_BLOCK.defaultBlockState());
                        if (rng.chance(0.4)) {
                            set(x, floorY + 1, z, Blocks.HAY_BLOCK.defaultBlockState());
                        }
                    }
                    case "logs" -> {
                        Direction.Axis ax = w[2] % 2 == 0 ? Direction.Axis.Z : Direction.Axis.X;
                        set(x, floorY, z, Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, ax));
                        set(x, floorY + 1, z, (rng.chance(0.5) ? Blocks.SPRUCE_LOG : Blocks.OAK_LOG).defaultBlockState().setValue(RotatedPillarBlock.AXIS, ax));
                    }
                    case "lantern" -> {
                        set(x, floorY, z, Blocks.OAK_FENCE.defaultBlockState());
                        set(x, floorY + 1, z, Blocks.LANTERN.defaultBlockState());
                    }
                    case "crafting" -> set(x, floorY, z, Blocks.CRAFTING_TABLE.defaultBlockState());
                    case "workbench" -> set(x, floorY, z, rf("workbench", in, Blocks.CRAFTING_TABLE.defaultBlockState()));
                    case "crate" -> {
                        loot(x, floorY, z, rf(wood + "_crate", in, facing(Blocks.BARREL, Direction.UP)),
                                abandoned ? "abandoned_camp" : "outpost");
                        if (rng.chance(0.4)) {
                            set(x, floorY + 1, z, rf(wood + "_crate", in, facing(Blocks.BARREL, Direction.UP)));
                        }
                    }
                    case "jar" -> {
                        set(x, floorY, z, facing(Blocks.BARREL, Direction.UP));
                        set(x, floorY + 1, z, rf(wood + "_storage_jar", in, Blocks.DECORATED_POT.defaultBlockState()));
                    }
                    case "grill" -> set(x, floorY, z, rf(color() + "_grill", in, facing(Blocks.SMOKER, in)));
                    case "cooler" -> loot(x, floorY, z, rf(color() + "_cooler", in, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, in)),
                            abandoned ? "abandoned_camp" : "outpost");
                    case "bin" -> set(x, floorY, z, rf("recycle_bin", in, Blocks.COMPOSTER.defaultBlockState()));
                    case "watercooler" -> office(x, floorY, z, "water_cooler", in, Blocks.WATER_CAULDRON.defaultBlockState()
                            .setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 3));
                    case "smoker" -> set(x, floorY, z, facing(Blocks.SMOKER, in));
                    case "cauldron" -> set(x, floorY, z, Blocks.WATER_CAULDRON.defaultBlockState()
                            .setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 1 + rng.nextInt(3)));
                    default -> ops.add(new Op(x, floorY, z, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, in), false,
                            ResourceLocation.fromNamespaceAndPath(OfficeRoomsIds.MOD_ID, abandoned ? "chests/abandoned_camp" : "chests/outpost")));
                }
                n++;
            }
        }

        // --- Rückkehrportal (unterste Ebene) ----------------------------------------------------

        void exitRoom() {
            int[] w = farthestWallTile();
            if (w == null) {
                return;
            }
            int side = w[2];
            int along = (side + 1) % 4;
            int[] l = {w[0] + DX[along], w[1] + DZ[along]};
            int[] r2 = {w[0] - DX[along], w[1] - DZ[along]};
            int[] front = {w[0] - DX[side], w[1] - DZ[side]};
            boolean wide = placeable(l[0], l[1]) && placeable(r2[0], r2[1]) && wallSide(l[0], l[1]) == side && wallSide(r2[0], r2[1]) == side;
            if (!free(front[0], front[1]) || !(wide ? claim(w, l, r2) : claim(new int[]{w[0], w[1]}))) {
                OfficeRooms.LOGGER.warn("Exit room {} has no space for the portal", r);
                return;
            }
            BlockState portal = de.officerooms.block.ModBlocks.OFFICE_PORTAL.get().defaultBlockState();
            BlockState frame = Blocks.CRYING_OBSIDIAN.defaultBlockState();
            setTile(w[0], floorY, w[1], portal);
            setTile(w[0], floorY + 1, w[1], portal);
            setTile(w[0], floorY + 2, w[1], frame);
            if (wide) {
                for (int[] t : new int[][]{l, r2}) {
                    for (int y = floorY; y <= floorY + 2; y++) {
                        setTile(t[0], y, t[1], frame);
                    }
                }
            }
        }

        // --- Verlassenes Lager ------------------------------------------------------------------

        void camp() {
            seedBlobs(Math.max(2, g.gw() * g.gd() / 70), 4.0);
            // Zelte an den Wänden (1–3): Seiten und First aus Wolle, Schlafsack (Bett) innen
            int tents = 1 + rng.nextInt(Math.min(3, 1 + g.gw() * g.gd() / 180));
            Block[] wool = {Blocks.BROWN_WOOL, Blocks.WHITE_WOOL, Blocks.GREEN_WOOL, Blocks.LIGHT_GRAY_WOOL};
            List<int[]> walls = shuffledWalls();
            int built = 0;
            for (int[] w : walls) {
                if (built >= tents) {
                    break;
                }
                int side = w[2];
                int along = (side + 1) % 4;
                List<int[]> cells = new ArrayList<>();
                for (int i = -1; i <= 1; i++) {
                    for (int j = 0; j <= 2; j++) {
                        cells.add(new int[]{w[0] + DX[along] * i - DX[side] * j, w[1] + DZ[along] * i - DZ[side] * j});
                    }
                }
                boolean onWall = wallSide(w[0] + DX[along], w[1] + DZ[along]) == side && wallSide(w[0] - DX[along], w[1] - DZ[along]) == side;
                if (!onWall || !claim(cells.toArray(new int[0][]))) {
                    continue;
                }
                Block cloth = wool[rng.nextInt(wool.length)];
                for (int j = 0; j <= 2; j++) {
                    for (int i = -1; i <= 1; i += 2) {
                        setTile(w[0] + DX[along] * i - DX[side] * j, floorY, w[1] + DZ[along] * i - DZ[side] * j, cloth.defaultBlockState());
                        if (rng.chance(0.3)) {
                            // Moos auf der Zeltbahn (Teppich nur auf vollem Wollblock)
                            setTile(w[0] + DX[along] * i - DX[side] * j, floorY + 1, w[1] + DZ[along] * i - DZ[side] * j,
                                    Blocks.MOSS_CARPET.defaultBlockState());
                        }
                    }
                    setTile(w[0] - DX[side] * j, floorY + 1, w[1] - DZ[side] * j, cloth.defaultBlockState());
                }
                // Schlafsack: Kopf an der Wand
                BlockState bed = (rng.chance(0.5) ? Blocks.BROWN_BED : Blocks.GREEN_BED).defaultBlockState().setValue(BedBlock.FACING, DIR[side]);
                setTile(w[0], floorY, w[1], bed.setValue(BedBlock.PART, BedPart.HEAD));
                setTile(w[0] - DX[side], floorY, w[1] - DZ[side], bed.setValue(BedBlock.PART, BedPart.FOOT));
                setTile(w[0] - 2 * DX[side], floorY, w[1] - 2 * DZ[side],
                        rng.chance(0.6) ? rf("door_mat", DIR[(side + 2) % 4], Blocks.AIR.defaultBlockState()) : Blocks.AIR.defaultBlockState());
                built++;
            }
            survivalDecor(true);
            // Lesepult mit einem Tagebuch
            for (int[] w : shuffledWalls()) {
                if (!claim(w)) {
                    continue;
                }
                Stories.Book book = Stories.ALL[(int) (Hashing.unit(Hashing.hash(p.seed, rx, rz, 0xB00C, r.index)) * Stories.ALL.length)];
                ops.add(new Op(g.wx(w[0]), floorY, g.wz(w[1]),
                        facing(Blocks.LECTERN, DIR[(w[2] + 2) % 4]).setValue(net.minecraft.world.level.block.LecternBlock.HAS_BOOK, true),
                        false, null, book));
                break;
            }
            // Verwitterung: Boden, Bewuchs, Spinnweben, Ranken
            for (int[] t : tiles()) {
                double b = blob(t[0], t[1]);
                int x = g.wx(t[0]);
                int z = g.wz(t[1]);
                boolean isBlocked = blocked[t[1] * g.gw() + t[0]];
                if (rng.nextDouble() < 0.1 + 0.55 * b) {
                    double v = rng.nextDouble();
                    BlockState ground = v < 0.4 ? Blocks.COARSE_DIRT.defaultBlockState() : v < 0.7 ? Blocks.MOSS_BLOCK.defaultBlockState()
                            : Blocks.PODZOL.defaultBlockState();
                    set(x, floorY - 1, z, ground);
                    if (!isBlocked) {
                        double w2 = rng.nextDouble();
                        BlockState top = ground.is(Blocks.MOSS_BLOCK) && w2 < 0.45 ? Blocks.MOSS_CARPET.defaultBlockState()
                                : w2 < 0.15 ? Blocks.DEAD_BUSH.defaultBlockState()
                                : w2 < 0.3 ? Blocks.GRASS.defaultBlockState() : Blocks.AIR.defaultBlockState();
                        set(x, floorY, z, top);
                    }
                }
                int side = wallSide(t[0], t[1]);
                if (side >= 0 && rng.nextDouble() < 0.25 * b + 0.04) {
                    setFree(x, topY, z, Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(DIR[side]), true));
                    if (topY - 1 > floorY + 1 && rng.chance(0.5)) {
                        setFree(x, topY - 1, z, Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(DIR[side]), true));
                    }
                }
                if (side >= 0 && wallSideCount(t[0], t[1]) >= 2 && rng.chance(0.6)) {
                    setFree(x, topY, z, Blocks.COBWEB.defaultBlockState());
                }
            }
        }

        int wallSideCount(int bx, int bz) {
            int n = 0;
            for (int d = 0; d < 4; d++) {
                if (!g.in(bx + DX[d], bz + DZ[d])) {
                    n++;
                }
            }
            return n;
        }

        // --- Oase (tropischer Wasserpark) -------------------------------------------------------

        void oasis() {
            BlockState sand = Blocks.SAND.defaultBlockState();
            BlockState rim = Blocks.SMOOTH_SANDSTONE.defaultBlockState();
            BlockState water = Blocks.WATER.defaultBlockState();
            int clear = topY - floorY + 1;
            // Becken: 1 großes, 1–2 kleine, mit Rand aus glattem Sandstein
            List<int[]> pools = new ArrayList<>();
            int[][] sizes = {{7 + rng.nextInt(4), 5 + rng.nextInt(3)}, {3 + rng.nextInt(2), 3 + rng.nextInt(2)}, {4, 3}};
            int want = 2 + rng.nextInt(2);
            for (int pi = 0; pi < want; pi++) {
                int w = sizes[pi][0];
                int d = sizes[pi][1];
                for (int tries = 0; tries < 80; tries++) {
                    int x0 = 1 + rng.nextInt(Math.max(1, g.gw() - w - 2));
                    int z0 = 1 + rng.nextInt(Math.max(1, g.gd() - d - 2));
                    List<int[]> cells = new ArrayList<>();
                    for (int z = z0; z < z0 + d; z++) {
                        for (int x = x0; x < x0 + w; x++) {
                            cells.add(new int[]{x, z});
                        }
                    }
                    // ein Feld Abstand zu anderen Objekten
                    boolean ok = true;
                    for (int z = z0 - 1; z <= z0 + d && ok; z++) {
                        for (int x = x0 - 1; x <= x0 + w && ok; x++) {
                            ok = placeable(x, z);
                        }
                    }
                    if (ok && claim(cells.toArray(new int[0][]))) {
                        pools.add(new int[]{x0, z0, w, d});
                        break;
                    }
                }
            }
            // Strand: Sand auf dem Boden (Teppichebene), Laufhöhe +1. Sand liegt auf dem festen Boden und kann
            // nicht in den Raum darunter fallen. Vor den Türen Halbstufen aus Holz als Übergang.
            BlockState step = Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
            for (int[] t : tiles()) {
                if (blocked[t[1] * g.gw() + t[0]]) {
                    continue;
                }
                set(g.wx(t[0]), floorY, g.wz(t[1]), g.keep(t[0], t[1]) ? step : sand);
            }
            for (int pi = 0; pi < pools.size(); pi++) {
                int[] pl = pools.get(pi);
                for (int z = pl[1]; z < pl[1] + pl[3]; z++) {
                    for (int x = pl[0]; x < pl[0] + pl[2]; x++) {
                        boolean edge = x == pl[0] || z == pl[1] || x == pl[0] + pl[2] - 1 || z == pl[1] + pl[3] - 1;
                        int wx = g.wx(x);
                        int wz = g.wz(z);
                        if (edge) {
                            set(wx, floorY, wz, rim);
                        } else {
                            // abgesenktes Becken: türkiser Grund (Bodenblock), Wasser auf Teppichebene
                            set(wx, floorY - 1, wz, rng.chance(0.15) ? Blocks.SEA_LANTERN.defaultBlockState()
                                    : rng.chance(0.5) ? Blocks.PRISMARINE.defaultBlockState() : Blocks.WARPED_WART_BLOCK.defaultBlockState());
                            set(wx, floorY, wz, water);
                            if (pi > 0 && rng.chance(0.08)) {
                                set(wx, floorY + 1, wz, Blocks.LILY_PAD.defaultBlockState());
                            }
                        }
                    }
                }
                // Brunnen in der Mitte des großen Beckens (geplantes, ruhendes Wasser)
                if (pi == 0 && pl[2] >= 7 && pl[3] >= 5 && clear >= 7) {
                    int fx = pl[0] + pl[2] / 2;
                    int fz = pl[1] + pl[3] / 2;
                    int wx = g.wx(fx);
                    int wz = g.wz(fz);
                    set(wx, floorY, wz, Blocks.CHISELED_SANDSTONE.defaultBlockState());
                    set(wx, floorY + 1, wz, Blocks.CHISELED_SANDSTONE.defaultBlockState());
                    set(wx, floorY + 2, wz, water);
                    for (int dd = 0; dd < 4; dd++) {
                        set(wx + DX[dd], floorY + 2, wz + DZ[dd], water.setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL, 1));
                        set(wx + DX[dd], floorY + 1, wz + DZ[dd], water.setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL, 8));
                    }
                }
            }
            // Palmen
            int palms = 3 + rng.nextInt(4);
            List<int[]> trunks = new ArrayList<>();
            for (int i = 0; i < 200 && trunks.size() < palms; i++) {
                int bx = 1 + rng.nextInt(Math.max(1, g.gw() - 2));
                int bz = 1 + rng.nextInt(Math.max(1, g.gd() - 2));
                boolean ok = clear >= 8;
                for (int[] t : trunks) {
                    ok &= Math.abs(t[0] - bx) + Math.abs(t[1] - bz) >= 5;
                }
                if (ok && claim(new int[]{bx, bz})) {
                    trunks.add(new int[]{bx, bz});
                }
            }
            for (int[] t : trunks) {
                palm(t[0], t[1], clear);
            }
            // Liegen mit Sonnenschirm
            int loungers = 2 + rng.nextInt(3);
            Block[] bedColors = {Blocks.WHITE_BED, Blocks.LIGHT_BLUE_BED, Blocks.YELLOW_BED, Blocks.ORANGE_BED};
            Block[] shade = {Blocks.RED_WOOL, Blocks.WHITE_WOOL, Blocks.ORANGE_WOOL, Blocks.CYAN_WOOL};
            for (int i = 0; i < 120 && loungers > 0; i++) {
                int bx = 1 + rng.nextInt(Math.max(1, g.gw() - 2));
                int bz = 1 + rng.nextInt(Math.max(1, g.gd() - 2));
                int dir = rng.nextInt(4);
                int[] head = {bx, bz};
                int[] foot = {bx + DX[dir], bz + DZ[dir]};
                int side = (dir + 1) % 4;
                int[] pole = {bx + DX[side], bz + DZ[side]};
                if (!claim(head, foot, pole)) {
                    continue;
                }
                BlockState bed = bedColors[rng.nextInt(bedColors.length)].defaultBlockState().setValue(BedBlock.FACING, DIR[(dir + 2) % 4]);
                setTile(head[0], (floorY + 1), head[1], bed.setValue(BedBlock.PART, BedPart.HEAD));
                setTile(foot[0], (floorY + 1), foot[1], bed.setValue(BedBlock.PART, BedPart.FOOT));
                Block cloth = shade[rng.nextInt(shade.length)];
                for (int y = (floorY + 1); y <= (floorY + 1) + 2; y++) {
                    setTile(pole[0], y, pole[1], Blocks.OAK_FENCE.defaultBlockState());
                }
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int px = pole[0] + dx;
                        int pz = pole[1] + dz;
                        if (g.in(px, pz) && (floorY + 1) + 3 <= topY) {
                            setFree(g.wx(px), (floorY + 1) + 3, g.wz(pz), ((dx + dz) % 2 == 0 ? cloth : Blocks.WHITE_WOOL).defaultBlockState());
                        }
                    }
                }
                // Kühlbox neben der Liege (Refurbished Furniture), mit Oasen-Fund
                int[] box = {head[0] - DX[side], head[1] - DZ[side]};
                if (hasRf() && rng.chance(0.6) && claim(box)) {
                    loot(g.wx(box[0]), floorY + 1, g.wz(box[1]), rf(color() + "_cooler", DIR[side], Blocks.AIR.defaultBlockState()), "oasis");
                }
                loungers--;
            }
            // Strandbar: Tisch mit Hockern, daneben ein Grill (Refurbished Furniture)
            int bars = 1 + rng.nextInt(2);
            for (int i = 0; i < bars; i++) {
                // Möbel stehen auf dem Sand (eine Ebene höher)
                int[] t = diningSet("jungle", true, rng.chance(0.5), floorY + 1);
                if (t != null) {
                    for (int d = 0; d < 4; d++) {
                        int[] gr = {t[0] + DX[d] * 2, t[1] + DZ[d] * 2};
                        if (claim(gr)) {
                            setTile(gr[0], floorY + 1, gr[1], rf(color() + "_grill", DIR[(d + 2) % 4], Blocks.AIR.defaultBlockState()));
                            break;
                        }
                    }
                }
            }
            // Pflanzbeete: Gras mit Farnen und Blumen, Bambus auf Sand
            for (int[] t : tiles()) {
                if (!placeable(t[0], t[1]) || rng.nextDouble() > 0.07) {
                    continue;
                }
                int x = g.wx(t[0]);
                int z = g.wz(t[1]);
                double v = rng.nextDouble();
                if (v < 0.35 && claim(t)) {
                    int h = 2 + rng.nextInt(Math.max(1, Math.min(4, clear - 3)));
                    for (int y = (floorY + 1); y < (floorY + 1) + h; y++) {
                        set(x, y, z, Blocks.BAMBOO.defaultBlockState()
                                .setValue(net.minecraft.world.level.block.BambooStalkBlock.LEAVES,
                                        y >= (floorY + 1) + h - 2 ? net.minecraft.world.level.block.state.properties.BambooLeaves.LARGE
                                                : net.minecraft.world.level.block.state.properties.BambooLeaves.NONE));
                    }
                } else {
                    set(x, floorY, z, Blocks.GRASS_BLOCK.defaultBlockState());
                    Block plant = v < 0.55 ? Blocks.FERN : v < 0.7 ? Blocks.BLUE_ORCHID : v < 0.8 ? Blocks.ALLIUM : Blocks.GRASS;
                    set(x, (floorY + 1), z, plant.defaultBlockState());
                }
            }
            // Holzstege: von den Türflächen zur Raummitte (Halbstufen, begehbar)
            int[] centre = {g.gw() / 2, g.gd() / 2};
            for (int[] t : tiles()) {
                if (!g.keep(t[0], t[1]) || rng.nextDouble() > 0.5) {
                    continue;
                }
                int x = t[0];
                int z = t[1];
                for (int walk = 0; walk < 40; walk++) {
                    if (free(x, z) && !g.noFurniture(x, z)) {
                        if (!g.keep(x, z)) {
                            set(g.wx(x), floorY, g.wz(z), Blocks.OAK_PLANKS.defaultBlockState());
                            set(g.wx(x), floorY + 1, g.wz(z), Blocks.AIR.defaultBlockState());
                        }
                    } else if (!g.keep(x, z)) {
                        break;
                    }
                    if (x != centre[0] && (z == centre[1] || rng.chance(0.5))) {
                        x += Integer.signum(centre[0] - x);
                    } else if (z != centre[1]) {
                        z += Integer.signum(centre[1] - z);
                    } else {
                        break;
                    }
                }
            }
            // Truhen mit Oasen-Loot unter Palmen
            int chests = 1 + rng.nextInt(2);
            for (int[] t : trunks) {
                if (chests <= 0) {
                    break;
                }
                for (int d = 0; d < 4 && chests > 0; d++) {
                    int[] c = {t[0] + DX[d], t[1] + DZ[d]};
                    if (claim(c)) {
                        ops.add(new Op(g.wx(c[0]), (floorY + 1), g.wz(c[1]), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, DIR[d]),
                                false, ResourceLocation.fromNamespaceAndPath(OfficeRoomsIds.MOD_ID, "chests/oasis")));
                        chests--;
                    }
                }
            }
        }

        void palm(int bx, int bz, int clear) {
            int h = Math.max(4, Math.min(5 + rng.nextInt(3), clear - 3));
            int x = g.wx(bx);
            int z = g.wz(bz);
            for (int i = 0; i < h; i++) {
                set(x, (floorY + 1) + i, z, Blocks.JUNGLE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y));
            }
            int top = (floorY + 1) + h - 1;
            BlockState leaves = Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
            if (top + 1 <= topY) {
                setFree(x, top + 1, z, leaves);
            }
            for (int d = 0; d < 4; d++) {
                for (int k = 1; k <= 3; k++) {
                    int lx = bx + DX[d] * k;
                    int lz = bz + DZ[d] * k;
                    if (!g.in(lx, lz)) {
                        break;
                    }
                    int y = k == 3 ? top - 1 : top;
                    setFree(g.wx(lx), y, g.wz(lz), leaves);
                }
                int dx2 = DX[d] + DX[(d + 1) % 4];
                int dz2 = DZ[d] + DZ[(d + 1) % 4];
                if (g.in(bx + dx2, bz + dz2)) {
                    setFree(g.wx(bx + dx2), top, g.wz(bz + dz2), leaves);
                }
            }
            // Kokosnüsse (Kakaofrüchte) am Stamm
            for (int d = 0; d < 4; d++) {
                if (rng.chance(0.35) && g.in(bx + DX[d], bz + DZ[d])) {
                    setFree(g.wx(bx + DX[d]), top - 1, g.wz(bz + DZ[d]), Blocks.COCOA.defaultBlockState()
                            .setValue(net.minecraft.world.level.block.CocoaBlock.FACING, DIR[(d + 2) % 4])
                            .setValue(net.minecraft.world.level.block.CocoaBlock.AGE, 2));
                }
            }
        }

        // --- Villager Outpost -----------------------------------------------------------------

        void outpost() {
            seal(true);
            List<VillagerProfession> profs = new ArrayList<>(List.of(
                    VillagerProfession.ARMORER, VillagerProfession.BUTCHER, VillagerProfession.CLERIC,
                    VillagerProfession.LEATHERWORKER, VillagerProfession.MASON, VillagerProfession.WEAPONSMITH,
                    VillagerProfession.TOOLSMITH, VillagerProfession.CARTOGRAPHER, VillagerProfession.FLETCHER,
                    VillagerProfession.SHEPHERD, VillagerProfession.FISHERMAN));
            // Mischen (deterministisch)
            for (int i = profs.size() - 1; i > 0; i--) {
                int j = rng.nextInt(i + 1);
                VillagerProfession tmp = profs.get(i);
                profs.set(i, profs.get(j));
                profs.set(j, tmp);
            }
            int count = Math.max(3, Math.min(6, r.cells / 6));
            List<VillagerProfession> chosen = new ArrayList<>();
            chosen.add(VillagerProfession.FARMER);
            chosen.add(VillagerProfession.LIBRARIAN);
            for (VillagerProfession pr : profs) {
                if (chosen.size() >= count) {
                    break;
                }
                chosen.add(pr);
            }
            // Mitte: Feld mit Wasser und Glocke
            int cxT = g.gw() / 2;
            int czT = g.gd() / 2;
            // organisch: Feld nicht exakt in der Mitte
            int[] farm = findFarm(cxT + rng.nextInt(5) - 2, czT + rng.nextInt(3) - 1);
            if (farm != null) {
                BlockState farmland = Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7);
                Block[] crops = {Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS};
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dx = -2; dx <= 2; dx++) {
                        int bx = farm[0] + dx;
                        int bz = farm[1] + dz;
                        if (dx == 0 && dz == 0) {
                            setTile(bx, floorY, bz, Blocks.WATER.defaultBlockState());
                            continue;
                        }
                        setTile(bx, floorY, bz, farmland);
                        Block crop = crops[Math.floorMod(dx + 2, crops.length)];
                        int maxAge = crop == Blocks.BEETROOTS ? 3 : 7;
                        BlockState cs = crop.defaultBlockState();
                        if (crop instanceof CropBlock cb) {
                            cs = cb.getStateForAge(rng.nextInt(maxAge + 1));
                        }
                        setTile(bx, floorY + 1, bz, cs);
                    }
                }
                // Holztreppen als Einfassung rund um das Feld (Treppen zeigen vom Feld weg)
                for (int dz = -2; dz <= 2; dz++) {
                    for (int dx = -3; dx <= 3; dx++) {
                        if (Math.abs(dz) <= 1 && Math.abs(dx) <= 2) {
                            continue;
                        }
                        int[] t = {farm[0] + dx, farm[1] + dz};
                        if (Math.abs(dx) == 3 && Math.abs(dz) == 2) {
                            continue; // Ecken frei: wirkt gezimmert statt gegossen
                        }
                        if (!claim(t)) {
                            continue;
                        }
                        Direction out = Math.abs(dz) == 2 ? (dz > 0 ? Direction.SOUTH : Direction.NORTH) : (dx > 0 ? Direction.EAST : Direction.WEST);
                        Block stairs = rng.chance(0.25) ? Blocks.SPRUCE_STAIRS : Blocks.OAK_STAIRS;
                        setTile(t[0], floorY, t[1], stairs.defaultBlockState().setValue(StairBlock.FACING, out.getOpposite()));
                    }
                }
            } else {
                chosen.remove(VillagerProfession.FARMER);
            }
            int[] bell = null;
            for (int rad = 0; rad < Math.max(g.gw(), g.gd()) && bell == null; rad++) {
                for (int dz = -rad; dz <= rad && bell == null; dz++) {
                    for (int dx = -rad; dx <= rad && bell == null; dx++) {
                        int[] t = {cxT + dx, czT + dz + (farm != null ? 3 : 0)};
                        if (placeable(t[0], t[1]) && claim(t)) {
                            bell = t;
                        }
                    }
                }
            }
            if (bell != null) {
                setTile(bell[0], floorY, bell[1], Blocks.BELL.defaultBlockState()
                        .setValue(BellBlock.ATTACHMENT, BellAttachType.FLOOR).setValue(BellBlock.FACING, Direction.NORTH));
            }
            // Betten und Arbeitsblöcke an den Wänden
            List<int[]> walls = new ArrayList<>();
            for (int[] t : tiles()) {
                int side = wallSide(t[0], t[1]);
                if (side >= 0) {
                    walls.add(new int[]{t[0], t[1], side});
                }
            }
            for (int i = walls.size() - 1; i > 0; i--) {
                int j = rng.nextInt(i + 1);
                int[] tmp = walls.get(i);
                walls.set(i, walls.get(j));
                walls.set(j, tmp);
            }
            String[] colors = {"white", "light_gray", "gray", "cyan", "blue", "green", "red", "yellow", "brown"};
            int placed = 0;
            for (VillagerProfession prof : chosen) {
                boolean done = false;
                for (int[] w : walls) {
                    int side = w[2];
                    Direction toWall = DIR[side];
                    int[] head = {w[0], w[1]};
                    int[] foot = {w[0] - DX[side], w[1] - DZ[side]};
                    // Arbeitsblock seitlich neben dem Kopfende
                    int along = (side + 1) % 4;
                    int[] station = {w[0] + DX[along], w[1] + DZ[along]};
                    if (wallSide(station[0], station[1]) != side) {
                        station = new int[]{w[0] - DX[along], w[1] - DZ[along]};
                        if (wallSide(station[0], station[1]) != side) {
                            continue;
                        }
                    }
                    int[] stand = {foot[0] - DX[side], foot[1] - DZ[side]};
                    if (!placeable(stand[0], stand[1]) || !claim(head, foot, station)) {
                        continue;
                    }
                    String color = colors[(placed + rng.nextInt(colors.length)) % colors.length];
                    Block bed = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("minecraft", color + "_bed"));
                    if (bed == null) {
                        bed = Blocks.WHITE_BED;
                    }
                    BlockState bs = bed.defaultBlockState().setValue(BedBlock.FACING, toWall);
                    setTile(foot[0], floorY, foot[1], bs.setValue(BedBlock.PART, BedPart.FOOT));
                    setTile(head[0], floorY, head[1], bs.setValue(BedBlock.PART, BedPart.HEAD));
                    setTile(station[0], floorY, station[1], workstation(prof, DIR[(side + 2) % 4]));
                    spawns.add(new Spawn(Kind.VILLAGER, g.wx(stand[0]) + 0.5, floorY + 0.0625, g.wz(stand[1]) + 0.5,
                            DIR[(side + 2) % 4].toYRot(), prof));
                    placed++;
                    done = true;
                    break;
                }
                if (!done) {
                    OfficeRooms.LOGGER.debug("Village room {}: no space for {}", r, prof);
                }
            }
            survivalDecor(false);
            // Eisengolem auf einer freien Fläche
            int[] spot = openSpot(null);
            if (spot != null) {
                spawns.add(new Spawn(Kind.IRON_GOLEM, g.wx(spot[0]) + 0.5, floorY + 0.0625, g.wz(spot[1]) + 0.5, rng.nextInt(360), null));
                // genau eine Katze pro Außenposten
                int[] cat = openSpot(spot);
                if (cat != null) {
                    spawns.add(new Spawn(Kind.CAT, g.wx(cat[0]) + 0.5, floorY + 0.0625, g.wz(cat[1]) + 0.5, rng.nextInt(360), null));
                }
            }
        }

        int[] findFarm(int cx, int cz) {
            for (int rad = 0; rad < Math.max(g.gw(), g.gd()); rad++) {
                for (int dz = -rad; dz <= rad; dz++) {
                    for (int dx = -rad; dx <= rad; dx++) {
                        int fx = cx + dx;
                        int fz = cz + dz;
                        List<int[]> t = new ArrayList<>();
                        boolean ok = true;
                        for (int ez = -2; ez <= 2 && ok; ez++) {
                            for (int ex = -3; ex <= 3 && ok; ex++) {
                                // 5×3 Feld plus ein Rand, der frei bleiben muss (Laufweg)
                                if (!g.in(fx + ex, fz + ez) || g.keep(fx + ex, fz + ez) || !free(fx + ex, fz + ez)) {
                                    ok = false;
                                }
                                if (Math.abs(ez) <= 1 && Math.abs(ex) <= 2) {
                                    t.add(new int[]{fx + ex, fz + ez});
                                }
                            }
                        }
                        if (ok && claim(t.toArray(new int[0][]))) {
                            return new int[]{fx, fz};
                        }
                    }
                }
            }
            return null;
        }

        BlockState workstation(VillagerProfession prof, Direction front) {
            if (prof == VillagerProfession.FARMER) {
                return Blocks.COMPOSTER.defaultBlockState();
            }
            if (prof == VillagerProfession.LIBRARIAN) {
                return Blocks.LECTERN.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, front);
            }
            if (prof == VillagerProfession.ARMORER) {
                return Blocks.BLAST_FURNACE.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, front);
            }
            if (prof == VillagerProfession.BUTCHER) {
                return Blocks.SMOKER.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, front);
            }
            if (prof == VillagerProfession.CLERIC) {
                return Blocks.BREWING_STAND.defaultBlockState();
            }
            if (prof == VillagerProfession.LEATHERWORKER) {
                return Blocks.CAULDRON.defaultBlockState();
            }
            if (prof == VillagerProfession.MASON) {
                return Blocks.STONECUTTER.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, front);
            }
            if (prof == VillagerProfession.WEAPONSMITH) {
                return Blocks.GRINDSTONE.defaultBlockState().setValue(GrindstoneBlock.FACE, AttachFace.FLOOR)
                        .setValue(HorizontalDirectionalBlock.FACING, front);
            }
            if (prof == VillagerProfession.TOOLSMITH) {
                return Blocks.SMITHING_TABLE.defaultBlockState();
            }
            if (prof == VillagerProfession.CARTOGRAPHER) {
                return Blocks.CARTOGRAPHY_TABLE.defaultBlockState();
            }
            if (prof == VillagerProfession.FLETCHER) {
                return Blocks.FLETCHING_TABLE.defaultBlockState();
            }
            if (prof == VillagerProfession.SHEPHERD) {
                return Blocks.LOOM.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, front);
            }
            return Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP);
        }
    }
}
