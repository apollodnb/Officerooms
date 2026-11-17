package de.officerooms.entity;

import de.officerooms.core.ModSounds;
import de.officerooms.core.OfficeRoomsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
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

import java.util.UUID;

/**
 * The Colony (0.6.0): eigenes, extrem seltenes Wesen – eine kriechende, vielbeinige Masse aus blassem,
 * krankem Wuchs mit leuchtenden Flecken. Schreit, sobald es eine Spur hat, und jagt dann unerbittlich
 * (findet Spieler auch ohne Sichtkontakt). Nach {@code chaseSeconds} ununterbrochener Jagd (Standard 10 s)
 * gerät es ins Stocken: Es zittert und ist {@code falterSeconds} lang langsam – die Chance zur Flucht.
 */
public class Colony extends Monster implements software.bernie.geckolib.animatable.GeoEntity {

    // --- 0.11.0: GeckoLib model and animations (assets from tools/gen_mobs_geo.py) --------------
    private final software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache geoCache =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    @Override
    public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers) {
        GeoAnims.register(this, controllers, 0.02, "scream");
    }

    @Override
    public software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    private static final EntityDataAccessor<Boolean> DATA_FALTER = SynchedEntityData.defineId(Colony.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HUNTING = SynchedEntityData.defineId(Colony.class, EntityDataSerializers.BOOLEAN);
    private static final UUID FALTER_ID = UUID.fromString("8f1d2a8e-4c43-4f3e-9a8a-0c0101060060");
    private static final AttributeModifier FALTER = new AttributeModifier(FALTER_ID, "colony falter", -0.75,
            AttributeModifier.Operation.MULTIPLY_TOTAL);

    private LivingEntity lastTarget;
    private int chaseTicks;
    private int falterTicks;
    private int screamCooldown;

    public Colony(EntityType<? extends Colony> type, Level level) {
        super(type, level);
        this.xpReward = 40;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 80.0)
                .add(Attributes.MOVEMENT_SPEED, 0.17)
                .add(Attributes.ATTACK_DAMAGE, 14.0)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9);
    }

    void applyConfig() {
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(OfficeRoomsConfig.COLONY_HEALTH.get());
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(OfficeRoomsConfig.COLONY_DAMAGE.get());
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(OfficeRoomsConfig.COLONY_SPEED.get());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_FALTER, false);
        entityData.define(DATA_HUNTING, false);
    }

    public boolean isFaltering() {
        return entityData.get(DATA_FALTER);
    }

    public boolean isHunting() {
        return entityData.get(DATA_HUNTING);
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
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.6, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // findet Spieler auch ohne Sichtkontakt (unerbittliche Verfolgung)
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false, null));
    }

    /** Anteil der Jagdzeit bis zum Stocken (für Tests und Anzeige). */
    public int chaseTicks() {
        return chaseTicks;
    }

    public int falterTicks() {
        return falterTicks;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            return;
        }
        LivingEntity t = getTarget();
        boolean hunting = t != null && t.isAlive();
        entityData.set(DATA_HUNTING, hunting);
        if (screamCooldown > 0) {
            screamCooldown--;
        }
        if (hunting && t != lastTarget && screamCooldown == 0) {
            triggerAnim("move", "scream");
            playSound(ModSounds.COLONY_SCREAM.get(), 4.0F, 0.9F + random.nextFloat() * 0.2F);
            screamCooldown = 120;
        }
        lastTarget = hunting ? t : null;
        var speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (falterTicks > 0) {
            if (--falterTicks == 0) {
                speed.removeModifier(FALTER_ID);
                entityData.set(DATA_FALTER, false);
                chaseTicks = 0;
                if (hunting) {
                    triggerAnim("move", "scream");
                    playSound(ModSounds.COLONY_SCREAM.get(), 4.0F, 1.1F);
                    screamCooldown = 120;
                }
            }
            return;
        }
        if (hunting) {
            chaseTicks++;
            if (chaseTicks >= OfficeRoomsConfig.COLONY_CHASE_SECONDS.get() * 20) {
                // stockt: zittert, wird langsam
                falterTicks = OfficeRoomsConfig.COLONY_FALTER_SECONDS.get() * 20;
                if (!speed.hasModifier(FALTER)) {
                    speed.addTransientModifier(FALTER);
                }
                entityData.set(DATA_FALTER, true);
                playSound(ModSounds.COLONY_CHITTER.get(), 2.0F, 0.7F);
            } else if (screamCooldown == 0 && random.nextInt(140) == 0) {
                triggerAnim("move", "scream");
                playSound(ModSounds.COLONY_SCREAM.get(), 3.5F, 0.8F + random.nextFloat() * 0.3F);
                screamCooldown = 100;
            }
        } else {
            chaseTicks = 0;
        }
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.COLONY_CHITTER.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.COLONY_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.COLONY_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(ModSounds.COLONY_STEP.get(), 0.4F, 0.8F + random.nextFloat() * 0.4F);
    }

    @Override
    public boolean removeWhenFarAway(double distSq) {
        return true;
    }
}
