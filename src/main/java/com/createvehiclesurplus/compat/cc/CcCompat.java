package com.createvehiclesurplus.compat.cc;

import com.createvehiclesurplus.VehicleSurplusBlockEntities;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * CC: Tweaked integration. Only ever reached behind {@code ModList.isLoaded("computercraft")},
 * so none of CC's classes load when the mod is absent.
 */
public final class CcCompat {
    public static final String MOD_ID = "computercraft";

    private CcCompat() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(CcCompat::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // Any face, adjacent computer or wired modem. A new wrapper per query is fine: CC compares with equals().
        event.registerBlockEntity(PeripheralCapability.get(), VehicleSurplusBlockEntities.TRANSMISSION.get(),
                (be, side) -> new TransmissionPeripheral(be));
    }
}
