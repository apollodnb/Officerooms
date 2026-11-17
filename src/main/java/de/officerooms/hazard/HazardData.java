package de.officerooms.hazard;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

/** 0.17.0: persistent hazard event state (stored with the overworld data, shared by all players). */
public final class HazardData extends SavedData {

    public long nextCycle;
    public int phase;
    public long phaseEnd;
    public int[] box = new int[6];
    public String name = "";
    /** Chests still to be placed: x, z, core (1/0). */
    public final List<int[]> pending = new ArrayList<>();
    public final List<Long> placed = new ArrayList<>();
    public final List<Long> toRemove = new ArrayList<>();

    public static HazardData load(CompoundTag t) {
        HazardData d = new HazardData();
        d.nextCycle = t.getLong("NextCycle");
        d.phase = t.getInt("Phase");
        d.phaseEnd = t.getLong("PhaseEnd");
        int[] b = t.getIntArray("Box");
        if (b.length == 6) {
            d.box = b;
        }
        d.name = t.getString("Name");
        for (Tag e : t.getList("Pending", Tag.TAG_INT_ARRAY)) {
            d.pending.add(((net.minecraft.nbt.IntArrayTag) e).getAsIntArray());
        }
        for (long l : t.getLongArray("Placed")) {
            d.placed.add(l);
        }
        for (long l : t.getLongArray("ToRemove")) {
            d.toRemove.add(l);
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
        ListTag pl = new ListTag();
        for (int[] p : pending) {
            pl.add(new net.minecraft.nbt.IntArrayTag(p));
        }
        t.put("Pending", pl);
        t.put("Placed", new LongArrayTag(placed));
        t.put("ToRemove", new LongArrayTag(toRemove));
        return t;
    }
}
