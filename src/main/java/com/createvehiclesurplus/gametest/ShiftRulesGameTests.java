package com.createvehiclesurplus.gametest;

import com.createvehiclesurplus.CreateVehicleSurplus;
import com.createvehiclesurplus.content.transmission.Gear;
import com.createvehiclesurplus.content.transmission.Role;
import com.createvehiclesurplus.content.transmission.ShiftRules;
import com.createvehiclesurplus.content.transmission.ShiftRules.Refusal;
import com.createvehiclesurplus.content.transmission.ShiftRules.Result;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** ShiftRules is pure logic; these tests need no blocks, the template is only there because GameTests need one. */
@GameTestHolder(CreateVehicleSurplus.ID)
@PrefixGameTestTemplate(false)
public class ShiftRulesGameTests {
    private static final String TEMPLATE = "empty_5x3x5";

    @GameTest(template = TEMPLATE)
    public static void analog_bands_map_to_gears(GameTestHelper helper) {
        Gear[] expected = {
                Gear.REVERSE, Gear.REVERSE,
                Gear.NEUTRAL, Gear.NEUTRAL, Gear.NEUTRAL,
                Gear.QUARTER, Gear.QUARTER, Gear.QUARTER,
                Gear.HALF, Gear.HALF, Gear.HALF,
                Gear.THREE_QUARTERS, Gear.THREE_QUARTERS,
                Gear.DIRECT, Gear.DIRECT};
        for (int strength = 1; strength <= 15; strength++)
            check(Gear.forAnalog(strength) == expected[strength - 1], "strength " + strength + " gave " + Gear.forAnalog(strength));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void held_up_signal_shifts_once(GameTestHelper helper) {
        ShiftRules rules = new ShiftRules();
        expectShift(rules.onSignal(Role.UP, 15, Gear.NEUTRAL, 0), Gear.QUARTER);
        rules.markShifted(0);
        expectNothing(rules.onSignal(Role.UP, 15, Gear.QUARTER, 10));
        expectNothing(rules.onSignal(Role.UP, 0, Gear.QUARTER, 11));
        expectShift(rules.onSignal(Role.UP, 7, Gear.QUARTER, 20), Gear.HALF);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void down_pulse_shifts_down(GameTestHelper helper) {
        ShiftRules rules = new ShiftRules();
        expectShift(rules.onSignal(Role.DOWN, 15, Gear.NEUTRAL, 0), Gear.REVERSE);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void ends_do_not_wrap(GameTestHelper helper) {
        ShiftRules rules = new ShiftRules();
        expectRefusal(rules.shiftUp(Gear.DIRECT, 0), Refusal.LIMIT);
        expectRefusal(rules.shiftDown(Gear.REVERSE, 0), Refusal.LIMIT);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void cooldown_blocks_fast_shifts(GameTestHelper helper) {
        ShiftRules rules = new ShiftRules();
        rules.markShifted(100);
        expectRefusal(rules.shiftUp(Gear.NEUTRAL, 103), Refusal.COOLDOWN);
        expectShift(rules.shiftUp(Gear.NEUTRAL, 104), Gear.QUARTER);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void analog_overrides_pulses_and_requests(GameTestHelper helper) {
        ShiftRules rules = new ShiftRules();
        expectShift(rules.onSignal(Role.ANALOG, 10, Gear.NEUTRAL, 0), Gear.HALF);
        rules.markShifted(0);
        expectRefusal(rules.onSignal(Role.UP, 15, Gear.HALF, 20), Refusal.ANALOG_OVERRIDE);
        expectRefusal(rules.request(Gear.DIRECT, Gear.HALF, 20), Refusal.ANALOG_OVERRIDE);
        check(rules.control() == ShiftRules.Control.ANALOG, "control should be analog");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void analog_change_during_cooldown_lands_on_tick(GameTestHelper helper) {
        ShiftRules rules = new ShiftRules();
        rules.markShifted(0);
        expectNothing(rules.onSignal(Role.ANALOG, 15, Gear.NEUTRAL, 1));
        expectNothing(rules.tick(Gear.NEUTRAL, 3));
        expectShift(rules.tick(Gear.NEUTRAL, 4), Gear.DIRECT);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void neutral_hold_locks_and_release_keeps_neutral(GameTestHelper helper) {
        ShiftRules rules = new ShiftRules();
        expectShift(rules.onSignal(Role.NEUTRAL, 15, Gear.HALF, 0), Gear.NEUTRAL);
        rules.markShifted(0);
        expectRefusal(rules.request(Gear.DIRECT, Gear.NEUTRAL, 10), Refusal.NEUTRAL_HOLD);
        expectRefusal(rules.onSignal(Role.UP, 15, Gear.NEUTRAL, 10), Refusal.NEUTRAL_HOLD);
        // Analog is set while neutral is held: neutral still wins.
        expectNothing(rules.onSignal(Role.ANALOG, 15, Gear.NEUTRAL, 11));
        // Releasing neutral hands control to analog.
        expectShift(rules.onSignal(Role.NEUTRAL, 0, Gear.NEUTRAL, 20), Gear.DIRECT);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void neutral_release_without_analog_stays_neutral(GameTestHelper helper) {
        ShiftRules rules = new ShiftRules();
        expectShift(rules.onSignal(Role.NEUTRAL, 15, Gear.HALF, 0), Gear.NEUTRAL);
        rules.markShifted(0);
        expectNothing(rules.onSignal(Role.NEUTRAL, 0, Gear.NEUTRAL, 20));
        check(rules.control() == ShiftRules.Control.FREE, "control should be free");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void request_for_current_gear_is_accepted_unchanged(GameTestHelper helper) {
        Result result = new ShiftRules().request(Gear.HALF, Gear.HALF, 0);
        check(result.accepted() && result.target() == null && result.refusal() == null, "expected UNCHANGED, got " + result);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void saved_strengths_prevent_false_pulses(GameTestHelper helper) {
        ShiftRules rules = new ShiftRules();
        rules.onSignal(Role.UP, 15, Gear.NEUTRAL, 0);
        ShiftRules loaded = new ShiftRules();
        loaded.read(rules.write());
        expectNothing(loaded.onSignal(Role.UP, 15, Gear.QUARTER, 50));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void labels_round_trip(GameTestHelper helper) {
        for (Gear gear : Gear.values())
            check(Gear.byLabel(gear.label()) == gear && Gear.byIndex(gear.index()) == gear, "round trip failed for " + gear);
        check(Gear.byLabel("2") == null, "unknown label should be null");
        helper.succeed();
    }

    private static void expectShift(Result result, Gear gear) {
        check(result.accepted() && result.target() == gear, "expected shift to " + gear + ", got " + result);
    }

    private static void expectNothing(Result result) {
        check(result.target() == null && result.refusal() == null, "expected no change, got " + result);
    }

    private static void expectRefusal(Result result, Refusal refusal) {
        check(!result.accepted() && result.refusal() == refusal, "expected refusal " + refusal + ", got " + result);
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new GameTestAssertException(message);
    }
}
