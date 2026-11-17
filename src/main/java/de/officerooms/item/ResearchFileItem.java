package de.officerooms.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** 0.12.0: rare exploration loot, exchanged at a Research Table. */
public class ResearchFileItem extends Item {

    public ResearchFileItem(Properties props) {
        super(props);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.office_rooms.research_file.desc").withStyle(ChatFormatting.GRAY));
    }
}
