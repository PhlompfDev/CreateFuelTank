package com.createfueltank.gametest;

import com.createfueltank.CreateFuelTank;
import com.createfueltank.FuelTankBlockEntities;
import com.createfueltank.content.fuel_tank.FuelTankBlockEntity;
import com.jesz.createdieselgenerators.CDGBlocks;
import com.jesz.createdieselgenerators.CDGFluids;
import com.jesz.createdieselgenerators.content.diesel_engine.normal.DieselEngineBlockEntity;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Headless in-world tests, run with {@code gradlew.bat runGameTestServer}.
 * <p>
 * Each test builds a tiny rig inside the empty 5x3x5 template: a Fuel Tank, a Diesel Engine
 * next to it on a face that is NOT the engine's pipe port, and sometimes redstone. The engine
 * faces north by default, so its port is on its bottom; the tank always sits to its west.
 */
@GameTestHolder(CreateFuelTank.ID)
@PrefixGameTestTemplate(false)
public class FuelTankGameTests {
    private static final String TEMPLATE = "empty_5x3x5";
    private static final int FUEL_MB = 4000;

    private static final BlockPos TANK = new BlockPos(1, 1, 1);
    private static final BlockPos ENGINE = new BlockPos(2, 1, 1);
    private static final BlockPos REDSTONE = new BlockPos(0, 1, 1);

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void feeds_adjacent_engine(GameTestHelper helper) {
        placeEngineAndTank(helper, diesel(), FUEL_MB);

        helper.runAfterDelay(10, () -> {
            FluidTank engineTank = engineTank(helper, ENGINE);
            helper.assertTrue(!engineTank.isEmpty(), "engine received no fuel from the fuel tank");
            helper.assertTrue(engineTank.getFluid().getFluid() == diesel(), "engine holds the wrong fluid");
            helper.assertTrue(fuelTank(helper, TANK).getTankInventory().getFluidAmount() < FUEL_MB, "fuel tank did not lose what the engine gained");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void water_is_not_pushed(GameTestHelper helper) {
        placeEngineAndTank(helper, Fluids.WATER, FUEL_MB);

        helper.runAfterDelay(20, () -> {
            helper.assertTrue(engineTank(helper, ENGINE).isEmpty(), "water was pushed into the engine");
            helper.assertTrue(fuelTank(helper, TANK).getTankInventory().getFluidAmount() == FUEL_MB, "fuel tank lost water");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void redstone_stops_and_resumes_feed(GameTestHelper helper) {
        placeEngineAndTank(helper, diesel(), FUEL_MB);
        // Placed after the tank, so the tank learns about it through neighborChanged.
        helper.setBlock(REDSTONE, Blocks.REDSTONE_BLOCK);

        helper.runAfterDelay(20, () -> {
            helper.assertTrue(engineTank(helper, ENGINE).isEmpty(), "powered fuel tank still fed the engine");
            helper.assertTrue(fuelTank(helper, TANK).isRedstoneDisabled(), "controller did not notice the redstone signal");

            helper.setBlock(REDSTONE, Blocks.AIR);
            helper.runAfterDelay(20, () -> {
                helper.assertTrue(!engineTank(helper, ENGINE).isEmpty(), "feed did not resume after the signal dropped");
                helper.assertTrue(!fuelTank(helper, TANK).isRedstoneDisabled(), "controller still thinks it is powered");
                helper.succeed();
            });
        });
    }

    /** A 2x2 tank; the engine touches a block that is NOT the controller. */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void multiblock_feeds_from_any_part(GameTestHelper helper) {
        BlockPos engine = new BlockPos(3, 1, 2);
        helper.setBlock(engine, CDGBlocks.DIESEL_ENGINE.getDefaultState());
        for (int x = 1; x <= 2; x++)
            for (int z = 1; z <= 2; z++)
                helper.setBlock(new BlockPos(x, 1, z), com.createfueltank.FuelTankBlocks.FUEL_TANK.getDefaultState());

        // The multiblock forms on the first tick; fill the controller once it exists.
        helper.runAfterDelay(5, () -> {
            FluidTankBlockEntity part = ConnectivityHandler.partAt(FuelTankBlockEntities.FUEL_TANK.get(), helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 2)));
            helper.assertTrue(part != null, "no fuel tank at (2,1,2)");
            FluidTankBlockEntity controller = part.getControllerBE();
            helper.assertTrue(controller != null && controller.getWidth() == 2, "2x2 fuel tank did not form a multiblock");
            controller.getTankInventory().setFluid(new FluidStack(diesel(), FUEL_MB));

            helper.runAfterDelay(30, () -> {
                helper.assertTrue(!engineTank(helper, engine).isEmpty(), "engine next to a non-controller part was not fed");
                helper.succeed();
            });
        });
    }

    /** Registrate's getSource() is generic, which makes FluidStack's constructors ambiguous without the cast. */
    private static net.minecraft.world.level.material.Fluid diesel() {
        return (net.minecraft.world.level.material.Fluid) CDGFluids.DIESEL.getSource();
    }

    private static void placeEngineAndTank(GameTestHelper helper, net.minecraft.world.level.material.Fluid fluid, int amount) {
        helper.setBlock(ENGINE, CDGBlocks.DIESEL_ENGINE.getDefaultState());
        helper.setBlock(TANK, com.createfueltank.FuelTankBlocks.FUEL_TANK.getDefaultState());
        fuelTank(helper, TANK).getTankInventory().setFluid(new FluidStack(fluid, amount));
    }

    private static FuelTankBlockEntity fuelTank(GameTestHelper helper, BlockPos pos) {
        BlockEntity be = helper.getBlockEntity(pos);
        if (!(be instanceof FuelTankBlockEntity tank))
            throw new GameTestAssertException("no fuel tank block entity at " + pos);
        return tank;
    }

    private static FluidTank engineTank(GameTestHelper helper, BlockPos pos) {
        BlockEntity be = helper.getBlockEntity(pos);
        if (!(be instanceof DieselEngineBlockEntity engine))
            throw new GameTestAssertException("no diesel engine block entity at " + pos);
        return engine.getTank();
    }
}
