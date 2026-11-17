package de.officerooms.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.officerooms.block.DecorBlock;
import de.officerooms.block.HazardMonitorBlock;
import de.officerooms.hazard.HazardState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

/** 0.17.0: draws the HAZARD ZONE status on the monitor screens. */
public class HazardMonitorRenderer implements BlockEntityRenderer<HazardMonitorBlock.Entity> {

    private static final String GLYPHS = "#%&@?!/\\|<>=+*0123456789ABCDEFXZ";
    private final Font font;

    public HazardMonitorRenderer(BlockEntityRendererProvider.Context ctx) {
        this.font = ctx.getFont();
    }

    @Override
    public void render(HazardMonitorBlock.Entity be, float pt, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        long ms = System.currentTimeMillis();
        long seed = be.getBlockPos().asLong();
        if (((ms / 120) + seed) % 53 == 0) {
            return;                                                  // a short flicker now and then
        }
        String[] lines;
        int color;
        HazardState s = HazardState.CLIENT;
        boolean blink = (ms / 500) % 2 == 0;
        if (be.getBlockState().getValue(HazardMonitorBlock.BROKEN)) {
            if (Math.floorMod(seed, 3) == 0) {
                return;                                              // switched off for good
            }
            StringBuilder[] b = {new StringBuilder(), new StringBuilder(), new StringBuilder()};
            java.util.Random r = new java.util.Random(seed + ms / 300);
            for (StringBuilder sb : b) {
                for (int i = 0; i < 12; i++) {
                    sb.append(r.nextInt(5) == 0 ? ' ' : GLYPHS.charAt(r.nextInt(GLYPHS.length())));
                }
            }
            lines = new String[]{b[0].toString(), "ERR 0x" + Long.toHexString(Math.abs(seed) % 0xFFFF).toUpperCase(), b[1].toString(), b[2].toString()};
            color = 0x6A8F6A;
        } else if (blackoutLines() != null) {                         // 0.22.0: blackout announcements take turns with the hazard ones
            String[] bl = blackoutLines();
            if (bl.length == 0) {
                return;                                              // inside a blackout the screens are dead
            }
            lines = bl;
            color = blink ? 0xFF9020 : 0xFFE060;
        } else if (s.phase == HazardState.WARNING) {
            lines = new String[]{"! WARNING !", "TOXIC CONTAMINATION", "DETECTED", "HAZARD ZONE IN", HazardState.clock(s.remaining())};
            color = blink ? 0xFF3030 : 0xFFD020;
        } else if (s.phase == HazardState.ACTIVE) {
            lines = new String[]{"☣ HAZARD ZONE ☣", "ACTIVE", s.name.toUpperCase(), "GAS MASK REQUIRED", "ENDS IN " + HazardState.clock(s.remaining())};
            color = blink ? 0xFF2020 : 0xB01010;
        } else {
            lines = new String[]{"OFFICE ROOMS", "FACILITY STATUS", "NOMINAL", "AIR QUALITY: OK"};
            color = 0x40D060;
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
            font.drawInBatch(line, -font.width(line) / 2.0F, y, color, false, pose.last().pose(), buffers, Font.DisplayMode.POLYGON_OFFSET,
                    0, LightTexture.FULL_BRIGHT);
            y += 10;
        }
        pose.popPose();
    }

    /** null = nothing to show about blackouts right now; empty = this screen is dark. */
    private String[] blackoutLines() {
        de.officerooms.hazard.BlackoutState b = de.officerooms.hazard.BlackoutState.CLIENT;
        boolean hazard = HazardState.CLIENT.phase != HazardState.IDLE;
        if (hazard && (System.currentTimeMillis() / 4000) % 2 == 0) {
            return null;
        }
        if (b.phase == HazardState.WARNING) {
            return new String[]{"! POWER GRID !", "UNSTABLE", "BLACKOUT IN", b.name, HazardState.clock(b.remaining())};
        }
        if (b.phase == HazardState.ACTIVE) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null && b.inside(mc.player.getX(), mc.player.getY(), mc.player.getZ())) {
                return new String[0];
            }
            return new String[]{"BLACKOUT", b.name, b.fuseFloor > 0 ? "FUSE BOX: FLOOR " + b.fuseFloor : "FUSE BOX: UNKNOWN", "REPAIR REQUIRED", "BACKUP IN " + HazardState.clock(b.remaining())};
        }
        return null;
    }
}
