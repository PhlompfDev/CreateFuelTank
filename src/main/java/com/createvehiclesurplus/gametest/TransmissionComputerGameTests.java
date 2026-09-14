package com.createvehiclesurplus.gametest;

import com.createvehiclesurplus.CreateVehicleSurplus;
import com.createvehiclesurplus.content.transmission.Gear;
import com.createvehiclesurplus.content.transmission.TransmissionComputerApi;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Arrays;
import java.util.List;

import static com.createvehiclesurplus.gametest.TransmissionGameTests.*;

/** What a computer sees through the {@code transmission} peripheral. */
@GameTestHolder(CreateVehicleSurplus.ID)
@PrefixGameTestTemplate(false)
public class TransmissionComputerGameTests {

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void computer_sets_and_reads_the_gear(GameTestHelper helper) {
        placeRig(helper, Gear.NEUTRAL);
        helper.runAfterDelay(10, () -> {
            Object[] result = api(helper).setGear("1/2");
            check(Arrays.equals(result, new Object[]{true}), "setGear returned " + Arrays.toString(result));
        });
        helper.runAfterDelay(20, () -> {
            TransmissionComputerApi api = api(helper);
            check(api.getGear().equals("1/2"), "getGear returned " + api.getGear());
            check(api.getRatio() == 0.5, "getRatio returned " + api.getRatio());
            check(api.getInputSpeed() != 0, "input speed is 0");
            check(api.getOutputSpeed() == api.getInputSpeed() * 0.5, "output " + api.getOutputSpeed() + " for input " + api.getInputSpeed());
            check(api.getControl().equals("free"), "getControl returned " + api.getControl());
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void computer_gets_refusal_reasons(GameTestHelper helper) {
        placeRig(helper, Gear.REVERSE);
        helper.runAfterDelay(10, () -> {
            check(Arrays.equals(api(helper).shiftDown(), new Object[]{false, "limit"}), "expected limit");
            helper.setBlock(ANALOG_FACE, Blocks.REDSTONE_BLOCK);
        });
        helper.runAfterDelay(20, () -> {
            TransmissionComputerApi api = api(helper);
            check(api.getControl().equals("analog"), "getControl returned " + api.getControl());
            check(Arrays.equals(api.setGear("1/4"), new Object[]{false, "analog_override"}), "expected analog_override");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void unknown_gear_label_is_an_error(GameTestHelper helper) {
        placeRig(helper, Gear.NEUTRAL);
        helper.runAfterDelay(5, () -> {
            try {
                api(helper).setGear("2");
            } catch (IllegalArgumentException expected) {
                helper.succeed();
                return;
            }
            check(false, "setGear(\"2\") should throw");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void computer_lists_the_gears(GameTestHelper helper) {
        placeRig(helper, Gear.NEUTRAL);
        helper.runAfterDelay(5, () -> {
            check(api(helper).getGears().equals(List.of("R", "N", "1/4", "1/2", "3/4", "1")), "getGears returned " + api(helper).getGears());
            helper.succeed();
        });
    }

    private static TransmissionComputerApi api(GameTestHelper helper) {
        return new TransmissionComputerApi(transmission(helper));
    }
}
