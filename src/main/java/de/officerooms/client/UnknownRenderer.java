package de.officerooms.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import de.officerooms.core.OfficeRoomsIds;
import de.officerooms.underside.Unknown;
import de.officerooms.underside.UnknownPart;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/**
 * 0.19.0: [Unknown]. Built from wire-thin black sticks: a scribble head (the Flayed's), a spine with centipede legs,
 * and a skinned ribcage with long human-like arms on every third segment (mix of sketches A and B).
 */
public class UnknownRenderer extends EntityRenderer<Unknown> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(OfficeRoomsIds.MOD_ID, "unknown"), "main");
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(OfficeRoomsIds.MOD_ID, "textures/entity/unknown.png");
    private static final float SPINE_Y = 1.8F;

    private final ModelPart head, seg, rib, legR, legL, armR, armL;

    public UnknownRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        ModelPart root = ctx.bakeLayer(LAYER);
        head = root.getChild("head");
        seg = root.getChild("seg");
        rib = root.getChild("rib");
        legR = root.getChild("legR");
        legL = root.getChild("legL");
        armR = root.getChild("armR");
        armL = root.getChild("armL");
        shadowRadius = 0;
    }

    private static CubeListBuilder stick(float len) {
        return CubeListBuilder.create().texOffs(0, 0).addBox(-len / 2, -0.5F, -0.5F, len, 1, 1);
    }

    /** 0.19.1: a single thin black hair along x, centred on the pivot's outer end. */
    private static CubeListBuilder hair(float len) {
        return CubeListBuilder.create().texOffs(0, 0).addBox(0, -0.2F, -0.2F, len, 0.4F, 0.4F);
    }

    private static void limb(PartDefinition root, String name, int s, boolean arm, Random r) {
        float th = arm ? 26 : 20, sh = arm ? 50 : 44;
        PartDefinition thigh = root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(0, 0)
                .addBox(s > 0 ? 0 : -th, -0.5F, -0.5F, th, 1, 1), PartPose.offsetAndRotation(s * 1.0F, 0, 0, 0, 0, -s * 0.75F));
        PartDefinition shin = thigh.addOrReplaceChild("shin", CubeListBuilder.create().texOffs(0, 0)
                .addBox(s > 0 ? 0 : -sh, -0.5F, -0.5F, sh, 1, 1), PartPose.offsetAndRotation(s * th, 0, 0, 0, 0, s * 1.95F));
        thigh.addOrReplaceChild("strand", CubeListBuilder.create().texOffs(0, 0).addBox(s > 0 ? 0 : -th, -0.3F, -0.3F, th, 0.6F, 0.6F),
                PartPose.offsetAndRotation(0, 0.8F, 0.4F, 0.1F, 0.05F, -s * 0.06F));
        for (int h = 0; h < 7; h++) {                                   // 0.19.1: bristles on thigh and shin
            float t = r.nextFloat();
            float out = (r.nextBoolean() ? 1 : -1) * (1.2F + r.nextFloat() * 0.8F);
            thigh.addOrReplaceChild("h" + h, hair(2 + r.nextFloat() * 3), PartPose.offsetAndRotation(s * t * th, 0, 0, r.nextFloat() - 0.5F, 0, out));
            shin.addOrReplaceChild("h" + h, hair(2 + r.nextFloat() * 4), PartPose.offsetAndRotation(s * t * sh, 0, 0, r.nextFloat() - 0.5F, 0, out));
        }
        if (arm) {
            for (int f = 0; f < 4; f++) {
                shin.addOrReplaceChild("f" + f, CubeListBuilder.create().texOffs(0, 0).addBox(s > 0 ? 0 : -7, -0.4F, -0.4F, 7, 0.8F, 0.8F),
                        PartPose.offsetAndRotation(s * sh, 0, 0, 0, (f - 1.5F) * 0.45F, s * 0.25F));
            }
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        Random r = new Random(1919);
        // head: a scribble ball, wider than tall, with an eye and a jaw of lines
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        for (int i = 0; i < 64; i++) {
            double th = r.nextDouble() * Math.PI * 2, ph = Math.acos(2 * r.nextDouble() - 1), rad = 11 + r.nextDouble() * 8;
            float x = (float) (rad * Math.sin(ph) * Math.cos(th) * 1.25), y = (float) (rad * Math.cos(ph) * 0.85) - 6, z = (float) (rad * Math.sin(ph) * Math.sin(th));
            head.addOrReplaceChild("s" + i, stick(8 + r.nextFloat() * 18), PartPose.offsetAndRotation(x, y, z,
                    r.nextFloat() * 3.14F, r.nextFloat() * 3.14F, r.nextFloat() * 3.14F));
        }
        head.addOrReplaceChild("eye", CubeListBuilder.create().texOffs(116, 120).addBox(-2, -2, -0.5F, 4, 4, 1), PartPose.offset(5, -9, 17));
        for (int i = 0; i < 7; i++) {
            head.addOrReplaceChild("jaw" + i, CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -0.5F, 0, 1, 1, 12 + r.nextFloat() * 8),
                    PartPose.offsetAndRotation(-6 + i * 2, 2 + r.nextFloat() * 3, 12, 0.35F + r.nextFloat() * 0.4F, (i - 3) * 0.08F, 0));
        }
        for (int i = 0; i < 6; i++) {
            head.addOrReplaceChild("neck" + i, CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -0.5F, -18, 1, 1, 18),
                    PartPose.offsetAndRotation(r.nextFloat() * 4 - 2, r.nextFloat() * 4 - 2, -4, r.nextFloat() * 0.2F - 0.1F, r.nextFloat() * 0.2F - 0.1F, 0));
        }
        for (int i = 0; i < 16; i++) {                                 // 0.19.1: long hair strands trailing back from the head
            double a = r.nextDouble() * Math.PI * 2;
            float len = 10 + r.nextFloat() * 14;
            head.addOrReplaceChild("hair" + i, CubeListBuilder.create().texOffs(0, 0).addBox(-0.2F, -0.2F, -len, 0.4F, 0.4F, len),
                    PartPose.offsetAndRotation((float) Math.cos(a) * 12, (float) Math.sin(a) * 9 - 8, -10, -0.3F + r.nextFloat() * 0.6F, r.nextFloat() * 0.6F - 0.3F, 0));
        }
        // segment: spine of several strands
        PartDefinition seg = root.addOrReplaceChild("seg", CubeListBuilder.create().texOffs(0, 0).addBox(-1, -1, -15, 2, 2, 30), PartPose.ZERO);
        for (int i = 0; i < 4; i++) {
            seg.addOrReplaceChild("st" + i, CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -0.5F, -16, 1, 1, 32),
                    PartPose.offsetAndRotation(r.nextFloat() * 3 - 1.5F, r.nextFloat() * 3 - 1.5F, 0, r.nextFloat() * 0.1F - 0.05F, r.nextFloat() * 0.1F - 0.05F, 0));
        }
        for (int i = 0; i < 3; i++) {
            seg.addOrReplaceChild("sp" + i, CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -6, -0.5F, 1, 6, 1),
                    PartPose.offsetAndRotation(0, -1, -10 + i * 10, -0.4F, 0, r.nextFloat() * 0.4F - 0.2F));
        }
        for (int i = 0; i < 60; i++) {                                 // 0.19.1: dense black body hair along the spine
            float a = (float) (r.nextDouble() * Math.PI * 2);
            float z = -15 + r.nextFloat() * 30;
            seg.addOrReplaceChild("hair" + i, hair(2.5F + r.nextFloat() * 5.5F),
                    PartPose.offsetAndRotation((float) Math.cos(a) * 1.0F, (float) Math.sin(a) * 1.0F, z, 0, -0.5F + r.nextFloat() * 0.3F, a));
        }
        // ribcage: broken rings around the spine + sternum
        PartDefinition rib = root.addOrReplaceChild("rib", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -0.5F, -12, 1, 1, 24), PartPose.offset(0, -12, 0));
        for (int j = 0; j < 4; j++) {
            float z = -9 + j * 6, rad = (j == 1 || j == 2) ? 11 : 8;
            for (int k = 0; k < 10; k++) {
                if (r.nextFloat() < 0.15F) {
                    continue;
                }
                float a = (float) (k * Math.PI * 2 / 10);
                rib.addOrReplaceChild("r" + j + "_" + k, stick((float) (2 * Math.PI * rad / 10 * 1.15)),
                        PartPose.offsetAndRotation((float) Math.cos(a) * rad, (float) Math.sin(a) * rad + 12, z, 0, 0, a + 1.5708F));
            }
        }
        limb(root, "legR", 1, false, r);
        limb(root, "legL", -1, false, r);
        limb(root, "armR", 1, true, r);
        limb(root, "armL", -1, true, r);
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public boolean shouldRender(Unknown e, Frustum f, double x, double y, double z) {
        return e.distanceToSqr(x, y, z) < 192 * 192;
    }

    @Override
    public ResourceLocation getTextureLocation(Unknown e) {
        return TEX;
    }

    private static Vec3 lerp(net.minecraft.world.entity.Entity e, float pt) {
        return new Vec3(Mth.lerp(pt, e.xo, e.getX()), Mth.lerp(pt, e.yo, e.getY()), Mth.lerp(pt, e.zo, e.getZ()));
    }

    private static void orient(PoseStack ps, Vec3 dir) {
        if (dir.lengthSqr() < 1e-6) {
            dir = new Vec3(0, 0, 1);
        }
        ps.mulPose(Axis.YP.rotation((float) Mth.atan2(dir.x, dir.z)));
        ps.mulPose(Axis.XP.rotation((float) -Mth.atan2(dir.y, dir.horizontalDistance())));
        ps.scale(-1, -1, 1);
    }

    @Override
    public void render(Unknown e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(TEX));
        int overlay = OverlayTexture.pack(OverlayTexture.u(0), OverlayTexture.v(e.hurtTime > 0 || e.deathTime > 0));
        Vec3 base = lerp(e, pt);
        UnknownPart[] parts = e.parts();
        float time = e.tickCount + pt;
        Vec3 headDir = base.subtract(lerp(parts[0], pt));
        ps.pushPose();
        ps.translate(0, SPINE_Y, 0);
        orient(ps, headDir);
        head.render(ps, vc, light, overlay);
        ps.popPose();
        int alive = Unknown.SEGMENTS - e.deathTime / 3;
        Vec3 front = base;
        for (int i = 0; i < Unknown.SEGMENTS; i++) {
            Vec3 p = lerp(parts[i], pt);
            Vec3 dir = front.subtract(p);
            front = p;
            if (i >= alive) {
                continue;
            }
            ps.pushPose();
            ps.translate(p.x - base.x, p.y - base.y + SPINE_Y, p.z - base.z);
            orient(ps, dir);
            seg.render(ps, vc, light, overlay);
            boolean ribs = i % 3 == 1;
            if (ribs) {
                rib.render(ps, vc, light, overlay);
            }
            ModelPart r = ribs ? armR : legR, l = ribs ? armL : legL;
            for (int k = 0; k < 2; k++) {
                float sw = Mth.sin(time * 0.55F + i * 1.3F + k * 3.1416F) * 0.45F;
                r.xRot = sw;
                l.xRot = -sw;
                ps.pushPose();
                ps.translate(0, 0, (k == 0 ? -7 : 7) / 16F);
                r.render(ps, vc, light, overlay);
                l.render(ps, vc, light, overlay);
                ps.popPose();
                if (ribs) {
                    break;
                }
            }
            ps.popPose();
        }
        super.render(e, yaw, pt, ps, buf, light);
    }
}
