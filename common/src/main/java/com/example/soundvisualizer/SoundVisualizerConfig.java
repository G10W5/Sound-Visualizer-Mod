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
import java.util.function.Function;
import java.util.stream.Collectors;

public class SoundVisualizerConfig {
    public static final Logger LOGGER = LoggerFactory.getLogger("soundvisualizer-config");
    public static final SoundVisualizerConfig INSTANCE = new SoundVisualizerConfig();
    private static final int CONFIG_VERSION = 5;

    public enum IndicatorStyle {
        ARC, DASHED, CHEVRON
    }

    public enum Preset {
        CUSTOM, MINIMAL, BALANCED, CINEMATIC, TACTICAL
    }

    // General (Minimal preset defaults)
    public boolean enabled = true;
    public IndicatorStyle indicatorStyle = IndicatorStyle.ARC;
    public double radius = 50.0;
    public float lineThickness = 2.5f;
    public float arcSpanDegrees = 28.0f;
    public float glowIntensity = 0.35f;
    public float opacity = 1.0f;
    public float fadeTimeSeconds = 1.5f;
    public boolean showRipples = false;

    // Icons
    public boolean showIcons = true;
    public float iconScale = 0.9f;
    public boolean iconBackdrop = false;
    public boolean tintIcons = false;

    // Detection & Stacking
    public float maxHearingDistance = 16.0f;
    public boolean hideOwnSounds = true;
    public float mergeAngle = 18.0f;
    public boolean stackSounds = true;
    public float stackAngle = 36.0f;
    public boolean splitOnLook = true;
    public boolean smoothTracking = true;
    public float smoothingSpeed = 14.0f;
    public boolean distanceScaling = true;
    public List<String> whitelist = new ArrayList<>();
    public List<String> blacklist = new ArrayList<>(List.of("minecraft:weather.rain"));

    // Categories
    public int colorHostile = SoundCategory.HOSTILE.getDefaultColor();
    public int colorFriendly = SoundCategory.FRIENDLY.getDefaultColor();
    public int colorAmbient = SoundCategory.AMBIENT.getDefaultColor();
    public int colorBlocks = SoundCategory.BLOCKS.getDefaultColor();
    public int colorPlayer = SoundCategory.PLAYER.getDefaultColor();
    public int colorNeutral = SoundCategory.NEUTRAL.getDefaultColor();
    public Set<SoundCategory> disabledCategories = EnumSet.noneOf(SoundCategory.class);

    private Path configPath;

    public SoundVisualizerConfig() {
    }

    public SoundVisualizerConfig copyVisual() {
        SoundVisualizerConfig copy = new SoundVisualizerConfig();
        copy.enabled = this.enabled;
        copy.indicatorStyle = this.indicatorStyle;
        copy.radius = this.radius;
        copy.lineThickness = this.lineThickness;
        copy.arcSpanDegrees = this.arcSpanDegrees;
        copy.glowIntensity = this.glowIntensity;
        copy.opacity = this.opacity;
        copy.fadeTimeSeconds = this.fadeTimeSeconds;
        copy.showRipples = this.showRipples;
        copy.showIcons = this.showIcons;
        copy.iconScale = this.iconScale;
        copy.iconBackdrop = this.iconBackdrop;
        copy.tintIcons = this.tintIcons;
        copy.stackSounds = this.stackSounds;
        copy.stackAngle = this.stackAngle;
        copy.splitOnLook = this.splitOnLook;
        copy.colorHostile = this.colorHostile;
        copy.colorFriendly = this.colorFriendly;
        copy.colorAmbient = this.colorAmbient;
        copy.colorBlocks = this.colorBlocks;
        copy.colorPlayer = this.colorPlayer;
        copy.colorNeutral = this.colorNeutral;
        return copy;
    }

    public void init(Path configPath) {
        this.configPath = configPath;
        load();
    }

    public int getColor(SoundCategory category) {
        return switch (category) {
            case HOSTILE -> colorHostile;
            case FRIENDLY -> colorFriendly;
            case NEUTRAL -> colorNeutral;
            case BLOCKS -> colorBlocks;
            case PLAYER -> colorPlayer;
            case AMBIENT -> colorAmbient;
        };
    }

    public void setColor(SoundCategory category, int rgb) {
        rgb &= 0xFFFFFF;
        switch (category) {
            case HOSTILE -> colorHostile = rgb;
            case FRIENDLY -> colorFriendly = rgb;
            case NEUTRAL -> colorNeutral = rgb;
            case BLOCKS -> colorBlocks = rgb;
            case PLAYER -> colorPlayer = rgb;
            case AMBIENT -> colorAmbient = rgb;
        }
    }

    public void applyPreset(Preset preset) {
        switch (preset) {
            case MINIMAL -> set(IndicatorStyle.ARC, 50.0, 2.5f, 28.0f, 0.35f, 1.5f, false, false, 0.9f);
            case BALANCED -> set(IndicatorStyle.ARC, 56.0, 3.0f, 34.0f, 1.0f, 2.0f, true, true, 1.0f);
            case CINEMATIC -> set(IndicatorStyle.ARC, 66.0, 4.0f, 48.0f, 1.7f, 2.6f, true, true, 1.15f);
            case TACTICAL -> {
                set(IndicatorStyle.CHEVRON, 52.0, 2.5f, 30.0f, 0.6f, 1.4f, false, true, 0.9f);
                smoothingSpeed = 24.0f;
            }
            case CUSTOM -> {
            }
        }
    }

    private void set(IndicatorStyle style, double radius, float thickness, float span, float glow, float fade,
                     boolean ripples, boolean backdrop, float iconScale) {
        this.indicatorStyle = style;
        this.radius = radius;
        this.lineThickness = thickness;
        this.arcSpanDegrees = span;
        this.glowIntensity = glow;
        this.fadeTimeSeconds = fade;
        this.showRipples = ripples;
        this.iconBackdrop = backdrop;
        this.iconScale = iconScale;
        this.smoothingSpeed = 14.0f;
    }

    // ---------------------------------------------------------------- persistence

    public void load() {
        if (configPath == null) return;
        if (!Files.exists(configPath)) {
            save();
            return;
        }
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(configPath)) {
            p.load(in);
        } catch (Exception e) {
            LOGGER.error("Failed to read config, using defaults", e);
            return;
        }

        int version = get(p, "configVersion", 1, Integer::parseInt);
        enabled = get(p, "enabled", enabled, Boolean::parseBoolean);

        if (version >= 4) {
            indicatorStyle = get(p, "indicatorStyle", indicatorStyle, s -> IndicatorStyle.valueOf(s.toUpperCase()));
            radius = get(p, "radius", radius, Double::parseDouble);
            lineThickness = get(p, "lineThickness", lineThickness, Float::parseFloat);
            arcSpanDegrees = get(p, "arcSpanDegrees", arcSpanDegrees, Float::parseFloat);
            glowIntensity = get(p, "glowIntensity", glowIntensity, Float::parseFloat);
            opacity = get(p, "opacity", get(p, "transparency", opacity, Float::parseFloat), Float::parseFloat);
            fadeTimeSeconds = get(p, "fadeTimeSeconds", fadeTimeSeconds, Float::parseFloat);
            showRipples = get(p, "showRipples", showRipples, Boolean::parseBoolean);

            showIcons = get(p, "showIcons", showIcons, Boolean::parseBoolean);
            iconScale = get(p, "iconScale", iconScale, Float::parseFloat);
            iconBackdrop = get(p, "iconBackdrop", iconBackdrop, Boolean::parseBoolean);
            tintIcons = get(p, "tintIcons", tintIcons, Boolean::parseBoolean);
        } else {
            applyPreset(Preset.MINIMAL);
            opacity = get(p, "opacity", get(p, "transparency", opacity, Float::parseFloat), Float::parseFloat);
            showIcons = get(p, "showIcons", showIcons, Boolean::parseBoolean);
            tintIcons = get(p, "tintIcons", tintIcons, Boolean::parseBoolean);
        }

        maxHearingDistance = get(p, "maxHearingDistance", maxHearingDistance, Float::parseFloat);
        hideOwnSounds = get(p, "hideOwnSounds", hideOwnSounds, Boolean::parseBoolean);
        mergeAngle = get(p, "mergeAngle", mergeAngle, Float::parseFloat);
        stackSounds = get(p, "stackSounds", stackSounds, Boolean::parseBoolean);
        stackAngle = get(p, "stackAngle", stackAngle, Float::parseFloat);
        splitOnLook = get(p, "splitOnLook", splitOnLook, Boolean::parseBoolean);
        smoothTracking = get(p, "smoothTracking", smoothTracking, Boolean::parseBoolean);
        smoothingSpeed = get(p, "smoothingSpeed", smoothingSpeed, Float::parseFloat);
        distanceScaling = get(p, "distanceScaling", distanceScaling, Boolean::parseBoolean);
        whitelist = parseList(p.getProperty("whitelist", ""));
        blacklist = parseList(p.getProperty("blacklist", "minecraft:weather.rain"));

        for (SoundCategory c : SoundCategory.values()) {
            String raw = p.getProperty("color" + c.getName());
            if (raw != null) setColor(c, parseColor(raw, c.getDefaultColor()));
        }
        disabledCategories = parseCategories(p.getProperty("disabledCategories", ""));

        if (version < CONFIG_VERSION) save();
    }

    public void save() {
        if (configPath == null) return;
        Properties p = new Properties();
        p.setProperty("configVersion", String.valueOf(CONFIG_VERSION));
        p.setProperty("enabled", String.valueOf(enabled));
        p.setProperty("indicatorStyle", indicatorStyle.name());
        p.setProperty("radius", String.valueOf(radius));
        p.setProperty("lineThickness", String.valueOf(lineThickness));
        p.setProperty("arcSpanDegrees", String.valueOf(arcSpanDegrees));
        p.setProperty("glowIntensity", String.valueOf(glowIntensity));
        p.setProperty("opacity", String.valueOf(opacity));
        p.setProperty("fadeTimeSeconds", String.valueOf(fadeTimeSeconds));
        p.setProperty("showRipples", String.valueOf(showRipples));
        p.setProperty("showIcons", String.valueOf(showIcons));
        p.setProperty("iconScale", String.valueOf(iconScale));
        p.setProperty("iconBackdrop", String.valueOf(iconBackdrop));
        p.setProperty("tintIcons", String.valueOf(tintIcons));
        p.setProperty("maxHearingDistance", String.valueOf(maxHearingDistance));
        p.setProperty("hideOwnSounds", String.valueOf(hideOwnSounds));
        p.setProperty("mergeAngle", String.valueOf(mergeAngle));
        p.setProperty("stackSounds", String.valueOf(stackSounds));
        p.setProperty("stackAngle", String.valueOf(stackAngle));
        p.setProperty("splitOnLook", String.valueOf(splitOnLook));
        p.setProperty("smoothTracking", String.valueOf(smoothTracking));
        p.setProperty("smoothingSpeed", String.valueOf(smoothingSpeed));
        p.setProperty("distanceScaling", String.valueOf(distanceScaling));
        p.setProperty("whitelist", String.join(",", whitelist));
        p.setProperty("blacklist", String.join(",", blacklist));
        for (SoundCategory c : SoundCategory.values()) {
            p.setProperty("color" + c.getName(), String.format("%06X", getColor(c)));
        }
        p.setProperty("disabledCategories",
                disabledCategories.stream().map(Enum::name).collect(Collectors.joining(",")));
        try {
            Files.createDirectories(configPath.getParent());
            try (OutputStream out = Files.newOutputStream(configPath)) {
                p.store(out, "Sound Visualizer Configuration");
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save config", e);
        }
    }

    private static <T> T get(Properties p, String key, T fallback, Function<String, T> parser) {
        String raw = p.getProperty(key);
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return parser.apply(raw.trim());
        } catch (Exception e) {
            LOGGER.warn("Invalid value '{}' for {}, using default", raw, key);
            return fallback;
        }
    }

    private static int parseColor(String hex, int fallback) {
        try {
            hex = hex.trim().replace("#", "");
            if (hex.length() == 8) hex = hex.substring(2);
            return Integer.parseInt(hex, 16) & 0xFFFFFF;
        } catch (Exception e) {
            return fallback;
        }
    }

    private static List<String> parseList(String raw) {
        if (raw == null || raw.isBlank()) return new ArrayList<>();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static Set<SoundCategory> parseCategories(String raw) {
        Set<SoundCategory> set = EnumSet.noneOf(SoundCategory.class);
        if (raw == null) return set;
        for (String s : raw.split(",")) {
            try {
                if (!s.isBlank()) set.add(SoundCategory.valueOf(s.trim().toUpperCase()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return set;
    }
}
