package com.createvehiclesurplus.client;

import com.createvehiclesurplus.CreateVehicleSurplus;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;

/**
 * Partial models drawn by {@link TransmissionRenderer}. They must be created before models bake,
 * so {@link #init()} is called from client setup (Create does the same with AllPartialModels).
 */
public final class TransmissionPartials {
    public static final PartialModel GLYPHS = PartialModel.of(CreateVehicleSurplus.rl("block/transmission/glyphs"));
    public static final PartialModel DRUM = PartialModel.of(CreateVehicleSurplus.rl("block/transmission/drum"));

    private TransmissionPartials() {
    }

    public static void init() {
        // Loads the class, which creates the partials above.
    }
}
