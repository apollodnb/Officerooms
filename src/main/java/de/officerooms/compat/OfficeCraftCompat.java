package de.officerooms.compat;

import de.officerooms.OfficeRooms;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optionale Anbindung an <b>GameFantom OfficeCraft</b> ({@code gamefantom_officecraft}).
 *
 * <p>Es wird nichts aus dem Mod kopiert oder eingebettet. Office Rooms verwendet ausschließlich die
 * registrierten Blöcke über ihre Registry-IDs und die öffentlichen Methoden des Mods, zur Laufzeit
 * per Reflection (keine Compile-Abhängigkeit):</p>
 * <ul>
 *     <li>{@code Catalog.get(id).cells(Direction, boolean)}: welche Nachbarzellen ein Möbel belegt,</li>
 *     <li>{@code PartEntity.configure(master, id, facing, gridAligned)}: Teilblöcke mit dem Hauptblock verbinden.</li>
 * </ul>
 * <p>Damit wird ein Möbel in der Weltgenerierung genauso aufgebaut wie beim Setzen durch einen Spieler,
 * nur ohne Block-Updates.</p>
 */
public final class OfficeCraftCompat {

    public static final String MOD_ID = "gamefantom_officecraft";

    private static volatile Boolean available;
    private static Method catalogGet;
    private static Method entryCells;
    private static Method partConfigure;
    private static Method entryShape;
    private static java.lang.reflect.Field entries;
    private static final Map<String, List<BlockPos>> CELL_CACHE = new ConcurrentHashMap<>();

    private OfficeCraftCompat() {
    }

    public static boolean isAvailable() {
        Boolean a = available;
        if (a == null) {
            a = init();
            available = a;
        }
        return a;
    }

    private static synchronized boolean init() {
        if (!ModList.get().isLoaded(MOD_ID)) {
            return false;
        }
        try {
            Class<?> catalog = Class.forName("com.gamefantom.officecraft.Catalog");
            Class<?> entry = Class.forName("com.gamefantom.officecraft.Catalog$Entry");
            Class<?> part = Class.forName("com.gamefantom.officecraft.PartEntity");
            catalogGet = catalog.getMethod("get", String.class);
            entryCells = entry.getMethod("cells", Direction.class, boolean.class);
            partConfigure = part.getMethod("configure", BlockPos.class, String.class, Direction.class, boolean.class);
            entryShape = entry.getMethod("shape", Direction.class, boolean.class);
            entries = catalog.getField("ENTRIES");
            OfficeRooms.LOGGER.info("OfficeCraft detected: furniture and ceiling lights enabled.");
            return true;
        } catch (ReflectiveOperationException | LinkageError e) {
            OfficeRooms.LOGGER.warn("OfficeCraft loaded, but its interface was not found – using vanilla replacements ({})", e.toString());
            return false;
        }
    }

    /**
     * Umgeht einen Thread-Sicherheitsfehler in OfficeCraft 1.0.3: {@code Catalog$Entry.shape/cells}
     * befüllen eine normale HashMap erst bei Bedarf. Baut Minecraft Chunk-Geometrie auf mehreren
     * Threads gleichzeitig, kann das mit einer {@code ConcurrentModificationException} abstürzen
     * (im Test reproduziert). Hier werden alle Einträge einmal vollständig auf einem Thread
     * vorberechnet; danach wird nur noch gelesen. Muss auf dem Haupt-Thread laufen (enqueueWork).
     */
    public static void warmUpCaches() {
        if (!isAvailable()) {
            return;
        }
        int n = 0;
        try {
            for (Object entry : (List<?>) entries.get(null)) {
                for (Direction d : Direction.values()) {
                    for (boolean grid : new boolean[]{false, true}) {
                        try {
                            entryShape.invoke(entry, d, grid);
                            entryCells.invoke(entry, d, grid);
                            n++;
                        } catch (ReflectiveOperationException | RuntimeException ignored) {
                            // nicht jede Kombination ist für jedes Möbel gültig
                        }
                    }
                }
            }
            OfficeRooms.LOGGER.info("OfficeCraft: {} shape caches precomputed (thread safety).", n);
        } catch (ReflectiveOperationException | RuntimeException e) {
            OfficeRooms.LOGGER.warn("OfficeCraft: precomputing shapes failed ({})", e.toString());
        }
    }

    public static Block block(String id) {
        return BuiltInRegistries.BLOCK.get(new ResourceLocation(MOD_ID, id));
    }

    /** Belegte Zellen relativ zum Hauptblock (inklusive 0,0,0) für Ausrichtung facing. */
    public static List<BlockPos> cells(String id, Direction facing) {
        if (!isAvailable()) {
            return List.of();
        }
        return CELL_CACHE.computeIfAbsent(id + "/" + facing.get2DDataValue(), k -> {
            try {
                Object entry = catalogGet.invoke(null, id);
                if (entry == null) {
                    return List.of();
                }
                Map<?, ?> m = (Map<?, ?>) entryCells.invoke(entry, facing, false);
                List<BlockPos> out = new ArrayList<>();
                for (Object o : m.keySet()) {
                    out.add(((BlockPos) o).immutable());
                }
                if (!out.contains(BlockPos.ZERO)) {
                    out.add(BlockPos.ZERO);
                }
                return Collections.unmodifiableList(out);
            } catch (ReflectiveOperationException | RuntimeException e) {
                OfficeRooms.LOGGER.warn("OfficeCraft: footprint of {} not readable ({})", id, e.toString());
                return List.of();
            }
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState with(BlockState s, String name, Comparable value) {
        Property p = s.getBlock().getStateDefinition().getProperty(name);
        if (p != null && p.getPossibleValues().contains(value)) {
            return s.setValue(p, value);
        }
        return s;
    }

    /**
     * Setzt ein OfficeCraft-Möbel mit allen Teilblöcken. Der Aufrufer garantiert, dass alle
     * Zellen frei sind und im aktuellen Chunk liegen. Liefert false, wenn nichts gesetzt wurde.
     */
    public static boolean place(WorldGenLevel level, BlockPos master, String id, Direction facing, boolean lit) {
        return place(level, master, id, facing, lit, 2);
    }

    public static boolean place(WorldGenLevel level, BlockPos master, String id, Direction facing, boolean lit, int flags) {
        if (!isAvailable()) {
            return false;
        }
        Block b = block(id);
        if (b == null || b == Blocks.AIR) {
            return false;
        }
        List<BlockPos> cells = cells(id, facing);
        if (cells.isEmpty()) {
            return false;
        }
        BlockState state = b.defaultBlockState();
        state = with(state, "facing", facing);
        state = with(state, "grid_aligned", false);
        state = with(state, "lit", lit);
        level.setBlock(master, state, flags);
        Block partBlock = block("furniture_part");
        for (BlockPos rel : cells) {
            if (rel.equals(BlockPos.ZERO)) {
                continue;
            }
            BlockPos p = master.offset(rel);
            level.setBlock(p, partBlock.defaultBlockState(), flags);
            BlockEntity be = level.getBlockEntity(p);
            if (be != null) {
                try {
                    partConfigure.invoke(be, master, id, facing, false);
                } catch (ReflectiveOperationException e) {
                    OfficeRooms.LOGGER.warn("OfficeCraft: part block not configurable ({})", e.toString());
                }
            }
        }
        return true;
    }
}
