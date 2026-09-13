package com.createvehiclesurplus.client.ponder;

import com.createvehiclesurplus.VehicleSurplusBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.gearbox.GearboxBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Ponder storyboard for the Differential: the same rig driven through a Differential and then
 * through a Gearbox, so the mirrored corners of the Gearbox are visible next to the aligned ones.
 */
public class DifferentialScenes {
    private static final float SPEED = 16;

    public static void differential(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("differential", "Splitting rotation with the Differential");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);

        BlockPos box = util.grid().at(2, 1, 2);
        BlockPos sourcePos = util.grid().at(1, 1, 2);
        Selection boxSel = util.select().position(box);
        Selection input = util.select().fromTo(0, 1, 2, 1, 1, 2);
        Selection east = util.select().fromTo(3, 1, 2, 4, 1, 2);
        Selection north = util.select().fromTo(2, 1, 0, 2, 1, 1);
        Selection south = util.select().fromTo(2, 1, 3, 2, 1, 4);
        BlockPos northEnd = util.grid().at(2, 1, 0);
        BlockPos southEnd = util.grid().at(2, 1, 4);
        BlockPos eastEnd = util.grid().at(4, 1, 2);
        BlockPos westEnd = util.grid().at(0, 1, 2);

        scene.world().setKineticSpeed(util.select().everywhere(), SPEED);
        scene.world().showSection(input, Direction.EAST);
        scene.idle(10);
        scene.world().showSection(boxSel, Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(60)
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().topOf(box))
                .text("The Differential relays rotation to its other three faces, like a Gearbox");
        scene.idle(30);
        scene.world().showSection(east, Direction.WEST);
        scene.world().showSection(north, Direction.SOUTH);
        scene.world().showSection(south, Direction.NORTH);
        scene.idle(40);

        indicate(scene, westEnd, northEnd, southEnd, eastEnd);
        scene.overlay().showText(80)
                .colored(PonderPalette.GREEN)
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().centerOf(southEnd))
                .text("Every output turns the same way as the input, sign included");
        scene.idle(90);

        // The same rig through a Gearbox. Its visual reads the source position from NBT.
        scene.world().setBlock(box, AllBlocks.GEARBOX.getDefaultState().setValue(BlockStateProperties.AXIS, Axis.Y), true);
        scene.world().modifyBlockEntityNBT(boxSel, GearboxBlockEntity.class,
                nbt -> nbt.put("Source", NbtUtils.writeBlockPos(sourcePos)));
        scene.world().setKineticSpeed(east, -SPEED);
        scene.world().setKineticSpeed(north, -SPEED);
        scene.world().setKineticSpeed(south, SPEED);
        scene.idle(20);
        indicate(scene, westEnd, northEnd, southEnd, eastEnd);
        scene.overlay().showText(80)
                .colored(PonderPalette.RED)
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().topOf(box))
                .text("A Gearbox mirrors the corners and reverses the straight-through output");
        scene.idle(90);
        scene.overlay().showText(70)
                .colored(PonderPalette.RED)
                .placeNearTarget()
                .pointAt(util.vector().centerOf(northEnd))
                .text("Two wheels on one axle would fight each other");
        scene.idle(80);

        scene.world().setBlock(box, VehicleSurplusBlocks.DIFFERENTIAL.getDefaultState().setValue(BlockStateProperties.AXIS, Axis.Y), true);
        scene.world().setKineticSpeed(util.select().everywhere(), SPEED);
        scene.idle(20);
        indicate(scene, westEnd, northEnd, southEnd, eastEnd);
        scene.overlay().showText(80)
                .colored(PonderPalette.GREEN)
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().topOf(box))
                .text("Put a Differential between them and both wheels drive forward together");
        scene.idle(90);
        scene.overlay().showText(80)
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(box, Direction.NORTH))
                .text("It sits flat by default. Sneak-place it against a wall for the vertical orientation, or turn it with a Wrench");
        scene.idle(90);
    }

    private static void indicate(CreateSceneBuilder scene, BlockPos... positions) {
        for (BlockPos pos : positions)
            scene.effects().rotationDirectionIndicator(pos);
        scene.idle(15);
    }
}
