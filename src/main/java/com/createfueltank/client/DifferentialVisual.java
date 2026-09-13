package com.createfueltank.client;

import com.createfueltank.content.differential.DifferentialBlockEntity;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.AbstractInstance;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.model.Models;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Flywheel visual: four half shafts, one per face on the two shaft axes, all spinning at the
 * block's own speed. Create's GearboxVisual flips the sign per face to match the Gearbox's
 * mirroring; here the speed passes through unchanged, matching what the block entity tells
 * the rotation propagator.
 */
public class DifferentialVisual extends KineticBlockEntityVisual<DifferentialBlockEntity> {

    private final EnumMap<Direction, RotatingInstance> shafts = new EnumMap<>(Direction.class);

    public DifferentialVisual(VisualizationContext context, DifferentialBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);

        Direction.Axis freeAxis = blockState.getValue(BlockStateProperties.AXIS);
        var instancer = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.SHAFT_HALF));

        for (Direction direction : Iterate.directions) {
            Direction.Axis axis = direction.getAxis();
            if (axis == freeAxis)
                continue;

            RotatingInstance instance = instancer.createInstance();
            instance.setup(blockEntity, axis, blockEntity.getSpeed())
                    .setPosition(getVisualPosition())
                    .rotateToFace(Direction.SOUTH, direction)
                    .setChanged();
            shafts.put(direction, instance);
        }
    }

    @Override
    public void update(float partialTick) {
        for (Map.Entry<Direction, RotatingInstance> entry : shafts.entrySet())
            entry.getValue()
                    .setup(blockEntity, entry.getKey().getAxis(), blockEntity.getSpeed())
                    .setChanged();
    }

    @Override
    public void updateLight(float partialTick) {
        relight(shafts.values().toArray(FlatLit[]::new));
    }

    @Override
    protected void _delete() {
        shafts.values().forEach(AbstractInstance::delete);
        shafts.clear();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        shafts.values().forEach(consumer);
    }
}
