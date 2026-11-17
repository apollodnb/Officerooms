package de.officerooms.hazard;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

/** 0.22.0: persistent blackout state (stored in the Overworld's data). */
public final class BlackoutData extends SavedData {

    public long nextCycle;
    public int phase;
    public long phaseEnd;
    public int[] box = new int[6];
    public String name = "";
    public long fuse;
    public int fuseFloor;
    /** 0.25.0: ceiling lights switched off by the blackout (restored when the power returns). */
    public final it.unimi.dsi.fastutil.longs.LongOpenHashSet off = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
    /** Lights that should come back on but whose chunk was not loaded at the end of the blackout. */
    public final it.unimi.dsi.fastutil.longs.LongOpenHashSet restore = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
    /** Chunks of the box already switched off during this blackout. */
    public final it.unimi.dsi.fastutil.longs.LongOpenHashSet doneChunks = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();

    public static BlackoutData load(CompoundTag t) {
        BlackoutData d = new BlackoutData();
        d.nextCycle = t.getLong("NextCycle");
        d.phase = t.getInt("Phase");
        d.phaseEnd = t.getLong("PhaseEnd");
        int[] b = t.getIntArray("Box");
        d.box = b.length == 6 ? b : new int[6];
        d.name = t.getString("Name");
        d.fuse = t.getLong("Fuse");
        d.fuseFloor = t.getInt("FuseFloor");
        for (long v : t.getLongArray("Off")) {
            d.off.add(v);
        }
        for (long v : t.getLongArray("Restore")) {
            d.restore.add(v);
        }
        for (long v : t.getLongArray("DoneChunks")) {
            d.doneChunks.add(v);
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag t) {
        t.putLong("NextCycle", nextCycle);
        t.putInt("Phase", phase);
        t.putLong("PhaseEnd", phaseEnd);
        t.putIntArray("Box", box);
        t.putString("Name", name);
        t.putLong("Fuse", fuse);
        t.putInt("FuseFloor", fuseFloor);
        t.putLongArray("Off", off.toLongArray());
        t.putLongArray("Restore", restore.toLongArray());
        t.putLongArray("DoneChunks", doneChunks.toLongArray());
        return t;
    }
}
