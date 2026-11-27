package de.officerooms.item;

import de.officerooms.dimension.OfficeRoomsDimension;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.bus.api.EventPriority;

import java.util.List;
import java.util.Optional;

/**
 * Totem of Mercy (0.7.0, extrem selten): Stirbt sein Träger in den Office Rooms, wird der Tod
 * verhindert und der Spieler in die Overworld gebracht (Schlafplatz/Respawn-Anker oder Weltspawn).
 * Wirkt aus dem ganzen Inventar und wird dabei verbraucht. Gegen Liquid Pain hilft er nicht.
 */
public final class TotemOfMercyItem extends Item {

    public TotemOfMercyItem(Properties props) {
        super(props);
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, TotemOfMercyItem::onDeath);
    }

    private static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.isCanceled()) {
            return;
        }
        if (player.level().dimension() != OfficeRoomsDimension.LEVEL_KEY || event.getSource().is(LiquidPainItem.DAMAGE)) {
            return;
        }
        ItemStack totem = find(player.getInventory());
        if (totem == null) {
            return;
        }
        ServerLevel overworld = player.server.overworld();
        if (overworld == null) {
            return;
        }
        event.setCanceled(true);
        totem.shrink(1);
        player.setHealth(4.0F);
        player.removeAllEffects();
        player.clearFire();
        player.setRemainingFireTicks(0);
        player.fallDistance = 0;
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0));
        player.level().playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 0.8F);

        Vec3 target = respawnInOverworld(player, overworld);
        player.teleportTo(overworld, target.x, target.y, target.z, player.getYRot(), player.getXRot());
        overworld.playSound(null, BlockPos.containing(target), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.2F);
        player.displayClientMessage(Component.translatable("item.office_rooms.totem_of_mercy.used").withStyle(ChatFormatting.GOLD), false);
    }

    private static ItemStack find(Inventory inv) {
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && s.getItem() instanceof TotemOfMercyItem) {
                return s;
            }
        }
        return null;
    }

    /** Eigener Spawnpunkt in der Overworld (Bett/Anker), sonst Weltspawn auf sicherer Höhe. */
    private static Vec3 respawnInOverworld(ServerPlayer player, ServerLevel overworld) {
        BlockPos rp = player.getRespawnPosition();
        if (rp != null && player.getRespawnDimension() == Level.OVERWORLD) {
            Optional<Vec3> v = net.minecraft.world.entity.player.Player.findRespawnPositionAndUseSpawnBlock(
                    overworld, rp, player.getRespawnAngle(), player.isRespawnForced(), false);
            if (v.isPresent()) {
                return v.get();
            }
        }
        BlockPos spawn = overworld.getSharedSpawnPos();
        overworld.getChunk(spawn); // Höhenkarte laden
        int y = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.getX(), spawn.getZ());
        return new Vec3(spawn.getX() + 0.5, y, spawn.getZ() + 0.5);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.office_rooms.totem_of_mercy.desc").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
