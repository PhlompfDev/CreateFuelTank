package com.createvehiclesurplus.compat.cc;

import com.createvehiclesurplus.content.transmission.TransmissionBlockEntity;
import com.createvehiclesurplus.content.transmission.TransmissionComputerApi;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IPeripheral;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** The {@code transmission} peripheral: Lua bindings over {@link TransmissionComputerApi}. */
public class TransmissionPeripheral implements IPeripheral {
    private final TransmissionBlockEntity be;
    private final TransmissionComputerApi api;

    public TransmissionPeripheral(TransmissionBlockEntity be) {
        this.be = be;
        this.api = new TransmissionComputerApi(be);
    }

    @Override
    public String getType() {
        return "transmission";
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof TransmissionPeripheral peripheral && peripheral.be == be;
    }

    @Override
    public Object getTarget() {
        return be;
    }

    @LuaFunction(mainThread = true)
    public final String getGear() {
        return api.getGear();
    }

    @LuaFunction(mainThread = true)
    public final List<String> getGears() {
        return api.getGears();
    }

    @LuaFunction(mainThread = true)
    public final MethodResult setGear(String label) throws LuaException {
        try {
            return MethodResult.of(api.setGear(label));
        } catch (IllegalArgumentException e) {
            throw new LuaException(e.getMessage());
        }
    }

    @LuaFunction(mainThread = true)
    public final MethodResult shiftUp() {
        return MethodResult.of(api.shiftUp());
    }

    @LuaFunction(mainThread = true)
    public final MethodResult shiftDown() {
        return MethodResult.of(api.shiftDown());
    }

    @LuaFunction(mainThread = true)
    public final double getRatio() {
        return api.getRatio();
    }

    @LuaFunction(mainThread = true)
    public final double getInputSpeed() {
        return api.getInputSpeed();
    }

    @LuaFunction(mainThread = true)
    public final double getOutputSpeed() {
        return api.getOutputSpeed();
    }

    @LuaFunction(mainThread = true)
    public final String getControl() {
        return api.getControl();
    }
}
