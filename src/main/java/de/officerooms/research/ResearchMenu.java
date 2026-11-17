package de.officerooms.research;

import de.officerooms.block.ModBlocks;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 0.12.0: Research Table. Button 0 = Heart Research (+1 heart, at most {@link #MAX_HEARTS}), button 1 = reforge the
 * weapon in the slot (+3 damage per level, at most 3 levels). Costs are paid in Research Files from the inventory.
 */
public class ResearchMenu extends AbstractContainerMenu {

    public static final int HEART_COST = 3;
    public static final int MAX_HEARTS = 5;
    public static final int MAX_REFORGE = 3;

    public static int reforgeCost(int level) {
        return 2 + level;                                   // 2, 3, 4 files
    }

    private final ContainerLevelAccess access;
    private final Player player;
    private final SimpleContainer weapon = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            slotsChanged(this);
        }
    };
    private final DataSlot hearts = DataSlot.standalone();
    private final DataSlot files = DataSlot.standalone();
    private final DataSlot weaponLevel = DataSlot.standalone();

    public ResearchMenu(int id, Inventory inv) {
        this(id, inv, ContainerLevelAccess.NULL);
    }

    public ResearchMenu(int id, Inventory inv, ContainerLevelAccess access) {
        super(ModMenus.RESEARCH.get(), id);
        this.access = access;
        this.player = inv.player;
        addSlot(new Slot(weapon, 0, 17, 48) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ResearchUpgrades.isReforgeable(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
            }
        }
        for (int c = 0; c < 9; c++) {
            addSlot(new Slot(inv, c, 8 + c * 18, 142));
        }
        addDataSlot(hearts);
        addDataSlot(files);
        addDataSlot(weaponLevel);
        refresh();
    }

    private void refresh() {
        if (player.level().isClientSide) {
            return;
        }
        hearts.set(ResearchUpgrades.hearts(player));
        files.set(ResearchUpgrades.countFiles(player));
        ItemStack s = weapon.getItem(0);
        weaponLevel.set(s.isEmpty() ? -1 : ResearchUpgrades.reforgeLevel(s));
    }

    public int hearts() {
        return hearts.get();
    }

    public int files() {
        return files.get();
    }

    /** -1 = empty slot, else the reforge level of the weapon in the slot. */
    public int weaponLevel() {
        return weaponLevel.get();
    }

    @Override
    public void broadcastChanges() {
        refresh();
        super.broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player p, int id) {
        int have = ResearchUpgrades.countFiles(p);
        if (id == 0) {
            if (ResearchUpgrades.hearts(p) >= MAX_HEARTS || have < HEART_COST) {
                return false;
            }
            ResearchUpgrades.takeFiles(p, HEART_COST);
            ResearchUpgrades.addHeart(p);
            if (p instanceof net.minecraft.server.level.ServerPlayer sp) {
                de.officerooms.quest.Milestones.grant(sp, "heart_research");
                if (ResearchUpgrades.hearts(p) >= MAX_HEARTS) {
                    de.officerooms.quest.Milestones.grant(sp, "heart_research_max");
                }
            }
            access.execute((l, pos) -> l.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.0F, 1.2F));
            refresh();
            return true;
        }
        if (id == 1) {
            ItemStack s = weapon.getItem(0);
            if (!ResearchUpgrades.isReforgeable(s)) {
                return false;
            }
            int lvl = ResearchUpgrades.reforgeLevel(s);
            if (lvl >= MAX_REFORGE || have < reforgeCost(lvl)) {
                return false;
            }
            ResearchUpgrades.takeFiles(p, reforgeCost(lvl));
            ResearchUpgrades.setReforge(s, lvl + 1);
            if (p instanceof net.minecraft.server.level.ServerPlayer sp) {
                de.officerooms.quest.Milestones.grant(sp, "reforge");
                if (lvl + 1 >= MAX_REFORGE) {
                    de.officerooms.quest.Milestones.grant(sp, "reforge_max");
                }
            }
            weapon.setChanged();
            access.execute((l, pos) -> l.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, 0.8F));
            refresh();
            return true;
        }
        return false;
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        access.execute((l, pos) -> clearContainer(p, weapon));
    }

    @Override
    public boolean stillValid(Player p) {
        return stillValid(access, p, ModBlocks.RESEARCH_TABLE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, 37, true)) {
                return ItemStack.EMPTY;
            }
        } else if (ResearchUpgrades.isReforgeable(stack) && !slots.get(0).hasItem()) {
            if (!moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < 28) {
            if (!moveItemStackTo(stack, 28, 37, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 1, 28, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }
}
