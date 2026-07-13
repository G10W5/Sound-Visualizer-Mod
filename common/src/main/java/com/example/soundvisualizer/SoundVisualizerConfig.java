package com.example.soundvisualizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public class SoundVisualizerConfig {
    public static final Logger LOGGER = LoggerFactory.getLogger("soundvisualizer-config");
    public static final SoundVisualizerConfig INSTANCE = new SoundVisualizerConfig();

    public int colorHostile = SoundCategory.HOSTILE.getDefaultColor();
    public int colorFriendly = SoundCategory.FRIENDLY.getDefaultColor();
    public int colorAmbient = SoundCategory.AMBIENT.getDefaultColor();
    public int colorBlocks = SoundCategory.BLOCKS.getDefaultColor();
    public int colorPlayer = SoundCategory.PLAYER.getDefaultColor();
    public int colorNeutral = SoundCategory.NEUTRAL.getDefaultColor();
    
    public double radius = 50.0;
    public float fadeTimeSeconds = 2.0f;
    
    public float arcThickness = 32.0f;
    public float arcSpanDegrees = 30.0f;
    public float iconScale = 1.0f;

    public List<String> whitelist = new ArrayList<>();
    public List<String> blacklist = new ArrayList<>(List.of("minecraft:weather.rain"));
    public boolean showIcons = true;
    public boolean distanceScaling = true;
    public float maxHearingDistance = 16.0f;
    public float opacity = 1.0f;
    public Set<SoundCategory> disabledCategories = EnumSet.noneOf(SoundCategory.class);

    private Path configPath;

    private SoundVisualizerConfig() {
    }

    public void init(Path configPath) {
        this.configPath = configPath;
        try {
            Files.createDirectories(configPath.getParent());
        } catch (IOException e) {
            LOGGER.error("Failed to create config directory", e);
        }
        load();
    }

    private static int parseColor(String hex, int defaultRgb) {
        try {
            hex = hex.trim().replace("#", "");
            if (hex.length() == 8)
                hex = hex.substring(2);
            return Integer.parseInt(hex, 16) & 0xFFFFFF;
        } catch (Exception e) {
            return defaultRgb;
        }
    }

    private static List<String> parseList(String raw) {
        if (raw == null || raw.isBlank())
            return new ArrayList<>();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static String listToString(List<String> list) {
        if (list == null || list.isEmpty())
            return "";
        return String.join(",", list);
    }

    private static Set<SoundCategory> parseDisabledCategories(String raw) {
        Set<SoundCategory> disabled = EnumSet.noneOf(SoundCategory.class);
        if (raw == null || raw.isBlank()) return disabled;
        for (String s : raw.split(",")) {
            String trimmed = s.trim().toUpperCase();
            if (!trimmed.isEmpty()) {
                try {
                    disabled.add(SoundCategory.valueOf(trimmed));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return disabled;
    }

    private static String disabledCategoriesToString(Set<SoundCategory> set) {
        if (set == null || set.isEmpty()) return "";
        return set.stream().map(Enum::name).collect(Collectors.joining(","));
    }

    public void load() {
        if (configPath == null) {
            LOGGER.warn("Config path not set, using defaults");
            return;
        }
        if (!Files.exists(configPath)) {
            LOGGER.info("Config file not found, saving defaults");
            save();
            return;
        }
        try (InputStream in = Files.newInputStream(configPath)) {
            Properties props = new Properties();
            props.load(in);

            colorHostile = parseColor(props.getProperty("colorHostile", "FF0000"), SoundCategory.HOSTILE.getDefaultColor());
            colorFriendly = parseColor(props.getProperty("colorFriendly", "00FF00"), SoundCategory.FRIENDLY.getDefaultColor());
            colorAmbient = parseColor(props.getProperty("colorAmbient", "00FFFF"), SoundCategory.AMBIENT.getDefaultColor());
            colorBlocks = parseColor(props.getProperty("colorBlocks", "FFFF00"), SoundCategory.BLOCKS.getDefaultColor());
            colorPlayer = parseColor(props.getProperty("colorPlayer", "FFFFFF"), SoundCategory.PLAYER.getDefaultColor());
            colorNeutral = parseColor(props.getProperty("colorNeutral", "AAAAAA"), SoundCategory.NEUTRAL.getDefaultColor());
            
            radius = Double.parseDouble(props.getProperty("radius", "50.0"));
            fadeTimeSeconds = Float.parseFloat(props.getProperty("fadeTimeSeconds", "2.0"));
            
            arcThickness = Float.parseFloat(props.getProperty("arcThickness", "32.0"));
            arcSpanDegrees = Float.parseFloat(props.getProperty("arcSpanDegrees", "30.0"));
            iconScale = Float.parseFloat(props.getProperty("iconScale", "1.0"));
            
            whitelist = parseList(props.getProperty("whitelist", ""));
            blacklist = parseList(props.getProperty("blacklist", "minecraft:weather.rain"));
            showIcons = Boolean.parseBoolean(props.getProperty("showIcons", "true"));
            distanceScaling = Boolean.parseBoolean(props.getProperty("distanceScaling", "true"));
            maxHearingDistance = Float.parseFloat(props.getProperty("maxHearingDistance", "16.0"));
            opacity = Float.parseFloat(props.getProperty("opacity", props.getProperty("transparency", "1.0")));
            disabledCategories = parseDisabledCategories(props.getProperty("disabledCategories", ""));
            LOGGER.info("Config loaded successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to load config, using defaults", e);
        }
    }

    public void save() {
        if (configPath == null) {
            LOGGER.warn("Config path not set, cannot save");
            return;
        }
        try {
            Files.createDirectories(configPath.getParent());
            Properties props = new Properties();
            props.setProperty("colorHostile", String.format("%06X", colorHostile & 0xFFFFFF));
            props.setProperty("colorFriendly", String.format("%06X", colorFriendly & 0xFFFFFF));
            props.setProperty("colorAmbient", String.format("%06X", colorAmbient & 0xFFFFFF));
            props.setProperty("colorBlocks", String.format("%06X", colorBlocks & 0xFFFFFF));
            props.setProperty("colorPlayer", String.format("%06X", colorPlayer & 0xFFFFFF));
            props.setProperty("colorNeutral", String.format("%06X", colorNeutral & 0xFFFFFF));
            
            props.setProperty("radius", String.valueOf(radius));
            props.setProperty("fadeTimeSeconds", String.valueOf(fadeTimeSeconds));
            
            props.setProperty("arcThickness", String.valueOf(arcThickness));
            props.setProperty("arcSpanDegrees", String.valueOf(arcSpanDegrees));
            props.setProperty("iconScale", String.valueOf(iconScale));
            
            props.setProperty("whitelist", listToString(whitelist));
            props.setProperty("blacklist", listToString(blacklist));
            props.setProperty("showIcons", String.valueOf(showIcons));
            props.setProperty("distanceScaling", String.valueOf(distanceScaling));
            props.setProperty("maxHearingDistance", String.valueOf(maxHearingDistance));
            props.setProperty("opacity", String.valueOf(opacity));
            props.setProperty("disabledCategories", disabledCategoriesToString(disabledCategories));
            
            try (OutputStream out = Files.newOutputStream(configPath)) {
                props.store(out, "Sound Visualizer Configuration");
            }
            LOGGER.info("Config saved to {}", configPath);
        } catch (IOException e) {
            LOGGER.error("Failed to save config", e);
        }
    }
}
