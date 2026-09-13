package com.createfueltank.compat.cdg;

import com.createfueltank.compat.FuelConsumer;
import com.jesz.createdieselgenerators.CDGRegistries;
import com.jesz.createdieselgenerators.content.diesel_engine.IEngine;
import com.jesz.createdieselgenerators.fuel_type.FuelType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;

/**
 * Create Diesel Generators engines (normal, modular and huge). All three implement {@link IEngine}
 * and hand out their whole tank for a {@code null} capability side.
 * <p>
 * Their tank has no validator of its own, so the fuel test mirrors {@code IEngine#validFS}: a
 * fluid is fuel iff CDG's datapack registry has a FuelType for it.
 */
public class CdgEngineConsumer implements FuelConsumer {
    @Override
    public boolean matches(BlockEntity be) {
        return be instanceof IEngine;
    }

    @Override
    public boolean acceptsFuel(Level level, BlockPos pos, BlockEntity be, Fluid fluid) {
        HolderLookup.RegistryLookup<FuelType> registry = level.registryAccess().lookupOrThrow(CDGRegistries.FUEL_TYPE);
        return FuelType.getTypeFor(registry, fluid) != FuelType.EMPTY;
    }
}
