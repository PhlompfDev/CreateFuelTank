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
 * Draws what the static model can't: the frequency items in the sockets, and on every long face
 * its role glyph and a gear wheel that rolls to the current gear. Both are partials modelled on
 * the north face and turned onto each face with that face's up direction, so they read upright
 * whatever the axis and role layout. Then hands over to Create's split-shaft renderer, which draws
 * the two half shafts only when Flywheel is off.
 */
public class TransmissionRenderer extends SplitShaftRenderer {
    // The wheel's axis in the partial's frame: along X, 8 px up, 6.16 px behind the block's north
    // boundary (a 7 px hexagon whose front side sits 0.1 px inside the boundary). Must match
    // tools/gen_transmission_assets.py.
    private static final float WHEEL_Y = 8f / 16;
    private static final float WHEEL_Z = (0.1f + 7f * (float) Math.sqrt(3) / 2) / 16;

    public TransmissionRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(SplitShaftBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (be instanceof TransmissionBlockEntity transmission && be.getBlockState().getBlock() instanceof TransmissionBlock) {
            renderFrequencies(transmission, ms, buffer, light, overlay);
            renderFaceParts(transmission, partialTicks, ms, buffer, light);
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

    private static void renderFaceParts(TransmissionBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light) {
        BlockState state = be.getBlockState();
        Direction.Axis axis = state.getValue(TransmissionBlock.AXIS);
        VertexConsumer consumer = buffer.getBuffer(RenderType.cutoutMipped());
        float wheelAngle = be.wheelAngle(partialTicks);
        for (Role role : Role.VALUES) {
            Direction face = TransmissionBlock.faceOf(state, role);
            ms.pushPose();
            ms.translate(0.5, 0.5, 0.5);
            ms.mulPose(northToFace(face, TransmissionBlock.faceUp(axis, face)));
            ms.translate(-0.5, -0.5, -0.5);
            CachedBuffers.partial(TransmissionPartials.GLYPHS.get(role), state).light(light).renderInto(ms, consumer);

            ms.translate(0, WHEEL_Y, WHEEL_Z);
            ms.mulPose(Axis.XP.rotationDegrees(wheelAngle));
            ms.translate(0, -WHEEL_Y, -WHEEL_Z);
            CachedBuffers.partial(TransmissionPartials.WHEEL, state).light(light).renderInto(ms, consumer);
            ms.popPose();
        }
    }

    /** Rotation taking the partials' frame (north face, up = +Y) onto {@code face} with up = {@code up}. */
    static Quaternionf northToFace(Direction face, Direction up) {
        Vector3f n = normal(face);
        Vector3f u = normal(up);
        Vector3f r = new Vector3f(u).cross(n);
        Vector3f n0 = normal(Direction.NORTH);
        Vector3f u0 = normal(Direction.UP);
        Vector3f r0 = new Vector3f(u0).cross(n0);
        // Columns are the frame's axes; for orthonormal frames the rotation is target * source^T.
        Matrix3f target = new Matrix3f(u, n, r);
        Matrix3f source = new Matrix3f(u0, n0, r0);
        return new Quaternionf().setFromNormalized(target.mul(source.transpose()));
    }

    private static Vector3f normal(Direction direction) {
        return new Vector3f(direction.getStepX(), direction.getStepY(), direction.getStepZ());
    }
}
