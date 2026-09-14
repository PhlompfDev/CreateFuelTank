package com.createvehiclesurplus.content.transmission;

import java.util.Arrays;
import java.util.List;

/**
 * Everything a computer can do with a Transmission, with no ComputerCraft types in sight: the CC
 * peripheral is a thin wrapper around this, and the GameTests call it directly. Requests go
 * through the same {@link ShiftRules} as links and wires. Results use Lua conventions:
 * {@code {true}} or {@code {false, reason}}.
 */
public final class TransmissionComputerApi {
    private static final List<String> GEARS = Arrays.stream(Gear.values()).map(Gear::label).toList();

    private final TransmissionBlockEntity be;

    public TransmissionComputerApi(TransmissionBlockEntity be) {
        this.be = be;
    }

    public String getGear() {
        return be.gear().label();
    }

    public List<String> getGears() {
        return GEARS;
    }

    /** @throws IllegalArgumentException for a label that is not one of {@link #getGears()} */
    public Object[] setGear(String label) {
        Gear gear = Gear.byLabel(label);
        if (gear == null)
            throw new IllegalArgumentException("Unknown gear '" + label + "', expected one of " + GEARS);
        return toLua(be.requestGear(gear));
    }

    public Object[] shiftUp() {
        return toLua(be.requestShift(true));
    }

    public Object[] shiftDown() {
        return toLua(be.requestShift(false));
    }

    public double getRatio() {
        return be.gear().ratio();
    }

    public double getInputSpeed() {
        return be.inputSpeed();
    }

    public double getOutputSpeed() {
        return be.outputSpeed();
    }

    public String getControl() {
        return be.control().id();
    }

    private static Object[] toLua(ShiftRules.Result result) {
        if (result.accepted())
            return new Object[]{true};
        return new Object[]{false, result.refusal() == null ? "unchanged" : result.refusal().id()};
    }
}
