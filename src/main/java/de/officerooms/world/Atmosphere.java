package de.officerooms.world;

import de.officerooms.block.CeilingLightBlock;
import de.officerooms.core.ModSounds;
import de.officerooms.core.OfficeRoomsConfig;
import de.officerooms.dimension.OfficeRoomsDimension;
import de.officerooms.megastructure.Maze;
import de.officerooms.megastructure.Neighbourhood;
import de.officerooms.megastructure.Poolrooms;
import de.officerooms.room.OfficePlanner;
import de.officerooms.worldgen.OfficeChunkGenerator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;

/**
 * 0.9.0: Atmosphere of the Office Rooms.
 *
 * <p><b>Megastructure hints:</b> a megastructure only fills a few storeys of its region. On the storeys above
 * and below, its sounds seep through the floors: lapping water (Poolrooms), buzzing neon (The Maze), distant
 * barking (Neighbourhood). The closer the storey, the louder and more frequent; the sound comes from the
 * direction (above / below) of the structure.</p>
 *
 * <p><b>Flickering lights:</b> ceiling lights close to a creature flicker, as a warning.</p>
 */
public final class Atmosphere {

    private static final int HINT_INTERVAL = 60;
    private static final Map<BlockPos, Long> OFF = new HashMap<>();
    private static final Map<UUID, List<BlockPos>> LIGHTS = new HashMap<>();

    private Atmosphere() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(Atmosphere::onLevelTick);
        MinecraftForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> {
            ServerLevel level = e.getServer().getLevel(OfficeRoomsDimension.LEVEL_KEY);
            if (level != null) {
                restore(level, Long.MAX_VALUE);
            }
            OFF.clear();
            LIGHTS.clear();
        });
    }

    private static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)
                || level.dimension() != OfficeRoomsDimension.LEVEL_KEY) {
            return;
        }
        long t = level.getGameTime();
        if (!OFF.isEmpty()) {
            restore(level, t);
        }
        if (level.players().isEmpty()) {
            return;
        }
        try {
            if (t % 2 == 0 && OfficeRoomsConfig.LIGHT_FLICKER.get()) {
                flicker(level, t);
            }
            if (t % HINT_INTERVAL == 11 && OfficeRoomsConfig.MEGA_HINTS.get()
                    && level.getChunkSource().getGenerator() instanceof OfficeChunkGenerator gen) {
                OfficePlanner p = gen.sampler(level.getChunkSource().randomState(), level).planner();
                for (ServerPlayer player : level.players()) {
                    hint(level, p, player);
                }
            }
        } catch (RuntimeException e) {
            de.officerooms.OfficeRooms.LOGGER.error("Atmosphere tick failed", e);
        }
    }

    // --- Megastructure hints --------------------------------------------------------------------

    private static void hint(ServerLevel level, OfficePlanner p, ServerPlayer player) {
        int rx = p.regionOf(player.getBlockX());
        int rz = p.regionOf(player.getBlockZ());
        int s0;
        int n;
        SoundEvent sound;
        float pitch = 0.9F + level.random.nextFloat() * 0.2F;
        Poolrooms.Site pool = p.pools.site(rx, rz);
        Neighbourhood.Site hood = pool == null ? p.hood.site(rx, rz) : null;
        Maze.Site maze = pool == null && hood == null ? p.maze.site(rx, rz) : null;
        de.officerooms.megastructure.Hotel.Site hotel = pool == null && hood == null && maze == null ? p.hotel.site(rx, rz) : null;
        de.officerooms.megastructure.Industrial.Site ind = hotel == null && pool == null && hood == null && maze == null ? p.industrial.site(rx, rz) : null;
        de.officerooms.megastructure.LevelFun.Site fun = ind == null && hotel == null && pool == null && hood == null && maze == null ? p.fun.site(rx, rz) : null;
        if (pool != null) {
            s0 = pool.s0;
            n = Poolrooms.STOREYS;
            sound = (level.random.nextInt(3) == 0 ? ModSounds.AMB_POOL_ECHO : ModSounds.AMB_POOL_LAP).get();
        } else if (hood != null) {
            s0 = hood.s0;
            n = hood.storeys;
            sound = ModSounds.HOUND_BARK.get();
            pitch *= 0.8F;
        } else if (maze != null) {
            return;                                                    // 0.27.0: no hint sound (sounded like 8-bit music)
        } else if (hotel != null) {
            s0 = hotel.s0;
            n = hotel.storeys;
            sound = net.minecraft.sounds.SoundEvents.BELL_BLOCK;      // a reception bell somewhere below
            pitch *= 0.7F;
        } else if (ind != null) {
            s0 = ind.s0;
            n = ind.storeys;
            sound = net.minecraft.sounds.SoundEvents.BEACON_AMBIENT;   // the hum of machinery somewhere close
            pitch *= 0.5F;
        } else if (fun != null) {
            s0 = fun.s0;
            n = fun.storeys;
            sound = ModSounds.PARTYGOER_GIGGLE.get();                    // distant giggling
            pitch *= 0.85F;
        } else {
            return;
        }
        int ps = p.storeyOf(player.getBlockY());
        int dist = ps < s0 ? s0 - ps : ps > s0 + n - 1 ? ps - (s0 + n - 1) : 0;
        int range = OfficeRoomsConfig.MEGA_HINT_STOREYS.get();
        if (dist == 0 || dist > range) {
            return;
        }
        double closeness = 1.0 - (dist - 1) / (double) range;   // 1 next storey … ~0 at the edge
        if (level.random.nextDouble() > 0.25 + 0.55 * closeness) {
            return;
        }
        float volume = (float) (0.25 + 0.75 * closeness);
        double dy = ps < s0 ? 6 : -6;
        double x = player.getX() + (level.random.nextDouble() - 0.5) * 6;
        double z = player.getZ() + (level.random.nextDouble() - 0.5) * 6;
        Holder<SoundEvent> holder = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound);
        player.connection.send(new ClientboundSoundPacket(holder, SoundSource.AMBIENT, x, player.getY() + dy, z, volume, pitch, level.random.nextLong()));
    }

    // --- Flickering lights ----------------------------------------------------------------------

    private static void flicker(ServerLevel level, long t) {
        int radius = OfficeRoomsConfig.LIGHT_FLICKER_RADIUS.get();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }
            if (t % 20 == 0) {
                LIGHTS.put(player.getUUID(), lightsNearCreatures(level, player, radius));
            }
            List<BlockPos> lights = LIGHTS.get(player.getUUID());
            if (lights == null || lights.isEmpty() || level.random.nextInt(3) != 0) {
                continue;
            }
            int n = 1 + level.random.nextInt(2);
            for (int i = 0; i < n; i++) {
                BlockPos pos = lights.get(level.random.nextInt(lights.size()));
                if (OFF.containsKey(pos) || !level.isLoaded(pos)) {
                    continue;
                }
                BlockState st = level.getBlockState(pos);
                if (st.getBlock() instanceof CeilingLightBlock && st.getValue(CeilingLightBlock.LIT)) {
                    level.setBlock(pos, st.setValue(CeilingLightBlock.LIT, false), 2 | 16);
                    OFF.put(pos, t + 2 + level.random.nextInt(7));
                }
            }
        }
        if (t % 200 == 0) {
            LIGHTS.keySet().removeIf(id -> level.getPlayerByUUID(id) == null);
        }
    }

    private static List<BlockPos> lightsNearCreatures(ServerLevel level, ServerPlayer player, int radius) {
        List<BlockPos> out = new ArrayList<>();
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(radius, 5, radius),
                e -> e.isAlive() && (e instanceof Enemy || e instanceof de.officerooms.entity.Observer));
        int used = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (Mob mob : mobs) {
            if (used++ >= 3) {
                break;
            }
            int bx = mob.getBlockX();
            int by = mob.getBlockY();
            int bz = mob.getBlockZ();
            for (int dy = 1; dy <= 6; dy++) {
                for (int dx = -5; dx <= 5; dx++) {
                    for (int dz = -5; dz <= 5; dz++) {
                        m.set(bx + dx, by + dy, bz + dz);
                        if (!level.isLoaded(m)) {
                            continue;
                        }
                        BlockState st = level.getBlockState(m);
                        if (st.getBlock() instanceof CeilingLightBlock && st.getValue(CeilingLightBlock.LIT)) {
                            out.add(m.immutable());
                        }
                    }
                }
            }
        }
        return out;
    }

    private static void restore(ServerLevel level, long now) {
        Iterator<Map.Entry<BlockPos, Long>> it = OFF.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Long> e = it.next();
            if (e.getValue() > now) {
                continue;
            }
            BlockPos pos = e.getKey();
            if (level.isLoaded(pos)) {
                BlockState st = level.getBlockState(pos);
                if (st.getBlock() instanceof CeilingLightBlock && !st.getValue(CeilingLightBlock.LIT)) {
                    level.setBlock(pos, st.setValue(CeilingLightBlock.LIT, true), 2 | 16);
                }
            }
            it.remove();
        }
    }
}
