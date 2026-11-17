package de.officerooms.outpost;

import de.officerooms.core.ModSounds;
import de.officerooms.underside.UndersideEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 0.20.0: the brain of an outpost, stored in the power lever. Phases: OFF → (lever) BOOT → WARNING → READY →
 * (red button) ACTIVATING → ONLINE. Pulling the lever back shuts everything down and closes the gateway.
 */
public final class Controller extends BlockEntity {

    public static final int OFF = 0, BOOT = 1, WARNING = 2, READY = 3, ACTIVATING = 4, ONLINE = 5;
    static final int BOOT_T = 50, WARN_T = 220, ACT_T = 170;

    int phase;
    long start;
    final List<BlockPos> monitors = new ArrayList<>(), lights = new ArrayList<>(), warn = new ArrayList<>(), frame = new ArrayList<>(), gate = new ArrayList<>();
    BlockPos button = BlockPos.ZERO;
    Direction.Axis axis = Direction.Axis.X;

    public Controller(BlockPos pos, BlockState state) {
        super(OutpostBlocks.CONTROLLER_BE.get(), pos, state);
    }

    /** Nearest controller within 16 blocks (the one wired to this button if there are several). */
    static Controller near(ServerLevel l, BlockPos pos) {
        Controller best = null;
        double bd = Double.MAX_VALUE;
        for (BlockPos q : BlockPos.betweenClosed(pos.offset(-16, -6, -16), pos.offset(16, 6, 16))) {
            if (l.getBlockEntity(q) instanceof Controller c) {
                if (c.button.equals(pos)) {
                    return c;
                }
                double d = q.distSqr(pos);
                if (d < bd) {
                    bd = d;
                    best = c;
                }
            }
        }
        return best;
    }

    // --- control ------------------------------------------------------------------------------------------
    void power(ServerLevel l, boolean on) {
        if (on) {
            setPhase(l, BOOT);
            l.playSound(null, worldPosition, ModSounds.OUTPOST_POWER_ON.get(), SoundSource.BLOCKS, 3.0F, 1.0F);
        } else {
            for (BlockPos p : lights) {
                lamp(l, p, false);
            }
            for (BlockPos p : warn) {
                lamp(l, p, false);
            }
            for (BlockPos p : frame) {
                frame(l, p, false);
            }
            for (BlockPos p : gate) {
                if (l.getBlockState(p).is(OutpostBlocks.GATEWAY.get())) {
                    l.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                }
            }
            setPhase(l, OFF);
            l.playSound(null, worldPosition, ModSounds.OUTPOST_POWER_OFF.get(), SoundSource.BLOCKS, 3.0F, 1.0F);
        }
    }

    String press(ServerLevel l) {
        return switch (phase) {
            case OFF -> "NO POWER. Restore main power first.";
            case BOOT, WARNING -> "SAFETY LOCK: warning sequence in progress.";
            case READY -> {
                setPhase(l, ACTIVATING);
                l.playSound(null, center().x, center().y, center().z, ModSounds.GATEWAY_CHARGE.get(), SoundSource.BLOCKS, 4.0F, 1.0F);
                yield "PORTAL ACTIVATION SEQUENCE INITIATED";
            }
            case ACTIVATING -> "ACTIVATION IN PROGRESS. Stand clear of the gateway.";
            default -> "The gateway is already open.";
        };
    }

    private void setPhase(ServerLevel l, int p) {
        phase = p;
        start = l.getGameTime();
        for (BlockPos m : monitors) {
            if (l.getBlockEntity(m) instanceof OutpostBlocks.MonitorEntity me) {
                me.set(p, start);
            }
        }
        setChanged();
    }

    private Vec3 center() {
        if (gate.isEmpty()) {
            return Vec3.atCenterOf(worldPosition);
        }
        double x = 0, y = 0, z = 0;
        for (BlockPos p : gate) {
            x += p.getX() + 0.5;
            y += p.getY() + 0.5;
            z += p.getZ() + 0.5;
        }
        return new Vec3(x / gate.size(), y / gate.size(), z / gate.size());
    }

    private static void lamp(ServerLevel l, BlockPos p, boolean on) {
        BlockState s = l.getBlockState(p);
        if (s.hasProperty(OutpostBlocks.LIT) && s.getValue(OutpostBlocks.LIT) != on) {
            l.setBlock(p, s.setValue(OutpostBlocks.LIT, on), 2);
        }
    }

    private static void frame(ServerLevel l, BlockPos p, boolean on) {
        BlockState s = l.getBlockState(p);
        if (s.hasProperty(OutpostBlocks.ACTIVE) && s.getValue(OutpostBlocks.ACTIVE) != on) {
            l.setBlock(p, s.setValue(OutpostBlocks.ACTIVE, on), 2);
        }
    }

    // --- sequence -----------------------------------------------------------------------------------------
    void tick(ServerLevel l) {
        if (phase == OFF) {
            return;
        }
        long t = l.getGameTime() - start;
        var r = l.random;
        Vec3 c = center();
        switch (phase) {
            case BOOT -> {
                for (int i = 0; i < lights.size(); i++) {
                    if (t == 8 + i * 7L) {
                        lamp(l, lights.get(i), true);
                        l.playSound(null, lights.get(i), ModSounds.OUTPOST_SPARK.get(), SoundSource.BLOCKS, 0.6F, 1.4F);
                    }
                }
                if (t >= BOOT_T) {
                    for (BlockPos p : lights) {
                        lamp(l, p, true);
                    }
                    setPhase(l, WARNING);
                }
            }
            case WARNING -> {
                if (t % 10 == 0) {
                    for (BlockPos p : warn) {
                        lamp(l, p, (t / 10) % 2 == 0);
                    }
                }
                if (t % 40 == 0) {
                    l.playSound(null, worldPosition, ModSounds.OUTPOST_ALARM.get(), SoundSource.BLOCKS, 3.0F, 1.0F);
                }
                if (t % 6 == 0 && !frame.isEmpty()) {
                    BlockPos f = frame.get(r.nextInt(frame.size()));
                    l.sendParticles(ParticleTypes.ELECTRIC_SPARK, f.getX() + 0.5, f.getY() + 0.5, f.getZ() + 0.5, 8, 0.4, 0.4, 0.4, 0.2);
                    if (r.nextInt(3) == 0) {
                        l.playSound(null, f, ModSounds.OUTPOST_SPARK.get(), SoundSource.BLOCKS, 1.0F, 0.8F + r.nextFloat() * 0.5F);
                    }
                }
                if (!lights.isEmpty() && r.nextInt(14) == 0) {
                    BlockPos p = lights.get(r.nextInt(lights.size()));
                    lamp(l, p, false);
                } else if (t % 7 == 0) {
                    for (BlockPos p : lights) {
                        lamp(l, p, true);
                    }
                }
                if (t >= WARN_T) {
                    for (BlockPos p : warn) {
                        lamp(l, p, true);
                    }
                    for (BlockPos p : lights) {
                        lamp(l, p, true);
                    }
                    setPhase(l, READY);
                    l.playSound(null, worldPosition, ModSounds.OUTPOST_BEEP.get(), SoundSource.BLOCKS, 2.0F, 0.6F);
                }
            }
            case READY -> {
                if (t % 60 == 0) {
                    l.playSound(null, button, ModSounds.OUTPOST_BEEP.get(), SoundSource.BLOCKS, 1.2F, 1.2F);
                }
            }
            case ACTIVATING -> {
                List<BlockPos> fr = new ArrayList<>(frame);
                fr.sort(Comparator.comparingInt(BlockPos::getY));
                for (int i = 0; i < fr.size(); i++) {
                    if (t == 10 + i * 4L) {
                        frame(l, fr.get(i), true);
                        l.playSound(null, fr.get(i), ModSounds.OUTPOST_SPARK.get(), SoundSource.BLOCKS, 1.2F, 0.6F + i * 0.05F);
                    }
                }
                if (t % 3 == 0 && fr.size() > 1) {
                    BlockPos a = fr.get(r.nextInt(fr.size())), b = fr.get(r.nextInt(fr.size()));
                    for (int k = 0; k <= 10; k++) {
                        double f = k / 10.0;
                        l.sendParticles(ParticleTypes.ELECTRIC_SPARK, a.getX() + 0.5 + (b.getX() - a.getX()) * f, a.getY() + 0.5 + (b.getY() - a.getY()) * f,
                                a.getZ() + 0.5 + (b.getZ() - a.getZ()) * f, 1, 0.1, 0.1, 0.1, 0.02);
                    }
                }
                if (!lights.isEmpty() && t % 2 == 0) {
                    for (BlockPos p : lights) {
                        lamp(l, p, r.nextInt(3) != 0);
                    }
                }
                if (t % 10 == 0) {
                    for (BlockPos p : warn) {
                        lamp(l, p, (t / 10) % 2 == 0);
                    }
                }
                if (t == 100) {
                    UndersideEvents.quake(l, c, 20, 70, 0.35F);
                }
                List<BlockPos> g = new ArrayList<>(gate);
                g.sort(Comparator.comparingInt(BlockPos::getY));
                for (int i = 0; i < g.size(); i++) {
                    if (t == 130 + i * 3L) {
                        l.setBlock(g.get(i), OutpostBlocks.GATEWAY.get().defaultBlockState().setValue(OutpostBlocks.Gateway.AXIS, axis), 3);
                    }
                }
                if (t >= ACT_T) {
                    for (BlockPos p : g) {
                        l.setBlock(p, OutpostBlocks.GATEWAY.get().defaultBlockState().setValue(OutpostBlocks.Gateway.AXIS, axis), 3);
                    }
                    for (BlockPos p : lights) {
                        lamp(l, p, true);
                    }
                    for (BlockPos p : warn) {
                        lamp(l, p, false);
                    }
                    l.sendParticles(ParticleTypes.FLASH, c.x, c.y, c.z, 2, 0, 0, 0, 0);
                    l.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y, c.z, 120, 1.2, 1.6, 1.2, 0.08);
                    l.playSound(null, c.x, c.y, c.z, ModSounds.GATEWAY_OPEN.get(), SoundSource.BLOCKS, 5.0F, 1.0F);
                    setPhase(l, ONLINE);
                    for (var pl : l.getEntitiesOfClass(net.minecraft.server.level.ServerPlayer.class, new net.minecraft.world.phys.AABB(c, c).inflate(32))) {
                        de.officerooms.quest.Milestones.grant(pl, "outpost_gateway");
                    }
                }
            }
            default -> {
            }
        }
    }

    // --- persistence --------------------------------------------------------------------------------------
    private static long[] pack(List<BlockPos> l) {
        return l.stream().mapToLong(BlockPos::asLong).toArray();
    }

    private static void unpack(long[] a, List<BlockPos> l) {
        l.clear();
        for (long v : a) {
            l.add(BlockPos.of(v));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag t) {
        t.putInt("Phase", phase);
        t.putLong("Start", start);
        t.putLongArray("Monitors", pack(monitors));
        t.putLongArray("Lights", pack(lights));
        t.putLongArray("Warn", pack(warn));
        t.putLongArray("Frame", pack(frame));
        t.putLongArray("Gate", pack(gate));
        t.putLong("Button", button.asLong());
        t.putString("Axis", axis.getName());
    }

    @Override
    public void load(CompoundTag t) {
        super.load(t);
        phase = t.getInt("Phase");
        start = t.getLong("Start");
        unpack(t.getLongArray("Monitors"), monitors);
        unpack(t.getLongArray("Lights"), lights);
        unpack(t.getLongArray("Warn"), warn);
        unpack(t.getLongArray("Frame"), frame);
        unpack(t.getLongArray("Gate"), gate);
        button = BlockPos.of(t.getLong("Button"));
        Direction.Axis a = Direction.Axis.byName(t.getString("Axis"));
        axis = a == null || a == Direction.Axis.Y ? Direction.Axis.X : a;
    }
}
