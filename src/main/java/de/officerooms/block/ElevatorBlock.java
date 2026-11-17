package de.officerooms.block;

import de.officerooms.world.Elevators;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 0.9.0: Elevator floor plate. Stand on it and jump to ride up, sneak to ride down. Right-click the plate
 * to choose how many floors one ride covers (1, 5, 10, 25). Works in the generated shafts of the Office
 * Rooms and between plates a player stacks above each other anywhere.
 * 0.26.0: in the generated shafts a right-click opens the floor panel instead ({@link de.officerooms.world.ElevatorRides}).
 */
public class ElevatorBlock extends Block {

    public ElevatorBlock(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            if (!de.officerooms.world.ElevatorRides.openPanel(sp, pos)) {
                Elevators.cycleStep(sp);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
