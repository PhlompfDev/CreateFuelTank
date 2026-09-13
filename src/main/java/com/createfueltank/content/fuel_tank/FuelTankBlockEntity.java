package com.createfueltank.content.fuel_tank;

import com.createfueltank.CreateFuelTank;
import com.createfueltank.FuelTankBlockEntities;
import com.jesz.createdieselgenerators.CDGRegistries;
import com.jesz.createdieselgenerators.content.diesel_engine.IEngine;
import com.jesz.createdieselgenerators.fuel_type.FuelType;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A Fluid Tank whose controller feeds every Diesel Generators engine touching the multiblock.
 * <p>
 * Server-side, once per tick, the controller pushes up to {@link #TRANSFER_RATE} mB of its
 * fluid into each cached adjacent engine via the engine's fluid capability (queried with a
 * {@code null} side, which every CDG engine answers with its whole internal tank regardless
 * of where its pipe port is). Only fluids CDG knows as a fuel are pushed, so water in a Fuel
 * Tank does not clog an engine.
 * <p>
 * The engine cache is rebuilt when a neighbour changes ({@link #markSurroundingsDirty()}),
 * when the multiblock re-forms, and every {@link #RESCAN_INTERVAL} ticks as a safety net.
 * The same rescan notices whether any block of the multiblock is redstone powered; if so the
 * whole tank stops feeding.
 */
public class FuelTankBlockEntity extends FluidTankBlockEntity {
    /** mB per tick per engine. A CDG engine's buffer is 1000 mB, so it refills in a few ticks. */
    public static final int TRANSFER_RATE = 250;
    private static final int RESCAN_INTERVAL = 20;

    private final List<BlockPos> adjacentEngines = new ArrayList<>();
    private boolean surroundingsDirty = true;
    private int rescanTimer = 0;

    // Synced to the client for the goggle overlay.
    private boolean redstoneDisabled = false;
    private int engineCount = 0;

    // Fully qualified: the inherited nested interface IMultiBlockEntityContainer.Fluid shadows the import.
    private net.minecraft.world.level.material.Fluid cachedFuelFluid = null;
    private boolean cachedIsFuel = false;

    public FuelTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FuelTankBlockEntities.FUEL_TANK.get(), (be, side) -> {
            // The parent computes fluidCapability in its constructor and on every controller
            // change, so this is only null in pathological cases; ask the tick to redo it.
            if (be.fluidCapability == null)
                be.updateCapability = true;
            return be.fluidCapability;
        });
    }

    public void markSurroundingsDirty() {
        surroundingsDirty = true;
    }

    @Override
    public void notifyMultiUpdated() {
        super.notifyMultiUpdated();
        surroundingsDirty = true;
    }

    @Override
    public void removeController(boolean keepFluids) {
        super.removeController(keepFluids);
        surroundingsDirty = true;
    }

    /** Fuel tanks never become boilers, even with steam engines bolted on. */
    @Override
    public void updateBoilerState() {
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide || !isController())
            return;

        if (surroundingsDirty || --rescanTimer <= 0)
            rescanSurroundings();

        if (redstoneDisabled || adjacentEngines.isEmpty())
            return;

        FluidStack contents = tankInventory.getFluid();
        if (contents.isEmpty() || !isFuel(contents.getFluid()))
            return;

        for (BlockPos enginePos : adjacentEngines) {
            if (!(level.getBlockEntity(enginePos) instanceof IEngine)) {
                surroundingsDirty = true;
                continue;
            }
            IFluidHandler engineTank = level.getCapability(Capabilities.FluidHandler.BLOCK, enginePos, null);
            if (engineTank == null)
                continue;
            FluidUtil.tryFluidTransfer(engineTank, tankInventory, TRANSFER_RATE, true);
            if (tankInventory.isEmpty())
                return;
        }
    }

    private void rescanSurroundings() {
        surroundingsDirty = false;
        rescanTimer = RESCAN_INTERVAL;

        int width = getWidth();
        int height = getHeight();
        BlockPos origin = worldPosition;
        boolean powered = false;
        Set<BlockPos> engines = new LinkedHashSet<>();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < width; z++) {
                    BlockPos part = origin.offset(x, y, z);
                    BlockState partState = level.getBlockState(part);
                    if (partState.getBlock() instanceof FuelTankBlock && partState.getValue(FuelTankBlock.POWERED))
                        powered = true;

                    for (Direction direction : Direction.values()) {
                        BlockPos neighbour = part.relative(direction);
                        if (isInsideMulti(neighbour, origin, width, height))
                            continue;
                        if (level.getBlockEntity(neighbour) instanceof IEngine)
                            engines.add(neighbour);
                    }
                }
            }
        }

        adjacentEngines.clear();
        adjacentEngines.addAll(engines);

        if (powered != redstoneDisabled || engines.size() != engineCount) {
            redstoneDisabled = powered;
            engineCount = engines.size();
            setChanged();
            sendData();
        }
    }

    private static boolean isInsideMulti(BlockPos pos, BlockPos origin, int width, int height) {
        int dx = pos.getX() - origin.getX();
        int dy = pos.getY() - origin.getY();
        int dz = pos.getZ() - origin.getZ();
        return dx >= 0 && dx < width && dy >= 0 && dy < height && dz >= 0 && dz < width;
    }

    /** Mirrors IEngine#validFS: a fluid is fuel iff CDG's datapack registry has a FuelType for it. */
    private boolean isFuel(net.minecraft.world.level.material.Fluid fluid) {
        if (fluid != cachedFuelFluid) {
            cachedFuelFluid = fluid;
            HolderLookup.RegistryLookup<FuelType> registry = level.registryAccess().lookupOrThrow(CDGRegistries.FUEL_TYPE);
            cachedIsFuel = FuelType.getTypeFor(registry, fluid) != FuelType.EMPTY;
        }
        return cachedIsFuel;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean shown = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        FluidTankBlockEntity controller = getControllerBE();
        if (!(controller instanceof FuelTankBlockEntity fuelTank))
            return shown;

        LangBuilder lang = new LangBuilder(CreateFuelTank.ID);
        if (fuelTank.redstoneDisabled)
            lang.translate("gui.goggles.fuel_tank.disabled").style(ChatFormatting.RED).forGoggles(tooltip);
        else if (fuelTank.engineCount == 0)
            lang.translate("gui.goggles.fuel_tank.no_engines").style(ChatFormatting.GRAY).forGoggles(tooltip);
        else
            lang.translate("gui.goggles.fuel_tank.feeding", fuelTank.engineCount).style(ChatFormatting.GREEN).forGoggles(tooltip);
        return true;
    }

    @Override
    public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        if (isController()) {
            compound.putBoolean("RedstoneDisabled", redstoneDisabled);
            compound.putInt("EngineCount", engineCount);
        }
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        redstoneDisabled = compound.getBoolean("RedstoneDisabled");
        engineCount = compound.getInt("EngineCount");
        if (!clientPacket)
            surroundingsDirty = true;
    }

    public boolean isRedstoneDisabled() {
        return redstoneDisabled;
    }

    public int getEngineCount() {
        return engineCount;
    }
}
