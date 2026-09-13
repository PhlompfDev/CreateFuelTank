package com.createfueltank.compat;

import com.createfueltank.compat.cdg.CdgEngineConsumer;
import com.createfueltank.compat.propulsion.PropulsionThrusterConsumer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** The machines a Fuel Tank knows how to feed, in the order they are matched. */
public final class FuelConsumers {
    public static final String PROPULSION_ID = "createpropulsion";

    private static final List<FuelConsumer> ALL = build();

    private FuelConsumers() {
    }

    public static List<FuelConsumer> all() {
        return ALL;
    }

    /** The consumer kind for this block entity, or null if the Fuel Tank should ignore it. */
    @Nullable
    public static FuelConsumer of(@Nullable BlockEntity be) {
        if (be == null)
            return null;
        for (FuelConsumer consumer : ALL)
            if (consumer.matches(be))
                return consumer;
        return null;
    }

    private static List<FuelConsumer> build() {
        // Diesel Generators is a hard dependency; Create Propulsion is optional and matched by
        // registry id only, so nothing here loads a Propulsion class.
        if (ModList.get().isLoaded(PROPULSION_ID))
            return List.of(new CdgEngineConsumer(), new PropulsionThrusterConsumer());
        return List.of(new CdgEngineConsumer());
    }
}
