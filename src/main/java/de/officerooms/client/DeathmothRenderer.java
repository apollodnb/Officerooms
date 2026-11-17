package de.officerooms.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import de.officerooms.core.OfficeRoomsIds;
import de.officerooms.entity.Deathmoth;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.ViewportEvent;

/**
 * 0.24.0: Deathmoth. Model built in code: hairy thorax, head with feathered antennae, a two-part abdomen and four
 * flat wings (cut-out texture). No emissive parts: it is lit like any other mob and disappears in the dark.
 */
public class DeathmothRenderer extends EntityRenderer<Deathmoth> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(OfficeRoomsIds.MOD_ID, "deathmoth"), "main");
    private static final ResourceLocation TEX = new ResourceLocation(OfficeRoomsIds.MOD_ID, "textures/entity/deathmoth.png");
    static final ResourceLocation OVERLAY = new ResourceLocation(OfficeRoomsIds.MOD_ID, "textures/gui/deathmoth_overlay.png");

    private final ModelPart root, foreL, foreR, hindL, hindR, antL, antR, abdomen;

    public DeathmothRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        root = ctx.bakeLayer(LAYER);
        foreL = root.getChild("foreL");
        foreR = root.getChild("foreR");
        hindL = root.getChild("hindL");
        hindR = root.getChild("hindR");
        antL = root.getChild("antL");
        antR = root.getChild("antR");
        abdomen = root.getChild("abdomen");
        shadowRadius = 0.25F;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        r.addOrReplaceChild("thorax", CubeListBuilder.create().texOffs(64, 0).addBox(-2.5F, -2F, -3F, 5, 4, 6), PartPose.ZERO);
        r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(64, 12).addBox(-2F, -1.5F, -3F, 4, 3, 3), PartPose.offset(0, 0.3F, -3F));
        PartDefinition ab = r.addOrReplaceChild("abdomen", CubeListBuilder.create().texOffs(64, 22).addBox(-2F, -1.5F, 0, 4, 3.5F, 5), PartPose.offset(0, 0.2F, 3F));
        ab.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(64, 34).addBox(-1.5F, -1F, 0, 3, 2.5F, 5), PartPose.offsetAndRotation(0, 0.3F, 4.8F, -0.12F, 0, 0));
        r.addOrReplaceChild("antL", CubeListBuilder.create().texOffs(0, 32).addBox(0, 0, -7, 3, 0, 7), PartPose.offsetAndRotation(0.6F, -1F, -5.5F, -0.35F, -0.35F, 0));
        r.addOrReplaceChild("antR", CubeListBuilder.create().texOffs(0, 32).mirror().addBox(-3, 0, -7, 3, 0, 7), PartPose.offsetAndRotation(-0.6F, -1F, -5.5F, -0.35F, 0.35F, 0));
        r.addOrReplaceChild("foreL", CubeListBuilder.create().texOffs(0, 0).addBox(0, 0, -6, 14, 0, 12), PartPose.offset(2.4F, -1.2F, -0.5F));
        r.addOrReplaceChild("foreR", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-14, 0, -6, 14, 0, 12), PartPose.offset(-2.4F, -1.2F, -0.5F));
        r.addOrReplaceChild("hindL", CubeListBuilder.create().texOffs(0, 16).addBox(0, 0, -2, 11, 0, 10), PartPose.offset(2.2F, -0.9F, 2.0F));
        r.addOrReplaceChild("hindR", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-11, 0, -2, 11, 0, 10), PartPose.offset(-2.2F, -0.9F, 2.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public ResourceLocation getTextureLocation(Deathmoth e) {
        return TEX;
    }

    private void wings(float flap, float sweep) {
        foreL.zRot = flap;
        foreR.zRot = -flap;
        hindL.zRot = flap * 0.85F;
        hindR.zRot = -flap * 0.85F;
        foreL.yRot = sweep;
        foreR.yRot = -sweep;
        hindL.yRot = sweep * 0.6F;
        hindR.yRot = -sweep * 0.6F;
    }

    @Override
    public void render(Deathmoth e, float entityYaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        Minecraft mc = Minecraft.getInstance();
        int state = e.state();
        if (state == Deathmoth.ATTACHED && mc.player != null && e.targetId() == mc.player.getId() && mc.options.getCameraType().isFirstPerson()) {
            return;                                               // the screen overlay shows it instead
        }
        float age = e.tickCount + pt;
        ps.pushPose();
        float yaw = Mth.rotLerp(pt, e.yRotO, e.getYRot());
        Direction n = e.wallNormal();
        if (state == Deathmoth.WALL) {
            ps.translate(0, 0.25, 0);
            if (n != null) {
                ps.translate(-n.getStepX() * 0.27, 0, -n.getStepZ() * 0.27);   // flat on the wall (hitbox stays clear of it)
                ps.mulPose(Axis.YP.rotationDegrees(-n.toYRot()));
                ps.mulPose(Axis.XP.rotationDegrees(90));          // belly on the wall, head up, back to the room
            }
            float twitch = (e.getId() * 37 + (int) age) % 160 < 4 ? 0.12F : 0F;   // rare small twitch
            float breathe = Mth.sin(age * 0.05F) * 0.02F;
            if (e.folded()) {
                wings(0.05F + breathe, 0.75F + twitch);           // delta shape: forewings swept back over the hindwings
            } else {
                wings(0.04F + breathe + twitch, 0.1F);
            }
            antL.xRot = -0.35F + breathe;
            antR.xRot = -0.35F + breathe;
            abdomen.xRot = 0;
        } else {
            ps.translate(0, 0.3, 0);
            ps.mulPose(Axis.YP.rotationDegrees(-yaw));
            float speed = state == Deathmoth.ATTACHED ? 2.7F : 1.55F;
            float amp = state == Deathmoth.ATTACHED ? 0.55F : 1.05F;
            wings(Mth.sin(age * speed) * amp + 0.15F, 0.05F);
            ps.mulPose(Axis.XP.rotationDegrees(state == Deathmoth.ATTACHED ? -60 : -12 + Mth.sin(age * 0.4F) * 6));
            abdomen.xRot = Mth.sin(age * 0.3F) * 0.12F;
            antL.xRot = -0.2F;
            antR.xRot = -0.2F;
        }
        ps.scale(-1, -1, 1);
        ps.scale(0.9F, 0.9F, 0.9F);
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(TEX));
        int overlay = OverlayTexture.pack(OverlayTexture.u(0), OverlayTexture.v(e.hurtTime > 0));
        root.render(ps, vc, light, overlay);
        ps.popPose();
        super.render(e, entityYaw, pt, ps, buf, light);
    }

    // ---------------------------------------------------------------------------------------------
    // Screen effect: wings and body over the camera while it flies at you / clings to your face
    // ---------------------------------------------------------------------------------------------

    /** 0 = nothing, 1 = full cover. */
    static float cover(float pt) {
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null || mc.level == null) {
            return 0;
        }
        float best = 0;
        for (Deathmoth m : mc.level.getEntitiesOfClass(Deathmoth.class, p.getBoundingBox().inflate(4))) {
            if (m.targetId() != p.getId()) {
                continue;
            }
            if (m.state() == Deathmoth.ATTACHED) {
                best = 1;
            } else if (m.state() == Deathmoth.FLY) {
                double d = m.position().distanceTo(p.getEyePosition(pt));
                best = Math.max(best, (float) Mth.clamp((3.0 - d) / 1.8, 0, 0.9));
            }
        }
        return best;
    }

    public static void renderOverlay(GuiGraphics g, float pt, int w, int h) {
        float c = cover(pt);
        if (c <= 0.01F) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        float t = (mc.player.tickCount + pt);
        float flutter = 1.0F + 0.06F * Mth.sin(t * 2.7F);
        int size = (int) (Math.max(w, h) * (0.9F + 0.55F * c) * flutter);
        int jx = (int) (Mth.sin(t * 1.9F) * 6 * c);
        int jy = (int) (Mth.cos(t * 2.3F) * 5 * c);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        // lit like the place you are standing in (no glow in the dark)
        net.minecraft.core.BlockPos at = net.minecraft.core.BlockPos.containing(mc.player.getEyePosition(pt));
        int bl = mc.level.getMaxLocalRawBrightness(at);
        float shade = 0.5F + 0.5F * (float) Math.pow(bl / 15.0, 1.3);
        RenderSystem.setShaderColor(shade, shade, shade, Math.min(1F, c * 1.05F));
        g.blit(OVERLAY, (w - size) / 2 + jx, (h - size) / 2 + jy, 0, 0, size, size, size, size);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.disableBlend();
    }

    /** Head jerks while it clings to the face. */
    public static void onCamera(ViewportEvent.ComputeCameraAngles e) {
        float c = cover((float) e.getPartialTick());
        if (c >= 0.99F) {
            float t = (float) (Minecraft.getInstance().player.tickCount + e.getPartialTick());
            e.setYaw(e.getYaw() + Mth.sin(t * 3.1F) * 1.6F);
            e.setPitch(e.getPitch() + Mth.cos(t * 2.6F) * 1.3F);
            e.setRoll(e.getRoll() + Mth.sin(t * 1.7F) * 2.5F);
        }
    }
}
