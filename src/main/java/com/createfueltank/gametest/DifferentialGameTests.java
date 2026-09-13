package com.createfueltank.gametest;

import com.createfueltank.CreateFuelTank;
import com.createfueltank.FuelTankBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Differential relays rotation like a Gearbox but every output carries the input's sign.
 * <p>
 * Rig: a Creative Motor drives a shaft into the block from the west; shafts on the other
 * three faces are the outputs. The speeds are read straight off the shafts' kinetic block
 * entities once Create's rotation propagator has settled.
 */
@GameTestHolder(CreateFuelTank.ID)
@PrefixGameTestTemplate(false)
public class DifferentialGameTests {
    private static final String TEMPLATE = "empty_5x3x5";

    private static final BlockPos MOTOR = new BlockPos(0, 1, 1);
    private static final BlockPos INPUT = new BlockPos(1, 1, 1);
    private static final BlockPos BOX = new BlockPos(2, 1, 1);
    private static final BlockPos STRAIGHT = new BlockPos(3, 1, 1);
    private static final BlockPos NORTH = new BlockPos(2, 1, 0);
    private static final BlockPos SOUTH = new BlockPos(2, 1, 2);

    /** Flat orientation: three outputs, all equal to the input speed, sign included. */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void all_outputs_turn_like_the_input(GameTestHelper helper) {
        placeFlatRig(helper, FuelTankBlocks.DIFFERENTIAL.getDefaultState().setValue(BlockStateProperties.AXIS, Axis.Y));

        helper.runAfterDelay(10, () -> {
            float in = speedAt(helper, INPUT);
            helper.assertTrue(in != 0, "input shaft is not turning");
            assertSpeed(helper, STRAIGHT, in);
            assertSpeed(helper, NORTH, in);
            assertSpeed(helper, SOUTH, in);
            helper.succeed();
        });
    }

    /** The same rig with Create's Gearbox mirrors the corners, which proves the test above measures something. */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void gearbox_control_still_mirrors(GameTestHelper helper) {
        placeFlatRig(helper, AllBlocks.GEARBOX.getDefaultState().setValue(BlockStateProperties.AXIS, Axis.Y));

        helper.runAfterDelay(10, () -> {
            float in = speedAt(helper, INPUT);
            helper.assertTrue(in != 0, "input shaft is not turning");
            assertSpeed(helper, STRAIGHT, -in);
            helper.assertTrue(speedAt(helper, NORTH) == -speedAt(helper, SOUTH), "gearbox corners should turn opposite ways");
            helper.succeed();
        });
    }

    /** Free axis X: shafts up, down, north and south all follow the input coming from the north. */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void vertical_orientation_works(GameTestHelper helper) {
        BlockPos motor = new BlockPos(2, 1, 0);
        BlockPos input = new BlockPos(2, 1, 1);
        BlockPos box = new BlockPos(2, 1, 2);
        BlockPos up = new BlockPos(2, 2, 2);
        BlockPos down = new BlockPos(2, 0, 2);
        BlockPos through = new BlockPos(2, 1, 3);

        helper.setBlock(motor, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.SOUTH));
        helper.setBlock(input, shaft(Axis.Z));
        helper.setBlock(box, FuelTankBlocks.DIFFERENTIAL.getDefaultState().setValue(BlockStateProperties.AXIS, Axis.X));
        helper.setBlock(up, shaft(Axis.Y));
        helper.setBlock(down, shaft(Axis.Y));
        helper.setBlock(through, shaft(Axis.Z));

        helper.runAfterDelay(10, () -> {
            float in = speedAt(helper, input);
            helper.assertTrue(in != 0, "input shaft is not turning");
            assertSpeed(helper, up, in);
            assertSpeed(helper, down, in);
            assertSpeed(helper, through, in);
            helper.succeed();
        });
    }

    private static void placeFlatRig(GameTestHelper helper, BlockState box) {
        helper.setBlock(MOTOR, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.EAST));
        helper.setBlock(INPUT, shaft(Axis.X));
        helper.setBlock(BOX, box);
        helper.setBlock(STRAIGHT, shaft(Axis.X));
        helper.setBlock(NORTH, shaft(Axis.Z));
        helper.setBlock(SOUTH, shaft(Axis.Z));
    }

    private static BlockState shaft(Axis axis) {
        return AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS, axis);
    }

    private static float speedAt(GameTestHelper helper, BlockPos pos) {
        BlockEntity be = helper.getBlockEntity(pos);
        if (!(be instanceof KineticBlockEntity kinetic))
            throw new GameTestAssertException("no kinetic block entity at " + pos);
        return kinetic.getSpeed();
    }

    private static void assertSpeed(GameTestHelper helper, BlockPos pos, float expected) {
        float actual = speedAt(helper, pos);
        helper.assertTrue(actual == expected, "shaft at " + pos + " turns at " + actual + ", expected " + expected);
    }
}
