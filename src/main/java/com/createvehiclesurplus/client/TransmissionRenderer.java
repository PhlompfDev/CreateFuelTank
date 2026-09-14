package com.createvehiclesurplus.client;

import com.createvehiclesurplus.content.link.SidedLinkBehaviour;
import com.createvehiclesurplus.content.transmission.Role;
import com.createvehiclesurplus.content.transmission.TransmissionBlock;
import com.createvehiclesurplus.content.transmission.TransmissionBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import com.simibubi.create.content.kinetics.transmission.SplitShaftRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxRenderer;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Draws what the static model can't: the frequency items in the sockets, the role glyphs and the
 * gear drum (both depend on the {@code roles} property, which blockstate rotations can't express
 * around a horizontal shaft). Then hands over to Create's split-shaft renderer, which draws the
 * two half shafts only when Flywheel is off.
 */
public class TransmissionRenderer extends SplitShaftRenderer {
    private static final float DRUM_X = 2.5f / 16;
    private static final float DRUM_Z = 2.5f / 16;

    public TransmissionRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(SplitShaftBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (be instanceof TransmissionBlockEntity transmission && be.getBlockState().getBlock() instanceof TransmissionBlock) {
            renderFrequencies(transmission, ms, buffer, light, overlay);
            renderRoleParts(transmission, partialTicks, ms, buffer, light);
        }
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
    }

    private static void renderFrequencies(TransmissionBlockEntity be, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        BlockState state = be.getBlockState();
        for (Role role : Role.VALUES) {
            SidedLinkBehaviour link = be.link(role);
            if (link == null)
                continue;
            for (boolean first : Iterate.trueAndFalse) {
                ItemStack stack = link.getFrequency(first).getStack();
                if (stack.isEmpty())
                    continue;
                ms.pushPose();
                link.getSlot(first).transform(be.getLevel(), be.getBlockPos(), state, ms);
                ValueBoxRenderer.renderItemIntoValueBox(stack, ms, buffer, light, overlay);
                ms.popPose();
            }
        }
    }

    private static void renderRoleParts(TransmissionBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light) {
        BlockState state = be.getBlockState();
        VertexConsumer consumer = buffer.getBuffer(RenderType.cutoutMipped());
        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(canonicalToBlock(state));
        ms.translate(-0.5, -0.5, -0.5);
        CachedBuffers.partial(TransmissionPartials.GLYPHS, state).light(light).renderInto(ms, consumer);

        ms.translate(DRUM_X, 0, DRUM_Z);
        ms.mulPose(Axis.YP.rotationDegrees(-be.drumAngle(partialTicks)));
        ms.translate(-DRUM_X, 0, -DRUM_Z);
        CachedBuffers.partial(TransmissionPartials.DRUM, state).light(light).renderInto(ms, consumer);
        ms.popPose();
    }

    /** Rotation taking the canonical frame (Up north, Analog east, axis Y) onto this block's roles. */
    static Quaternionf canonicalToBlock(BlockState state) {
        Vector3f up = normal(TransmissionBlock.faceOf(state, Role.UP));
        Vector3f analog = normal(TransmissionBlock.faceOf(state, Role.ANALOG));
        Vector3f third = new Vector3f(up).cross(analog);
        Vector3f canonicalUp = normal(Direction.NORTH);
        Vector3f canonicalAnalog = normal(Direction.EAST);
        Vector3f canonicalThird = new Vector3f(canonicalUp).cross(canonicalAnalog);
        // Columns are the frame's axes; for orthonormal frames the rotation is target * source^T.
        Matrix3f target = new Matrix3f(up, analog, third);
        Matrix3f source = new Matrix3f(canonicalUp, canonicalAnalog, canonicalThird);
        return new Quaternionf().setFromNormalized(target.mul(source.transpose()));
    }

    private static Vector3f normal(Direction direction) {
        return new Vector3f(direction.getStepX(), direction.getStepY(), direction.getStepZ());
    }
}
