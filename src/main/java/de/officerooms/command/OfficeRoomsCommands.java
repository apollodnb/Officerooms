package de.officerooms.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import de.officerooms.OfficeRooms;
import de.officerooms.atrium.Atrium;
import de.officerooms.dimension.OfficeRoomsDimension;
import de.officerooms.room.Navigator;
import de.officerooms.room.OfficePlanner;
import de.officerooms.room.PlanVerifier;
import de.officerooms.room.RegionLayout;
import de.officerooms.room.RegionPlan;
import de.officerooms.room.Room;
import de.officerooms.room.structure.BlockSampler;
import de.officerooms.worldgen.OfficeChunkGenerator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.util.Map;

/**
 * {@code /officerooms ...} – Test- und Komfortbefehle (Berechtigungsstufe 2).
 *
 * <ul>
 *     <li>{@code enter}: in einen Büroraum in Weltmitte teleportieren (mittleres Geschoss).</li>
 *     <li>{@code window [minHeight]}: zum nächstgelegenen Atriumfenster teleportieren (optional nur
 *     Atrien mit mindestens minHeight Blöcken Höhe), Blick durchs Fenster.</li>
 *     <li>{@code info}: Region, Geschoss und Raum an der aktuellen Position.</li>
 *     <li>{@code stats}: Kennzahlen der aktuellen Region (Raumtypen, Höhen, Atrien, Öffnungen).</li>
 *     <li>{@code verify [radius]}: vergleicht die generierten Weltblöcke mit der Planung und prüft
 *     die Regeln (Fenster nur zu Atrien, keine Türen ins Leere/in Wände, keine Luft im Massiv).</li>
 * </ul>
 */
public final class OfficeRoomsCommands {

    private OfficeRoomsCommands() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(OfficeRoomsCommands::onRegister);
    }

    private static void onRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        net.minecraft.commands.CommandBuildContext ctxBuild = event.getBuildContext();
        d.register(Commands.literal("officerooms")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("enter").executes(OfficeRoomsCommands::enter))
                .then(Commands.literal("window")
                        .executes(ctx -> window(ctx, 0))
                        .then(Commands.argument("minHeight", IntegerArgumentType.integer(0, 4000))
                                .executes(ctx -> window(ctx, IntegerArgumentType.getInteger(ctx, "minHeight")))))
                .then(Commands.literal("locate").then(Commands.argument("type", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .suggests((c, b2) -> net.minecraft.commands.SharedSuggestionProvider.suggest(LOCATE_TYPES, b2))
                        .executes(ctx -> locate(ctx, com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "type")))))
                .then(Commands.literal("hazard")
                        .then(Commands.literal("status").executes(ctx -> hazard(ctx.getSource(), "status")))
                        .then(Commands.literal("warn").executes(ctx -> hazard(ctx.getSource(), "warn")))
                        .then(Commands.literal("start").executes(ctx -> hazard(ctx.getSource(), "start")))
                        .then(Commands.literal("stop").executes(ctx -> hazard(ctx.getSource(), "stop"))))
                .then(Commands.literal("info").executes(OfficeRoomsCommands::info))
                .then(Commands.literal("stats").executes(OfficeRoomsCommands::stats))
                .then(Commands.literal("furniture").executes(OfficeRoomsCommands::furniture))
                .then(Commands.literal("mobs").executes(OfficeRoomsCommands::mobs))
                .then(Commands.literal("find").then(Commands.argument("block", net.minecraft.commands.arguments.blocks.BlockStateArgument.block(ctxBuild))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 64)).executes(ctx -> {
                            // Testhilfe: sucht einen Block im Würfel um den Ausführenden (in dessen Dimension)
                            BlockState want = net.minecraft.commands.arguments.blocks.BlockStateArgument.getBlock(ctx, "block").getState();
                            int rad = IntegerArgumentType.getInteger(ctx, "radius");
                            ServerLevel lvl = ctx.getSource().getLevel();
                            BlockPos c = BlockPos.containing(ctx.getSource().getPosition());
                            int found = 0;
                            for (BlockPos q : BlockPos.betweenClosed(c.offset(-rad, -rad, -rad), c.offset(rad, rad, rad))) {
                                if (lvl.isLoaded(q) && lvl.getBlockState(q).is(want.getBlock())) {
                                    say(ctx.getSource(), "found " + q.getX() + " " + q.getY() + " " + q.getZ());
                                    if (++found >= 5) {
                                        break;
                                    }
                                }
                            }
                            if (found == 0) {
                                say(ctx.getSource(), "not found");
                            }
                            return found;
                        }))))
                .then(Commands.literal("deathmoth").executes(ctx -> {
                    var pl = ctx.getSource().getPlayerOrException();
                    boolean ok = pl.level() instanceof ServerLevel sl && de.officerooms.entity.DeathmothDirector.trySpawn(sl, pl);
                    say(ctx.getSource(), ok ? "A Deathmoth is waiting somewhere nearby." : "No hidden dark wall found here (or a forbidden area).");
                    return ok ? 1 : 0;
                }))
                .then(Commands.literal("observer").executes(ctx -> {
                    net.minecraft.server.level.ServerPlayer pl = ctx.getSource().getPlayerOrException();
                    boolean ok = pl.level() instanceof ServerLevel sl && de.officerooms.entity.ObserverDirector.trySpawn(sl, pl, false);
                    say(ctx.getSource(), ok ? "Something is watching." : "No suitable place for an observer here.");
                    return ok ? 1 : 0;
                }))
                .then(Commands.literal("spawning").then(Commands.argument("enabled", com.mojang.brigadier.arguments.BoolArgumentType.bool())
                        .executes(ctx -> {
                            boolean on = com.mojang.brigadier.arguments.BoolArgumentType.getBool(ctx, "enabled");
                            de.officerooms.core.OfficeRoomsConfig.SPAWN_ENABLED.set(on);
                            say(ctx.getSource(), "Office Rooms mob spawns " + (on ? "on" : "off"));
                            return 1;
                        })))
                .then(Commands.literal("blackout")
                        .then(Commands.literal("status").executes(ctx -> {
                            var bd = de.officerooms.hazard.BlackoutEvents.data(ctx.getSource().getServer());
                            long now = ctx.getSource().getServer().overworld().getGameTime();
                            say(ctx.getSource(), "Blackout phase " + bd.phase + " (" + bd.name + "), next/phase change in " + Math.max(0, ((bd.phase == 0 ? bd.nextCycle : bd.phaseEnd) - now) / 20) + " s, fuse floor " + bd.fuseFloor);
                            return 1;
                        }))
                        .then(Commands.literal("warn").executes(ctx -> {
                            var srv = ctx.getSource().getServer();
                            var l = srv.getLevel(de.officerooms.dimension.OfficeRoomsDimension.LEVEL_KEY);
                            boolean ok = l != null && de.officerooms.hazard.BlackoutEvents.warn(srv, l, de.officerooms.hazard.BlackoutEvents.data(srv), ctx.getSource().getPlayer());
                            say(ctx.getSource(), ok ? "Blackout announced." : "No blackout possible: no player (outside spectator mode) in the Office Rooms.");
                            return ok ? 1 : 0;
                        }))
                        .then(Commands.literal("start").executes(ctx -> {
                            var srv = ctx.getSource().getServer();
                            var l = srv.getLevel(de.officerooms.dimension.OfficeRoomsDimension.LEVEL_KEY);
                            var bd = de.officerooms.hazard.BlackoutEvents.data(srv);
                            if (l == null) {
                                return 0;
                            }
                            if (bd.phase == 0 && !de.officerooms.hazard.BlackoutEvents.warn(srv, l, bd, ctx.getSource().getPlayer())) {
                                return fail(ctx.getSource(), "No blackout possible: no player (outside spectator mode) in the Office Rooms.");
                            }
                            de.officerooms.hazard.BlackoutEvents.start(srv, l, bd);
                            say(ctx.getSource(), "Blackout started: " + bd.name + ", fuse box on floor " + bd.fuseFloor);
                            return 1;
                        }))
                        .then(Commands.literal("stop").executes(ctx -> {
                            var srv = ctx.getSource().getServer();
                            var l = srv.getLevel(de.officerooms.dimension.OfficeRoomsDimension.LEVEL_KEY);
                            if (l != null) {
                                de.officerooms.hazard.BlackoutEvents.end(srv, l, de.officerooms.hazard.BlackoutEvents.data(srv), null);
                            }
                            say(ctx.getSource(), "Blackout ended.");
                            return 1;
                        })))
                .then(Commands.literal("level0")
                        .executes(ctx -> {
                            var pl = ctx.getSource().getPlayerOrException();
                            de.officerooms.level0.Level0Events.enter(pl, pl.getBlockX(), pl.getBlockZ());
                            return 1;
                        })
                        .then(Commands.argument("find", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(new String[]{"manila", "threshold"}, b))
                                .executes(ctx -> {
                                    var pl = ctx.getSource().getPlayerOrException();
                                    var l0 = pl.server.getLevel(de.officerooms.level0.Level0Events.LEVEL_KEY);
                                    if (l0 == null) {
                                        return 0;
                                    }
                                    net.minecraft.core.BlockPos to = de.officerooms.level0.Level0Events.locate(l0.getSeed(), pl.getBlockX(), pl.getBlockZ(),
                                            com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "find").equals("manila"));
                                    if (to == null) {
                                        return fail(ctx.getSource(), "Nothing found nearby.");
                                    }
                                    pl.teleportTo(l0, to.getX() + 0.5, to.getY(), to.getZ() + 0.5, pl.getYRot(), 0F);
                                    say(ctx.getSource(), "Level 0: " + to.toShortString());
                                    return 1;
                                })))
                .then(Commands.literal("outpost")
                        .then(Commands.argument("theme", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(new String[]{"lab", "ancient", "jungle", "cave", "nether", "surface", "ocean", "try_surface", "try_ocean"}, b))
                                .executes(ctx -> {
                                    var pl = ctx.getSource().getPlayerOrException();
                                    String arg = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "theme");
                                    if (arg.startsWith("try_")) {                // 0.39.0: run the natural placement check for this chunk
                                        var t = de.officerooms.outpost.OutpostBuilder.Theme.valueOf(arg.substring(4).toUpperCase(java.util.Locale.ROOT));
                                        net.minecraft.core.BlockPos at = de.officerooms.outpost.OutpostPlacer.tryAt(pl.serverLevel(), t, pl.chunkPosition());
                                        say(ctx.getSource(), at == null ? "Natural " + t + " placement refused: " + de.officerooms.outpost.OutpostPlacer.reason : "Natural " + t + " outpost placed, lever at " + at.toShortString());
                                        return at == null ? 0 : 1;
                                    }
                                    var th = de.officerooms.outpost.OutpostBuilder.Theme.valueOf(
                                            com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "theme").toUpperCase(java.util.Locale.ROOT));
                                    net.minecraft.core.Direction look = pl.getDirection();
                                    net.minecraft.core.BlockPos door = pl.blockPosition().relative(look, 2);
                                    net.minecraft.core.BlockPos lever = de.officerooms.outpost.OutpostBuilder.build(pl.serverLevel(), door, look.getOpposite(), th, pl.getRandom().nextLong());
                                    say(ctx.getSource(), "Research outpost (" + th + ") built, lever at " + lever.toShortString());
                                    return 1;
                                })))
                .then(Commands.literal("underside")
                        .executes(ctx -> { de.officerooms.underside.UndersideEvents.enter(ctx.getSource().getPlayerOrException(), false); return 1; })
                        .then(Commands.literal("now")
                                .executes(ctx -> { de.officerooms.underside.UndersideEvents.enter(ctx.getSource().getPlayerOrException(), true); return 1; })))
                .then(Commands.literal("goto")
                        .then(Commands.argument("type", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                        java.util.stream.Stream.concat(java.util.Arrays.stream(de.officerooms.furnish.RoomType.values()).map(t -> t.id),
                                                java.util.stream.Stream.concat(java.util.stream.Stream.of("poolrooms", "poolrooms_maze", "neighbourhood", "maze", "maze_vault", "hotel", "hotel_vault", "industrial", "level_fun", "cinema", "hideout", "elevator", "secure_storage",
                                                        "fuse_box", "hazard_zone", "outpost", "shrine"), java.util.Arrays.stream(HIDEOUT_IDS))), b))
                                .executes(ctx -> gotoType(ctx, com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "type")))))
                .then(Commands.literal("verify")
                        .executes(ctx -> verify(ctx, 1))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(0, 6))
                                .executes(ctx -> verify(ctx, IntegerArgumentType.getInteger(ctx, "radius"))))));
    }

    private static ServerLevel officeLevel(CommandSourceStack src) {
        return src.getServer().getLevel(OfficeRoomsDimension.LEVEL_KEY);
    }

    private static BlockSampler sampler(ServerLevel level) {
        if (level != null && level.getChunkSource().getGenerator() instanceof OfficeChunkGenerator g) {
            return g.sampler(level.getChunkSource().randomState(), level);
        }
        return null;
    }

    /** 0.17.0: control the HAZARD ZONE event (status, warn = start the warning now, start = activate now, stop). */
    private static int hazard(CommandSourceStack src, String what) {
        net.minecraft.server.MinecraftServer srv = src.getServer();
        net.minecraft.server.level.ServerLevel office = srv.getLevel(de.officerooms.dimension.OfficeRoomsDimension.LEVEL_KEY);
        if (office == null) {
            return fail(src, "The Office Rooms dimension is not loaded.");
        }
        de.officerooms.hazard.HazardData d = de.officerooms.hazard.HazardEvents.data(srv);
        long t = srv.overworld().getGameTime();
        switch (what) {
            case "warn" -> {
                if (d.phase != de.officerooms.hazard.HazardState.IDLE || !de.officerooms.hazard.HazardEvents.startWarning(srv, office, d, t)) {
                    return fail(src, "No warning started (an event is running or no megastructure is near).");
                }
            }
            case "start" -> {
                if (d.phase == de.officerooms.hazard.HazardState.IDLE && !de.officerooms.hazard.HazardEvents.startWarning(srv, office, d, t)) {
                    return fail(src, "No megastructure near a player.");
                }
                if (d.phase == de.officerooms.hazard.HazardState.WARNING) {
                    de.officerooms.hazard.HazardEvents.activate(srv, office, d, t);
                }
            }
            case "stop" -> {
                if (d.phase != de.officerooms.hazard.HazardState.IDLE) {
                    de.officerooms.hazard.HazardEvents.end(srv, office, d);
                }
            }
            default -> {
            }
        }
        String[] ph = {"idle", "WARNING", "ACTIVE"};
        long left = d.phase == 0 ? d.nextCycle - t : d.phaseEnd - t;
        say(src, "Hazard Zone: " + ph[d.phase] + (d.phase == 0 ? ", next warning in " : ", phase ends in ")
                + de.officerooms.hazard.HazardState.clock((int) Math.max(0, left / 20)) + (d.phase == 0 ? "" : " \u2013 " + d.name + " x "
                + d.box[0] + ".." + d.box[3] + " y " + d.box[1] + ".." + d.box[4] + " z " + d.box[2] + ".." + d.box[5]
                + ", chests placed " + d.placed.size() + ", waiting " + d.pending.size()));
        return 1;
    }

    private static void say(CommandSourceStack src, String msg) {
        src.sendSuccess(() -> Component.literal("[Office Rooms] " + msg), false);
        OfficeRooms.LOGGER.info("[Befehl] {}", msg);
    }

    private static int fail(CommandSourceStack src, String msg) {
        src.sendFailure(Component.literal("[Office Rooms] " + msg));
        OfficeRooms.LOGGER.warn("[Befehl] {}", msg);
        return 0;
    }

    private static int enter(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer player = src.getPlayerOrException();
        ServerLevel level = officeLevel(src);
        BlockSampler s = sampler(level);
        if (s == null) {
            return fail(src, "The dimension office_rooms:office_rooms does not use the Office Rooms generator.");
        }
        OfficePlanner p = s.planner();
        Navigator.Target t = Navigator.findRoom(p, 0, 0, p.storeys / 2);
        if (t == null) {
            return fail(src, "No arrival room found.");
        }
        player.teleportTo(level, t.x(), t.y(), t.z(), t.yaw(), 0f);
        say(src, "Welcome. Storey " + p.storeyOf((int) t.y()) + " of " + p.storeys + " (Y " + (int) t.y() + ").");
        return 1;
    }

    private static int window(CommandContext<CommandSourceStack> ctx, int minHeight) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer player = src.getPlayerOrException();
        ServerLevel level = officeLevel(src);
        BlockSampler s = sampler(level);
        if (s == null) {
            return fail(src, "Office Rooms generator not active.");
        }
        BlockPos pos = player.level() == level ? player.blockPosition() : new BlockPos(0, s.planner().floorY(s.planner().storeys / 2), 0);
        Navigator.Target t = Navigator.findWindow(s.planner(), pos.getX(), pos.getY(), pos.getZ(), minHeight > 40 ? 3 : 1, minHeight);
        if (t == null) {
            return fail(src, "No atrium window found nearby.");
        }
        player.teleportTo(level, t.x(), t.y(), t.z(), t.yaw(), 0f);
        say(src, "Window into: " + t.description());
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        BlockSampler s = sampler(src.getLevel());
        if (s == null) {
            return fail(src, "Not in the Office Rooms dimension.");
        }
        OfficePlanner p = s.planner();
        BlockPos pos = BlockPos.containing(src.getPosition());
        int rx = p.regionOf(pos.getX());
        int rz = p.regionOf(pos.getZ());
        int st = p.storeyOf(pos.getY());
        say(src, "Region " + rx + "," + rz + ", storey " + st + " (floor Y " + p.floorY(st) + "), cell "
                + Math.floorMod(pos.getX(), p.regionBlocks) / p.unit + "," + Math.floorMod(pos.getZ(), p.regionBlocks) / p.unit);
        if (st >= 0 && st < p.storeys) {
            Room r = p.layout(rx, rz).rawRoom(st, Math.floorMod(pos.getX(), p.regionBlocks) / p.unit,
                    Math.floorMod(pos.getZ(), p.regionBlocks) / p.unit);
            say(src, r == null ? "solid" : r + (r.atrium != null ? " – " + r.atrium : ""));
        }
        return 1;
    }

    private static int stats(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        BlockSampler s = sampler(src.getLevel());
        if (s == null) {
            return fail(src, "Not in the Office Rooms dimension.");
        }
        OfficePlanner p = s.planner();
        BlockPos pos = BlockPos.containing(src.getPosition());
        RegionPlan plan = p.plan(p.regionOf(pos.getX()), p.regionOf(pos.getZ()));
        RegionLayout l = plan.layout;
        int rooms = 0;
        int corridors = 0;
        int stairs = 0;
        int sealed = 0;
        int[] bands = new int[4];
        for (Room r : l.rooms) {
            if (r.cells == 0) {
                continue;
            }
            if (r.sealed) {
                sealed++;
                continue;
            }
            switch (r.kind) {
                case ROOM -> {
                    rooms++;
                    bands[r.clear <= 4 ? 0 : r.clear <= 8 ? 1 : r.clear <= 16 ? 2 : 3]++;
                }
                case CORRIDOR -> corridors++;
                case STAIRS -> stairs++;
                default -> {
                }
            }
        }
        say(src, "Region " + l.rx + "," + l.rz + ": " + p.storeys + " storeys, " + rooms + " rooms, " + corridors
                + " corridor networks, " + stairs + " staircases, " + l.atria.size() + " atriums, " + sealed + " sealed");
        say(src, String.format("Room heights: 4 = %.1f %%, 5-8 = %.1f %%, 9-16 = %.1f %%, >16 = %.1f %%",
                100.0 * bands[0] / Math.max(1, rooms), 100.0 * bands[1] / Math.max(1, rooms),
                100.0 * bands[2] / Math.max(1, rooms), 100.0 * bands[3] / Math.max(1, rooms)));
        say(src, "Openings: " + plan.doors + " doors/passages (" + plan.boundaryDoors + " on region borders), "
                + plan.windows + " windows");
        for (Atrium a : l.atria) {
            say(src, " - " + a + ", galleries " + a.galleryCount() + ", floor Y " + p.floorY(a.s0));
        }
        return 1;
    }

    /** Teleportiert vor die Tür des nächsten Raums mit diesem Einrichtungstyp (gleiche Region, nahe Geschosse). */
    /** 0.25.0: the hidden rooms one by one (index = Extras.Hideout type). */
    static final String[] HIDEOUT_IDS = {"maintenance_room", "supply_closet", "forgotten_break_room", "security_office", "research_lab", "concealed_room"};

    private static int hideoutFilter(String t) {
        return java.util.Arrays.asList(HIDEOUT_IDS).indexOf(t);
    }

    /** 0.25.0: targets that are not rooms of the planner: blackout fuse box, hazard zone, research outposts, shrine. */
    private static int gotoSpecial(CommandContext<CommandSourceStack> ctx, String typeId) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer player = src.getPlayerOrException();
        var srv = src.getServer();
        if (typeId.equals("fuse_box") || typeId.equals("hazard_zone")) {
            ServerLevel office = srv.getLevel(OfficeRoomsDimension.LEVEL_KEY);
            if (office == null) {
                return fail(src, "Office Rooms dimension missing.");
            }
            BlockPos at;
            if (typeId.equals("fuse_box")) {
                var d = de.officerooms.hazard.BlackoutEvents.data(srv);
                if (d.fuse == 0) {
                    return fail(src, "No broken fuse box right now (start a blackout with /officerooms blackout start).");
                }
                at = BlockPos.of(d.fuse);
            } else {
                var d = de.officerooms.hazard.HazardEvents.data(srv);
                if (d.phase == 0) {
                    return fail(src, "No Hazard Zone announced or active.");
                }
                at = new BlockPos((d.box[0] + d.box[3]) / 2, player.getBlockY(), (d.box[2] + d.box[5]) / 2);
            }
            BlockPos t = safeSpot(office, at);
            player.teleportTo(office, t.getX() + 0.5, t.getY(), t.getZ() + 0.5, player.getYRot(), player.getXRot());
            say(src, typeId.replace('_', ' ') + " at " + t.getX() + " " + t.getY() + " " + t.getZ());
            return 1;
        }
        if (typeId.equals("outpost")) {
            de.officerooms.outpost.OutpostRegistry.Entry best = null;
            ServerLevel bestLevel = null;
            double bd = Double.MAX_VALUE;
            for (ServerLevel l : srv.getAllLevels()) {
                for (var e : de.officerooms.outpost.OutpostRegistry.of(l).entries) {
                    double d = l == player.level() ? e.pos().distSqr(player.blockPosition()) : 1e12 + e.pos().distSqr(BlockPos.ZERO);
                    if (d < bd) {
                        bd = d;
                        best = e;
                        bestLevel = l;
                    }
                }
            }
            if (best == null) {
                return fail(src, "No research outpost generated yet. They appear in new chunks near ancient cities, jungle temples, deep caves and in the Nether.");
            }
            BlockPos t = safeSpot(bestLevel, best.pos());
            player.teleportTo(bestLevel, t.getX() + 0.5, t.getY(), t.getZ() + 0.5, player.getYRot(), player.getXRot());
            say(src, "Research outpost (" + best.theme() + ") at " + t.getX() + " " + t.getY() + " " + t.getZ() + " in " + bestLevel.dimension().location());
            return 1;
        }
        // shrine: vanilla structure search in the Overworld
        ServerLevel ow = srv.overworld();
        var reg = ow.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE, new ResourceLocation("office_rooms", "abandoned_office_shrine"));
        var holder = reg.getHolder(key);
        if (holder.isEmpty()) {
            return fail(src, "Shrine structure not registered.");
        }
        BlockPos from = player.level() == ow ? player.blockPosition() : ow.getSharedSpawnPos();
        var found = ow.getChunkSource().getGenerator().findNearestMapStructure(ow, net.minecraft.core.HolderSet.direct(holder.get()), from, 100, false);
        if (found == null) {
            return fail(src, "No shrine found within 100 chunks.");
        }
        BlockPos p = found.getFirst();
        int y = ow.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p.getX(), p.getZ());
        player.teleportTo(ow, p.getX() + 0.5, y, p.getZ() + 0.5, player.getYRot(), player.getXRot());
        say(src, "Abandoned Office Shrine at " + p.getX() + " " + y + " " + p.getZ());
        return 1;
    }

    private static int gotoType(CommandContext<CommandSourceStack> ctx, String typeId) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        if (java.util.List.of("fuse_box", "hazard_zone", "outpost", "shrine").contains(typeId)) {
            return gotoSpecial(ctx, typeId);
        }
        if ("atrium".equals(typeId)) {
            return window(ctx, 0); // Atrien: über ein Fenster ansteuern (die Raumsuche schließt sie aus)
        }
        CommandSourceStack src = ctx.getSource();
        ServerPlayer player = src.getPlayerOrException();
        ServerLevel level = officeLevel(src);
        BlockSampler s = sampler(level);
        if (s == null) {
            return fail(src, "Office Rooms generator not active.");
        }
        OfficePlanner p = s.planner();
        if (typeId.equals("poolrooms") || typeId.equals("poolrooms_maze")) {
            BlockPos from = player.level() == level ? player.blockPosition() : BlockPos.ZERO;
            int rx0 = p.regionOf(from.getX());
            int rz0 = p.regionOf(from.getZ());
            de.officerooms.megastructure.Poolrooms.Site best = null;
            for (int r = 0; r <= 40 && best == null; r++) {
                for (int dx = -r; dx <= r && best == null; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                            continue;
                        }
                        de.officerooms.megastructure.Poolrooms.Site site = p.pools.site(rx0 + dx, rz0 + dz);
                        if (site != null) {
                            best = site;
                            break;
                        }
                    }
                }
            }
            if (best == null) {
                return fail(src, "No Poolrooms within 40 regions.");
            }
            boolean maze = typeId.equals("poolrooms_maze") && best.entranceX >= 0;
            int tx = best.rx * p.regionBlocks + (maze ? best.entranceX + 3 : best.hallX);
            int tz = best.rz * p.regionBlocks + (maze ? best.entranceZ + 1 : best.hallZ);
            int ty = best.yBase + de.officerooms.megastructure.Poolrooms.FLOOR + 1;
            player.teleportTo(level, tx + 0.5, ty, tz + 0.5, player.getYRot(), player.getXRot());
            say(src, "Room type poolrooms at " + tx + " " + ty + " " + tz + (maze ? " (maze entrance)" : ""));
            return 1;
        }
        if (java.util.List.of("hotel", "hotel_vault", "industrial", "level_fun", "cinema", "hideout", "elevator", "secure_storage").contains(typeId) || hideoutFilter(typeId) >= 0) {
            BlockPos from = player.level() == level ? player.blockPosition() : BlockPos.ZERO;
            java.util.List<Hit> hits = new java.util.ArrayList<>();
            megaHits(p, typeId, from, 30, hits);
            if (hits.isEmpty()) {
                return fail(src, "No " + typeId + " found nearby.");
            }
            hits.sort(java.util.Comparator.comparingDouble(Hit::dist));
            Hit h = hits.get(0);
            BlockPos t = safeSpot(level, new BlockPos(h.x(), h.y(), h.z()));
            player.teleportTo(level, t.getX() + 0.5, t.getY(), t.getZ() + 0.5, player.getYRot(), player.getXRot());
            say(src, h.what() + " at " + t.getX() + " " + t.getY() + " " + t.getZ());
            return 1;
        }
        if (typeId.equals("neighbourhood") || typeId.equals("maze") || typeId.equals("maze_vault")) {
            BlockPos from = player.level() == level ? player.blockPosition() : BlockPos.ZERO;
            int rx0 = p.regionOf(from.getX());
            int rz0 = p.regionOf(from.getZ());
            boolean hood = typeId.equals("neighbourhood");
            for (int r = 0; r <= 60; r++) {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                            continue;
                        }
                        int rx = rx0 + dx;
                        int rz = rz0 + dz;
                        int baseX = rx * p.regionBlocks;
                        int baseZ = rz * p.regionBlocks;
                        int tx;
                        int ty;
                        int tz;
                        String what;
                        if (hood) {
                            de.officerooms.megastructure.Neighbourhood.Site site = p.hood.site(rx, rz);
                            if (site == null) {
                                continue;
                            }
                            // on the street in front of the first house
                            tx = baseX + de.officerooms.megastructure.Neighbourhood.houseX(0) + 6;
                            tz = baseZ + de.officerooms.megastructure.Neighbourhood.houseZ(0) - 8;
                            ty = site.yBase + 1;
                            what = "Neighbourhood (storeys " + site.s0 + "-" + (site.s0 + site.storeys - 1) + ")";
                        } else {
                            de.officerooms.megastructure.Maze.Site site = p.maze.site(rx, rz);
                            if (site == null) {
                                continue;
                            }
                            ty = site.yBase + 1;
                            if (typeId.equals("maze_vault") && site.vault != null) {
                                tx = baseX + site.vault.doorX;
                                tz = baseZ + site.vault.doorZ - 3;
                                ty = site.yBase + site.vault.walkOff();
                                what = "Maze vault door (level " + site.vault.level + ", maze floor " + (site.vault.floor + 1) + ")";
                            } else {
                                de.officerooms.megastructure.Maze.Exit e = site.exits.isEmpty() ? null : site.exits.get(0);
                                tx = baseX + (e == null ? 5 : e.west() ? 4 : e.lx());
                                tz = baseZ + (e == null ? 5 : e.west() ? e.lz() : 4);
                                what = "The Maze (storey " + site.s0 + ", " + site.exits.size() + " exits)";
                            }
                        }
                        player.teleportTo(level, tx + 0.5, ty, tz + 0.5, player.getYRot(), player.getXRot());
                        say(src, what + " at " + tx + " " + ty + " " + tz);
                        return 1;
                    }
                }
            }
            return fail(src, "No " + (hood ? "Neighbourhood" : "Maze") + " within 60 regions.");
        }
        de.officerooms.furnish.RoomType type = de.officerooms.furnish.RoomType.byId(typeId);
        de.officerooms.furnish.FurnishingPlanner fp = ((OfficeChunkGenerator) level.getChunkSource().getGenerator()).furnishing(s);
        BlockPos pos = player.level() == level ? player.blockPosition() : new BlockPos(0, p.floorY(p.storeys / 2), 0);
        int rx = p.regionOf(pos.getX());
        int rz = p.regionOf(pos.getZ());
        int st = Math.max(0, Math.min(p.storeys - 1, p.storeyOf(pos.getY())));
        RegionLayout l = p.layout(rx, rz);
        int[] best = null;
        double bestD = Double.MAX_VALUE;
        if (type.isSpecial()) {
            // Spezialräume sind selten: 5 × 5 Regionen und alle Geschosse durchsuchen
            de.officerooms.special.SpecialType want = de.officerooms.special.SpecialType.valueOf(type.name());
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    RegionLayout ll = p.layout(rx + dx, rz + dz);
                    for (Room r : ll.rooms) {
                        if (de.officerooms.special.SpecialRooms.of(p, rx + dx, rz + dz, r) != want) {
                            continue;
                        }
                        int[] v = fp.room(rx + dx, rz + dz, r).view();
                        if (v == null) {
                            continue;
                        }
                        double d = Math.pow(v[0] - pos.getX(), 2) + Math.pow(v[2] - pos.getZ(), 2) + 25 * Math.pow(v[1] - pos.getY(), 2);
                        if (d < bestD) {
                            bestD = d;
                            best = v;
                        }
                    }
                }
            }
        }
        for (Room r : l.rooms) {
            if (type.isSpecial() || r.cells == 0 || r.sealed || r.kind == Room.Kind.STAIRS || r.kind == Room.Kind.ATRIUM || Math.abs(r.bottom - st) > 6) {
                continue;
            }
            de.officerooms.furnish.RoomFurnisher.Result res = fp.room(rx, rz, r);
            if (res.type() != type || res.view() == null) {
                continue;
            }
            int[] v = res.view();
            double d = Math.pow(v[0] - pos.getX(), 2) + Math.pow(v[2] - pos.getZ(), 2) + 25 * Math.pow(v[1] - pos.getY(), 2);
            if (d < bestD) {
                bestD = d;
                best = v;
            }
        }
        if (best == null) {
            return fail(src, "No room of type " + typeId + " nearby.");
        }
        float yaw = (float) Math.toDegrees(Math.atan2(-(best[3] - best[0]), best[4] - best[2]));
        player.teleportTo(level, best[0] + 0.5, best[1], best[2] + 0.5, yaw, 15f);
        say(src, "Room type " + typeId + " at " + best[0] + " " + best[1] + " " + best[2]);
        return 1;
    }

    /** Nearest spot (same height first, then ±1) with two free blocks and solid ground, searching up to 6 blocks around. */
    private static BlockPos safeSpot(ServerLevel level, BlockPos c) {
        level.getChunk(c);
        for (int r = 0; r <= 6; r++) {
            for (int dy : new int[] {0, 1, -1}) {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                            continue;
                        }
                        BlockPos q = c.offset(dx, dy, dz);
                        if (level.getBlockState(q).getCollisionShape(level, q).isEmpty()
                                && level.getBlockState(q.above()).getCollisionShape(level, q.above()).isEmpty()
                                && !level.getBlockState(q.below()).getCollisionShape(level, q.below()).isEmpty()) {
                            return q;
                        }
                    }
                }
            }
        }
        return c;
    }

    // --- 0.8.4: /officerooms locate <type|all> - list coordinates without teleporting -------------

    private static final java.util.List<String> LOCATE_TYPES = java.util.List.of("all", "poolrooms", "neighbourhood", "maze",
            "maze_vault", "hotel", "hotel_vault", "industrial", "level_fun", "cinema", "elevator", "secure_storage", "hideout", "villager_outpost", "lush_cave", "water_office", "sculk_office", "abandoned_camp", "oasis", "exit_room",
            "maintenance_room", "supply_closet", "forgotten_break_room", "security_office", "research_lab", "concealed_room", "outpost", "shrine");

    private record Hit(String what, int x, int y, int z, int storey, double dist) {
    }

    private static int locate(CommandContext<CommandSourceStack> ctx, String typeId) {
        CommandSourceStack src = ctx.getSource();
        if (typeId.equals("outpost")) {               // 0.41.0: research outposts generated so far (all dimensions)
            java.util.List<String> lines = new java.util.ArrayList<>();
            java.util.List<Double> dist = new java.util.ArrayList<>();
            for (ServerLevel l : src.getServer().getAllLevels()) {
                for (var e : de.officerooms.outpost.OutpostRegistry.of(l).entries) {
                    double d = l == src.getLevel() ? Math.sqrt(e.pos().distSqr(BlockPos.containing(src.getPosition()))) : 1e9;
                    dist.add(d);
                    lines.add("  Research outpost (" + e.theme() + "): " + e.pos().getX() + " " + e.pos().getY() + " " + e.pos().getZ() + " in "
                            + l.dimension().location() + (d < 1e9 ? "  (" + Math.round(d) + " blocks)" : ""));
                }
            }
            if (lines.isEmpty()) {
                return fail(src, "No research outpost generated yet. They appear in new chunks (ancient cities, jungle temples, deep caves, Nether, rarely on the surface and in deep oceans).");
            }
            Integer[] idx = new Integer[lines.size()];
            for (int i = 0; i < idx.length; i++) {
                idx[i] = i;
            }
            java.util.Arrays.sort(idx, java.util.Comparator.comparingDouble(dist::get));
            say(src, "Research outposts generated so far (" + lines.size() + "):");
            for (int i = 0; i < Math.min(8, idx.length); i++) {
                say(src, lines.get(idx[i]));
            }
            return lines.size();
        }
        if (typeId.equals("shrine")) {                // 0.41.0: same search as /locate structure office_rooms:abandoned_office_shrine
            ServerLevel ow = src.getServer().overworld();
            var holder = ow.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                    .getHolder(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE, new ResourceLocation("office_rooms", "abandoned_office_shrine")));
            BlockPos from = src.getLevel() == ow ? BlockPos.containing(src.getPosition()) : ow.getSharedSpawnPos();
            var found = holder.isEmpty() ? null : ow.getChunkSource().getGenerator().findNearestMapStructure(ow, net.minecraft.core.HolderSet.direct(holder.get()), from, 100, false);
            if (found == null) {
                return fail(src, "No shrine found within 100 chunks.");
            }
            BlockPos q = found.getFirst();
            say(src, "Abandoned Office Shrine (Overworld): " + q.getX() + " ~ " + q.getZ() + "  (" + Math.round(Math.sqrt(q.distSqr(new BlockPos(from.getX(), q.getY(), from.getZ())))) + " blocks)");
            return 1;
        }
        ServerLevel level = officeLevel(src);
        BlockSampler s = sampler(level);
        if (s == null) {
            return fail(src, "Office Rooms generator not active.");
        }
        if (!LOCATE_TYPES.contains(typeId)) {
            return fail(src, "Unknown type " + typeId + ". Use: " + String.join(", ", LOCATE_TYPES));
        }
        OfficePlanner p = s.planner();
        BlockPos from = src.getLevel() == level ? BlockPos.containing(src.getPosition()) : BlockPos.ZERO;
        boolean all = typeId.equals("all");
        java.util.List<String> types = all ? LOCATE_TYPES.subList(1, LOCATE_TYPES.size() - 2) : java.util.List.of(typeId);
        say(src, "Office Rooms locate (from " + from.getX() + " " + from.getY() + " " + from.getZ() + "):");
        int total = 0;
        for (String t : types) {
            java.util.List<Hit> hits = new java.util.ArrayList<>();
            boolean mega = t.equals("poolrooms") || t.equals("neighbourhood") || t.equals("maze") || t.equals("maze_vault")
                    || t.equals("hotel") || t.equals("hotel_vault") || t.equals("industrial") || t.equals("level_fun") || t.equals("cinema")
                    || t.equals("elevator") || t.equals("secure_storage") || t.equals("hideout") || hideoutFilter(t) >= 0;
            if (mega) {
                megaHits(p, t, from, all ? 12 : 30, hits);
            } else {
                specialHits(p, level, s, t, from, hits, all ? 1 : 2);   // 0.41.0: "all" plans fewer regions (server watchdog)
            }
            hits.sort(java.util.Comparator.comparingDouble(Hit::dist));
            int n = all ? 1 : 5;
            if (hits.isEmpty()) {
                say(src, "  " + t + ": none " + (mega ? "within " + (all ? 12 : 30) + " regions" : "within " + (all ? 1 : 2) + " regions"));
                continue;
            }
            for (int i = 0; i < Math.min(n, hits.size()); i++) {
                Hit h = hits.get(i);
                say(src, "  " + h.what() + ": " + h.x() + " " + h.y() + " " + h.z() + "  (storey " + h.storey() + ", "
                        + Math.round(h.dist()) + " blocks)");
                total++;
            }
            if (!all && hits.size() > n) {
                say(src, "  ... " + (hits.size() - n) + " more in the search area");
            }
        }
        say(src, "Teleport with /officerooms goto <type> or /tp.");
        return Math.max(1, total);
    }

    private static void megaHits(OfficePlanner p, String t, BlockPos from, int radius, java.util.List<Hit> out) {
        int rx0 = p.regionOf(from.getX());
        int rz0 = p.regionOf(from.getZ());
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int rx = rx0 + dx;
                int rz = rz0 + dz;
                int bx = rx * p.regionBlocks;
                int bz = rz * p.regionBlocks;
                int x;
                int y;
                int z;
                int st;
                String what = t;
                if (t.equals("elevator") || t.equals("secure_storage") || t.equals("hideout") || hideoutFilter(t) >= 0) {
                    de.officerooms.room.Extras.Region ex = Math.max(Math.abs(dx), Math.abs(dz)) <= (radius < 30 ? 1 : hideoutFilter(t) >= 0 ? 10 : 3) ? p.extras.of(rx, rz) : null;
                    if (ex == null) {
                        continue;
                    }
                    int ps = p.storeyOf(from.getY());
                    if (t.equals("elevator")) {
                        if (!ex.hasElevator()) {
                            continue;
                        }
                        int stop = ex.isStop(ps) ? ps : Math.max(ex.nextStop(ps, 1, 1), ex.nextStop(ps, -1, 1));
                        int ey = p.floorY(Math.max(stop, 0)) + 1;
                        int ex1 = bx + ex.ex0 + 2;
                        int ez1 = bz + ex.ez0 + 2;
                        out.add(new Hit("Elevator", ex1, ey, ez1, stop, Math.sqrt(Math.pow(ex1 - from.getX(), 2) + Math.pow(ez1 - from.getZ(), 2))));
                    } else if (t.equals("hideout") || hideoutFilter(t) >= 0) {
                        for (de.officerooms.room.Extras.Hideout h : ex.hideouts) {
                            if (hideoutFilter(t) >= 0 && h.type() != hideoutFilter(t)) {
                                continue;
                            }
                            int hx = bx + h.doorX();
                            int hz = bz + h.doorZ() + 1;
                            int hy = p.floorY(h.s()) + 1;
                            out.add(new Hit("Hidden " + de.officerooms.room.Extras.Hideout.NAMES[h.type()] + (h.empty() ? " (empty)" : ""), hx, hy, hz, h.s(),
                                    Math.sqrt(Math.pow(hx - from.getX(), 2) + Math.pow(hz - from.getZ(), 2) + Math.pow(hy - from.getY(), 2))));
                        }
                    } else {
                        for (de.officerooms.room.Extras.Vault v : ex.vaults) {
                            int vx = bx + v.x0() + 2;
                            int vz = bz + v.z0() + 5;
                            int vy = p.floorY(v.s()) + 1;
                            out.add(new Hit("Secure storage (level " + v.level() + ")", vx, vy, vz, v.s(),
                                    Math.sqrt(Math.pow(vx - from.getX(), 2) + Math.pow(vz - from.getZ(), 2) + Math.pow(vy - from.getY(), 2))));
                        }
                    }
                    continue;
                }
                switch (t) {
                    case "poolrooms" -> {
                        de.officerooms.megastructure.Poolrooms.Site site = p.pools.site(rx, rz);
                        if (site == null) {
                            continue;
                        }
                        x = bx + site.hallX;
                        z = bz + site.hallZ;
                        y = site.yBase + de.officerooms.megastructure.Poolrooms.FLOOR + 1;
                        st = site.s0;
                        what = "Poolrooms";
                    }
                    case "cinema" -> {
                        de.officerooms.megastructure.Cinema.Site site = p.cinema.site(rx, rz);
                        if (site == null) {
                            continue;
                        }
                        x = bx + 80;
                        z = bz + 84;
                        y = site.yBase + 1;
                        st = site.s0;
                        what = "Cinema";
                    }
                    case "level_fun" -> {
                        de.officerooms.megastructure.LevelFun.Site site = p.fun.site(rx, rz);
                        if (site == null) {
                            continue;
                        }
                        x = bx + 80;
                        z = bz + 80;
                        y = site.yBase + 1;
                        st = site.s0;
                        what = "Level Fun =)";
                    }
                    case "industrial" -> {
                        de.officerooms.megastructure.Industrial.Site site = p.industrial.site(rx, rz);
                        if (site == null) {
                            continue;
                        }
                        int[] hc = site.halls[0];
                        x = bx + hc[0] * 16 + 4;
                        z = bz + hc[1] * 16 + 4;
                        y = site.yBase + 1;
                        st = site.s0;
                        what = "Industrial Complex (generator hall)";
                    }
                    case "hotel", "hotel_vault" -> {
                        de.officerooms.megastructure.Hotel.Site site = p.hotel.site(rx, rz);
                        if (site == null) {
                            continue;
                        }
                        st = site.s0 + (t.equals("hotel") ? 1 : 0);
                        if (t.equals("hotel")) {
                            x = bx + 72;
                            z = bz + 68;
                            y = site.yBase + 7;
                            what = "Abandoned Hotel (lobby)";
                        } else {
                            x = bx + site.vx + de.officerooms.megastructure.Hotel.VS / 2;
                            z = bz + site.vz + de.officerooms.megastructure.Hotel.VS;
                            y = site.yBase + 1;
                            what = "Hotel vault door (level " + site.vaultLevel + ")";
                        }
                    }
                    case "neighbourhood" -> {
                        de.officerooms.megastructure.Neighbourhood.Site site = p.hood.site(rx, rz);
                        if (site == null) {
                            continue;
                        }
                        x = bx + 80;
                        z = bz + 80;
                        y = site.yBase + 1;
                        st = site.s0;
                        what = "Neighbourhood";
                    }
                    default -> {
                        de.officerooms.megastructure.Maze.Site site = p.maze.site(rx, rz);
                        if (site == null) {
                            continue;
                        }
                        y = site.yBase + 1;
                        st = site.s0;
                        if (t.equals("maze_vault") && site.vault != null) {
                            x = bx + site.vault.doorX;
                            z = bz + site.vault.doorZ - 1;
                            y = site.yBase + site.vault.walkOff();
                            what = "Maze vault door (level " + site.vault.level + ", maze floor " + (site.vault.floor + 1) + ")";
                        } else {
                            x = bx + 80;
                            z = bz + 80;
                            what = "The Maze (" + site.exits.size() + " exits)";
                        }
                    }
                }
                double d = Math.sqrt(Math.pow(x - from.getX(), 2) + Math.pow(z - from.getZ(), 2) + Math.pow(y - from.getY(), 2));
                out.add(new Hit(what, x, y, z, st, d));
            }
        }
    }

    private static void specialHits(OfficePlanner p, ServerLevel level, BlockSampler s, String t, BlockPos from, java.util.List<Hit> out, int rad) {
        de.officerooms.special.SpecialType want = de.officerooms.special.SpecialType.valueOf(t.toUpperCase(java.util.Locale.ROOT));
        de.officerooms.furnish.FurnishingPlanner fp = ((OfficeChunkGenerator) level.getChunkSource().getGenerator()).furnishing(s);
        int rx0 = p.regionOf(from.getX());
        int rz0 = p.regionOf(from.getZ());
        for (int dx = -rad; dx <= rad; dx++) {
            for (int dz = -rad; dz <= rad; dz++) {
                RegionLayout ll = p.layout(rx0 + dx, rz0 + dz);
                for (Room r : ll.rooms) {
                    if (de.officerooms.special.SpecialRooms.of(p, rx0 + dx, rz0 + dz, r) != want) {
                        continue;
                    }
                    int[] v = fp.room(rx0 + dx, rz0 + dz, r).view();
                    if (v == null) {
                        continue;
                    }
                    double d = Math.sqrt(Math.pow(v[0] - from.getX(), 2) + Math.pow(v[2] - from.getZ(), 2) + Math.pow(v[1] - from.getY(), 2));
                    out.add(new Hit(t, v[0], v[1], v[2], r.bottom, d));
                }
            }
        }
    }

    /** Diagnose: Mobs in der Nähe, KI-Zustand und Verhältnis Räume / Mobs. */
    private static int mobs(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        ServerLevel level = src.getLevel();
        BlockSampler s = sampler(level);
        net.minecraft.world.phys.Vec3 pos = src.getPosition();
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(pos, pos).inflate(64, 12, 64);
        int spiders = level.getEntitiesOfClass(de.officerooms.entity.Wolfspider.class, box).size();
        say(src, "Wolfspiders within 64/±12: " + spiders);
        if (s != null) {
            int rooms = de.officerooms.entity.MobSpawner.roomsAround(s.planner(), (int) pos.x, (int) pos.y, (int) pos.z, 48, 2).size();
            int near = level.getEntitiesOfClass(de.officerooms.entity.Wolfspider.class,
                    new net.minecraft.world.phys.AABB(pos.x - 48, s.planner().floorY(s.planner().storeyOf((int) pos.y) - 2), pos.z - 48,
                            pos.x + 49, s.planner().floorY(s.planner().storeyOf((int) pos.y) + 3), pos.z + 49)).size();
            say(src, "Spawn area (±48, ±2 storeys): " + rooms + " rooms, " + near + " wolfspiders"
                    + (near > 0 ? String.format(" -> 1 per %.1f rooms", rooms / (double) near) : ""));
        }
        for (de.officerooms.entity.HeadlessWarden w : level.getEntitiesOfClass(de.officerooms.entity.HeadlessWarden.class, box.inflate(64, 40, 64))) {
            say(src, String.format("Headless Warden %d at %.1f %.1f %.1f, distance %.1f, target %s, hunting %s, noise %s",
                    w.getId(), w.getX(), w.getY(), w.getZ(), Math.sqrt(w.distanceToSqr(pos)),
                    w.getTarget() == null ? "-" : w.getTarget().getName().getString(), w.isChasing(),
                    w.noiseTarget() == null ? "-" : String.format("%.1f %.1f %.1f", w.noiseTarget().x, w.noiseTarget().y, w.noiseTarget().z)));
        }
        return 1;
    }

    /** Diagnose: erkannte Möbel-Mods und tatsächliche Belegung der OfficeCraft-Möbel je Ausrichtung. */
    private static int furniture(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        say(src, "OfficeCraft: " + de.officerooms.compat.OfficeCraftCompat.isAvailable()
                + ", Refurbished: " + net.minecraftforge.fml.ModList.get().isLoaded("refurbished_furniture"));
        if (!de.officerooms.compat.OfficeCraftCompat.isAvailable()) {
            return 1;
        }
        for (net.minecraft.world.level.block.Block b : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
            net.minecraft.resources.ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b);
            if (!id.getNamespace().equals(de.officerooms.compat.OfficeCraftCompat.MOD_ID)) {
                continue;
            }
            StringBuilder sb = new StringBuilder(id.getPath()).append(" props=").append(b.getStateDefinition().getProperties());
            for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                java.util.List<BlockPos> cells = de.officerooms.compat.OfficeCraftCompat.cells(id.getPath(), d);
                sb.append(" | ").append(d.getName()).append(':');
                for (BlockPos c : cells) {
                    sb.append('(').append(c.getX()).append(',').append(c.getY()).append(',').append(c.getZ()).append(')');
                }
            }
            OfficeRooms.LOGGER.info("[Furniture] {}", sb);
        }
        say(src, "Block property dump written to the log.");
        return 1;
    }

    private static int verify(CommandContext<CommandSourceStack> ctx, int radius) {
        CommandSourceStack src = ctx.getSource();
        ServerLevel level = src.getLevel();
        BlockSampler s = sampler(level);
        if (s == null) {
            return fail(src, "Not in the Office Rooms dimension (e.g. 'execute in office_rooms:office_rooms run ...').");
        }
        OfficePlanner p = s.planner();
        BlockPos pos = BlockPos.containing(src.getPosition());
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        long t0 = System.currentTimeMillis();
        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                level.getChunk(x, z);
            }
        }
        long t1 = System.currentTimeMillis();
        Map<BlockState, Integer> reverse = OfficeChunkGenerator.reverseStates();
        PlanVerifier v = new PlanVerifier(p);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        PlanVerifier.KindSource world = (x, y, z) -> {
            ChunkAccess c = level.getChunk(x >> 4, z >> 4);
            BlockState bs = c.getBlockState(m.set(x, y, z));
            Integer k = reverse.get(bs);
            if (k == null && !bs.getFluidState().isEmpty() && bs.getFluidState().is(net.minecraft.tags.FluidTags.WATER)) {
                return PlanVerifier.WATER;
            }
            return k == null ? -1 : k;
        };
        int x0 = (cx - radius) << 4;
        int z0 = (cz - radius) << 4;
        int x1 = ((cx + radius) << 4) + 15;
        int z1 = ((cz + radius) << 4) + 15;
        PlanVerifier.Result r = v.verify(world, x0, z0, x1, z1, level.getMinBuildHeight(), level.getMaxBuildHeight() - 1, true);
        long t2 = System.currentTimeMillis();
        say(src, "Verification " + (2 * radius + 1) + "x" + (2 * radius + 1) + " Chunks (X " + x0 + ".." + x1 + ", Z " + z0 + ".." + z1
                + "): " + r + ", load/generate " + (t1 - t0) + " ms, check " + (t2 - t1) + " ms");
        for (String e : r.errors) {
            say(src, "  " + e);
        }
        // Teppiche: jeder Teppich muss auf einem Block mit voller Oberseite liegen (nie schwebend, nie überstehend)
        long carpets = 0;
        long badCarpets = 0;
        BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos();
        for (int ccx = cx - radius; ccx <= cx + radius; ccx++) {
            for (int ccz = cz - radius; ccz <= cz + radius; ccz++) {
                ChunkAccess c = level.getChunk(ccx, ccz);
                net.minecraft.world.level.chunk.LevelChunkSection[] secs = c.getSections();
                for (int si = 0; si < secs.length; si++) {
                    if (!secs[si].maybeHas(st -> st.getBlock() instanceof net.minecraft.world.level.block.CarpetBlock)) {
                        continue;
                    }
                    int sy = c.getSectionYFromSectionIndex(si) << 4;
                    for (int lx = 0; lx < 16; lx++) {
                        for (int ly = 0; ly < 16; ly++) {
                            for (int lz = 0; lz < 16; lz++) {
                                BlockState st = secs[si].getBlockState(lx, ly, lz);
                                if (!(st.getBlock() instanceof net.minecraft.world.level.block.CarpetBlock)) {
                                    continue;
                                }
                                carpets++;
                                q.set((ccx << 4) + lx, sy + ly - 1, (ccz << 4) + lz);
                                if (!level.getBlockState(q).isFaceSturdy(level, q, net.minecraft.core.Direction.UP)) {
                                    badCarpets++;
                                    if (badCarpets <= 5) {
                                        say(src, "  Carpet without full block below at " + q.getX() + " " + (q.getY() + 1) + " " + q.getZ()
                                                + " (below: " + level.getBlockState(q).getBlock() + ")");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        say(src, "Carpets: " + carpets + ", faulty: " + badCarpets);
        boolean ok = r.ok() && badCarpets == 0;
        say(src, ok ? "RESULT: OK" : "RESULT: ERRORS");
        return ok ? 1 : 0;
    }
}
