package de.officerooms.client;

import de.officerooms.entity.FriendlyWolfspider;
import net.minecraft.world.entity.Mob;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Eigenes Modell der Wolfspider: Spinnenkörper (Brustteil, Hinterleib, acht Beine) mit einem
 * Wolfskopf (Schädel, Schnauze, Ohren). Eigene Geometrie und eigenes UV-Layout (Textur 64 × 32).
 */
public class WolfspiderModel<T extends Mob> extends HierarchicalModel<T> {

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart[] rightLegs = new ModelPart[4];
    private final ModelPart[] leftLegs = new ModelPart[4];

    public WolfspiderModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        for (int i = 0; i < 4; i++) {
            rightLegs[i] = root.getChild("right_leg" + i);
            leftLegs[i] = root.getChild("left_leg" + i);
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("thorax", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -3, -3, 6, 6, 6), PartPose.offset(0, 15, 0));
        root.addOrReplaceChild("abdomen", CubeListBuilder.create().texOffs(0, 12).addBox(-5, -4, -6, 10, 8, 12), PartPose.offset(0, 15, 9));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(44, 12).addBox(-3, -3, -4, 6, 6, 4)
                .texOffs(44, 22).addBox(-1.5F, 0, -8, 3, 3, 4)
                .texOffs(24, 4).addBox(-3, -5, -3, 2, 2, 1)
                .texOffs(30, 4).addBox(1, -5, -3, 2, 2, 1), PartPose.offset(0, 14, -3));
        float[] zs = {2, 1, 0, -1};
        for (int i = 0; i < 4; i++) {
            root.addOrReplaceChild("right_leg" + i, CubeListBuilder.create().texOffs(24, 0).addBox(-15, -1, -1, 16, 2, 2), PartPose.offset(-4, 15, zs[i]));
            root.addOrReplaceChild("left_leg" + i, CubeListBuilder.create().texOffs(24, 0).mirror().addBox(-1, -1, -1, 16, 2, 2), PartPose.offset(4, 15, zs[i]));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(T e, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        // Sitzen (freundliche Variante): Körper abgesenkt, Beine flach ausgebreitet
        boolean sitting = e instanceof FriendlyWolfspider f && f.isInSittingPose();
        root.y = sitting ? 4F : 0F;
        if (sitting) {
            limbSwingAmount = 0F;
        }
        float q = Mth.PI / 4F;
        // Grundhaltung (hinten -> vorne)
        float[] baseZ = {q, 0.58F, 0.58F, q};
        float[] baseY = {q, 0.39F, -0.39F, -q};
        for (int i = 0; i < 4; i++) {
            float phase = i * Mth.PI / 2F;
            float swingY = -(Mth.cos(limbSwing * 0.6662F * 2F + phase) * 0.4F) * limbSwingAmount;
            float swingZ = Math.abs(Mth.sin(limbSwing * 0.6662F + phase) * 0.4F) * limbSwingAmount;
            float sign = (i % 2 == 0) ? 1F : -1F;
            float flat = sitting ? 0.35F : 1F;
            rightLegs[i].zRot = (-baseZ[i] - swingZ) * flat;
            leftLegs[i].zRot = (baseZ[i] + swingZ) * flat;
            rightLegs[i].yRot = baseY[i] + sign * swingY;
            leftLegs[i].yRot = -baseY[i] - sign * swingY;
        }
    }
}
