package de.officerooms.entity;

import java.util.UUID;

import de.officerooms.core.ModSounds;
import de.officerooms.core.OfficeRoomsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 0.13.0: The Flayed. A stretched, skinless figure of black muscle, far taller than a person. It drifts slowly and
 * twitches when idle. When it sees a player it rears up and lets out an enormous, distorted scream, then sprints after
 * them faster than they can run and hits extremely hard. After {@code chaseSeconds} of continuous pursuit it falters for
 * {@code falterSeconds}: the one chance to break line of sight and get away. Extremely rare, at most one near a player.
 */
public class Flayed extends Monster implements software.bernie.geckolib.animatable.GeoEntity {

    private final software.bernie.geckolib.animatable.instance.AnimatableInstanceCache geoCache =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    private static final UUID PHASE_ID = UUID.fromString("5f0c1d2e-7a41-4c35-9a0e-0ff1ce000013");
    private static final int SCREAM_TICKS = 32;
    private static final int IDLE = 0;
    private static final int SCREAM = 1;
    private static final int CHASE = 2;
    private static final int FALTER = 3;

    private int phase = IDLE;
    /** 0.30.1: synced so the client can loop the hunting scream. */
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> DATA_HUNT =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Flayed.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_HUNT, false);
    }

    public boolean hunting() {
        return entityData.get(DATA_HUNT);
    }
    private int timer;
    private int chaseTicks;
    private LivingEntity lastTarget;

    public Flayed(EntityType<? extends Flayed> type, Level level) {
        super(type, level);
        this.xpReward = 40;
        setMaxUpStep(1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 80.0)
                .add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.ATTACK_DAMAGE, 18.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9);
    }

    @Override
    public void registerControllers(software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar controllers) {
        GeoAnims.register(this, controllers, 0.02, "scream", "falter");
    }

    @Override
    public software.bernie.geckolib.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    private void applyConfig() {
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(OfficeRoomsConfig.FLAYED_HEALTH.get());
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(OfficeRoomsConfig.FLAYED_DAMAGE.get());
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

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.45));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));   // needs line of sight
    }

    /** Speed per phase: idle as is, frozen while screaming, sprinting while chasing, crawling while it falters. */
    private void setPhase(int p) {
        phase = p;
        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        speed.removeModifier(PHASE_ID);
        double mul = switch (p) {
            case SCREAM -> -1.0;
            case CHASE -> OfficeRoomsConfig.FLAYED_CHASE_MULTIPLIER.get() - 1.0;
            case FALTER -> -0.75;
            default -> 0.0;
        };
        if (mul != 0.0) {
            speed.addTransientModifier(new AttributeModifier(PHASE_ID, "flayed phase", mul, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    public int phase() {
        return phase;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (hunting()) {
                net.neoforged.fml.DistExecutor.unsafeRunWhenOn(net.neoforged.api.distmarker.Dist.CLIENT, () -> () ->
                        de.officerooms.client.MobLoops.hunting(this, ModSounds.FLAYED_HUNT_LOOP.get(), Flayed::hunting));
            }
            return;
        }
        LivingEntity t = getTarget();
        boolean has = t != null && t.isAlive() && !(t instanceof Player pl && (pl.isCreative() || pl.isSpectator()));
        entityData.set(DATA_HUNT, has);
        if (!has) {
            if (phase != IDLE) {
                setPhase(IDLE);
            }
            chaseTicks = 0;
            lastTarget = null;
            return;
        }
        if (t != lastTarget) {                                       // discovered someone: rear up and scream
            lastTarget = t;
            setPhase(SCREAM);
            timer = SCREAM_TICKS;
            getNavigation().stop();
            triggerAnim("move", "scream");
            screamShake();                                           // 0.30.1: the voice is the client-side hunt loop
        }
        switch (phase) {
            case SCREAM -> {
                getNavigation().stop();
                getLookControl().setLookAt(t, 30.0F, 30.0F);
                if (--timer <= 0) {
                    setPhase(CHASE);
                    chaseTicks = 0;
                }
            }
            case CHASE -> {
                if (++chaseTicks >= OfficeRoomsConfig.FLAYED_CHASE_SECONDS.get() * 20) {
                    setPhase(FALTER);
                    timer = OfficeRoomsConfig.FLAYED_FALTER_SECONDS.get() * 20;
                    triggerAnim("move", "falter");
                    playSound(ModSounds.FLAYED_FALTER.get(), 2.0F, 1.0F);
                }
            }
            case FALTER -> {
                if (--timer <= 0) {
                    setPhase(CHASE);
                    chaseTicks = 0;
                    playSound(ModSounds.FLAYED_TEAR.get(), 1.5F, 0.9F);                               // 0.18.0: it pulls itself together
                    if (random.nextBoolean()) {
                        screamShake();
                    }
                }
            }
            default -> setPhase(CHASE);
        }
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        if (phase == SCREAM) {
            return false;
        }
        playSound(ModSounds.FLAYED_TEAR.get(), 1.2F, 0.9F + random.nextFloat() * 0.2F);             // 0.18.0
        return super.doHurtTarget(target);
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return phase == IDLE ? ModSounds.FLAYED_AMBIENT.get() : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.FLAYED_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.FLAYED_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(ModSounds.FLAYED_STEP.get(), 0.5F, 0.8F + random.nextFloat() * 0.2F);
    }

    @Override
    public boolean removeWhenFarAway(double distSq) {
        return true;
    }

    /** 0.26.0: the scream shakes the camera of nearby players (strong up close, fading to 32 blocks). */
    private void screamShake() {
        for (net.minecraft.world.entity.player.Player pl : level().players()) {
            if (pl instanceof net.minecraft.server.level.ServerPlayer sp && !sp.isSpectator()) {
                double d = Math.sqrt(sp.distanceToSqr(this));
                if (d < 32) {
                    float strength = (float) (0.9 * (1.0 - d / 32.0)) + 0.15F;
                    de.officerooms.hazard.HazardNet.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                            new de.officerooms.hazard.HazardNet.Quake(SCREAM_TICKS, strength));
                }
            }
        }
    }
}
