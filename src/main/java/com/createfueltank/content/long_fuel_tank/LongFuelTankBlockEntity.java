package com.createfueltank.content.long_fuel_tank;

import com.createfueltank.content.fuel_tank.FuelTankBlockEntity;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

/**
 * Block entity of the {@link LongFuelTankBlock}: a {@link FuelTankBlockEntity} whose multiblock
 * runs along the block's horizontal axis instead of up.
 * <p>
 * Create's {@code ConnectivityHandler} already forms multiblocks along whatever
 * {@link #getMainConnectionAxis()} says (the Item Vault relies on it); "height" then means
 * length along the axis and "width" the 1x1 cross-section. What has to be redone here is every
 * place the upright tank assumes Y: the end-cap blockstates, the window toggle, the glow of
 * luminous fluids and the render bounds. Feeding logic is inherited and already axis-aware.
 */
public class LongFuelTankBlockEntity extends FuelTankBlockEntity {
    public static final int MAX_LENGTH = 8;

    public LongFuelTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public Axis getMainConnectionAxis() {
        Axis axis = LongFuelTankBlock.getAxis(getBlockState());
        return axis == null ? Axis.Z : axis;
    }

    @Override
    public int getMaxLength(Axis longAxis, int width) {
        return longAxis == Axis.Y ? 1 : MAX_LENGTH;
    }

    @Override
    public int getMaxWidth() {
        return 1;
    }

    public boolean hasWindows() {
        return window;
    }

    /** Position of the {@code index}-th block of this (controller's) tube, counted from the controller. */
    private BlockPos partAlong(int index) {
        return worldPosition.relative(Direction.fromAxisAndDirection(getMainConnectionAxis(), Direction.AxisDirection.POSITIVE), index);
    }

    @Override
    public void notifyMultiUpdated() {
        BlockState state = getBlockState();
        if (LongFuelTankBlock.isLongTank(state)) {
            Axis axis = getMainConnectionAxis();
            BlockPos controller = getController();
            int along = axis.choose(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ())
                    - axis.choose(controller.getX(), controller.getY(), controller.getZ());
            boolean atMin = along == 0;
            boolean atMax = along == height - 1;
            boolean upIsPositive = LongFuelTankBlock.upEnd(axis).getAxisDirection() == Direction.AxisDirection.POSITIVE;
            state = state.setValue(FluidTankBlock.TOP, upIsPositive ? atMax : atMin)
                    .setValue(FluidTankBlock.BOTTOM, upIsPositive ? atMin : atMax);
            level.setBlock(worldPosition, state, 6);
        }
        if (isController())
            setWindows(window);
        onFluidStackChanged(tankInventory.getFluid());
        setChanged();
        markSurroundingsDirty();
    }

    @Override
    public void setWindows(boolean window) {
        this.window = window;
        for (int i = 0; i < height; i++) {
            BlockPos pos = partAlong(i);
            BlockState state = level.getBlockState(pos);
            if (!LongFuelTankBlock.isLongTank(state))
                continue;
            level.setBlock(pos, state.setValue(FluidTankBlock.SHAPE, window ? FluidTankBlock.Shape.WINDOW : FluidTankBlock.Shape.PLAIN), 22);
            level.getChunkSource().getLightEngine().checkBlock(pos);
        }
    }

    /** A lying tank's level is vertical inside every block, so all of them glow alike. */
    @Override
    protected void onFluidStackChanged(FluidStack newFluidStack) {
        if (!hasLevel())
            return;
        FluidType type = newFluidStack.getFluid().getFluidType();
        int luminosity = (int) (type.getLightLevel(newFluidStack) / 1.2f);
        int actual = getFillState() > 0 ? luminosity : (luminosity > 0 ? 1 : 0);

        for (int i = 0; i < height; i++) {
            BlockPos pos = partAlong(i);
            // partAt filters by our own type, so every part is a LongFuelTankBlockEntity; the
            // instanceof is what lets us touch the protected luminosity field.
            if (!(ConnectivityHandler.partAt(getType(), level, pos) instanceof LongFuelTankBlockEntity tankAt))
                continue;
            level.updateNeighbourForOutputSignal(pos, tankAt.getBlockState().getBlock());
            if (tankAt.luminosity != actual)
                tankAt.setLuminosity(actual);
        }

        if (!level.isClientSide) {
            setChanged();
            sendData();
        }
        if (isVirtual()) {
            if (getFluidLevel() == null)
                setFluidLevel(LerpedFloat.linear().startWithValue(getFillState()));
            getFluidLevel().chase(getFillState(), 0.5f, Chaser.EXP);
        }
    }

    @Override
    protected AABB createRenderBoundingBox() {
        AABB box = new AABB(worldPosition);
        if (!isController())
            return box;
        Axis axis = getMainConnectionAxis();
        return box.expandTowards(axis == Axis.X ? height - 1 : 0, 0, axis == Axis.Z ? height - 1 : 0);
    }
}
