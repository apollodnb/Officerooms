package de.officerooms.item;

import de.officerooms.core.ModSounds;
import de.officerooms.dimension.OfficeRoomsDimension;
import de.officerooms.underside.UndersideEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 0.19.0: the six artifacts (one in every megastructure), the Underside Key crafted from them and the Noclip Sword
 * dropped by [Unknown].
 */
public final class UndersideItems {

    /** Order: Poolrooms, Neighbourhood, Maze, Hotel, Industrial Complex, Level Fun. */
    public static final String[] ARTIFACTS = {"chlorine_pearl", "faded_photograph", "thread_of_the_maze", "room_key_000", "humming_fuse", "party_smile_badge",
            "you_are_here_pin", "captains_compass"};   // 0.43.0: Dead Mall, SS Fun =)
    public static final List<RegistryObject<Item>> ARTIFACT_ITEMS = new ArrayList<>();
    public static final RegistryObject<Item> UNDERSIDE_KEY;
    public static final RegistryObject<Item> NOCLIP_SWORD;

    static {
        for (String a : ARTIFACTS) {
            ARTIFACT_ITEMS.add(ModItems.ITEMS.register(a, () -> new Lore(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), a)));
        }
        UNDERSIDE_KEY = ModItems.ITEMS.register("underside_key", () -> new Key(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
        NOCLIP_SWORD = ModItems.ITEMS.register("noclip_sword", () -> new NoclipSword(new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    }

    private UndersideItems() {
    }

    public static void init() {
    }

    static class Lore extends Item {
        private final String key;

        Lore(Properties p, String name) {
            super(p);
            this.key = "item.office_rooms." + name + ".desc";
        }

        @Override
        public boolean isFoil(ItemStack s) {
            return true;
        }

        @Override
        public void appendHoverText(ItemStack s, Level l, List<Component> tip, TooltipFlag f) {
            tip.add(Component.translatable(key).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }

    static final class Key extends Lore {
        Key(Properties p) {
            super(p, "underside_key");
        }

        @Override
        public InteractionResult useOn(UseOnContext c) {
            Level level = c.getLevel();
            BlockPos pos = c.getClickedPos();
            Player player = c.getPlayer();
            if (level.dimension() != OfficeRoomsDimension.LEVEL_KEY || !level.getBlockState(pos).is(Blocks.BEDROCK) || pos.getY() > OfficeRoomsDimension.MIN_Y + 1) {
                if (player != null && !level.isClientSide) {
                    player.displayClientMessage(Component.literal("It only fits the bedrock beneath the lowest floor of the Office Rooms."), true);
                }
                return InteractionResult.FAIL;
            }
            if (level instanceof ServerLevel sl) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos q = pos.offset(dx, 0, dz);
                        if (sl.getBlockState(q).is(Blocks.BEDROCK)) {
                            sl.setBlock(q, Blocks.AIR.defaultBlockState(), 3);
                        }
                    }
                }
                sl.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 80, 1, 0.3, 1, 0.05);
                sl.playSound(null, pos, ModSounds.BEDROCK_CRACK.get(), SoundSource.BLOCKS, 3.0F, 1.0F);
                UndersideEvents.quake(sl, Vec3.atCenterOf(pos), 24, 30, 0.6F);
                if (player != null) {
                    player.displayClientMessage(Component.literal("The bedrock gives way. Something is down there."), true);
                    if (!player.getAbilities().instabuild) {
                        c.getItemInHand().shrink(1);
                    }
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    /** Right-click: walk through walls (not floors) for 10 s, then 30 s cooldown. */
    public static final class NoclipSword extends SwordItem {
        NoclipSword(Properties p) {
            super(Tiers.NETHERITE, 4, -2.4F, p);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack s = player.getItemInHand(hand);
            if (de.officerooms.level0.Level0Events.blocksNoclip(player)) {
                if (!level.isClientSide) {
                    player.displayClientMessage(Component.literal("The walls here are too thick. Reality will not bend."), true);
                }
                return InteractionResultHolder.fail(s);
            }
            if (player.getCooldowns().isOnCooldown(this)) {
                return InteractionResultHolder.pass(s);
            }
            player.getPersistentData().putLong(UndersideEvents.NOCLIP, level.getGameTime() + 200);
            player.getCooldowns().addCooldown(this, 800);
            if (!level.isClientSide) {
                level.playSound(null, player.blockPosition(), ModSounds.NOCLIP_ON.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                ((ServerLevel) level).sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1, player.getZ(), 40, 0.4, 0.8, 0.4, 0.05);
            }
            return InteractionResultHolder.sidedSuccess(s, level.isClientSide);
        }

        @Override
        public void appendHoverText(ItemStack s, Level l, List<Component> tip, TooltipFlag f) {
            tip.add(Component.translatable("item.office_rooms.noclip_sword.desc").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }
}
