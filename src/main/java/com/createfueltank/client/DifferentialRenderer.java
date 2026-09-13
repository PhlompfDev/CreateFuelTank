package com.createfueltank.client;

import com.createfueltank.content.differential.DifferentialBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Fallback for when Flywheel's instancing backend is off: draws the four half shafts the
 * classic way, at the block's own speed on every face (see {@link DifferentialVisual}).
 */
public class DifferentialRenderer extends KineticBlockEntityRenderer<DifferentialBlockEntity> {

    public DifferentialRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(DifferentialBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (VisualizationManager.supportsVisualization(be.getLevel()))
            return;

        Axis freeAxis = be.getBlockState().getValue(BlockStateProperties.AXIS);
        BlockPos pos = be.getBlockPos();
        float time = AnimationTickHolder.getRenderTime(be.getLevel());

        for (Direction direction : Iterate.directions) {
            Axis axis = direction.getAxis();
            if (axis == freeAxis)
                continue;

            SuperByteBuffer shaft = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, be.getBlockState(), direction);
            float offset = getRotationOffsetForPosition(be, pos, axis);
            float angle = ((time * be.getSpeed() * 3f / 10) % 360 + offset) / 180f * (float) Math.PI;

            kineticRotationTransform(shaft, be, axis, angle, light);
            shaft.renderInto(ms, buffer.getBuffer(RenderType.solid()));
        }
    }
}
