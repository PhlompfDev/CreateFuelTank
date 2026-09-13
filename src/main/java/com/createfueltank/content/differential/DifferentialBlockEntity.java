package com.createfueltank.content.differential;

import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The whole trick lives in {@link #getRotationSpeedModifier}: Create's
 * {@code RotationPropagator.getAxisModifier} asks a {@link SplitShaftBlockEntity} for the
 * ratio on each face, and 1 everywhere means every connected shaft gets the block's speed with
 * the block's sign. Two shafts on the same axis with the same signed speed turn the same way,
 * which is what a pair of wheels on an axle needs. (A Gearbox is the special case in that
 * method that mirrors corners and reverses the straight-through.)
 */
public class DifferentialBlockEntity extends SplitShaftBlockEntity {

    public DifferentialBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public float getRotationSpeedModifier(Direction face) {
        return 1;
    }

    /** Like the Gearbox: relays make no noise of their own. */
    @Override
    protected boolean isNoisy() {
        return false;
    }
}
