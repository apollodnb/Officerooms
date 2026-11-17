package de.officerooms.compat;

import de.officerooms.OfficeRooms;
import de.officerooms.core.OfficeRoomsIds;
import de.officerooms.furnish.Item;
import de.officerooms.furnish.Placement;
import de.officerooms.furnish.RoomFurnisher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraftforge.fml.ModList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Setzt geplante Einrichtungsgegenstände ({@link Placement}) als echte Blöcke.
 *
 * <p>Quellen pro Gegenstand: GameFantom OfficeCraft (Büromöbel, Leuchten) → MrCrayfish's Furniture
 * Refurbished (Schubladen, Schränke, Küche, Tische) → Vanilla-Ersatz. Fremde Blöcke werden nur über
 * ihre Registry-IDs angesprochen.</p>
 *
 * <p>Zwei Zeitpunkte:</p>
 * <ul>
 *     <li><b>Generierung</b> ({@link #placeWorldgenLight}): nur das Licht. Mit OfficeCraft ein unsichtbarer
 *     Vanilla-Lichtblock (Stärke 15, keine BlockEntity) an der Stelle des späteren Panels, ohne
 *     OfficeCraft direkt Seelaterne bzw. ausgeschaltete Redstone-Lampe.</li>
 *     <li><b>In Spielernähe</b> ({@link #placeNearPlayer}): Möbel und Leuchtenmodelle. Nur wenn alle
 *     Zielblöcke noch Luft/Teppich bzw. der Lichtblock sind; Spielerbauten werden nie überschrieben,
 *     nichts entsteht doppelt.</li>
 * </ul>
 */
public final class FurnitureBuilder {

    public static final String RF = "refurbished_furniture";
    /** Blockupdate-Flags: an Clients senden, keine Nachbar-Updates, keine Form-Updates. */
    public static final int FLAGS = 2 | 16;
    private static final Set<String> WARNED = new HashSet<>();
    private static volatile Boolean rfAvailable;

    private FurnitureBuilder() {
    }

    public static boolean refurbishedAvailable() {
        Boolean b = rfAvailable;
        if (b == null) {
            b = ModList.get().isLoaded(RF);
            if (b) {
                OfficeRooms.LOGGER.info("Refurbished Furniture detected: drawers, cabinets and kitchens enabled.");
            }
            rfAvailable = b;
        }
        return b;
    }

    public static Direction dir(int facing) {
        return switch (facing & 3) {
            case 0 -> Direction.EAST;
            case 1 -> Direction.SOUTH;
            case 2 -> Direction.WEST;
            default -> Direction.NORTH;
        };
    }

    private static Block rf(String id) {
        if (!refurbishedAvailable()) {
            return null;
        }
        Block b = BuiltInRegistries.BLOCK.get(new ResourceLocation(RF, id));
        return b == Blocks.AIR ? null : b;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState with(BlockState s, String name, Comparable value) {
        Property prop = s.getBlock().getStateDefinition().getProperty(name);
        if (prop != null && prop.getPossibleValues().contains(value)) {
            return s.setValue(prop, value);
        }
        return s;
    }

    /**
     * Refurbished-Ausrichtung (im Spiel geprüft): Schubladen, Schränke, Spüle, Kühlschrank und Stühle
     * zeigen ihre Vorderseite entgegen "facing", die Mikrowelle in Richtung "facing".
     */
    private static BlockState rfState(Block b, Direction front, String id) {
        Direction facing = id.endsWith("microwave") ? front : front.getOpposite();
        return with(b.defaultBlockState(), "facing", facing);
    }

    /**
     * Refurbished-Furniture-Block für Spezialräume (Liminal-World-Update) oder null, wenn der Mod fehlt.
     * {@code front} ist die Seite, zu der die Nutzerseite zeigt.
     */
    public static BlockState refurbished(String id, Direction front) {
        Block b = rf(id);
        return b == null ? null : rfState(b, front, id);
    }

    /**
     * OfficeCraft-Ausrichtung (v0.5 erneut im Spiel geprüft): Schreibtische (Bildschirm, Tastatur,
     * Schubladen), Stühle, Cubicles, Tafel, Wasserspender, Automat, Drucker und Kaffeemaschine zeigen
     * ihre Nutzerseite in Richtung "facing".
     */
    private static Direction ocFacing(String id, Direction front) {
        return front;
    }

    // -------------------------------------------------------------------------------------------
    // Generierung: nur Licht
    // -------------------------------------------------------------------------------------------

    public static void placeWorldgenLight(WorldGenLevel level, Placement pl) {
        BlockPos pos = new BlockPos(pl.x(), pl.y(), pl.z());
        boolean strip = pl.item() == Item.LIGHT_BAR;
        Block b = (strip ? de.officerooms.block.ModBlocks.CEILING_STRIP : de.officerooms.block.ModBlocks.CEILING_PANEL).get();
        Direction.Axis axis = ((pl.facing() + 1) % 2 == 0) ? Direction.Axis.X : Direction.Axis.Z;
        BlockState base = b.defaultBlockState()
                .setValue(de.officerooms.block.CeilingLightBlock.LIT, pl.lit())
                .setValue(de.officerooms.block.CeilingLightBlock.AXIS, axis);
        if (!strip) {
            if (level.getBlockState(pos).isAir()) {
                level.setBlock(pos, base, 2);
            }
            return;
        }
        for (int[] c : RoomFurnisher.footprint(pl.item(), pl.facing())) {
            BlockPos q = pos.offset(c[0], 0, c[1]);
            if (level.getBlockState(q).isAir()) {
                level.setBlock(q, base.setValue(de.officerooms.block.CeilingLightBlock.CENTER, c[0] == 0 && c[1] == 0), 2);
            }
        }
    }

    // -------------------------------------------------------------------------------------------
    // In Spielernähe
    // -------------------------------------------------------------------------------------------

    private static boolean free(WorldGenLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        return s.isAir() || s.is(Blocks.GRAY_CARPET) || s.is(Blocks.MOSS_CARPET) || de.officerooms.block.RoomDecorBlocks.isCover(s)
                || (s.is(Blocks.WATER) && s.getFluidState().isSource());
    }

    /** Alle Zielblöcke des Gegenstands sind noch unverändert frei. */
    private static boolean vacant(WorldGenLevel level, Placement pl) {
        BlockPos pos = new BlockPos(pl.x(), pl.y(), pl.z());
        for (int[] c : RoomFurnisher.footprint(pl.item(), pl.facing())) {
            for (int h = 0; h < pl.item().height; h++) {
                if (!free(level, pos.offset(c[0], h, c[1]))) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Setzt den Gegenstand in Spielernähe. Liefert true, wenn etwas gesetzt wurde. */
    public static boolean placeNearPlayer(WorldGenLevel level, Placement pl) {
        BlockPos pos = new BlockPos(pl.x(), pl.y(), pl.z());
        if (pl.item().isLight()) {
            return false; // Leuchten sind eigene Blöcke und stehen seit der Generierung
        }
        if (pl.item().isSurface()) {
            return surface(level, pl, pos);
        }
        if (!vacant(level, pl)) {
            return false;
        }
        Direction front = dir(pl.facing());
        Item it = pl.item();
        if (it == Item.MONITOR) {
            set(level, pos, de.officerooms.block.ModBlocks.monitor(front, pl.variant() == 1));
            return true;
        }
        int n = switch (it) {
            case DESK, WORKSTATION, PARTITION, CHAIR, CONFERENCE_LARGE, CONFERENCE_MEDIUM, BOARD, WATER_COOLER,
                    VENDING, PRINTER, COFFEE -> officeItem(level, pos, pl, front);
            default -> refurbishedItem(level, pos, pl, front);
        };
        if (n > 0 && it != Item.COFFEE && it != Item.MICROWAVE) {
            // Unter Möbeln liegt statt Teppich grauer Beton (von unten als graues Deckenelement sichtbar).
            for (int[] c : RoomFurnisher.footprint(it, pl.facing())) {
                BlockPos below = pos.offset(c[0], -1, c[1]);
                if (level.getBlockState(below).is(Blocks.SMOOTH_QUARTZ)) {
                    level.setBlock(below, Blocks.GRAY_CONCRETE.defaultBlockState(), FLAGS);
                }
            }
        }
        if (it.container && pl.loot() != null) {
            applyLoot(level, pos, pl);
            if (it.height == 2) {
                applyLoot(level, pos.above(), pl);
            }
        }
        return n > 0;
    }

    /** 0.23.0: floor layers replace only the generated carpet, wall tiles only the generated quartz wall. */
    private static boolean surface(WorldGenLevel level, Placement pl, BlockPos pos) {
        if (pl.item() == Item.FLOOR_COVER) {
            BlockState here = level.getBlockState(pos);
            boolean carpet = here.is(Blocks.GRAY_CARPET);
            if (pl.variant() == 0) {
                // tiles replace the grey wool floor (the ceiling of the storey below stays untouched)
                if (level.getBlockState(pos.below()).is(Blocks.GRAY_WOOL) && (carpet || here.isAir())) {
                    set(level, pos.below(), de.officerooms.block.RoomDecorBlocks.FLOOR_TILE.get().defaultBlockState());
                    if (carpet) {
                        set(level, pos, Blocks.AIR.defaultBlockState());
                    }
                    return true;
                }
                return false;
            }
            if (!carpet && !(here.isAir() && level.getBlockState(pos.below()).is(Blocks.GRAY_WOOL))) {
                return false;
            }
            set(level, pos, (pl.variant() == 1 ? de.officerooms.block.RoomDecorBlocks.RUG : de.officerooms.block.RoomDecorBlocks.DOORMAT).get().defaultBlockState());
            return true;
        }
        boolean any = false;
        BlockState tile = de.officerooms.block.RoomDecorBlocks.decor(de.officerooms.furnish.Decor.WALL_TILE, dir(pl.facing()));
        for (int h = 0; h < pl.variant(); h++) {
            if (level.getBlockState(pos.above(h)).is(Blocks.QUARTZ_BLOCK)) {
                set(level, pos.above(h), tile);
                any = true;
            }
        }
        return any;
    }

    private static final String[] HANDOUTS = {
            "{\"text\":\"ORIENTATION\\nSession 14\\n\\nWelcome to your first day.\\n\\nPlease take a seat. All seats face the screen. If a seat does not face the screen, do not sit in it.\"}",
            "{\"text\":\"POWER INTERRUPTIONS\\n\\nThe monitors will warn you. Believe them.\\n\\nWhen the lights go out, find the fuse box on your floor and repair it. Repairs are rewarded.\\n\\nDo not wait in the dark.\"}",
            "{\"text\":\"BUILDING LAYOUT\\n\\nThere is no top floor.\\n\\nWhat is above the top floor is not part of the building. Staff who leave through the ceiling are not considered absent. They are considered elsewhere.\"}",
            "{\"text\":\"QUESTIONS\\n\\nQuestions may be asked at the end of the session.\\n\\nThe session does not end.\\n\\nThank you for your attention.\\n\\n- Facilities\"}"};

    private static net.minecraft.world.item.ItemStack handout() {
        net.minecraft.world.item.ItemStack book = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WRITTEN_BOOK);
        net.minecraft.nbt.CompoundTag tag = book.getOrCreateTag();
        tag.putString("title", "Orientation Handout");
        tag.putString("author", "Facilities");
        net.minecraft.nbt.ListTag pages = new net.minecraft.nbt.ListTag();
        for (String page : HANDOUTS) {
            pages.add(net.minecraft.nbt.StringTag.valueOf(page));
        }
        tag.put("pages", pages);
        return book;
    }

    private static BlockState rfOr(String id, Direction front, BlockState fallback) {
        Block b = rf(id);
        return b != null ? rfState(b, front, id) : fallback;
    }

    private static void applyLoot(WorldGenLevel level, BlockPos pos, Placement pl) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof RandomizableContainerBlockEntity rc) {
            long seed = net.minecraft.util.Mth.getSeed(pos.getX(), pos.getY(), pos.getZ());
            rc.setLootTable(new ResourceLocation(OfficeRoomsIds.MOD_ID, "chests/" + pl.loot()), seed);
        }
    }

    private static String officeId(Placement pl) {
        return switch (pl.item()) {
            case DESK -> "office_desk";
            case WORKSTATION -> "cubicle_workstation";
            case PARTITION -> "cubicle_partitions";
            case CHAIR -> (pl.variant() & 1) == 0 ? "chair_gray" : "chair_black";
            case CONFERENCE_LARGE -> "conference_large";
            case CONFERENCE_MEDIUM -> "conference_medium";
            case BOARD -> "presentation_board";
            case WATER_COOLER -> "water_cooler";
            case VENDING -> "vending_machine";
            case PRINTER -> "printer";
            case COFFEE -> "coffee_machine";
            default -> null;
        };
    }

    private static int officeItem(WorldGenLevel level, BlockPos pos, Placement pl, Direction front) {
        String id = officeId(pl);
        if (id != null && OfficeCraftCompat.isAvailable()) {
            Direction facing = ocFacing(id, front);
            if (fitsReserved(id, pl, facing) && OfficeCraftCompat.place(level, pos, id, facing, false, FLAGS)) {
                return 1;
            }
        }
        return vanillaOffice(level, pos, pl, front);
    }

    /** Sicherheitsprüfung: die tatsächliche Belegung des Mods liegt innerhalb der geplanten Belegung. */
    private static boolean fitsReserved(String id, Placement pl, Direction facing) {
        List<BlockPos> cells = OfficeCraftCompat.cells(id, facing);
        Set<Long> planned = new HashSet<>();
        for (int[] c : RoomFurnisher.footprint(pl.item(), pl.facing())) {
            planned.add(((long) c[0] << 32) ^ (c[1] & 0xFFFFFFFFL));
        }
        for (BlockPos c : cells) {
            if (c.getY() < 0 || c.getY() >= pl.item().height || !planned.contains(((long) c.getX() << 32) ^ (c.getZ() & 0xFFFFFFFFL))) {
                if (WARNED.add(id + facing)) {
                    OfficeRooms.LOGGER.warn("OfficeCraft furniture {} ({}) occupies more space than planned – using the vanilla replacement.", id, facing);
                }
                return false;
            }
        }
        return !cells.isEmpty();
    }

    private static void set(WorldGenLevel level, BlockPos pos, BlockState s) {
        level.setBlock(pos, s, FLAGS);
    }

    private static int vanillaOffice(WorldGenLevel level, BlockPos pos, Placement pl, Direction front) {
        BlockState top = Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
        switch (pl.item()) {
            case DESK, CONFERENCE_LARGE, CONFERENCE_MEDIUM -> {
                for (int[] c : RoomFurnisher.footprint(pl.item(), pl.facing())) {
                    set(level, pos.offset(c[0], 0, c[1]), top);
                }
                return 1;
            }
            case WORKSTATION -> {
                for (int[] c : RoomFurnisher.footprint(Item.DESK, pl.facing())) {
                    set(level, pos.offset(c[0], 0, c[1]), top);
                }
                return 1;
            }
            case CHAIR -> {
                set(level, pos, Blocks.SMOOTH_QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING, front.getOpposite()));
                return 1;
            }
            case WATER_COOLER -> {
                set(level, pos, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
                return 1;
            }
            case PRINTER -> {
                set(level, pos, with(Blocks.DISPENSER.defaultBlockState(), "facing", front));
                return 1;
            }
            default -> {
                return 0;
            }
        }
    }

    private static int refurbishedItem(WorldGenLevel level, BlockPos pos, Placement pl, Direction front) {
        boolean white = (pl.variant() & 1) == 0;
        switch (pl.item()) {
            case DRAWER -> {
                String id = white ? "white_kitchen_drawer" : "light_gray_kitchen_drawer";
                Block b = rf(id);
                set(level, pos, b != null ? rfState(b, front, id) : with(Blocks.DROPPER.defaultBlockState(), "facing", front));
                return 1;
            }
            case DRAWER_STACK -> {
                String dId = white ? "white_kitchen_drawer" : "light_gray_kitchen_drawer";
                String cId = white ? "white_kitchen_storage_cabinet" : "light_gray_kitchen_storage_cabinet";
                Block d = rf(dId);
                Block c = rf(cId);
                if (d != null && c != null) {
                    set(level, pos, rfState(d, front, dId));
                    set(level, pos.above(), rfState(c, front, cId));
                } else {
                    set(level, pos, with(Blocks.DROPPER.defaultBlockState(), "facing", front));
                    set(level, pos.above(), with(Blocks.DROPPER.defaultBlockState(), "facing", front));
                }
                return 2;
            }
            case COUNTER -> {
                Block b = rf("white_kitchen_cabinetry");
                set(level, pos, b != null ? rfState(b, front, "white_kitchen_cabinetry") : Blocks.SMOOTH_STONE.defaultBlockState());
                return 1;
            }
            case SINK -> {
                Block b = rf("white_kitchen_sink");
                set(level, pos, b != null ? rfState(b, front, "white_kitchen_sink") : Blocks.CAULDRON.defaultBlockState());
                return 1;
            }
            case FRIDGE -> {
                Block f = rf("light_fridge");
                Block z = rf("light_freezer");
                if (f != null && z != null) {
                    set(level, pos, rfState(f, front, "light_fridge"));
                    set(level, pos.above(), rfState(z, front, "light_freezer"));
                } else {
                    set(level, pos, Blocks.IRON_BLOCK.defaultBlockState());
                    set(level, pos.above(), Blocks.IRON_BLOCK.defaultBlockState());
                }
                return 2;
            }
            case MICROWAVE -> {
                Block b = rf("light_microwave");
                set(level, pos, b != null ? rfState(b, front, "light_microwave") : with(Blocks.SMOKER.defaultBlockState(), "facing", front));
                return 1;
            }
            case TABLE -> {
                Block b = rf("birch_table");
                set(level, pos, b != null ? b.defaultBlockState()
                        : Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
                return 1;
            }
            case SIDE_CHAIR -> {
                String cid = pl.variant() == 1 ? "dark_oak_chair" : "birch_chair";
                Block b = rf(cid);
                set(level, pos, b != null ? rfState(b, front, cid)
                        : Blocks.STONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, front.getOpposite()));
                return 1;
            }
            case TOILET -> {
                set(level, pos, rfOr("white_toilet", front, Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING, front.getOpposite())));
                return 1;
            }
            case BASIN -> {
                set(level, pos, rfOr("white_basin", front, Blocks.CAULDRON.defaultBlockState()));
                return 1;
            }
            case EXEC_DESK -> {
                set(level, pos, rfOr("dark_oak_desk", front, Blocks.DARK_OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP)));
                return 1;
            }
            case SOFA -> {
                String id = new String[]{"black_sofa", "brown_sofa", "gray_sofa", "light_gray_sofa"}[pl.variant() & 3];
                set(level, pos, rfOr(id, front, Blocks.BROWN_WOOL.defaultBlockState()));
                return 1;
            }
            case CABINET -> {
                set(level, pos, rfOr("dark_oak_storage_cabinet", front, with(Blocks.BARREL.defaultBlockState(), "facing", front)));
                return 1;
            }
            case BOOKSHELF -> {
                set(level, pos, Blocks.BOOKSHELF.defaultBlockState());
                set(level, pos.above(), pl.variant() == 0 ? Blocks.CHISELED_BOOKSHELF.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, front)
                        : Blocks.BOOKSHELF.defaultBlockState());
                return 2;
            }
            case LECTERN -> {
                set(level, pos, Blocks.LECTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LecternBlock.FACING, front)
                        .setValue(net.minecraft.world.level.block.LecternBlock.HAS_BOOK, true));
                if (level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.LecternBlockEntity lb) {
                    lb.setBook(handout());
                }
                return 1;
            }
            case DECOR -> {
                set(level, pos, de.officerooms.block.RoomDecorBlocks.decor(pl.variant(), front));
                return 1;
            }
            case RACK -> {
                BlockState o = Blocks.OBSERVER.defaultBlockState().setValue(BlockStateProperties.FACING, front);
                set(level, pos, o);
                set(level, pos.above(), o);
                return 2;
            }
            default -> {
                return 0;
            }
        }
    }
}
