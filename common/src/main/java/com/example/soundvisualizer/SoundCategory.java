package com.example.soundvisualizer;

public enum SoundCategory {
    HOSTILE("Hostile", 0xFFFF0000), // Red
    FRIENDLY("Friendly", 0xFF00FF00), // Green
    AMBIENT("Ambient", 0xFF00FFFF), // Cyan
    BLOCKS("Blocks", 0xFFFFFF00), // Yellow
    PLAYER("Player", 0xFFFFFFFF), // White
    NEUTRAL("Neutral", 0xFFAAAAAA); // Grey

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
