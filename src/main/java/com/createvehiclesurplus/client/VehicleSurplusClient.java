package com.createvehiclesurplus.client;

import com.createvehiclesurplus.client.ponder.VehicleSurplusPonderPlugin;
import net.createmod.ponder.foundation.PonderIndex;

/**
 * Client-only setup. Only ever touched behind a {@code Dist.CLIENT} check, so none of the client
 * classes it references are loaded on a dedicated server.
 */
public class VehicleSurplusClient {

    public static void init() {
        // Same hook Create uses (CreateClient): plugins are queried when the Ponder index is built.
        PonderIndex.addPlugin(new VehicleSurplusPonderPlugin());
    }
}
