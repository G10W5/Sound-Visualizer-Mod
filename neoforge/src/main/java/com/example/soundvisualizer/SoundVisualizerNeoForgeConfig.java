package com.example.soundvisualizer;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

public class SoundVisualizerNeoForgeConfig {
    public static Screen create(Screen parent) {
        SoundVisualizerConfig config = SoundVisualizerConfig.INSTANCE;
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("title.soundvisualizer.config"));

        ConfigEntryBuilder eb = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(
                Component.translatable("category.soundvisualizer.general"));

        general.addEntry(eb
                .startColorField(Component.translatable("option.soundvisualizer.color"),
                        config.indicatorColor)
                .setDefaultValue(0xFF0000)
                .setTooltip(Component.translatable("tooltip.soundvisualizer.color"))
                .setSaveConsumer(val -> config.indicatorColor = val & 0xFFFFFF)
                .build());

        general.addEntry(eb
                .startColorField(Component.translatable("option.soundvisualizer.colorHostile"),
                        config.colorHostile)
                .setDefaultValue(SoundCategory.HOSTILE.getDefaultColor())
                .setSaveConsumer(val -> config.colorHostile = val & 0xFFFFFF)
                .build());

        general.addEntry(eb
                .startColorField(Component.translatable("option.soundvisualizer.colorFriendly"),
                        config.colorFriendly)
                .setDefaultValue(SoundCategory.FRIENDLY.getDefaultColor())
                .setSaveConsumer(val -> config.colorFriendly = val & 0xFFFFFF)
                .build());

        general.addEntry(eb
                .startColorField(Component.translatable("option.soundvisualizer.colorAmbient"),
                        config.colorAmbient)
                .setDefaultValue(SoundCategory.AMBIENT.getDefaultColor())
                .setSaveConsumer(val -> config.colorAmbient = val & 0xFFFFFF)
                .build());

        general.addEntry(eb
                .startColorField(Component.translatable("option.soundvisualizer.colorBlocks"),
                        config.colorBlocks)
                .setDefaultValue(SoundCategory.BLOCKS.getDefaultColor())
                .setSaveConsumer(val -> config.colorBlocks = val & 0xFFFFFF)
                .build());

        general.addEntry(eb
                .startColorField(Component.translatable("option.soundvisualizer.colorPlayer"),
                        config.colorPlayer)
                .setDefaultValue(SoundCategory.PLAYER.getDefaultColor())
                .setSaveConsumer(val -> config.colorPlayer = val & 0xFFFFFF)
                .build());

        general.addEntry(eb
                .startColorField(Component.translatable("option.soundvisualizer.colorNeutral"),
                        config.colorNeutral)
                .setDefaultValue(SoundCategory.NEUTRAL.getDefaultColor())
                .setSaveConsumer(val -> config.colorNeutral = val & 0xFFFFFF)
                .build());

        general.addEntry(eb
                .startIntSlider(Component.translatable("option.soundvisualizer.size"),
                        (int) config.indicatorSize, 1, 16)
                .setDefaultValue(4)
                .setTooltip(Component.translatable("tooltip.soundvisualizer.size"))
                .setSaveConsumer(val -> config.indicatorSize = val)
                .build());

        general.addEntry(eb
                .startIntSlider(Component.translatable("option.soundvisualizer.width"),
                        config.indicatorWidth, 1, 12)
                .setDefaultValue(3)
                .setTooltip(Component.translatable("tooltip.soundvisualizer.width"))
                .setSaveConsumer(val -> config.indicatorWidth = val)
                .build());

        general.addEntry(eb
                .startIntSlider(Component.translatable("option.soundvisualizer.radius"),
                        (int) config.radius, 20, 200)
                .setDefaultValue(50)
                .setTooltip(Component.translatable("tooltip.soundvisualizer.radius"))
                .setSaveConsumer(val -> config.radius = val)
                .build());

        general.addEntry(eb
                .startSelector(Component.translatable("option.soundvisualizer.style"),
                        new String[] { "ARCH", "CHEVRON", "DOT" }, config.style)
                .setDefaultValue("ARCH")
                .setSaveConsumer(val -> config.style = val)
                .build());

        general.addEntry(eb
                .startFloatField(Component.translatable("option.soundvisualizer.fadeTime"),
                        config.fadeTimeSeconds)
                .setDefaultValue(2.0f)
                .setSaveConsumer(val -> config.fadeTimeSeconds = val)
                .build());

        general.addEntry(eb
                .startBooleanToggle(
                        Component.translatable(
                                "option.soundvisualizer.distanceScaling"),
                        config.distanceScaling)
                .setDefaultValue(true)
                .setSaveConsumer(val -> config.distanceScaling = val)
                .build());

        general.addEntry(eb
                .startBooleanToggle(Component.translatable("option.soundvisualizer.showIcons"),
                        config.showIcons)
                .setDefaultValue(false)
                .setSaveConsumer(val -> config.showIcons = val)
                .build());

        general.addEntry(eb
                .startIntSlider(Component.translatable("option.soundvisualizer.iconOffset"),
                        (int) config.iconOffset, 0, 60)
                .setDefaultValue(10)
                .setSaveConsumer(val -> config.iconOffset = val)
                .build());

        general.addEntry(eb
                .startBooleanToggle(
                        Component.translatable("option.soundvisualizer.subtitleOnly"),
                        config.subtitleOnly)
                .setDefaultValue(true)
                .setSaveConsumer(val -> config.subtitleOnly = val)
                .build());

        general.addEntry(eb
                .startIntSlider(Component
                        .translatable("option.soundvisualizer.maxHearingDistance"),
                        (int) config.maxHearingDistance, 8, 128)
                .setDefaultValue(16)
                .setTooltip(Component
                        .translatable("tooltip.soundvisualizer.maxHearingDistance"))
                .setSaveConsumer(val -> config.maxHearingDistance = val)
                .build());

        ConfigCategory filters = builder.getOrCreateCategory(
                Component.translatable("category.soundvisualizer.filters"));

        filters.addEntry(eb
                .startStrList(Component.translatable("option.soundvisualizer.whitelist"),
                        new ArrayList<>(config.whitelist))
                .setDefaultValue(new ArrayList<>())
                .setTooltip(Component.translatable("tooltip.soundvisualizer.whitelist"))
                .setSaveConsumer(val -> config.whitelist = val)
                .build());

        filters.addEntry(eb
                .startStrList(Component.translatable("option.soundvisualizer.blacklist"),
                        new ArrayList<>(config.blacklist))
                .setDefaultValue(new ArrayList<>(java.util.List.of("minecraft:weather.rain")))
                .setTooltip(Component.translatable("tooltip.soundvisualizer.blacklist"))
                .setSaveConsumer(val -> config.blacklist = val)
                .build());

        builder.setSavingRunnable(config::save);
        return builder.build();
    }
}
