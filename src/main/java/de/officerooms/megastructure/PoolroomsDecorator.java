package de.officerooms.megastructure;

import de.officerooms.core.OfficeRoomsIds;
import de.officerooms.room.OfficePlanner;
import de.officerooms.special.SpecialDecorator;
import de.officerooms.special.Stories;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;

/** Setzt die Block-Entities der Poolrooms (Truhen, Lesepulte) in der FEATURES-Stufe. */
public final class PoolroomsDecorator {

    private static final Direction[] DIR = {Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH};

    private PoolroomsDecorator() {
    }

    public static void decorate(OfficePlanner p, WorldGenLevel level, ChunkAccess chunk) {
        int bx = chunk.getPos().getMinBlockX();
        int bz = chunk.getPos().getMinBlockZ();
        int rx = p.regionOf(bx);
        int rz = p.regionOf(bz);
        Poolrooms.Site site = p.pools.site(rx, rz);
        if (site == null) {
            return;
        }
        int baseX = rx * p.regionBlocks;
        int baseZ = rz * p.regionBlocks;
        for (Poolrooms.Chest c : site.chests) {
            int x = baseX + c.lx();
            int z = baseZ + c.lz();
            if ((x >> 4) != chunk.getPos().x || (z >> 4) != chunk.getPos().z) {
                continue;
            }
            BlockPos pos = new BlockPos(x, site.yBase + c.off(), z);
            boolean wet = c.off() < Poolrooms.FLOOR;
            level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, DIR[c.facing() & 3])
                    .setValue(ChestBlock.WATERLOGGED, wet), 2);
            if (level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity be) {
                be.setLootTable(ResourceLocation.fromNamespaceAndPath(OfficeRoomsIds.MOD_ID, "chests/" + c.loot()),
                        p.seed ^ pos.asLong());
            }
        }
        for (Poolrooms.Lectern l : site.lecterns) {
            int x = baseX + l.lx();
            int z = baseZ + l.lz();
            if ((x >> 4) != chunk.getPos().x || (z >> 4) != chunk.getPos().z) {
                continue;
            }
            BlockPos pos = new BlockPos(x, site.yBase + Poolrooms.FLOOR + 1, z);
            level.setBlock(pos, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, DIR[l.facing() & 3])
                    .setValue(LecternBlock.HAS_BOOK, true), 2);
            if (level.getBlockEntity(pos) instanceof LecternBlockEntity le) {
                le.setBook(SpecialDecorator.bookStack(Stories.POOL[Math.floorMod(l.bookIndex(), Stories.POOL.length)]));
            }
        }
    }
}
