package de.officerooms.entity;

import de.officerooms.core.ModSounds;
import de.officerooms.core.OfficeRoomsConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Headless Warden: kopfloser, extrem schneller und gefährlicher Gegner mit rein geräuschbasierter KI.
 *
 * <p><b>Keine Erkennung über Sicht.</b> Es gibt keine Zielauswahl über Sichtlinie (kein
 * {@code NearestAttackableTargetGoal}, kein {@code HurtByTargetGoal}). Ein Ziel entsteht nur über
 * {@link #hear}, das vom {@link NoiseSystem} für Geräusche von Spielern aufgerufen wird (Schritte,
 * Rennen, Springen, Landen, Türen, Blöcke abbauen/setzen, Container, Treffer usw.). Bewegt sich der
 * Spieler schleichend oder gar nicht, entstehen keine Geräusche, und der Warden bleibt ahnungslos,
 * selbst direkt neben ihm.</p>
 *
 * <p>Ablauf: Geräusch registrieren → zur Quelle gehen → weiteren Geräuschen folgen → ist der
 * Spieler beim Geräusch in unmittelbarer Nähe, beginnt mit einem Schrei die extrem schnelle
 * Verfolgung. Bleibt der Spieler {@code forgetSeconds} still (und berührt den Warden nicht), verliert
 * dieser die Spur und untersucht die letzte bekannte Position.</p>
 */
public class HeadlessWarden extends Monster implements software.bernie.geckolib.animatable.GeoEntity {

    // --- 0.11.0: GeckoLib model and animations (assets from tools/gen_mobs_geo.py) --------------
    private final software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache geoCache =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    @Override
    public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers) {
        GeoAnims.register(this, controllers, 0.02, "attack", "scream");
    }

    @Override
    public software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    /** Markierung des einen Wardens, der in jedem Sculk-Raum erscheint (dauerhaft, kein Despawn). */
    public static final String SCULK_TAG = "office_rooms.sculk_room";

    /**
     * Glitched Ingot: genau ein Wurf pro Tod (Standard 50 %). Vanilla ruft diese Methode einmal in
     * {@code dropAllDeathLoot} auf; es gibt keine Loot-Tabelle, die zusätzlich würfelt.
     */
    @Override
    protected void dropCustomDeathLoot(net.minecraft.world.damagesource.DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (random.nextDouble() < de.officerooms.core.OfficeRoomsConfig.WARDEN_INGOT_CHANCE.get()) {
            spawnAtLocation(new net.minecraft.world.item.ItemStack(de.officerooms.item.ModItems.GLITCHED_INGOT.get()));
        }
    }

    private static final EntityDataAccessor<Boolean> DATA_CHASING = SynchedEntityData.defineId(HeadlessWarden.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SCREAM = SynchedEntityData.defineId(HeadlessWarden.class, EntityDataSerializers.INT);

    /** Spieler in diesem Abstand zum Geräusch und zum Warden lösen die Verfolgung aus. */
    static final double DETECT_RANGE = 5.0;
    static final int SCREAM_TICKS = 26;

    private Vec3 noiseTarget;
    private long noiseTime;
    private long lastHeardTarget;
    private int screamCooldown;
    private int attackCooldown;

    public HeadlessWarden(EntityType<? extends HeadlessWarden> type, Level level) {
        super(type, level);
        this.xpReward = 40;
        setMaxUpStep(1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 160.0)
                .add(Attributes.MOVEMENT_SPEED, 0.42)
                .add(Attributes.ATTACK_DAMAGE, 30.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5)
                .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    void applyConfig() {
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(OfficeRoomsConfig.WARDEN_HEALTH.get());
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(OfficeRoomsConfig.WARDEN_DAMAGE.get());
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(OfficeRoomsConfig.WARDEN_SPEED.get());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_CHASING, false);
        entityData.define(DATA_SCREAM, 0);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        SpawnGroupData data, CompoundTag tag) {
        applyConfig();
        setHealth(getMaxHealth());
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        applyConfig();
    }

    // -------------------------------------------------------------------------------------------
    // Zustand (auch für Animation und Audio auf dem Client)
    // -------------------------------------------------------------------------------------------

    public boolean isChasing() {
        return entityData.get(DATA_CHASING);
    }

    /** Verbleibende Schrei-Ticks (> 0 während des Schreis). */
    public int screamTicks() {
        return entityData.get(DATA_SCREAM);
    }

    public Vec3 noiseTarget() {
        return noiseTarget;
    }

    // -------------------------------------------------------------------------------------------
    // Hören
    // -------------------------------------------------------------------------------------------

    static boolean huntable(Player p) {
        return p != null && p.isAlive() && !p.isCreative() && !p.isSpectator();
    }

    /**
     * Ein Geräusch eines Spielers wurde in Hörweite erzeugt (Hörweite prüft das {@link NoiseSystem}).
     */
    public void hear(Vec3 pos, Player source, double radius) {
        if (!huntable(source) || level().isClientSide) {
            return;
        }
        long now = level().getGameTime();
        noiseTarget = pos;
        noiseTime = now;
        if (getTarget() == source) {
            lastHeardTarget = now;
            return;
        }
        double toPlayer = distanceTo(source);
        boolean nearSource = pos.distanceTo(position()) <= DETECT_RANGE;
        if (getTarget() == null && (toPlayer <= DETECT_RANGE || (nearSource && toPlayer <= DETECT_RANGE + 3))) {
            startChase(source);
        }
    }

    void startChase(Player p) {
        setTarget(p);
        lastHeardTarget = level().getGameTime();
        entityData.set(DATA_CHASING, true);
        if (screamCooldown <= 0) {
            entityData.set(DATA_SCREAM, SCREAM_TICKS);
            triggerAnim("move", "scream");
            level().playSound(null, getX(), getY() + 1.5, getZ(), ModSounds.WARDEN_SCREAM.get(), SoundSource.HOSTILE,
                    4.0F, 0.9F + random.nextFloat() * 0.2F);
            screamCooldown = 200;
        }
    }

    void loseTarget() {
        LivingEntity t = getTarget();
        if (t != null) {
            noiseTarget = t.position();
            noiseTime = level().getGameTime();
        }
        setTarget(null);
        entityData.set(DATA_CHASING, false);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (screamCooldown > 0) {
            screamCooldown--;
        }
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        int scream = entityData.get(DATA_SCREAM);
        if (scream > 0) {
            entityData.set(DATA_SCREAM, scream - 1);
        }
        LivingEntity t = getTarget();
        if (t != null) {
            boolean silent = level().getGameTime() - lastHeardTarget > OfficeRoomsConfig.WARDEN_FORGET_SECONDS.get() * 20L;
            if (!(t instanceof Player p) || !huntable(p) || p.level() != level() || (silent && distanceTo(p) > 2.0)) {
                loseTarget();
            }
        } else if (isChasing()) {
            entityData.set(DATA_CHASING, false);
        }
    }

    // -------------------------------------------------------------------------------------------
    // Ziele (Goals)
    // -------------------------------------------------------------------------------------------

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new ChaseGoal());
        goalSelector.addGoal(2, new InvestigateGoal());
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.45));
        // bewusst KEINE targetSelector-Goals: keine Erkennung über Sicht
    }

    /** Verfolgung des gehörten Spielers mit sehr hoher Geschwindigkeit. */
    private final class ChaseGoal extends Goal {
        private int repath;

        ChaseGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return getTarget() != null && getTarget().isAlive();
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity t = getTarget();
            if (t == null) {
                return;
            }
            if (screamTicks() > 0) {
                getNavigation().stop();
                return;
            }
            getLookControl().setLookAt(t, 30.0F, 30.0F);
            if (--repath <= 0) {
                repath = 4;
                getNavigation().moveTo(t, OfficeRoomsConfig.WARDEN_CHASE_MULTIPLIER.get());
            }
            double reach = getBbWidth() * 2.0F * getBbWidth() * 2.0F + t.getBbWidth();
            if (attackCooldown <= 0 && distanceToSqr(t) <= reach + 1.0) {
                triggerAnim("move", "attack");
                swing(InteractionHand.MAIN_HAND);
                doHurtTarget(t);
                attackCooldown = 20;
            }
        }
    }

    /** Zur letzten Geräuschquelle gehen und dort horchen. */
    private final class InvestigateGoal extends Goal {
        private Vec3 going;
        private int listen;

        InvestigateGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return getTarget() == null && noiseTarget != null && level().getGameTime() - noiseTime < 1200;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse() && listen < 60;
        }

        @Override
        public void start() {
            going = null;
            listen = 0;
        }

        @Override
        public void tick() {
            if (going != noiseTarget) {
                going = noiseTarget;
                listen = 0;
                getNavigation().moveTo(going.x, going.y, going.z, 1.15);
            }
            if (position().distanceTo(going) < 2.0 || getNavigation().isDone()) {
                listen++; // am Ziel: stehen und horchen
            }
        }

        @Override
        public void stop() {
            if (listen >= 60) {
                noiseTarget = null;
            }
        }
    }

    // -------------------------------------------------------------------------------------------
    // Sonstiges
    // -------------------------------------------------------------------------------------------

    @Override
    public boolean removeWhenFarAway(double distSq) {
        return getTarget() == null;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null; // still: der Herzschlag in seiner Nähe ist das einzige Warnsignal
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.WARDEN_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.WARDEN_DEATH.get();
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        playSound(SoundEvents.WARDEN_STEP, isChasing() ? 1.0F : 0.6F, 1.0F);
    }

    @Override
    protected float getSoundVolume() {
        return 2.0F;
    }
}
