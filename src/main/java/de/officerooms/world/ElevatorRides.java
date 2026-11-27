package de.officerooms.world;

import de.officerooms.block.ModBlocks;
import de.officerooms.dimension.OfficeRoomsDimension;
import de.officerooms.entity.ModEntities;
import de.officerooms.hazard.BlackoutData;
import de.officerooms.hazard.BlackoutEvents;
import de.officerooms.hazard.HazardNet;
import de.officerooms.hazard.HazardState;
import de.officerooms.megastructure.Hotel;
import de.officerooms.megastructure.Industrial;
import de.officerooms.megastructure.LevelFun;
import de.officerooms.megastructure.Maze;
import de.officerooms.megastructure.Neighbourhood;
import de.officerooms.megastructure.Poolrooms;
import de.officerooms.room.Extras;
import de.officerooms.room.OfficePlanner;
import de.officerooms.worldgen.OfficeChunkGenerator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import de.officerooms.block.ElevatorDoorBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.minecraftforge.network.PacketDistributor;

/**
 * 0.26.0: real elevator rides in the generated shafts of the Office Rooms.
 *
 * <p>Right-clicking the floor plate opens a panel where a floor is typed in. The cabin doors close, the cabin
 * travels for a few seconds and stops on a <b>random</b> floor, never the one asked for (the display claims it
 * is). If a megastructure lies within the floors passed, the doors always open inside that megastructure
 * instead. Everything standing in the cabin rides along, mobs included. After a ride the elevator does not
 * respond for five minutes.</p>
 *
 * <p>Rare disturbances: during a blackout the cabin stalls and the doors stay shut (they can be broken), a
 * creature may wait in front of the doors, and something may knock on the cabin roof.</p>
 */
public final class ElevatorRides {

    /** Ticks an elevator does not respond after a ride. */
    public static final int COOLDOWN = 5 * 60 * 20;
    private static final int CLOSE = 20;
    private static final int OPEN_DELAY = 16;
    private static final int STALL_MAX = 3 * 60 * 20;

    private static final Map<Long, Long> COOLDOWNS = new HashMap<>();
    private static final Map<Long, Ride> RIDES = new HashMap<>();

    private ElevatorRides() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ElevatorRides::onTick);
    }

    /** A generated shaft at a plate. */
    record Shaft(OfficePlanner p, Extras.Region ex, int x0, int z0, long key) {
        /** World x/z of the cabin interior corner (3 × 3). */
        int ix() {
            return x0 + 1;
        }

        int iz() {
            return z0 + 1;
        }
    }

    static Shaft shaftAt(ServerLevel level, BlockPos plate) {
        if (level.dimension() != OfficeRoomsDimension.LEVEL_KEY || !(level.getChunkSource().getGenerator() instanceof OfficeChunkGenerator gen)) {
            return null;
        }
        OfficePlanner p = gen.sampler(level.getChunkSource().randomState(), level).planner();
        int rx = p.regionOf(plate.getX());
        int rz = p.regionOf(plate.getZ());
        Extras.Region ex = p.extras.of(rx, rz);
        if (ex == null || !ex.inElevator(plate.getX() - rx * p.regionBlocks, plate.getZ() - rz * p.regionBlocks)) {
            return null;
        }
        int x0 = rx * p.regionBlocks + ex.ex0;
        int z0 = rz * p.regionBlocks + ex.ez0;
        return new Shaft(p, ex, x0, z0, BlockPos.asLong(x0, 0, z0));
    }

    /**
     * Right-click on a plate or a cabin panel. Returns false if the block is not in a generated shaft (the old
     * free-standing plate behaviour applies).
     */
    public static boolean openPanel(ServerPlayer player, BlockPos at) {
        ServerLevel level = player.serverLevel();
        Shaft sh = shaftAt(level, at);
        if (sh == null) {
            return false;
        }
        int s = sh.p().storeyOf(at.getY());
        if (s < 0 || s >= sh.ex().doors.length || at.getY() > sh.p().floorY(s) + 4) {
            return false;
        }
        BlockPos center = new BlockPos(sh.x0() + 2, sh.p().floorY(s), sh.z0() + 2);
        if (busy(level, sh, player)) {
            return true;
        }
        level.playSound(null, at, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.5F, 1.6F);
        HazardNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new HazardNet.ElevatorPanel(center, s, sh.ex().doors.length - 1));
        return true;
    }

    private static boolean busy(ServerLevel level, Shaft sh, ServerPlayer player) {
        if (RIDES.containsKey(sh.key())) {
            player.displayClientMessage(Component.literal("The elevator is in use.").withStyle(ChatFormatting.GRAY), true);
            return true;
        }
        long left = COOLDOWNS.getOrDefault(sh.key(), 0L) - level.getGameTime();
        if (left > 0) {
            long sec = (left + 19) / 20;
            player.displayClientMessage(Component.literal("The panel stays dark. (" + sec / 60 + ":" + String.format("%02d", sec % 60) + ")")
                    .withStyle(ChatFormatting.DARK_GRAY), true);
            level.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3F, 0.6F);
            return true;
        }
        return false;
    }

    /** Floor typed into the panel (from the client). */
    public static void call(ServerPlayer player, BlockPos plate, int wanted) {
        ServerLevel level = player.serverLevel();
        if (player.distanceToSqr(plate.getX() + 0.5, plate.getY() + 1, plate.getZ() + 0.5) > 25) {
            return;
        }
        Shaft sh = shaftAt(level, plate);
        if (sh == null || busy(level, sh, player)) {
            return;
        }
        int s = sh.p().storeyOf(plate.getY());
        int max = sh.ex().doors.length - 1;
        wanted = Math.max(0, Math.min(max, wanted));
        RandomSource rnd = level.random;
        int target = pickTarget(sh.ex(), s, wanted, rnd);
        if (target < 0) {
            player.displayClientMessage(Component.literal("Nothing happens.").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        Ride r = new Ride(level, sh, s, wanted, target, rnd);
        RIDES.put(sh.key(), r);
        r.close();
    }

    /** A random stop: in the direction asked for, never the floor asked for (unless it is the only other stop). */
    static int pickTarget(Extras.Region ex, int s, int wanted, RandomSource rnd) {
        int dir = wanted > s ? 1 : wanted < s ? -1 : rnd.nextBoolean() ? 1 : -1;
        for (int pass = 0; pass < 3; pass++) {
            List<Integer> c = new ArrayList<>();
            for (int t = 0; t < ex.doors.length; t++) {
                if (t == s || !ex.isStop(t)) {
                    continue;
                }
                if (pass == 0 && (Integer.signum(t - s) != dir || t == wanted)) {
                    continue;
                }
                if (pass == 1 && t == wanted) {
                    continue;
                }
                c.add(t);
            }
            if (!c.isEmpty()) {
                return c.get(rnd.nextInt(c.size()));
            }
        }
        return -1;
    }

    private static void onTick(ServerTickEvent.Post e) {
        if (RIDES.isEmpty()) {
            return;
        }
        Iterator<Ride> it = RIDES.values().iterator();
        while (it.hasNext()) {
            Ride r = it.next();
            if (r.tick()) {
                COOLDOWNS.put(r.sh.key(), r.level.getGameTime() + COOLDOWN);
                it.remove();
            }
        }
    }

    private static final class Ride {
        final ServerLevel level;
        final Shaft sh;
        final int from;
        final int wanted;
        int target;
        final RandomSource rnd;
        final int travel;
        final int knockAt;
        int t;
        boolean stalled;
        boolean arrived;
        int openAt = -1;
        BlockPos megaSpot;
        final List<BlockPos> doorsFrom = new ArrayList<>();
        final List<BlockPos> doorsTo = new ArrayList<>();
        final List<Entity> riders = new ArrayList<>();
        String megaName;

        Ride(ServerLevel level, Shaft sh, int from, int wanted, int target, RandomSource rnd) {
            this.level = level;
            this.sh = sh;
            this.from = from;
            this.wanted = wanted;
            this.target = target;
            this.rnd = rnd;
            this.travel = 80 + rnd.nextInt(121);
            this.knockAt = rnd.nextFloat() < 0.15F ? CLOSE + 20 + rnd.nextInt(Math.max(1, travel - 40)) : -1;
        }

        int floorY(int s) {
            return sh.p().floorY(s);
        }

        double cx() {
            return sh.x0() + 2.5;
        }

        double cz() {
            return sh.z0() + 2.5;
        }

        AABB cabin(int s) {
            int y = floorY(s) + 1;
            return new AABB(sh.ix(), y, sh.iz(), sh.ix() + 3, y + 4, sh.iz() + 3);
        }

        void sound(int s, SoundEvent ev, float vol, float pitch) {
            level.playSound(null, cx(), floorY(s) + 2, cz(), ev, SoundSource.BLOCKS, vol, pitch);
        }

        /** Fill every open block of the cabin shell with door blocks (at the given stage: 2 = almost open, 0 = shut). */
        void seal(int s, List<BlockPos> out, int stage) {
            int y0 = floorY(s) + 1;
            for (int dy = 0; dy < 4; dy++) {
                for (int i = 0; i < 5; i++) {
                    for (int j = 0; j < 5; j++) {
                        if (i != 0 && i != 4 && j != 0 && j != 4) {
                            continue;
                        }
                        BlockPos q = new BlockPos(sh.x0() + i, y0 + dy, sh.z0() + j);
                        BlockState st = level.getBlockState(q);
                        if (st.isAir() || st.canBeReplaced() && st.getCollisionShape(level, q).isEmpty()) {
                            Direction.Axis ax = i == 0 || i == 4 ? Direction.Axis.Z : Direction.Axis.X;
                            level.setBlock(q, ModBlocks.ELEVATOR_DOOR.get().defaultBlockState()
                                    .setValue(ElevatorDoorBlock.AXIS, ax).setValue(ElevatorDoorBlock.STAGE, stage), 3);
                            out.add(q);
                        }
                    }
                }
            }
        }

        /** Slide the leaves: stage 0..2, or -1 to remove the doors. */
        void stage(List<BlockPos> list, int stage) {
            for (BlockPos q : list) {
                BlockState st = level.getBlockState(q);
                if (st.is(ModBlocks.ELEVATOR_DOOR.get())) {
                    level.setBlock(q, stage < 0 ? Blocks.AIR.defaultBlockState() : st.setValue(ElevatorDoorBlock.STAGE, stage), 3);
                }
            }
            if (stage < 0) {
                list.clear();
            }
        }

        void unseal(List<BlockPos> list) {
            stage(list, -1);
        }

        boolean blackoutAt(int s) {
            BlackoutData d = BlackoutEvents.data(level.getServer());
            return d.phase == HazardState.ACTIVE && BlackoutEvents.inBox(d, cx(), floorY(s) + 2, cz());
        }

        void close() {
            sound(from, SoundEvents.NOTE_BLOCK_BELL.value(), 0.8F, 1.2F);
            seal(from, doorsFrom, 2);
            sound(from, SoundEvents.IRON_DOOR_CLOSE, 0.9F, 0.7F);
            sound(from, SoundEvents.PISTON_CONTRACT, 0.5F, 0.6F);
            stalled = blackoutAt(from) || blackoutAt(target);
            // a megastructure within the floors passed swallows the ride
            int lo = Math.min(from, target);
            int hi = Math.max(from, target);
            megaSpot = megaArrival(lo, hi);
        }

        void display(int floor, boolean glitch) {
            String txt;
            if (glitch) {
                String[] g = {"--", "??", "8" + rnd.nextInt(10), "-" + rnd.nextInt(10), "" + (100 + rnd.nextInt(900)), "0" + rnd.nextInt(10)};
                txt = g[rnd.nextInt(g.length)];
            } else {
                txt = Integer.toString(floor);
            }
            String arrow = wanted > from ? "▲ " : wanted < from ? "▼ " : "";
            for (Entity e : level.getEntitiesOfClass(ServerPlayer.class, cabin(arrived ? target : from).inflate(0.2))) {
                ((ServerPlayer) e).displayClientMessage(Component.literal(arrow + "Floor " + txt).withStyle(ChatFormatting.GOLD), true);
            }
        }

        void quake(int s, int ticks, float strength) {
            for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, cabin(s).inflate(0.2))) {
                HazardNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new HazardNet.Quake(ticks, strength));
            }
        }

        /** Returns true when the ride is over. */
        boolean tick() {
            t++;
            if (!arrived) {
                if (t == 4) {
                    stage(doorsFrom, 1);
                }
                if (t == 8) {
                    stage(doorsFrom, 0);
                    sound(from, SoundEvents.IRON_DOOR_CLOSE, 0.7F, 0.5F);
                }
                if (t == CLOSE) {
                    quake(from, 8, 0.35F);
                    sound(from, SoundEvents.MINECART_RIDING, 0.4F, 0.5F);
                }
                if (t > CLOSE) {
                    if ((t - CLOSE) % 30 == 0) {
                        sound(from, SoundEvents.BEACON_AMBIENT, 0.9F, 0.5F);
                    }
                    if (rnd.nextInt(45) == 0) {
                        sound(from, SoundEvents.CHAIN_STEP, 0.7F, 0.5F + rnd.nextFloat() * 0.3F);
                    }
                    if (knockAt > 0 && t >= knockAt && t <= knockAt + 20 && (t - knockAt) % 8 == 0) {
                        level.playSound(null, cx(), floorY(from) + 5, cz(), SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.HOSTILE, 0.7F, 0.5F);
                    }
                    if (t % 10 == 0) {
                        float f = Math.min(1F, (t - CLOSE) / (float) travel);
                        int shown = Math.round(from + (wanted - from) * f);
                        display(shown, rnd.nextFloat() < 0.15F);
                    }
                }
                if (t >= CLOSE + travel) {
                    arrive();
                }
                return false;
            }
            if (megaDone) {
                return true;
            }
            if (stalled) {
                boolean broken = doorsTo.stream().noneMatch(q -> level.getBlockState(q).is(ModBlocks.ELEVATOR_DOOR.get()));
                if (!blackoutAt(target) || t - openAt > STALL_MAX || broken) {
                    stalled = false;
                    openAt = t + OPEN_DELAY;
                    sound(target, SoundEvents.BEACON_ACTIVATE, 0.8F, 0.6F);
                }
                return false;
            }
            if (t == openAt) {
                sound(target, SoundEvents.NOTE_BLOCK_BELL.value(), 0.9F, 1.0F);
                sound(target, SoundEvents.IRON_DOOR_OPEN, 0.9F, 0.7F);
                stage(doorsTo, 1);
                display(wanted, false);
            } else if (t == openAt + 4) {
                stage(doorsTo, 2);
            } else if (t >= openAt + 8) {
                unseal(doorsTo);
                maybeVisitor();
                return true;
            }
            return false;
        }

        void arrive() {
            arrived = true;
            AABB box = cabin(from).inflate(0.1);
            riders.addAll(level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && !e.isSpectator()));
            if (megaSpot != null && !stalled) {
                // the doors open somewhere else entirely
                for (Entity e : riders) {
                    double ox = e.getX() - cx();
                    double oz = e.getZ() - cz();
                    if (e instanceof ServerPlayer sp) {
                        sp.teleportTo(level, megaSpot.getX() + 0.5 + ox * 0.5, megaSpot.getY(), megaSpot.getZ() + 0.5 + oz * 0.5, sp.getYRot(), sp.getXRot());
                        HazardNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new HazardNet.Quake(10, 0.4F));
                        sp.displayClientMessage(Component.literal("Floor " + wanted).withStyle(ChatFormatting.GOLD), true);
                    } else {
                        e.teleportTo(megaSpot.getX() + 0.5 + ox * 0.5, megaSpot.getY(), megaSpot.getZ() + 0.5 + oz * 0.5);
                    }
                    e.fallDistance = 0;
                }
                level.playSound(null, megaSpot, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.9F, 0.9F);
                level.playSound(null, megaSpot, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.9F, 0.6F);
                unseal(doorsFrom);
                openAt = t;
                target = from;   // nothing left to open; cooldown starts now
                stalled = false;
                arrived = true;
                megaDone = true;
                return;
            }
            level.getChunk(BlockPos.containing(cx(), floorY(target), cz()));
            seal(target, doorsTo, 0);
            int dy = floorY(target) - floorY(from);
            for (Entity e : riders) {
                if (e instanceof ServerPlayer sp) {
                    sp.teleportTo(level, e.getX(), e.getY() + dy, e.getZ(), sp.getYRot(), sp.getXRot());
                } else {
                    e.teleportTo(e.getX(), e.getY() + dy, e.getZ());
                }
                e.fallDistance = 0;
            }
            unseal(doorsFrom);
            quake(target, 10, 0.45F);
            sound(target, SoundEvents.ANVIL_LAND, 0.25F, 0.5F);
            if (stalled) {
                sound(target, SoundEvents.BEACON_DEACTIVATE, 0.9F, 0.6F);
                for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, cabin(target).inflate(0.2))) {
                    p.displayClientMessage(Component.literal("The elevator jolts to a halt. The doors won't open.").withStyle(ChatFormatting.RED), true);
                }
                openAt = t;
                return;
            }
            display(wanted, false);
            openAt = t + OPEN_DELAY;
        }

        boolean megaDone;

        /** Rarely, something is waiting in front of the doors. */
        void maybeVisitor() {
            if (megaDone || rnd.nextFloat() >= 0.08F || !de.officerooms.core.OfficeRoomsConfig.mobsAllowed(level)) {   // 0.41.2
                return;
            }
            int doors = sh.ex().doors[target];
            int[][] out = {{-3, 0}, {0, -3}, {3, 0}, {0, 3}};
            for (int side = 0; side < 4; side++) {
                if ((doors & (1 << side)) == 0) {
                    continue;
                }
                BlockPos q = BlockPos.containing(cx() + out[side][0] + (out[side][0] == 0 ? 0 : Math.signum(out[side][0])),
                        floorY(target) + 1, cz() + out[side][1] + (out[side][1] == 0 ? 0 : Math.signum(out[side][1])));
                if (!level.getBlockState(q).getCollisionShape(level, q).isEmpty() || !level.getBlockState(q.above()).getCollisionShape(level, q.above()).isEmpty()) {
                    continue;
                }
                Mob m = (rnd.nextInt(3) == 0 ? ModEntities.GAUNT.get() : ModEntities.HOUND.get()).create(level);
                if (m == null) {
                    return;
                }
                m.moveTo(q.getX() + 0.5, q.getY(), q.getZ() + 0.5, rnd.nextFloat() * 360F, 0F);
                m.finalizeSpawn(level, level.getCurrentDifficultyAt(q), MobSpawnType.EVENT, null, null);
                level.addFreshEntity(m);
                return;
            }
        }

        /** Arrival spot in the nearest megastructure whose storeys lie within [lo, hi], or null. */
        BlockPos megaArrival(int lo, int hi) {
            OfficePlanner p = sh.p();
            int rx0 = p.regionOf(sh.x0());
            int rz0 = p.regionOf(sh.z0());
            BlockPos best = null;
            double bestD = Double.MAX_VALUE;
            int radius = 1;               // only megastructures right next to the shaft ("below" it)
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int rx = rx0 + dx;
                    int rz = rz0 + dz;
                    int bx = rx * p.regionBlocks;
                    int bz = rz * p.regionBlocks;
                    BlockPos c = null;
                    int s0 = -1;
                    int n = 1;
                    String name = null;
                    Poolrooms.Site pool = p.pools.site(rx, rz);
                    if (pool != null) {
                        c = new BlockPos(bx + pool.hallX, pool.yBase + Poolrooms.FLOOR + 1, bz + pool.hallZ);
                        s0 = pool.s0;
                        n = Poolrooms.STOREYS;
                        name = "Poolrooms";
                    }
                    if (c == null) {
                        Neighbourhood.Site h = p.hood.site(rx, rz);
                        if (h != null) {
                            c = new BlockPos(bx + Neighbourhood.houseX(0) + 6, h.yBase + 1, bz + Neighbourhood.houseZ(0) - 8);
                            s0 = h.s0;
                            n = h.storeys;
                            name = "Neighbourhood";
                        }
                    }
                    if (c == null) {
                        Maze.Site m = p.maze.site(rx, rz);
                        if (m != null) {
                            Maze.Exit e = m.exits.isEmpty() ? null : m.exits.get(0);
                            c = new BlockPos(bx + (e == null ? 5 : e.west() ? 4 : e.lx()), m.yBase + 1, bz + (e == null ? 5 : e.west() ? e.lz() : 4));
                            s0 = m.s0;
                            n = Maze.STOREYS;
                            name = "Maze";
                        }
                    }
                    if (c == null) {
                        Hotel.Site h = p.hotel.site(rx, rz);
                        if (h != null) {
                            c = new BlockPos(bx + 72, h.yBase + 7, bz + 68);
                            s0 = h.s0;
                            n = h.storeys;
                            name = "Hotel";
                        }
                    }
                    if (c == null) {
                        Industrial.Site ind = p.industrial.site(rx, rz);
                        if (ind != null) {
                            int[] hc = ind.halls[0];
                            c = new BlockPos(bx + hc[0] * 16 + 4, ind.yBase + 1, bz + hc[1] * 16 + 4);
                            s0 = ind.s0;
                            n = ind.storeys;
                            name = "Industrial";
                        }
                    }
                    if (c == null) {
                        de.officerooms.megastructure.Cinema.Site cs = p.cinema.site(rx, rz);
                        if (cs != null) {
                            c = new BlockPos(bx + 80, cs.yBase + 1, bz + 80);
                            s0 = cs.s0;
                            n = de.officerooms.megastructure.Cinema.STOREYS;
                            name = "Cinema";
                        }
                    }
                    if (c == null) {
                        de.officerooms.megastructure.Mall.Site ms = p.mall.site(rx, rz);
                        if (ms != null) {
                            c = new BlockPos(bx + 80, ms.yBase + 1, bz + 100);
                            s0 = ms.s0;
                            n = de.officerooms.megastructure.Mall.STOREYS;
                            name = "Mall";
                        }
                    }
                    if (c == null) {
                        LevelFun.Site f = p.fun.site(rx, rz);
                        if (f != null) {
                            c = new BlockPos(bx + 80, f.yBase + 1, bz + 80);
                            s0 = f.s0;
                            n = f.storeys;
                            name = "Level Fun";
                        }
                    }
                    if (c == null || s0 + n - 1 < lo || s0 > hi) {
                        continue;
                    }
                    double d = Math.pow(c.getX() - cx(), 2) + Math.pow(c.getZ() - cz(), 2);
                    if (d < bestD) {
                        bestD = d;
                        best = c;
                        megaName = name;
                    }
                }
            }
            return best == null ? null : safeSpot(best);
        }

        BlockPos safeSpot(BlockPos c) {
            level.getChunk(c);
            for (int r = 0; r <= 8; r++) {
                for (int dy : new int[]{0, 1, -1, 2}) {
                    for (int dx = -r; dx <= r; dx++) {
                        for (int dz = -r; dz <= r; dz++) {
                            if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                                continue;
                            }
                            BlockPos q = c.offset(dx, dy, dz);
                            if (level.getBlockState(q).getCollisionShape(level, q).isEmpty()
                                    && level.getBlockState(q.above()).getCollisionShape(level, q.above()).isEmpty()
                                    && !level.getBlockState(q.below()).getCollisionShape(level, q.below()).isEmpty()
                                    && level.getFluidState(q).isEmpty()) {
                                return q;
                            }
                        }
                    }
                }
            }
            return c;
        }
    }
}
