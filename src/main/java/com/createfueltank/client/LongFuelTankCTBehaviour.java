package com.createfueltank.client;

import com.createfueltank.content.long_fuel_tank.LongFuelTankBlock;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.fluids.tank.FluidTankCTBehaviour;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Connected textures for the lying tank. The models are the upright ones under log-style
 * blockstate rotation ({@code x=90} for axis Z, {@code x=90,y=90} for axis X), so two things
 * change compared with {@link FluidTankCTBehaviour}: the "top" sprites now sit on the faces
 * along the axis, and the side sprites' "up" runs along the axis. The direction bookkeeping is
 * Create's own {@code RotatedPillarCTBehaviour} recipe, which was written for exactly this
 * rotation scheme.
 */
public class LongFuelTankCTBehaviour extends FluidTankCTBehaviour {
    public LongFuelTankCTBehaviour() {
        super(FuelTankSpriteShifts.FUEL_TANK, FuelTankSpriteShifts.FUEL_TANK_TOP, FuelTankSpriteShifts.FUEL_TANK_INNER);
    }

    private static Axis axis(BlockState state) {
        Axis axis = LongFuelTankBlock.getAxis(state);
        return axis == null ? Axis.Z : axis;
    }

    @Override
    public CTSpriteShiftEntry getShift(BlockState state, Direction direction, @Nullable TextureAtlasSprite sprite) {
        // Faces along the axis are the model's top/bottom; everything else is a side.
        return super.getShift(state, direction.getAxis() == axis(state) ? Direction.UP : Direction.SOUTH, sprite);
    }

    @Override
    public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter reader, BlockPos pos, BlockPos otherPos, Direction face) {
        return state.getBlock() == other.getBlock()
                && LongFuelTankBlock.getAxis(other) == axis(state)
                && ConnectivityHandler.isConnected(reader, pos, otherPos);
    }

    @Override
    protected boolean reverseUVs(BlockState state, Direction face) {
        Axis axis = axis(state);
        if (axis == Axis.X)
            return face.getAxisDirection() == AxisDirection.NEGATIVE && face.getAxis() != Axis.X;
        return face != Direction.NORTH && face.getAxisDirection() != AxisDirection.POSITIVE;
    }

    @Override
    protected boolean reverseUVsVertically(BlockState state, Direction face) {
        Axis axis = axis(state);
        if (axis == Axis.X && face == Direction.NORTH)
            return false;
        if (axis == Axis.Z && face == Direction.WEST)
            return false;
        return super.reverseUVsVertically(state, face);
    }

    @Override
    protected Direction getUpDirection(BlockAndTintGetter reader, BlockPos pos, BlockState state, Direction face) {
        Axis axis = axis(state);
        boolean alongX = axis == Axis.X;
        if (face.getAxis().isVertical() && alongX)
            return super.getUpDirection(reader, pos, state, face).getClockWise();
        if (face.getAxis() != axis && !face.getAxis().isVertical())
            return Direction.fromAxisAndDirection(axis, alongX ? AxisDirection.POSITIVE : AxisDirection.NEGATIVE);
        return super.getUpDirection(reader, pos, state, face);
    }

    @Override
    protected Direction getRightDirection(BlockAndTintGetter reader, BlockPos pos, BlockState state, Direction face) {
        Axis axis = axis(state);
        if (face.getAxis().isVertical() && axis == Axis.X)
            return super.getRightDirection(reader, pos, state, face).getClockWise();
        if (face.getAxis() != axis && !face.getAxis().isVertical())
            return Direction.fromAxisAndDirection(Axis.Y, face.getAxisDirection());
        return super.getRightDirection(reader, pos, state, face);
    }
}
