package de.officerooms.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** 0.8.0: Level Access Card. Opens security doors up to its level. */
public class AccessCardItem extends Item {

    public final int level;

    public AccessCardItem(int level, Properties props) {
        super(props);
        this.level = level;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("Clearance level " + this.level).withStyle(ChatFormatting.AQUA));
        tip.add(Component.literal("Opens security doors up to level " + this.level + ".").withStyle(ChatFormatting.GRAY));
    }
}
