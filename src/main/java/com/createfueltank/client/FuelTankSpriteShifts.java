package com.createfueltank.client;

import com.createfueltank.CreateFuelTank;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;

/** Connected-texture sprite pairs for the fuel tank, mirroring Create's FLUID_TANK / _TOP / _INNER. */
public class FuelTankSpriteShifts {
    public static final CTSpriteShiftEntry FUEL_TANK = rectangle("fuel_tank");
    public static final CTSpriteShiftEntry FUEL_TANK_TOP = rectangle("fuel_tank_top");
    public static final CTSpriteShiftEntry FUEL_TANK_INNER = rectangle("fuel_tank_inner");

    private static CTSpriteShiftEntry rectangle(String name) {
        return CTSpriteShifter.getCT(AllCTTypes.RECTANGLE,
                CreateFuelTank.rl("block/" + name),
                CreateFuelTank.rl("block/" + name + "_connected"));
    }
}
