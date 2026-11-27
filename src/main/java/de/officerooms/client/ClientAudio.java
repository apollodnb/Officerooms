package de.officerooms.client;

import de.officerooms.core.ModSounds;
import de.officerooms.core.OfficeRoomsConfig;
import de.officerooms.dimension.OfficeRoomsDimension;
import de.officerooms.entity.HeadlessWarden;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Zentrales Audiosystem der Dimension (nur Client), getrennt von der KI.
 *
 * <ul>
 *     <li><b>Lampenbrummen:</b> genau eine nahtlos geloopte Klanginstanz für die ganze Dimension (keine
 *     Sounds pro Lampenblock, keine mehrfachen Loops). Die Lautstärke folgt sanft der Blockhelligkeit
 *     am Spieler: unter beleuchteten Deckenpanels etwas lauter, im Dunkeln leiser.</li>
 *     <li><b>Musik:</b> die bereitgestellte Aufnahme als eine einzige, gestreamte und nahtlos geloopte
 *     Instanz (Kategorie Musik, also über den Musikregler steuerbar). Sie startet beim Betreten der
 *     Dimension, läuft bei Chunk- und Geschosswechseln einfach weiter und stoppt beim Verlassen. In der
 *     Dimension wird die Vanilla-Musik (auch die Kreativ-Musik) unterdrückt.</li>
 *     <li><b>Herzschlag:</b> nur in der Nähe eines Headless Warden, umso schneller und lauter, je näher er
 *     ist; schneller, wenn er jagt. Räumlich an seiner Position (Richtung hörbar), Lautstärke über die
 *     Entfernung selbst geregelt; nie mehrere Herzschläge gleichzeitig.</li>
 * </ul>
 */
public final class ClientAudio {

    private static HumSound hum;
    private static LoopSound music;
    private static LoopSound funMusic;
    private static boolean inFun;
    private static int funLeft;
    private static final java.util.Random RNG = new java.util.Random();
    private static int heartbeatTimer;

    private static long ticks;
    private static long nextMusic;
    private static boolean entered;
    private static net.minecraft.client.multiplayer.ClientLevel lastLevel;

    /** Läuft die Hauptmusik gerade? (Umgebungsgeräusche pausieren dann.) */
    static boolean musicPlaying(Minecraft mc) {
        // Level Fun music does not count: the party ambience keeps playing underneath it
        return music != null && !music.isStopped() && mc.getSoundManager().isActive(music);
    }

    private ClientAudio() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ClientAudio::onClientTick);
        NeoForge.EVENT_BUS.addListener(ClientAudio::onPlaySound);
    }

    /** Protokolliert (Debug-Level) jeden gestarteten Office-Rooms-Sound – für Tests ohne Lautsprecher. */
    private static void onPlaySound(net.neoforged.neoforge.client.event.sound.PlaySoundEvent event) {
        SoundInstance s = event.getSound();
        if (s != null && s.getLocation().getNamespace().equals(de.officerooms.core.OfficeRoomsIds.MOD_ID)) {
            de.officerooms.OfficeRooms.LOGGER.debug("[Audio] {} ({}) at {} {} {}", s.getLocation(), s.getSource(),
                    (int) s.getX(), (int) s.getY(), (int) s.getZ());
        }
    }

    static boolean inOffice() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.player != null && mc.level.dimension() == OfficeRoomsDimension.LEVEL_KEY;
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        if (false) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            lastLevel = null;
            hum = null;
            music = null;
            funMusic = null;
            inFun = false;
            entered = false;
            return;
        }
        boolean office = inOffice();
        ticks++;
        // Neue Client-Welt (Dimensionswechsel, auch ein sehr kurzer über ein Portal): Minecraft hat dabei alle
        // Sounds beendet. Wie Verlassen behandeln, damit die Musik beim (Wieder-)Betreten sicher einmal startet.
        if (mc.level != lastLevel) {
            lastLevel = mc.level;
            entered = false;
            music = null;
            funMusic = null;
            inFun = false;
            hum = null;
            AmbientSounds.reset();
        }
        if (office) {
            // Vanilla-Musik (auch Kreativ-Musik) in der Dimension unterdrücken
            mc.getMusicManager().stopPlaying();
            // Hauptmusik: beim Betreten einmal, danach alle 30 Minuten (ab Start gerechnet), solange man bleibt.
            // Chunk- oder Geschosswechsel ändern nichts; es gibt nie mehr als eine Instanz.
            // Echtzeit statt Client-Ticks: bei niedriger Bildrate laufen Ticks langsamer (im Test 2 statt 2:50 min)
            long now = net.minecraft.Util.getMillis();
            // 0.30.0: Level Fun has its own looping music. Checked every 2 s with hysteresis so walking along a
            // Fun wall does not flicker; while it plays the office music is paused and its timer waits.
            if (ticks % 40 == 0) {
                if (AmbientSounds.funNearby(mc, mc.player)) {
                    funLeft = 0;
                    inFun = true;
                } else if (inFun && ++funLeft >= 4) {
                    inFun = false;
                }
            }
            if (inFun) {
                if (music != null) {
                    mc.getSoundManager().stop(music);
                    music = null;
                }
                if (audible(mc, SoundSource.MUSIC) && dead(mc, funMusic)) {
                    funMusic = new LoopSound(ModSounds.MUSIC_LEVEL_FUN.get(), SoundSource.MUSIC,
                            () -> OfficeRoomsConfig.MUSIC_VOLUME.get().floatValue(), true);
                    mc.getSoundManager().play(funMusic);
                }
                if (nextMusic < now + 60_000L) {
                    nextMusic = now + 60_000L; // a fresh office track only after Level Fun, never straight away
                }
            } else {
                if (funMusic != null) {
                    mc.getSoundManager().stop(funMusic);
                    funMusic = null;
                }
                // Office music: one random track (of three) on joining/entering, then about one per half hour
                // (randomised 70-130 % of the interval, counted from the start of the last track). In between
                // only the hum and the ambient sounds play.
                if (!entered) {
                    entered = true;
                    nextMusic = now;
                }
                if (now >= nextMusic && !musicPlaying(mc)) {
                    long base = OfficeRoomsConfig.MUSIC_INTERVAL_MINUTES.get() * 60_000L;
                    nextMusic = now + (long) (base * (0.7 + RNG.nextDouble() * 0.6));
                    music = null;
                    if (audible(mc, SoundSource.MUSIC)) {
                        music = new LoopSound(ModSounds.MUSIC.get(), SoundSource.MUSIC, () -> OfficeRoomsConfig.MUSIC_VOLUME.get().floatValue(), false);
                        mc.getSoundManager().play(music);
                    }
                }
            }
            if (audible(mc, SoundSource.AMBIENT) && dead(mc, hum)) {
                hum = new HumSound();
                mc.getSoundManager().play(hum);
            }
            AmbientSounds.tick(mc, ticks, musicPlaying(mc));
        } else {
            // Verlassen: Musiklogik zurücksetzen (beim nächsten Betreten startet die Musik wieder sofort)
            entered = false;
            if (music != null) {
                mc.getSoundManager().stop(music);
                music = null;
            }
            if (funMusic != null) {
                mc.getSoundManager().stop(funMusic);
                funMusic = null;
            }
            inFun = false;
            if (hum != null) {
                mc.getSoundManager().stop(hum);
                hum = null;
            }
            AmbientSounds.reset();
        }
        if (office) {
            heartbeat(mc);
        }

    }

    /**
     * Performance: Bei Lautstärke 0 spielt Minecraft eine Instanz gar nicht erst ab. Ohne diese
     * Prüfung würde alle 2 s eine neue Schleife angelegt (und die Musikdatei neu geöffnet).
     */
    private static boolean audible(Minecraft mc, SoundSource src) {
        return mc.options.getSoundSourceVolume(SoundSource.MASTER) > 0F && mc.options.getSoundSourceVolume(src) > 0F;
    }

    /** Instanz fehlt, ist gestoppt oder läuft seit über 2 s nicht mehr (z. B. von Minecraft beendet). */
    private static boolean dead(Minecraft mc, LoopSound s) {
        if (s == null || s.isStopped()) {
            return true;
        }
        if (mc.getSoundManager().isActive(s)) {
            s.inactive = 0;
            return false;
        }
        return ++s.inactive > 40;
    }

    private static void heartbeat(Minecraft mc) {
        // Performance: Umgebung nur durchsuchen, wenn der nächste Schlag fällig ist (sonst jedes Tick).
        if (--heartbeatTimer > 0) {
            return;
        }
        LocalPlayer player = mc.player;
        int range = OfficeRoomsConfig.HEARTBEAT_RANGE.get();
        HeadlessWarden nearest = null;
        double best = Double.MAX_VALUE;
        for (HeadlessWarden w : mc.level.getEntitiesOfClass(HeadlessWarden.class, player.getBoundingBox().inflate(range))) {
            double d = w.distanceTo(player);
            if (d < best && w.isAlive()) {
                best = d;
                nearest = w;
            }
        }
        if (nearest == null || best > range) {
            heartbeatTimer = 5;
            return;
        }
        float intensity = (float) (1.0 - best / range);
        int interval = Math.round(Mth.lerp(intensity, 34F, 9F) * (nearest.isChasing() ? 0.7F : 1F));
        heartbeatTimer = Math.max(6, interval);
        float volume = (float) (OfficeRoomsConfig.HEARTBEAT_VOLUME.get() * (0.12 + 0.88 * intensity * intensity));
        mc.getSoundManager().play(new SimpleSoundInstance(ModSounds.WARDEN_HEARTBEAT.get().getLocation(), SoundSource.HOSTILE,
                volume, 0.9F + 0.2F * intensity, RandomSource.create(), false, 0, SoundInstance.Attenuation.NONE,
                nearest.getX(), nearest.getY() + 1.4, nearest.getZ(), false));
    }

    /**
     * Nahtlos geloopte, nicht-räumliche Instanz, die sich beim Verlassen der Dimension selbst stoppt.
     * Für die Musik: die bereitgestellte Aufnahme als gestreamter Loop (Kategorie Musik).
     */
    private static class LoopSound extends AbstractTickableSoundInstance {
        private final java.util.function.Supplier<Float> vol;
        int inactive;

        LoopSound(net.minecraft.sounds.SoundEvent event, SoundSource source, java.util.function.Supplier<Float> vol) {
            this(event, source, vol, true);
        }

        LoopSound(net.minecraft.sounds.SoundEvent event, SoundSource source, java.util.function.Supplier<Float> vol, boolean loop) {
            super(event, source, SoundInstance.createUnseededRandom());
            this.vol = vol;
            this.looping = loop;
            this.delay = 0;
            this.relative = true;
            this.attenuation = Attenuation.NONE;
            this.volume = vol.get();
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            if (!inOffice()) {
                stop();
                return;
            }
            volume = vol.get();
        }
    }

    /** Lampenbrummen: Lautstärke folgt sanft der Blockhelligkeit am Spieler. */
    private static final class HumSound extends LoopSound {
        private float smooth = 0.5F;

        HumSound() {
            super(ModSounds.LAMP_HUM.get(), SoundSource.AMBIENT, () -> 0.01F);
        }

        @Override
        public void tick() {
            Minecraft mc = Minecraft.getInstance();
            if (!inOffice()) {
                stop();
                return;
            }
            int light = mc.level.getBrightness(LightLayer.BLOCK, mc.player.blockPosition().above());
            float target = 0.35F + 0.65F * (light / 15F);
            smooth += (target - smooth) * 0.05F;
            volume = (float) (OfficeRoomsConfig.HUM_VOLUME.get() * smooth * Math.max(0F, (de.officerooms.hazard.BlackoutState.dim - 0.1F) / 0.9F));
        }
    }
}
