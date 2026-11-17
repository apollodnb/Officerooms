package de.officerooms.level0;

import de.officerooms.OfficeRooms;
import de.officerooms.block.UndersidePortalBlock;
import de.officerooms.core.ModSounds;
import de.officerooms.core.OfficeRoomsIds;
import de.officerooms.dimension.OfficeRoomsDimension;
import de.officerooms.entity.Flayed;
import de.officerooms.entity.ModEntities;
import de.officerooms.item.ModItems;
import de.officerooms.worldgen.OfficeChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.UUID;

/**
 * 0.21.0: Level 0. Entry by breaking through the topmost ceiling of the Office Rooms; no mining, building or teleport tricks inside;
 * exactly one immortal Flayed hunting whoever is there; escape through the Manila Room water passage, which sends
 * you to a random office far from where you came in and grants one reward per trip.
 */
public final class Level0Events {

    public static final ResourceKey<Level> LEVEL_KEY = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(OfficeRoomsIds.MOD_ID, "level_0"));
    static final String TICKET = "office_rooms_l0_ticket", FROM_X = "office_rooms_l0_x", FROM_Z = "office_rooms_l0_z", DRY = "office_rooms_l0_dry";
    public static final String IMMORTAL = "office_rooms.immortal";

    private static UUID hunter;
    private static int grace = -1, away, empty, stuck;
    private static double lastBest = Double.MAX_VALUE;

    private Level0Events() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(Level0Events.class);
    }

    static boolean inLevel0(Entity e) {
        return e.level().dimension() == LEVEL_KEY;
    }

    // --- way in -----------------------------------------------------------------------------------------------
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBreak(BlockEvent.BreakEvent e) {
        Player p = e.getPlayer();
        if (!(e.getLevel() instanceof ServerLevel l)) {
            return;
        }
        if (l.dimension() == LEVEL_KEY) {
            if (!p.isCreative()) {
                e.setCanceled(true);
            }
            return;
        }
        if (l.dimension() != OfficeRoomsDimension.LEVEL_KEY || !(p instanceof ServerPlayer sp)
                || !(l.getChunkSource().getGenerator() instanceof OfficeChunkGenerator gen)) {
            return;
        }
        BlockPos pos = e.getPos();
        var planner = gen.sampler(l.getChunkSource().randomState(), l).planner();
        if (pos.getY() >= planner.y0 + planner.phys * planner.storeys) {
            e.setCanceled(true);                                            // the top ceiling of the Office Rooms stays intact
            enter(sp, pos.getX(), pos.getZ());
        }
    }

    /** Into Level 0, at a free cell near (x, z) on a random floor. */
    public static void enter(ServerPlayer p, int x, int z) {
        ServerLevel l0 = p.server.getLevel(LEVEL_KEY);
        if (l0 == null) {
            return;
        }
        long seed = l0.getSeed();
        BlockPos to = freeSpot(l0, seed, (int) (p.getRandom().nextFloat() * Level0Layout.FLOORS), x, z, 0, 9);
        if (to == null) {
            return;
        }
        p.getPersistentData().putBoolean(TICKET, true);
        p.getPersistentData().putInt(FROM_X, x);
        p.getPersistentData().putInt(FROM_Z, z);
        p.teleportTo(l0, to.getX() + 0.5, to.getY(), to.getZ() + 0.5, p.getYRot(), 0F);
        p.displayClientMessage(Component.literal("The ceiling gave way, and so did everything else."), true);
        l0.playSound(null, to, ModSounds.LAMP_HUM.get(), SoundSource.AMBIENT, 1.0F, 0.8F);
        if (hunter == null) {
            grace = 30;
        }
    }

    /** Centre of a free cell on floor k, searched in rings of cells around (x, z) from radius r0 to r1. */
    static BlockPos freeSpot(ServerLevel l, long seed, int k, int x, int z, int r0, int r1) {
        int gx0 = Math.floorDiv(x, Level0Layout.CELL), gz0 = Math.floorDiv(z, Level0Layout.CELL);
        for (int r = r0; r <= r1; r++) {
            for (int a = 0; a < 8 * Math.max(1, r); a++) {
                double ang = l.random.nextDouble() * Math.PI * 2;
                int gx = gx0 + (int) Math.round(Math.cos(ang) * r), gz = gz0 + (int) Math.round(Math.sin(ang) * r);
                byte t = Level0Layout.typeAt(seed, k, gx, gz);
                if (t == Level0Layout.BLOCKED || t == Level0Layout.MANILA || t == Level0Layout.THRESH) {
                    continue;
                }
                BlockPos c = new BlockPos(gx * Level0Layout.CELL + 3, Level0Layout.walk(k), gz * Level0Layout.CELL + 3);
                if (l.getBlockState(c).isAir() && l.getBlockState(c.above()).isAir()) {
                    return c;
                }
            }
        }
        return null;
    }

    // --- way out ----------------------------------------------------------------------------------------------
    static void escape(ServerPlayer p) {
        ServerLevel office = p.server.getLevel(OfficeRoomsDimension.LEVEL_KEY);
        if (office == null || p.isOnPortalCooldown()) {
            return;
        }
        p.setPortalCooldown();
        int fx = p.getPersistentData().getInt(FROM_X), fz = p.getPersistentData().getInt(FROM_Z);
        boolean sent = false;
        for (int a = 0; a < 6 && !sent; a++) {
            double ang = p.getRandom().nextDouble() * Math.PI * 2, d = 800 + p.getRandom().nextDouble() * 2400;
            sent = UndersidePortalBlock.sendToOffice(p, fx + (int) (Math.cos(ang) * d), fz + (int) (Math.sin(ang) * d));
        }
        if (!sent) {
            return;
        }
        p.clearFire();
        p.setAirSupply(p.getMaxAirSupply());
        if (p.getPersistentData().getBoolean(TICKET)) {
            p.getPersistentData().remove(TICKET);                            // one reward per trip
            de.officerooms.quest.Milestones.grant(p, "level0_escaped");
            ItemStack reward = switch (p.getRandom().nextInt(3)) {
                case 0 -> new ItemStack(ModItems.RESEARCH_FILE.get(), 3);
                case 1 -> new ItemStack(ModItems.MD_SWORD.get());
                default -> new ItemStack(ModItems.GLITCHED_INGOT.get(), 6);
            };
            if (!p.getInventory().add(reward)) {
                p.drop(reward, false);
            }
            p.displayClientMessage(Component.literal("You got out. You brought something with you: " + reward.getCount() + " × " + reward.getHoverName().getString()), false);
        } else {
            p.displayClientMessage(Component.literal("You got out."), true);
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    /** The Manila shaft (bx = bz = 3, two blocks deep) at this position, or null; returns the top block of the shaft. */
    static BlockPos shaftTop(long seed, BlockPos p) {
        int gx = Math.floorDiv(p.getX(), Level0Layout.CELL), gz = Math.floorDiv(p.getZ(), Level0Layout.CELL);
        int rx = Math.floorDiv(gx, Level0Layout.R), rz = Math.floorDiv(gz, Level0Layout.R);
        int[] m = Level0Layout.manila(seed, rx, rz);
        if (m == null) {
            return null;
        }
        int sx = (rx * Level0Layout.R + m[1]) * Level0Layout.CELL + 3, sz = (rz * Level0Layout.R + m[2]) * Level0Layout.CELL + 3, w = Level0Layout.walk(m[0]);
        return p.getX() == sx && p.getZ() == sz && p.getY() >= w - 2 && p.getY() <= w ? new BlockPos(sx, w - 1, sz) : null;
    }

    /** Emptying the bucket into the narrow hole fills the whole shaft (otherwise the water would only cover the bottom). */
    @SubscribeEvent
    public static void onBucket(net.minecraftforge.event.entity.player.FillBucketEvent e) {
        if (!(e.getLevel() instanceof ServerLevel l) || l.dimension() != LEVEL_KEY || !e.getEmptyBucket().is(net.minecraft.world.item.Items.WATER_BUCKET)
                || !(e.getTarget() instanceof net.minecraft.world.phys.BlockHitResult hit) || hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) {
            return;
        }
        BlockPos top = shaftTop(l.getSeed(), hit.getBlockPos().relative(hit.getDirection()));
        if (top == null) {
            top = shaftTop(l.getSeed(), hit.getBlockPos());
        }
        if (top == null) {
            return;
        }
        l.setBlock(top, net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), 11);
        l.playSound(null, top, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        e.setFilledBucket(new ItemStack(net.minecraft.world.item.Items.BUCKET));
        e.setResult(net.minecraftforge.eventbus.api.Event.Result.ALLOW);
    }

    // --- rules inside -----------------------------------------------------------------------------------------
    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent e) {
        if (e.getEntity() instanceof Player p && inLevel0(p) && !p.isCreative()) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate e) {
        if (e.getLevel().dimension() == LEVEL_KEY) {
            e.getAffectedBlocks().clear();
        }
    }

    @SubscribeEvent
    public static void onPearl(EntityTeleportEvent.EnderPearl e) {
        if (inLevel0(e.getEntity())) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onChorus(EntityTeleportEvent.ChorusFruit e) {
        if (inLevel0(e.getEntity())) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent e) {
        // 0.25.0: server only. Entity tags are not synced, so on the client the immortal Flayed looked like an
        // intruder and was cancelled: it hunted you invisibly.
        if (e.getLevel().isClientSide() || e.getLevel().dimension() != LEVEL_KEY || !(e.getEntity() instanceof Mob m)) {
            return;
        }
        if (m.getTags().contains(IMMORTAL)) {
            if (hunter == null) {
                hunter = m.getUUID();                                       // the one saved with the world: it is still here
            } else if (!m.getUUID().equals(hunter)) {
                e.setCanceled(true);                                        // never two
            }
            return;
        }
        e.setCanceled(true);                                                // nothing else lives here
    }

    @SubscribeEvent
    public static void onAttack(LivingAttackEvent e) {
        LivingEntity v = e.getEntity();
        if (v.getTags().contains(IMMORTAL) && !e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            e.setCanceled(true);
        }
    }

    // --- every second -----------------------------------------------------------------------------------------
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.getServer().getTickCount() % 20 != 0) {
            return;
        }
        ServerLevel l = e.getServer().getLevel(LEVEL_KEY);
        if (l == null) {
            return;
        }
        List<ServerPlayer> players = l.players().stream().filter(p -> !p.isSpectator()).toList();
        Entity f = hunter == null ? null : l.getEntity(hunter);
        if (players.isEmpty()) {
            if (++empty > 5) {
                if (f != null) {
                    f.discard();
                }
                hunter = null;
                grace = -1;
            }
            return;
        }
        empty = 0;
        for (ServerPlayer p : players) {
            if (!p.isCreative()) {
                p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, 4, false, false));
            }
            rescueFromShaft(l, p);
        }
        if (f == null || !f.isAlive()) {
            if (hunter != null && f == null && grace < 0) {
                grace = 0;                                                  // its chunk got unloaded: bring it back
            }
            if (grace < 0) {
                grace = 30;
            }
            if (--grace <= 0) {
                spawnHunter(l, players.get(l.random.nextInt(players.size())));
            }
            return;
        }
        ServerPlayer near = null;
        double best = Double.MAX_VALUE;
        for (ServerPlayer p : players) {
            double d = p.distanceToSqr(f);
            if (d < best) {
                best = d;
                near = p;
            }
        }
        boolean otherFloor = near != null && Level0Layout.floorOf(near.getY()) != Level0Layout.floorOf(f.getY());
        if (near != null && (best > 72 * 72 || otherFloor)) {
            if (++away > 20) {                                              // relentless: it finds you again
                BlockPos to = freeSpot(l, l.getSeed(), Level0Layout.floorOf(near.getY()), near.getBlockX(), near.getBlockZ(), 4, 7);
                if (to != null) {
                    f.teleportTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
                }
                away = 0;
            }
        } else {
            away = 0;
        }
        // its pathfinding gives up in long mazes: if it gets no closer for 8 s and cannot see anyone, it reappears nearby
        boolean sees = f instanceof LivingEntity le && le.hasLineOfSight(near);
        if (near != null && best > 3 * 3 && best > lastBest - 2 && (!sees || best > 6 * 6)) {
            if (++stuck >= 6) {
                BlockPos to = freeSpot(l, l.getSeed(), Level0Layout.floorOf(near.getY()), near.getBlockX(), near.getBlockZ(), 2, 3);
                if (to != null) {
                    f.teleportTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
                }
                stuck = 0;
            }
        } else {
            stuck = 0;
        }
        lastBest = best;
        if (f instanceof Mob m && near != null && !near.isCreative()) {
            m.setTarget(near);
        }
    }

    private static void spawnHunter(ServerLevel l, ServerPlayer p) {
        BlockPos to = freeSpot(l, l.getSeed(), Level0Layout.floorOf(p.getY()), p.getBlockX(), p.getBlockZ(), 5, 8);
        if (to == null) {
            return;
        }
        Flayed f = ModEntities.FLAYED.get().create(l);
        if (f == null) {
            return;
        }
        f.moveTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5, l.random.nextFloat() * 360F, 0F);
        f.addTag(IMMORTAL);
        f.setInvulnerable(true);
        f.setPersistenceRequired();
        var range = f.getAttribute(Attributes.FOLLOW_RANGE);
        if (range != null) {
            range.setBaseValue(96.0);
        }
        hunter = f.getUUID();
        l.addFreshEntity(f);
        OfficeRooms.LOGGER.info("Level 0: the immortal Flayed was released at {}", to.toShortString());
    }

    /** Someone who jumped into the dry shaft cannot climb out: after 8 s they are helped back up. */
    private static void rescueFromShaft(ServerLevel l, ServerPlayer p) {
        int k = Level0Layout.floorOf(p.getY() + 2);
        double depth = Level0Layout.walk(k) - p.getY();
        if (depth > 1.5 && depth < 2.5 && !p.isInWater() && p.onGround()) {
            int n = p.getPersistentData().getInt(DRY) + 1;
            p.getPersistentData().putInt(DRY, n);
            if (n >= 8) {
                p.teleportTo(p.getX() + 1.0, Level0Layout.walk(k), p.getZ() + 1.0);
                p.getPersistentData().remove(DRY);
                p.displayClientMessage(Component.literal("You claw your way back out. It is too narrow to go on dry."), true);
            }
        } else {
            p.getPersistentData().remove(DRY);
        }
    }

    /** Test helper: nearest Manila Room ("manila") or Threshold room ("threshold") as a walkable position. */
    public static BlockPos locate(long seed, int x, int z, boolean manila) {
        int rx0 = Math.floorDiv(Math.floorDiv(x, Level0Layout.CELL), Level0Layout.R), rz0 = Math.floorDiv(Math.floorDiv(z, Level0Layout.CELL), Level0Layout.R);
        for (int r = 0; r < 24; r++) {
            for (int rx = rx0 - r; rx <= rx0 + r; rx++) {
                for (int rz = rz0 - r; rz <= rz0 + r; rz++) {
                    if (Math.max(Math.abs(rx - rx0), Math.abs(rz - rz0)) != r) {
                        continue;
                    }
                    if (manila) {
                        int[] m = Level0Layout.manila(seed, rx, rz);
                        if (m != null) {
                            return new BlockPos((rx * Level0Layout.R + m[1]) * Level0Layout.CELL + 12, Level0Layout.walk(m[0]), (rz * Level0Layout.R + m[2]) * Level0Layout.CELL + 12);
                        }
                    } else {
                        for (int k = 1; k < Level0Layout.FLOORS; k++) {
                            int[] t = Level0Layout.thresh(seed, k, rx, rz);
                            if (t != null) {
                                return new BlockPos((rx * Level0Layout.R + t[0]) * Level0Layout.CELL + 1, Level0Layout.walk(k), (rz * Level0Layout.R + t[1]) * Level0Layout.CELL + 1);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /** The Noclip Sword does not work in Level 0. */
    public static boolean blocksNoclip(Player p) {
        return inLevel0(p);
    }
}
