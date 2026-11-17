package de.officerooms.underside;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.entity.PartEntity;

/** 0.19.0: one body segment of [Unknown] (hitbox only; drawn by the boss renderer). */
public final class UnknownPart extends PartEntity<Unknown> {

    public final int index;
    private final EntityDimensions size;

    public UnknownPart(Unknown parent, int index, float w, float h) {
        super(parent);
        this.index = index;
        this.size = EntityDimensions.scalable(w, h);
        refreshDimensions();
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public ItemStack getPickResult() {
        return getParent().getPickResult();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return !isInvulnerableTo(source) && getParent().hurtFromPart(this, source, amount);
    }

    @Override
    public boolean is(Entity other) {
        return this == other || getParent() == other;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return size;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
