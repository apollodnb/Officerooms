package de.officerooms.block;

import de.officerooms.core.OfficeRoomsIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Eigene Blöcke (Phase 4): Deckenleuchten. */
public final class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, OfficeRoomsIds.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, OfficeRoomsIds.MOD_ID);

    private static Block.Properties lightProps() {
        return Block.Properties.of().mapColor(MapColor.SNOW).strength(0.3F).sound(SoundType.GLASS).noOcclusion()
                .noCollission().pushReaction(PushReaction.DESTROY).isViewBlocking((s, l, p) -> false)
                .lightLevel(CeilingLightBlock::lightLevel);
    }

    public static final DeferredHolder<Block, Block> CEILING_PANEL = BLOCKS.register("ceiling_panel",
            () -> new CeilingLightBlock(lightProps(), false));
    /** 0.32.0: a ceiling tile with a dark hole in it, a sign that something nests up there. */
    public static final DeferredHolder<Block, Block> CEILING_HOLE = BLOCKS.register("ceiling_hole",
            () -> new Block(Block.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.QUARTZ).strength(1.0F, 3.0F)
                    .sound(net.minecraft.world.level.block.SoundType.STONE)));
    public static final DeferredHolder<Item, Item> CEILING_HOLE_ITEM = ITEMS.register("ceiling_hole", () -> new BlockItem(CEILING_HOLE.get(), new Item.Properties()));
    public static final DeferredHolder<Block, Block> CEILING_STRIP = BLOCKS.register("ceiling_strip",
            () -> new CeilingLightBlock(lightProps(), true));

    /** Portal von/zu den Office Rooms (Liminal-World-Update). Leuchtet schwach, keine Kollision, kein Drop. */
    public static final DeferredHolder<Block, Block> OFFICE_PORTAL = BLOCKS.register("office_portal",
            () -> new OfficePortalBlock(Block.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(5.0F, 1200.0F)
                    .sound(SoundType.SCULK).noCollission().noOcclusion().lightLevel(s -> 11).noLootTable()
                    .pushReaction(PushReaction.BLOCK)));

    /** Poolrooms (0.6.0): cremefarbene Fliesen für Wände, Böden, Decken; dazu Stufen und Platten. */
    private static Block.Properties tileProps() {
        return Block.Properties.of().mapColor(MapColor.SAND).strength(1.5F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops();
    }

    public static final DeferredHolder<Block, Block> POOL_TILE = BLOCKS.register("pool_tile", () -> new Block(tileProps()));
    public static final DeferredHolder<Block, Block> POOL_FLOOR_TILE = BLOCKS.register("pool_floor_tile", () -> new Block(tileProps()));
    public static final DeferredHolder<Block, Block> POOL_TILE_STAIRS = BLOCKS.register("pool_tile_stairs",
            () -> new net.minecraft.world.level.block.StairBlock(() -> POOL_TILE.get().defaultBlockState(), tileProps()));
    public static final DeferredHolder<Block, Block> POOL_TILE_SLAB = BLOCKS.register("pool_tile_slab",
            () -> new net.minecraft.world.level.block.SlabBlock(tileProps()));

    public static final DeferredHolder<Item, Item> POOL_TILE_ITEM = ITEMS.register("pool_tile", () -> new BlockItem(POOL_TILE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> POOL_FLOOR_TILE_ITEM = ITEMS.register("pool_floor_tile", () -> new BlockItem(POOL_FLOOR_TILE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> POOL_TILE_STAIRS_ITEM = ITEMS.register("pool_tile_stairs", () -> new BlockItem(POOL_TILE_STAIRS.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> POOL_TILE_SLAB_ITEM = ITEMS.register("pool_tile_slab", () -> new BlockItem(POOL_TILE_SLAB.get(), new Item.Properties()));

    // --- 0.8.0: The Maze (Level 0) ------------------------------------------------------------

    private static Block.Properties mazeProps(MapColor c) {
        return Block.Properties.of().mapColor(c).strength(1.2F, 4.0F).sound(SoundType.WOOL);
    }

    public static final DeferredHolder<Block, Block> MAZE_WALLPAPER = BLOCKS.register("maze_wallpaper",
            () -> new Block(Block.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(1.5F, 6.0F).sound(SoundType.WOOD)));
    public static final DeferredHolder<Block, Block> MAZE_CARPET = BLOCKS.register("maze_carpet", () -> new Block(mazeProps(MapColor.TERRACOTTA_YELLOW)));
    public static final DeferredHolder<Block, Block> MAZE_CEILING = BLOCKS.register("maze_ceiling",
            () -> new Block(Block.Properties.of().mapColor(MapColor.SAND).strength(1.0F, 3.0F).sound(SoundType.STONE)));

    /** Unbreakable plating of the security vault. */
    private static Block.Properties securityProps() {
        return Block.Properties.of().mapColor(MapColor.METAL).strength(-1.0F, 3600000.0F).sound(SoundType.NETHERITE_BLOCK)
                .noLootTable().pushReaction(PushReaction.BLOCK).isValidSpawn((s, l, p, t) -> false);
    }

    public static final DeferredHolder<Block, Block> SECURITY_WALL = BLOCKS.register("security_wall", () -> new Block(securityProps()));
    public static final DeferredHolder<Block, Block> SECURITY_DOOR = BLOCKS.register("security_door",
            () -> new SecurityDoorBlock(securityProps().noOcclusion()));

    /** 0.9.0: elevator floor plate (jump = up, sneak = down, right-click = floors per ride). */
    public static final DeferredHolder<Block, Block> ELEVATOR = BLOCKS.register("elevator",
            () -> new ElevatorBlock(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops().lightLevel(s -> 12)));
    /** 0.26.0: elevator cabin: sliding steel doors (placed by the ride), button panel, steel walls and a clean floor. */
    public static final DeferredHolder<Block, Block> ELEVATOR_DOOR = BLOCKS.register("elevator_door",
            () -> new ElevatorDoorBlock(Block.Properties.of().mapColor(MapColor.METAL).strength(6.0F, 1200.0F).sound(SoundType.METAL)
                    .noLootTable().noOcclusion().pushReaction(PushReaction.BLOCK)));
    public static final DeferredHolder<Block, Block> ELEVATOR_PANEL = BLOCKS.register("elevator_panel",
            () -> new ElevatorPanelBlock(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F).sound(SoundType.METAL)
                    .noCollission().noOcclusion().noLootTable().pushReaction(PushReaction.BLOCK).lightLevel(s -> 4)));
    public static final DeferredHolder<Block, Block> ELEVATOR_WALL = BLOCKS.register("elevator_wall",
            () -> new Block(Block.Properties.of().mapColor(MapColor.METAL).strength(1.8F, 6.0F).sound(SoundType.METAL)));
    /** 0.27.0: right-clicking the cabin floor also opens the panel (for cabins without room for a wall panel). */
    public static final DeferredHolder<Block, Block> ELEVATOR_FLOOR = BLOCKS.register("elevator_floor",
            () -> new Block(Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(3.0F, 6.0F).sound(SoundType.METAL)
                    .isValidSpawn((st, l, ps, t) -> false)) {
                @Override
                public net.minecraft.world.InteractionResult use(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level,
                        net.minecraft.core.BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand,
                        net.minecraft.world.phys.BlockHitResult hit) {
                    if (!player.getItemInHand(hand).isEmpty()) {
                        return net.minecraft.world.InteractionResult.PASS;
                    }
                    if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer sp) {
                        de.officerooms.world.ElevatorRides.openPanel(sp, pos);
                    }
                    return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
                }
            });
    public static final DeferredHolder<Item, Item> ELEVATOR_ITEM = ITEMS.register("elevator", () -> new BlockItem(ELEVATOR.get(), new Item.Properties()));

    /** 0.10.1: invisible temporary light (glowsticks, flashlight beam, flares) and chalk marks. */
    public static final DeferredHolder<Block, Block> TEMP_LIGHT = BLOCKS.register("temp_light", () -> new TempLightBlock(
            Block.Properties.of().replaceable().noCollission().noLootTable().instabreak().noOcclusion()
                    .pushReaction(PushReaction.DESTROY).lightLevel(TempLightBlock::lightLevel)));
    public static final DeferredHolder<Block, Block> CHALK_MARK = BLOCKS.register("chalk_mark", () -> new ChalkMarkBlock(
            Block.Properties.of().replaceable().noCollission().noLootTable().instabreak().noOcclusion()
                    .pushReaction(PushReaction.DESTROY).sound(SoundType.CALCITE)));

    /** 0.11.0: looks exactly like an office wall but sounds hollow and breaks quickly. Hides secret rooms. */
    public static final DeferredHolder<Block, Block> LOOSE_PANEL = BLOCKS.register("loose_panel", () -> new Block(
            Block.Properties.of().mapColor(MapColor.QUARTZ).strength(0.4F, 1.0F).sound(SoundType.WOOD).noLootTable()));

    public static final DeferredHolder<Item, Item> MAZE_WALLPAPER_ITEM = ITEMS.register("maze_wallpaper", () -> new BlockItem(MAZE_WALLPAPER.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> MAZE_CARPET_ITEM = ITEMS.register("maze_carpet", () -> new BlockItem(MAZE_CARPET.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> MAZE_CEILING_ITEM = ITEMS.register("maze_ceiling", () -> new BlockItem(MAZE_CEILING.get(), new Item.Properties()));

    public static final DeferredHolder<Item, Item> CEILING_PANEL_ITEM = ITEMS.register("ceiling_panel",
            () -> new BlockItem(CEILING_PANEL.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> CEILING_STRIP_ITEM = ITEMS.register("ceiling_strip",
            () -> new BlockItem(CEILING_STRIP.get(), new Item.Properties()));

    // --- 0.12.0: research and lab / security equipment --------------------------------------------------
    public static final java.util.List<DeferredHolder<Item, Item>> DECOR_ITEMS = new java.util.ArrayList<>();

    private static Block.Properties equip(SoundType sound, int light) {
        return Block.Properties.of().mapColor(MapColor.METAL).strength(1.5F, 3.0F).sound(sound).noOcclusion().lightLevel(s -> light);
    }

    private static DeferredHolder<Block, Block> decor(String name, Block.Properties props, net.minecraft.world.phys.shapes.VoxelShape shape) {
        DeferredHolder<Block, Block> b = BLOCKS.register(name, () -> new DecorBlock(props, shape));
        DECOR_ITEMS.add(ITEMS.register(name, () -> new BlockItem(b.get(), new Item.Properties())));
        return b;
    }

    private static net.minecraft.world.phys.shapes.VoxelShape box(double x0, double y0, double z0, double x1, double y1, double z1) {
        return Block.box(x0, y0, z0, x1, y1, z1);
    }

    public static final DeferredHolder<Block, Block> RESEARCH_TABLE = BLOCKS.register("research_table",
            () -> new ResearchTableBlock(equip(SoundType.WOOD, 0).mapColor(MapColor.WOOD), box(0, 0, 0, 16, 16, 16)));
    public static final DeferredHolder<Item, Item> RESEARCH_TABLE_ITEM = ITEMS.register("research_table",
            () -> new BlockItem(RESEARCH_TABLE.get(), new Item.Properties()));
    public static final DeferredHolder<Block, Block> CRT_MONITOR = decor("crt_monitor", equip(SoundType.METAL, 3), box(2, 0, 2, 14, 12, 14));
    public static final DeferredHolder<Block, Block> CCTV_BANK = decor("cctv_bank", equip(SoundType.METAL, 5), box(0, 0, 0, 16, 16, 16));
    public static final DeferredHolder<Block, Block> CONTROL_PANEL = decor("control_panel", equip(SoundType.METAL, 2), box(0, 0, 0, 16, 11, 16));
    public static final DeferredHolder<Block, Block> SERVER_RACK = decor("server_rack", equip(SoundType.METAL, 3), box(1, 0, 1, 15, 16, 15));
    public static final DeferredHolder<Block, Block> LAB_CABINET = decor("lab_cabinet", equip(SoundType.METAL, 0), box(0, 0, 0, 16, 16, 16));
    public static final DeferredHolder<Block, Block> MICROSCOPE = decor("microscope", equip(SoundType.METAL, 0), box(4, 0, 4, 12, 12, 12));
    public static final DeferredHolder<Block, Block> SPECIMEN_JAR = decor("specimen_jar", equip(SoundType.GLASS, 6), box(5, 0, 5, 11, 10, 11));
    public static final DeferredHolder<Block, Block> CONTAINMENT_TANK = decor("containment_tank", equip(SoundType.GLASS, 7), box(1, 0, 1, 15, 16, 15));
    public static final DeferredHolder<Block, Block> EXPERIMENT_MACHINE = decor("experiment_machine", equip(SoundType.METAL, 9), box(1, 0, 1, 15, 16, 15));

    // --- 0.14.0: The Abandoned Hotel ------------------------------------------------------------------
    private static DeferredHolder<Block, Block> simple(String name, Block.Properties props) {
        DeferredHolder<Block, Block> b = BLOCKS.register(name, () -> new Block(props));
        DECOR_ITEMS.add(ITEMS.register(name, () -> new BlockItem(b.get(), new Item.Properties())));
        return b;
    }

    private static Block.Properties hotel(MapColor color, SoundType sound, int light) {
        return Block.Properties.of().mapColor(color).strength(1.5F, 3.0F).sound(sound).lightLevel(s -> light);
    }

    public static final DeferredHolder<Block, Block> HOTEL_WALLPAPER = simple("hotel_wallpaper", hotel(MapColor.COLOR_RED, SoundType.WOOD, 0));
    public static final DeferredHolder<Block, Block> HOTEL_WAINSCOT = simple("hotel_wainscot", hotel(MapColor.COLOR_BROWN, SoundType.WOOD, 0));
    public static final DeferredHolder<Block, Block> HOTEL_CARPET = simple("hotel_carpet", hotel(MapColor.COLOR_RED, SoundType.WOOL, 0).strength(0.8F));
    public static final DeferredHolder<Block, Block> HOTEL_CARPET_TEAL = simple("hotel_carpet_teal", hotel(MapColor.COLOR_CYAN, SoundType.WOOL, 0).strength(0.8F));
    public static final DeferredHolder<Block, Block> HOTEL_MARBLE = simple("hotel_marble", hotel(MapColor.QUARTZ, SoundType.STONE, 0));
    public static final DeferredHolder<Block, Block> HOTEL_CEILING = simple("hotel_ceiling", hotel(MapColor.SAND, SoundType.STONE, 0));
    public static final DeferredHolder<Block, Block> HOTEL_LAMP = decor("hotel_lamp", hotel(MapColor.GOLD, SoundType.GLASS, 12).noOcclusion(), box(3, 12, 3, 13, 16, 13));
    public static final DeferredHolder<Block, Block> HOTEL_LAMP_BROKEN = decor("hotel_lamp_broken", hotel(MapColor.GOLD, SoundType.GLASS, 0).noOcclusion(), box(3, 12, 3, 13, 16, 13));
    public static final DeferredHolder<Block, Block> HOTEL_BED = decor("hotel_bed", hotel(MapColor.COLOR_RED, SoundType.WOOL, 0).noOcclusion(), box(0, 0, 0, 16, 9, 16));
    public static final DeferredHolder<Block, Block> HOTEL_BED_FOOT = decor("hotel_bed_foot", hotel(MapColor.COLOR_RED, SoundType.WOOL, 0).noOcclusion(), box(0, 0, 0, 16, 9, 16));
    public static final DeferredHolder<Block, Block> HOTEL_NIGHTSTAND = decor("hotel_nightstand", hotel(MapColor.COLOR_BROWN, SoundType.WOOD, 7).noOcclusion(), box(2, 0, 2, 14, 16, 14));
    public static final DeferredHolder<Block, Block> HOTEL_DRESSER = decor("hotel_dresser", hotel(MapColor.COLOR_BROWN, SoundType.WOOD, 0).noOcclusion(), box(0, 0, 1, 16, 14, 15));
    public static final DeferredHolder<Block, Block> HOTEL_RECEPTION_DESK = decor("hotel_reception_desk", hotel(MapColor.COLOR_BROWN, SoundType.WOOD, 0).noOcclusion(), box(0, 0, 0, 16, 16, 16));
    public static final DeferredHolder<Block, Block> PIPE = BLOCKS.register("pipe", () -> new PipeBlock(
            Block.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(2.0F, 6.0F).sound(SoundType.COPPER).noOcclusion()));
    public static final DeferredHolder<Item, Item> PIPE_ITEM = ITEMS.register("pipe", () -> new BlockItem(PIPE.get(), new Item.Properties()));

    // --- 0.15.0: The Industrial Complex -----------------------------------------------------------------
    public static final net.minecraft.world.level.block.state.properties.BlockSetType GRATE_SET =
            net.minecraft.world.level.block.state.properties.BlockSetType.register(new net.minecraft.world.level.block.state.properties.BlockSetType(
                    "office_rooms_grate", true, SoundType.METAL,
                    net.minecraft.sounds.SoundEvents.IRON_DOOR_CLOSE, net.minecraft.sounds.SoundEvents.IRON_DOOR_OPEN,
                    net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_CLOSE, net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_OPEN,
                    net.minecraft.sounds.SoundEvents.METAL_PRESSURE_PLATE_CLICK_OFF, net.minecraft.sounds.SoundEvents.METAL_PRESSURE_PLATE_CLICK_ON,
                    net.minecraft.sounds.SoundEvents.STONE_BUTTON_CLICK_OFF, net.minecraft.sounds.SoundEvents.STONE_BUTTON_CLICK_ON));

    private static Block.Properties metal(int light) {
        return Block.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F).sound(SoundType.METAL).lightLevel(s -> light);
    }

    public static final DeferredHolder<Block, Block> INDUSTRIAL_WALL = simple("industrial_wall", metal(0).mapColor(MapColor.COLOR_GRAY).sound(SoundType.STONE));
    public static final DeferredHolder<Block, Block> INDUSTRIAL_FLOOR = simple("industrial_floor", metal(0).mapColor(MapColor.COLOR_GRAY).sound(SoundType.STONE));
    public static final DeferredHolder<Block, Block> INDUSTRIAL_CEILING = simple("industrial_ceiling", metal(0).mapColor(MapColor.COLOR_GRAY).sound(SoundType.STONE));
    public static final DeferredHolder<Block, Block> GRATE = simple("grate", metal(0).noOcclusion().isViewBlocking((st, l, p) -> false)
            .isSuffocating((st, l, p) -> false));
    public static final DeferredHolder<Block, Block> GENERATOR_CASING = simple("generator_casing", metal(0));
    /** 0.18.0: a running core hums loudly (machine hum now and then while a player is near). */
    public static final DeferredHolder<Block, Block> GENERATOR_CORE = BLOCKS.register("generator_core", () -> new Block(metal(13)) {
        @Override
        public void animateTick(net.minecraft.world.level.block.state.BlockState s, net.minecraft.world.level.Level l, net.minecraft.core.BlockPos p,
                                net.minecraft.util.RandomSource r) {
            if (r.nextInt(4) == 0) {
                l.playLocalSound(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, de.officerooms.core.ModSounds.AMB_MACHINE.get(),
                        net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 0.9F + r.nextFloat() * 0.2F, false);
            }
        }
    });
    public static final DeferredHolder<Item, Item> GENERATOR_CORE_ITEM = ITEMS.register("generator_core", () -> new BlockItem(GENERATOR_CORE.get(), new Item.Properties()));
    public static final DeferredHolder<Block, Block> GENERATOR_CORE_DEAD = simple("generator_core_dead", metal(0));
    public static final DeferredHolder<Block, Block> CAGE_LAMP = decor("cage_lamp", metal(12).noOcclusion(), box(4, 9, 4, 12, 16, 12));
    public static final DeferredHolder<Block, Block> CAGE_LAMP_BROKEN = decor("cage_lamp_broken", metal(0).noOcclusion(), box(4, 9, 4, 12, 16, 12));
    public static final DeferredHolder<Block, Block> GRATE_DOOR = BLOCKS.register("grate_door", () -> new net.minecraft.world.level.block.DoorBlock(
            metal(0).noOcclusion().pushReaction(PushReaction.DESTROY), GRATE_SET));
    public static final DeferredHolder<Item, Item> GRATE_DOOR_ITEM = ITEMS.register("grate_door",
            () -> new net.minecraft.world.item.DoubleHighBlockItem(GRATE_DOOR.get(), new Item.Properties()));

    // --- 0.16.0: Level Fun ------------------------------------------------------------------------------
    private static Block.Properties party(MapColor c, SoundType sound) {
        return Block.Properties.of().mapColor(c).strength(0.5F).sound(sound);
    }

    public static final DeferredHolder<Block, Block> FUN_WALLPAPER = simple("fun_wallpaper", party(MapColor.COLOR_YELLOW, SoundType.WOOD).strength(1.5F, 3.0F));
    public static final DeferredHolder<Block, Block> BALLOON_RED = decor("balloon_red", party(MapColor.COLOR_RED, SoundType.WOOL).noCollission().noOcclusion(), box(4, 0, 4, 12, 16, 12));
    public static final DeferredHolder<Block, Block> BALLOON_BLUE = decor("balloon_blue", party(MapColor.COLOR_BLUE, SoundType.WOOL).noCollission().noOcclusion(), box(4, 0, 4, 12, 16, 12));
    public static final DeferredHolder<Block, Block> BALLOON_GREEN = decor("balloon_green", party(MapColor.COLOR_GREEN, SoundType.WOOL).noCollission().noOcclusion(), box(4, 0, 4, 12, 16, 12));
    public static final DeferredHolder<Block, Block> BALLOON_PINK = decor("balloon_pink", party(MapColor.COLOR_PINK, SoundType.WOOL).noCollission().noOcclusion(), box(4, 0, 4, 12, 16, 12));
    public static final DeferredHolder<Block, Block> BALLOON_PURPLE = decor("balloon_purple", party(MapColor.COLOR_PURPLE, SoundType.WOOL).noCollission().noOcclusion(), box(4, 0, 4, 12, 16, 12));
    public static final DeferredHolder<Block, Block> STREAMER = decor("streamer", party(MapColor.COLOR_PINK, SoundType.WOOL).noCollission().noOcclusion(), box(0, 10, 7, 16, 16, 9));
    public static final DeferredHolder<Block, Block> CONFETTI = decor("confetti", party(MapColor.COLOR_PINK, SoundType.WOOL).noCollission().noOcclusion().instabreak(), box(0, 0, 0, 16, 0.5, 16));
    public static final DeferredHolder<Block, Block> GIFT_BOX = decor("gift_box", party(MapColor.COLOR_LIGHT_BLUE, SoundType.WOOL).noOcclusion(), box(3, 0, 3, 13, 10, 13));
    public static final DeferredHolder<Block, Block> PARTY_TABLE = decor("party_table", party(MapColor.SNOW, SoundType.WOOD).noOcclusion(), box(0, 0, 0, 16, 15, 16));
    public static final DeferredHolder<Block, Block> PLAY_CARPET = simple("play_carpet", party(MapColor.COLOR_RED, SoundType.WOOL).strength(0.8F));
    public static final DeferredHolder<Block, Block> INFLATABLE_RED = simple("inflatable_red", party(MapColor.COLOR_RED, SoundType.WOOL).strength(0.6F));
    public static final DeferredHolder<Block, Block> INFLATABLE_BLUE = simple("inflatable_blue", party(MapColor.COLOR_BLUE, SoundType.WOOL).strength(0.6F));
    public static final DeferredHolder<Block, Block> INFLATABLE_YELLOW = simple("inflatable_yellow", party(MapColor.COLOR_YELLOW, SoundType.WOOL).strength(0.6F));
    public static final DeferredHolder<Block, Block> SMILE_GRAFFITI = decor("smile_graffiti", party(MapColor.COLOR_RED, SoundType.STONE).noCollission().noOcclusion().instabreak(), box(0, 0, 15, 16, 16, 16));

    // --- 0.17.0: HAZARD ZONE monitor -------------------------------------------------------------------
    public static final net.neoforged.neoforge.registries.DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES =
            net.neoforged.neoforge.registries.DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.BLOCK_ENTITY_TYPES, OfficeRoomsIds.MOD_ID);
    public static final DeferredHolder<Block, Block> HAZARD_MONITOR = BLOCKS.register("hazard_monitor", () -> new HazardMonitorBlock(
            equip(SoundType.METAL, 4), box(0, 2, 6, 16, 14, 16)));
    public static final DeferredHolder<Item, Item> HAZARD_MONITOR_ITEM = ITEMS.register("hazard_monitor",
            () -> new BlockItem(HAZARD_MONITOR.get(), new Item.Properties()));
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<HazardMonitorBlock.Entity>> HAZARD_MONITOR_BE =
            BLOCK_ENTITIES.register("hazard_monitor", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder
                    .of(HazardMonitorBlock.Entity::new, HAZARD_MONITOR.get()).build(null));

    /** A HAZARD ZONE monitor facing {@code f}; broken ones only show corrupted data. */
    public static net.minecraft.world.level.block.state.BlockState monitor(net.minecraft.core.Direction f, boolean broken) {
        return HAZARD_MONITOR.get().defaultBlockState().setValue(DecorBlock.FACING, f).setValue(HazardMonitorBlock.BROKEN, broken);
    }

    private ModBlocks() {
    }

    /** 0.22.0: blackouts. */
    public static final DeferredHolder<Block, Block> FUSE_BOX = BLOCKS.register("fuse_box", () -> new FuseBoxBlock(Block.Properties.of().mapColor(MapColor.METAL)
            .strength(3.0F, 6.0F).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredHolder<Item, Item> FUSE_BOX_ITEM = ITEMS.register("fuse_box", () -> new BlockItem(FUSE_BOX.get(), new Item.Properties()));

    /** 0.19.0: the Underside. */
    public static final DeferredHolder<Block, Block> UNDERSIDE_GRASS = simple("underside_grass", Block.Properties.of().mapColor(MapColor.GRASS).strength(0.6F).sound(SoundType.GRASS));
    public static final DeferredHolder<Block, Block> UNDERSIDE_PORTAL = BLOCKS.register("underside_portal",
            () -> new UndersidePortalBlock(Block.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(-1.0F, 3600000.0F)
                    .sound(SoundType.GLASS).noCollission().noOcclusion().lightLevel(s -> 12).noLootTable().pushReaction(PushReaction.BLOCK)));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(ModBlocks::onCreativeTab);
    }

    private static void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(POOL_TILE_ITEM);
            event.accept(POOL_FLOOR_TILE_ITEM);
            event.accept(POOL_TILE_STAIRS_ITEM);
            event.accept(POOL_TILE_SLAB_ITEM);
        }
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(CEILING_PANEL_ITEM);
            event.accept(CEILING_STRIP_ITEM);
        }
    }
}
