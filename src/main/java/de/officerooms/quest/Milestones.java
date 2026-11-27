package de.officerooms.quest;

import de.officerooms.dimension.OfficeRoomsDimension;
import de.officerooms.megastructure.Megastructures;
import de.officerooms.room.OfficePlanner;
import de.officerooms.worldgen.OfficeChunkGenerator;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

import java.util.Locale;

/**
 * 0.25.0: hidden milestone advancements ({@code office_rooms:milestones/...}). They have no display, so they never
 * show up in the advancement screen; quest mods (FTB Quests) check them for events that are not an item or a kill:
 * repairing a fuse box, activating an outpost gateway, research upgrades, finding a megastructure, Level 0.
 */
public final class Milestones {

    private Milestones() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(Milestones::onTick);
    }

    /** Grants {@code office_rooms:milestones/<path>} (no-op if already done or unknown). */
    public static void grant(ServerPlayer p, String path) {
        if (p == null || p.server == null) {
            return;
        }
        Advancement a = p.server.getAdvancements().getAdvancement(new ResourceLocation("office_rooms", "milestones/" + path));
        if (a != null && !p.getAdvancements().getOrStartProgress(a).isDone()) {
            p.getAdvancements().award(a, "done");
        }
    }

    private static void onTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p) || p.tickCount % 40 != 13 || p.isSpectator()) {
            return;
        }
        ServerLevel level = p.serverLevel();
        if (level.dimension() == OfficeRoomsDimension.LEVEL_KEY) {
            if (level.getChunkSource().getGenerator() instanceof OfficeChunkGenerator gen) {
                OfficePlanner pl = gen.sampler(level.getChunkSource().randomState(), level).planner();
                if (pl != null && pl.cfg.modern()) {
                    // 0.29.0: curiosity rooms
                    int rx = pl.regionOf(p.getBlockX());
                    int rz = pl.regionOf(p.getBlockZ());
                    int ux = Math.floorDiv(p.getBlockX() - rx * pl.regionBlocks, pl.unit);
                    int uz = Math.floorDiv(p.getBlockZ() - rz * pl.regionBlocks, pl.unit);
                    de.officerooms.special.SpecialType t = de.officerooms.special.SpecialRooms.at(pl, rx, rz, pl.storeyOf(p.getBlockY()), ux, uz);
                    switch (t) {
                        case COLD_STORAGE, LAST_MEETING, MAIL_ROOM, RAIN_ROOM, CANTEEN, ARCADE, DEPARTURE_GATE -> grant(p, "room_" + t.id);
                        default -> {
                        }
                    }
                }
                if (pl != null && pl.megaContains(p.getBlockX(), p.getBlockY(), p.getBlockZ())) {
                    Megastructures.Kind k = Megastructures.kindOf(pl, pl.regionOf(p.getBlockX()), pl.regionOf(p.getBlockZ()));
                    if (k != Megastructures.Kind.NONE) {
                        grant(p, "discover_" + k.name().toLowerCase(Locale.ROOT));
                    }
                    // 0.38.0: the top floor of the cinema and the top storey of the hotel
                    var cs = pl.cinema.at(p.getBlockX(), p.getBlockY(), p.getBlockZ());
                    if (cs != null && p.getBlockY() >= cs.yBase + 2 * de.officerooms.megastructure.Cinema.FH) {
                        grant(p, "cinema_top");
                    }
                    // 0.43.0: inside one of the closed department stores of the Dead Mall
                    var ms = pl.mall.at(p.getBlockX(), p.getBlockY(), p.getBlockZ());
                    if (ms != null) {
                        int lx = p.getBlockX() - ms.rx * pl.regionBlocks;
                        int lz = p.getBlockZ() - ms.rz * pl.regionBlocks;
                        int a = lx < de.officerooms.megastructure.Mall.C0 ? de.officerooms.megastructure.Mall.C0 - 1 - lx : lx - de.officerooms.megastructure.Mall.C1 - 1;
                        int b = lz < de.officerooms.megastructure.Mall.C0 ? de.officerooms.megastructure.Mall.C0 - 1 - lz : lz - de.officerooms.megastructure.Mall.C1 - 1;
                        if (a >= 22 && b >= 22 && a <= 59 && b <= 59) {
                            grant(p, "mall_dept");
                        }
                    }
                    var hs = pl.hotel.at(p.getBlockX(), p.getBlockY(), p.getBlockZ());
                    if (hs != null && hs.modern && pl.storeyOf(p.getBlockY()) >= hs.s0 + hs.storeys - 1) {
                        grant(p, "hotel_top");
                    }
                }
            }
        } else if (level.dimension() == de.officerooms.level0.Level0Events.LEVEL_KEY) {
            var below = level.getBlockState(p.blockPosition().below());
            if (below.getBlock() == de.officerooms.level0.Level0Blocks.MANILA_CARPET.get()) {
                grant(p, "level0_manila");
            }
        }
    }
}
