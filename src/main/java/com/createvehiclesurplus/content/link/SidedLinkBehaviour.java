package com.createvehiclesurplus.content.link;

import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.IntConsumer;

/**
 * A receiving Redstone Link built into one side of a block. Create's own {@code LinkBehaviour}
 * has a single static behaviour type, so a block entity can only hold one; this one takes its
 * type and NBT key prefix per instance, so a block can carry one link per side (the pattern
 * Create Propulsion's vector thrusters use). It joins Create's regular link network, so Redstone
 * Links and Linked Controllers talk to it as usual.
 */
public class SidedLinkBehaviour extends BlockEntityBehaviour implements IRedstoneLinkable {
    private static final List<BehaviourType<SidedLinkBehaviour>> TYPES = new CopyOnWriteArrayList<>();

    /** Creates and remembers a behaviour type; the click handler and the outliner look for all of them. */
    public static BehaviourType<SidedLinkBehaviour> newType(String name) {
        BehaviourType<SidedLinkBehaviour> type = new BehaviourType<>(name);
        TYPES.add(type);
        return type;
    }

    public static List<BehaviourType<SidedLinkBehaviour>> types() {
        return TYPES;
    }

    private final BehaviourType<SidedLinkBehaviour> type;
    private final String prefix;
    private final Component label;
    private final ValueBoxTransform firstSlot;
    private final ValueBoxTransform secondSlot;
    private final IntConsumer signalCallback;
    private Frequency frequencyFirst = Frequency.EMPTY;
    private Frequency frequencyLast = Frequency.EMPTY;
    private boolean newPosition = true;

    public SidedLinkBehaviour(SmartBlockEntity be, BehaviourType<SidedLinkBehaviour> type, String nbtPrefix, Component label,
                              Pair<ValueBoxTransform, ValueBoxTransform> slots, IntConsumer signalCallback) {
        super(be);
        this.type = type;
        this.prefix = nbtPrefix;
        this.label = label;
        this.firstSlot = slots.getLeft();
        this.secondSlot = slots.getRight();
        this.signalCallback = signalCallback;
    }

    @Override
    public BehaviourType<?> getType() {
        return type;
    }

    public Component label() {
        return label;
    }

    public ValueBoxTransform getSlot(boolean first) {
        return first ? firstSlot : secondSlot;
    }

    public Frequency getFrequency(boolean first) {
        return first ? frequencyFirst : frequencyLast;
    }

    /** Hit test against one slot, with {@code worldHit} in world coordinates. */
    public boolean testHit(boolean first, Vec3 worldHit) {
        Vec3 local = worldHit.subtract(Vec3.atLowerCornerOf(getPos()));
        return getSlot(first).testHit(getWorld(), getPos(), blockEntity.getBlockState(), local);
    }

    public void setFrequency(boolean first, ItemStack stack) {
        stack = stack.copy();
        stack.setCount(1);
        ItemStack current = getFrequency(first).getStack();
        boolean changed = !ItemStack.isSameItemSameComponents(stack, current);
        if (changed)
            Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(getWorld(), this);
        if (first)
            frequencyFirst = Frequency.of(stack);
        else
            frequencyLast = Frequency.of(stack);
        if (changed) {
            blockEntity.sendData();
            Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(getWorld(), this);
        }
    }

    @Override
    public void initialize() {
        super.initialize();
        if (!getWorld().isClientSide) {
            Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(getWorld(), this);
            newPosition = true;
        }
    }

    @Override
    public void unload() {
        super.unload();
        if (!getWorld().isClientSide)
            Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(getWorld(), this);
    }

    // IRedstoneLinkable

    @Override
    public int getTransmittedStrength() {
        return 0;
    }

    @Override
    public void setReceivedStrength(int power) {
        if (newPosition)
            signalCallback.accept(power);
    }

    @Override
    public boolean isListening() {
        return true;
    }

    @Override
    public boolean isAlive() {
        Level level = getWorld();
        BlockPos pos = getPos();
        return !blockEntity.isChunkUnloaded() && !blockEntity.isRemoved() && level.isLoaded(pos)
                && level.getBlockEntity(pos) == blockEntity;
    }

    @Override
    public Couple<Frequency> getNetworkKey() {
        return Couple.create(frequencyFirst, frequencyLast);
    }

    @Override
    public BlockPos getLocation() {
        return getPos();
    }

    // NBT

    @Override
    public boolean isSafeNBT() {
        return true;
    }

    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(nbt, registries, clientPacket);
        nbt.put(prefix + "FrequencyFirst", frequencyFirst.getStack().saveOptional(registries));
        nbt.put(prefix + "FrequencyLast", frequencyLast.getStack().saveOptional(registries));
        nbt.putLong(prefix + "LastKnownPosition", getPos().asLong());
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        newPosition = getPos().asLong() != nbt.getLong(prefix + "LastKnownPosition");
        super.read(nbt, registries, clientPacket);
        frequencyFirst = Frequency.of(ItemStack.parseOptional(registries, nbt.getCompound(prefix + "FrequencyFirst")));
        frequencyLast = Frequency.of(ItemStack.parseOptional(registries, nbt.getCompound(prefix + "FrequencyLast")));
    }
}
