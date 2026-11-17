package de.officerooms.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.officerooms.block.DecorBlock;
import de.officerooms.outpost.Controller;
import de.officerooms.outpost.OutpostBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

/** 0.20.0: the outpost screens: boot, emergency warnings, READY, activation progress, gateway open. */
public class OutpostMonitorRenderer implements BlockEntityRenderer<OutpostBlocks.MonitorEntity> {

    private static final String[][] WARNINGS = {
            {"!! WARNING !!", "DIMENSIONAL", "INSTABILITY", "DETECTED"},
            {"!! ALERT !!", "CONTAINMENT", "FAILURE", "SECTOR 0"},
            {"GATEWAY TARGET:", "UNKNOWN", "DESTINATION", "60 Hz RESONANCE"},
            {"!! WARNING !!", "DO NOT APPROACH", "THE GATEWAY", ""},
            {"STAFF STATUS:", "MISSING", "MISSING", "MISSING"},
            {"FIELD COILS:", "CHARGING", "CRITICAL", "TEMPERATURE"},
            {"SYSTEM CHECK", "SAFETY LOCK", "ENGAGED", "PLEASE WAIT"},
            {"PORTAL SYSTEM", "READY", "IN 00:0X", ""}};
    private final Font font;

    public OutpostMonitorRenderer(BlockEntityRendererProvider.Context ctx) {
        this.font = ctx.getFont();
    }

    @Override
    public void render(OutpostBlocks.MonitorEntity be, float pt, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (be.phase == Controller.OFF || Minecraft.getInstance().level == null) {
            return;
        }
        long t = Minecraft.getInstance().level.getGameTime() - be.start;
        long seed = be.getBlockPos().asLong();
        long ms = System.currentTimeMillis();
        boolean blink = (ms / 400) % 2 == 0;
        String[] lines;
        int color;
        switch (be.phase) {
            case Controller.BOOT -> {
                int n = (int) Math.min(10, t / 5);
                lines = new String[]{"PROJECT LIMINAL", "BOOTING...", "[" + "#".repeat(n) + ".".repeat(10 - n) + "]"};
                color = 0x40D060;
            }
            case Controller.WARNING -> {
                if (((ms / 90) + seed) % 9 == 0) {
                    return;                                                  // flicker
                }
                int i = (int) Math.floorMod(t / 25 + Math.floorMod(seed, 5), WARNINGS.length);
                lines = WARNINGS[i].clone();
                if (i == WARNINGS.length - 1) {
                    lines[2] = "IN 00:" + String.format("%02d", Math.max(0, (220 - t) / 20));
                }
                color = blink ? 0xFF3030 : 0xFFC020;
            }
            case Controller.READY -> {
                lines = new String[]{"PORTAL SYSTEM", "READY", "", "PRESS THE RED", "EMERGENCY BUTTON"};
                color = blink ? 0x50FF60 : 0xFFE040;
            }
            case Controller.ACTIVATING -> {
                if (((ms / 60) + seed) % 6 == 0) {
                    return;
                }
                int pc = (int) Math.min(100, t * 100 / 170);
                lines = new String[]{"ACTIVATION", "IN PROGRESS", pc + " %", "STAND CLEAR"};
                color = 0x60E0FF;
            }
            default -> {
                lines = new String[]{"GATEWAY OPEN", "DESTINATION:", "UNKNOWN", "~ 60 Hz ~"};
                color = blink ? 0x60E0FF : 0xE8E070;
            }
        }
        Direction f = be.getBlockState().getValue(DecorBlock.FACING);
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-f.toYRot()));
        pose.translate(0, 0, 0.131);
        float sc = 1.0F / 136.0F;
        pose.scale(sc, -sc, sc);
        int y = -lines.length * 10 / 2 + 1;
        for (String line : lines) {
            font.drawInBatch(line, -font.width(line) / 2.0F, y, color, false, pose.last().pose(), buffers, Font.DisplayMode.POLYGON_OFFSET, 0, LightTexture.FULL_BRIGHT);
            y += 10;
        }
        pose.popPose();
    }
}
