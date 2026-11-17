package de.officerooms.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import de.officerooms.OfficeRooms;
import de.officerooms.core.OfficeRoomsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.fml.ModList;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.io.DataInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;

/**
 * Experimentelle Darstellung der OfficeCraft-Möbel mit Shaderpacks (Oculus/Iris).
 *
 * <p>OfficeCraft zeichnet seine Möbel mit eigenem VertexBuffer und eigenem Shader an Minecrafts
 * Rendertypen vorbei. Oculus/Iris ersetzen die Render-Pipeline durch den Shaderpack und erfassen
 * diese Zeichnungen nicht: die Möbel sind unsichtbar (nur Umrisse). Diese Klasse ersetzt den
 * Block-Entity-Renderer von OfficeCraft durch eine Hülle: ohne Shaderpack zeichnet weiter das
 * Original, mit Shaderpack liest sie dieselben Mesh-Dateien aus dem installierten OfficeCraft-JAR
 * (nichts wird mitgeliefert) und gibt die Dreiecke über normale Entity-Rendertypen aus, die Iris
 * verarbeitet. Alles über Reflection; Getestet gegen OfficeCraft 1.0.3.</p>
 */
public final class OfficeCraftShaderCompat {

    private static final String OC = "gamefantom_officecraft";
    private static final int MAGIC = 0x564D4431; // "VMD1"

    /** Ein Material eines Meshes: x y z u v nx ny nz je Ecke (8 Floats) und RGBA (ein int je Ecke). */
    private record Group(ResourceLocation texture, boolean emissive, boolean transparent, boolean screen, float[] v, int[] rgba,
                         float[][] lodV, int[][] lodC) {
    }

    /** 0.9.0: simplified shapes. Share of the surface area kept and triangle cap per mesh for LOD 1 and 2. */
    private static final float[] LOD_AREA = {0.90F, 0.70F};
    private static final int[] LOD_CAP = {600, 150};
    private static Method irisShadowPass;

    private static final Map<String, List<Group>> MESHES = new ConcurrentHashMap<>();
    private static boolean failed;

    // Reflection auf OfficeCraft
    private static Method mEntry, mGrid, mScreenOn, mProps, mOffset, mScaleX, mScaleY, mScaleZ, mCatalogGet;
    private static Method pId, pX, pY, pZ, pTurns, pScreen;
    private static Field fId, fLight;
    private static Property<?> facingProp, litProp;
    private static Constructor<?> originalCtor;

    // Shaderpack-Erkennung (Iris-API, von Oculus mitgeliefert)
    private static Object irisApi;
    private static Method irisInUse;
    private static boolean irisLooked;
    private static long irisCheckedAt;
    private static boolean irisActive;

    private OfficeCraftShaderCompat() {
    }

    /** Ersetzt OfficeCraft's Renderer, falls OfficeCraft installiert ist (läuft nach OfficeCraft, siehe mods.toml). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        if (!ModList.get().isLoaded(OC)) {
            return;
        }
        try {
            BlockEntityType<?> type = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(new ResourceLocation(OC, "furniture"));
            if (type == null) {
                return;
            }
            init();
            event.registerBlockEntityRenderer((BlockEntityType) type, ctx -> new Wrapper(ctx));
            OfficeRooms.LOGGER.info("OfficeCraft shader compatibility renderer installed.");
        } catch (Throwable t) {
            OfficeRooms.LOGGER.warn("OfficeCraft shader compatibility not available: {}", t.toString());
        }
    }

    private static void init() throws ReflectiveOperationException {
        Class<?> ent = Class.forName("com.gamefantom.officecraft.FurnitureEntity");
        Class<?> entry = Class.forName("com.gamefantom.officecraft.Catalog$Entry");
        Class<?> prop = Class.forName("com.gamefantom.officecraft.FurnitureEntity$Prop");
        Class<?> block = Class.forName("com.gamefantom.officecraft.FurnitureBlock");
        Class<?> catalog = Class.forName("com.gamefantom.officecraft.Catalog");
        mEntry = ent.getMethod("entry");
        mGrid = ent.getMethod("gridAligned");
        mScreenOn = ent.getMethod("screenOn");
        mProps = ent.getMethod("props");
        mOffset = entry.getMethod("offset", boolean.class);
        mScaleX = entry.getMethod("scaleX");
        mScaleY = entry.getMethod("scaleY");
        mScaleZ = entry.getMethod("scaleZ");
        fId = entry.getField("id");
        fLight = entry.getField("light");
        mCatalogGet = catalog.getMethod("get", String.class);
        pId = prop.getMethod("id");
        pX = prop.getMethod("x");
        pY = prop.getMethod("y");
        pZ = prop.getMethod("z");
        pTurns = prop.getMethod("turns");
        pScreen = prop.getMethod("screenOn");
        facingProp = (Property<?>) block.getField("FACING").get(null);
        litProp = (Property<?>) block.getField("LIT").get(null);
        originalCtor = Class.forName("com.gamefantom.officecraft.client.FurnitureRenderer").getConstructor(BlockEntityRendererProvider.Context.class);
    }

    /** Ist gerade ein Shaderpack aktiv? (höchstens alle 0,5 s nachgefragt) */
    static boolean shaderPackActive() {
        long now = System.nanoTime();
        if (now - irisCheckedAt < 500_000_000L) {
            return irisActive;
        }
        irisCheckedAt = now;
        if (!irisLooked) {
            irisLooked = true;
            for (String cls : new String[]{"net.irisshaders.iris.api.v0.IrisApi", "net.coderbot.iris.apiimpl.IrisApiV0Impl"}) {
                try {
                    Class<?> c = Class.forName(cls);
                    irisApi = c.getMethod("getInstance").invoke(null);
                    irisInUse = irisApi.getClass().getMethod("isShaderPackInUse");
                    try {
                        irisShadowPass = irisApi.getClass().getMethod("isRenderingShadowPass");
                    } catch (Throwable noShadowApi) {
                        irisShadowPass = null;
                    }
                    OfficeRooms.LOGGER.info("OfficeCraft shader compatibility: shader API found ({}).", cls);
                    break;
                } catch (Throwable ignored) {
                    // nächster Kandidat
                }
            }
        }
        try {
            irisActive = irisInUse != null && (boolean) irisInUse.invoke(irisApi);
        } catch (Throwable t) {
            irisActive = false;
        }
        return irisActive;
    }

    static boolean useCompat() {
        OfficeRoomsConfig.ShaderCompat mode;
        try {
            mode = OfficeRoomsConfig.OFFICECRAFT_SHADER_COMPAT.get();
        } catch (IllegalStateException notLoaded) {
            mode = OfficeRoomsConfig.ShaderCompat.AUTO;
        }
        return switch (mode) {
            case ALWAYS -> true;
            case OFF -> false;
            default -> shaderPackActive();
        };
    }

    /** Hülle um OfficeCraft's Renderer. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static final class Wrapper implements BlockEntityRenderer<BlockEntity> {

        private final BlockEntityRenderer original;

        Wrapper(BlockEntityRendererProvider.Context ctx) {
            BlockEntityRenderer o = null;
            try {
                o = (BlockEntityRenderer) originalCtor.newInstance(ctx);
            } catch (Throwable t) {
                OfficeRooms.LOGGER.warn("OfficeCraft renderer could not be created: {}", t.toString());
            }
            this.original = o;
        }

        @Override
        public void render(BlockEntity be, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            if (failed || !useCompat()) {
                if (original != null) {
                    original.render(be, partial, pose, buffers, light, overlay);
                }
                return;
            }
            try {
                int lod = lodFor(be);
                if (lod < 0) {
                    return;
                }
                renderCompat(be, pose, buffers, light, lod);
            } catch (Throwable t) {
                failed = true;
                OfficeRooms.LOGGER.error("OfficeCraft shader compatibility failed, falling back to the original renderer", t);
            }
        }

        @Override
        public boolean shouldRenderOffScreen(BlockEntity be) {
            return true;
        }

        @Override
        public int getViewDistance() {
            return original != null ? original.getViewDistance() : 96;
        }

        @Override
        public boolean shouldRender(BlockEntity be, Vec3 camera) {
            return original != null ? original.shouldRender(be, camera) : BlockEntityRenderer.super.shouldRender(be, camera);
        }
    }

    // ------------------------------------------------------------------------------------------

    /** Level of detail for this piece of furniture: 0 full, 1 simplified, 2 coarse, -1 not drawn. */
    private static int lodFor(BlockEntity be) {
        OfficeRoomsConfig.ShaderDetail detail;
        int fullDist;
        int maxDist;
        boolean shadows;
        try {
            detail = OfficeRoomsConfig.OFFICECRAFT_SHADER_DETAIL.get();
            fullDist = OfficeRoomsConfig.OFFICECRAFT_SHADER_FULL_DISTANCE.get();
            maxDist = OfficeRoomsConfig.OFFICECRAFT_SHADER_RENDER_DISTANCE.get();
            shadows = OfficeRoomsConfig.OFFICECRAFT_SHADER_SHADOWS.get();
        } catch (IllegalStateException notLoaded) {
            detail = OfficeRoomsConfig.ShaderDetail.BALANCED;
            fullDist = 8;
            maxDist = 48;
            shadows = false;
        }
        boolean shadowPass = false;
        if (irisShadowPass != null && irisActive) {
            try {
                shadowPass = (boolean) irisShadowPass.invoke(irisApi);
            } catch (Throwable t) {
                irisShadowPass = null;
            }
        }
        if (shadowPass && !shadows) {
            return -1;
        }
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        net.minecraft.core.BlockPos p = be.getBlockPos();
        double dx = p.getX() + 0.5 - cam.x;
        double dy = p.getY() + 0.5 - cam.y;
        double dz = p.getZ() + 0.5 - cam.z;
        double d2 = dx * dx + dy * dy + dz * dz;
        if (d2 > (double) maxDist * maxDist) {
            return -1;
        }
        if (shadowPass) {
            return 2;
        }
        if (detail == OfficeRoomsConfig.ShaderDetail.FULL) {
            return 0;
        }
        double mid = Math.max(fullDist, maxDist * 0.5);
        if (detail == OfficeRoomsConfig.ShaderDetail.SIMPLE) {
            return d2 <= mid * mid ? 1 : 2;
        }
        return d2 <= (double) fullDist * fullDist ? 0 : d2 <= mid * mid ? 1 : 2;
    }

    private static void renderCompat(BlockEntity be, PoseStack pose, MultiBufferSource buffers, int light, int lod) throws ReflectiveOperationException {
        BlockState state = be.getBlockState();
        Object entry = mEntry.invoke(be);
        boolean grid = (boolean) mGrid.invoke(be);
        boolean lit = state.hasProperty(litProp) && (Boolean) state.getValue(litProp);
        Direction facing = state.hasProperty(facingProp) ? (Direction) state.getValue(facingProp) : Direction.NORTH;

        pose.pushPose();
        pose.translate(0.5, 0.0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        Vec3 off = (Vec3) mOffset.invoke(entry, grid);
        pose.translate(off.x, 0.0, off.z);

        pose.pushPose();
        scale(pose, entry, grid);
        drawMesh((String) fId.get(entry), pose, buffers, light, fLight.getInt(entry) == 0 || lit, (boolean) mScreenOn.invoke(be), lod);
        pose.popPose();

        for (Object p : (List<?>) mProps.invoke(be)) {
            String id = (String) pId.invoke(p);
            Object pe = mCatalogGet.invoke(null, id);
            if (pe == null) {
                continue;
            }
            pose.pushPose();
            pose.translate((double) pX.invoke(p), (double) pY.invoke(p), (double) pZ.invoke(p));
            pose.mulPose(Axis.YP.rotationDegrees((int) pTurns.invoke(p) * 90.0F));
            scale(pose, pe, grid);
            drawMesh(id, pose, buffers, light, fLight.getInt(pe) == 0 || lit, (boolean) pScreen.invoke(p), lod);
            pose.popPose();
        }
        pose.popPose();
    }

    private static void scale(PoseStack pose, Object entry, boolean grid) throws ReflectiveOperationException {
        if (grid) {
            pose.scale((float) (double) mScaleX.invoke(entry), (float) (double) mScaleY.invoke(entry), (float) (double) mScaleZ.invoke(entry));
        }
    }

    private static void drawMesh(String id, PoseStack pose, MultiBufferSource buffers, int light, boolean lit, boolean screenOn, int lod) {
        List<Group> groups = MESHES.computeIfAbsent(id, OfficeCraftShaderCompat::load);
        if (groups.isEmpty()) {
            return;
        }
        PoseStack.Pose last = pose.last();
        Matrix4f m = last.pose();
        Matrix3f n = last.normal();
        for (Group g : groups) {
            int l = light;
            boolean dark = false;
            if (g.screen && !screenOn) {
                dark = true;
            } else if (g.emissive) {
                if (lit) {
                    l = LightTexture.FULL_BRIGHT;
                } else {
                    dark = true;
                }
            }
            RenderType type = g.transparent ? RenderType.entityTranslucent(g.texture) : RenderType.entityCutoutNoCull(g.texture);
            VertexConsumer vc = buffers.getBuffer(type);
            float[] v = lod == 0 ? g.v : g.lodV[lod - 1];
            int[] c = lod == 0 ? g.rgba : g.lodC[lod - 1];
            int verts = c.length;
            if (verts == 0) {
                continue;
            }
            // Entity-Rendertypen zeichnen Vierecke: jedes Dreieck als Viereck mit doppelter letzter Ecke
            for (int i = 0; i + 2 < verts; i += 3) {
                emit(vc, m, n, v, c, i, l, dark);
                emit(vc, m, n, v, c, i + 1, l, dark);
                emit(vc, m, n, v, c, i + 2, l, dark);
                emit(vc, m, n, v, c, i + 2, l, dark);
            }
        }
    }

    private static void emit(VertexConsumer vc, Matrix4f m, Matrix3f n, float[] v, int[] c, int i, int light, boolean dark) {
        int o = i * 8;
        int rgba = c[i];
        int r = (rgba >>> 24) & 0xFF;
        int g = (rgba >>> 16) & 0xFF;
        int b = (rgba >>> 8) & 0xFF;
        int a = rgba & 0xFF;
        if (dark) {
            r = g = b = 8;
        }
        vc.vertex(m, v[o], v[o + 1], v[o + 2])
                .color(r, g, b, a)
                .uv(v[o + 3], v[o + 4])
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(n, v[o + 5], v[o + 6], v[o + 7])
                .endVertex();
    }

    /** Liest {@code meshes/<id>.vmesh.gz} aus den Ressourcen von OfficeCraft. */
    private static List<Group> load(String id) {
        List<Group> out = new ArrayList<>();
        ResourceLocation rl = new ResourceLocation(OC, "meshes/" + id + ".vmesh.gz");
        try (InputStream raw = Minecraft.getInstance().getResourceManager().open(rl);
             DataInputStream in = new DataInputStream(new java.io.BufferedInputStream(new GZIPInputStream(raw)))) {
            if (in.readInt() != MAGIC) {
                throw new java.io.IOException("unknown mesh header");
            }
            int materials = in.readInt();
            if (materials < 0 || materials > 100) {
                throw new java.io.IOException("invalid material count");
            }
            for (int mi = 0; mi < materials; mi++) {
                String tex = in.readUTF();
                int flags = in.readUnsignedByte();
                int count = in.readInt();
                if (count < 0 || count % 3 != 0 || count > 10_000_000) {
                    throw new java.io.IOException("invalid vertex count");
                }
                float[] v = new float[count * 8];
                int[] c = new int[count];
                for (int k = 0; k < count; k++) {
                    int o = k * 8;
                    v[o] = in.readFloat();
                    v[o + 1] = in.readFloat();
                    v[o + 2] = in.readFloat();
                    v[o + 3] = in.readFloat();
                    v[o + 4] = in.readFloat();
                    int r = in.readUnsignedByte();
                    int g = in.readUnsignedByte();
                    int b = in.readUnsignedByte();
                    int a = in.readUnsignedByte();
                    c[k] = (r << 24) | (g << 16) | (b << 8) | a;
                    v[o + 5] = in.readFloat();
                    v[o + 6] = in.readFloat();
                    v[o + 7] = in.readFloat();
                }
                out.add(new Group(new ResourceLocation(OC, "textures/mesh/" + tex), (flags & 1) != 0, (flags & 2) != 0,
                        tex.endsWith("screen_landscape.png"), v, c, null, null));
            }
            out = simplify(out);
        } catch (Exception e) {
            OfficeRooms.LOGGER.warn("OfficeCraft shader compatibility: mesh {} not readable ({})", id, e.toString());
        }
        return out;
    }

    private static float triArea(float[] v, int i) {
        int a = i * 8;
        int b = a + 8;
        int c = a + 16;
        float ux = v[b] - v[a];
        float uy = v[b + 1] - v[a + 1];
        float uz = v[b + 2] - v[a + 2];
        float wx = v[c] - v[a];
        float wy = v[c + 1] - v[a + 1];
        float wz = v[c + 2] - v[a + 2];
        float cx = uy * wz - uz * wy;
        float cy = uz * wx - ux * wz;
        float cz = ux * wy - uy * wx;
        return 0.5F * (float) Math.sqrt(cx * cx + cy * cy + cz * cz);
    }

    /**
     * 0.9.0: builds the simplified shapes. The largest triangles of a mesh carry almost all of its visible
     * surface (desk tops, panels, cushions); the many tiny ones are rounded edges and screws. Each level keeps
     * the largest triangles until a share of the total area or a triangle cap is reached. Screens and lamps
     * (emissive groups) always stay complete so displays keep working.
     */
    private static List<Group> simplify(List<Group> groups) {
        List<Float> areas = new ArrayList<>();
        double total = 0;
        for (Group g : groups) {
            if (g.emissive || g.screen) {
                continue;
            }
            for (int i = 0; i + 2 < g.rgba.length; i += 3) {
                float a = triArea(g.v, i);
                areas.add(a);
                total += a;
            }
        }
        areas.sort(java.util.Comparator.reverseOrder());
        float[] threshold = new float[LOD_AREA.length];
        for (int l = 0; l < LOD_AREA.length; l++) {
            double acc = 0;
            int n = 0;
            float th = 0;
            for (float a : areas) {
                acc += a;
                n++;
                th = a;
                if (acc >= LOD_AREA[l] * total || n >= LOD_CAP[l]) {
                    break;
                }
            }
            threshold[l] = th;
        }
        List<Group> out = new ArrayList<>(groups.size());
        for (Group g : groups) {
            float[][] lv = new float[LOD_AREA.length][];
            int[][] lc = new int[LOD_AREA.length][];
            for (int l = 0; l < LOD_AREA.length; l++) {
                if (g.emissive || g.screen) {
                    lv[l] = g.v;
                    lc[l] = g.rgba;
                    continue;
                }
                int keep = 0;
                for (int i = 0; i + 2 < g.rgba.length; i += 3) {
                    if (triArea(g.v, i) >= threshold[l]) {
                        keep++;
                    }
                }
                float[] v = new float[keep * 24];
                int[] c = new int[keep * 3];
                int k = 0;
                for (int i = 0; i + 2 < g.rgba.length; i += 3) {
                    if (triArea(g.v, i) >= threshold[l]) {
                        System.arraycopy(g.v, i * 8, v, k * 24, 24);
                        System.arraycopy(g.rgba, i, c, k * 3, 3);
                        k++;
                    }
                }
                lv[l] = v;
                lc[l] = c;
            }
            out.add(new Group(g.texture, g.emissive, g.transparent, g.screen, g.v, g.rgba, lv, lc));
        }
        return out;
    }

    /** Nach einem Ressourcen-Neuladen (F3+T) Meshes neu einlesen. */
    public static void clear() {
        MESHES.clear();
        failed = false;
    }
}
