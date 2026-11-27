package de.officerooms.worldgen;

import de.officerooms.OfficeRooms;
import de.officerooms.room.GenSettings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * 0.43.0: the content version a world was created with. The dimension JSON is read from the mod's data pack every time
 * a world loads, so its {@code content_version} alone would switch new megastructures on in old worlds too (and cut them
 * off at already explored chunks). The version is therefore stored once per world: new worlds get {@link #CURRENT},
 * worlds that existed before 0.43.0 keep content version 2 (departure gates, no mall).
 */
public final class ContentVersion {

    public static final int CURRENT = 3;
    static final int LEGACY = 2;
    private static volatile int cap = Integer.MAX_VALUE;

    private ContentVersion() {
    }

    public static int cap() {
        return cap;
    }

    static GenSettings apply(GenSettings s) {
        int c = cap;
        if (s.contentVersion() <= c) {
            return s;
        }
        return new GenSettings(s.grid(), s.roomHeights(), s.roomSizes(), s.compositeChance(), s.corridors(), s.stairsPerStorey(), s.atria(), s.doors(),
                s.windows(), s.furnishing(), s.special(), c);
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ContentVersion::onLoad);
    }

    private static void onLoad(LevelEvent.Load e) {
        if (!(e.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
            return;
        }
        boolean fresh = level.getGameTime() == 0;
        Data d = level.getDataStorage().computeIfAbsent(Data::load, () -> new Data(fresh ? CURRENT : LEGACY), "office_rooms_content");
        d.setDirty();
        cap = d.version;
        OfficeRooms.LOGGER.info("Office Rooms content version of this world: {}{}", d.version, fresh ? " (new world)" : "");
    }

    static final class Data extends SavedData {
        final int version;

        Data(int version) {
            this.version = version;
        }

        static Data load(CompoundTag t) {
            return new Data(t.contains("version") ? t.getInt("version") : LEGACY);
        }

        @Override
        public CompoundTag save(CompoundTag t) {
            t.putInt("version", version);
            return t;
        }
    }
}
