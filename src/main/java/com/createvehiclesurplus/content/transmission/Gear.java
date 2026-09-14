package com.createvehiclesurplus.content.transmission;

import org.jetbrains.annotations.Nullable;

/**
 * The Transmission's gears in shift order. Every forward ratio is a reduction: a ratio of at most 1
 * can never push the output past Create's max rotation speed, which Create enforces by breaking
 * the block.
 */
public enum Gear {
    REVERSE("R", -1f),
    NEUTRAL("N", 0f),
    QUARTER("1/4", 0.25f),
    HALF("1/2", 0.5f),
    THREE_QUARTERS("3/4", 0.75f),
    DIRECT("1", 1f);

    private static final Gear[] VALUES = values();

    private final String label;
    private final float ratio;

    Gear(String label, float ratio) {
        this.label = label;
        this.ratio = ratio;
    }

    public String label() {
        return label;
    }

    /** Output speed divided by input speed. */
    public float ratio() {
        return ratio;
    }

    /** Index into the gear list; also the value of the block's {@code gear} property. */
    public int index() {
        return ordinal();
    }

    public static Gear byIndex(int index) {
        return VALUES[Math.max(0, Math.min(VALUES.length - 1, index))];
    }

    @Nullable
    public static Gear byLabel(String label) {
        for (Gear gear : VALUES)
            if (gear.label.equals(label))
                return gear;
        return null;
    }

    /** The analog side's bands: 1-2 R, 3-5 N, 6-8 1/4, 9-11 1/2, 12-13 3/4, 14-15 1. Call with strength > 0. */
    public static Gear forAnalog(int strength) {
        if (strength <= 2)
            return REVERSE;
        if (strength <= 5)
            return NEUTRAL;
        if (strength <= 8)
            return QUARTER;
        if (strength <= 11)
            return HALF;
        if (strength <= 13)
            return THREE_QUARTERS;
        return DIRECT;
    }

    @Nullable
    public Gear up() {
        return ordinal() + 1 < VALUES.length ? VALUES[ordinal() + 1] : null;
    }

    @Nullable
    public Gear down() {
        return ordinal() > 0 ? VALUES[ordinal() - 1] : null;
    }
}
