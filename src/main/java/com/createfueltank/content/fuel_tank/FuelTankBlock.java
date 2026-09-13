package com.createfueltank.content.fuel_tank;

import com.createfueltank.FuelTankBlockEntities;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Create's Fluid Tank with one extra blockstate property: {@code powered}.
 * <p>
 * Everything a Fluid Tank does (multiblock forming, windows, wrench, bucket interaction,
 * comparator output, mounted storage on contraptions) is inherited unchanged. The only
 * behavioural addition lives in {@link FuelTankBlockEntity}: the controller pushes fuel into
 * adjacent Diesel Generators engines unless any block of the tank is powered.
 * <p>
 * Fuel Tanks and Fluid Tanks never merge into one multiblock: Create's ConnectivityHandler
 * groups by block entity type, and this block has its own.
 */
public class FuelTankBlock extends FluidTankBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public FuelTankBlock(Properties properties) {
        super(properties, false);
        registerDefaultState(defaultBlockState().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null)
            return null;
        return state.setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (level.isClientSide)
            return;

        boolean powered = level.hasNeighborSignal(pos);
        if (state.getValue(POWERED) != powered) {
            // Flag 2: sync to clients, no further neighbour updates. Same block => block entity survives.
            level.setBlock(pos, state.setValue(POWERED, powered), 2);
        }

        // Any neighbour change (redstone, an engine placed or removed next to us) invalidates
        // the controller's engine cache. Cheap: it just sets a flag, the rescan happens on tick.
        getBlockEntityOptional(level, pos)
                .map(FluidTankBlockEntity::getControllerBE)
                .ifPresent(controller -> {
                    if (controller instanceof FuelTankBlockEntity fuelTank)
                        fuelTank.markSurroundingsDirty();
                });
    }

    @Override
    public BlockEntityType<? extends FluidTankBlockEntity> getBlockEntityType() {
        return FuelTankBlockEntities.FUEL_TANK.get();
    }
}
