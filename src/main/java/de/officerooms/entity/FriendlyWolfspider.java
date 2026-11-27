package de.officerooms.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Freundliche Wolfspider (Phase 4): gleiche Gestalt wie die Wolfspider, aber mit weißen, leuchtenden
 * Augen. Greift Spieler nie an und verursacht keinen Wither. Mit Pilzsuppe zähmbar (wie ein Wolf mit
 * Knochen: jede Suppe hat eine Chance von 1/3), danach gehört sie dem Spieler, folgt ihm, setzt sich auf
 * Rechtsklick und steht wieder auf, verteidigt ihren Besitzer. Gezähmte Tiere bleiben dauerhaft
 * (Besitzer und Zustand werden von {@link TamableAnimal} gespeichert, sie verschwinden nicht).
 */
public class FriendlyWolfspider extends TamableAnimal {

    private static final Ingredient FOOD = Ingredient.of(Items.MUSHROOM_STEW);

    public FriendlyWolfspider(EntityType<? extends FriendlyWolfspider> type, Level level) {
        super(type, level);
        setTame(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(4, new LeapAtTargetGoal(this, 0.35F));
        goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.1, true));
        goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.0, 10.0F, 2.0F, false));
        goalSelector.addGoal(7, new TemptGoal(this, 1.0, FOOD, false));
        goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
    }

    /** Nie Spieler, nie andere zahme Tiere, keine Creeper/Ghasts (wie beim Wolf). */
    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        if (target instanceof Player || target instanceof Creeper || target instanceof Ghast) {
            return false;
        }
        if (target instanceof TamableAnimal t && t.isTame()) {
            return false;
        }
        return !(target instanceof AbstractHorse h && h.isTamed());
    }

    @Override
    public void setTarget(LivingEntity target) {
        if (target instanceof Player) {
            return;
        }
        super.setTarget(target);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean food = FOOD.test(stack);
        if (level().isClientSide) {
            boolean handled = isOwnedBy(player) || (food && !isTame()) || (food && isTame() && getHealth() < getMaxHealth());
            return handled ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        if (isTame()) {
            if (food && getHealth() < getMaxHealth()) {
                eat(player, stack);
                heal(8.0F);
                return InteractionResult.SUCCESS;
            }
            if (isOwnedBy(player)) {
                setOrderedToSit(!isOrderedToSit());
                jumping = false;
                navigation.stop();
                setTarget(null);
                return InteractionResult.SUCCESS;
            }
            return super.mobInteract(player, hand);
        }
        if (food) {
            eat(player, stack);
            if (random.nextInt(3) == 0 && !net.neoforged.neoforge.event.ForgeEventFactory.onAnimalTame(this, player)) {
                tame(player);
                navigation.stop();
                setTarget(null);
                setOrderedToSit(true);
                setPersistenceRequired();
                level().broadcastEntityEvent(this, (byte) 7);
            } else {
                level().broadcastEntityEvent(this, (byte) 6);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    private void eat(Player player, ItemStack stack) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
            ItemStack bowl = new ItemStack(Items.BOWL);
            if (!player.getInventory().add(bowl)) {
                player.drop(bowl, false);
            }
        }
        playSound(SoundEvents.GENERIC_EAT, 0.8F, 1.0F);
    }

    @Override
    public void setTame(boolean tame) {
        super.setTame(tame);
        if (getAttribute(Attributes.MAX_HEALTH) != null) {
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(tame ? 30.0 : 20.0);
            if (tame) {
                setHealth(getMaxHealth());
            }
        }
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false; // keine Zucht (kein Jungtiermodell)
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    @Override
    public boolean removeWhenFarAway(double distSq) {
        return !isTame();
    }

    @Override
    public MobType getMobType() {
        return MobType.ARTHROPOD;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        if (isTame() && getHealth() < getMaxHealth() * 0.5F) {
            return SoundEvents.WOLF_WHINE;
        }
        return random.nextInt(3) == 0 ? SoundEvents.WOLF_PANT : SoundEvents.WOLF_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WOLF_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WOLF_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.WOLF_STEP, 0.15F, 1.0F);
    }
}
