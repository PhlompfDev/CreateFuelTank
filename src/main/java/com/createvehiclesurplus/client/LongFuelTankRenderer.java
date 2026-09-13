package com.createvehiclesurplus.client;

import com.createvehiclesurplus.content.long_fuel_tank.LongFuelTankBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Fluid inside a lying tank. Unlike the upright tank, the level is vertical within the
 * one-block cross-section and the fluid box stretches the whole tube along its axis, minus the
 * end caps. Same hull and cap sizes as Create's {@code FluidTankRenderer}.
 */
public class LongFuelTankRenderer extends SafeBlockEntityRenderer<LongFuelTankBlockEntity> {
    private static final float HULL = 1 / 16f + 1 / 128f;
    private static final float CAP = 1 / 4f;
    private static final float MIN_PUDDLE = 1 / 16f;

    public LongFuelTankRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(LongFuelTankBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (!be.isController() || !be.hasWindows())
            return;
        LerpedFloat fluidLevel = be.getFluidLevel();
        if (fluidLevel == null)
            return;
        FluidStack fluidStack = be.getTankInventory().getFluid();
        if (fluidStack.isEmpty())
            return;

        float level = Mth.clamp(fluidLevel.getValue(partialTicks), 0, 1);
        if (level < 1 / 512f)
            return;

        float innerHeight = 1 - 2 * HULL;
        float fill = MIN_PUDDLE + level * (innerHeight - MIN_PUDDLE);
        boolean floats = fluidStack.getFluid().getFluidType().isLighterThanAir();
        float yMin = floats ? 1 - HULL - fill : HULL;
        float yMax = yMin + fill;

        float length = be.getHeight();
        boolean alongX = be.getMainConnectionAxis() == Axis.X;
        float xMin = alongX ? CAP : HULL;
        float xMax = alongX ? length - CAP : 1 - HULL;
        float zMin = alongX ? HULL : CAP;
        float zMax = alongX ? 1 - HULL : length - CAP;

        ms.pushPose();
        NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(fluidStack, xMin, yMin, zMin, xMax, yMax, zMax, buffer, ms, light, false, true);
        ms.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(LongFuelTankBlockEntity be) {
        return be.isController();
    }
}
