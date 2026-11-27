package com.example.soundvisualizer;

import net.minecraft.resources.Identifier;

public enum SoundCategory {
    HOSTILE("Hostile", 0xFF3B3B, 100, "hostile"),
    PLAYER("Player", 0xF2F2F2, 80, "player"),
    NEUTRAL("Neutral", 0xB4B9C2, 60, "footsteps"),
    FRIENDLY("Friendly", 0x4DFF7A, 40, "friendly"),
    BLOCKS("Blocks", 0xFFD23F, 20, "block"),
    AMBIENT("Ambient", 0x3FE0FF, 10, "ambient");

    private final String name;
    private final int defaultColor;
    private final int priority;
    private final Identifier icon;

    SoundCategory(String name, int defaultColor, int priority, String iconName) {
        this.name = name;
        this.defaultColor = defaultColor;
        this.priority = priority;
        this.icon = Identifier.fromNamespaceAndPath("soundvisualizer", "textures/gui/" + iconName + ".png");
    }

    public String getName() {
        return name;
    }

    public int getDefaultColor() {
        return defaultColor;
    }

    public int getPriority() {
        return priority;
    }

    public Identifier getIcon() {
        return icon;
    }
}
