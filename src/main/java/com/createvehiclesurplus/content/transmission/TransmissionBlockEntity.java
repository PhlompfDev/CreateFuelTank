package com.createvehiclesurplus.content.transmission;

import com.createvehiclesurplus.CreateVehicleSurplus;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Locale;

/**
 * The Transmission's state machine host. Create's rotation propagator asks
 * {@link #getRotationSpeedModifier} for the ratio on each face; the source face gets 1, the
 * other end the current gear's ratio (0 in neutral, which disconnects it like a Clutch).
 * Signals from wires and links go through {@link ShiftRules}; accepted shifts are applied with
 * {@link TransmissionBlock#shift}.
 */
public class TransmissionBlockEntity extends SplitShaftBlockEntity {
    private final ShiftRules rules = new ShiftRules();
    private final int[] wired = new int[Role.VALUES.length];
    private final int[] linked = new int[Role.VALUES.length];

    public TransmissionBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public Gear gear() {
        return TransmissionBlock.gearOf(getBlockState());
    }

    @Override
    public float getRotationSpeedModifier(Direction face) {
        if (!hasSource() || face == getSourceFacing())
            return 1;
        return gear().ratio();
    }

    @Override
    public void initialize() {
        super.initialize();
        updateWiredSignals();
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide)
            return;
        apply(rules.tick(gear(), level.getGameTime()), false);
    }

    /** Re-reads the redstone signal on each role's face. Called on neighbour changes and role rotation. */
    public void updateWiredSignals() {
        if (level == null || level.isClientSide)
            return;
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof TransmissionBlock))
            return;
        for (Role role : Role.VALUES) {
            Direction face = TransmissionBlock.faceOf(state, role);
            int strength = level.getSignal(worldPosition.relative(face), face);
            if (strength != wired[role.ordinal()]) {
                wired[role.ordinal()] = strength;
                signalChanged(role);
            }
        }
    }

    /** A role's link network strength changed (called by that role's link behaviour). */
    void setLinkedStrength(Role role, int strength) {
        if (level == null || level.isClientSide || strength == linked[role.ordinal()])
            return;
        linked[role.ordinal()] = strength;
        signalChanged(role);
    }

    private void signalChanged(Role role) {
        int effective = Math.max(wired[role.ordinal()], linked[role.ordinal()]);
        apply(rules.onSignal(role, effective, gear(), level.getGameTime()), true);
        setChanged();
        sendData();
    }

    /** A computer (or anything else) asks for a specific gear. */
    public ShiftRules.Result requestGear(Gear target) {
        return apply(rules.request(target, gear(), level.getGameTime()), true);
    }

    public ShiftRules.Result requestShift(boolean up) {
        long now = level.getGameTime();
        return apply(up ? rules.shiftUp(gear(), now) : rules.shiftDown(gear(), now), true);
    }

    public ShiftRules.Control control() {
        return rules.control();
    }

    public float inputSpeed() {
        return getSpeed();
    }

    public float outputSpeed() {
        return getSpeed() * gear().ratio();
    }

    private ShiftRules.Result apply(ShiftRules.Result result, boolean feedback) {
        if (result.target() != null) {
            BlockState state = getBlockState();
            if (state.getBlock() instanceof TransmissionBlock block) {
                block.shift(level, worldPosition, state, result.target());
                rules.markShifted(level.getGameTime());
            }
        } else if (feedback && result.refusal() != null && level instanceof ServerLevel server) {
            server.playSound(null, worldPosition, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.25f, 0.6f);
            server.sendParticles(ParticleTypes.SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 0.8,
                    worldPosition.getZ() + 0.5, 4, 0.2, 0.1, 0.2, 0.01);
        }
        return result;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("ShiftRules", rules.write());
        tag.putIntArray("Wired", wired.clone());
        tag.putIntArray("Linked", linked.clone());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        rules.read(tag.getCompound("ShiftRules"));
        copyInto(tag.getIntArray("Wired"), wired);
        copyInto(tag.getIntArray("Linked"), linked);
    }

    private static void copyInto(int[] from, int[] to) {
        for (int i = 0; i < to.length && i < from.length; i++)
            to[i] = from[i];
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        Gear gear = gear();
        new LangBuilder(CreateVehicleSurplus.ID).translate("gui.goggles.transmission.gear", gear.label(), rpm(gear.ratio()))
                .style(ChatFormatting.GOLD).forGoggles(tooltip);
        new LangBuilder(CreateVehicleSurplus.ID).translate("gui.goggles.transmission.speed", rpm(inputSpeed()), rpm(outputSpeed()))
                .style(ChatFormatting.GRAY).forGoggles(tooltip);
        new LangBuilder(CreateVehicleSurplus.ID).translate("gui.goggles.transmission.control." + control().id())
                .style(ChatFormatting.GRAY).forGoggles(tooltip);
        return true;
    }

    private static String rpm(float value) {
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("\\.?0+$", "");
    }
}
