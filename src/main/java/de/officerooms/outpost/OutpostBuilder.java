package de.officerooms.outpost;

import de.officerooms.block.DecorBlock;
import de.officerooms.block.ModBlocks;
import de.officerooms.megastructure.MegaDecorator;
import de.officerooms.special.SpecialDecorator;
import de.officerooms.special.Stories;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * 0.20.0: builds one abandoned research outpost. Local frame: u to the right, v backwards from the entrance wall
 * (v = 0), y up from the walking level. Interior u −6..6, v 1..11, y 0..5; the gateway stands at v = 10.
 */
public final class OutpostBuilder {

    public enum Theme { LAB, ANCIENT, JUNGLE, CAVE, NETHER, SURFACE, OCEAN }   // 0.39.0: rare field station on flat land / drilling rig over deep ocean

    private static final ResourceLocation LOOT = ResourceLocation.fromNamespaceAndPath("office_rooms", "chests/research_outpost");
    private final ServerLevel l;
    private final BlockPos o;
    private final Direction f, r, back;
    private final Theme th;
    private final RandomSource rnd;
    private final Controller ctl;

    private OutpostBuilder(ServerLevel l, BlockPos o, Direction f, Theme th, long seed) {
        this.l = l;
        this.o = o;
        this.f = f;
        this.r = f.getClockWise();
        this.back = f.getOpposite();
        this.th = th;
        this.rnd = RandomSource.create(seed);
        BlockPos lever = at(-6, 1, 3);
        l.setBlock(lever, OutpostBlocks.POWER_LEVER.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, r), 3);
        this.ctl = (Controller) l.getBlockEntity(lever);
    }

    /** Builds the outpost; {@code o} is the middle of the entrance at walking level, {@code f} points out of the door. */
    public static BlockPos build(ServerLevel l, BlockPos o, Direction f, Theme th, long seed) {
        OutpostBuilder b = new OutpostBuilder(l, o, f, th, seed);
        if (th == Theme.OCEAN) {
            b.rig();
        }
        b.shell();
        b.gateway();
        b.controls();
        b.furniture();
        b.decay();
        if (th == Theme.SURFACE || th == Theme.OCEAN) {
            b.exterior(th == Theme.SURFACE);
        }
        b.ctl.setChanged();
        return b.at(-6, 1, 3);
    }

    BlockPos at(int u, int y, int v) {
        return o.relative(r, u).relative(back, v).above(y);
    }

    private void set(int u, int y, int v, BlockState s) {
        BlockPos p = at(u, y, v);
        if (!l.getBlockState(p).is(OutpostBlocks.POWER_LEVER.get())) {
            l.setBlock(p, s, 2);
        }
    }

    private BlockState wall() {
        return switch (th) {
            case NETHER -> (rnd.nextInt(5) == 0 ? Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS : Blocks.POLISHED_BLACKSTONE_BRICKS).defaultBlockState();
            case ANCIENT -> rnd.nextInt(9) == 0 ? Blocks.SCULK.defaultBlockState() : OutpostBlocks.OUTPOST_WALL.get().defaultBlockState();
            case JUNGLE -> rnd.nextInt(7) == 0 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState() : OutpostBlocks.OUTPOST_WALL.get().defaultBlockState();
            case CAVE -> rnd.nextInt(10) == 0 ? Blocks.COBBLED_DEEPSLATE.defaultBlockState() : OutpostBlocks.OUTPOST_WALL.get().defaultBlockState();
            case OCEAN -> rnd.nextInt(6) == 0 ? Blocks.WAXED_WEATHERED_CUT_COPPER.defaultBlockState() : OutpostBlocks.OUTPOST_WALL.get().defaultBlockState();
            case SURFACE -> rnd.nextInt(8) == 0 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState() : rnd.nextInt(12) == 0 ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState()
                    : OutpostBlocks.OUTPOST_WALL.get().defaultBlockState();
            default -> OutpostBlocks.OUTPOST_WALL.get().defaultBlockState();
        };
    }

    private BlockState floor() {
        return switch (th) {
            case NETHER -> (rnd.nextInt(4) == 0 ? Blocks.POLISHED_BASALT : Blocks.POLISHED_BLACKSTONE).defaultBlockState();
            case ANCIENT -> rnd.nextInt(5) == 0 ? Blocks.SCULK.defaultBlockState() : OutpostBlocks.OUTPOST_FLOOR.get().defaultBlockState();
            case JUNGLE -> rnd.nextInt(6) == 0 ? Blocks.MOSS_BLOCK.defaultBlockState() : OutpostBlocks.OUTPOST_FLOOR.get().defaultBlockState();
            default -> OutpostBlocks.OUTPOST_FLOOR.get().defaultBlockState();
        };
    }

    private BlockState ceiling() {
        return th == Theme.NETHER ? Blocks.POLISHED_BASALT.defaultBlockState() : OutpostBlocks.OUTPOST_CEILING.get().defaultBlockState();
    }

    private void shell() {
        for (int u = -7; u <= 7; u++) {
            for (int v = 0; v <= 12; v++) {
                for (int y = -1; y <= 6; y++) {
                    boolean side = u == -7 || u == 7 || v == 0 || v == 12;
                    if (y == -1) {
                        set(u, y, v, side ? wall() : (Math.abs(u) <= 2 && v >= 8 && v <= 11 ? OutpostBlocks.HAZARD_FLOOR.get().defaultBlockState() : floor()));
                    } else if (y == 6) {
                        set(u, y, v, side ? wall() : ceiling());
                    } else {
                        set(u, y, v, side ? wall() : Blocks.AIR.defaultBlockState());
                    }
                }
                if (th == Theme.NETHER || th == Theme.SURFACE) {
                    BlockState fill = th == Theme.NETHER ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState();
                    for (int y = -2; y >= -14; y--) {
                        BlockPos p = at(u, y, v);
                        BlockState s = l.getBlockState(p);
                        if (!(s.isAir() || s.canBeReplaced() || !s.getFluidState().isEmpty())) {
                            break;
                        }
                        l.setBlock(p, fill, 2);
                    }
                }
            }
        }
        for (int u = -1; u <= 1; u++) {
            for (int y = 0; y <= 2; y++) {
                set(u, y, 0, Blocks.AIR.defaultBlockState());
            }
        }
        if (th == Theme.NETHER) {
            for (int u : new int[]{-6, 6}) {
                for (int v : new int[]{1, 11}) {
                    for (int y = 0; y <= 5; y++) {
                        set(u, y, v, Blocks.BASALT.defaultBlockState());
                    }
                }
            }
            for (int u : new int[]{-5, -4, 4, 5}) {
                set(u, 2, 0, Blocks.IRON_BARS.defaultBlockState());
                set(u, 3, 0, Blocks.IRON_BARS.defaultBlockState());
            }
        }
    }

    private void gateway() {
        for (int u = -2; u <= 2; u++) {
            for (int y = 0; y <= 5; y++) {
                BlockPos p = at(u, y, 10);
                if (Math.abs(u) == 2 || y == 0 || y == 5) {
                    set(u, y, 10, OutpostBlocks.GATEWAY_FRAME.get().defaultBlockState());
                    ctl.frame.add(p);
                } else {
                    set(u, y, 10, Blocks.AIR.defaultBlockState());
                    ctl.gate.add(p);
                }
            }
        }
        ctl.axis = r.getAxis();
        for (int[] w : new int[][]{{-4, 9}, {4, 9}, {-4, 2}, {4, 2}}) {
            set(w[0], 5, w[1], OutpostBlocks.WARNING_LIGHT.get().defaultBlockState());
            ctl.warn.add(at(w[0], 5, w[1]));
        }
        for (int[] w : new int[][]{{0, 2}, {-4, 6}, {0, 6}, {4, 6}, {0, 8}}) {
            set(w[0], 5, w[1], OutpostBlocks.FACILITY_LIGHT.get().defaultBlockState());
            ctl.lights.add(at(w[0], 5, w[1]));
        }
        MegaDecorator.sign(l, at(0, 5, 9), f, "", "GATEWAY 0", "authorised", "personnel only");
    }

    private void controls() {
        for (int u = -2; u <= 2; u++) {
            set(u, 0, 4, OutpostBlocks.CONTROL_CONSOLE.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, f));
        }
        for (int u : new int[]{-2, 0, 2}) {
            set(u, 1, 4, OutpostBlocks.OUTPOST_MONITOR.get().defaultBlockState().setValue(DecorBlock.FACING, f));
            ctl.monitors.add(at(u, 1, 4));
        }
        for (int u : new int[]{-1, 1}) {
            set(u, 1, 4, ModBlocks.CONTROL_PANEL.get().defaultBlockState().setValue(DecorBlock.FACING, f));
        }
        set(4, 0, 4, OutpostBlocks.EMERGENCY_BUTTON.get().defaultBlockState());
        ctl.button = at(4, 0, 4);
        set(4, -1, 4, OutpostBlocks.HAZARD_FLOOR.get().defaultBlockState());
        set(-6, -1, 3, OutpostBlocks.HAZARD_FLOOR.get().defaultBlockState());
        MegaDecorator.sign(l, at(-6, 2, 3), r, "", "MAIN POWER", "(step 1)", "");
    }

    private void chest(int u, int v, Direction facing) {
        BlockPos p = at(u, 0, v);
        l.setBlock(p, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), 2);
        if (l.getBlockEntity(p) instanceof RandomizableContainerBlockEntity c) {
            c.setLootTable(LOOT, rnd.nextLong());
        }
    }

    private void furniture() {
        Direction in = r, out = r.getOpposite();
        set(-6, 0, 6, ModBlocks.LAB_CABINET.get().defaultBlockState().setValue(DecorBlock.FACING, in));
        set(-6, 0, 7, ModBlocks.LAB_CABINET.get().defaultBlockState().setValue(DecorBlock.FACING, in));
        set(-6, 1, 6, ModBlocks.SPECIMEN_JAR.get().defaultBlockState().setValue(DecorBlock.FACING, in));
        set(-6, 0, 8, ModBlocks.RESEARCH_TABLE.get().defaultBlockState());
        set(-6, 0, 5, ModBlocks.CCTV_BANK.get().defaultBlockState().setValue(DecorBlock.FACING, in));
        chest(-6, 10, in);
        set(6, 0, 1, ModBlocks.SERVER_RACK.get().defaultBlockState().setValue(DecorBlock.FACING, out));
        set(6, 0, 2, ModBlocks.SERVER_RACK.get().defaultBlockState().setValue(DecorBlock.FACING, out));
        set(6, 0, 6, ModBlocks.CONTAINMENT_TANK.get().defaultBlockState().setValue(DecorBlock.FACING, out));
        set(6, 0, 7, ModBlocks.EXPERIMENT_MACHINE.get().defaultBlockState().setValue(DecorBlock.FACING, out));
        set(6, 0, 8, ModBlocks.MICROSCOPE.get().defaultBlockState().setValue(DecorBlock.FACING, out));
        chest(6, 10, out);
        set(2, 0, 7, ModBlocks.CRT_MONITOR.get().defaultBlockState().setValue(DecorBlock.FACING, r));
        BlockPos lec = at(-3, 0, 1);
        l.setBlock(lec, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, back).setValue(LecternBlock.HAS_BOOK, true), 2);
        if (l.getBlockEntity(lec) instanceof LecternBlockEntity le) {
            le.setBook(SpecialDecorator.bookStack(OutpostNotes.forTheme(th, rnd)));
        }
        if (th == Theme.NETHER) {
            for (int[] w : new int[][]{{-5, 1}, {5, 11}, {-5, 11}}) {
                set(w[0], 5, w[1], Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
            }
            set(3, -1, 9, Blocks.MAGMA_BLOCK.defaultBlockState());
            set(-3, -1, 9, Blocks.MAGMA_BLOCK.defaultBlockState());
        }
    }

    /** 0.39.0: the drilling rig under an ocean station: legs to the sea floor, a railed deck, a derrick and a ladder out of the water. */
    private void rig() {
        int deck = -1;
        int sea = l.getSeaLevel();
        for (int u = -10; u <= 10; u++) {
            for (int v = -6; v <= 15; v++) {
                boolean edge = Math.abs(u) == 10 || v == -6 || v == 15;
                set(u, deck, v, edge ? Blocks.POLISHED_ANDESITE.defaultBlockState() : (u + v) % 5 == 0 ? Blocks.IRON_TRAPDOOR.defaultBlockState()
                        : Blocks.SMOOTH_STONE.defaultBlockState());
                if (edge && !(v == -6 && Math.abs(u) <= 1)) {
                    set(u, 0, v, Blocks.IRON_BARS.defaultBlockState());
                }
            }
        }
        // legs and braces down to the sea floor
        for (int[] g : new int[][]{{-10, -6}, {10, -6}, {-10, 15}, {10, 15}, {-10, 5}, {10, 5}, {0, -6}}) {
            BlockPos top = at(g[0], deck - 1, g[1]);
            for (int y = top.getY(); y > l.getMinBuildHeight() + 1; y--) {
                BlockPos p = new BlockPos(top.getX(), y, top.getZ());
                BlockState s = l.getBlockState(p);
                if (y < sea - 1 && !(s.isAir() || s.canBeReplaced() || !s.getFluidState().isEmpty())) {
                    break;
                }
                l.setBlock(p, (y - top.getY()) % 7 == 3 ? Blocks.WAXED_OXIDIZED_COPPER.defaultBlockState() : Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 2);
            }
        }
        // a ladder from below the waterline up to the gap in the railing
        BlockPos lad = at(0, 0, -7);
        for (int y = sea - 8; y <= lad.getY(); y++) {
            l.setBlock(new BlockPos(lad.getX(), y, lad.getZ()), Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, f)
                    .setValue(BlockStateProperties.WATERLOGGED, y < sea), 2);
        }
        // derrick: a lattice tower in the back corner, a lamp on top
        for (int y = 0; y <= 14; y++) {
            for (int[] c : new int[][]{{-9, 12}, {-7, 12}, {-9, 14}, {-7, 14}}) {
                set(c[0], y, c[1], Blocks.IRON_BARS.defaultBlockState());
            }
            if (y % 4 == 3) {
                set(-8, y, 12, Blocks.CHAIN.defaultBlockState().setValue(BlockStateProperties.AXIS, net.minecraft.core.Direction.Axis.X));
            }
        }
        set(-8, 15, 13, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(BlockStateProperties.LIT, true));
        set(-8, 16, 13, Blocks.LIGHTNING_ROD.defaultBlockState());
        for (int[] c : new int[][]{{-9, -5}, {9, -5}, {9, 14}}) {
            set(c[0], 1, c[1], Blocks.LANTERN.defaultBlockState());
        }
        set(8, 0, -4, Blocks.BARREL.defaultBlockState());
        set(8, 0, -3, Blocks.BARREL.defaultBlockState());
        set(8, 1, -4, Blocks.BARREL.defaultBlockState());
    }

    /** 0.39.0: outside of a surface station (land: path, sign, fence; ocean: sign only): windows, antenna. */
    private void exterior(boolean land) {
        for (int u : new int[]{-5, -4, 4, 5}) {
            set(u, 2, 0, Blocks.IRON_BARS.defaultBlockState());
            set(u, 3, 0, Blocks.IRON_BARS.defaultBlockState());
        }
        for (int u : new int[]{-7, 7}) {
            for (int v = 4; v <= 6; v++) {
                set(u, 2, v, Blocks.IRON_BARS.defaultBlockState());
                set(u, 3, v, Blocks.IRON_BARS.defaultBlockState());
            }
        }
        for (int y = 7; y <= 10; y++) {
            set(5, y, 9, Blocks.IRON_BARS.defaultBlockState());
        }
        set(5, 11, 9, Blocks.LIGHTNING_ROD.defaultBlockState());
        set(4, 7, 9, Blocks.IRON_TRAPDOOR.defaultBlockState());
        set(-5, 7, 3, Blocks.CAULDRON.defaultBlockState());
        if (!land) {
            sign();
            return;
        }
        // a clear apron in front of the door, gravel underfoot
        for (int u = -2; u <= 2; u++) {
            for (int v = -4; v <= -1; v++) {
                for (int y = 0; y <= 3; y++) {
                    set(u, y, v, Blocks.AIR.defaultBlockState());
                }
                for (int y = -1; y >= -6; y--) {
                    BlockPos p = at(u, y, v);
                    BlockState s = l.getBlockState(p);
                    boolean top = y == -1;
                    if (!top && !(s.isAir() || s.canBeReplaced() || !s.getFluidState().isEmpty())) {
                        break;
                    }
                    l.setBlock(p, top && rnd.nextInt(3) > 0 ? Blocks.GRAVEL.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState(), 2);
                }
            }
        }
        sign();
        l.setBlock(at(-2, 2, -1), Blocks.LANTERN.defaultBlockState(), 2);
        l.setBlock(at(-2, 1, -1), Blocks.IRON_BARS.defaultBlockState(), 2);
        l.setBlock(at(-2, 0, -1), Blocks.IRON_BARS.defaultBlockState(), 2);
        fence();
    }

    private void sign() {
        BlockPos sp = at(2, 2, -1);
        l.setBlock(sp, Blocks.SPRUCE_WALL_SIGN.defaultBlockState().setValue(net.minecraft.world.level.block.WallSignBlock.FACING, f), 2);
        if (l.getBlockEntity(sp) instanceof net.minecraft.world.level.block.entity.SignBlockEntity s) {
            String[] lines = {"RESEARCH", "STATION " + (1 + rnd.nextInt(9)), "AUTHORIZED", "PERSONNEL ONLY"};
            net.minecraft.world.level.block.entity.SignText t = new net.minecraft.world.level.block.entity.SignText();
            for (int i = 0; i < 4; i++) {
                t = t.setMessage(i, net.minecraft.network.chat.Component.literal(lines[i]));
            }
            s.setText(t, true);
        }
    }

    private void fence() {
        // chain-link fence two blocks around the station, open in front of the door, some panels missing
        for (int u = -9; u <= 9; u++) {
            for (int v = -6; v <= 14; v++) {
                boolean ring = Math.abs(u) == 9 || v == -6 || v == 14;
                if (!ring || v == -6 && Math.abs(u) <= 2 || rnd.nextInt(9) == 0) {
                    continue;
                }
                BlockPos c = at(u, 0, v);
                int top = l.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, c.getX(), c.getZ());
                for (int h = 0; h < 2; h++) {
                    BlockPos p = new BlockPos(c.getX(), top + h, c.getZ());
                    if (l.getBlockState(p).canBeReplaced() && l.getFluidState(p).isEmpty()) {
                        l.setBlock(p, Blocks.IRON_BARS.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private void decay() {
        for (int u = -6; u <= 6; u++) {
            for (int v = 1; v <= 11; v++) {
                for (int y = 0; y <= 5; y++) {
                    BlockPos p = at(u, y, v);
                    if (!l.getBlockState(p).isAir()) {
                        continue;
                    }
                    boolean corner = (y >= 4) && (Math.abs(u) == 6 || v == 1 || v == 11);
                    if (corner && th != Theme.NETHER && rnd.nextInt(4) == 0) {
                        l.setBlock(p, Blocks.COBWEB.defaultBlockState(), 2);
                    } else if (th == Theme.JUNGLE && Math.abs(u) == 6 && rnd.nextInt(3) == 0) {
                        Direction wallSide = u < 0 ? r.getOpposite() : r;
                        l.setBlock(p, Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(wallSide), true), 2);
                    } else if (th == Theme.ANCIENT && y == 0 && rnd.nextInt(14) == 0) {
                        l.setBlock(p, Blocks.SCULK_VEIN.defaultBlockState().setValue(BlockStateProperties.DOWN, true), 2);
                    } else if (th == Theme.JUNGLE && y == 0 && rnd.nextInt(9) == 0) {
                        l.setBlock(p, Blocks.MOSS_CARPET.defaultBlockState(), 2);
                    } else if (th == Theme.CAVE && y == 0 && rnd.nextInt(16) == 0) {
                        l.setBlock(p, Blocks.POINTED_DRIPSTONE.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    // --- access ---------------------------------------------------------------------------------------------
    /** A 3 × 3 tunnel from the entrance to {@code to}, stepping one block up or down per block where needed. */
    public static void tunnel(ServerLevel l, BlockPos door, BlockPos to, Theme th) {
        double x = door.getX() + 0.5, z = door.getZ() + 0.5;
        int y = door.getY();
        double dx = to.getX() + 0.5 - x, dz = to.getZ() + 0.5 - z, len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1) {
            return;
        }
        dx /= len;
        dz /= len;
        BlockState fl = th == Theme.ANCIENT ? Blocks.DEEPSLATE_TILES.defaultBlockState() : th == Theme.NETHER ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.COBBLED_DEEPSLATE.defaultBlockState();
        for (int s = 0; s <= (int) len + 1 && s < 64; s++) {
            if (s > 2 && y != to.getY()) {
                y += Integer.signum(to.getY() - y);
            }
            BlockPos c = BlockPos.containing(x + dx * s, y, z + dz * s);
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    for (int h = 0; h <= 2; h++) {
                        BlockPos p = c.offset(a, h, b);
                        if (!l.getBlockState(p).is(OutpostBlocks.OUTPOST_WALL.get())) {
                            l.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                    BlockPos fp = c.offset(a, -1, b);
                    if (l.getBlockState(fp).canBeReplaced()) {
                        l.setBlock(fp, fl, 2);
                    }
                }
            }
        }
    }

    /** A ladder shaft from {@code bottom} up to the surface, closed with a trapdoor. */
    public static void shaft(ServerLevel l, BlockPos bottom, Direction ladderFacing) {
        int top = l.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bottom.getX(), bottom.getZ());
        BlockPos wallPos = bottom.relative(ladderFacing.getOpposite());
        for (int y = bottom.getY(); y < top; y++) {
            BlockPos p = new BlockPos(bottom.getX(), y, bottom.getZ());
            l.setBlock(p, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, ladderFacing), 2);
            BlockPos w = new BlockPos(wallPos.getX(), y, wallPos.getZ());
            if (l.getBlockState(w).canBeReplaced()) {
                l.setBlock(w, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 2);
            }
        }
        l.setBlock(new BlockPos(bottom.getX(), top, bottom.getZ()), Blocks.IRON_TRAPDOOR.defaultBlockState()
                .setValue(net.minecraft.world.level.block.TrapDoorBlock.FACING, ladderFacing), 2);
    }
}
