package de.officerooms.worldgen;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Merkt sich dauerhaft (im Weltordner, {@code data/office_rooms_furnished.dat}), welche
 * Einheiten aus Chunk und Geschoss bereits eingerichtet wurden. Dadurch wird nichts doppelt gesetzt,
 * auch nicht nach Neustart oder erneutem Laden eines Chunks.
 */
public final class FurnishedData extends SavedData {

    private static final String NAME = "office_rooms_furnished";
    private final LongOpenHashSet done = new LongOpenHashSet();

    public static FurnishedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FurnishedData::load, FurnishedData::new, NAME);
    }

    public static long key(int cx, int cz, int storey) {
        return ((long) (cx & 0x3FFFFF) << 42) | ((long) (cz & 0x3FFFFF) << 20) | (storey & 0xFFFFF);
    }

    public boolean isDone(long key) {
        return done.contains(key);
    }

    public void markDone(long key) {
        if (done.add(key)) {
            setDirty();
        }
    }

    public int size() {
        return done.size();
    }

    private static FurnishedData load(CompoundTag tag) {
        FurnishedData d = new FurnishedData();
        for (long k : tag.getLongArray("units")) {
            d.done.add(k);
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLongArray("units", done.toLongArray());
        return tag;
    }
}
