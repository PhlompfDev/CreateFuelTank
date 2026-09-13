package com.createfueltank.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/**
 * A kind of block the Fuel Tank feeds: Diesel Generators engines, Create Propulsion thrusters, ...
 * <p>
 * One instance per kind, registered in {@link FuelConsumers}. The tank asks {@link #matches} while
 * scanning its neighbours and {@link #acceptsFuel} before pushing, so each kind can apply its own
 * notion of "is this fluid a fuel" and the tank never clogs a machine with water.
 */
public interface FuelConsumer {
    /** True if this block entity is a machine of this kind. */
    boolean matches(BlockEntity be);

    /** True if the machine at {@code pos} would burn {@code fluid}. Only called after {@link #matches}. */
    boolean acceptsFuel(Level level, BlockPos pos, BlockEntity be, net.minecraft.world.level.material.Fluid fluid);

    /**
     * The handler to push into. The default asks for the block's fluid capability with a {@code null}
     * side; every supported machine answers that with its whole internal tank regardless of where its
     * pipe port is, which is what lets the Fuel Tank sit on any face.
     */
    @Nullable
    default IFluidHandler getTank(Level level, BlockPos pos, BlockEntity be) {
        return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
    }
}
