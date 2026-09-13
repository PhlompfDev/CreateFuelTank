package com.createvehiclesurplus.client.ponder;

import com.createvehiclesurplus.content.fuel_tank.FuelTankBlockEntity;
import com.jesz.createdieselgenerators.CDGFluids;
import com.jesz.createdieselgenerators.content.diesel_engine.normal.DieselEngineBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/**
 * Ponder storyboards for the Fuel Tank and the Long Fuel Tank. The Ponder level never runs the
 * server tick, so the feeding is acted out: fuel is moved between the tanks by hand and the engine
 * is spun up with a kinetic speed, exactly like Create's own fluid scenes do.
 */
public class FuelTankScenes {
    private static final int ENGINE_SPEED = 32;

    public static void feeding(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("fuel_tank_feeding", "Feeding engines with the Fuel Tank");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);

        BlockPos tankPos = util.grid().at(2, 1, 2);
        BlockPos enginePos = util.grid().at(3, 1, 2);
        BlockPos leverPos = util.grid().at(1, 1, 2);
        Selection tank = util.select().position(tankPos);
        Selection engineRig = util.select().fromTo(3, 1, 0, 3, 1, 2);
        Selection lever = util.select().position(leverPos);

        scene.world().showSection(tank, Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(70)
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().topOf(tankPos))
                .text("The Fuel Tank is a Fluid Tank that pushes its fuel into any Diesel Engine touching it");
        scene.idle(80);

        scene.world().showSection(engineRig, Direction.DOWN);
        scene.idle(15);
        fill(scene, tankPos, 8000);
        scene.idle(15);
        scene.overlay().showText(70)
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(enginePos, Direction.WEST))
                .text("No pipes, no pump: it works on any side of the engine, the engine's port does not matter");
        scene.idle(30);
        transfer(scene, tankPos, enginePos, 4);
        scene.world().setKineticSpeed(engineRig, ENGINE_SPEED);
        scene.effects().rotationSpeedIndicator(util.grid().at(3, 1, 0));
        scene.idle(40);
        scene.overlay().showText(60)
                .placeNearTarget()
                .pointAt(util.vector().topOf(enginePos))
                .text("The engine keeps running for as long as the tank holds fuel");
        scene.idle(70);

        scene.world().showSection(lever, Direction.EAST);
        scene.idle(15);
        scene.world().toggleRedstonePower(lever);
        scene.world().toggleRedstonePower(tank);
        scene.effects().indicateRedstone(leverPos);
        scene.world().setKineticSpeed(engineRig, 0);
        scene.idle(10);
        scene.overlay().showText(80)
                .colored(PonderPalette.RED)
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(tankPos, Direction.WEST))
                .text("A Redstone signal on any block of the tank stops the feed. The fuel stays inside");
        scene.idle(90);

        scene.world().toggleRedstonePower(lever);
        scene.world().toggleRedstonePower(tank);
        scene.effects().indicateRedstone(leverPos);
        transfer(scene, tankPos, enginePos, 2);
        scene.world().setKineticSpeed(engineRig, ENGINE_SPEED);
        scene.idle(10);
        scene.overlay().showText(60)
                .colored(PonderPalette.GREEN)
                .placeNearTarget()
                .pointAt(util.vector().topOf(enginePos))
                .text("Drop the signal and feeding resumes");
        scene.idle(70);
    }

    public static void longTank(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("long_fuel_tank", "The Long Fuel Tank");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);

        BlockPos controllerPos = util.grid().at(0, 1, 2);
        BlockPos enginePos = util.grid().at(3, 1, 2);
        Selection engineRig = util.select().fromTo(3, 1, 0, 3, 1, 2);

        for (int x = 0; x < 3; x++) {
            scene.world().showSection(util.select().position(x, 1, 2), Direction.DOWN);
            scene.idle(8);
        }
        scene.idle(10);
        scene.overlay().showText(70)
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().topOf(1, 1, 2))
                .text("The Long Fuel Tank is the same tank lying on its side: one block thick, chaining end to end");
        scene.idle(80);
        scene.overlay().showText(60)
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(util.grid().at(2, 1, 2), Direction.NORTH))
                .text("Up to 8 blocks join into one tank, made for thin airframes");
        scene.idle(70);

        scene.world().showSection(engineRig, Direction.DOWN);
        scene.idle(15);
        fill(scene, controllerPos, 12000);
        scene.idle(15);
        transfer(scene, controllerPos, enginePos, 4);
        scene.world().setKineticSpeed(engineRig, ENGINE_SPEED);
        scene.effects().rotationSpeedIndicator(util.grid().at(3, 1, 0));
        scene.idle(10);
        scene.overlay().showText(70)
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(enginePos, Direction.WEST))
                .text("Engines and thrusters touching any block of the row are fed like from a Fuel Tank");
        scene.idle(80);
        scene.overlay().showText(60)
                .placeNearTarget()
                .pointAt(util.vector().topOf(0, 1, 2))
                .text("Craft it from a Fuel Tank alone in the crafting grid, and back again");
        scene.idle(70);
    }

    private static Fluid diesel() {
        return (Fluid) CDGFluids.DIESEL.getSource();
    }

    private static void fill(CreateSceneBuilder scene, BlockPos tankPos, int amount) {
        scene.world().modifyBlockEntity(tankPos, FuelTankBlockEntity.class,
                be -> be.getTankInventory().fill(new FluidStack(diesel(), amount), FluidAction.EXECUTE));
    }

    /** Moves fuel from the tank into the engine in visible steps of 500 mB. */
    private static void transfer(CreateSceneBuilder scene, BlockPos tankPos, BlockPos enginePos, int steps) {
        for (int i = 0; i < steps; i++) {
            scene.world().modifyBlockEntity(tankPos, FuelTankBlockEntity.class,
                    be -> be.getTankInventory().drain(500, FluidAction.EXECUTE));
            scene.world().modifyBlockEntity(enginePos, DieselEngineBlockEntity.class,
                    be -> be.getTank().fill(new FluidStack(diesel(), 500), FluidAction.EXECUTE));
            scene.idle(5);
        }
    }
}
