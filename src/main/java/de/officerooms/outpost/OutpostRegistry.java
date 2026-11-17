package de.officerooms.outpost;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** 0.25.0: every placed research outpost (per dimension), so /officerooms goto outpost can find them. */
public final class OutpostRegistry extends SavedData {

    public record Entry(String theme, BlockPos pos) {
    }

    public final List<Entry> entries = new ArrayList<>();

    public static OutpostRegistry of(ServerLevel l) {
        return l.getDataStorage().computeIfAbsent(OutpostRegistry::load, OutpostRegistry::new, "office_rooms_outposts");
    }

    public static void add(ServerLevel l, String theme, BlockPos pos) {
        OutpostRegistry r = of(l);
        r.entries.add(new Entry(theme, pos.immutable()));
        r.setDirty();
    }

    static OutpostRegistry load(CompoundTag t) {
        OutpostRegistry r = new OutpostRegistry();
        for (var e : t.getList("Outposts", 10)) {
            CompoundTag c = (CompoundTag) e;
            r.entries.add(new Entry(c.getString("Theme"), BlockPos.of(c.getLong("Pos"))));
        }
        return r;
    }

    @Override
    public CompoundTag save(CompoundTag t) {
        ListTag list = new ListTag();
        for (Entry e : entries) {
            CompoundTag c = new CompoundTag();
            c.putString("Theme", e.theme());
            c.putLong("Pos", e.pos().asLong());
            list.add(c);
        }
        t.put("Outposts", list);
        return t;
    }
}
