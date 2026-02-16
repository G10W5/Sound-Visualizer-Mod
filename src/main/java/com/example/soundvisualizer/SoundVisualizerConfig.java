package com.example.soundvisualizer;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class SoundVisualizerConfig {
    public static final Logger LOGGER = LoggerFactory.getLogger("soundvisualizer-config");
    public static final SoundVisualizerConfig INSTANCE = new SoundVisualizerConfig();

    public int indicatorColor = 0xFF0000; // Red default as per user request
    public float indicatorSize = 4.0f;
    public double radius = 50.0;
    public float fadeTimeSeconds = 2.0f;
    public String style = "ARCH"; // ARCH, DOT, ICON
    public boolean showIcons = false;
    public boolean distanceScaling = true;
    public boolean subtitleOnly = true;

    private final Path configPath;

    private SoundVisualizerConfig() {
        this.configPath = FabricLoader.getInstance().getConfigDir().resolve("soundvisualizer.properties");
        load();
    }

    public void load() {
        if (!Files.exists(configPath)) {
            save(); // Create default config
            return;
        }

        try {
            Properties props = new Properties();
            props.load(Files.newInputStream(configPath));

            indicatorColor = Integer.parseInt(props.getProperty("indicatorColor", "FFFFFF"), 16);
            indicatorSize = Float.parseFloat(props.getProperty("indicatorSize", "4.0"));
            radius = Double.parseDouble(props.getProperty("radius", "50.0"));
            fadeTimeSeconds = Float.parseFloat(props.getProperty("fadeTimeSeconds", "2.0"));
            showIcons = Boolean.parseBoolean(props.getProperty("showIcons", "false"));
            style = props.getProperty("style", "ARCH");
            distanceScaling = Boolean.parseBoolean(props.getProperty("distanceScaling", "true"));
            subtitleOnly = Boolean.parseBoolean(props.getProperty("subtitleOnly", "true"));
        } catch (Exception e) {
            LOGGER.error("Failed to load config", e);
        }
    }

    public void save() {
        try {
            Properties props = new Properties();
            props.setProperty("indicatorColor", Integer.toHexString(indicatorColor).toUpperCase());
            props.setProperty("indicatorSize", String.valueOf(indicatorSize));
            props.setProperty("radius", String.valueOf(radius));
            props.setProperty("fadeTimeSeconds", String.valueOf(fadeTimeSeconds));
            props.setProperty("showIcons", String.valueOf(showIcons));
            props.setProperty("style", style);
            props.setProperty("distanceScaling", String.valueOf(distanceScaling));
            props.setProperty("subtitleOnly", String.valueOf(subtitleOnly));

            props.store(Files.newOutputStream(configPath), "Sound Visualizer Configuration");
        } catch (IOException e) {
            LOGGER.error("Failed to save config", e);
        }
    }
}
