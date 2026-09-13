package com.createvehiclesurplus.client;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.foundation.block.connected.CTModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link FuelTankModel} for the lying tank: rotated connected textures, and faces that point
 * at another block of the same tube are dropped so the row reads as one hollow body.
 */
public class LongFuelTankModel extends CTModel {
    private static final ModelProperty<boolean[]> CULLED = new ModelProperty<>();

    public LongFuelTankModel(BakedModel originalModel) {
        super(originalModel, new LongFuelTankCTBehaviour());
    }

    @Override
    protected ModelData.Builder gatherModelData(ModelData.Builder builder, BlockAndTintGetter world, BlockPos pos, BlockState state, ModelData blockEntityData) {
        super.gatherModelData(builder, world, pos, state, blockEntityData);
        boolean[] culled = new boolean[6];
        for (Direction d : Direction.values())
            culled[d.get3DDataValue()] = ConnectivityHandler.isConnected(world, pos, pos.relative(d));
        return builder.with(CULLED, culled);
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData extraData, RenderType renderType) {
        if (side != null)
            return Collections.emptyList();

        boolean[] culled = extraData.get(CULLED);
        List<BakedQuad> quads = new ArrayList<>();
        for (Direction d : Direction.values()) {
            if (culled != null && culled[d.get3DDataValue()])
                continue;
            quads.addAll(super.getQuads(state, d, rand, extraData, renderType));
        }
        quads.addAll(super.getQuads(state, null, rand, extraData, renderType));
        return quads;
    }
}
