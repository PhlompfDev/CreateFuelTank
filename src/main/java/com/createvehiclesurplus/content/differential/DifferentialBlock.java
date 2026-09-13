package com.createvehiclesurplus.content.differential;

import com.createvehiclesurplus.VehicleSurplusBlockEntities;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

/**
 * A Gearbox whose outputs all turn the way the input does.
 * <p>
 * Same shape as Create's Gearbox: {@code AXIS} is the free axis (no shaft), the four faces on
 * the other two axes carry shafts. Create's Gearbox mirrors rotation around corners and
 * reverses the straight-through output, which is exactly wrong for two wheels on one axle;
 * the {@link DifferentialBlockEntity} answers Create's rotation propagator with a ratio of 1
 * on every face instead.
 */
public class DifferentialBlock extends RotatedPillarKineticBlock implements IBE<DifferentialBlockEntity> {

    public DifferentialBlock(Properties properties) {
        super(properties);
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.PUSH_ONLY;
    }

    /**
     * Flat (free axis up, four horizontal shafts) is what a vehicle wants, so that is the
     * default. Sneaking makes the clicked face's axis the free one: sneak-place against a wall
     * for the vertical orientation. A wrench rotates it afterwards as with any Create block.
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Player player = context.getPlayer();
        Axis axis = player != null && player.isShiftKeyDown() ? context.getClickedFace().getAxis() : Axis.Y;
        return defaultBlockState().setValue(AXIS, axis);
    }

    // IRotate

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() != state.getValue(AXIS);
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    // IBE

    @Override
    public Class<DifferentialBlockEntity> getBlockEntityClass() {
        return DifferentialBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends DifferentialBlockEntity> getBlockEntityType() {
        return VehicleSurplusBlockEntities.DIFFERENTIAL.get();
    }
}
