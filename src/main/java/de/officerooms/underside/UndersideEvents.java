package de.officerooms.underside;

import de.officerooms.block.ModBlocks;
import de.officerooms.block.UndersidePortalBlock;
import de.officerooms.core.ModSounds;
import de.officerooms.core.OfficeRoomsIds;
import de.officerooms.dimension.OfficeRoomsDimension;
import de.officerooms.entity.ModEntities;
import de.officerooms.hazard.HazardNet;
import de.officerooms.item.UndersideItems;
import de.officerooms.megastructure.MegaDecorator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 0.19.0: falling through the opened bedrock into the Underside, the 20-second wait and the quake before [Unknown]
 * breaks out of the meadow, the reward when it dies (Noclip Sword + two portals) and the Noclip Sword's effect.
 */
public final class UndersideEvents {

    public static final ResourceKey<Level> LEVEL_KEY = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(OfficeRoomsIds.MOD_ID, "underside"));
    public static final String NOCLIP = "office_rooms_noclip_until";
    static final int WAIT = 400;      // 20 s in the Underside before it comes
    static final int QUAKE = 90;      // the last 4.5 s shake

    private static final Map<UUID, Long> ARRIVED = new HashMap<>();
    private static Unknown active;
    private static long introStart = -1;
    private static Vec3 emergence;
    private static int emptyTicks;

    private UndersideEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.register(UndersideEvents.class);
    }

    // --- getting there --------------------------------------------------------------------------------------
    /** Puts the player under the upside-down meadow, falling slowly, and starts the 20-second clock. */
    public static void enter(ServerPlayer p, boolean soon) {
        ServerLevel ul = p.server.getLevel(LEVEL_KEY);
        if (ul == null) {
            return;
        }
        int x = Mth.floor(p.getX()), z = Mth.floor(p.getZ());
        for (int k = 0; k < 12 && UndersideTerrain.hangingAt(x, z); k++) {
            x += 3;
        }
        double y = UndersideTerrain.ceil(x, z) - 2.2;
        p.fallDistance = 0;
        p.setDeltaMovement(Vec3.ZERO);
        p.teleportTo(ul, x + 0.5, y, z + 0.5, p.getYRot(), 30F);
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 140, 0, false, false));
        p.displayClientMessage(Component.literal("You fell through the floor of the world."), true);
        arrived(p, soon);
    }

    static void arrived(ServerPlayer p, boolean soon) {
        if (active == null || !active.isAlive()) {
            ARRIVED.put(p.getUUID(), p.serverLevel().getGameTime() - (soon ? WAIT - QUAKE - 20 : 0));
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getTo() == LEVEL_KEY && e.getEntity() instanceof ServerPlayer p && !ARRIVED.containsKey(p.getUUID())) {
            arrived(p, false);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && p.level().dimension() == LEVEL_KEY) {
            arrived(p, false);
        }
    }

    // --- per player: the fall, noclip ------------------------------------------------------------------------
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent e) {
        if (e.getEntity() instanceof Player p && noclip(p)) {
            p.noPhysics = true;
        }
    }

    public static boolean noclip(Player p) {
        long until = p.getPersistentData().getLong(NOCLIP);
        return until != 0 && p.level().getGameTime() < until;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (false) {
            return;
        }
        Player p = e.getEntity();
        long until = p.getPersistentData().getLong(NOCLIP);
        if (until != 0) {
            long left = until - p.level().getGameTime();
            if (left > 0) {
                if (p.level().isClientSide) {
                    floorClamp(p);
                } else if (left % 20 == 0) {
                    p.displayClientMessage(Component.literal("Noclip: " + left / 20 + " s"), true);
                }
            } else {
                p.getPersistentData().remove(NOCLIP);
                p.noPhysics = false;
                if (p instanceof ServerPlayer sp) {
                    unstick(sp);
                    sp.level().playSound(null, sp.blockPosition(), ModSounds.NOCLIP_OFF.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
                    sp.displayClientMessage(Component.literal("Noclip faded."), true);
                }
            }
        }
        if (p instanceof ServerPlayer sp && sp.level().dimension() == OfficeRoomsDimension.LEVEL_KEY && sp.getY() < OfficeRoomsDimension.MIN_Y - 8) {
            enter(sp, false);
        }
    }

    /** Noclip goes through walls, not floors: stop on any block top the feet passed this tick (client side). */
    private static void floorClamp(Player p) {
        if (p.getDeltaMovement().y > 0 && p.getY() > p.yo) {
            p.setOnGround(false);
            return;
        }
        double best = Double.NEGATIVE_INFINITY;
        double y = p.getY(), yo = Math.max(p.yo, y);
        for (double ox : new double[]{-0.29, 0.29}) {
            for (double oz : new double[]{-0.29, 0.29}) {
                for (int by = Mth.floor(y - 0.01); by <= Mth.floor(yo); by++) {
                    BlockPos bp = BlockPos.containing(p.getX() + ox, by, p.getZ() + oz);
                    BlockState st = p.level().getBlockState(bp);
                    var shape = st.getCollisionShape(p.level(), bp);
                    if (shape.isEmpty()) {
                        continue;
                    }
                    double top = by + shape.max(Direction.Axis.Y);
                    if (top <= yo + 1e-4 && top >= y - 1e-4 && top > best) {
                        best = top;
                    }
                }
            }
        }
        if (best > Double.NEGATIVE_INFINITY) {
            p.setPos(p.getX(), best, p.getZ());
            p.setDeltaMovement(p.getDeltaMovement().multiply(1, 0, 1));
            p.setOnGround(true);
            p.fallDistance = 0;
        } else {
            p.setOnGround(false);
        }
    }

    private static void unstick(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        BlockPos c = p.blockPosition();
        if (free(l, c)) {
            return;
        }
        for (int r = 1; r <= 6; r++) {
            for (int dy = 0; dy <= 2; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        BlockPos q = c.offset(dx, dy, dz);
                        if (Math.max(Math.abs(dx), Math.abs(dz)) == r && free(l, q)) {
                            p.teleportTo(q.getX() + 0.5, q.getY(), q.getZ() + 0.5);
                            return;
                        }
                    }
                }
            }
        }
    }

    private static boolean free(ServerLevel l, BlockPos q) {
        return l.getBlockState(q).getCollisionShape(l, q).isEmpty() && l.getBlockState(q.above()).getCollisionShape(l, q.above()).isEmpty()
                && !l.getBlockState(q.below()).getCollisionShape(l, q.below()).isEmpty();
    }

    // --- the boss -------------------------------------------------------------------------------------------
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        if (false) {
            return;
        }
        MinecraftServer server = e.getServer();
        ServerLevel ul = server.getLevel(LEVEL_KEY);
        if (ul == null) {
            return;
        }
        var players = ul.players().stream().filter(p -> !p.isSpectator()).toList();
        if (players.isEmpty()) {
            if (++emptyTicks > 100) {
                if (active != null && !active.isRemoved()) {
                    active.discard();
                }
                active = null;
                introStart = -1;
                ARRIVED.clear();
            }
            return;
        }
        emptyTicks = 0;
        if (active != null && (active.isRemoved() || active.level() != ul)) {
            active = null;
        }
        long now = ul.getGameTime();
        if (active != null) {
            return;
        }
        if (introStart < 0) {
            for (ServerPlayer p : players) {
                Long t = ARRIVED.get(p.getUUID());
                if (t != null && now - t >= WAIT - QUAKE) {
                    introStart = now;
                    double a = ul.random.nextDouble() * Math.PI * 2;
                    double ex = p.getX() + Math.cos(a) * 11, ez = p.getZ() + Math.sin(a) * 11;
                    emergence = new Vec3(ex, UndersideTerrain.floor(Mth.floor(ex), Mth.floor(ez)) + 1, ez);
                    quake(ul, emergence, 64, QUAKE + 30, 0.35F);
                    ul.playSound(null, emergence.x, emergence.y, emergence.z, ModSounds.UNDERSIDE_QUAKE.get(), SoundSource.HOSTILE, 6.0F, 0.7F);
                    break;
                }
            }
            return;
        }
        long t = now - introStart;
        if (t % 5 == 0) {
            double r = 0.5 + t / 18.0;
            ul.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()), emergence.x, emergence.y + 0.1, emergence.z,
                    (int) (6 + t / 4), r, 0.05, r, 0.2);
            ul.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GRASS_BLOCK.defaultBlockState()), emergence.x, emergence.y + 0.1, emergence.z,
                    (int) (3 + t / 8), r, 0.05, r, 0.2);
            ul.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState()), emergence.x,
                    UndersideTerrain.ceil(Mth.floor(emergence.x), Mth.floor(emergence.z)) - 0.1, emergence.z, 4, r * 2, 0.05, r * 2, 0.1);
        }
        if (t == 45) {
            quake(ul, emergence, 64, 60, 0.7F);
            ul.playSound(null, emergence.x, emergence.y, emergence.z, ModSounds.UNDERSIDE_QUAKE.get(), SoundSource.HOSTILE, 6.0F, 0.55F);
        }
        if (t >= QUAKE) {
            Unknown u = ModEntities.UNKNOWN.get().create(ul);
            if (u == null) {
                return;
            }
            u.moveTo(emergence.x, emergence.y - 7, emergence.z, 0, -90);
            ServerPlayer p = players.get(0);
            Vec3 dir = p.position().subtract(emergence).multiply(1, 0, 1);
            u.startIntro(dir.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : dir.normalize().scale(-1));
            ul.addFreshEntity(u);
            active = u;
            introStart = -1;
            ARRIVED.clear();
            quake(ul, emergence, 80, 40, 1.0F);
            ul.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()), emergence.x, emergence.y, emergence.z, 160, 2, 0.6, 2, 0.5);
            ul.sendParticles(ParticleTypes.EXPLOSION_EMITTER, emergence.x, emergence.y, emergence.z, 1, 0, 0, 0, 0);
        }
    }

    public static void quake(ServerLevel l, Vec3 at, double radius, int ticks, float strength) {
        for (ServerPlayer p : l.players()) {
            double d = p.position().distanceTo(at);
            if (d < radius) {
                HazardNet.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> p),
                        new HazardNet.Quake(ticks, strength * (float) (1 - 0.6 * d / radius)));
            }
        }
    }

    // --- reward ---------------------------------------------------------------------------------------------
    static void defeated(ServerLevel l, Unknown u) {
        active = null;
        BlockPos g = u.groundBelow();
        Player near = l.getNearestPlayer(u, 64);
        if (near != null && near.distanceToSqr(u) > 25 * 25) {
            g = BlockPos.containing(near.getX(), UndersideTerrain.floor(near.getBlockX(), near.getBlockZ()) + 1, near.getZ());
        }
        ItemEntity sword = new ItemEntity(l, g.getX() + 0.5, g.getY() + 1.0, g.getZ() + 0.5, new ItemStack(UndersideItems.NOCLIP_SWORD.get()));
        sword.setGlowingTag(true);
        sword.setUnlimitedLifetime();
        l.addFreshEntity(sword);
        portal(l, g.offset(-6, 0, -1), false);
        portal(l, g.offset(5, 0, -1), true);
        for (ServerPlayer p : l.players()) {
            p.displayClientMessage(Component.literal("[Unknown] sinks back into the meadow. Two doors open."), true);
        }
    }

    /** A 2 × 3 portal in a 4 × 5 frame (wallpaper for the Office, mossy stone for the Overworld), facing south. */
    private static void portal(ServerLevel l, BlockPos at, boolean overworld) {
        int fy = UndersideTerrain.floor(at.getX(), at.getZ()) + 1;
        for (int dx = 0; dx <= 1; dx++) {
            fy = Math.max(fy, UndersideTerrain.floor(at.getX() + dx, at.getZ()) + 1);
        }
        BlockState frame = overworld ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : ModBlocks.MAZE_WALLPAPER.get().defaultBlockState();
        BlockState gate = ModBlocks.UNDERSIDE_PORTAL.get().defaultBlockState().setValue(UndersidePortalBlock.OVERWORLD, overworld);
        for (int dx = -3; dx <= 4; dx++) {
            for (int dz = -3; dz <= 4; dz++) {
                for (int dy = 0; dy <= 7; dy++) {
                    l.setBlock(new BlockPos(at.getX() + dx, fy + dy, at.getZ() + dz), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        for (int dx = -1; dx <= 2; dx++) {
            for (int dy = -1; dy <= 3; dy++) {
                boolean edge = dx == -1 || dx == 2 || dy == -1 || dy == 3;
                l.setBlock(new BlockPos(at.getX() + dx, fy + dy, at.getZ()), edge ? frame : gate, 2);
            }
        }
        MegaDecorator.sign(l, new BlockPos(at.getX(), fy + 3, at.getZ() + 1), Direction.SOUTH, "", overworld ? "OVERWORLD" : "BACK TO", overworld ? "" : "THE OFFICE", "");
        l.sendParticles(ParticleTypes.REVERSE_PORTAL, at.getX() + 1, fy + 1.5, at.getZ() + 0.5, 60, 1, 1.5, 0.3, 0.05);
    }
}
