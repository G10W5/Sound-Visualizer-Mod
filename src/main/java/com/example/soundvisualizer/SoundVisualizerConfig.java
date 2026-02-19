package com.example.soundvisualizer;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class SoundVisualizerConfig {
    public static final Logger LOGGER = LoggerFactory.getLogger("soundvisualizer-config");
    public static final SoundVisualizerConfig INSTANCE = new SoundVisualizerConfig();

    public int indicatorColor = 0xFF0000;
    public float indicatorSize = 4.0f;
    public int indicatorWidth = 3; // NEW: Stroke width for ARCH / CHEVRON
    public float iconSize = 4.0f;
    public double iconOffset = 10.0;
    public double radius = 50.0;
    public float fadeTimeSeconds = 2.0f;
    public String style = "ARCH";
    public List<String> whitelist = new ArrayList<>();
    public List<String> blacklist = new ArrayList<>(List.of("minecraft:weather.rain"));
    public boolean showIcons = false;
    public boolean distanceScaling = true;
    public boolean subtitleOnly = true;
    public float maxHearingDistance = 16.0f; // NEW: configurable max dist

    private final Path configPath;

    private SoundVisualizerConfig() {
        this.configPath = FabricLoader.getInstance().getConfigDir().resolve("soundvisualizer.properties");
        load();
    }

    /** Parse hex color safely. Handles both 6-char RGB and 8-char ARGB. */
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

    /** Convert a comma-separated string to a trimmed list of non-empty entries. */
    private static List<String> parseList(String raw) {
        if (raw == null || raw.isBlank())
            return new ArrayList<>();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /** Convert a list back to a comma-separated string for storage. */
    private static String listToString(List<String> list) {
        if (list == null || list.isEmpty())
            return "";
        return String.join(",", list);
    }

    public void load() {
        if (!Files.exists(configPath)) {
            save();
            return;
        }
        try {
            Properties props = new Properties();
            props.load(Files.newInputStream(configPath));

            indicatorColor = parseColor(props.getProperty("indicatorColor", "FF0000"), 0xFF0000);
            indicatorSize = Float.parseFloat(props.getProperty("indicatorSize", "4.0"));
            indicatorWidth = Integer.parseInt(props.getProperty("indicatorWidth", "3"));
            iconSize = Float.parseFloat(props.getProperty("iconSize", "4.0"));
            iconOffset = Double.parseDouble(props.getProperty("iconOffset", "10.0"));
            radius = Double.parseDouble(props.getProperty("radius", "50.0"));
            whitelist = parseList(props.getProperty("whitelist", ""));
            blacklist = parseList(props.getProperty("blacklist", "minecraft:weather.rain"));
            fadeTimeSeconds = Float.parseFloat(props.getProperty("fadeTimeSeconds", "2.0"));
            showIcons = Boolean.parseBoolean(props.getProperty("showIcons", "false"));
            style = props.getProperty("style", "ARCH");
            distanceScaling = Boolean.parseBoolean(props.getProperty("distanceScaling", "true"));
            subtitleOnly = Boolean.parseBoolean(props.getProperty("subtitleOnly", "true"));
            maxHearingDistance = Float.parseFloat(props.getProperty("maxHearingDistance", "16.0"));
        } catch (Exception e) {
            LOGGER.error("Failed to load config", e);
        }
    }

    public void save() {
        try {
            Properties props = new Properties();
            props.setProperty("indicatorColor", String.format("%06X", indicatorColor & 0xFFFFFF));
            props.setProperty("indicatorSize", String.valueOf(indicatorSize));
            props.setProperty("indicatorWidth", String.valueOf(indicatorWidth));
            props.setProperty("iconSize", String.valueOf(iconSize));
            props.setProperty("iconOffset", String.valueOf(iconOffset));
            props.setProperty("radius", String.valueOf(radius));
            props.setProperty("whitelist", listToString(whitelist));
            props.setProperty("blacklist", listToString(blacklist));
            props.setProperty("fadeTimeSeconds", String.valueOf(fadeTimeSeconds));
            props.setProperty("showIcons", String.valueOf(showIcons));
            props.setProperty("style", style);
            props.setProperty("distanceScaling", String.valueOf(distanceScaling));
            props.setProperty("subtitleOnly", String.valueOf(subtitleOnly));
            props.setProperty("maxHearingDistance", String.valueOf(maxHearingDistance));
            props.store(Files.newOutputStream(configPath), "Sound Visualizer Configuration");
        } catch (IOException e) {
            LOGGER.error("Failed to save config", e);
        }
    }
}
