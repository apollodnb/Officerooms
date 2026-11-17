package de.officerooms.client;

import de.officerooms.hazard.HazardNet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** 0.26.0: the elevator floor panel. Type a floor (buttons or number keys), then GO. */
public class ElevatorPanelScreen extends Screen {

    private static final int W = 120;
    private static final int H = 168;
    private final BlockPos plate;
    private final int storey;
    private final int max;
    private String input = "";

    public ElevatorPanelScreen(BlockPos plate, int storey, int max) {
        super(Component.literal("Elevator"));
        this.plate = plate;
        this.storey = storey;
        this.max = max;
    }

    public static void open(BlockPos plate, int storey, int max) {
        Minecraft.getInstance().setScreen(new ElevatorPanelScreen(plate, storey, max));
    }

    @Override
    protected void init() {
        int x0 = (width - W) / 2;
        int y0 = (height - H) / 2;
        String[] keys = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "<", "0", "GO"};
        for (int i = 0; i < keys.length; i++) {
            String k = keys[i];
            int bx = x0 + 14 + (i % 3) * 32;
            int by = y0 + 52 + (i / 3) * 26;
            addRenderableWidget(Button.builder(Component.literal(k), b -> press(k)).bounds(bx, by, 28, 22).build());
        }
    }

    private void press(String k) {
        switch (k) {
            case "<" -> input = input.isEmpty() ? "" : input.substring(0, input.length() - 1);
            case "GO" -> go();
            default -> {
                if (input.length() < 3) {
                    input = input + k;
                }
            }
        }
    }

    private void go() {
        if (input.isEmpty()) {
            return;
        }
        int f = Integer.parseInt(input);
        if (f > max) {
            input = "";
            return;
        }
        HazardNet.CHANNEL.sendToServer(new HazardNet.ElevatorCall(plate, f));
        onClose();
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) {
            press(Integer.toString(key - GLFW.GLFW_KEY_0));
            return true;
        }
        if (key >= GLFW.GLFW_KEY_KP_0 && key <= GLFW.GLFW_KEY_KP_9) {
            press(Integer.toString(key - GLFW.GLFW_KEY_KP_0));
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            press("<");
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            go();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        int x0 = (width - W) / 2;
        int y0 = (height - H) / 2;
        g.fill(x0 - 2, y0 - 2, x0 + W + 2, y0 + H + 2, 0xFF6E6E6A);
        g.fill(x0, y0, x0 + W, y0 + H, 0xFFB9B9B2);
        g.fill(x0 + 10, y0 + 10, x0 + W - 10, y0 + 42, 0xFF140A04);
        String shown = input.isEmpty() ? "--" : input;
        g.drawCenteredString(font, Component.literal(shown), x0 + W / 2, y0 + 15, 0xFFFF9A2E);
        g.drawCenteredString(font, Component.literal("You are on " + storey), x0 + W / 2, y0 + 29, 0xFF8A5A22);
        g.drawCenteredString(font, Component.literal("Floors 0-" + max), x0 + W / 2, y0 + H - 11, 0xFF3A3A36);
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
