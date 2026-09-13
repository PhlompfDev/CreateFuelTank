package com.createfueltank.content.fuel_tank;

import com.createfueltank.CreateFuelTank;
import com.createfueltank.FuelTankBlockEntities;
import com.createfueltank.compat.FuelConsumer;
import com.createfueltank.compat.FuelConsumers;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A Fluid Tank whose controller feeds every fuel-burning machine touching the multiblock:
 * Diesel Generators engines and, when Create Propulsion is installed, its liquid thrusters
 * (see {@link FuelConsumers}).
 * <p>
 * Server-side, once per tick, the controller pushes up to {@link #TRANSFER_RATE} mB of its
 * fluid into each cached adjacent machine through the handler its {@link FuelConsumer} hands
 * back (the block's fluid capability on a {@code null} side, which every supported machine
 * answers with its whole internal tank regardless of where its pipe port is). Each kind decides
 * for itself whether the tank's fluid is a fuel, so water in a Fuel Tank never clogs anything.
 * <p>
 * The machine cache is rebuilt when a neighbour changes ({@link #markSurroundingsDirty()}),
 * when the multiblock re-forms, and every {@link #RESCAN_INTERVAL} ticks as a safety net.
 * The same rescan notices whether any block of the multiblock is redstone powered; if so the
 * whole tank stops feeding.
 */
public class FuelTankBlockEntity extends FluidTankBlockEntity {
    /** mB per tick per machine. A CDG engine's buffer is 1000 mB, so it refills in a few ticks. */
    public static final int TRANSFER_RATE = 250;
    private static final int RESCAN_INTERVAL = 20;

    private final List<BlockPos> adjacentConsumers = new ArrayList<>();
    private boolean surroundingsDirty = true;
    private int rescanTimer = 0;

    // Synced to the client for the goggle overlay.
    private boolean redstoneDisabled = false;
    private int consumerCount = 0;

    // "Would the machine at this position burn the tank's fluid" is cached per machine until the
    // fluid changes or the surroundings are rescanned. Fully qualified: the inherited nested
    // interface IMultiBlockEntityContainer.Fluid shadows the import.
    private net.minecraft.world.level.material.Fluid cachedFuelFluid = null;
    private final Map<BlockPos, Boolean> cachedAccepts = new HashMap<>();

    public FuelTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        registerFluidHandler(event, FuelTankBlockEntities.FUEL_TANK.get());
        registerFluidHandler(event, FuelTankBlockEntities.LONG_FUEL_TANK.get());
    }

    private static void registerFluidHandler(RegisterCapabilitiesEvent event, BlockEntityType<? extends FuelTankBlockEntity> type) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type, (be, side) -> {
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

        if (redstoneDisabled || adjacentConsumers.isEmpty())
            return;

        FluidStack contents = tankInventory.getFluid();
        if (contents.isEmpty())
            return;
        net.minecraft.world.level.material.Fluid fluid = contents.getFluid();
        if (fluid != cachedFuelFluid) {
            cachedFuelFluid = fluid;
            cachedAccepts.clear();
        }

        for (BlockPos pos : adjacentConsumers) {
            BlockEntity be = level.getBlockEntity(pos);
            FuelConsumer consumer = FuelConsumers.of(be);
            if (consumer == null) {
                surroundingsDirty = true;
                continue;
            }
            if (!accepts(consumer, pos, be, fluid))
                continue;
            IFluidHandler target = consumer.getTank(level, pos, be);
            if (target == null)
                continue;
            FluidUtil.tryFluidTransfer(target, tankInventory, TRANSFER_RATE, true);
            if (tankInventory.isEmpty())
                return;
        }
    }

    private boolean accepts(FuelConsumer consumer, BlockPos pos, BlockEntity be, net.minecraft.world.level.material.Fluid fluid) {
        Boolean cached = cachedAccepts.get(pos);
        if (cached != null)
            return cached;
        boolean result = consumer.acceptsFuel(level, pos, be, fluid);
        cachedAccepts.put(pos, result);
        return result;
    }

    private void rescanSurroundings() {
        surroundingsDirty = false;
        rescanTimer = RESCAN_INTERVAL;
        // A machine's answer can depend on its own contents (a thruster already holding another
        // fuel rejects a second kind), so re-ask everyone on every rescan.
        cachedAccepts.clear();

        int width = getWidth();
        int height = getHeight();
        Direction.Axis axis = getMainConnectionAxis();
        boolean powered = false;
        Set<BlockPos> consumers = new LinkedHashSet<>();

        // "height" is the extent along the main axis, "width" the two others: the same layout
        // ConnectivityHandler builds, whether the tank stands up (Y) or lies along X or Z.
        for (int along = 0; along < height; along++) {
            for (int a = 0; a < width; a++) {
                for (int b = 0; b < width; b++) {
                    BlockPos part = partAt(axis, along, a, b);
                    BlockState partState = level.getBlockState(part);
                    if (partState.hasProperty(BlockStateProperties.POWERED) && partState.getValue(BlockStateProperties.POWERED))
                        powered = true;

                    for (Direction direction : Direction.values()) {
                        BlockPos neighbour = part.relative(direction);
                        if (isInsideMulti(neighbour, axis, width, height))
                            continue;
                        if (FuelConsumers.of(level.getBlockEntity(neighbour)) != null)
                            consumers.add(neighbour);
                    }
                }
            }
        }

        adjacentConsumers.clear();
        adjacentConsumers.addAll(consumers);

        if (powered != redstoneDisabled || consumers.size() != consumerCount) {
            redstoneDisabled = powered;
            consumerCount = consumers.size();
            setChanged();
            sendData();
        }
    }

    /** Position of a part: {@code along} steps down the main axis, {@code a}/{@code b} across it. Mirrors ConnectivityHandler. */
    private BlockPos partAt(Direction.Axis axis, int along, int a, int b) {
        return switch (axis) {
            case X -> worldPosition.offset(along, a, b);
            case Y -> worldPosition.offset(a, along, b);
            case Z -> worldPosition.offset(a, b, along);
        };
    }

    private boolean isInsideMulti(BlockPos pos, Direction.Axis axis, int width, int height) {
        for (Direction.Axis check : Direction.Axis.VALUES) {
            int d = check.choose(pos.getX(), pos.getY(), pos.getZ()) - check.choose(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
            int extent = check == axis ? height : width;
            if (d < 0 || d >= extent)
                return false;
        }
        return true;
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
        else if (fuelTank.consumerCount == 0)
            lang.translate("gui.goggles.fuel_tank.no_engines").style(ChatFormatting.GRAY).forGoggles(tooltip);
        else
            lang.translate("gui.goggles.fuel_tank.feeding", fuelTank.consumerCount).style(ChatFormatting.GREEN).forGoggles(tooltip);
        return true;
    }

    @Override
    public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        if (isController()) {
            compound.putBoolean("RedstoneDisabled", redstoneDisabled);
            // Key kept from 0.1.0, when only engines counted.
            compound.putInt("EngineCount", consumerCount);
        }
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        redstoneDisabled = compound.getBoolean("RedstoneDisabled");
        consumerCount = compound.getInt("EngineCount");
        if (!clientPacket)
            surroundingsDirty = true;
    }

    public boolean isRedstoneDisabled() {
        return redstoneDisabled;
    }

    /** Number of engines and thrusters currently being fed (the goggle readout). */
    public int getConsumerCount() {
        return consumerCount;
    }
}
