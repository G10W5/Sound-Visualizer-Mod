package com.example.soundvisualizer;

public enum SoundCategory {
    HOSTILE("Hostile", 0xFF0000), // Red
    FRIENDLY("Friendly", 0x00FF00), // Green
    AMBIENT("Ambient", 0x00FFFF), // Cyan
    BLOCKS("Blocks", 0xFFFF00), // Yellow
    PLAYER("Player", 0xFFFFFF), // White
    NEUTRAL("Neutral", 0xAAAAAA); // Grey

    private final String name;
    private final int defaultColor;

    SoundCategory(String name, int defaultColor) {
        this.name = name;
        this.defaultColor = defaultColor;
    }

    public String getName() {
        return name;
    }

    public int getDefaultColor() {
        return defaultColor;
    }
}
