package com.createfueltank.content.long_fuel_tank;

import com.createfueltank.FuelTankBlockEntities;
import com.createfueltank.content.fuel_tank.FuelTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

/**
 * A Fuel Tank lying on its side: a 1x1 tube along a horizontal axis, up to
 * {@link LongFuelTankBlockEntity#MAX_LENGTH} blocks long, that chains end to end the way the
 * upright tank stacks. Built for thin airframes where the thruster sits at the end of a row.
 * <p>
 * The block reuses every upright model through log-style blockstate rotation, so the window
 * strip runs along the length. {@code top}/{@code bottom} mark the two ends (which end is
 * "top" follows the model's rotation, see {@link #upEnd}); {@code axis} is X or Z; {@code powered}
 * works like the upright tank's. Long tanks never merge with upright ones: separate block
 * entity type, and Create's ConnectivityHandler groups by type.
 */
public class LongFuelTankBlock extends FluidTankBlock {
    public static final EnumProperty<Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public LongFuelTankBlock(Properties properties) {
        super(properties, false);
        registerDefaultState(defaultBlockState().setValue(AXIS, Axis.Z).setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(AXIS, POWERED);
    }

    public static boolean isLongTank(BlockState state) {
        return state.getBlock() instanceof LongFuelTankBlock;
    }

    @Nullable
    public static Axis getAxis(BlockState state) {
        return isLongTank(state) ? state.getValue(AXIS) : null;
    }

    /**
     * The end of the tube the rotated model's "top" faces. Blockstate rotation {@code x=90}
     * (axis Z) points the model's up at north; {@code x=90,y=90} (axis X) points it east.
     */
    public static Direction upEnd(Axis axis) {
        return Direction.fromAxisAndDirection(axis, axis == Axis.X ? AxisDirection.POSITIVE : AxisDirection.NEGATIVE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null)
            return null;

        // Like an Item Vault: continue the row you click against, unless sneaking.
        Axis axis = context.getHorizontalDirection().getAxis();
        if (context.getPlayer() == null || !context.getPlayer().isShiftKeyDown()) {
            BlockState placedOn = context.getLevel().getBlockState(context.getClickedPos().relative(context.getClickedFace().getOpposite()));
            Axis preferred = getAxis(placedOn);
            if (preferred != null)
                axis = preferred;
        }
        return state.setValue(AXIS, axis)
                .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        FuelTankBlock.onNeighborChanged(this, state, level, pos);
    }

    @Override
    public BlockEntityType<? extends FluidTankBlockEntity> getBlockEntityType() {
        return FuelTankBlockEntities.LONG_FUEL_TANK.get();
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        Axis axis = state.getValue(AXIS);
        return state.setValue(AXIS, rotation.rotate(Direction.fromAxisAndDirection(axis, AxisDirection.POSITIVE)).getAxis());
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state;
    }
}
