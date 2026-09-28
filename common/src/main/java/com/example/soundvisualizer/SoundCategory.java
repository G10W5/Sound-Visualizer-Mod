package com.example.soundvisualizer;

public enum SoundCategory {
    HOSTILE("Hostile", 0xFF0000, 100), // Red
    PLAYER("Player", 0xFFFFFF, 80),   // White
    NEUTRAL("Neutral", 0xAAAAAA, 60), // Grey / Footsteps
    FRIENDLY("Friendly", 0x00FF00, 40), // Green
    BLOCKS("Blocks", 0xFFFF00, 20),   // Yellow
    AMBIENT("Ambient", 0x00FFFF, 10);  // Cyan

    private final String name;
    private final int defaultColor;
    private final int priority;

    SoundCategory(String name, int defaultColor, int priority) {
        this.name = name;
        this.defaultColor = defaultColor;
        this.priority = priority;
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
}
