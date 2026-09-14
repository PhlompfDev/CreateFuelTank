package com.createvehiclesurplus.gametest;

import com.createvehiclesurplus.CreateVehicleSurplus;
import com.createvehiclesurplus.VehicleSurplusBlocks;
import com.createvehiclesurplus.content.transmission.Gear;
import com.createvehiclesurplus.content.transmission.Role;
import com.createvehiclesurplus.content.transmission.ShiftRules;
import com.createvehiclesurplus.content.transmission.TransmissionBlock;
import com.createvehiclesurplus.content.transmission.TransmissionBlockEntity;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Rig: a Creative Motor (16 RPM) at the west end drives shaft -> Transmission -> shaft along X.
 * With AXIS = X and ROLES = 0 the role faces are Up = UP, Analog = SOUTH, Down = DOWN,
 * Neutral = NORTH (TransmissionBlock.ring(X) = UP, SOUTH, DOWN, NORTH).
 */
@GameTestHolder(CreateVehicleSurplus.ID)
@PrefixGameTestTemplate(false)
public class TransmissionGameTests {
    static final String TEMPLATE = "empty_5x3x5";

    static final BlockPos MOTOR = new BlockPos(0, 1, 1);
    static final BlockPos INPUT = new BlockPos(1, 1, 1);
    static final BlockPos BOX = new BlockPos(2, 1, 1);
    static final BlockPos OUTPUT = new BlockPos(3, 1, 1);
    static final BlockPos UP_FACE = new BlockPos(2, 2, 1);
    static final BlockPos ANALOG_FACE = new BlockPos(2, 1, 2);
    static final BlockPos DOWN_FACE = new BlockPos(2, 0, 1);
    static final BlockPos NEUTRAL_FACE = new BlockPos(2, 1, 0);

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void reverse_gear_ratio(GameTestHelper helper) { ratioTest(helper, Gear.REVERSE); }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void neutral_gear_ratio(GameTestHelper helper) { ratioTest(helper, Gear.NEUTRAL); }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void quarter_gear_ratio(GameTestHelper helper) { ratioTest(helper, Gear.QUARTER); }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void half_gear_ratio(GameTestHelper helper) { ratioTest(helper, Gear.HALF); }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void three_quarters_gear_ratio(GameTestHelper helper) { ratioTest(helper, Gear.THREE_QUARTERS); }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void direct_gear_ratio(GameTestHelper helper) { ratioTest(helper, Gear.DIRECT); }

    /** The ratio follows the drive direction: driven from the east, the west shaft is the output. */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void ratio_follows_drive_direction(GameTestHelper helper) {
        helper.setBlock(BOX, box(Gear.HALF));
        helper.setBlock(new BlockPos(1, 1, 1), shaft(Axis.X));
        helper.setBlock(new BlockPos(3, 1, 1), shaft(Axis.X));
        helper.setBlock(new BlockPos(4, 1, 1), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.WEST));
        helper.runAfterDelay(10, () -> {
            float in = speedAt(helper, new BlockPos(3, 1, 1));
            check(in != 0, "input shaft is not turning");
            checkSpeed(helper, new BlockPos(1, 1, 1), in * 0.5f);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void held_up_wire_shifts_once(GameTestHelper helper) {
        placeRig(helper, Gear.NEUTRAL);
        helper.runAfterDelay(10, () -> helper.setBlock(UP_FACE, Blocks.REDSTONE_BLOCK));
        helper.runAfterDelay(20, () -> {
            checkGear(helper, Gear.QUARTER);
            checkSpeed(helper, OUTPUT, speedAt(helper, INPUT) * 0.25f);
        });
        helper.runAfterDelay(40, () -> {
            checkGear(helper, Gear.QUARTER);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void down_wire_reaches_reverse(GameTestHelper helper) {
        placeRig(helper, Gear.NEUTRAL);
        helper.runAfterDelay(10, () -> helper.setBlock(DOWN_FACE, Blocks.REDSTONE_BLOCK));
        helper.runAfterDelay(20, () -> {
            checkGear(helper, Gear.REVERSE);
            checkSpeed(helper, OUTPUT, -speedAt(helper, INPUT));
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void neutral_wire_holds_neutral(GameTestHelper helper) {
        placeRig(helper, Gear.HALF);
        helper.runAfterDelay(10, () -> helper.setBlock(NEUTRAL_FACE, Blocks.REDSTONE_BLOCK));
        helper.runAfterDelay(20, () -> {
            checkGear(helper, Gear.NEUTRAL);
            checkSpeed(helper, OUTPUT, 0);
            ShiftRules.Result result = transmission(helper).requestGear(Gear.DIRECT);
            check(result.refusal() == ShiftRules.Refusal.NEUTRAL_HOLD, "expected neutral_hold, got " + result);
            helper.succeed();
        });
    }

    /** A redstone block gives 15, the top analog band. */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void analog_wire_sets_gear(GameTestHelper helper) {
        placeRig(helper, Gear.NEUTRAL);
        helper.runAfterDelay(10, () -> helper.setBlock(ANALOG_FACE, Blocks.REDSTONE_BLOCK));
        helper.runAfterDelay(20, () -> {
            checkGear(helper, Gear.DIRECT);
            helper.succeed();
        });
    }

    /** Rotating the roles once moves Up from the top face to the south face. */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void rotated_roles_move_the_up_face(GameTestHelper helper) {
        placeRig(helper, Gear.NEUTRAL);
        helper.runAfterDelay(5, () -> {
            BlockPos abs = helper.absolutePos(BOX);
            BlockState state = helper.getLevel().getBlockState(abs);
            ((TransmissionBlock) state.getBlock()).rotateRoles(helper.getLevel(), abs, state);
            check(TransmissionBlock.faceOf(helper.getLevel().getBlockState(abs), Role.UP) == Direction.SOUTH, "Up should face south after one rotation");
        });
        helper.runAfterDelay(10, () -> helper.setBlock(ANALOG_FACE, Blocks.REDSTONE_BLOCK));
        helper.runAfterDelay(20, () -> {
            checkGear(helper, Gear.QUARTER);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void requests_respect_cooldown(GameTestHelper helper) {
        placeRig(helper, Gear.NEUTRAL);
        helper.runAfterDelay(10, () -> {
            TransmissionBlockEntity be = transmission(helper);
            check(be.requestGear(Gear.HALF).accepted(), "first request should be accepted");
            ShiftRules.Result second = be.requestGear(Gear.DIRECT);
            check(second.refusal() == ShiftRules.Refusal.COOLDOWN, "expected cooldown, got " + second);
        });
        helper.runAfterDelay(20, () -> {
            check(transmission(helper).requestGear(Gear.DIRECT).accepted(), "request after the cooldown should be accepted");
            helper.succeed();
        });
    }

    /**
     * A turbocharged Diesel engine turns at 192 RPM. Every gear is a reduction, so shifting from R
     * all the way up to 1 must never trip Create's max-speed rule (which would break the block).
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void survives_every_shift_at_192_rpm(GameTestHelper helper) {
        placeRig(helper, Gear.REVERSE);
        helper.runAfterDelay(5, () -> {
            if (!(helper.getBlockEntity(MOTOR) instanceof CreativeMotorBlockEntity motor))
                throw new GameTestAssertException("no creative motor");
            motor.generatedSpeed.setValue(192);
        });
        for (int i = 0; i < 5; i++)
            helper.runAfterDelay(20 + i * 8, () -> check(transmission(helper).requestShift(true).accepted(), "shift up refused"));
        helper.runAfterDelay(80, () -> {
            check(helper.getBlockState(BOX).getBlock() instanceof TransmissionBlock, "the Transmission broke");
            checkGear(helper, Gear.DIRECT);
            check(Math.abs(speedAt(helper, INPUT)) == 192, "input is not 192 RPM: " + speedAt(helper, INPUT));
            checkSpeed(helper, OUTPUT, speedAt(helper, INPUT));
            helper.succeed();
        });
    }

    // ---- helpers (package-private: later test classes reuse them) ----

    static void ratioTest(GameTestHelper helper, Gear gear) {
        placeRig(helper, gear);
        helper.runAfterDelay(10, () -> {
            float in = speedAt(helper, INPUT);
            check(in != 0, "input shaft is not turning");
            checkSpeed(helper, OUTPUT, in * gear.ratio());
            helper.succeed();
        });
    }

    static void placeRig(GameTestHelper helper, Gear gear) {
        helper.setBlock(BOX, box(gear));
        helper.setBlock(INPUT, shaft(Axis.X));
        helper.setBlock(OUTPUT, shaft(Axis.X));
        helper.setBlock(MOTOR, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.EAST));
    }

    static BlockState box(Gear gear) {
        return VehicleSurplusBlocks.TRANSMISSION.getDefaultState()
                .setValue(BlockStateProperties.AXIS, Axis.X)
                .setValue(TransmissionBlock.ROLES, 0)
                .setValue(TransmissionBlock.GEAR, gear.index());
    }

    static BlockState shaft(Axis axis) {
        return AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS, axis);
    }

    static TransmissionBlockEntity transmission(GameTestHelper helper) {
        BlockEntity be = helper.getBlockEntity(BOX);
        if (!(be instanceof TransmissionBlockEntity transmission))
            throw new GameTestAssertException("no Transmission at " + BOX);
        return transmission;
    }

    static float speedAt(GameTestHelper helper, BlockPos pos) {
        BlockEntity be = helper.getBlockEntity(pos);
        if (!(be instanceof KineticBlockEntity kinetic))
            throw new GameTestAssertException("no kinetic block entity at " + pos);
        return kinetic.getSpeed();
    }

    static void checkSpeed(GameTestHelper helper, BlockPos pos, float expected) {
        float actual = speedAt(helper, pos);
        check(actual == expected, "shaft at " + pos + " turns at " + actual + ", expected " + expected);
    }

    static void checkGear(GameTestHelper helper, Gear expected) {
        Gear actual = transmission(helper).gear();
        check(actual == expected, "gear is " + actual + ", expected " + expected);
    }

    static void check(boolean condition, String message) {
        if (!condition)
            throw new GameTestAssertException(message);
    }
}
