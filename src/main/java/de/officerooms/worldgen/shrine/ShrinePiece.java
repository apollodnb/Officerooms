package de.officerooms.worldgen.shrine;

import de.officerooms.block.CeilingLightBlock;
import de.officerooms.block.ModBlocks;
import de.officerooms.core.OfficeRoomsIds;
import de.officerooms.special.SpecialDecorator;
import de.officerooms.special.Stories;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Das Bauteil der Oberwelt-Ruine. Alles wird aus dem gespeicherten Seed berechnet (Hash je Position), damit
 * die Ruine über Chunkgrenzen hinweg identisch entsteht. Lokale Koordinaten 0..10, Boden auf Höhe F.
 *
 * <p>Teppiche liegen nur auf vollen Blöcken (glatter Quarz bzw. Moosblock) und nur, wenn darüber Luft ist.</p>
 */
public final class ShrinePiece extends StructurePiece {

    private static final int S = ShrineStructure.SIZE;
    private static final int F = 4;
    private static final int H = 12;
    private static final ResourceLocation LOOT = new ResourceLocation(OfficeRoomsIds.MOD_ID, "chests/office_shrine");

    private final long seed;

    public ShrinePiece(BlockPos origin, Direction facing, long seed) {
        super(ShrineStructures.SHRINE_PIECE.get(), 0,
                makeBoundingBox(origin.getX(), origin.getY() - F, origin.getZ(), facing, S, H, S));
        setOrientation(facing);
        this.seed = seed;
    }

    public ShrinePiece(CompoundTag tag) {
        super(ShrineStructures.SHRINE_PIECE.get(), tag);
        this.seed = tag.getLong("Seed");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putLong("Seed", seed);
    }

    /** Pseudozufall 0..1, nur abhängig von Seed, Position und Zweck. */
    private double h(int x, int z, int salt) {
        long v = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL) ^ (salt * 0x165667B19E3779F9L);
        v = (v ^ (v >>> 31)) * 0x7FB5D329728EA185L;
        v = (v ^ (v >>> 27)) * 0x81DADEF4BC2DD44DL;
        v ^= v >>> 33;
        return (v >>> 11) * 0x1.0p-53;
    }

    private boolean footprint(int x, int z) {
        if (x < 0 || z < 0 || x >= S || z >= S) {
            return false;
        }
        if (x < 8 && z < 8) {
            return true;
        }
        // abgebrochene Kante nach Süden/Osten
        return h(x, z, 1) > 0.45 && (x < 10 || z < 10);
    }

    private int wallHeight(int i, int salt) {
        if (i <= 3) {
            return 4;
        }
        return 1 + (int) (h(i, salt, 2) * 4);
    }

    private boolean altarSpot(int x, int z) {
        return x >= 4 && x <= 6 && z >= 3 && z <= 6;
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator gen, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        BlockState air = Blocks.AIR.defaultBlockState();
        // 1) Fundament, Freiraum und Boden
        for (int x = 0; x < S; x++) {
            for (int z = 0; z < S; z++) {
                if (!footprint(x, z)) {
                    continue;
                }
                for (int y = 0; y < F; y++) {
                    BlockState b = getBlock(level, x, y, z, box);
                    if (b.isAir() || b.canBeReplaced() || !b.getFluidState().isEmpty()) {
                        placeBlock(level, Blocks.DIRT.defaultBlockState(), x, y, z, box);
                    }
                }
                for (int y = F + 1; y < H; y++) {
                    placeBlock(level, air, x, y, z, box);
                }
                double r = h(x, z, 3);
                BlockState floor;
                if ((x == 5 && z == 5) || (x == 5 && z == 6) || (x == 4 && z == 5) || (x == 6 && z == 6)) {
                    floor = Blocks.SCULK.defaultBlockState();
                } else if (r < 0.18) {
                    floor = Blocks.MOSS_BLOCK.defaultBlockState();
                } else if (r < 0.26) {
                    floor = Blocks.COARSE_DIRT.defaultBlockState();
                } else {
                    floor = Blocks.SMOOTH_QUARTZ.defaultBlockState();
                }
                placeBlock(level, floor, x, F, z, box);
            }
        }
        // 2) Wände (Nord z=0 und West x=0), Eckpfeiler, Fenster, Durchgang
        for (int i = 0; i < S; i++) {
            if (footprint(i, 0)) {
                int hgt = (i == 6 || i == 7) ? 0 : (i == 3 ? Math.max(3, wallHeight(i, 7)) : wallHeight(i, 7));
                for (int y = 1; y <= hgt; y++) {
                    boolean window = i == 2 && (y == 2 || y == 3);
                    placeBlock(level, window ? (h(i, y, 4) < 0.5 ? Blocks.GLASS.defaultBlockState() : air)
                            : Blocks.QUARTZ_BLOCK.defaultBlockState(), i, F + y, 0, box);
                }
                topGrowth(level, box, i, F + hgt + 1, 0, hgt);
            }
            if (i > 0 && footprint(0, i)) {
                int hgt = i == 6 ? Math.max(3, wallHeight(i, 9)) : wallHeight(i, 9);
                for (int y = 1; y <= hgt; y++) {
                    placeBlock(level, Blocks.QUARTZ_BLOCK.defaultBlockState(), 0, F + y, i, box);
                }
                topGrowth(level, box, 0, F + hgt + 1, i, hgt);
            }
        }
        for (int y = 1; y <= 5; y++) {
            placeBlock(level, Blocks.QUARTZ_PILLAR.defaultBlockState(), 0, F + y, 0, box);
        }
        // 3) Rest der Decke mit ausgefallener Leuchte
        for (int x = 1; x <= 3; x++) {
            for (int z = 1; z <= 3; z++) {
                if ((x == 2 && z == 2) || h(x, z, 5) > 0.3) {
                    placeBlock(level, Blocks.SMOOTH_QUARTZ.defaultBlockState(), x, F + 5, z, box);
                }
            }
        }
        placeBlock(level, ModBlocks.CEILING_PANEL.get().defaultBlockState().setValue(CeilingLightBlock.LIT, false), 2, F + 4, 2, box);
        // 4) Spinnweben in der Ecke, Ranken an den Wänden
        placeBlock(level, Blocks.COBWEB.defaultBlockState(), 1, F + 3, 1, box);
        placeBlock(level, Blocks.COBWEB.defaultBlockState(), 1, F + 4, 2, box);
        placeBlock(level, Blocks.COBWEB.defaultBlockState(), 3, F + 4, 1, box);
        for (int i = 1; i < S; i++) {
            if (footprint(i, 0) && footprint(i, 1) && h(i, 0, 6) < 0.45 && i != 3) {
                int hgt = (i == 6 || i == 7) ? 0 : wallHeight(i, 7);
                for (int y = Math.max(2, hgt - 1); y <= hgt; y++) {
                    placeIfAir(level, box, Blocks.VINE.defaultBlockState().setValue(VineBlock.NORTH, true), i, F + y, 1);
                }
            }
            if (footprint(0, i) && footprint(1, i) && h(0, i, 6) < 0.45 && i != 6) {
                int hgt = wallHeight(i, 9);
                for (int y = Math.max(2, hgt - 1); y <= hgt; y++) {
                    placeIfAir(level, box, Blocks.VINE.defaultBlockState().setValue(VineBlock.WEST, true), 1, F + y, i);
                }
            }
        }
        // 5) Der Schrein: Lesepult mit Buch, Truhe mit den Portalzutaten, Kerzen, Schädel
        placeBlock(level, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.SOUTH)
                .setValue(LecternBlock.HAS_BOOK, true), 5, F + 1, 3, box);
        BlockPos lectern = worldPos(5, F + 1, 3);
        if (box.isInside(lectern) && level.getBlockEntity(lectern) instanceof LecternBlockEntity le) {
            le.setBook(SpecialDecorator.bookStack(Stories.SHRINE[(int) Math.floorMod(seed, Stories.SHRINE.length)]));
        }
        placeBlock(level, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH), 6, F + 1, 3, box);
        BlockPos chest = worldPos(6, F + 1, 3);
        if (box.isInside(chest) && level.getBlockEntity(chest) instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity c) {
            c.setLootTable(LOOT, seed ^ 0x5A17L);
        }
        placeBlock(level, Blocks.WHITE_CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true), 4, F + 1, 3, box);
        placeBlock(level, Blocks.WHITE_CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 2).setValue(CandleBlock.LIT, true), 7, F + 1, 3, box);
        placeBlock(level, Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, 7), 6, F + 1, 5, box);
        placeBlock(level, Blocks.SCULK_VEIN.defaultBlockState()
                .setValue(net.minecraft.world.level.block.MultifaceBlock.getFaceProperty(Direction.DOWN), true), 4, F + 1, 6, box);
        // 6) Warnschilder
        sign(level, box, 3, F + 2, 1, Direction.SOUTH, "Those who", "dare will", "never leave.", "");
        sign(level, box, 1, F + 2, 6, Direction.EAST, "Do not", "combine them.", "You won't", "survive.");
        // 7) Herabgefallene Deckenplatten und ein Schreibtisch
        int[][] fallen = {{2, 6}, {7, 7}, {3, 4}, {6, 1}};
        for (int[] f : fallen) {
            if (footprint(f[0], f[1]) && h(f[0], f[1], 8) < 0.8) {
                placeBlock(level, Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState(), f[0], F + 1, f[1], box);
            }
        }
        if (footprint(8, 1) && footprint(9, 1)) {
            placeBlock(level, Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP), 8, F + 1, 1, box);
            placeBlock(level, Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP), 9, F + 1, 1, box);
        }
        // 8) Bodenbelag und Bewuchs – nur auf vollen Blöcken mit Luft darüber
        for (int x = 0; x < S; x++) {
            for (int z = 0; z < S; z++) {
                if (!footprint(x, z) || altarSpot(x, z)) {
                    continue;
                }
                BlockState floor = getBlock(level, x, F, z, box);
                if (!getBlock(level, x, F + 1, z, box).isAir()) {
                    continue;
                }
                double r = h(x, z, 9);
                if (floor.is(Blocks.SMOOTH_QUARTZ) && r < 0.5) {
                    placeBlock(level, Blocks.GRAY_CARPET.defaultBlockState(), x, F + 1, z, box);
                } else if (floor.is(Blocks.MOSS_BLOCK)) {
                    placeBlock(level, r < 0.4 ? Blocks.MOSS_CARPET.defaultBlockState()
                            : r < 0.65 ? Blocks.GRASS.defaultBlockState() : r < 0.8 ? Blocks.FERN.defaultBlockState() : air, x, F + 1, z, box);
                } else if (floor.is(Blocks.COARSE_DIRT) && r < 0.3) {
                    placeBlock(level, Blocks.DEAD_BUSH.defaultBlockState(), x, F + 1, z, box);
                }
            }
        }
        // 9) Trümmer auf dem Gelände um die Ruine
        for (int x = 0; x < S; x++) {
            for (int z = 0; z < S; z++) {
                if (footprint(x, z) || h(x, z, 10) > 0.25) {
                    continue;
                }
                BlockPos wp = worldPos(x, 0, z);
                if (!box.isInside(new BlockPos(wp.getX(), box.minY(), wp.getZ()))) {
                    continue;
                }
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, wp.getX(), wp.getZ());
                BlockPos at = new BlockPos(wp.getX(), top, wp.getZ());
                BlockPos below = at.below();
                if (!box.isInside(at) || !level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
                        || !level.getBlockState(at).isAir()) {
                    continue;
                }
                level.setBlock(at, h(x, z, 11) < 0.5 ? Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState() : Blocks.QUARTZ_BLOCK.defaultBlockState(), 2);
            }
        }
    }

    private void topGrowth(WorldGenLevel level, BoundingBox box, int x, int y, int z, int hgt) {
        if (hgt <= 0) {
            return;
        }
        double r = h(x, z, 12);
        if (r < 0.35) {
            placeBlock(level, Blocks.AZALEA_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true), x, y, z, box);
        } else if (r < 0.5) {
            placeBlock(level, Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true), x, y, z, box);
        } else if (r < 0.7) {
            placeBlock(level, Blocks.MOSS_CARPET.defaultBlockState(), x, y, z, box);
        }
    }

    private void placeIfAir(WorldGenLevel level, BoundingBox box, BlockState s, int x, int y, int z) {
        if (getBlock(level, x, y, z, box).isAir()) {
            placeBlock(level, s, x, y, z, box);
        }
    }

    private BlockPos worldPos(int x, int y, int z) {
        return new BlockPos(getWorldX(x, z), getWorldY(y), getWorldZ(x, z));
    }

    private void sign(WorldGenLevel level, BoundingBox box, int x, int y, int z, Direction facing, String... lines) {
        placeBlock(level, Blocks.DARK_OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, facing), x, y, z, box);
        BlockPos p = worldPos(x, y, z);
        if (!box.isInside(p)) {
            return;
        }
        BlockEntity be = level.getBlockEntity(p);
        if (be instanceof SignBlockEntity s) {
            SignText t = new SignText();
            for (int i = 0; i < 4 && i < lines.length; i++) {
                t = t.setMessage(i, Component.literal(lines[i]));
            }
            CompoundTag tag = s.saveWithoutMetadata();
            SignText txt = t;
            SignText.DIRECT_CODEC.encodeStart(NbtOps.INSTANCE, txt).result().ifPresent(n -> tag.put("front_text", n));
            s.load(tag);
        }
    }
}
