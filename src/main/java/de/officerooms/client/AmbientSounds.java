package de.officerooms.client;

import de.officerooms.core.ModSounds;
import de.officerooms.core.OfficeRoomsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;

/**
 * Sporadische Umgebungsgeräusche (Liminal-World-Update), nur Client.
 *
 * <ul>
 *     <li>Ein Zeitgeber würfelt die nächste Wiedergabe in langen, zufälligen Abständen (Standard
 *     45–150 s). Er hängt nur an der Spielzeit, nicht an Chunks: Chunkwechsel lösen nichts aus.</li>
 *     <li>Drei Klangpools: Büro, Lush (Moos/Azaleen in der Nähe) und dunkle Ebenen (unter Y −38).</li>
 *     <li>Jedes Geräusch hat eine Position: 8–22 Blöcke entfernt in zufälliger Richtung, „hinter dir“
 *     2–4 Blöcke hinter der Blickrichtung. Lautstärke dezent.</li>
 *     <li>Während die Hauptmusik läuft, wird nichts abgespielt; der Zeitgeber wartet.</li>
 *     <li>Kosten: ein Zufallswurf pro Tick, Umgebungsprüfung nur beim Abspielen.</li>
 * </ul>
 */
public final class AmbientSounds {

    /** Gewichteter Eintrag eines Klangpools. */
    private record Entry(RegistryObject<SoundEvent> sound, int weight, boolean behind) {
    }

    private static final Entry[] OFFICE = {
            new Entry(ModSounds.AMB_STEPS, 14, false), new Entry(ModSounds.AMB_KNOCK, 6, false),
            new Entry(ModSounds.AMB_DOOR, 10, false), new Entry(ModSounds.AMB_NEON, 16, false),
            new Entry(ModSounds.AMB_METAL, 7, false), new Entry(ModSounds.AMB_CREAK, 9, false),
            new Entry(ModSounds.AMB_VOICES, 4, false), new Entry(ModSounds.AMB_WHISPER, 1, true),
            new Entry(ModSounds.AMB_WIND, 8, false), new Entry(ModSounds.AMB_BEHIND, 2, true),
            new Entry(ModSounds.AMB_BUZZ, 14, false), new Entry(ModSounds.AMB_VENT, 7, false),       // 0.18.0
            new Entry(ModSounds.AMB_PIPE, 7, false), new Entry(ModSounds.AMB_DISTANT, 3, false)};
    private static final Entry[] LUSH = {
            new Entry(ModSounds.AMB_RUSTLE, 18, false), new Entry(ModSounds.AMB_DRIP, 14, false),
            new Entry(ModSounds.AMB_WIND, 10, false), new Entry(ModSounds.AMB_CREAK, 6, false),
            new Entry(ModSounds.AMB_STEPS, 3, false), new Entry(ModSounds.AMB_WHISPER, 1, true)};
    private static final Entry[] DARK = {
            new Entry(ModSounds.AMB_STEPS, 12, false), new Entry(ModSounds.AMB_KNOCK, 8, false),
            new Entry(ModSounds.AMB_METAL, 8, false), new Entry(ModSounds.AMB_RUMBLE, 10, false),
            new Entry(ModSounds.AMB_VOICES, 5, false), new Entry(ModSounds.AMB_WHISPER, 4, true),
            new Entry(ModSounds.AMB_BEHIND, 6, true), new Entry(ModSounds.AMB_CREAK, 6, false),
            new Entry(ModSounds.AMB_PIPE, 8, false), new Entry(ModSounds.AMB_DISTANT, 7, false), new Entry(ModSounds.AMB_VENT, 5, false)};
    /** 0.18.0: area soundscapes. */
    private static final Entry[] INDUSTRIAL = {
            new Entry(ModSounds.AMB_MACHINE, 20, false), new Entry(ModSounds.AMB_PIPE, 14, false), new Entry(ModSounds.AMB_STEAM, 8, false),
            new Entry(ModSounds.AMB_BUZZ, 8, false), new Entry(ModSounds.AMB_METAL, 9, false), new Entry(ModSounds.AMB_DISTANT, 4, false),
            new Entry(ModSounds.AMB_STEPS, 3, false)};
    private static final Entry[] LAB = {
            new Entry(ModSounds.AMB_LAB, 18, false), new Entry(ModSounds.AMB_BUZZ, 10, false), new Entry(ModSounds.AMB_NEON, 6, false),
            new Entry(ModSounds.AMB_VENT, 6, false), new Entry(ModSounds.AMB_STEPS, 3, false), new Entry(ModSounds.AMB_WHISPER, 1, true)};
    private static final Entry[] HOTEL = {
            new Entry(ModSounds.AMB_HOTEL, 14, false), new Entry(ModSounds.AMB_CREAK, 10, false), new Entry(ModSounds.AMB_DOOR, 8, false),
            new Entry(ModSounds.AMB_STEPS, 7, false), new Entry(ModSounds.AMB_VENT, 4, false), new Entry(ModSounds.AMB_DISTANT, 3, false),
            new Entry(ModSounds.AMB_WHISPER, 1, true)};
    private static final Entry[] FUN = {
            new Entry(ModSounds.AMB_FUN_PARTY, 14, false), // 0.30.0: music box dropped, Level Fun has its own music
            new Entry(ModSounds.PARTYGOER_GIGGLE, 7, false), new Entry(ModSounds.PARTYGOER_HORN, 4, false), new Entry(ModSounds.AMB_BUZZ, 5, false),
            new Entry(ModSounds.AMB_WHISPER, 2, true), new Entry(ModSounds.AMB_BEHIND, 1, true)};

    /** Unterste dunkle Ebenen (Geschoss 0–4) liegen unter dieser Höhe. */
    /** Poolrooms: Wasser, Hall, Pumpe – eigene Klangwelt. */
    private static final Entry[] POOL = {
            new Entry(ModSounds.AMB_POOL_LAP, 18, false), new Entry(ModSounds.AMB_POOL_DRIP, 16, false),
            new Entry(ModSounds.AMB_POOL_SPLASH, 7, false), new Entry(ModSounds.AMB_POOL_PUMP, 10, false),
            new Entry(ModSounds.AMB_POOL_ECHO, 3, false), new Entry(ModSounds.AMB_STEPS, 2, false),
            new Entry(ModSounds.AMB_WHISPER, 1, true), new Entry(ModSounds.AMB_POOL_DISTANT, 5, false)};
    static final int DARK_Y = -33; // unter Geschoss 5 (y0 -63 + 5 × 6)

    private static final RandomSource RNG = RandomSource.create();
    private static long next = -1;
    private static SoundEvent last;

    private AmbientSounds() {
    }

    static void reset() {
        next = -1;
    }

    static void tick(Minecraft mc, long ticks, boolean musicPlaying) {
        if (next < 0) {
            next = ticks + delay();
            return;
        }
        if (ticks < next) {
            return;
        }
        if (musicPlaying || OfficeRoomsConfig.AMBIENT_VOLUME.get() <= 0) {
            next = ticks + 200; // später erneut versuchen
            return;
        }
        next = ticks + delay();
        play(mc);
    }

    private static long delay() {
        int lo = OfficeRoomsConfig.AMBIENT_MIN_SECONDS.get();
        int hi = Math.max(lo + 1, OfficeRoomsConfig.AMBIENT_MAX_SECONDS.get());
        return (lo + RNG.nextInt(hi - lo)) * 20L;
    }

    private static void play(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) {
            return;
        }
        Entry[] pool = poolroomsNearby(mc, p) ? POOL : area(mc, p);
        Entry e = pick(pool);
        if (e.sound().get() == last && RNG.nextInt(3) != 0) {
            e = pick(pool); // gleiche Geräusche nicht direkt hintereinander
        }
        last = e.sound().get();
        Vec3 pos;
        if (e.behind()) {
            Vec3 look = p.getLookAngle().multiply(1, 0, 1).normalize();
            double d = 2 + RNG.nextDouble() * 2;
            pos = p.position().subtract(look.scale(d)).add(0, 1.2, 0);
        } else {
            double a = RNG.nextDouble() * Math.PI * 2;
            double d = 8 + RNG.nextDouble() * 14;
            pos = p.position().add(Math.cos(a) * d, RNG.nextInt(3) - 0.5, Math.sin(a) * d);
        }
        float vol = (float) (OfficeRoomsConfig.AMBIENT_VOLUME.get() * (0.5 + 0.3 * RNG.nextDouble()));
        mc.getSoundManager().play(new SimpleSoundInstance(e.sound().get().getLocation(), SoundSource.AMBIENT, vol,
                0.85F + 0.3F * RNG.nextFloat(), RandomSource.create(RNG.nextLong()), false, 0,
                SoundInstance.Attenuation.LINEAR, pos.x, pos.y, pos.z, false));
    }

    private static Entry pick(Entry[] pool) {
        int total = 0;
        for (Entry e : pool) {
            total += e.weight();
        }
        int x = RNG.nextInt(total);
        for (Entry e : pool) {
            x -= e.weight();
            if (x < 0) {
                return e;
            }
        }
        return pool[0];
    }

    /** Poolrooms-Fliesen unter oder neben dem Spieler (nur beim Abspielen geprüft). */
    static boolean poolroomsNearby(Minecraft mc, LocalPlayer p) {
        BlockPos base = p.blockPosition();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -3; dx <= 3; dx += 3) {
            for (int dz = -3; dz <= 3; dz += 3) {
                for (int dy = -2; dy <= 0; dy++) {
                    var s = mc.level.getBlockState(m.set(base.getX() + dx, base.getY() + dy, base.getZ() + dz));
                    if (s.is(de.officerooms.block.ModBlocks.POOL_FLOOR_TILE.get()) || s.is(de.officerooms.block.ModBlocks.POOL_TILE.get())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** 0.30.0: Level Fun wallpaper or play carpet around the player (for the Level Fun music). */
    static boolean funNearby(Minecraft mc, LocalPlayer p) {
        BlockPos base = p.blockPosition();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int fun = 0;
        for (int dx = -6; dx <= 6; dx += 3) {
            for (int dz = -6; dz <= 6; dz += 3) {
                for (int dy = -1; dy <= 2; dy++) {
                    var b = mc.level.getBlockState(m.set(base.getX() + dx, base.getY() + dy, base.getZ() + dz)).getBlock();
                    if (b == de.officerooms.block.ModBlocks.FUN_WALLPAPER.get() || b == de.officerooms.block.ModBlocks.PLAY_CARPET.get()) {
                        if (++fun >= 4) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /** 0.18.0: which soundscape fits the blocks around the player (sampled only when a sound plays). */
    private static Entry[] area(Minecraft mc, LocalPlayer p) {
        BlockPos base = p.blockPosition();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int ind = 0;
        int lab = 0;
        int hotel = 0;
        int fun = 0;
        for (int dx = -6; dx <= 6; dx += 3) {
            for (int dz = -6; dz <= 6; dz += 3) {
                for (int dy = -1; dy <= 2; dy++) {
                    var b = mc.level.getBlockState(m.set(base.getX() + dx, base.getY() + dy, base.getZ() + dz)).getBlock();
                    if (b == de.officerooms.block.ModBlocks.INDUSTRIAL_WALL.get() || b == de.officerooms.block.ModBlocks.GRATE.get()
                            || b == de.officerooms.block.ModBlocks.GENERATOR_CASING.get() || b == de.officerooms.block.ModBlocks.PIPE.get()) {
                        ind++;
                    } else if (b == de.officerooms.block.ModBlocks.SERVER_RACK.get() || b == de.officerooms.block.ModBlocks.CONTAINMENT_TANK.get()
                            || b == de.officerooms.block.ModBlocks.CCTV_BANK.get() || b == de.officerooms.block.ModBlocks.CONTROL_PANEL.get()
                            || b == de.officerooms.block.ModBlocks.LAB_CABINET.get()) {
                        lab++;
                    } else if (b == de.officerooms.block.ModBlocks.HOTEL_WALLPAPER.get() || b == de.officerooms.block.ModBlocks.HOTEL_CARPET.get()
                            || b == de.officerooms.block.ModBlocks.HOTEL_MARBLE.get()) {
                        hotel++;
                    } else if (b == de.officerooms.block.ModBlocks.FUN_WALLPAPER.get() || b == de.officerooms.block.ModBlocks.PLAY_CARPET.get()) {
                        fun++;
                    }
                }
            }
        }
        if (ind >= 4) {
            return INDUSTRIAL;
        }
        if (fun >= 4) {
            return FUN;
        }
        if (hotel >= 4) {
            return HOTEL;
        }
        if (lab >= 2) {
            return LAB;
        }
        return p.getY() < DARK_Y ? DARK : lushNearby(mc, p) ? LUSH : OFFICE;
    }

    /** Moos oder Azaleen in wenigen Blöcken Umkreis (nur beim Abspielen geprüft). */
    private static boolean lushNearby(Minecraft mc, LocalPlayer p) {
        BlockPos base = p.blockPosition();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -4; dx <= 4; dx += 2) {
            for (int dz = -4; dz <= 4; dz += 2) {
                for (int dy = -1; dy <= 1; dy++) {
                    var s = mc.level.getBlockState(m.set(base.getX() + dx, base.getY() + dy, base.getZ() + dz));
                    if (s.is(Blocks.MOSS_BLOCK) || s.is(Blocks.MOSS_CARPET) || s.is(Blocks.AZALEA_LEAVES)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
