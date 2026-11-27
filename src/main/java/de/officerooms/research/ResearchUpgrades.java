package de.officerooms.research;

import java.util.UUID;

import de.officerooms.core.OfficeRoomsIds;
import de.officerooms.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingHurtEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * 0.12.0: permanent progression. Heart Research is stored in the player's persisted data (survives death) and applied as
 * a max-health modifier. Reforging stores a level (1–3) on the item; melee weapons get +3 attack damage per level as an
 * extra attribute modifier (default modifiers and enchantments stay), bows, crossbows and thrown tridents add +3 per level
 * to the projectile's damage.
 */
public final class ResearchUpgrades {

    public static final String REFORGE_TAG = "OfficeReforge";
    private static final String HEARTS_KEY = "office_rooms_hearts";
    private static final UUID HEART_ID = UUID.fromString("5f0c1d2e-7a41-4c35-9a0e-0ff1ce000001");
    private static final UUID REFORGE_ID = UUID.fromString("5f0c1d2e-7a41-4c35-9a0e-0ff1ce000002");
    public static final TagKey<Item> REFORGEABLE = ItemTags.create(ResourceLocation.fromNamespaceAndPath(OfficeRoomsIds.MOD_ID, "reforgeable"));
    private static final String[] ROMAN = {"", "I", "II", "III"};

    private ResearchUpgrades() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.register(ResearchUpgrades.class);
    }

    // --- hearts --------------------------------------------------------------------------------------

    private static CompoundTag persisted(Player p) {
        CompoundTag d = p.getPersistentData();
        if (!d.contains(Player.PERSISTED_NBT_TAG, 10)) {
            d.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return d.getCompound(Player.PERSISTED_NBT_TAG);
    }

    public static int hearts(Player p) {
        return persisted(p).getInt(HEARTS_KEY);
    }

    public static void addHeart(Player p) {
        persisted(p).putInt(HEARTS_KEY, Math.min(ResearchMenu.MAX_HEARTS, hearts(p) + 1));
        apply(p);
        p.heal(2.0F);
    }

    static void apply(Player p) {
        AttributeInstance a = p.getAttribute(Attributes.MAX_HEALTH);
        if (a == null) {
            return;
        }
        a.removeModifier(HEART_ID);
        int n = hearts(p);
        if (n > 0) {
            a.addPermanentModifier(new AttributeModifier(HEART_ID, "Office Rooms heart research", 2.0 * n, AttributeModifier.Operation.ADDITION));
        }
    }

    // --- files ---------------------------------------------------------------------------------------

    public static int countFiles(Player p) {
        Inventory inv = p.getInventory();
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(ModItems.RESEARCH_FILE.get())) {
                n += s.getCount();
            }
        }
        return n;
    }

    public static void takeFiles(Player p, int n) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize() && n > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(ModItems.RESEARCH_FILE.get())) {
                int k = Math.min(n, s.getCount());
                s.shrink(k);
                n -= k;
            }
        }
        inv.setChanged();
    }

    // --- reforging -----------------------------------------------------------------------------------

    public static boolean isReforgeable(ItemStack s) {
        if (s.isEmpty()) {
            return false;
        }
        Item i = s.getItem();
        return i instanceof SwordItem || i instanceof AxeItem || i instanceof TridentItem || i instanceof ProjectileWeaponItem || s.is(REFORGEABLE);
    }

    public static int reforgeLevel(ItemStack s) {
        return s.hasTag() ? s.getTag().getInt(REFORGE_TAG) : 0;
    }

    public static void setReforge(ItemStack s, int level) {
        s.getOrCreateTag().putInt(REFORGE_TAG, level);
    }

    // --- events --------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        apply(e.getEntity());
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone e) {
        CompoundTag old = e.getOriginal().getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (old.contains(HEARTS_KEY)) {
            persisted(e.getEntity()).putInt(HEARTS_KEY, old.getInt(HEARTS_KEY));
        }
        apply(e.getEntity());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        apply(e.getEntity());
        e.getEntity().setHealth(e.getEntity().getMaxHealth());
    }

    @SubscribeEvent
    public static void onAttributes(ItemAttributeModifierEvent e) {
        if (e.getSlotType() != EquipmentSlot.MAINHAND || e.getItemStack().getItem() instanceof ProjectileWeaponItem) {
            return;
        }
        int lvl = reforgeLevel(e.getItemStack());
        if (lvl > 0) {
            e.addModifier(Attributes.ATTACK_DAMAGE, new AttributeModifier(REFORGE_ID, "Office Rooms reforge", 3.0 * lvl, AttributeModifier.Operation.ADDITION));
        }
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent e) {
        int lvl = reforgeLevel(e.getItemStack());
        if (lvl <= 0 || e.getToolTip().isEmpty()) {
            return;
        }
        String key = e.getItemStack().getItem() instanceof ProjectileWeaponItem ? "tooltip.office_rooms.reforged_ranged" : "tooltip.office_rooms.reforged";
        e.getToolTip().add(1, Component.translatable(key, ROMAN[Math.min(lvl, 3)], 3 * lvl).withStyle(ChatFormatting.GOLD));
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide || e.loadedFromDisk() || !(e.getEntity() instanceof AbstractArrow arrow)
                || !(arrow.getOwner() instanceof Player pl)) {
            return;
        }
        ItemStack w;
        if (arrow instanceof ThrownTrident) {
            w = pl.getUseItem().getItem() instanceof TridentItem ? pl.getUseItem()
                    : pl.getMainHandItem().getItem() instanceof TridentItem ? pl.getMainHandItem() : pl.getOffhandItem();
        } else {
            w = pl.getMainHandItem().getItem() instanceof ProjectileWeaponItem ? pl.getMainHandItem() : pl.getOffhandItem();
        }
        int lvl = reforgeLevel(w);
        if (lvl > 0) {
            arrow.getPersistentData().putInt(REFORGE_TAG, lvl);
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent e) {
        if (e.getSource().getDirectEntity() instanceof AbstractArrow a) {
            int lvl = a.getPersistentData().getInt(REFORGE_TAG);
            if (lvl > 0) {
                e.setAmount(e.getAmount() + 3.0F * lvl);
            }
        }
    }
}
