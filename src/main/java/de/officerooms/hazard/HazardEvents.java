package de.officerooms.hazard;

import java.util.ArrayList;
import java.util.List;

import de.officerooms.core.OfficeRoomsConfig;
import de.officerooms.core.OfficeRoomsIds;
import de.officerooms.dimension.OfficeRoomsDimension;
import de.officerooms.entity.ModEntities;
import de.officerooms.item.HazardHeadgear;
import de.officerooms.item.ModItems;
import de.officerooms.megastructure.Megastructures;
import de.officerooms.room.OfficePlanner;
import de.officerooms.worldgen.OfficeChunkGenerator;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 0.17.0: HAZARD ZONE, a recurring world event. Every cycle (default 100 minutes) a toxic zone is announced on the
 * monitors 15 minutes ahead, stays active for 45 minutes inside a random megastructure near the players, then
 * disappears; the remaining 40 minutes are a cooldown. The schedule lives in {@link HazardData} (overworld game time),
 * so it survives restarts and is the same for every player; clients get it through {@link HazardNet}.
 */
public final class HazardEvents {

    public static final String MOB_TAG = OfficeRoomsIds.MOD_ID + ".hazard";
    private static final String TOX = "office_rooms_tox";

    private HazardEvents() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(HazardEvents.class);
    }

    public static HazardData data(MinecraftServer srv) {
        return srv.overworld().getDataStorage().computeIfAbsent(HazardData::load, HazardData::new, "office_rooms_hazard");
    }

    private static long now(MinecraftServer srv) {
        return srv.overworld().getGameTime();
    }

    private static int minutes(int m) {
        return m * 1200;
    }

    public static HazardNet.Sync sync(MinecraftServer srv) {
        HazardData d = data(srv);
        int left = d.phase == HazardState.IDLE ? (int) Math.max(0, (d.nextCycle - now(srv)) / 20) : (int) Math.max(0, (d.phaseEnd - now(srv)) / 20);
        return new HazardNet.Sync(d.phase, left, d.box, d.name);
    }

    // --- schedule -----------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !OfficeRoomsConfig.HAZARD_ENABLED.get()) {
            return;
        }
        MinecraftServer srv = e.getServer();
        ServerLevel office = srv.getLevel(OfficeRoomsDimension.LEVEL_KEY);
        long t = now(srv);
        if (office == null || t % 20 != 0) {
            return;
        }
        HazardData d = data(srv);
        if (d.nextCycle == 0) {
            d.nextCycle = t + minutes(OfficeRoomsConfig.HAZARD_FIRST_DELAY.get());
            d.setDirty();
        }
        if (d.phase == HazardState.IDLE && t >= d.nextCycle) {
            startWarning(srv, office, d, t);
        } else if (d.phase == HazardState.WARNING && t >= d.phaseEnd) {
            activate(srv, office, d, t);
        } else if (d.phase == HazardState.ACTIVE && t >= d.phaseEnd) {
            end(srv, office, d);
        }
        if (t % 100 == 0) {
            HazardNet.sendAll(sync(srv));
        }
        chests(office, d);
        for (ServerPlayer p : office.players()) {
            playerTick(p, d, t);
        }
        if (d.phase == HazardState.ACTIVE && t % 60 == 0) {
            spawnCreatures(office, d);
        }
    }

    /** Starts the warning phase (also used by the command). Returns false if no megastructure is near. */
    public static boolean startWarning(MinecraftServer srv, ServerLevel office, HazardData d, long t) {
        d.nextCycle = t + minutes(OfficeRoomsConfig.HAZARD_PERIOD.get());
        if (!chooseZone(office, d)) {
            d.setDirty();
            return false;
        }
        d.phase = HazardState.WARNING;
        d.phaseEnd = t + minutes(OfficeRoomsConfig.HAZARD_WARNING.get());
        d.setDirty();
        HazardNet.sendAll(sync(srv));
        return true;
    }

    public static void activate(MinecraftServer srv, ServerLevel office, HazardData d, long t) {
        d.phase = HazardState.ACTIVE;
        d.phaseEnd = t + minutes(OfficeRoomsConfig.HAZARD_ACTIVE.get());
        d.pending.clear();
        RandomSource r = RandomSource.create(t);
        int cx = (d.box[0] + d.box[3]) / 2;
        int cz = (d.box[2] + d.box[5]) / 2;
        int rad = (d.box[3] - d.box[0]) / 2;
        for (int i = 0; i < OfficeRoomsConfig.HAZARD_CHESTS.get(); i++) {
            int x = d.box[0] + r.nextInt(d.box[3] - d.box[0] + 1);
            int z = d.box[2] + r.nextInt(d.box[5] - d.box[2] + 1);
            boolean core = Math.max(Math.abs(x - cx), Math.abs(z - cz)) < rad * 0.4;
            d.pending.add(new int[]{x, z, core ? 1 : 0});
        }
        d.setDirty();
        HazardNet.sendAll(sync(srv));
        for (ServerPlayer p : office.players()) {
            if (inZone(d, p)) {
                p.playNotifySound(SoundEvents.FIRE_EXTINGUISH, SoundSource.AMBIENT, 1.0F, 0.5F);
            }
        }
    }

    public static void end(MinecraftServer srv, ServerLevel office, HazardData d) {
        d.phase = HazardState.IDLE;
        d.toRemove.addAll(d.placed);
        d.placed.clear();
        d.pending.clear();
        AABB box = new AABB(d.box[0] - 32, d.box[1] - 8, d.box[2] - 32, d.box[3] + 32, d.box[4] + 8, d.box[5] + 32);
        for (Entity en : office.getEntities((Entity) null, box, en -> en.getTags().contains(MOB_TAG))) {
            en.discard();
        }
        d.setDirty();
        HazardNet.sendAll(sync(srv));
    }

    /** Picks a random megastructure near a random player in the dimension (or near the origin). */
    static boolean chooseZone(ServerLevel office, HazardData d) {
        if (!(office.getChunkSource().getGenerator() instanceof OfficeChunkGenerator gen)) {
            return false;
        }
        OfficePlanner p = gen.sampler(office.getChunkSource().randomState(), office).planner();
        List<ServerPlayer> ps = office.players();
        RandomSource r = office.getRandom();
        int ax = ps.isEmpty() ? 0 : ps.get(r.nextInt(ps.size())).getBlockX();
        int az = ps.isEmpty() ? 0 : ps.get(r.nextInt(ps.size())).getBlockZ();
        List<int[]> found = new ArrayList<>();
        int rx0 = p.regionOf(ax);
        int rz0 = p.regionOf(az);
        int rad = OfficeRoomsConfig.HAZARD_SEARCH_REGIONS.get();
        for (int dx = -rad; dx <= rad; dx++) {
            for (int dz = -rad; dz <= rad; dz++) {
                int[] band = band(p, rx0 + dx, rz0 + dz);
                if (band != null && !occupied(p, ps, rx0 + dx, rz0 + dz, band)) {
                    found.add(new int[]{rx0 + dx, rz0 + dz, band[0], band[1], band[2]});
                }
            }
        }
        if (found.isEmpty()) {
            return false;
        }
        int[] f = found.get(r.nextInt(found.size()));
        int bx = f[0] * p.regionBlocks;
        int bz = f[1] * p.regionBlocks;
        int rz = OfficeRoomsConfig.HAZARD_RADIUS.get();
        int ox = r.nextInt(31) - 15;
        int oz = r.nextInt(31) - 15;
        int yb = p.floorY(f[2]);
        d.box = new int[]{Math.max(bx + 3, bx + 80 + ox - rz), yb, Math.max(bz + 3, bz + 80 + oz - rz),
                Math.min(bx + 156, bx + 80 + ox + rz), yb + f[3] * 6 - 1, Math.min(bz + 156, bz + 80 + oz + rz)};
        d.name = f[4] < NAMES.length ? NAMES[f[4]] : "Megastructure";   // 0.37.1: never fail on a kind without a name
        return true;
    }

    /** Never pick the megastructure a player is in right now (or just above/below it): they would be gassed without warning. */
    static boolean occupied(OfficePlanner p, List<ServerPlayer> ps, int rx, int rz, int[] band) {
        int y0 = p.floorY(band[0]) - 12;
        int y1 = p.floorY(band[0]) + band[1] * 6 + 12;
        for (ServerPlayer pl : ps) {
            if (p.regionOf(pl.getBlockX()) == rx && p.regionOf(pl.getBlockZ()) == rz && pl.getBlockY() >= y0 && pl.getBlockY() <= y1) {
                return true;
            }
        }
        return false;
    }

    private static final String[] NAMES = {"", "Poolrooms", "Neighbourhood", "The Maze", "Abandoned Hotel", "Industrial Complex", "Level Fun", "Abandoned Cinema"};

    /** {s0, storeys, kind ordinal} of the megastructure in a region, or null. */
    static int[] band(OfficePlanner p, int rx, int rz) {
        Megastructures.Kind k = Megastructures.kindOf(p, rx, rz);
        int s0;
        int n;
        switch (k) {
            case POOLROOMS -> {
                var s = p.pools.site(rx, rz);
                if (s == null) {
                    return null;
                }
                s0 = s.s0;
                n = de.officerooms.megastructure.Poolrooms.STOREYS;
            }
            case NEIGHBOURHOOD -> {
                var s = p.hood.site(rx, rz);
                if (s == null) {
                    return null;
                }
                s0 = s.s0;
                n = s.storeys;
            }
            case MAZE -> {
                var s = p.maze.site(rx, rz);
                if (s == null) {
                    return null;
                }
                s0 = s.s0;
                n = de.officerooms.megastructure.Maze.STOREYS;
            }
            case HOTEL -> {
                var s = p.hotel.site(rx, rz);
                if (s == null) {
                    return null;
                }
                s0 = s.s0;
                n = s.storeys;
            }
            case INDUSTRIAL -> {
                var s = p.industrial.site(rx, rz);
                if (s == null) {
                    return null;
                }
                s0 = s.s0;
                n = s.storeys;
            }
            case CINEMA -> {
                var s = p.cinema.site(rx, rz);
                if (s == null) {
                    return null;
                }
                s0 = s.s0;
                n = de.officerooms.megastructure.Cinema.STOREYS;
            }
            case LEVEL_FUN -> {
                var s = p.fun.site(rx, rz);
                if (s == null) {
                    return null;
                }
                s0 = s.s0;
                n = s.storeys;
            }
            default -> {
                return null;
            }
        }
        return new int[]{s0, n, k.ordinal()};
    }

    public static boolean inZone(HazardData d, Entity e) {
        return d.phase == HazardState.ACTIVE && e.level().dimension() == OfficeRoomsDimension.LEVEL_KEY
                && e.getX() >= d.box[0] && e.getX() < d.box[3] + 1 && e.getY() >= d.box[1] && e.getY() < d.box[4] + 1
                && e.getZ() >= d.box[2] && e.getZ() < d.box[5] + 1;
    }

    // --- players: toxic gas, gear, detector ---------------------------------------------------------------

    private static void playerTick(ServerPlayer p, HazardData d, long t) {
        if (p.isSpectator()) {
            return;
        }
        ItemStack head = p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        HazardHeadgear gear = head.getItem() instanceof HazardHeadgear h ? h : null;
        // night vision from goggles or the visor
        if (gear != null && gear.nightVision && HazardHeadgear.charge(head) > 0) {
            HazardHeadgear.useCharge(head);
            p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false, true));
            if (HazardHeadgear.charge(head) == 0) {
                p.displayClientMessage(Component.literal("Goggles: battery empty. Use a Portable Battery to recharge.").withStyle(ChatFormatting.GRAY), true);
            }
        }
        int tox = p.getPersistentData().getInt(TOX);
        if (p.isCreative()) {
            tox = 0;
        } else if (inZone(d, p)) {
            boolean protectedNow = gear != null && gear.gasMask && HazardHeadgear.filter(head) > 0;
            if (protectedNow) {
                HazardHeadgear.useFilter(head);
                int pct = HazardHeadgear.filter(head) * 100 / HazardHeadgear.MAX_FILTER;
                if (t % 60 == 0 || pct < 15) {
                    p.displayClientMessage(Component.literal("☣ Gas mask filter: " + pct + "%").withStyle(pct < 15 ? ChatFormatting.RED : ChatFormatting.GREEN), true);
                }
                tox = Math.max(0, tox - 1);
            } else {
                tox = Math.min(100, tox + 2);
                p.displayClientMessage(Component.literal("☣ TOXIC GAS – exposure " + tox + "%  (wear a gas mask!)").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD), true);
                if (tox >= 20) {
                    p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0, true, false, true));
                }
                if (tox >= 40) {
                    p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, true, false, true));
                    p.hurt(p.damageSources().magic(), tox >= 70 ? 3.0F : 1.5F);
                }
                if (t % 80 == 0) {
                    p.playNotifySound(SoundEvents.PLAYER_HURT_DROWN, SoundSource.PLAYERS, 0.6F, 0.6F);
                }
            }
        } else if (tox > 0) {
            tox = Math.max(0, tox - 3);
            if (tox == 0) {
                p.displayClientMessage(Component.literal("You can breathe again.").withStyle(ChatFormatting.GRAY), true);
            }
        } else if (d.phase != HazardState.IDLE && (p.getMainHandItem().is(ModItems.EVENT_DETECTOR.get()) || p.getOffhandItem().is(ModItems.EVENT_DETECTOR.get()))) {
            detector(p, d, t);
        }
        if (tox > 0 || p.getPersistentData().contains(TOX)) {
            p.getPersistentData().putInt(TOX, tox);
        }
    }

    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

    private static void detector(ServerPlayer p, HazardData d, long t) {
        double cx = (d.box[0] + d.box[3] + 1) / 2.0;
        double cz = (d.box[2] + d.box[5] + 1) / 2.0;
        double dx = cx - p.getX();
        double dz = cz - p.getZ();
        int dist = (int) Math.sqrt(dx * dx + dz * dz);
        float target = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;   // yaw that faces the zone
        float diff = Mth.wrapDegrees(target - p.getYRot());
        String arrow = ARROWS[Math.floorMod(Math.round(diff / 45.0F), 8)];
        int y = p.getBlockY();
        String vert = y < d.box[1] ? (d.box[1] - y) + " below the zone" : y > d.box[4] ? (y - d.box[4]) + " above the zone" : "level with the zone";
        boolean active = d.phase == HazardState.ACTIVE;
        int left = (int) Math.max(0, (d.phaseEnd - t) / 20);
        String head = active ? "HAZARD ZONE ACTIVE" : "HAZARD ZONE IN " + HazardState.clock(left);
        p.displayClientMessage(Component.literal(arrow + "  " + head + " – " + d.name + "  " + dist + " blocks, " + vert)
                .withStyle(active ? ChatFormatting.RED : ChatFormatting.GOLD), true);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp && sp.getServer() != null) {
            HazardNet.send(sp, sync(sp.getServer()));
        }
    }

    @SubscribeEvent
    public static void onChangeDim(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp && sp.getServer() != null) {
            sp.getPersistentData().remove(TOX);
            HazardNet.send(sp, sync(sp.getServer()));
        }
    }

    @SubscribeEvent
    public static void onJoin(net.minecraftforge.event.entity.EntityJoinLevelEvent e) {
        if (!e.getLevel().isClientSide && e.getEntity().getTags().contains(MOB_TAG) && e.getLevel().getServer() != null
                && data(e.getLevel().getServer()).phase != HazardState.ACTIVE) {
            e.setCanceled(true);
        }
    }

    // --- chests -------------------------------------------------------------------------------------------

    private static void chests(ServerLevel office, HazardData d) {
        boolean dirty = false;
        for (var it = d.toRemove.iterator(); it.hasNext(); ) {
            BlockPos pos = BlockPos.of(it.next());
            if (!office.hasChunkAt(pos)) {
                continue;
            }
            BlockEntity be = office.getBlockEntity(pos);
            if (be instanceof Container c) {
                c.clearContent();
            }
            if (office.getBlockState(pos).is(Blocks.CHEST)) {
                office.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
            it.remove();
            dirty = true;
        }
        if (d.phase == HazardState.ACTIVE) {
            for (var it = d.pending.iterator(); it.hasNext(); ) {
                int[] c = it.next();
                if (!office.hasChunkAt(new BlockPos(c[0], d.box[1], c[1])) || office.getNearestPlayer(c[0], d.box[1], c[1], 96, false) == null) {
                    continue;
                }
                List<Integer> floors = new ArrayList<>();
                for (int y = d.box[1] + 1; y < d.box[4]; y++) {
                    BlockPos q = new BlockPos(c[0], y, c[1]);
                    BlockState below = office.getBlockState(q.below());
                    if (office.getBlockState(q).isAir() && office.getBlockState(q.above()).isAir() && below.isFaceSturdy(office, q.below(), Direction.UP)) {
                        floors.add(y);
                    }
                }
                it.remove();
                dirty = true;
                if (floors.isEmpty()) {
                    continue;
                }
                BlockPos q = new BlockPos(c[0], floors.get(office.getRandom().nextInt(floors.size())), c[1]);
                office.setBlock(q, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(office.getRandom())), 3);
                if (office.getBlockEntity(q) instanceof RandomizableContainerBlockEntity be) {
                    be.setLootTable(new ResourceLocation(OfficeRoomsIds.MOD_ID, c[2] == 1 ? "chests/hazard_core" : "chests/hazard_cache"), office.getRandom().nextLong());
                }
                d.placed.add(q.asLong());
            }
        }
        if (dirty) {
            d.setDirty();
        }
    }

    // --- creatures ----------------------------------------------------------------------------------------

    private static void spawnCreatures(ServerLevel office, HazardData d) {
        if (!de.officerooms.core.OfficeRoomsConfig.mobsAllowed(office)) {   // 0.41.2
            return;
        }
        AABB zone = new AABB(d.box[0], d.box[1], d.box[2], d.box[3] + 1, d.box[4] + 1, d.box[5] + 1);
        for (ServerPlayer p : office.players()) {
            if (!inZone(d, p) || p.isSpectator() || office.getRandom().nextFloat() > 0.35F) {
                continue;
            }
            double u = office.getRandom().nextDouble();
            EntityType<? extends Mob> type = u < 0.15 ? ModEntities.FLAYED.get() : u < 0.30 ? ModEntities.COLONY.get()
                    : u < 0.55 ? ModEntities.HEADLESS_WARDEN.get() : ModEntities.HOUND.get();
            int cap = type == ModEntities.HOUND.get() ? 4 : type == ModEntities.HEADLESS_WARDEN.get() ? 2 : 1;
            if (office.getEntities(type, zone, en -> en.isAlive()).size() >= cap) {
                continue;
            }
            for (int attempt = 0; attempt < 12; attempt++) {
                double a = office.getRandom().nextDouble() * Math.PI * 2;
                double r = 16 + office.getRandom().nextDouble() * 20;
                int x = (int) Math.floor(p.getX() + Math.cos(a) * r);
                int z = (int) Math.floor(p.getZ() + Math.sin(a) * r);
                int y = p.getBlockY();
                BlockPos q = new BlockPos(x, y, z);
                if (!zone.contains(x + 0.5, y + 0.5, z + 0.5) || !office.isPositionEntityTicking(q)
                        || !office.getBlockState(q.below()).isSolid() || !office.noCollision(type.getAABB(x + 0.5, y, z + 0.5))) {
                    continue;
                }
                Mob m = type.create(office);
                if (m == null) {
                    break;
                }
                m.moveTo(x + 0.5, y, z + 0.5, office.getRandom().nextFloat() * 360F, 0F);
                m.finalizeSpawn(office, office.getCurrentDifficultyAt(q), MobSpawnType.EVENT, null, null);
                m.addTag(MOB_TAG);
                office.addFreshEntity(m);
                break;
            }
        }
    }

    public static boolean playerProtected(Player p) {
        ItemStack head = p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        return head.getItem() instanceof HazardHeadgear h && h.gasMask && HazardHeadgear.filter(head) > 0;
    }
}
