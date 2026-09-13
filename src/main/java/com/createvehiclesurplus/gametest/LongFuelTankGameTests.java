package com.createvehiclesurplus.gametest;

import com.createvehiclesurplus.CreateVehicleSurplus;
import com.createvehiclesurplus.VehicleSurplusBlockEntities;
import com.createvehiclesurplus.VehicleSurplusBlocks;
import com.createvehiclesurplus.content.long_fuel_tank.LongFuelTankBlock;
import com.createvehiclesurplus.content.long_fuel_tank.LongFuelTankBlockEntity;
import com.jesz.createdieselgenerators.CDGBlocks;
import com.jesz.createdieselgenerators.CDGFluids;
import com.jesz.createdieselgenerators.content.diesel_engine.normal.DieselEngineBlockEntity;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The lying tank: forms a row along X or Z, feeds a machine at the end of the row, respects
 * redstone on any block of the row, and never merges with a row on the other axis.
 */
@GameTestHolder(CreateVehicleSurplus.ID)
@PrefixGameTestTemplate(false)
public class LongFuelTankGameTests {
    private static final String TEMPLATE = "empty_5x3x5";

    /** A 3-long row along X at z=1 with the engine touching its east end. */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void row_along_x_feeds_engine_at_the_end(GameTestHelper helper) {
        BlockPos engine = new BlockPos(4, 1, 1);
        helper.setBlock(engine, CDGBlocks.DIESEL_ENGINE.getDefaultState());
        placeRow(helper, Axis.X, 1, 3);

        helper.runAfterDelay(5, () -> {
            LongFuelTankBlockEntity controller = controllerOf(helper, new BlockPos(3, 1, 1));
            helper.assertTrue(controller.getBlockPos().equals(helper.absolutePos(new BlockPos(1, 1, 1))), "controller is not the west end of the row");
            helper.assertTrue(controller.getHeight() == 3 && controller.getWidth() == 1, "row did not form a 1x1x3 tank, got " + controller.getWidth() + "x" + controller.getHeight());
            helper.assertTrue(controller.getTankInventory().getCapacity() == 3 * FluidTankBlockEntity.getCapacityMultiplier(), "capacity is not three blocks' worth");
            assertEndCaps(helper, Axis.X, 1, 3);

            controller.getTankInventory().setFluid(new FluidStack(diesel(), 4000));
            helper.runAfterDelay(30, () -> {
                helper.assertTrue(!engineTank(helper, engine).isEmpty(), "engine at the end of the row was not fed");
                helper.succeed();
            });
        });
    }

    /** Same along Z, engine at the south end, and the tank keeps its window strip. */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void row_along_z_feeds_engine_at_the_end(GameTestHelper helper) {
        BlockPos engine = new BlockPos(1, 1, 4);
        helper.setBlock(engine, CDGBlocks.DIESEL_ENGINE.getDefaultState());
        placeRow(helper, Axis.Z, 1, 3);

        helper.runAfterDelay(5, () -> {
            LongFuelTankBlockEntity controller = controllerOf(helper, new BlockPos(1, 1, 2));
            helper.assertTrue(controller.getHeight() == 3, "row along Z did not form");
            assertEndCaps(helper, Axis.Z, 1, 3);
            for (int i = 1; i <= 3; i++)
                helper.assertBlockProperty(new BlockPos(1, 1, i), FluidTankBlock.SHAPE, FluidTankBlock.Shape.WINDOW);

            controller.getTankInventory().setFluid(new FluidStack(diesel(), 4000));
            helper.runAfterDelay(30, () -> {
                helper.assertTrue(!engineTank(helper, engine).isEmpty(), "engine at the end of the Z row was not fed");
                helper.succeed();
            });
        });
    }

    /** Redstone against the middle block of the row stops the whole row. */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void redstone_on_middle_block_stops_row(GameTestHelper helper) {
        BlockPos engine = new BlockPos(4, 1, 1);
        helper.setBlock(engine, CDGBlocks.DIESEL_ENGINE.getDefaultState());
        placeRow(helper, Axis.X, 1, 3);

        helper.runAfterDelay(5, () -> {
            LongFuelTankBlockEntity controller = controllerOf(helper, new BlockPos(1, 1, 1));
            controller.getTankInventory().setFluid(new FluidStack(diesel(), 4000));
            helper.setBlock(new BlockPos(2, 2, 1), Blocks.REDSTONE_BLOCK);

            helper.runAfterDelay(25, () -> {
                helper.assertTrue(engineTank(helper, engine).isEmpty(), "powered row still fed the engine");
                helper.assertTrue(controller.isRedstoneDisabled(), "controller did not notice redstone on a middle block");
                helper.succeed();
            });
        });
    }

    /** A row along X touching a row along Z stays two tanks. */
    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void rows_on_different_axes_do_not_merge(GameTestHelper helper) {
        placeRow(helper, Axis.X, 1, 2);                        // (1,1,1) (2,1,1)
        helper.setBlock(new BlockPos(3, 1, 1), VehicleSurplusBlocks.LONG_FUEL_TANK.getDefaultState().setValue(LongFuelTankBlock.AXIS, Axis.Z));
        helper.setBlock(new BlockPos(3, 1, 2), VehicleSurplusBlocks.LONG_FUEL_TANK.getDefaultState().setValue(LongFuelTankBlock.AXIS, Axis.Z));

        helper.runAfterDelay(5, () -> {
            LongFuelTankBlockEntity xRow = controllerOf(helper, new BlockPos(2, 1, 1));
            LongFuelTankBlockEntity zRow = controllerOf(helper, new BlockPos(3, 1, 2));
            helper.assertTrue(xRow != zRow, "rows on different axes merged");
            helper.assertTrue(xRow.getHeight() == 2 && zRow.getHeight() == 2, "each row should be 2 long");
            helper.succeed();
        });
    }

    private static void placeRow(GameTestHelper helper, Axis axis, int from, int count) {
        for (int i = 0; i < count; i++) {
            BlockPos pos = axis == Axis.X ? new BlockPos(from + i, 1, 1) : new BlockPos(1, 1, from + i);
            helper.setBlock(pos, VehicleSurplusBlocks.LONG_FUEL_TANK.getDefaultState().setValue(LongFuelTankBlock.AXIS, axis));
        }
    }

    /** The model's "top" cap sits on the up-end of the row, the "bottom" cap on the other, middles have neither. */
    private static void assertEndCaps(GameTestHelper helper, Axis axis, int from, int count) {
        boolean upIsPositive = LongFuelTankBlock.upEnd(axis).getAxisDirection() == net.minecraft.core.Direction.AxisDirection.POSITIVE;
        for (int i = 0; i < count; i++) {
            BlockPos pos = axis == Axis.X ? new BlockPos(from + i, 1, 1) : new BlockPos(1, 1, from + i);
            boolean atMin = i == 0, atMax = i == count - 1;
            helper.assertBlockProperty(pos, FluidTankBlock.TOP, upIsPositive ? atMax : atMin);
            helper.assertBlockProperty(pos, FluidTankBlock.BOTTOM, upIsPositive ? atMin : atMax);
        }
    }

    private static LongFuelTankBlockEntity controllerOf(GameTestHelper helper, BlockPos part) {
        FluidTankBlockEntity be = ConnectivityHandler.partAt(VehicleSurplusBlockEntities.LONG_FUEL_TANK.get(), helper.getLevel(), helper.absolutePos(part));
        if (be == null)
            throw new GameTestAssertException("no long fuel tank at " + part);
        if (!(be.getControllerBE() instanceof LongFuelTankBlockEntity controller))
            throw new GameTestAssertException("long fuel tank at " + part + " has no controller");
        return controller;
    }

    private static net.minecraft.world.level.material.Fluid diesel() {
        return (net.minecraft.world.level.material.Fluid) CDGFluids.DIESEL.getSource();
    }

    private static net.neoforged.neoforge.fluids.capability.templates.FluidTank engineTank(GameTestHelper helper, BlockPos pos) {
        BlockEntity be = helper.getBlockEntity(pos);
        if (!(be instanceof DieselEngineBlockEntity engine))
            throw new GameTestAssertException("no diesel engine block entity at " + pos);
        return engine.getTank();
    }
}
