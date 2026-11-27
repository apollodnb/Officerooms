package de.officerooms.megastructure;

import de.officerooms.OfficeRooms;
import de.officerooms.block.ModBlocks;
import de.officerooms.block.SecurityDoorBlock;
import de.officerooms.core.OfficeRoomsIds;
import de.officerooms.entity.ModEntities;
import de.officerooms.room.OfficePlanner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * 0.8.0: Block entities and residents of the Neighbourhood and The Maze, set in the FEATURES stage
 * (chests with loot tables, the creatures in every third house, the security door, its sign and the
 * vault chest).
 */
public final class MegaDecorator {

    private MegaDecorator() {
    }

    public static void decorate(OfficePlanner p, WorldGenLevel level, ChunkAccess chunk) {
        int bx = chunk.getPos().getMinBlockX();
        int bz = chunk.getPos().getMinBlockZ();
        int rx = p.regionOf(bx);
        int rz = p.regionOf(bz);
        Neighbourhood.Site hood = p.hood.site(rx, rz);
        if (hood != null) {
            neighbourhood(p, hood, level, chunk, rx, rz);
        }
        de.officerooms.room.Extras.Region ex = p.extras.of(rx, rz);
        if (ex != null) {
            for (de.officerooms.room.Extras.Vault v : ex.vaults) {
                storage(p, v, level, chunk, rx, rz);
            }
            for (de.officerooms.room.Extras.Hideout h : ex.hideouts) {
                de.officerooms.special.Hideouts.decorate(p, h, level, chunk, rx * p.regionBlocks, rz * p.regionBlocks);
            }
        }
        Maze.Site maze = p.maze.site(rx, rz);
        if (maze != null) {
            maze(p, maze, level, chunk, rx, rz);
        }
        Hotel.Site hotel = p.hotel.site(rx, rz);
        if (hotel != null) {
            HotelBlocks.decorate(p, hotel, level, chunk, rx * p.regionBlocks, rz * p.regionBlocks);
        }
        LevelFun.Site fun = p.fun.site(rx, rz);
        if (fun != null) {
            LevelFunBlocks.decorate(p, fun, level, chunk, rx * p.regionBlocks, rz * p.regionBlocks);
        }
        Mall.Site mall = p.mall.site(rx, rz);
        if (mall != null) {
            MallDecorator.decorate(p, mall, level, chunk);
        }
        Cinema.Site cin = p.cinema.site(rx, rz);
        if (cin != null) {
            CinemaBlocks.decorate(p, cin, level, chunk, rx * p.regionBlocks, rz * p.regionBlocks);
        }
        Industrial.Site ind = p.industrial.site(rx, rz);
        if (ind != null) {
            IndustrialBlocks.decorate(p, ind, level, chunk, rx * p.regionBlocks, rz * p.regionBlocks);
        }
    }

    static boolean chunkHas(ChunkAccess chunk, int x, int z) {
        return inChunk(chunk, x, z);
    }

    /** 0.19.0: the first house with a chest holds the Neighbourhood artifact. */
    static int firstChestHouse() {
        for (int h = 0; h < Neighbourhood.houseCount(); h++) {
            if (!Neighbourhood.hasResident(h)) {
                return h;
            }
        }
        return -1;
    }

    static void lootChest(OfficePlanner p, WorldGenLevel level, BlockPos pos, Direction facing, String table) {
        chest(p, level, pos, facing, table);
    }

    private static boolean inChunk(ChunkAccess chunk, int x, int z) {
        return (x >> 4) == chunk.getPos().x && (z >> 4) == chunk.getPos().z;
    }

    private static void chest(OfficePlanner p, WorldGenLevel level, BlockPos pos, Direction facing, String table) {
        level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), 2);
        if (level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity be) {
            be.setLootTable(ResourceLocation.fromNamespaceAndPath(OfficeRoomsIds.MOD_ID, "chests/" + table), p.seed ^ pos.asLong());
        }
    }

    private static void neighbourhood(OfficePlanner p, Neighbourhood.Site s, WorldGenLevel level, ChunkAccess chunk, int rx, int rz) {
        int baseX = rx * p.regionBlocks;
        int baseZ = rz * p.regionBlocks;
        if (s.modern) {
            MegaFurniture.swap(level, chunk, baseX, baseZ, s.yBase, s.h, (lx, off, lz) -> MegaFurniture.hood(s, lx, off, lz));
        }
        for (int h = 0; h < Neighbourhood.houseCount(); h++) {
            int hx = baseX + Neighbourhood.houseX(h);
            int hz = baseZ + Neighbourhood.houseZ(h);
            if (Neighbourhood.hasResident(h)) {
                int x = hx + Neighbourhood.MOB_HX + (s.park(h) ? 6 : 0);
                int z = hz + Neighbourhood.MOB_HZ;
                if (inChunk(chunk, x, z)) {
                    resident(level, p.hood.resident(s, h), x + 0.5, s.yBase + 1, z + 0.5);
                }
            } else {
                if (s.modern && s.hh(6, h) < 0.5 && inChunk(chunk, hx + 3, hz + 2)) {   // 0.36.0: a second chest upstairs
                    chest(p, level, new BlockPos(hx + 3, s.yBase + 6, hz + 2), Direction.SOUTH, "neighbourhood_house");
                }
                int x = hx + Neighbourhood.CHEST_HX;
                int z = hz + Neighbourhood.CHEST_HZ;
                if (inChunk(chunk, x, z)) {
                    chest(p, level, new BlockPos(x, s.yBase + 1, z), Direction.NORTH, h == firstChestHouse() ? "neighbourhood_house_artifact" : "neighbourhood_house");
                }
            }
        }
    }

    private static void resident(WorldGenLevel level, Neighbourhood.Resident r, double x, double y, double z) {
        EntityType<? extends Mob> type = switch (r) {
            case SMILER -> ModEntities.SMILER.get();
            case HEADLESS_WARDEN -> ModEntities.HEADLESS_WARDEN.get();
            case HOUND -> ModEntities.HOUND.get();
            case COLONY -> ModEntities.COLONY.get();
        };
        Mob m = de.officerooms.core.OfficeRoomsConfig.mobsAllowed(level.getLevel()) ? type.create(level.getLevel()) : null;   // 0.41.2
        if (m == null) {
            return;
        }
        m.moveTo(x, y, z, 180F, 0F);
        m.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(x, y, z)), MobSpawnType.STRUCTURE, null, null);
        m.setPersistenceRequired();
        m.addTag(OfficeRoomsIds.MOD_ID + ".neighbourhood");
        level.addFreshEntityWithPassengers(m);
    }

    private static void maze(OfficePlanner p, Maze.Site s, WorldGenLevel level, ChunkAccess chunk, int rx, int rz) {
        Maze.Vault v = s.vault;
        if (v == null) {
            return;
        }
        int baseX = rx * p.regionBlocks;
        int baseZ = rz * p.regionBlocks;
        int y = s.yBase + v.walkOff();                               // 0.34.0: the vault's own level
        int dx = baseX + v.doorX;
        int dz = baseZ + v.doorZ;
        if (inChunk(chunk, dx, dz)) {
            BlockState lower = ModBlocks.SECURITY_DOOR.get().defaultBlockState()
                    .setValue(DoorBlock.FACING, Direction.SOUTH)
                    .setValue(SecurityDoorBlock.LEVEL, v.level);
            level.setBlock(new BlockPos(dx, y, dz), lower, 2);
            level.setBlock(new BlockPos(dx, y + 1, dz), lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 2);
        }
        int sx = baseX + v.signX;
        int sz = baseZ + v.signZ;
        if (inChunk(chunk, sx, sz)) {
            sign(level, new BlockPos(sx, y + 1, sz), Direction.NORTH,
                    "SECURITY", "LEVEL " + v.level, "Access card", "required");
        }
        int cx = baseX + v.chestX;
        int cz = baseZ + v.chestZ;
        if (inChunk(chunk, cx, cz)) {
            chest(p, level, new BlockPos(cx, y, cz), Direction.NORTH, "maze_vault_artifact");
            OfficeRooms.LOGGER.debug("Maze vault (level {}) at {} {} {}", v.level, cx, y, cz);
        }
    }

    /** 0.9.0: secure storage room: security door, sign and chest by level. */
    private static void storage(OfficePlanner p, de.officerooms.room.Extras.Vault v, WorldGenLevel level, ChunkAccess chunk, int rx, int rz) {
        int baseX = rx * p.regionBlocks;
        int baseZ = rz * p.regionBlocks;
        int y = p.floorY(v.s()) + 1;
        int dx = baseX + v.x0() + 2;
        int dz = baseZ + v.z0() + 4;
        if (inChunk(chunk, dx, dz)) {
            BlockState lower = ModBlocks.SECURITY_DOOR.get().defaultBlockState()
                    .setValue(DoorBlock.FACING, Direction.NORTH)
                    .setValue(SecurityDoorBlock.LEVEL, v.level());
            level.setBlock(new BlockPos(dx, y, dz), lower, 2);
            level.setBlock(new BlockPos(dx, y + 1, dz), lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 2);
        }
        if (inChunk(chunk, dx + 1, dz + 1)) {
            sign(level, new BlockPos(dx + 1, y + 1, dz + 1), Direction.SOUTH, "SECURE", "STORAGE", "LEVEL " + v.level(), "Access card");
        }
        int cx = baseX + v.x0() + 2;
        int cz = baseZ + v.z0() + 1;
        if (inChunk(chunk, cx, cz)) {
            chest(p, level, new BlockPos(cx, y, cz), Direction.SOUTH, "secure_storage_" + v.level());
        }
        // 0.13.0: 40 % of the restricted-access rooms hold a Research Table
        if (inChunk(chunk, cx + 1, cz) && Math.floorMod(de.officerooms.core.Hashing.hash(p.seed, baseX + v.x0(), baseZ + v.z0(), v.s(), 0x9305), 10) < 4) {
            level.setBlock(new BlockPos(cx + 1, y, cz), ModBlocks.RESEARCH_TABLE.get().defaultBlockState()
                    .setValue(de.officerooms.block.DecorBlock.FACING, Direction.SOUTH), 2);
        }
        if (inChunk(chunk, cx - 1, cz)) {
            level.setBlock(new BlockPos(cx - 1, y, cz), Blocks.BARREL.defaultBlockState(), 2);
            level.setBlock(new BlockPos(cx - 1, y + 1, cz), ModBlocks.monitor(Direction.SOUTH, (cx * 31 + cz) % 6 == 0), 2);
            if (level.getBlockEntity(new BlockPos(cx - 1, y, cz)) instanceof RandomizableContainerBlockEntity be) {
                be.setLootTable(ResourceLocation.fromNamespaceAndPath(OfficeRoomsIds.MOD_ID, "chests/secure_storage_supplies"), p.seed ^ (cx * 31L + cz));
            }
        }
    }

    public static void sign(WorldGenLevel level, BlockPos pos, Direction facing, String... lines) {
        level.setBlock(pos, Blocks.DARK_OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, facing), 2);
        if (level.getBlockEntity(pos) instanceof SignBlockEntity sb) {
            SignText t = new SignText();
            for (int i = 0; i < 4 && i < lines.length; i++) {
                t = t.setMessage(i, Component.literal(lines[i]));
            }
            CompoundTag tag = sb.saveWithoutMetadata();
            SignText.DIRECT_CODEC.encodeStart(NbtOps.INSTANCE, t).result().ifPresent(n -> tag.put("front_text", n));
            sb.load(tag);
        }
    }
}
