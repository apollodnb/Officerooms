package de.officerooms.hazard;

/** 0.22.0: the client's copy of the blackout (from {@link HazardNet.Blackout}); also the smoothed light dimming. */
public final class BlackoutState {

    public static final BlackoutState CLIENT = new BlackoutState();
    public volatile int phase;
    public volatile int secondsLeft;
    public volatile long receivedAt;
    public volatile int[] box = new int[6];
    public volatile String name = "";
    public volatile int fuseFloor;
    /** 1 = normal light, down to ~0.1 in a blackout (set by the client tick). */
    public static volatile float dim = 1F;

    public void set(int phase, int secondsLeft, int[] box, String name, int fuseFloor) {
        this.phase = phase;
        this.secondsLeft = secondsLeft;
        this.receivedAt = System.currentTimeMillis();
        this.box = box.clone();
        this.name = name;
        this.fuseFloor = fuseFloor;
    }

    public int remaining() {
        return (int) Math.max(0, secondsLeft - (System.currentTimeMillis() - receivedAt) / 1000L);
    }

    public boolean inside(double x, double y, double z) {
        int[] b = box;
        return phase == HazardState.ACTIVE && x >= b[0] && x <= b[3] + 1 && y >= b[1] && y <= b[4] + 1 && z >= b[2] && z <= b[5] + 1;
    }
}
