package de.officerooms.entity;

import de.officerooms.core.ModSounds;
import de.officerooms.core.OfficeRoomsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
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
import net.minecraft.nbt.CompoundTag;

/**
 * Wolfspider: Spinnenkörper mit Wolfskopf, vollständig schwarz.
 *
 * <p>Wandert selbstständig durch Räume und Korridore (Boden-Navigation, öffnet keine Türen, passt
 * mit 0,9 Blöcken Breite durch 1 Block breite Türöffnungen), erkennt Spieler normal über Sicht und
 * greift an. Jeder Treffer verursacht normalen Schaden plus einen konfigurierbaren Wither-Effekt.
 * Ihr verzerrtes, rückwärts klingendes Jaulen ertönt in unregelmäßigen Abständen.</p>
 */
public class Wolfspider extends Monster {

    public Wolfspider(EntityType<? extends Wolfspider> type, Level level) {
        super(type, level);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    /** Werte aus der Konfiguration übernehmen (beim Spawnen und Laden). */
    void applyConfig() {
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(OfficeRoomsConfig.WOLFSPIDER_HEALTH.get());
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(OfficeRoomsConfig.WOLFSPIDER_DAMAGE.get());
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(OfficeRoomsConfig.WOLFSPIDER_SPEED.get());
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
        goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.35F));
        goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (!super.doHurtTarget(target)) {
            return false;
        }
        playSound(ModSounds.WOLFSPIDER_ATTACK.get(), 1.0F, 0.85F + random.nextFloat() * 0.3F);       // 0.18.0
        int seconds = OfficeRoomsConfig.WOLFSPIDER_WITHER_SECONDS.get();
        if (seconds > 0 && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.WITHER, seconds * 20, OfficeRoomsConfig.WOLFSPIDER_WITHER_LEVEL.get()), this);
        }
        return true;
    }

    @Override
    public MobType getMobType() {
        return MobType.ARTHROPOD;
    }

    @Override
    public int getAmbientSoundInterval() {
        // Mob würfelt jede Sekunde neu; zusammen mit diesem Wert entstehen unregelmäßige Abstände von grob 10–40 s.
        return 360;
    }

    @Override
    protected float getSoundVolume() {
        return 0.45F; // deutlich leiser (Update „Liminal World“, vorher 1.6)
    }

    @Override
    public float getVoicePitch() {
        return 0.85F + random.nextFloat() * 0.3F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.WOLFSPIDER_HOWL.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.WOLFSPIDER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.WOLFSPIDER_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(ModSounds.WOLFSPIDER_STEP.get(), 0.35F, 0.85F + random.nextFloat() * 0.3F);
    }

    @Override
    public boolean removeWhenFarAway(double distSq) {
        return true;
    }
}
