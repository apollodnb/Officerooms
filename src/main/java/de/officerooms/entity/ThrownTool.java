package de.officerooms.entity;

import de.officerooms.item.ModItems;
import de.officerooms.item.ThrowableToolItem;
import de.officerooms.world.ToolEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** 0.10.1: A thrown glowstick, emergency flare or noise maker. */
public class ThrownTool extends ThrowableItemProjectile {

    public ThrownTool(EntityType<? extends ThrownTool> type, Level level) {
        super(type, level);
    }

    public ThrownTool(Level level, LivingEntity owner) {
        super(ModEntities.THROWN_TOOL.get(), owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.GLOWSTICK.get();
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (level().isClientSide) {
            return;
        }
        BlockPos pos = hit instanceof BlockHitResult b ? b.getBlockPos().relative(b.getDirection()) : blockPosition();
        if (getItem().getItem() instanceof ThrowableToolItem t) {
            ToolEffects.land(t.kind, level(), pos);
        }
        discard();
    }
}
