package de.officerooms.sea;

import static de.officerooms.sea.Ship.*;

import de.officerooms.core.Hashing;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 0.42.0: turns the plan of a {@link Ship} into blocks: hull, decks, corridors, walls, ceilings with lamps and vents,
 * doors, staircases, railings, the funnel and the lifeboats. Everything that needs a block entity (chests, furniture of
 * the furniture mods, beds) comes later from {@link ShipDecorator}.
 */
public final class ShipShape {

    private ShipShape() {
    }

    static final BlockState AIR = Blocks.AIR.defaultBlockState();
    static final BlockState WATER = Blocks.WATER.defaultBlockState();
    private static final BlockState NOFLIGHT = Blocks.STRUCTURE_VOID.defaultBlockState();

    static BlockState b(net.minecraftforge.registries.RegistryObject<Block> r) {
        return r.get().defaultBlockState();
    }

    /** A block of this mod (or another) by id, or air when it is missing. */
    static BlockState id(String id) {
        Block bl = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id.contains(":") ? id : "office_rooms:" + id));
        return bl == null ? AIR : bl.defaultBlockState();
    }

    static long h(Ship s, int a, int b, int c) {
        return Hashing.hash(s.seed ^ 0x51F0, s.gx * 4096L + a, s.gz * 4096L + b, c);
    }

    static double u(Ship s, int a, int b, int c) {
        return Hashing.unit(h(s, a, b, c));
    }

    /** World direction of a local horizontal direction (the ship may face west). */
    static Direction w(Ship s, Direction d) {
        return s.flip && d.getAxis() == Direction.Axis.X ? d.getOpposite() : d;
    }

    static Direction xdir(Ship s, int dir) {
        return w(s, dir > 0 ? Direction.EAST : Direction.WEST);
    }

    /** Block at local (lx, lz), absolute y. null = not part of the ship (water or air of the sea). */
    public static BlockState at(Ship s, int lx, int y, int lz) {
        int e = e(lz);
        if (y < KEEL) {
            return null;
        }
        if (y < fy(0)) {
            return e < half(lx, y) ? b(ShipBlocks.HULL_DARK) : null;
        }
        int d = (y - fy(0)) / DH, o = (y - fy(0)) % DH;
        if (d >= DECKS) {
            return top(s, lx, y, lz);
        }
        if (s.hasFlight(lx, lz)) {
            BlockState f = flight(s, lx, y, lz);
            if (f != NOFLIGHT) {
                return f;
            }
        }
        byte c = s.code(d, lx, lz);
        if (c == O) {
            if (e < half(lx, y) && d < 10) {
                return hull(s, d, o, lx, y, lz);               // the hull flares out above the plan level
            }
            return d == 10 ? lifeboat(s, lx, o, lz) : null;
        }
        if (d == 11 && o >= 1) {
            BlockState t = top(s, lx, y, lz);
            if (t != null) {
                return t;
            }
        }
        return switch (o) {
            case 0 -> floor(s, d, lx, lz, c);
            case 4 -> ceiling(s, d, lx, lz, c);
            default -> wall(s, d, o, lx, y, lz, c);
        };
    }

    // --- staircases --------------------------------------------------------------------------------------------------

    static BlockState flight(Ship s, int lx, int y, int lz) {
        for (Flight f : s.flights) {
            if (lz < f.z0() || lz > f.z1()) {
                continue;
            }
            int k = f.k(lx);
            int base = fy(f.d());
            if (k < 0 || y < base + 1 || y > base + 6) {
                continue;
            }
            int sy = base + 1 + k;
            if (y == sy) {
                Block st = f.steel() || f.d() == 10 ? ShipBlocks.STEEL_STAIRS.get() : ShipBlocks.TEAL_STAIRS.get();
                return st.defaultBlockState().setValue(StairBlock.FACING, xdir(s, f.dir()));
            }
            if (y < sy) {
                return f.steel() ? b(ShipBlocks.STEEL_FLOOR) : f.d() == 10 ? b(ShipBlocks.HULL) : b(ShipBlocks.WOOD_PANEL);
            }
            return AIR;
        }
        return NOFLIGHT;
    }

    // --- hull --------------------------------------------------------------------------------------------------------

    static BlockState hull(Ship s, int d, int o, int lx, int y, int lz) {
        int side = lz > C ? 1 : -1;
        // the =) logo near the bow, two by two blocks
        int la = L - 58;
        if ((lx == la || lx == la + 1) && (y == fy(8) + 2 || y == fy(8) + 3) && e(lz) >= half(lx, y) - 1) {
            int col = lx == la ? 0 : 1;
            boolean lowerWorldX = s.wx(lx) < s.wx(lx == la ? la + 1 : la);
            int c0 = side > 0 ? (lowerWorldX ? 0 : 1) : (lowerWorldX ? 1 : 0);
            int row = y == fy(8) + 3 ? 0 : 1;
            return b(ShipBlocks.SMILE).setValue(ShipBlocks.Variant.VARIANT, c0 + 2 * row);
        }
        if (y < SEA) {
            return b(ShipBlocks.HULL_DARK);
        }
        byte win = s.aux(d, lx, lz);
        if (o >= 1 && o <= 3 && s.code(d, lx, lz) == HU) {
            if (win == WIN_PORT && o == 2) {
                return b(ShipBlocks.PORTHOLE);
            }
            if (win == WIN_BIG) {
                return b(ShipBlocks.WINDOW);
            }
        }
        if (o == 3 && win == WIN_PORT && u(s, lx, y, lz) < 0.5 || u(s, lx, y, lz + 999) < 0.03) {
            return b(ShipBlocks.HULL_RUST);                    // rust weeping from under the portholes
        }
        return b(ShipBlocks.HULL);
    }

    // --- floors ------------------------------------------------------------------------------------------------------

    static BlockState floor(Ship s, int d, int lx, int lz, byte c) {
        boolean crew = d <= 4;
        Room r = s.room(d, lx, lz);
        switch (c) {
            case VO:
                return AIR;
            case HU:
                return b(lz > C ? ShipBlocks.HULL : ShipBlocks.HULL);
            case PR, TOP, RA:
                if (d == 11 && lx >= 98 && lx <= 120 && e(lz) <= 5) {
                    return b(ShipBlocks.POOL_TILE);
                }
                return b(ShipBlocks.TEAK);
            case CO, FD, BL:
                if (crew) {
                    return e(lz) <= 1 ? b(ShipBlocks.CARPET_BROWN) : b(ShipBlocks.STEEL_FLOOR);
                }
                if (d >= 5 && d <= 8 && lx > ATR0 && lx < ATR1) {
                    return b(ShipBlocks.MARBLE);
                }
                if (Math.floorMod(lx, 14) == 0) {
                    return b(ShipBlocks.CARPET_STARS);
                }
                return u(s, lx, d, lz) < 0.025 ? b(ShipBlocks.CARPET_WET) : b(ShipBlocks.CARPET);
            case LO:
                return crew ? b(ShipBlocks.STEEL_FLOOR) : b(ShipBlocks.CARPET_TEAL);
            case LANE:
                if (crew && d >= 2 && d <= 3 && lx >= ENG0 && lx <= ENG1) {
                    return b(ShipBlocks.GRATE);
                }
                return crew ? b(ShipBlocks.STEEL_FLOOR) : b(ShipBlocks.CARPET_TEAL);
            case RL:
                if (crew && d >= 2 && d <= 3 && lx >= ENG0 && lx <= ENG1) {
                    return b(ShipBlocks.GRATE);
                }
                if (d == 11) {
                    return b(ShipBlocks.TEAK);
                }
                return crew ? b(ShipBlocks.STEEL_FLOOR) : b(ShipBlocks.CARPET_TEAL);
            case EL, ELD:
                return b(ShipBlocks.WOOD_PANEL);
            case RM:
                return r == null ? b(ShipBlocks.CARPET) : roomFloor(s, r, lx, lz);
            default:
                return crew ? b(ShipBlocks.STEEL_FLOOR) : b(ShipBlocks.CARPET);
        }
    }

    static BlockState roomFloor(Ship s, Room r, int lx, int lz) {
        return switch (r.kind) {
            case FLOODED -> b(ShipBlocks.CARPET_WET);
            case MESSY -> u(s, lx, r.d, lz) < 0.25 ? b(ShipBlocks.CARPET_WET) : b(ShipBlocks.CARPET);
            case SUITE, PURSER, BAR, LOUNGE, CAPTAIN -> b(ShipBlocks.CARPET_TEAL);
            case CASINO, THEATRE, LIBRARY, DINING -> b(ShipBlocks.CARPET_RED);
            case EMBARK, SHOPS, ATRIUM, BUFFET -> b(ShipBlocks.MARBLE);
            case KIDS -> id("play_carpet");
            case GAMES -> id("arcade_carpet");
            case SPA -> b(ShipBlocks.POOL_TILE);
            case MEDICAL -> Blocks.WHITE_CONCRETE.defaultBlockState();
            case COLD_ROOM -> Blocks.PACKED_ICE.defaultBlockState();
            case CREW_CABIN, CREW_MESS, CREW_OFFICE, LINEN, BRIDGE -> b(ShipBlocks.CARPET_BROWN);
            case CABIN, TWIN, DARK, BARRICADED, PARTY, STATIC, LUGGAGE, UPSIDE -> b(ShipBlocks.CARPET);
            case STRIPPED -> b(ShipBlocks.STEEL_FLOOR);
            default -> b(ShipBlocks.STEEL_FLOOR);
        };
    }

    // --- ceilings ----------------------------------------------------------------------------------------------------

    static BlockState lamp(Ship s, int lx, int d, int lz, boolean crew, double dead) {
        BlockState l = b(crew ? ShipBlocks.CREW_LIGHT : ShipBlocks.LIGHT);
        return u(s, lx, d * 7 + 1, lz) < dead ? l.setValue(ShipBlocks.Lamp.LIT, false) : l;
    }

    static BlockState ceiling(Ship s, int d, int lx, int lz, byte c) {
        boolean crew = d <= 4;
        if (s.code(d + 1, lx, lz) == VO) {
            return AIR;
        }
        if (d == 10 && (c == TOP || (c == RA && (lx < SUP0 || lx > SUP1)))) {
            return AIR;
        }
        if (d == 11) {
            return AIR;
        }
        int e = e(lz);
        BlockState plain = b(crew ? ShipBlocks.STEEL_FRAME : ShipBlocks.CEILING);
        switch (c) {
            case CO, FD, BL, RL:
                if (crew) {
                    if ((e == 0 && Math.floorMod(lx, 6) == 3) || (e == 9 && Math.floorMod(lx, 7) == 2)) {
                        return lamp(s, lx, d, lz, true, 0.25);
                    }
                    if (Math.floorMod(lx, 13) == 5 && (e == 1 || e == 9)) {
                        return b(ShipBlocks.VENT);
                    }
                    return plain;
                }
                if (d >= 5 && d <= 8 && lx > ATR0 && lx < ATR1) {
                    return b(ShipBlocks.GOLD_CEILING);
                }
                boolean narrow = s.inner(d, lx) < 13;
                if ((narrow ? e == 0 : e == 8) && Math.floorMod(lx, 5) == 2) {
                    return lamp(s, lx, d, lz, false, 0.18);
                }
                if (Math.floorMod(lx, 11) == 6 && (narrow ? e == 1 : e == 9)) {
                    return b(ShipBlocks.VENT);
                }
                return plain;
            case LO, LANE:
                if (crew) {
                    return Math.floorMod(lx, 4) == 1 && e % 4 == 1 ? lamp(s, lx, d, lz, true, 0.2) : plain;
                }
                return Math.floorMod(lx, 4) == 1 && e % 4 == 1 ? lamp(s, lx, d, lz, false, 0.1) : b(ShipBlocks.GOLD_CEILING);
            case PR, RA:
                return Math.floorMod(lx, 6) == 0 && c == PR && e == s.inner(d, lx) - 4 ? lamp(s, lx, d, lz, false, 0.3) : b(ShipBlocks.STEEL_FRAME);
            case RM: {
                Room r = s.room(d, lx, lz);
                if (r == null) {
                    return plain;
                }
                if (r.kind.venue()) {
                    boolean gold = r.kind == Kind.EMBARK || r.kind == Kind.CASINO || r.kind == Kind.LOUNGE || r.kind == Kind.ATRIUM || r.kind == Kind.BAR;
                    if (Math.floorMod(lx - r.x0, 4) == 2 && Math.floorMod(lz, 4) == 2) {
                        return lamp(s, lx, d, lz, crew, r.kind == Kind.ENGINE ? 0.35 : 0.15);
                    }
                    if (u(s, lx, d * 3 + 2, lz) < 0.006) {
                        return b(ShipBlocks.VENT);
                    }
                    return gold ? b(ShipBlocks.GOLD_CEILING) : plain;
                }
                int cx = (r.x0 + r.x1) / 2, cz = (r.z0 + r.z1) / 2;
                if (lx == cx && lz == cz) {
                    double dead = r.kind == Kind.DARK || r.kind == Kind.STRIPPED ? 1.0 : r.kind == Kind.UPSIDE ? 0.6 : 0.3;
                    return lamp(s, lx, d, lz, crew, dead);
                }
                if (lx == r.x0 && lz == (r.z0 > C ? r.z1 : r.z0) && Hashing.unit(r.seed) < 0.3) {
                    return b(ShipBlocks.VENT);
                }
                return plain;
            }
            case HU:
                return b(ShipBlocks.HULL);
            default:
                return plain;
        }
    }

    // --- walls and the air in between --------------------------------------------------------------------------------

    static BlockState wall(Ship s, int d, int o, int lx, int y, int lz, byte c) {
        boolean crew = d <= 4;
        int e = e(lz);
        switch (c) {
            case HU:
                return hull(s, d, o, lx, y, lz);
            case WA:
                return wallBlock(s, d, o, lx, lz, crew);
            case DR: {
                if (o == 3) {
                    return wallBlock(s, d, o, lx, lz, crew);
                }
                Direction f = s.aux(d, lx, lz) == 1 ? Direction.SOUTH : Direction.NORTH;
                Block door = (crew ? ShipBlocks.CREW_DOOR : ShipBlocks.CABIN_DOOR).get();
                boolean open = u(s, lx, d * 11, lz) < 0.18;
                return door.defaultBlockState().setValue(DoorBlock.FACING, f).setValue(DoorBlock.OPEN, open)
                        .setValue(DoorBlock.HALF, o == 1 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
            }
            case FD: {
                if (o == 3) {
                    return wallBlock(s, d, o, lx, lz, crew);
                }
                boolean left = s.code(d, lx, lz + 1) == FD;
                return ShipBlocks.FIRE_DOOR.get().defaultBlockState().setValue(DoorBlock.FACING, xdir(s, 1))
                        .setValue(DoorBlock.HINGE, left ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT)
                        .setValue(DoorBlock.HALF, o == 1 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
            }
            case BL: {
                double r = u(s, lx, y, lz);
                if (o == 1) {
                    return r < 0.35 ? Blocks.BARREL.defaultBlockState() : r < 0.6 ? id("refurbished_furniture:spruce_crate").isAir()
                            ? Blocks.SPRUCE_PLANKS.defaultBlockState() : id("refurbished_furniture:spruce_crate")
                            : r < 0.85 ? b(ShipBlocks.SUITCASE).setValue(ShipBlocks.Thin.FACING, Direction.NORTH) : Blocks.COBWEB.defaultBlockState();
                }
                if (o == 2) {
                    return r < 0.4 ? Blocks.IRON_BARS.defaultBlockState() : r < 0.7 ? Blocks.CHAIN.defaultBlockState() : Blocks.COBWEB.defaultBlockState();
                }
                return r < 0.3 ? Blocks.COBWEB.defaultBlockState() : AIR;
            }
            case EL:
                return crew ? b(ShipBlocks.STEEL_WALL) : (o == 3 ? b(ShipBlocks.TEAL_TRIM) : b(ShipBlocks.WOOD_PANEL));
            case ELD: {
                if (o == 3) {
                    return crew ? b(ShipBlocks.STEEL_WALL) : b(ShipBlocks.BRASS_COLUMN);
                }
                int dir = s.code(d, lx - 1, lz) == LO ? -1 : 1;
                return ShipBlocks.f(ShipBlocks.ELEVATOR_DOOR, xdir(s, dir));
            }
            case RL:
                return o == 1 ? fence(s, d, lx, lz, crew || d == 11 ? ShipBlocks.RAILING.get() : ShipBlocks.BRASS_RAILING.get()) : AIR;
            case RA:
                return o == 1 ? fence(s, d, lx, lz, ShipBlocks.RAILING.get()) : AIR;
            case CO:
                return corridor(s, d, o, lx, y, lz, crew, e);
            case RM: {
                Room r = s.room(d, lx, lz);
                if (r != null && o == 1 && (r.kind == Kind.FLOODED || (r.kind == Kind.PUMP) || s.aux(d, lx, lz) == 1)) {
                    return WATER;
                }
                return AIR;
            }
            case PR:
                if (o == 2 && e == s.inner(d, lx) - 5 && Math.floorMod(lx, 9) == 4 && s.code(d, lx, lz + (lz > C ? -1 : 1)) == WA) {
                    return ShipBlocks.f(ShipBlocks.SCONCE, lz > C ? Direction.SOUTH : Direction.NORTH);
                }
                return AIR;
            default:
                return AIR;
        }
    }

    static BlockState wallBlock(Ship s, int d, int o, int lx, int lz, boolean crew) {
        if (crew) {
            return o == 3 && Math.floorMod(lx, 9) == 0 ? b(ShipBlocks.PIPES) : b(ShipBlocks.STEEL_WALL);
        }
        // walls of some venues get their own look
        for (int[] n : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            Room r = s.room(d, lx + n[0], lz + n[1]);
            if (r != null && r.kind.venue()) {
                switch (r.kind) {
                    case THEATRE -> {
                        return b(ShipBlocks.CURTAIN);
                    }
                    case DINING, BAR, LOUNGE, LIBRARY, CASINO, CAPTAIN, EMBARK -> {
                        return o == 1 ? b(ShipBlocks.WAINSCOT) : o == 3 ? b(ShipBlocks.TEAL_TRIM) : b(ShipBlocks.WOOD_PANEL);
                    }
                    case KIDS -> {
                        return id("fun_wallpaper");
                    }
                    case SPA -> {
                        return b(ShipBlocks.POOL_TILE);
                    }
                    default -> {
                    }
                }
            }
        }
        if (d == 10 && s.code(d, lx, lz + (lz > C ? 1 : -1)) == PR) {
            return o == 2 && Math.floorMod(lx, 4) != 0 ? b(ShipBlocks.WINDOW) : b(ShipBlocks.HULL);   // promenade wall
        }
        return o == 1 ? b(ShipBlocks.WAINSCOT) : b(ShipBlocks.WALL_PANEL);
    }

    /** Corridor air with the little things: handrails, sconces, life rings, fire hoses, the odd suitcase. */
    static BlockState corridor(Ship s, int d, int o, int lx, int y, int lz, boolean crew, int e) {
        int wallDz = 0;
        if (isWall(s.code(d, lx, lz - 1))) {
            wallDz = -1;
        } else if (isWall(s.code(d, lx, lz + 1))) {
            wallDz = 1;
        }
        if (wallDz != 0) {
            Direction away = wallDz < 0 ? Direction.SOUTH : Direction.NORTH;
            if (!crew && o == 1 && !(d >= 5 && d <= 8 && lx > ATR0 && lx < ATR1)) {
                return ShipBlocks.f(ShipBlocks.HANDRAIL, away);
            }
            if (!crew && o == 2 && Math.floorMod(lx, 8) == 4) {
                return ShipBlocks.f(ShipBlocks.SCONCE, away);
            }
            if (crew && o == 2 && Math.floorMod(lx, 23) == 7) {
                return ShipBlocks.f(ShipBlocks.FIRE_HOSE, away);
            }
            if (crew && o == 2 && Math.floorMod(lx, 31) == 18) {
                return ShipBlocks.f(ShipBlocks.LIFE_RING, away);
            }
            if (o == 3 && Math.floorMod(lx, 37) == 11) {
                return ShipBlocks.f(ShipBlocks.EXIT_SIGN, away);
            }
        }
        if (o == 1 && u(s, lx, d * 5 + 3, lz) < 0.004) {
            return ShipBlocks.f(ShipBlocks.SUITCASE, u(s, lx, 1, lz) < 0.5 ? Direction.EAST : Direction.SOUTH);
        }
        return AIR;
    }

    static boolean isWall(byte c) {
        return c == WA || c == HU;
    }

    static BlockState fence(Ship s, int d, int lx, int lz, Block fence) {
        BlockState f = fence.defaultBlockState();
        boolean e = rail(s.code(d, lx + 1, lz)), wst = rail(s.code(d, lx - 1, lz));
        if (s.flip) {
            boolean t = e;
            e = wst;
            wst = t;
        }
        return f.setValue(FenceBlock.EAST, e).setValue(FenceBlock.WEST, wst)
                .setValue(FenceBlock.SOUTH, rail(s.code(d, lx, lz + 1))).setValue(FenceBlock.NORTH, rail(s.code(d, lx, lz - 1)));
    }

    static boolean rail(byte c) {
        return c == RA || c == RL || c == HU || c == WA;
    }

    // --- outside: lifeboats, the funnel, the mast ----------------------------------------------------------------------

    static BlockState lifeboat(Ship s, int lx, int o, int lz) {
        int e = e(lz), side = lz > C ? 1 : -1;
        for (int[] lb : s.lifeboats) {
            if (lb[1] != side || lx < lb[0] - 1 || lx > lb[0] + 9) {
                continue;
            }
            int in = s.inner(10, lx);
            int k = lx - lb[0], q = e - in;                     // q 1..3 across the boat
            if (o == 4 && (k == 1 || k == 7) && q >= 0 && q <= 2) {
                return b(ShipBlocks.HULL);                       // davit arm
            }
            if (q < 1 || q > 3 || k < 0 || k > 8) {
                return null;
            }
            boolean end = k == 0 || k == 8;
            if (o == 1) {
                return (q == 2 && !end) || (k >= 2 && k <= 6) ? b(ShipBlocks.LIFEBOAT) : null;
            }
            if (o == 2) {
                return end && q != 2 ? null : b(ShipBlocks.LIFEBOAT);
            }
            if (o == 3) {
                return end ? null : b(ShipBlocks.LIFEBOAT_CANOPY);
            }
        }
        return null;
    }

    /** Sun deck features and everything above it: the funnel with the =) logo and a mast over the bridge. */
    static BlockState top(Ship s, int lx, int y, int lz) {
        int e = e(lz);
        int base = fy(11);
        double fx = (lx - 45) / 7.5, fz = e / 5.5;
        if (fx * fx + fz * fz <= 1.0 && y > base && y <= base + 17) {
            if (y >= base + 16) {
                return b(ShipBlocks.FUNNEL_TOP);
            }
            double fxo = (lx - 45) / 6.5, fzo = (e + 1) / 5.5;
            boolean surface = fxo * fxo + fzo * fzo > 1.0;
            if (surface && (lx == 44 || lx == 45) && (y == base + 10 || y == base + 11)) {
                int side = lz > C ? 1 : -1;
                boolean lowerWorldX = s.wx(lx) < s.wx(lx == 44 ? 45 : 44);
                int c0 = side > 0 ? (lowerWorldX ? 0 : 1) : (lowerWorldX ? 1 : 0);
                int row = y == base + 11 ? 0 : 1;
                return b(ShipBlocks.FUNNEL_SMILE).setValue(ShipBlocks.Variant.VARIANT, c0 + 2 * row);
            }
            return b(ShipBlocks.FUNNEL);
        }
        int mx = SUP1 - 5;
        if (lx == mx && lz == C && y > base && y <= base + 13) {
            return fence(s, 11, lx, lz, ShipBlocks.RAILING.get()).setValue(FenceBlock.NORTH, y == base + 11).setValue(FenceBlock.SOUTH, y == base + 11);
        }
        if (lx == mx && e <= 3 && e > 0 && y == base + 11) {
            return ShipBlocks.RAILING.get().defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);
        }
        if (y == base + 1 && s.code(11, lx, lz) == TOP && lx >= 98 && lx <= 120 && e <= 5) {
            if (lx == 98 || lx == 120 || e == 5) {
                return b(ShipBlocks.POOL_TILE);                  // rim of the pool
            }
            return (s.openCore % 2 == 0) ? WATER : AIR;            // some pools were drained
        }
        return null;
    }
}
