package com.createvehiclesurplus.content.transmission;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * One of a role's two frequency slots. They are stacked like a wall-mounted Redstone Link's: the
 * first 2.5 px above the face centre, the second 2.5 px below it, where "above" is the face's
 * {@link TransmissionBlock#faceUp up direction}; a quarter pixel outside the block where the brass
 * sockets end.
 */
public class TransmissionSlot extends ValueBoxTransform.Dual {
    private final Role role;

    public TransmissionSlot(boolean first, Role role) {
        super(first);
        this.role = role;
    }

    @Override
    public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof TransmissionBlock))
            return null;
        Direction face = TransmissionBlock.faceOf(state, role);
        Direction up = TransmissionBlock.faceUp(state.getValue(TransmissionBlock.AXIS), face);
        double upOffset = (isFirst() ? 2.5 : -2.5) / 16;
        return new Vec3(0.5, 0.5, 0.5)
                .add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.5 + 0.25 / 16))
                .add(Vec3.atLowerCornerOf(up.getNormal()).scale(upOffset));
    }

    @Override
    public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
        if (!(state.getBlock() instanceof TransmissionBlock))
            return;
        Direction face = TransmissionBlock.faceOf(state, role);
        // Same as Create's link slot: walls face outwards, top and bottom lie flat.
        float yRot = face.getAxis().isVertical() ? 0 : AngleHelper.horizontalAngle(face) + 180;
        float xRot = face == Direction.UP ? 90 : face == Direction.DOWN ? 270 : 0;
        TransformStack.of(ms).rotateYDegrees(yRot).rotateXDegrees(xRot);
    }

    @Override
    public void transform(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
        super.transform(level, pos, state, ms);
        ms.scale(0.75f, 0.75f, 0.75f);
    }

    @Override
    public float getScale() {
        return 0.4975f;
    }
}
