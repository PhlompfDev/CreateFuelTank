package com.createfueltank;

import com.createfueltank.client.LongFuelTankRenderer;
import com.createfueltank.content.fuel_tank.FuelTankBlockEntity;
import com.createfueltank.content.long_fuel_tank.LongFuelTankBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankRenderer;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

public class FuelTankBlockEntities {
    private static final CreateRegistrate REGISTRATE = CreateFuelTank.REGISTRATE;

    public static final BlockEntityEntry<FuelTankBlockEntity> FUEL_TANK = REGISTRATE
            .blockEntity("fuel_tank", FuelTankBlockEntity::new)
            .validBlocks(FuelTankBlocks.FUEL_TANK)
            // Create's fluid tank renderer only needs a FluidTankBlockEntity; reuse it as-is.
            .renderer(() -> FluidTankRenderer::new)
            .register();

    public static final BlockEntityEntry<LongFuelTankBlockEntity> LONG_FUEL_TANK = REGISTRATE
            .blockEntity("long_fuel_tank", LongFuelTankBlockEntity::new)
            .validBlocks(FuelTankBlocks.LONG_FUEL_TANK)
            // Create's renderer draws the fluid column upward; the lying tank needs its own.
            .renderer(() -> LongFuelTankRenderer::new)
            .register();

    public static void register() {
        // Forces class init so the Registrate entries above are queued before registry events fire.
    }
}
