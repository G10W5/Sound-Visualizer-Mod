package com.example.soundvisualizer;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.text.Text;

import java.util.ArrayList;

public class SoundVisualizerModMenu implements ModMenuApi {
        @Override
        public ConfigScreenFactory<?> getModConfigScreenFactory() {
                return parent -> {
                        SoundVisualizerConfig config = SoundVisualizerConfig.INSTANCE;
                        ConfigBuilder builder = ConfigBuilder.create()
                                        .setParentScreen(parent)
                                        .setTitle(Text.translatable("title.soundvisualizer.config"));

                        ConfigEntryBuilder eb = builder.entryBuilder();
                        ConfigCategory general = builder
                                        .getOrCreateCategory(Text.translatable("category.soundvisualizer.general"));

                        // Color Wheel Picker (Cloth Config's colorField renders a color wheel popup)
                        general.addEntry(eb
                                        .startColorField(Text.translatable("option.soundvisualizer.color"),
                                                        config.indicatorColor)
                                        .setDefaultValue(0xFF0000)
                                        .setTooltip(Text.translatable("tooltip.soundvisualizer.color"))
                                        .setSaveConsumer(val -> {
                                                // Cloth Config saves ARGB — strip alpha so our renderer works with pure
                                                // RGB
                                                config.indicatorColor = val & 0xFFFFFF;
                                        })
                                        .build());

                        // Indicator Size (radius of the dot/arc)
                        general.addEntry(eb
                                        .startIntSlider(Text.translatable("option.soundvisualizer.size"),
                                                        (int) config.indicatorSize, 1, 16)
                                        .setDefaultValue(4)
                                        .setTooltip(Text.translatable("tooltip.soundvisualizer.size"))
                                        .setSaveConsumer(val -> config.indicatorSize = val)
                                        .build());

                        // Indicator Width (stroke thickness) — NEW
                        general.addEntry(eb
                                        .startIntSlider(Text.translatable("option.soundvisualizer.width"),
                                                        config.indicatorWidth, 1, 12)
                                        .setDefaultValue(3)
                                        .setTooltip(Text.translatable("tooltip.soundvisualizer.width"))
                                        .setSaveConsumer(val -> config.indicatorWidth = val)
                                        .build());

                        // Radius
                        general.addEntry(eb
                                        .startIntSlider(Text.translatable("option.soundvisualizer.radius"),
                                                        (int) config.radius, 20, 200)
                                        .setDefaultValue(50)
                                        .setTooltip(Text.translatable("tooltip.soundvisualizer.radius"))
                                        .setSaveConsumer(val -> config.radius = val)
                                        .build());

                        // Style selector
                        general.addEntry(eb
                                        .startSelector(Text.translatable("option.soundvisualizer.style"),
                                                        new String[] { "ARCH", "CHEVRON", "DOT" },
                                                        config.style)
                                        .setDefaultValue("ARCH")
                                        .setSaveConsumer(val -> config.style = val)
                                        .build());

                        // Fade Time
                        general.addEntry(eb
                                        .startFloatField(Text.translatable("option.soundvisualizer.fadeTime"),
                                                        config.fadeTimeSeconds)
                                        .setDefaultValue(2.0f)
                                        .setSaveConsumer(val -> config.fadeTimeSeconds = val)
                                        .build());

                        // Distance Scaling
                        general.addEntry(eb
                                        .startBooleanToggle(Text.translatable("option.soundvisualizer.distanceScaling"),
                                                        config.distanceScaling)
                                        .setDefaultValue(true)
                                        .setSaveConsumer(val -> config.distanceScaling = val)
                                        .build());

                        // Show Icons
                        general.addEntry(eb
                                        .startBooleanToggle(Text.translatable("option.soundvisualizer.showIcons"),
                                                        config.showIcons)
                                        .setDefaultValue(false)
                                        .setSaveConsumer(val -> config.showIcons = val)
                                        .build());

                        // Icon Offset
                        general.addEntry(eb
                                        .startIntSlider(Text.translatable("option.soundvisualizer.iconOffset"),
                                                        (int) config.iconOffset, 0, 60)
                                        .setDefaultValue(10)
                                        .setSaveConsumer(val -> config.iconOffset = val)
                                        .build());

                        // Subtitle Only
                        general.addEntry(eb
                                        .startBooleanToggle(Text.translatable("option.soundvisualizer.subtitleOnly"),
                                                        config.subtitleOnly)
                                        .setDefaultValue(true)
                                        .setSaveConsumer(val -> config.subtitleOnly = val)
                                        .build());

                        // Max Hearing Distance
                        general.addEntry(eb
                                        .startIntSlider(Text.translatable("option.soundvisualizer.maxHearingDistance"),
                                                        (int) config.maxHearingDistance, 8, 128)
                                        .setDefaultValue(16)
                                        .setTooltip(Text.translatable("tooltip.soundvisualizer.maxHearingDistance"))
                                        .setSaveConsumer(val -> config.maxHearingDistance = val)
                                        .build());

                        // --- Filter Settings — Whitelist & Blacklist as proper lists ---
                        ConfigCategory filters = builder
                                        .getOrCreateCategory(Text.translatable("category.soundvisualizer.filters"));

                        // Whitelist: shows a list with +/- buttons per entry
                        filters.addEntry(eb
                                        .startStrList(Text.translatable("option.soundvisualizer.whitelist"),
                                                        new ArrayList<>(config.whitelist))
                                        .setDefaultValue(new ArrayList<>())
                                        .setTooltip(Text.translatable("tooltip.soundvisualizer.whitelist"))
                                        .setSaveConsumer(val -> config.whitelist = val)
                                        .build());

                        // Blacklist: same pattern
                        filters.addEntry(eb
                                        .startStrList(Text.translatable("option.soundvisualizer.blacklist"),
                                                        new ArrayList<>(config.blacklist))
                                        .setDefaultValue(new ArrayList<>(java.util.List.of("minecraft:weather.rain")))
                                        .setTooltip(Text.translatable("tooltip.soundvisualizer.blacklist"))
                                        .setSaveConsumer(val -> config.blacklist = val)
                                        .build());

                        builder.setSavingRunnable(config::save);
                        return builder.build();
                };
        }
}
