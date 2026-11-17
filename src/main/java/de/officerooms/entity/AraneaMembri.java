package de.officerooms.entity;

import de.officerooms.block.CeilingLightBlock;
import de.officerooms.core.ModSounds;
import de.officerooms.core.OfficeRoomsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 0.32.0: Aranea Membri (a community Backrooms entity, modelled for this mod). It nests in the ceiling.
 *
 * <ul>
 *     <li><b>Lurking:</b> invisible up under the ceiling, it cannot be hit. You only hear scratching above you, see dust
 *     trickling down and find dark holes in the ceiling tiles nearby. The closer you are, the more it scratches.</li>
 *     <li><b>Drop:</b> walk underneath it and the ceiling bursts, the lamp falls with it, it lands behind you and
 *     screeches (camera shake).</li>
 *     <li><b>Hunt:</b> fast and heavy; the ceiling lights it passes under die.</li>
 *     <li><b>Retreat:</b> lose it for a while and it climbs back into the ceiling where it stands, and waits.</li>
 * </ul>
 */
public class AraneaMembri extends Monster implements software.bernie.geckolib.animatable.GeoEntity {

    private static final EntityDataAccessor<Boolean> DATA_LURK = SynchedEntityData.defineId(AraneaMembri.class, EntityDataSerializers.BOOLEAN);

    private final software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache geoCache =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    private boolean anchored;
    private int dropCooldown;
    private int lostTicks;
    private int scratchTimer = 60;
    private int screechTimer;

    public AraneaMembri(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 20;
        if (!level.isClientSide) {
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(OfficeRoomsConfig.ARANEA_HEALTH.get());
            getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(OfficeRoomsConfig.ARANEA_DAMAGE.get());
            setHealth(getMaxHealth());
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 70.0)
                .add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_LURK, true);
    }

    public boolean lurking() {
        return entityData.get(DATA_LURK);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, true));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.55));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers) {
        GeoAnims.register(this, controllers, 0.03, "screech", "attack");
    }

    @Override
    public software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType reason, SpawnGroupData data, CompoundTag tag) {
        SpawnGroupData r = super.finalizeSpawn(level, diff, reason, data, tag);
        if (reason == MobSpawnType.SPAWN_EGG) {
            setLurking(false);                                   // from an egg it is awake right away
        }
        return r;
    }

    private void setLurking(boolean l) {
        entityData.set(DATA_LURK, l);
        setInvisible(l);
        setNoAi(l);
        setNoGravity(l);
        setSilent(l);
        noPhysics = l;
        if (l) {
            setTarget(null);
            setDeltaMovement(Vec3.ZERO);
            anchored = false;
        }
    }

    // --- server logic -------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !(level() instanceof ServerLevel sl)) {
            return;
        }
        if (lurking()) {
            lurk(sl);
        } else {
            hunt(sl);
        }
    }

    /** Up under the ceiling: anchor once, scratch, trickle dust, wait for someone to walk underneath. */
    private void lurk(ServerLevel sl) {
        if (!lurking() || isNoAi() != lurking()) {
            setLurking(true);
        }
        if (!anchored) {
            anchor(sl);
        }
        setDeltaMovement(Vec3.ZERO);
        if (dropCooldown > 0) {
            dropCooldown--;
        }
        Player near = sl.getNearestPlayer(getX(), getY(), getZ(), 24, e -> e instanceof Player p && !p.isCreative() && !p.isSpectator());
        if (near == null) {
            return;
        }
        double dx = near.getX() - getX();
        double dz = near.getZ() - getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);
        boolean below = near.getY() < getY() + 0.5 && near.getY() > getY() - 7;
        if (dropCooldown <= 0 && below && flat < 2.4) {
            drop(sl, near);
            return;
        }
        if (--scratchTimer <= 0) {
            scratchTimer = flat < 10 ? 40 + random.nextInt(60) : 120 + random.nextInt(160);
            BlockPos ceil = BlockPos.containing(getX(), getY() + getBbHeight() + 0.2, getZ());
            sl.playSound(null, getX(), getY() + getBbHeight(), getZ(), ModSounds.ARANEA_SCRATCH.get(), SoundSource.HOSTILE,
                    flat < 10 ? 1.2F : 0.8F, 0.85F + random.nextFloat() * 0.3F);
            BlockState cs = sl.getBlockState(ceil);
            if (!cs.isAir()) {
                sl.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, cs), getX(), ceil.getY() - 0.05, getZ(),
                        6 + random.nextInt(8), 0.7, 0.0, 0.7, 0.0);
            }
        }
    }

    /** Moves up until the hitbox touches the ceiling and leaves a hole or two in the tiles around it. */
    private void anchor(ServerLevel sl) {
        anchored = true;
        BlockPos base = blockPosition();
        for (int up = 0; up <= 12; up++) {
            BlockPos q = base.above(up);
            if (!sl.getBlockState(q).getCollisionShape(sl, q).isEmpty() && up > 0) {
                setPos(getX(), q.getY() - getBbHeight() - 0.01, getZ());
                if (sl.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
                    for (int k = 0; k < 2; k++) {
                        BlockPos h = q.offset(random.nextInt(7) - 3, 0, random.nextInt(7) - 3);
                        BlockState hs = sl.getBlockState(h);
                        if (!hs.isAir() && !(hs.getBlock() instanceof CeilingLightBlock) && hs.getDestroySpeed(sl, h) >= 0
                                && hs.isCollisionShapeFullBlock(sl, h) && sl.getBlockState(h.below()).isAir()) {
                            sl.setBlock(h, de.officerooms.block.ModBlocks.CEILING_HOLE.get().defaultBlockState(), 3);
                        }
                    }
                }
                return;
            }
        }
    }

    private void drop(ServerLevel sl, Player victim) {
        BlockPos ceil = BlockPos.containing(getX(), getY() + getBbHeight() + 0.2, getZ());
        boolean grief = sl.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        // the ceiling bursts and the lamps right there come down with it
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos q = ceil.offset(dx, 0, dz);
                BlockState s = sl.getBlockState(q);
                if (s.isAir() || s.getDestroySpeed(sl, q) < 0) {
                    continue;
                }
                boolean centre = dx == 0 && dz == 0;
                if (grief && (centre || s.getBlock() instanceof CeilingLightBlock || s.is(de.officerooms.block.ModBlocks.CEILING_HOLE.get()))) {
                    sl.destroyBlock(q, false, this);
                } else {
                    sl.levelEvent(2001, q, net.minecraft.world.level.block.Block.getId(s));
                }
            }
        }
        // land behind the victim when there is room
        Vec3 look = victim.getLookAngle().multiply(1, 0, 1);
        if (look.lengthSqr() > 1e-4) {
            Vec3 behind = victim.position().subtract(look.normalize().scale(2.2));
            AABB box = getBoundingBox().move(behind.x - getX(), 0, behind.z - getZ());
            if (sl.noCollision(this, box)) {
                setPos(behind.x, getY(), behind.z);
            }
        }
        setLurking(false);
        dropCooldown = 0;
        lostTicks = 0;
        screechTimer = 160 + random.nextInt(120);
        setTarget(victim);
        triggerAnim("move", "screech");
        sl.playSound(null, getX(), getY(), getZ(), ModSounds.ARANEA_SCREECH.get(), SoundSource.HOSTILE, 4.0F, 0.95F + random.nextFloat() * 0.1F);
        for (ServerPlayer p : sl.players()) {
            double d = p.distanceTo(this);
            if (d < 24) {
                float strength = (float) Math.max(0.25, 1.0 - d / 24.0);
                de.officerooms.hazard.HazardNet.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> p),
                        new de.officerooms.hazard.HazardNet.Quake(24, strength));
            }
        }
    }

    /** Awake: kill the lights it passes, screech now and then, go back up when nobody is around any more. */
    private void hunt(ServerLevel sl) {
        LivingEntity t = getTarget();
        boolean has = t != null && t.isAlive() && !(t instanceof Player p && (p.isCreative() || p.isSpectator()));
        if (tickCount % 10 == 0) {
            BlockPos b = blockPosition();
            for (int up = 2; up <= 6; up++) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos q = b.offset(dx, up, dz);
                        BlockState s = sl.getBlockState(q);
                        if (s.getBlock() instanceof CeilingLightBlock && s.hasProperty(CeilingLightBlock.LIT) && s.getValue(CeilingLightBlock.LIT)) {
                            sl.setBlock(q, s.setValue(CeilingLightBlock.LIT, false), 3);
                            sl.playSound(null, q, net.minecraft.sounds.SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.6F, 1.4F);
                        }
                    }
                }
            }
        }
        if (has) {
            lostTicks = 0;
            if (--screechTimer <= 0) {
                screechTimer = 160 + random.nextInt(120);
                sl.playSound(null, getX(), getY(), getZ(), ModSounds.ARANEA_SCREECH.get(), SoundSource.HOSTILE, 2.5F, 0.85F + random.nextFloat() * 0.2F);
            }
            return;
        }
        if (++lostTicks > 200) {
            Player seen = sl.getNearestPlayer(this, 12);
            if (seen == null || !seen.hasLineOfSight(this)) {
                sl.playSound(null, getX(), getY() + 2, getZ(), ModSounds.ARANEA_SCRATCH.get(), SoundSource.HOSTILE, 1.0F, 0.7F);
                setLurking(true);
                dropCooldown = 600;
                lostTicks = 0;
            }
        }
    }

    // --- damage, sounds, saving ---------------------------------------------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (lurking() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean isPickable() {
        return !lurking() && super.isPickable();
    }

    @Override
    public boolean isPushable() {
        return !lurking() && super.isPushable();
    }

    @Override
    public boolean causeFallDamage(float dist, float mul, DamageSource source) {
        return false;
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        triggerAnim("move", "attack");
        return super.doHurtTarget(target);
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return lurking() ? null : ModSounds.ARANEA_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ARANEA_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ARANEA_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(ModSounds.ARANEA_STEP.get(), 0.6F, 0.8F + random.nextFloat() * 0.3F);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Lurking", lurking());
        tag.putBoolean("Anchored", anchored);
        tag.putInt("DropCooldown", dropCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Lurking")) {
            setLurking(tag.getBoolean("Lurking"));
        }
        anchored = tag.getBoolean("Anchored");
        dropCooldown = tag.getInt("DropCooldown");
    }
}
