package com.example.soundvisualizer;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

public class SoundVisualizerConfigScreen {

    public static Screen create(Screen parent) {
        SoundVisualizerConfig config = SoundVisualizerConfig.INSTANCE;
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("title.soundvisualizer.config"));

        ConfigEntryBuilder eb = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(
                Component.translatable("category.soundvisualizer.general"));

        // Global settings
        general.addEntry(eb.startIntSlider(Component.translatable("option.soundvisualizer.arcThickness"), (int) config.arcThickness, 8, 64)
                .setDefaultValue(32)
                .setSaveConsumer(val -> config.arcThickness = val)
                .build());

        general.addEntry(eb.startIntSlider(Component.translatable("option.soundvisualizer.radius"), (int) config.radius, 20, 200)
                .setDefaultValue(50)
                .setSaveConsumer(val -> config.radius = (double) val)
                .build());

        general.addEntry(eb.startFloatField(Component.translatable("option.soundvisualizer.fadeTime"), config.fadeTimeSeconds)
                .setDefaultValue(2.0f)
                .setSaveConsumer(val -> config.fadeTimeSeconds = val)
                .build());

        general.addEntry(eb.startBooleanToggle(Component.translatable("option.soundvisualizer.distanceScaling"), config.distanceScaling)
                .setDefaultValue(true)
                .setSaveConsumer(val -> config.distanceScaling = val)
                .build());

        general.addEntry(eb.startBooleanToggle(Component.translatable("option.soundvisualizer.showIcons"), config.showIcons)
                .setDefaultValue(true)
                .setSaveConsumer(val -> config.showIcons = val)
                .build());

        general.addEntry(eb.startIntSlider(Component.translatable("option.soundvisualizer.maxHearingDistance"), (int) config.maxHearingDistance, 8, 128)
                .setDefaultValue(16)
                .setSaveConsumer(val -> config.maxHearingDistance = (float) val)
                .build());

        general.addEntry(eb.startIntSlider(Component.translatable("option.soundvisualizer.iconScale"), (int) (config.iconScale * 10), 1, 20)
                .setDefaultValue(10)
                .setSaveConsumer(val -> config.iconScale = val / 10.0f)
                .build());

        general.addEntry(eb.startIntSlider(Component.translatable("option.soundvisualizer.opacity"), (int) (config.opacity * 100), 0, 100)
                .setDefaultValue(100)
                .setSaveConsumer(val -> config.opacity = val / 100.0f)
                .build());

        // Colors
        ConfigCategory colors = builder.getOrCreateCategory(
                Component.translatable("category.soundvisualizer.colors"));

        for (SoundCategory cat : SoundCategory.values()) {
            int colorVal = getCatColor(config, cat);
            colors.addEntry(eb.startBooleanToggle(
                    Component.translatable("option.soundvisualizer.enable" + cat.getName()), !config.disabledCategories.contains(cat))
                    .setDefaultValue(true)
                    .setSaveConsumer(val -> {
                        if (val) config.disabledCategories.remove(cat);
                        else config.disabledCategories.add(cat);
                    })
                    .build());
            colors.addEntry(eb.startColorField(
                    Component.translatable("option.soundvisualizer.color" + cat.getName()), colorVal & 0xFFFFFF)
                    .setDefaultValue(cat.getDefaultColor() & 0xFFFFFF)
                    .setSaveConsumer(val -> setCatColor(config, cat, val & 0xFFFFFF))
                    .build());
        }

        // Filters
        ConfigCategory filters = builder.getOrCreateCategory(
                Component.translatable("category.soundvisualizer.filters"));

        filters.addEntry(eb.startStrList(Component.translatable("option.soundvisualizer.whitelist"), new ArrayList<>(config.whitelist))
                .setDefaultValue(new ArrayList<>())
                .setSaveConsumer(val -> config.whitelist = val)
                .build());

        filters.addEntry(eb.startStrList(Component.translatable("option.soundvisualizer.blacklist"), new ArrayList<>(config.blacklist))
                .setDefaultValue(new ArrayList<>(java.util.List.of("minecraft:weather.rain")))
                .setSaveConsumer(val -> config.blacklist = val)
                .build());

        builder.setSavingRunnable(config::save);
        return builder.build();
    }

    private static int getCatColor(SoundVisualizerConfig config, SoundCategory cat) {
        return switch (cat) {
            case HOSTILE -> config.colorHostile;
            case FRIENDLY -> config.colorFriendly;
            case AMBIENT -> config.colorAmbient;
            case BLOCKS -> config.colorBlocks;
            case PLAYER -> config.colorPlayer;
            case NEUTRAL -> config.colorNeutral;
        };
    }

    private static void setCatColor(SoundVisualizerConfig config, SoundCategory cat, int color) {
        switch (cat) {
            case HOSTILE -> config.colorHostile = color;
            case FRIENDLY -> config.colorFriendly = color;
            case AMBIENT -> config.colorAmbient = color;
            case BLOCKS -> config.colorBlocks = color;
            case PLAYER -> config.colorPlayer = color;
            case NEUTRAL -> config.colorNeutral = color;
        }
    }
}
