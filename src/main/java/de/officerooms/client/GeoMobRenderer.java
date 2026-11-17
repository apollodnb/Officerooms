package de.officerooms.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.officerooms.core.OfficeRoomsIds;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * 0.11.0: one renderer for all redesigned creatures (geo/entity/&lt;name&gt;.geo.json,
 * animations/entity/&lt;name&gt;.animation.json, textures/entity/&lt;name&gt;.png + _glowmask.png).
 */
public class GeoMobRenderer<T extends LivingEntity & GeoAnimatable> extends GeoEntityRenderer<T> {

    private final boolean translucent;
    private final boolean fullBright;

    public GeoMobRenderer(EntityRendererProvider.Context ctx, String name, float shadow, float scale, boolean translucent, boolean fullBright) {
        super(ctx, new DefaultedEntityGeoModel<>(new ResourceLocation(OfficeRoomsIds.MOD_ID, name), true));
        // Glow layer: drawn full bright with the vanilla "eyes" render type, which also works in dimensions
        // without sky light (GeckoLib's own glow layer uses sky light and stays dark in the Office Rooms).
        RenderType glow = RenderType.eyes(new ResourceLocation(OfficeRoomsIds.MOD_ID, "textures/entity/" + name + "_glowmask.png"));
        addRenderLayer(new GeoRenderLayer<>(this) {
            @Override
            public void render(PoseStack pose, T animatable, BakedGeoModel model, RenderType type, MultiBufferSource buffers,
                               VertexConsumer buffer, float partialTick, int light, int overlay) {
                getRenderer().reRender(model, pose, buffers, animatable, glow, buffers.getBuffer(glow), partialTick,
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            }
        });
        this.shadowRadius = shadow;
        this.translucent = translucent;
        this.fullBright = fullBright;
        if (scale != 1.0F) {
            withScale(scale);
        }
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture, MultiBufferSource buffers, float partialTick) {
        if (translucent) {
            return RenderType.entityTranslucent(texture);
        }
        return fullBright ? RenderType.entityCutoutNoCull(texture) : super.getRenderType(animatable, texture, buffers, partialTick);
    }

    @Override
    public void render(T entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(entity, yaw, partialTick, pose, buffers, fullBright ? LightTexture.FULL_BRIGHT : light);
    }

    @Override
    public boolean shouldShowName(T entity) {
        return false;
    }
}
