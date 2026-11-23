package de.officerooms.hazard;

import de.officerooms.OfficeRooms;
import de.officerooms.block.FuseBoxBlock;
import de.officerooms.block.ModBlocks;
import de.officerooms.core.ModSounds;
import de.officerooms.core.OfficeRoomsConfig;
import de.officerooms.dimension.OfficeRoomsDimension;
import de.officerooms.entity.ModEntities;
import de.officerooms.room.OfficePlanner;
import de.officerooms.worldgen.OfficeChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 0.22.0: BLACKOUT. Every now and then the power grid fails in an area around a player (about 100 × 100 blocks,
 * several storeys). The monitors announce it minutes before. Inside, the lights die (client-side lightmap), the hum
 * stops, the monitors go dark and Gaunts come up from the dark storeys. A sparking fuse box somewhere in the area
 * can be repaired with Electronic Components; otherwise the backup power comes on after a while.
 */
public final class BlackoutEvents {

    public static final String TAG = "office_rooms.blackout";

    private BlackoutEvents() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(BlackoutEvents.class);
    }

    public static BlackoutData data(MinecraftServer srv) {
        return srv.overworld().getDataStorage().computeIfAbsent(BlackoutData::load, BlackoutData::new, "office_rooms_blackout");
    }

    static OfficePlanner planner(ServerLevel l) {
        return l.getChunkSource().getGenerator() instanceof OfficeChunkGenerator gen ? gen.sampler(l.getChunkSource().randomState(), l).planner() : null;
    }

    public static HazardNet.Blackout sync(MinecraftServer srv) {
        BlackoutData d = data(srv);
        long now = srv.overworld().getGameTime();
        int left = d.phase == HazardState.IDLE ? 0 : (int) Math.max(0, (d.phaseEnd - now) / 20);
        return new HazardNet.Blackout(d.phase, left, d.box, d.name, d.fuseFloor);
    }

    static void sendAll(MinecraftServer srv) {
        HazardNet.CHANNEL.send(net.minecraftforge.network.PacketDistributor.ALL.noArg(), sync(srv));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp && sp.getServer() != null) {
            HazardNet.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp), sync(sp.getServer()));
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.getServer().getTickCount() % 20 != 7 || !OfficeRoomsConfig.BLACKOUT_ENABLED.get()) {
            return;
        }
        MinecraftServer srv = e.getServer();
        ServerLevel l = srv.getLevel(OfficeRoomsDimension.LEVEL_KEY);
        if (l == null) {
            return;
        }
        BlackoutData d = data(srv);
        long now = srv.overworld().getGameTime();
        if (d.nextCycle == 0) {
            d.nextCycle = now + OfficeRoomsConfig.BLACKOUT_FIRST_DELAY.get() * 1200L;
            d.setDirty();
        }
        switch (d.phase) {
            case HazardState.IDLE -> {
                if (now >= d.nextCycle) {
                    warn(srv, l, d, null);
                }
            }
            case HazardState.WARNING -> {
                if (now >= d.phaseEnd) {
                    start(srv, l, d);
                }
            }
            case HazardState.ACTIVE -> {
                if (now >= d.phaseEnd) {
                    end(srv, l, d, null);
                } else {
                    gaunts(l, d);
                }
            }
            default -> d.phase = HazardState.IDLE;
        }
        if (now % 100 == 7) {
            sendAll(srv);
        }
    }

    /** Announces a blackout around a random player in the Office Rooms (or around {@code at}). */
    public static boolean warn(MinecraftServer srv, ServerLevel l, BlackoutData d, ServerPlayer at) {
        List<ServerPlayer> ps = l.players().stream().filter(p -> !p.isSpectator()).toList();
        OfficePlanner p = planner(l);
        long now = srv.overworld().getGameTime();
        if (p == null || (ps.isEmpty() && at == null)) {
            d.nextCycle = now + 1200L;                                      // try again in a minute
            d.setDirty();
            return false;
        }
        ServerPlayer c = at != null ? at : ps.get(l.random.nextInt(ps.size()));
        int r = OfficeRoomsConfig.BLACKOUT_RADIUS.get(), st = OfficeRoomsConfig.BLACKOUT_STOREYS.get();
        int s = Math.floorDiv(c.getBlockY() - p.y0, p.phys);
        int s0 = Math.max(0, s - st), s1 = Math.min(p.storeys - 1, s + st);
        d.box = new int[]{c.getBlockX() - r, p.y0 + p.phys * s0, c.getBlockZ() - r, c.getBlockX() + r, p.y0 + p.phys * (s1 + 1) - 1, c.getBlockZ() + r};
        d.name = "SECTOR " + (char) ('A' + l.random.nextInt(8)) + "-" + (1 + l.random.nextInt(9)) + ", FLOORS " + (s0 + 1) + "-" + (s1 + 1);
        d.phase = HazardState.WARNING;
        d.phaseEnd = now + OfficeRoomsConfig.BLACKOUT_WARNING.get() * 1200L;
        d.fuse = 0;
        d.fuseFloor = 0;
        d.setDirty();
        sendAll(srv);
        OfficeRooms.LOGGER.info("Blackout announced: {}", d.name);
        return true;
    }

    // ---------------------------------------------------------------------------------------------
    // 0.25.0: the lights really go out (server side: also dark with shader packs)
    // ---------------------------------------------------------------------------------------------

    private static boolean lit(net.minecraft.world.level.block.state.BlockState s) {
        return s.getBlock() instanceof de.officerooms.block.CeilingLightBlock && s.getValue(de.officerooms.block.CeilingLightBlock.LIT);
    }

    /** Switches off every lit ceiling light of one loaded chunk inside the box. */
    private static void darkenChunk(ServerLevel l, BlackoutData d, net.minecraft.world.level.chunk.LevelChunk c) {
        int[] b = d.box;
        int x0 = Math.max(b[0], c.getPos().getMinBlockX()), x1 = Math.min(b[3], c.getPos().getMaxBlockX());
        int z0 = Math.max(b[2], c.getPos().getMinBlockZ()), z1 = Math.min(b[5], c.getPos().getMaxBlockZ());
        if (x0 > x1 || z0 > z1) {
            return;
        }
        var sections = c.getSections();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int si = 0; si < sections.length; si++) {
            var sec = sections[si];
            int sy = c.getSectionYFromSectionIndex(si) << 4;
            if (sec.hasOnlyAir() || sy + 15 < b[1] || sy > b[4] || !sec.maybeHas(BlackoutEvents::lit)) {
                continue;
            }
            for (int y = Math.max(sy, b[1]); y <= Math.min(sy + 15, b[4]); y++) {
                for (int z = z0; z <= z1; z++) {
                    for (int x = x0; x <= x1; x++) {
                        var s = sec.getBlockState(x & 15, y & 15, z & 15);
                        if (lit(s)) {
                            m.set(x, y, z);
                            l.setBlock(m, s.setValue(de.officerooms.block.CeilingLightBlock.LIT, false), 2);
                            d.off.add(m.asLong());
                        }
                    }
                }
            }
        }
    }

    /** Works through the chunks of the box a few per second (spreads the light updates). */
    private static void darken(ServerLevel l, BlackoutData d) {
        int[] b = d.box;
        int budget = 6;
        for (int cz = b[2] >> 4; cz <= b[5] >> 4 && budget > 0; cz++) {
            for (int cx = b[0] >> 4; cx <= b[3] >> 4 && budget > 0; cx++) {
                long key = net.minecraft.world.level.ChunkPos.asLong(cx, cz);
                if (d.doneChunks.contains(key)) {
                    continue;
                }
                var c = l.getChunkSource().getChunkNow(cx, cz);
                if (c == null) {
                    continue;                                               // switched off when it loads
                }
                darkenChunk(l, d, c);
                d.doneChunks.add(key);
                budget--;
                d.setDirty();
            }
        }
    }

    /** Lights back on (loaded chunks now, the rest when they load). */
    private static void relight(ServerLevel l, BlackoutData d) {
        d.restore.addAll(d.off);
        d.off.clear();
        d.doneChunks.clear();
        restoreLoaded(l, d, 4000);
    }

    private static void restoreLoaded(ServerLevel l, BlackoutData d, int budget) {
        var it = d.restore.iterator();
        while (it.hasNext() && budget-- > 0) {
            BlockPos p = BlockPos.of(it.nextLong());
            if (l.getChunkSource().getChunkNow(p.getX() >> 4, p.getZ() >> 4) == null) {
                continue;
            }
            var s = l.getBlockState(p);
            if (s.getBlock() instanceof de.officerooms.block.CeilingLightBlock && !s.getValue(de.officerooms.block.CeilingLightBlock.LIT)) {
                l.setBlock(p, s.setValue(de.officerooms.block.CeilingLightBlock.LIT, true), 2);
            }
            it.remove();
        }
        d.setDirty();
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.level instanceof ServerLevel l) || l.dimension() != OfficeRoomsDimension.LEVEL_KEY
                || l.getGameTime() % 5 != 2) {
            return;
        }
        BlackoutData d = data(l.getServer());
        if (d.phase == HazardState.ACTIVE) {
            darken(l, d);
        } else if (!d.restore.isEmpty() && l.getGameTime() % 20 == 2) {
            restoreLoaded(l, d, 2000);
        }
    }

    /** The lights go out. */
    public static void start(MinecraftServer srv, ServerLevel l, BlackoutData d) {
        long now = srv.overworld().getGameTime();
        d.phase = HazardState.ACTIVE;
        d.phaseEnd = now + OfficeRoomsConfig.BLACKOUT_ACTIVE.get() * 1200L;
        BlockPos fuse = placeFuseBox(l, d);
        d.fuse = fuse == null ? 0 : fuse.asLong();
        OfficePlanner p = planner(l);
        d.fuseFloor = fuse == null || p == null ? 0 : Math.floorDiv(fuse.getY() - p.y0, p.phys) + 1;
        d.doneChunks.clear();
        d.off.clear();
        d.setDirty();
        sendAll(srv);
        for (ServerPlayer pl : inside(l, d)) {
            l.playSound(null, pl.blockPosition(), ModSounds.OUTPOST_POWER_OFF.get(), SoundSource.AMBIENT, 2.0F, 0.8F);
            pl.displayClientMessage(Component.literal(d.fuseFloor > 0 ? "The power is out. Somewhere on floor " + d.fuseFloor + " a fuse box is sparking."
                    : "The power is out."), true);
        }
    }

    /** Power comes back: repaired ({@code by} != null) or backup power. */
    public static void end(MinecraftServer srv, ServerLevel l, BlackoutData d, ServerPlayer by) {
        for (ServerPlayer pl : inside(l, d)) {
            l.playSound(null, pl.blockPosition(), ModSounds.OUTPOST_POWER_ON.get(), SoundSource.AMBIENT, 2.0F, 1.0F);
            pl.displayClientMessage(Component.literal(by != null ? "Power restored by " + by.getGameProfile().getName() + "." : "Backup power. The hum returns."), true);
        }
        List<Entity> gone = new ArrayList<>();
        for (Entity en : l.getAllEntities()) {
            if (en.getTags().contains(TAG)) {
                gone.add(en);
            }
        }
        gone.forEach(Entity::discard);                                      // they go back down into the dark
        relight(l, d);
        if (d.fuse != 0) {
            BlockPos fp = BlockPos.of(d.fuse);
            if (l.isLoaded(fp) && l.getBlockState(fp).getBlock() instanceof FuseBoxBlock) {
                l.setBlock(fp, l.getBlockState(fp).setValue(FuseBoxBlock.BROKEN, false), 3);
            }
        }
        d.phase = HazardState.IDLE;
        d.nextCycle = srv.overworld().getGameTime() + OfficeRoomsConfig.BLACKOUT_PERIOD.get() * 1200L;
        d.fuse = 0;
        d.setDirty();
        sendAll(srv);
    }

    /** Called by the fuse box when it is repaired; true if that ended a blackout. */
    public static boolean repaired(ServerLevel l, BlockPos pos, ServerPlayer by) {
        BlackoutData d = data(l.getServer());
        if (d.phase == HazardState.ACTIVE && (d.fuse == 0 || d.fuse == pos.asLong() || inBox(d, pos.getX(), pos.getY(), pos.getZ()))) {
            end(l.getServer(), l, d, by);
            return true;
        }
        return false;
    }

    public static boolean inBox(BlackoutData d, double x, double y, double z) {
        int[] b = d.box;
        return x >= b[0] && x <= b[3] + 1 && y >= b[1] && y <= b[4] + 1 && z >= b[2] && z <= b[5] + 1;
    }

    static List<ServerPlayer> inside(ServerLevel l, BlackoutData d) {
        return l.players().stream().filter(p -> inBox(d, p.getX(), p.getY(), p.getZ())).toList();
    }

    /** A wall spot at eye level in a loaded room of the area. */
    private static BlockPos placeFuseBox(ServerLevel l, BlackoutData d) {
        OfficePlanner p = planner(l);
        if (p == null) {
            return null;
        }
        int[] b = d.box;
        int s0 = Math.floorDiv(b[1] - p.y0, p.phys), s1 = Math.floorDiv(b[4] - p.y0, p.phys);
        for (int a = 0; a < 600; a++) {
            int s = s0 + l.random.nextInt(s1 - s0 + 1);
            BlockPos at = new BlockPos(b[0] + l.random.nextInt(b[3] - b[0] + 1), p.y0 + p.phys * s + 3, b[2] + l.random.nextInt(b[5] - b[2] + 1));
            if (!l.isLoaded(at) || !l.getBlockState(at).isAir() || !l.getBlockState(at.below()).isAir()
                    || !l.getBlockState(at.below(2)).isFaceSturdy(l, at.below(2), Direction.UP)) {
                continue;
            }
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos w = at.relative(dir);
                if (l.getBlockState(w).isFaceSturdy(l, w, dir.getOpposite())) {
                    l.setBlock(at, ModBlocks.FUSE_BOX.get().defaultBlockState().setValue(FuseBoxBlock.FACING, dir.getOpposite()).setValue(FuseBoxBlock.BROKEN, true), 3);
                    return at;
                }
            }
        }
        return null;
    }

    /** Gaunts come up out of the dark (out of sight, 14–26 blocks away, at most N at a time). */
    private static void gaunts(ServerLevel l, BlackoutData d) {
        if (l.random.nextFloat() > 0.08F || !de.officerooms.core.OfficeRoomsConfig.mobsAllowed(l)) {   // 0.41.2
            return;
        }
        int count = 0;
        for (Entity en : l.getAllEntities()) {
            if (en.getTags().contains(TAG)) {
                count++;
            }
        }
        List<ServerPlayer> ps = inside(l, d).stream().filter(p -> !p.isSpectator() && !p.isCreative()).toList();
        if (count >= OfficeRoomsConfig.BLACKOUT_GAUNTS.get() || ps.isEmpty()) {
            return;
        }
        ServerPlayer pl = ps.get(l.random.nextInt(ps.size()));
        for (int a = 0; a < 30; a++) {
            double ang = l.random.nextDouble() * Math.PI * 2, r = 14 + l.random.nextDouble() * 12;
            BlockPos at = BlockPos.containing(pl.getX() + Math.cos(ang) * r, pl.getY(), pl.getZ() + Math.sin(ang) * r);
            if (!inBox(d, at.getX(), at.getY(), at.getZ()) || !l.isLoaded(at) || !l.getBlockState(at).isAir() || !l.getBlockState(at.above()).isAir()
                    || !l.getBlockState(at.above(2)).isAir() || !l.getBlockState(at.below()).isFaceSturdy(l, at.below(), Direction.UP)) {
                continue;
            }
            Monster g = ModEntities.GAUNT.get().create(l);
            if (g == null) {
                return;
            }
            g.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, l.random.nextFloat() * 360F, 0);
            if (g.hasLineOfSight(pl)) {
                continue;
            }
            g.finalizeSpawn(l, l.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null, null);
            g.addTag(TAG);
            l.addFreshEntity(g);
            return;
        }
    }
}
