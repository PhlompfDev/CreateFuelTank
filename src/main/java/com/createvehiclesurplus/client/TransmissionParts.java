package com.createvehiclesurplus.client;

import com.createvehiclesurplus.CreateVehicleSurplus;
import com.createvehiclesurplus.content.transmission.Gear;
import com.createvehiclesurplus.content.transmission.TransmissionBlock;
import com.createvehiclesurplus.content.transmission.TransmissionBlockEntity;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

/**
 * The moving parts of the Transmission and where they sit: the layshaft rod and its cluster of
 * five gears in the channel along the body's (+p,+q) edge, and the sliding main-shaft gears, one
 * size per forward gear, that emerge from the body to mesh the selected cluster gear. All partials
 * are modelled along Z, centred on the block, like Create's half shaft, so {@code partialFacing}
 * / {@code rotateToFace(SOUTH, ...)} orient them and the instance position places them. The pixel
 * constants mirror {@code LAYOUT} in tools/gen_transmission_assets.py. Created before models
 * bake: {@link #init()} is called from client setup, as Create does with AllPartialModels.
 */
public final class TransmissionParts {
    public static final PartialModel LAY_ROD = PartialModel.of(CreateVehicleSurplus.rl("block/transmission/lay_rod"));
    public static final PartialModel LAY_REVERSE = PartialModel.of(CreateVehicleSurplus.rl("block/transmission/lay_reverse"));
    /** Cluster gears and sliders by size: 1/4, 1/2, 3/4, 1:1. Reverse shares the 1/4 size. */
    public static final PartialModel[] LAY = {
            PartialModel.of(CreateVehicleSurplus.rl("block/transmission/lay_1")),
            PartialModel.of(CreateVehicleSurplus.rl("block/transmission/lay_2")),
            PartialModel.of(CreateVehicleSurplus.rl("block/transmission/lay_3")),
            PartialModel.of(CreateVehicleSurplus.rl("block/transmission/lay_4"))};
    public static final PartialModel[] SLIDER = {
            PartialModel.of(CreateVehicleSurplus.rl("block/transmission/slider_1")),
            PartialModel.of(CreateVehicleSurplus.rl("block/transmission/slider_2")),
            PartialModel.of(CreateVehicleSurplus.rl("block/transmission/slider_3")),
            PartialModel.of(CreateVehicleSurplus.rl("block/transmission/slider_4"))};

    /** Layshaft centre, p and q, in pixels. */
    public static final float LAY_CENTRE = 12.75f;
    /** Where the slider parks in Neutral: under the socket zone, inside the body. */
    public static final float NEUTRAL_A = 8f;
    /** Slot of each gear along the shaft, in pixels from the negative end; index = Gear.index(). */
    public static final float[] SLOT_A = {2.0f, NEUTRAL_A, 4.0f, 11.0f, 12.6f, 14.2f};
    /** Cluster gears turn opposite to the slider; this phase interleaves the teeth where speeds match. */
    public static final float MESH_OFFSET = 22.5f;
    /** Engagement slots: forward gears 0..3, then reverse. */
    public static final int SLOTS = TransmissionBlockEntity.ENGAGEMENT_SLOTS;
    public static final int REVERSE_SLOT = SLOTS - 1;

    private TransmissionParts() {
    }

    public static void init() {
        // Loads the class, which creates the partials above.
    }

    /** Engagement slot of a gear, or -1 for neutral. */
    public static int slotOf(Gear gear) {
        return TransmissionBlockEntity.engagementSlot(gear);
    }

    /** The gear an engagement slot belongs to. */
    public static Gear gearOfSlot(int slot) {
        return slot == REVERSE_SLOT ? Gear.REVERSE : Gear.byIndex(slot + 2);
    }

    /** Partial size index (0..3) of a gear's cluster gear and slider; reverse uses the 1/4 size. */
    public static int sizeOf(Gear gear) {
        return gear == Gear.REVERSE ? 0 : gear.index() - 2;
    }

    /** The shaft end the rotation comes in through: the source's side, or the negative end while unpowered. */
    public static Direction inputEnd(TransmissionBlockEntity be) {
        BlockState state = be.getBlockState();
        Axis axis = state.getValue(TransmissionBlock.AXIS);
        if (be.hasSource()) {
            Direction facing = be.getSourceFacing();
            if (facing.getAxis() == axis)
                return facing;
        }
        return Direction.get(AxisDirection.NEGATIVE, axis);
    }

    /** Offset from the block centre, in blocks, of a point on the main shaft {@code a} pixels from the negative end. */
    public static Vector3f onShaft(Axis axis, float a) {
        return world(axis, 0, 0, a - 8).div(16);
    }

    /** Offset from the block centre, in blocks, of a point on the layshaft {@code a} pixels from the negative end. */
    public static Vector3f onLayshaft(Axis axis, float a) {
        return world(axis, LAY_CENTRE - 8, LAY_CENTRE - 8, a - 8).div(16);
    }

    /** Where a slider sits, {@code engagement} of the way from its Neutral park to its gear's slot. */
    public static float sliderA(Gear gear, float engagement) {
        return NEUTRAL_A + (SLOT_A[gear.index()] - NEUTRAL_A) * engagement;
    }

    /** (p, q) cross-section coordinates and a along the shaft into world coordinates, as the asset generator does. */
    private static Vector3f world(Axis axis, float p, float q, float a) {
        return switch (axis) {
            case X -> new Vector3f(a, p, q);
            case Y -> new Vector3f(p, a, q);
            case Z -> new Vector3f(p, q, a);
        };
    }
}
