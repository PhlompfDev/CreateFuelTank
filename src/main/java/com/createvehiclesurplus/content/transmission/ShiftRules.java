package com.createvehiclesurplus.content.transmission;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/**
 * Decides every gear change of one Transmission. Pure logic with no world access, so it can be
 * tested directly. The block entity feeds it role signal strengths, shift pulses and computer
 * requests, applies the gear it returns and reports back with {@link #markShifted}.
 * <p>
 * Precedence: neutral hold, then analog, then pulses and computer requests. Up/Down act on a
 * rising edge (0 to anything above 0); Analog and Neutral are level-triggered and re-evaluated
 * every tick so a change that arrives during the cooldown still lands.
 */
public final class ShiftRules {
    /** Minimum ticks between shifts. Create destroys a block whose speed changes too often. */
    public static final int COOLDOWN_TICKS = 4;

    public enum Refusal {
        COOLDOWN("cooldown"),
        NEUTRAL_HOLD("neutral_hold"),
        ANALOG_OVERRIDE("analog_override"),
        LIMIT("limit");

        private final String id;

        Refusal(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    public enum Control {
        NEUTRAL_HOLD("neutral_hold"),
        ANALOG("analog"),
        FREE("free");

        private final String id;

        Control(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    /**
     * @param accepted false for refusals and for signals that need nothing
     * @param target   the gear to switch to, or null when the gear stays as it is
     * @param refusal  why a request was refused, or null
     */
    public record Result(boolean accepted, @Nullable Gear target, @Nullable Refusal refusal) {
        public static final Result NOTHING = new Result(false, null, null);
        public static final Result UNCHANGED = new Result(true, null, null);

        static Result shiftTo(Gear gear) {
            return new Result(true, gear, null);
        }

        static Result refuse(Refusal refusal) {
            return new Result(false, null, refusal);
        }
    }

    private final int[] strengths = new int[Role.VALUES.length];
    private long lastShift = Long.MIN_VALUE / 2;

    public int strength(Role role) {
        return strengths[role.ordinal()];
    }

    public Control control() {
        if (strength(Role.NEUTRAL) > 0)
            return Control.NEUTRAL_HOLD;
        if (strength(Role.ANALOG) > 0)
            return Control.ANALOG;
        return Control.FREE;
    }

    /** A role's effective strength (max of its link and its wire) is now {@code strength}. */
    public Result onSignal(Role role, int strength, Gear current, long now) {
        int previous = strengths[role.ordinal()];
        strengths[role.ordinal()] = strength;
        boolean risingEdge = previous == 0 && strength > 0;
        return switch (role) {
            case UP -> risingEdge ? pulse(current.up(), now) : Result.NOTHING;
            case DOWN -> risingEdge ? pulse(current.down(), now) : Result.NOTHING;
            case ANALOG, NEUTRAL -> followLevel(current, now);
        };
    }

    public Result shiftUp(Gear current, long now) {
        return pulse(current.up(), now);
    }

    public Result shiftDown(Gear current, long now) {
        return pulse(current.down(), now);
    }

    /** A computer asks for a specific gear. */
    public Result request(Gear target, Gear current, long now) {
        Refusal blocked = blockedBy();
        if (blocked != null)
            return Result.refuse(blocked);
        if (target == current)
            return Result.UNCHANGED;
        if (coolingDown(now))
            return Result.refuse(Refusal.COOLDOWN);
        return Result.shiftTo(target);
    }

    /** Called every server tick. */
    public Result tick(Gear current, long now) {
        return followLevel(current, now);
    }

    public void markShifted(long now) {
        lastShift = now;
    }

    private Result pulse(@Nullable Gear target, long now) {
        Refusal blocked = blockedBy();
        if (blocked != null)
            return Result.refuse(blocked);
        if (target == null)
            return Result.refuse(Refusal.LIMIT);
        if (coolingDown(now))
            return Result.refuse(Refusal.COOLDOWN);
        return Result.shiftTo(target);
    }

    private Result followLevel(Gear current, long now) {
        Gear target = levelTarget();
        if (target == null || target == current || coolingDown(now))
            return Result.NOTHING;
        return Result.shiftTo(target);
    }

    @Nullable
    private Gear levelTarget() {
        if (strength(Role.NEUTRAL) > 0)
            return Gear.NEUTRAL;
        int analog = strength(Role.ANALOG);
        return analog > 0 ? Gear.forAnalog(analog) : null;
    }

    @Nullable
    private Refusal blockedBy() {
        return switch (control()) {
            case NEUTRAL_HOLD -> Refusal.NEUTRAL_HOLD;
            case ANALOG -> Refusal.ANALOG_OVERRIDE;
            case FREE -> null;
        };
    }

    private boolean coolingDown(long now) {
        return now - lastShift < COOLDOWN_TICKS;
    }

    /** Strengths are saved so a signal held across a reload is not mistaken for a new pulse. */
    public CompoundTag write() {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("Strengths", strengths.clone());
        return tag;
    }

    public void read(CompoundTag tag) {
        int[] saved = tag.getIntArray("Strengths");
        for (int i = 0; i < strengths.length && i < saved.length; i++)
            strengths[i] = saved[i];
    }
}
