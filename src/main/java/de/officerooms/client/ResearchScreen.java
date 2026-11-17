package de.officerooms.client;

import de.officerooms.core.OfficeRoomsIds;
import de.officerooms.research.ResearchMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** 0.12.0: Research Table screen. */
public class ResearchScreen extends AbstractContainerScreen<ResearchMenu> {

    private static final ResourceLocation BG = new ResourceLocation(OfficeRoomsIds.MOD_ID, "textures/gui/research_table.png");
    private static final String[] ROMAN = {"", "I", "II", "III"};
    private Button heart;
    private Button reforge;

    public ResearchScreen(ResearchMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        heart = addRenderableWidget(Button.builder(Component.empty(), b -> click(0)).bounds(leftPos + 50, topPos + 18, 120, 20).build());
        reforge = addRenderableWidget(Button.builder(Component.empty(), b -> click(1)).bounds(leftPos + 50, topPos + 42, 120, 20).build());
        containerTick();
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (heart == null) {
            return;
        }
        int h = menu.hearts();
        int f = menu.files();
        boolean maxed = h >= ResearchMenu.MAX_HEARTS;
        heart.setMessage(maxed ? Component.translatable("gui.office_rooms.heart_max")
                : Component.translatable("gui.office_rooms.heart_research", ResearchMenu.HEART_COST));
        heart.active = !maxed && f >= ResearchMenu.HEART_COST;
        heart.setTooltip(Tooltip.create(Component.translatable("gui.office_rooms.heart_tip", h, ResearchMenu.MAX_HEARTS)));
        int w = menu.weaponLevel();
        boolean full = w >= ResearchMenu.MAX_REFORGE;
        reforge.setMessage(full ? Component.translatable("gui.office_rooms.reforge_max")
                : Component.translatable("gui.office_rooms.reforge", ResearchMenu.reforgeCost(Math.max(0, w))));
        reforge.active = w >= 0 && !full && f >= ResearchMenu.reforgeCost(w);
        reforge.setTooltip(Tooltip.create(Component.translatable("gui.office_rooms.reforge_tip")));
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        g.blit(BG, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);   // no "Inventory" label, the status line sits there
        Component hearts = Component.translatable("gui.office_rooms.hearts", menu.hearts(), ResearchMenu.MAX_HEARTS);
        g.drawString(font, hearts, imageWidth - 8 - font.width(hearts), titleLabelY, 0xA02020, false);
        g.drawString(font, Component.translatable("gui.office_rooms.files", menu.files()), 8, 22, 0x404040, false);
        int w = menu.weaponLevel();
        Component status = w < 0 ? Component.translatable("gui.office_rooms.place_weapon")
                : w == 0 ? Component.translatable("gui.office_rooms.not_reforged")
                : Component.translatable("gui.office_rooms.reforged", ROMAN[Math.min(3, w)], 3 * w);
        g.drawString(font, status, 8, 70, w > 0 ? 0x8A6000 : 0x606060, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
    }
}
