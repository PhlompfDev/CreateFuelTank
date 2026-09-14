package com.createvehiclesurplus.client;

import com.createvehiclesurplus.CreateVehicleSurplus;
import com.createvehiclesurplus.content.transmission.Role;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.Util;

import java.util.EnumMap;
import java.util.Map;

/**
 * Partial models drawn by {@link TransmissionRenderer}, all modelled on the north face with +Y as
 * the face's up direction. They must be created before models bake, so {@link #init()} is called
 * from client setup (Create does the same with AllPartialModels).
 */
public final class TransmissionPartials {
    /** The role marking beside the sockets (▲, A, ▼, N). */
    public static final Map<Role, PartialModel> GLYPHS = Util.make(new EnumMap<>(Role.class), map -> {
        for (Role role : Role.VALUES)
            map.put(role, PartialModel.of(CreateVehicleSurplus.rl("block/transmission/glyph_" + role.id())));
    });
    /** The hexagonal gear wheel (an OBJ mesh), one gear label per side. */
    public static final PartialModel WHEEL = PartialModel.of(CreateVehicleSurplus.rl("block/transmission/wheel"));

    private TransmissionPartials() {
    }

    public static void init() {
        // Loads the class, which creates the partials above.
    }
}
