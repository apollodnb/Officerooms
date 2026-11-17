package de.officerooms.hazard;

/**
 * 0.17.0: the hazard event as seen by a client (also used on the server as a plain value object). Filled by
 * {@link HazardNet} sync packets; read by the monitors, the fog/particle effects and the detector.
 */
public final class HazardState {

    public static final int IDLE = 0;
    public static final int WARNING = 1;
    public static final int ACTIVE = 2;

    /** The client's copy. */
    public static final HazardState CLIENT = new HazardState();

    public volatile int phase;
    public volatile int secondsLeft;
    public volatile long receivedAt;
    public volatile int minX;
    public volatile int minY;
    public volatile int minZ;
    public volatile int maxX;
    public volatile int maxY;
    public volatile int maxZ;
    public volatile String name = "";

    public void set(int phase, int secondsLeft, int[] box, String name) {
        this.phase = phase;
        this.secondsLeft = secondsLeft;
        this.receivedAt = System.currentTimeMillis();
        this.minX = box[0];
        this.minY = box[1];
        this.minZ = box[2];
        this.maxX = box[3];
        this.maxY = box[4];
        this.maxZ = box[5];
        this.name = name;
    }

    /** Seconds until the next phase change, counted down locally between syncs. */
    public int remaining() {
        long gone = (System.currentTimeMillis() - receivedAt) / 1000L;
        return (int) Math.max(0, secondsLeft - gone);
    }

    public boolean active() {
        return phase == ACTIVE;
    }

    public boolean contains(double x, double y, double z) {
        return phase == ACTIVE && x >= minX && x < maxX + 1 && y >= minY && y < maxY + 1 && z >= minZ && z < maxZ + 1;
    }

    public static String clock(int s) {
        return String.format("%02d:%02d", s / 60, s % 60);
    }
}
