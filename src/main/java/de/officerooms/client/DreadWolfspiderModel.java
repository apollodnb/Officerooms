package de.officerooms.client;

import de.officerooms.entity.Wolfspider;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * 0.26.0: the hostile Wolfspider redesigned (own design). A gaunt wolf skull with a hanging jaw and too many eyes
 * on a narrow thorax, a long drooping segmented abdomen and eight stilt legs of three hair-thin segments with
 * high knees. Movement is deliberately wrong: staccato steps, single legs tapping, the head snapping to a tilt
 * and back, the jaw chattering when it hunts. Texture 64 × 64 (+ glowing eyes layer).
 */
public class DreadWolfspiderModel extends HierarchicalModel<Wolfspider> {

    private static final CubeDeformation THIN = new CubeDeformation(-0.36F);
    private static final CubeDeformation SLIM = new CubeDeformation(-0.2F);
    /** Leg spread (yaw) front to back, degrees. */
    private static final float[] SPREAD = {52, 20, -16, -46};
    private static final float COXA_UP = -42F;
    private static final float FEMUR_REL = 104F;
    private static final float TIBIA_REL = 18F;

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart[] abdomen = new ModelPart[3];
    private final ModelPart[][] legs = new ModelPart[8][3];  // coxa, femur, tibia

    public DreadWolfspiderModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = body.getChild("neck").getChild("head");
        this.jaw = head.getChild("jaw");
        ModelPart a = body;
        for (int i = 0; i < 3; i++) {
            a = a.getChild("abdomen" + i);
            abdomen[i] = a;
        }
        for (int l = 0; l < 8; l++) {
            ModelPart coxa = body.getChild("hip" + l).getChild("coxa");
            legs[l][0] = coxa;
            legs[l][1] = coxa.getChild("femur");
            legs[l][2] = legs[l][1].getChild("tibia");
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 8).addBox(-1.5F, -1.5F, -4F, 3, 3, 8, SLIM)
                .texOffs(52, 30).addBox(-0.5F, -3.5F, -2F, 1, 3, 1, THIN)
                .texOffs(52, 30).addBox(-0.5F, -3.2F, 1F, 1, 3, 1, THIN), PartPose.offset(0, 9.5F, 0));
        PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create()
                .texOffs(24, 8).addBox(-0.5F, -0.5F, -6F, 1, 1, 6, new CubeDeformation(-0.25F)),
                PartPose.offsetAndRotation(0, -0.5F, -3.5F, -0.35F, 0, 0));
        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 30).addBox(-2F, -2.5F, -3.5F, 4, 3, 4, SLIM)              // skull
                .texOffs(16, 30).addBox(-1F, -1.6F, -8.5F, 2, 2, 5, SLIM)             // long snout
                .texOffs(44, 30).addBox(-2F, -6.5F, -1F, 1, 4, 1, THIN)               // ears (thin spikes)
                .texOffs(44, 30).addBox(1F, -6.5F, -1F, 1, 4, 1, THIN)
                .texOffs(48, 30).addBox(-2.1F, -2.3F, -3.6F, 1, 1, 1, new CubeDeformation(-0.18F))   // eyes: 3 per side
                .texOffs(48, 30).addBox(1.1F, -2.3F, -3.6F, 1, 1, 1, new CubeDeformation(-0.18F))
                .texOffs(48, 30).addBox(-1.6F, -1.4F, -4.3F, 1, 1, 1, new CubeDeformation(-0.28F))
                .texOffs(48, 30).addBox(0.6F, -1.4F, -4.3F, 1, 1, 1, new CubeDeformation(-0.28F))
                .texOffs(48, 30).addBox(-1.4F, -2.6F, -2.2F, 1, 1, 1, new CubeDeformation(-0.3F))
                .texOffs(48, 30).addBox(0.4F, -2.6F, -2.2F, 1, 1, 1, new CubeDeformation(-0.3F)),
                PartPose.offsetAndRotation(0, 0, -6F, 0.35F, 0, 0));
        head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(30, 30).addBox(-1F, 0, -5F, 2, 1, 5, SLIM), PartPose.offset(0, 0.3F, -3F));
        // abdomen: three narrowing segments drooping behind
        PartDefinition a0 = body.addOrReplaceChild("abdomen0", CubeListBuilder.create()
                .texOffs(0, 20).addBox(-1.5F, -1.5F, 0, 3, 3, 5, SLIM)
                .texOffs(52, 30).addBox(-0.5F, -4F, 1F, 1, 3, 1, THIN), PartPose.offsetAndRotation(0, 0, 3.6F, 0.25F, 0, 0));
        PartDefinition a1 = a0.addOrReplaceChild("abdomen1", CubeListBuilder.create()
                .texOffs(0, 20).addBox(-1.5F, -1.5F, 0, 3, 3, 5, new CubeDeformation(-0.35F))
                .texOffs(52, 30).addBox(-0.5F, -3.6F, 2F, 1, 3, 1, THIN), PartPose.offsetAndRotation(0, 0, 4.4F, 0.3F, 0, 0));
        a1.addOrReplaceChild("abdomen2", CubeListBuilder.create()
                .texOffs(16, 20).addBox(-1F, -1F, 0, 2, 2, 4, SLIM), PartPose.offsetAndRotation(0, 0, 4.2F, 0.35F, 0, 0));
        // legs: hip (yaw) -> coxa (up) -> femur (down) -> tibia (to the ground)
        float[] zs = {-2.6F, -0.9F, 0.9F, 2.6F};
        for (int l = 0; l < 8; l++) {
            int i = l % 4;
            boolean left = l < 4;
            float s = left ? 1F : -1F;
            PartDefinition hip = body.addOrReplaceChild("hip" + l, CubeListBuilder.create(),
                    PartPose.offsetAndRotation(1.2F * s, 0.2F, zs[i], 0, (left ? SPREAD[i] : -SPREAD[i]) * Mth.DEG_TO_RAD, 0));
            CubeListBuilder coxa = CubeListBuilder.create().texOffs(0, 0);
            CubeListBuilder femur = CubeListBuilder.create().texOffs(0, 2);
            CubeListBuilder tibia = CubeListBuilder.create().texOffs(0, 4);
            if (left) {
                coxa.addBox(0, -0.5F, -0.5F, 9, 1, 1, THIN).texOffs(30, 0).addBox(8.4F, -0.5F, -0.5F, 1, 1, 1, new CubeDeformation(-0.15F));
                femur.addBox(0, -0.5F, -0.5F, 12, 1, 1, THIN).texOffs(30, 0).addBox(11.4F, -0.5F, -0.5F, 1, 1, 1, new CubeDeformation(-0.2F));
                tibia.addBox(0, -0.5F, -0.5F, 10.5F, 1, 1, THIN).texOffs(30, 0).addBox(10F, -0.5F, -0.5F, 1, 1, 1, new CubeDeformation(-0.3F));
            } else {
                coxa.addBox(-9, -0.5F, -0.5F, 9, 1, 1, THIN).texOffs(30, 0).addBox(-9.4F, -0.5F, -0.5F, 1, 1, 1, new CubeDeformation(-0.15F));
                femur.addBox(-12, -0.5F, -0.5F, 12, 1, 1, THIN).texOffs(30, 0).addBox(-12.4F, -0.5F, -0.5F, 1, 1, 1, new CubeDeformation(-0.2F));
                tibia.addBox(-10.5F, -0.5F, -0.5F, 10.5F, 1, 1, THIN).texOffs(30, 0).addBox(-11F, -0.5F, -0.5F, 1, 1, 1, new CubeDeformation(-0.3F));
            }
            PartDefinition c = hip.addOrReplaceChild("coxa", coxa, PartPose.ZERO);
            PartDefinition f = c.addOrReplaceChild("femur", femur, PartPose.offset(9F * s, 0, 0));
            f.addOrReplaceChild("tibia", tibia, PartPose.offset(12F * s, 0, 0));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    /** Deterministic noise per entity and time slot, 0..1. */
    private static float rnd(int id, int slot, int salt) {
        int h = id * 0x27D4EB2D ^ slot * 0x165667B1 ^ salt * 0x5BD1E995;
        h ^= h >>> 15;
        h *= 0x2C1B3C6D;
        h ^= h >>> 12;
        return (h & 0xFFFF) / 65535F;
    }

    @Override
    public void setupAnim(Wolfspider e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        int id = e.getId();
        boolean hunting = e.isAggressive() || e.getTarget() != null;
        float move = Math.min(1F, limbSwingAmount * 1.6F);

        // body: slight bob, sometimes a shiver
        int slot = (int) (age / 9F);
        float shiver = rnd(id, slot, 1) < 0.18F ? Mth.sin(age * 9F) * 0.6F : 0F;
        body.y += Mth.sin(age * 0.12F) * 0.25F + shiver * 0.3F;
        body.zRot += shiver * 0.04F;

        // head: follows the target, plus wrong tilts that snap in and out
        head.yRot += netHeadYaw * Mth.DEG_TO_RAD * 0.8F;
        head.xRot += headPitch * Mth.DEG_TO_RAD * 0.6F;
        int hslot = (int) (age / 23F);
        float tilt = rnd(id, hslot, 2);
        if (tilt < 0.35F) {
            head.zRot += (tilt < 0.17F ? 1F : -1F) * (0.7F + rnd(id, hslot, 3) * 0.6F);     // sideways, almost 90°
        } else if (tilt < 0.45F) {
            head.zRot += Mth.PI * 0.5F * (rnd(id, hslot, 4) < 0.5F ? 1F : -1F);
        }
        if (rnd(id, (int) (age / 3F), 5) < 0.12F) {                                          // micro twitch
            head.yRot += (rnd(id, (int) age, 6) - 0.5F) * 0.5F;
        }

        // jaw: hangs open, chatters while hunting
        float open = 0.35F + Mth.sin(age * 0.05F) * 0.1F;
        if (hunting) {
            open = 0.45F + Math.abs(Mth.sin(age * 2.4F)) * 0.45F;
        }
        jaw.xRot += open;

        // abdomen sways like something hanging
        for (int i = 0; i < 3; i++) {
            abdomen[i].yRot += Mth.sin(age * 0.07F + i * 0.9F) * 0.12F * (i + 1);
            abdomen[i].xRot += Mth.sin(age * 0.05F + i) * 0.05F;
        }

        // legs: staccato gait (alternating tetrapod), quantised so they jerk instead of gliding
        float walk = limbSwing * 0.9F;
        for (int l = 0; l < 8; l++) {
            int i = l % 4;
            boolean left = l < 4;
            float s = left ? 1F : -1F;
            int group = (i + (left ? 0 : 1)) % 2;
            float ph = walk + group * Mth.PI;
            float raw = Mth.sin(ph);
            float stepped = Math.round(raw * 3F) / 3F;                // jerky
            float lift = Math.max(0F, stepped) * move;
            float sweep = Mth.cos(ph) * 0.35F * move;
            // idle: single legs tapping now and then
            int tslot = (int) (age / 11F);
            if (move < 0.1F && rnd(id, tslot, 10 + l) < 0.07F) {
                lift = Math.max(lift, Math.abs(Mth.sin(age * 1.7F)) * 0.8F);
            }
            ModelPart coxa = legs[l][0];
            ModelPart femur = legs[l][1];
            ModelPart tibia = legs[l][2];
            coxa.zRot = s * (COXA_UP - lift * 22F) * Mth.DEG_TO_RAD;
            coxa.yRot = -s * sweep;
            femur.zRot = s * (FEMUR_REL - lift * 30F) * Mth.DEG_TO_RAD;
            tibia.zRot = s * (TIBIA_REL + lift * 25F + (hunting ? 6F : 0F)) * Mth.DEG_TO_RAD;
        }
    }
}
