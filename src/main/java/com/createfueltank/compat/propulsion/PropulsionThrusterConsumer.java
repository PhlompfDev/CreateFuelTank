package com.createfueltank.compat.propulsion;

import com.createfueltank.compat.FuelConsumer;
import com.createfueltank.compat.FuelConsumers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.Set;

/**
 * Create Propulsion: Simulated's liquid-fuelled thrusters: the Thruster and the Liquid Vector
 * Thruster. (The ion, creative and solid-fuel thrusters take no fluid.)
 * <p>
 * Matched by block entity registry id rather than class: Propulsion moved these classes between
 * packages across releases, and the tank only needs two facts that are stable. Both thrusters
 * answer a {@code null} capability side with their fuel tank (a multiblock Thruster forwards to
 * its controller), and that tank carries Propulsion's own validator, so a simulated fill is an
 * exact "would this burn" test that also covers fuels CDG has never heard of.
 */
public class PropulsionThrusterConsumer implements FuelConsumer {
    private static final Set<ResourceLocation> THRUSTER_TYPES = Set.of(
            ResourceLocation.fromNamespaceAndPath(FuelConsumers.PROPULSION_ID, "thruster_block_entity"),
            ResourceLocation.fromNamespaceAndPath(FuelConsumers.PROPULSION_ID, "liquid_vector_thruster_block_entity"));

    @Override
    public boolean matches(BlockEntity be) {
        ResourceLocation id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType());
        return id != null && THRUSTER_TYPES.contains(id);
    }

    @Override
    public boolean acceptsFuel(Level level, BlockPos pos, BlockEntity be, Fluid fluid) {
        IFluidHandler tank = getTank(level, pos, be);
        if (tank == null)
            return false;
        // Compare against what is already inside first: a full tank of the same fuel would
        // otherwise read as "rejects", which is harmless but muddles the goggle readout.
        for (int i = 0; i < tank.getTanks(); i++) {
            FluidStack held = tank.getFluidInTank(i);
            if (!held.isEmpty() && held.getFluid() == fluid)
                return true;
        }
        return tank.fill(new FluidStack(fluid, 1), IFluidHandler.FluidAction.SIMULATE) > 0;
    }
}
