package com.createfueltank.gametest;

import com.createfueltank.CreateFuelTank;
import com.createfueltank.FuelTankBlocks;
import com.createfueltank.compat.FuelConsumers;
import com.createfueltank.content.fuel_tank.FuelTankBlockEntity;
import com.jesz.createdieselgenerators.CDGFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Create Propulsion: Simulated coverage, run with {@code gradlew.bat runGameTestServer}.
 * <p>
 * Propulsion is not on the compile classpath (see {@code build.gradle}), so its blocks are
 * looked up by id and its tanks read through the fluid capability. Each test passes trivially
 * when the mod is absent from the runtime.
 */
@GameTestHolder(CreateFuelTank.ID)
@PrefixGameTestTemplate(false)
public class PropulsionGameTests {
    private static final String TEMPLATE = "empty_5x3x5";
    private static final int FUEL_MB = 4000;

    private static final BlockPos TANK = new BlockPos(1, 1, 1);
    private static final BlockPos THRUSTER = new BlockPos(2, 1, 1);

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void feeds_liquid_vector_thruster(GameTestHelper helper) {
        if (skipWithoutPropulsion(helper))
            return;
        placeThrusterAndTank(helper, "liquid_vector_thruster", diesel(), FUEL_MB);

        helper.runAfterDelay(10, () -> {
            helper.assertTrue(fuelIn(helper, THRUSTER) > 0, "liquid vector thruster received no fuel");
            helper.assertTrue(fuelTank(helper, TANK).getTankInventory().getFluidAmount() < FUEL_MB, "fuel tank did not lose what the thruster gained");
            helper.assertTrue(fuelTank(helper, TANK).getConsumerCount() == 1, "goggle count does not include the thruster");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void feeds_plain_thruster(GameTestHelper helper) {
        if (skipWithoutPropulsion(helper))
            return;
        placeThrusterAndTank(helper, "thruster", diesel(), FUEL_MB);

        helper.runAfterDelay(10, () -> {
            helper.assertTrue(fuelIn(helper, THRUSTER) > 0, "thruster received no fuel");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void water_is_not_pushed_into_thruster(GameTestHelper helper) {
        if (skipWithoutPropulsion(helper))
            return;
        placeThrusterAndTank(helper, "liquid_vector_thruster", Fluids.WATER, FUEL_MB);

        helper.runAfterDelay(20, () -> {
            helper.assertTrue(fuelIn(helper, THRUSTER) == 0, "water was pushed into the thruster");
            helper.assertTrue(fuelTank(helper, TANK).getTankInventory().getFluidAmount() == FUEL_MB, "fuel tank lost water");
            helper.succeed();
        });
    }

    private static boolean skipWithoutPropulsion(GameTestHelper helper) {
        if (ModList.get().isLoaded(FuelConsumers.PROPULSION_ID))
            return false;
        helper.succeed();
        return true;
    }

    private static void placeThrusterAndTank(GameTestHelper helper, String thrusterBlock, net.minecraft.world.level.material.Fluid fluid, int amount) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(FuelConsumers.PROPULSION_ID, thrusterBlock);
        Block block = BuiltInRegistries.BLOCK.getOptional(id)
                .orElseThrow(() -> new GameTestAssertException("no block " + id + " in the registry"));
        helper.setBlock(THRUSTER, block.defaultBlockState());
        helper.setBlock(TANK, FuelTankBlocks.FUEL_TANK.getDefaultState());
        fuelTank(helper, TANK).getTankInventory().setFluid(new FluidStack(fluid, amount));
    }

    /** Registrate's getSource() is generic, which makes FluidStack's constructors ambiguous without the cast. */
    private static net.minecraft.world.level.material.Fluid diesel() {
        return (net.minecraft.world.level.material.Fluid) CDGFluids.DIESEL.getSource();
    }

    private static FuelTankBlockEntity fuelTank(GameTestHelper helper, BlockPos pos) {
        BlockEntity be = helper.getBlockEntity(pos);
        if (!(be instanceof FuelTankBlockEntity tank))
            throw new GameTestAssertException("no fuel tank block entity at " + pos);
        return tank;
    }

    /** Total mB in the thruster's tank, read the same way the Fuel Tank pushes: null side. */
    private static int fuelIn(GameTestHelper helper, BlockPos pos) {
        IFluidHandler handler = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(pos), null);
        if (handler == null)
            throw new GameTestAssertException("thruster at " + pos + " exposes no fluid handler on the null side");
        int total = 0;
        for (int i = 0; i < handler.getTanks(); i++)
            total += handler.getFluidInTank(i).getAmount();
        return total;
    }
}
