package de.officerooms.sea;

import de.officerooms.core.ModSounds;
import de.officerooms.core.OfficeRoomsConfig;
import de.officerooms.entity.GeoAnims;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 0.42.0: the Mascot of the SS Fun =). An old kids' club costume, soaked through and swollen, with a face that is only
 * "=)": two slits and a stitched smile that does not move. Something is still wearing it.
 *
 * <ul>
 *     <li><b>In the vents:</b> invisible above a ceiling vent. Water drips, something squeaks like a rubber toy.</li>
 *     <li><b>The drop:</b> walk under its vent and it falls out with a honk, right behind you.</li>
 *     <li><b>Waving:</b> sometimes it just stands at the end of a corridor and waves. Look away and it is closer. Get too
 *     close, or stare too long, and it comes running on all fours.</li>
 *     <li><b>The hug:</b> it grabs you and does not let go: you are slowed, it giggles, it squeezes. Hit it to break free.</li>
 *     <li><b>Back up:</b> lose it and it climbs back into the nearest vent.</li>
 * </ul>
 */
public class Mascot extends Monster implements software.bernie.geckolib.animatable.GeoEntity {

    public static final int LURK = 0, WAVE = 1, HUNT = 2;
    private static final EntityDataAccessor<Integer> DATA_MODE = SynchedEntityData.defineId(Mascot.class, EntityDataSerializers.INT);

    private final software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache geoCache =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    private int squeakTimer = 80;
    private int stared;
    private int lostTicks;
    private int hugTicks;
    private int hugCooldown;
    private int giggleTimer;
    private LivingEntity hugged;

    public Mascot(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 25;
        if (!level.isClientSide) {
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(OfficeRoomsConfig.MASCOT_HEALTH.get());
            setHealth(getMaxHealth());
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 50.0)
                .add(Attributes.MOVEMENT_SPEED, 0.33)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_MODE, LURK);
    }

    public int mode() {
        return entityData.get(DATA_MODE);
    }

    public boolean hugging() {
        return hugTicks > 0;
    }

    void setMode(int m) {
        entityData.set(DATA_MODE, m);
        boolean hidden = m == LURK;
        setInvisible(hidden);
        setNoAi(m != HUNT);
        setSilent(hidden);
        noPhysics = hidden;
        setNoGravity(hidden);
        if (m != HUNT) {
            setTarget(null);
            setDeltaMovement(Vec3.ZERO);
        }
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.35, true));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers) {
        var c = new software.bernie.geckolib.core.animation.AnimationController<>(this, "move", 4, state -> {
            if (mode() == WAVE) {
                return state.setAndContinue(software.bernie.geckolib.core.animation.RawAnimation.begin().thenLoop("wave"));
            }
            if (hugTicks > 0) {
                return state.setAndContinue(software.bernie.geckolib.core.animation.RawAnimation.begin().thenLoop("hug"));
            }
            if (state.isMoving()) {
                return state.setAndContinue(isAggressive() ? GeoAnims.RUN : GeoAnims.WALK);
            }
            return state.setAndContinue(GeoAnims.IDLE);
        });
        c.triggerableAnim("drop", software.bernie.geckolib.core.animation.RawAnimation.begin().thenPlay("drop"));
        controllers.add(c);
    }

    @Override
    public software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType reason, SpawnGroupData data, CompoundTag tag) {
        SpawnGroupData r = super.finalizeSpawn(level, diff, reason, data, tag);
        if (reason == MobSpawnType.SPAWN_EGG || reason == MobSpawnType.COMMAND) {
            setMode(HUNT);
        }
        return r;
    }

    // --- server logic -------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (random.nextInt(mode() == LURK ? 30 : 6) == 0) {         // it is always dripping
                level().addParticle(ParticleTypes.DRIPPING_WATER, getX() + random.nextGaussian() * 0.3, getY() + (mode() == LURK ? 3.0 : 1.0 + random.nextDouble()),
                        getZ() + random.nextGaussian() * 0.3, 0, 0, 0);
            }
            return;
        }
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        if (hugCooldown > 0) {
            hugCooldown--;
        }
        switch (mode()) {
            case LURK -> lurk(sl);
            case WAVE -> wave(sl);
            default -> hunt(sl);
        }
    }

    private Player nearest(ServerLevel sl, double r) {
        return sl.getNearestPlayer(getX(), getY(), getZ(), r, e -> e instanceof Player p && !p.isCreative() && !p.isSpectator());
    }

    private void lurk(ServerLevel sl) {
        if (!isInvisible() || !isNoAi()) {
            setMode(LURK);
        }
        setDeltaMovement(Vec3.ZERO);
        Player near = nearest(sl, 24);
        if (near == null) {
            return;
        }
        double flat = Math.hypot(near.getX() - getX(), near.getZ() - getZ());
        if (flat < 2.0 && Math.abs(near.getY() - getY()) < 2.0) {
            drop(sl, near);
            return;
        }
        if (--squeakTimer <= 0) {
            squeakTimer = flat < 10 ? 50 + random.nextInt(70) : 140 + random.nextInt(180);
            sl.playSound(null, getX(), getY() + 3, getZ(), ModSounds.MASCOT_SQUEAK.get(), SoundSource.HOSTILE, flat < 10 ? 1.0F : 0.7F,
                    0.8F + random.nextFloat() * 0.4F);
        }
    }

    /** Out of the vent: break it, land behind the player, honk. */
    void drop(ServerLevel sl, Player victim) {
        BlockPos vent = blockPosition().above(3);
        for (int up = 2; up <= 4; up++) {
            BlockPos q = blockPosition().above(up);
            if (sl.getBlockState(q).is(ShipBlocks.VENT.get())) {
                vent = q;
                sl.setBlock(q, ShipBlocks.VENT_BROKEN.get().defaultBlockState(), 3);
                sl.levelEvent(2001, q, net.minecraft.world.level.block.Block.getId(ShipBlocks.VENT.get().defaultBlockState()));
            }
        }
        Vec3 look = victim.getLookAngle().multiply(1, 0, 1);
        if (look.lengthSqr() > 1e-4) {
            Vec3 behind = victim.position().subtract(look.normalize().scale(1.8));
            if (sl.noCollision(this, getBoundingBox().move(behind.x - getX(), behind.y - getY(), behind.z - getZ()))) {
                setPos(behind.x, behind.y, behind.z);
            }
        }
        setMode(HUNT);
        setTarget(victim);
        lostTicks = 0;
        triggerAnim("move", "drop");
        sl.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 2, getZ(), 30, 0.4, 0.6, 0.4, 0.1);
        sl.playSound(null, getX(), getY(), getZ(), ModSounds.MASCOT_HONK.get(), SoundSource.HOSTILE, 3.0F, 0.9F + random.nextFloat() * 0.15F);
        quake(sl, 0.8F);
    }

    void quake(ServerLevel sl, float s) {
        for (ServerPlayer p : sl.players()) {
            double d = p.distanceTo(this);
            if (d < 20) {
                de.officerooms.hazard.HazardNet.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> p),
                        new de.officerooms.hazard.HazardNet.Quake(16, (float) Math.max(0.2, s * (1 - d / 20))));
            }
        }
    }

    /** Standing at the end of a corridor, waving. It only moves while nobody looks at it. */
    private void wave(ServerLevel sl) {
        Player near = nearest(sl, 48);
        if (near == null) {
            if (++lostTicks > 1200) {
                discard();
            }
            return;
        }
        lostTicks = 0;
        getLookControl().setLookAt(near, 30, 30);
        setYRot((float) (Math.toDegrees(Math.atan2(near.getZ() - getZ(), near.getX() - getX())) - 90));
        setYBodyRot(getYRot());
        setYHeadRot(getYRot());
        double d = distanceTo(near);
        boolean seen = near.hasLineOfSight(this) && looksAt(near);
        if (seen) {
            stared++;
        }
        if (d < 12 || stared > 70) {
            setMode(HUNT);
            setTarget(near);
            sl.playSound(null, getX(), getY(), getZ(), ModSounds.MASCOT_GIGGLE.get(), SoundSource.HOSTILE, 2.5F, 0.7F);
            return;
        }
        if (!seen && tickCount % 40 == 0) {                     // closer every time you look back
            Vec3 dir = near.position().subtract(position()).multiply(1, 0, 1).normalize();
            Vec3 to = position().add(dir.scale(3 + random.nextInt(3)));
            if (sl.noCollision(this, getBoundingBox().move(to.x - getX(), 0, to.z - getZ())) && near.position().distanceTo(to) > 9) {
                setPos(to.x, getY(), to.z);
                if (random.nextInt(3) == 0) {
                    sl.playSound(null, getX(), getY(), getZ(), ModSounds.MASCOT_STEP.get(), SoundSource.HOSTILE, 0.8F, 0.7F);
                }
            }
        }
    }

    private boolean looksAt(Player p) {
        Vec3 view = p.getViewVector(1.0F).normalize();
        Vec3 to = new Vec3(getX() - p.getX(), getEyeY() - p.getEyeY(), getZ() - p.getZ());
        double len = to.length();
        return len > 0 && view.dot(to.scale(1 / len)) > 0.96;
    }

    private void hunt(ServerLevel sl) {
        LivingEntity t = getTarget();
        boolean has = t != null && t.isAlive() && !(t instanceof Player p && (p.isCreative() || p.isSpectator()));
        if (hugTicks > 0) {
            hug(sl);
            return;
        }
        if (tickCount % 10 == 0) {                              // the lights it passes go out
            BlockPos b = blockPosition();
            for (int up = 2; up <= 4; up++) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos q = b.offset(dx, up, dz);
                        BlockState s = sl.getBlockState(q);
                        if (s.getBlock() instanceof ShipBlocks.Lamp && s.getValue(ShipBlocks.Lamp.LIT)) {
                            sl.setBlock(q, s.setValue(ShipBlocks.Lamp.LIT, false), 3);
                            sl.playSound(null, q, net.minecraft.sounds.SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.5F, 1.5F);
                        }
                    }
                }
            }
        }
        if (has) {
            lostTicks = 0;
            if (--giggleTimer <= 0) {
                giggleTimer = 100 + random.nextInt(120);
                sl.playSound(null, getX(), getY(), getZ(), ModSounds.MASCOT_GIGGLE.get(), SoundSource.HOSTILE, 2.0F, 0.85F + random.nextFloat() * 0.25F);
            }
            return;
        }
        if (++lostTicks > 200) {
            Player seen = nearest(sl, 14);
            if (seen == null || !seen.hasLineOfSight(this)) {
                backIntoVent(sl);
            }
        }
    }

    private void backIntoVent(ServerLevel sl) {
        BlockPos b = blockPosition();
        for (int r = 0; r <= 8; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    for (int up = 2; up <= 5; up++) {
                        BlockPos q = b.offset(dx, up, dz);
                        if (sl.getBlockState(q).is(ShipBlocks.VENT.get()) || sl.getBlockState(q).is(ShipBlocks.VENT_BROKEN.get())) {
                            sl.playSound(null, q, ModSounds.MASCOT_SQUEAK.get(), SoundSource.HOSTILE, 1.0F, 0.6F);
                            setPos(q.getX() + 0.5, q.getY() - 3, q.getZ() + 0.5);
                            setMode(LURK);
                            lostTicks = 0;
                            return;
                        }
                    }
                }
            }
        }
        discard();
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        if (hugCooldown <= 0 && target instanceof LivingEntity le && level() instanceof ServerLevel sl) {
            hugged = le;
            hugTicks = 50;
            hugCooldown = 120;
            sl.playSound(null, getX(), getY(), getZ(), ModSounds.MASCOT_GIGGLE.get(), SoundSource.HOSTILE, 2.5F, 1.1F);
            return true;
        }
        return super.doHurtTarget(target);
    }

    /** Holding on: slowed, squeezed, giggled at. */
    private void hug(ServerLevel sl) {
        LivingEntity v = hugged;
        if (v == null || !v.isAlive() || v.distanceTo(this) > 4) {
            hugTicks = 0;
            return;
        }
        hugTicks--;
        Vec3 front = v.position().add(v.getLookAngle().multiply(1, 0, 1).normalize().scale(0.6));
        setPos(front.x, v.getY(), front.z);
        getLookControl().setLookAt(v, 90, 90);
        v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 4, false, false));
        v.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, false, false));
        if (hugTicks % 12 == 0) {
            v.invulnerableTime = 0;
            v.hurt(damageSources().mobAttack(this), OfficeRoomsConfig.MASCOT_DAMAGE.get().floatValue());
            sl.sendParticles(ParticleTypes.FALLING_WATER, v.getX(), v.getY() + 1.6, v.getZ(), 8, 0.3, 0.3, 0.3, 0);
        }
        if (hugTicks == 0) {
            v.knockback(0.8, getX() - v.getX(), getZ() - v.getZ());
        }
    }

    // --- damage, sounds, saving ---------------------------------------------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (mode() == LURK && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        if (hugTicks > 0 && source.getEntity() == hugged) {
            hugTicks = 0;                                       // hitting it makes it let go
        }
        if (mode() == WAVE && source.getEntity() instanceof Player p) {
            setMode(HUNT);
            setTarget(p);
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean isPickable() {
        return mode() != LURK && super.isPickable();
    }

    @Override
    public boolean isPushable() {
        return mode() == HUNT && super.isPushable();
    }

    @Override
    public boolean causeFallDamage(float dist, float mul, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return d > 96 * 96;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return mode() == LURK ? null : ModSounds.MASCOT_SQUEAK.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.MASCOT_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.MASCOT_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(ModSounds.MASCOT_STEP.get(), 0.7F, 0.8F + random.nextFloat() * 0.3F);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Mode", mode());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Mode")) {
            setMode(tag.getInt("Mode"));
        }
    }
}
