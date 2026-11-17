package de.officerooms.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

/**
 * Darstellung der Office-Rooms-Dimension auf dem Client.
 *
 * <ul>
 *     <li>Kein Himmel ({@link SkyType#NONE}), keine Wolken, keine Sterne.</li>
 *     <li>Hintergrund- und Nebelfarbe ist ein sehr dunkles, neutrales Grau (Phase 2). Alles,
 *     was außerhalb der Sichtweite liegt, verschwindet in derselben Dunkelheit wie unbeleuchtete
 *     Gebäudeteile, ohne Sterne oder End-Himmel.</li>
 *     <li>Normale gerichtete Flächenschattierung (Boden hell, Decke dunkler), damit Wände, Böden
 *     und Decken im durchgehend weißen Quarz klar unterscheidbar bleiben.</li>
 * </ul>
 */
public final class OfficeRoomsSkyEffects extends DimensionSpecialEffects {

    /**
     * Dunstfarbe (RGB 0..1). Seit Phase 2 (dunkle Liminal-Atmosphäre) ein fast schwarzes, leicht
     * kühles Grau: Ferne Bereiche verschwinden in Dunkelheit wie unbeleuchtete Gebäudeteile.
     */
    public static final Vec3 HAZE = new Vec3(0.043, 0.043, 0.051);

    public OfficeRoomsSkyEffects() {
        super(Float.NaN, false, SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return HAZE;
    }

    /**
     * 0.16.0: a much steeper light curve. Full lamp light stays as it is, but everything further away from a lamp
     * falls off quickly into darkness (block light 8 ends up at about 60 %, block light 3 at about 40 %).
     * 0.26.0: everything another 30 % darker.
     */
    @Override
    public void adjustLightmapColors(net.minecraft.client.multiplayer.ClientLevel level, float partialTicks, float skyDarken,
                                     float blockLightRedFlicker, float skyLight, int pixelX, int pixelY, org.joml.Vector3f colors) {
        float b = Math.max(pixelX, pixelY) / 15.0F;
        float dim = de.officerooms.hazard.BlackoutState.dim;           // 0.22.0: blackout (1 = normal, 0.1 = power out)
        colors.mul((0.35F + 0.65F * (float) Math.pow(b, 1.6)) * dim * 0.8F);   // 0.26.0: the whole dimension 30 % darker; 0.41.1: 20 % (user: a bit brighter)
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }
}
