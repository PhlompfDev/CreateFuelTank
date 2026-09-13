package com.createfueltank;

import com.createfueltank.client.FuelTankModel;
import com.createfueltank.content.fuel_tank.FuelTankBlock;
import com.createfueltank.content.fuel_tank.FuelTankItem;
import com.simibubi.create.AllMountedStorageTypes;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorageType;
import com.simibubi.create.content.fluids.tank.FluidTankMovementBehavior;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.client.renderer.RenderType;

public class FuelTankBlocks {
    private static final CreateRegistrate REGISTRATE = CreateFuelTank.REGISTRATE;

    public static final BlockEntry<FuelTankBlock> FUEL_TANK = REGISTRATE.block("fuel_tank", FuelTankBlock::new)
            .initialProperties(SharedProperties::copperMetal)
            .properties(p -> p.noOcclusion().isRedstoneConductor((state, level, pos) -> true))
            // Connected-texture model with our own sprites; same culling rules as Create's tank.
            .onRegister(CreateRegistrate.blockModel(() -> FuelTankModel::new))
            // On a Create contraption (train, bearing, ...) the tank is plain fluid storage.
            // Create's own mounted storage accepts any FluidTankBlockEntity, which we are.
            .transform(MountedFluidStorageType.mountedFluidStorage(AllMountedStorageTypes.FLUID_TANK))
            .onRegister(MovementBehaviour.movementBehaviour(new FluidTankMovementBehavior()))
            .addLayer(() -> RenderType::cutoutMipped)
            .item(FuelTankItem::new)
            .build()
            .register();

    public static void register() {
        // Forces class init so the Registrate entries above are queued before registry events fire.
    }
}
