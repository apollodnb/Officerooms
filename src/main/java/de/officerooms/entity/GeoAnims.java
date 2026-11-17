package de.officerooms.entity;

import net.minecraft.world.entity.Mob;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;

/** 0.11.0: shared GeckoLib controller for the creatures: idle / walk / run plus triggerable one-shots. */
public final class GeoAnims {

    public static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    public static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    public static final RawAnimation RUN = RawAnimation.begin().thenLoop("run");

    private GeoAnims() {
    }

    /** Controller "move". Runs when aggressive or fast, walks otherwise; {@code triggers} are one-shot animations. */
    public static <T extends Mob & GeoAnimatable> void register(T mob, AnimatableManager.ControllerRegistrar controllers,
                                                               double runSpeedSq, String... triggers) {
        AnimationController<T> c = new AnimationController<>(mob, "move", 4, state -> {
            if (state.isMoving()) {
                return state.setAndContinue(mob.isAggressive() || mob.getDeltaMovement().horizontalDistanceSqr() > runSpeedSq ? RUN : WALK);
            }
            return state.setAndContinue(IDLE);
        });
        for (String t : triggers) {
            c.triggerableAnim(t, RawAnimation.begin().thenPlay(t));
        }
        controllers.add(c);
    }
}
