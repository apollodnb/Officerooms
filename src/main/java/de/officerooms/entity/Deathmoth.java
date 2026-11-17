package de.officerooms.entity;

import de.officerooms.core.ModSounds;
import de.officerooms.core.OfficeRoomsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 0.24.0: The Deathmoth. A large, dusty moth that sits motionless on dark walls. When a player comes close it bursts
 * off the wall, flies at the player's face, clings there and drains health for a few seconds, then leaves.
 * It never emits light and never chases for long.
 */
public class Deathmoth extends Monster {

    public static final int WALL = 0, FLY = 1, ATTACHED = 2, LEAVE = 3;
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(Deathmoth.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WALL_DIR = SynchedEntityData.defineId(Deathmoth.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET = SynchedEntityData.defineId(Deathmoth.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FOLDED = SynchedEntityData.defineId(Deathmoth.class, EntityDataSerializers.BOOLEAN);

    private int stateTicks;
    /** Despawn time while still waiting on the wall (director spawns only; 0 = never). */
    private int idleLimit;
    private Vec3 lastPos = Vec3.ZERO;
    private int stuck;

    public Deathmoth(EntityType<? extends Deathmoth> type, Level level) {
        super(type, level);
        setNoGravity(true);
        xpReward = 6;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(STATE, WALL);
        entityData.define(WALL_DIR, -1);
        entityData.define(TARGET, -1);
        entityData.define(FOLDED, false);
    }

    public int state() {
        return entityData.get(STATE);
    }

    public int targetId() {
        return entityData.get(TARGET);
    }

    public boolean folded() {
        return entityData.get(FOLDED);
    }

    /** Direction pointing away from the wall, or null. */
    public Direction wallNormal() {
        int d = entityData.get(WALL_DIR);
        return d < 0 ? null : Direction.from2DDataValue(d);
    }

    public int stateTicks() {
        return stateTicks;
    }

    private void setState(int s) {
        entityData.set(STATE, s);
        stateTicks = 0;
    }

    /** Sits on the wall behind the air block {@code air}; {@code normal} points from the wall into the air block. */
    public void attachTo(BlockPos air, Direction normal, int idleTicks) {
        entityData.set(WALL_DIR, normal.get2DDataValue());
        entityData.set(FOLDED, random.nextInt(3) == 0);
        idleLimit = idleTicks;
        double x = air.getX() + 0.5 - normal.getStepX() * 0.14;
        double z = air.getZ() + 0.5 - normal.getStepZ() * 0.14;
        moveTo(x, air.getY() + 0.2 + random.nextDouble() * 0.4, z, normal.toYRot(), 0);
        setYHeadRot(normal.toYRot());
        setYBodyRot(normal.toYRot());
        setState(WALL);
    }

    private boolean configured;

    private void configure() {
        configured = true;
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(OfficeRoomsConfig.DEATHMOTH_HEALTH.get());
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(OfficeRoomsConfig.DEATHMOTH_BITE.get());
        if (getHealth() > getMaxHealth() || getHealth() >= 15.99F) {
            setHealth(getMaxHealth());
        }
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return state() != WALL;
    }

    @Override
    protected void doPush(Entity e) {
    }

    @Override
    public boolean causeFallDamage(float d, float m, DamageSource s) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState s, BlockPos p) {
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return state() == WALL || state() == LEAVE;
    }

    @Override
    protected void registerGoals() {
        // the Deathmoth is driven by its own state machine (see tick)
    }

    private Player findVictim(double range) {
        Player best = null;
        double bd = range * range;
        for (Player p : level().players()) {
            if (p.isSpectator() || p.isCreative() || !p.isAlive()) {
                continue;
            }
            double d = p.distanceToSqr(this);
            if (d < bd && (d < 6.25 || sees(p))) {
                best = p;
                bd = d;
            }
        }
        return best;
    }

    /** Line of sight from the body (a little away from the wall) to the player's eyes. */
    private boolean sees(Player p) {
        Direction n = wallNormal();
        Vec3 from = position().add(n == null ? 0 : n.getStepX() * 0.3, getBbHeight() / 2, n == null ? 0 : n.getStepZ() * 0.3);
        return level().clip(new net.minecraft.world.level.ClipContext(from, p.getEyePosition(), net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, this)).getType() == net.minecraft.world.phys.HitResult.Type.MISS;
    }

    private void trigger(Player p) {
        entityData.set(TARGET, p.getId());
        setState(FLY);
        Direction n = wallNormal();
        Vec3 push = n == null ? new Vec3(0, 0.3, 0) : new Vec3(n.getStepX() * 0.5, 0.15, n.getStepZ() * 0.5);
        setDeltaMovement(push);
        playSound(ModSounds.DEATHMOTH_TRIGGER.get(), 1.4F, 0.9F + random.nextFloat() * 0.2F);
    }

    private Player target() {
        Entity e = level().getEntity(targetId());
        return e instanceof Player p && p.isAlive() && !p.isSpectator() ? p : null;
    }

    @Override
    public void tick() {
        noPhysics = state() == ATTACHED;
        super.tick();
        if (level().isClientSide) {
            stateTicks++;
            return;
        }
        if (!configured) {
            configure();
        }
        stateTicks++;
        switch (state()) {
            case WALL -> tickWall();
            case FLY -> tickFly();
            case ATTACHED -> tickAttached();
            default -> tickLeave();
        }
    }

    private void tickWall() {
        setDeltaMovement(Vec3.ZERO);
        Direction n = wallNormal();
        if (n != null) {
            setYRot(n.toYRot());
            setYBodyRot(n.toYRot());
            setYHeadRot(n.toYRot());
        }
        if (tickCount % 5 == 0) {
            Player p = findVictim(OfficeRoomsConfig.DEATHMOTH_RANGE.get());
            if (p != null) {
                trigger(p);
                return;
            }
        }
        if (n != null && tickCount % 40 == 0) {
            BlockPos wall = BlockPos.containing(getX(), getY() + 0.2, getZ()).relative(n.getOpposite());
            if (!level().getBlockState(wall).isFaceSturdy(level(), wall, n)) {
                entityData.set(WALL_DIR, -1);                     // the wall is gone: just hang in the air
            }
        }
        if (random.nextInt(520) == 0 && level().getNearestPlayer(this, 14) != null) {
            playSound(ModSounds.DEATHMOTH_IDLE.get(), 0.35F, 0.9F + random.nextFloat() * 0.2F);
        }
        if (idleLimit > 0 && stateTicks > idleLimit && level().getNearestPlayer(this, 24) == null) {
            discard();
        }
    }

    private void tickFly() {
        Player p = target();
        if (p == null || stateTicks > OfficeRoomsConfig.DEATHMOTH_CHASE_SECONDS.get() * 20) {
            setState(LEAVE);
            return;
        }
        Vec3 eye = p.getEyePosition().add(0, -0.25, 0);
        Vec3 to = eye.subtract(position());
        double dist = to.length();
        if (dist < 1.25) {
            setState(ATTACHED);
            playSound(ModSounds.DEATHMOTH_ATTACK.get(), 1.3F, 0.9F + random.nextFloat() * 0.2F);
            p.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
            return;
        }
        double wob = Math.sin(tickCount * 0.7) * 0.12;
        Vec3 want = to.normalize().scale(0.48).add(-to.z / Math.max(0.1, dist) * wob, Math.cos(tickCount * 0.9) * 0.08, to.x / Math.max(0.1, dist) * wob);
        Vec3 v = getDeltaMovement().lerp(want, 0.3);
        if (position().distanceToSqr(lastPos) < 0.0025) {
            if (++stuck > 20) {
                v = v.add(0, 0.25, 0);
                stuck = 0;
            }
        } else {
            stuck = 0;
        }
        lastPos = position();
        setDeltaMovement(v);
        move(MoverType.SELF, v);
        face(v);
        if (stateTicks % 11 == 0) {
            playSound(ModSounds.DEATHMOTH_FLAP.get(), 0.9F, 0.9F + random.nextFloat() * 0.2F);
        }
    }

    private void tickAttached() {
        Player p = target();
        int limit = (int) (OfficeRoomsConfig.DEATHMOTH_ATTACH_SECONDS.get() * 20);
        if (p == null || stateTicks > limit) {
            setState(LEAVE);
            return;
        }
        Vec3 look = p.getLookAngle();
        Vec3 at = p.getEyePosition().add(look.scale(0.32)).add(0, -getBbHeight() / 2, 0);
        setPos(at.x, at.y, at.z);
        setDeltaMovement(Vec3.ZERO);
        setYRot(p.getYRot() + 180);
        if (stateTicks % 12 == 6) {
            p.hurt(damageSources().mobAttack(this), OfficeRoomsConfig.DEATHMOTH_DRAIN.get().floatValue());
            p.invulnerableTime = 0;                               // the drain is steady, not blocked by hit frames
        }
        if (stateTicks % 32 == 31) {
            playSound(ModSounds.DEATHMOTH_ATTACK.get(), 1.0F, 0.85F + random.nextFloat() * 0.3F);
        }
    }

    private void tickLeave() {
        Player p = target();
        Vec3 away = p == null ? new Vec3(random.nextDouble() - 0.5, 0.4, random.nextDouble() - 0.5)
                : position().subtract(p.position()).multiply(1, 0, 1).normalize().add(0, 0.35, 0);
        Vec3 v = getDeltaMovement().lerp(away.normalize().scale(0.55), 0.25);
        setDeltaMovement(v);
        move(MoverType.SELF, v);
        face(v);
        if (stateTicks % 11 == 0) {
            playSound(ModSounds.DEATHMOTH_FLAP.get(), 0.6F, 1.0F);
        }
        if (stateTicks > 60) {
            discard();                                            // gone into the dark
        }
    }

    private void face(Vec3 v) {
        if (v.horizontalDistanceSqr() > 1.0E-4) {
            float yaw = (float) (Mth.atan2(v.z, v.x) * Mth.RAD_TO_DEG) - 90F;
            setYRot(yaw);
            setYBodyRot(yaw);
            setYHeadRot(yaw);
        }
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        boolean r = super.hurt(src, amount);
        if (!level().isClientSide && r && isAlive()) {
            if (state() == WALL) {
                Player p = src.getEntity() instanceof Player pl ? pl : findVictim(16);
                if (p != null) {
                    trigger(p);
                } else {
                    setState(LEAVE);
                }
            } else if (state() == ATTACHED) {
                setState(LEAVE);                                  // knocked off the face
                setDeltaMovement(getDeltaMovement().add(0, 0.3, 0));
            }
        }
        return r;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return ModSounds.DEATHMOTH_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.DEATHMOTH_DEATH.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("MothState", state() == WALL ? WALL : LEAVE);
        tag.putInt("WallDir", entityData.get(WALL_DIR));
        tag.putBoolean("Folded", folded());
        tag.putInt("IdleLimit", idleLimit);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(STATE, tag.getInt("MothState"));
        entityData.set(WALL_DIR, tag.contains("WallDir") ? tag.getInt("WallDir") : -1);
        entityData.set(FOLDED, tag.getBoolean("Folded"));
        idleLimit = tag.getInt("IdleLimit");
    }

    /** Spawn egg / summon: cling to the closest wall around the spawn position, if any. */
    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.world.DifficultyInstance diff,
                                                                   net.minecraft.world.entity.MobSpawnType reason, net.minecraft.world.entity.SpawnGroupData data,
                                                                   CompoundTag tag) {
        BlockPos at = blockPosition();
        Direction n = DeathmothDirector.wallNormal(level, at);
        if (n != null) {
            // a little higher up the wall if there is room (natural spawns stand on the ground)
            int lift = random.nextInt(3);
            for (int k = lift; k > 0; k--) {
                BlockPos up = at.above(k);
                if (level.getBlockState(up).getCollisionShape(level, up).isEmpty() && DeathmothDirector.wallNormal(level, up) == n) {
                    at = up;
                    break;
                }
            }
            attachTo(at, n, 0);
        }
        return super.finalizeSpawn(level, diff, reason, data, tag);
    }
}
