package de.officerooms.item;

import de.officerooms.core.OfficeRoomsIds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.ForgeTier;
import net.minecraftforge.common.TierSortingRegistry;

import java.util.List;

/**
 * Werkzeugstufe „Multidimensional“: etwa 20 % über Netherite.
 *
 * <pre>
 *              Netherite   Multidimensional
 * Haltbarkeit   2031        2437
 * Abbautempo    9,0         10,8
 * Schadensbonus 4,0         4,6
 * Verzauberbar  15          18
 * </pre>
 *
 * Abbaustufe 4 (wie Netherite) und im Forge-Stufenregister oberhalb von Netherite einsortiert.
 * Reparatur im Amboss mit Glitched Ingots.
 */
public final class MultidimensionalTier {

    public static final ForgeTier TIER = new ForgeTier(4, 2437, 10.8F, 4.6F, 18,
            BlockTags.create(new ResourceLocation(OfficeRoomsIds.MOD_ID, "needs_multidimensional_tool")),
            () -> Ingredient.of(ModItems.GLITCHED_INGOT.get()));

    private MultidimensionalTier() {
    }

    public static void register() {
        TierSortingRegistry.registerTier(TIER, new ResourceLocation(OfficeRoomsIds.MOD_ID, "multidimensional"),
                List.of(Tiers.NETHERITE), List.of());
    }
}
