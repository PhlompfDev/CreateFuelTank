package com.createvehiclesurplus.content.transmission;

/**
 * What a long face of the Transmission does. Declared in ring order around the shaft
 * (Up, Analog, Down, Neutral), so Up is opposite Down and Analog opposite Neutral.
 */
public enum Role {
    UP("up"),
    ANALOG("analog"),
    DOWN("down"),
    NEUTRAL("neutral");

    public static final Role[] VALUES = values();

    private final String id;

    Role(String id) {
        this.id = id;
    }

    /** Lower-case id used in NBT keys, lang keys and behaviour type names. */
    public String id() {
        return id;
    }
}
