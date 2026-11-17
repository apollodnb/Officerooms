package de.officerooms.underside;

/** 0.19.0: quake state on the client (set by {@link de.officerooms.hazard.HazardNet.Quake}, read by the camera). */
public final class UndersideClientState {

    public static int left;
    public static int total = 1;
    public static float strength;

    private UndersideClientState() {
    }

    public static void quake(int ticks, float s) {
        if (left <= 0 || s >= strength * left / total) {
            left = ticks;
            total = Math.max(1, ticks);
            strength = s;
        }
    }
}
