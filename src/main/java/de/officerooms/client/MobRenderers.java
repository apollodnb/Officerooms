package de.officerooms.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.officerooms.core.OfficeRoomsIds;
import de.officerooms.entity.Wolfspider;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Code-model renderers (client only). Since 0.26.2 all other creatures are GeckoLib models ({@link GeoMobRenderer});
 * 0.39.1 removed their unused code models (Gaunt, Colony, Headless Warden, Observer, Hound, Smiler, Hunter).
 */
public final class MobRenderers {

    public static final ModelLayerLocation DREAD_WOLFSPIDER_LAYER = new ModelLayerLocation(new ResourceLocation(OfficeRoomsIds.MOD_ID, "wolfspider_dread"), "main");
    public static final ModelLayerLocation WOLFSPIDER_LAYER = new ModelLayerLocation(new ResourceLocation(OfficeRoomsIds.MOD_ID, "wolfspider"), "main");

    private MobRenderers() {
    }

    /** Freundliche Wolfspider: schwarzer Körper, weiße, selbstleuchtende Augen. */
    public static final class FriendlyWolfspiderRenderer extends MobRenderer<de.officerooms.entity.FriendlyWolfspider,
            WolfspiderModel<de.officerooms.entity.FriendlyWolfspider>> {
        private static final ResourceLocation TEXTURE = new ResourceLocation(OfficeRoomsIds.MOD_ID, "textures/entity/wolfspider_friendly.png");
        private static final RenderType EYES = RenderType.eyes(new ResourceLocation(OfficeRoomsIds.MOD_ID, "textures/entity/wolfspider_friendly_eyes.png"));

        public FriendlyWolfspiderRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new WolfspiderModel<>(ctx.bakeLayer(WOLFSPIDER_LAYER)), 0.6F);
            addLayer(new net.minecraft.client.renderer.entity.layers.EyesLayer<>(this) {
                @Override
                public RenderType renderType() {
                    return EYES;
                }
            });
        }

        @Override
        protected void scale(de.officerooms.entity.FriendlyWolfspider e, PoseStack pose, float partial) {
            pose.scale(0.8F, 0.8F, 0.8F);
        }

        @Override
        protected float getFlipDegrees(de.officerooms.entity.FriendlyWolfspider e) {
            return 180.0F;
        }

        @Override
        public ResourceLocation getTextureLocation(de.officerooms.entity.FriendlyWolfspider e) {
            return TEXTURE;
        }
    }

    /** 0.26.0: the hostile Wolfspider uses its own gaunt model ({@link DreadWolfspiderModel}) with glowing eyes. */
    public static final class WolfspiderRenderer extends MobRenderer<Wolfspider, DreadWolfspiderModel> {
        private static final ResourceLocation TEXTURE = new ResourceLocation(OfficeRoomsIds.MOD_ID, "textures/entity/wolfspider_dread.png");
        private static final net.minecraft.client.renderer.RenderType EYES =
                net.minecraft.client.renderer.RenderType.eyes(new ResourceLocation(OfficeRoomsIds.MOD_ID, "textures/entity/wolfspider_dread_eyes.png"));

        public WolfspiderRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new DreadWolfspiderModel(ctx.bakeLayer(DREAD_WOLFSPIDER_LAYER)), 0.7F);
            addLayer(new net.minecraft.client.renderer.entity.layers.EyesLayer<>(this) {
                @Override
                public net.minecraft.client.renderer.RenderType renderType() {
                    return EYES;
                }
            });
        }

        @Override
        protected float getFlipDegrees(Wolfspider e) {
            return 180.0F;
        }

        @Override
        public ResourceLocation getTextureLocation(Wolfspider e) {
            return TEXTURE;
        }
    }
}
