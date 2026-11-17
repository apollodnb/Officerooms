package de.officerooms.entity;

import de.officerooms.core.OfficeRoomsConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.tags.GameEventTags;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.VanillaGameEvent;
import net.minecraftforge.event.entity.living.LivingEvent;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Geräuscherkennung für den Headless Warden – getrennt von KI und Audio.
 *
 * <p>Quelle sind die Vanilla-Spielereignisse ({@link GameEvent}), die Forge über
 * {@link VanillaGameEvent} für jedes Ereignis in jeder Welt meldet, plus Sprünge
 * ({@link LivingEvent.LivingJumpEvent}, dafür gibt es kein Vanilla-Ereignis). Es werden keine
 * Sculk-Blöcke oder Vanilla-Vibrationsempfänger verwendet.</p>
 *
 * <p>Jedes Ereignis bekommt eine Hörweite; lautere Aktionen reichen weiter. Gezählt werden nur
 * Ereignisse, die ein Spieler (oder sein Geschoss) auslöst. Die Hörweite wird <b>nicht</b> durch
 * Wände oder geschlossene Türen begrenzt (keine Verdeckungsprüfung) – der Warden hört durch Wände.</p>
 *
 * <p>Schleichen: Ereignisse aus dem Vanilla-Tag {@code ignore_vibrations_sneaking} (Schritte, Landen,
 * Schwimmen, Gleiten …) werden beim Schleichen ignoriert – genau wie bei Sculk-Sensoren. Die übrigen
 * Geräusche eines schleichenden Spielers zählen mit halber Reichweite.</p>
 */
public final class NoiseSystem {

    private static final Map<GameEvent, Double> RADIUS = new IdentityHashMap<>();

    static {
        RADIUS.put(GameEvent.STEP, 10.0);              // Gehen (Rennen: siehe SPRINT_STEP)
        RADIUS.put(GameEvent.HIT_GROUND, 14.0);        // Landen
        RADIUS.put(GameEvent.SPLASH, 12.0);
        RADIUS.put(GameEvent.SWIM, 10.0);
        RADIUS.put(GameEvent.FLAP, 8.0);
        RADIUS.put(GameEvent.ELYTRA_GLIDE, 10.0);
        RADIUS.put(GameEvent.BLOCK_OPEN, 18.0);        // Türen, Falltüren, Zauntore
        RADIUS.put(GameEvent.BLOCK_CLOSE, 18.0);
        RADIUS.put(GameEvent.BLOCK_DESTROY, 20.0);     // Blöcke abbauen
        RADIUS.put(GameEvent.BLOCK_PLACE, 14.0);       // Blöcke setzen
        RADIUS.put(GameEvent.BLOCK_CHANGE, 10.0);
        RADIUS.put(GameEvent.BLOCK_ACTIVATE, 12.0);
        RADIUS.put(GameEvent.BLOCK_DEACTIVATE, 12.0);
        RADIUS.put(GameEvent.BLOCK_ATTACH, 8.0);
        RADIUS.put(GameEvent.BLOCK_DETACH, 8.0);
        RADIUS.put(GameEvent.CONTAINER_OPEN, 14.0);    // Schubladen, Truhen
        RADIUS.put(GameEvent.CONTAINER_CLOSE, 14.0);
        RADIUS.put(GameEvent.ENTITY_DAMAGE, 16.0);     // Treffer
        RADIUS.put(GameEvent.PROJECTILE_SHOOT, 22.0);
        RADIUS.put(GameEvent.PROJECTILE_LAND, 12.0);
        RADIUS.put(GameEvent.PRIME_FUSE, 20.0);
        RADIUS.put(GameEvent.EXPLODE, 64.0);
        RADIUS.put(GameEvent.EAT, 8.0);
        RADIUS.put(GameEvent.DRINK, 8.0);
        RADIUS.put(GameEvent.ITEM_INTERACT_FINISH, 8.0);
        RADIUS.put(GameEvent.EQUIP, 6.0);
        RADIUS.put(GameEvent.SHEAR, 10.0);
        RADIUS.put(GameEvent.INSTRUMENT_PLAY, 48.0);
        RADIUS.put(GameEvent.NOTE_BLOCK_PLAY, 32.0);
        RADIUS.put(GameEvent.JUKEBOX_PLAY, 48.0);
        RADIUS.put(GameEvent.ENTITY_PLACE, 10.0);
        RADIUS.put(GameEvent.TELEPORT, 16.0);
        RADIUS.put(GameEvent.FLUID_PLACE, 10.0);
        RADIUS.put(GameEvent.FLUID_PICKUP, 10.0);
    }

    static final double SPRINT_STEP = 18.0;
    static final double JUMP = 12.0;

    private NoiseSystem() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(NoiseSystem::onGameEvent);
        MinecraftForge.EVENT_BUS.addListener(NoiseSystem::onJump);
    }

    private static Player playerOf(Entity e) {
        if (e instanceof Player p) {
            return p;
        }
        if (e instanceof Projectile proj && proj.getOwner() instanceof Player p) {
            return p;
        }
        return null;
    }

    private static void onGameEvent(VanillaGameEvent event) {
        Level level = event.getLevel();
        // Performance: Schritte aller Wesen in allen Dimensionen kommen hier an; früh aussteigen.
        if (level.isClientSide || level.dimension() != de.officerooms.dimension.OfficeRoomsDimension.LEVEL_KEY) {
            return;
        }
        Player player = playerOf(event.getCause());
        if (player == null) {
            return;
        }
        GameEvent ge = event.getVanillaEvent();
        // Wie Vanilla-Vibrationsempfänger: vorsichtiges Gehen (Schleichen) dämpft Schritte, Landen usw. vollständig.
        if (player.isSteppingCarefully() && ge.is(GameEventTags.IGNORE_VIBRATIONS_SNEAKING)) {
            return;
        }
        Double radius = RADIUS.get(ge);
        if (radius == null) {
            return;
        }
        double r = radius;
        if (ge == GameEvent.STEP && player.isSprinting()) {
            r = SPRINT_STEP;
        }
        if (player.isCrouching()) {
            r *= 0.5;
        }
        emit(level, event.getEventPosition(), player, r);
    }

    private static void onJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof Player p && !p.level().isClientSide && !p.isCrouching()
                && p.level().dimension() == de.officerooms.dimension.OfficeRoomsDimension.LEVEL_KEY) {
            emit(p.level(), p.position(), p, JUMP);
        }
    }

    /** Verteilt ein Geräusch an alle Headless Warden in Hörweite (ohne Verdeckung durch Wände). */
    public static int emit(Level level, Vec3 pos, Player source, double radius) {
        double r = radius * OfficeRoomsConfig.WARDEN_HEARING.get();
        int n = 0;
        for (HeadlessWarden w : level.getEntitiesOfClass(HeadlessWarden.class, new AABB(pos, pos).inflate(r))) {
            if (w.isAlive() && w.position().distanceToSqr(pos) <= r * r) {
                w.hear(pos, source, r);
                n++;
            }
        }
        return n;
    }
}
