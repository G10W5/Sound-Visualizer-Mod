package com.example.soundvisualizer;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.text.Text;

public class SoundVisualizerModMenu implements ModMenuApi {
        @Override
        public ConfigScreenFactory<?> getModConfigScreenFactory() {
                return parent -> {
                        SoundVisualizerConfig config = SoundVisualizerConfig.INSTANCE;
                        ConfigBuilder builder = ConfigBuilder.create()
                                        .setParentScreen(parent)
                                        .setTitle(Text.translatable("title.soundvisualizer.config"));

                        ConfigCategory general = builder
                                        .getOrCreateCategory(Text.translatable("category.soundvisualizer.general"));
                        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

                        general.addEntry(entryBuilder
                                        .startColorField(Text.translatable("option.soundvisualizer.color"),
                                                        config.indicatorColor)
                                        .setDefaultValue(0xFF0000)
                                        .setTooltip(Text.translatable("tooltip.soundvisualizer.color"))
                                        .setSaveConsumer(val -> config.indicatorColor = val)
                                        .build());

                        general.addEntry(
                                        entryBuilder.startFloatField(Text.translatable("option.soundvisualizer.size"),
                                                        config.indicatorSize)
                                                        .setDefaultValue(4.0f)
                                                        .setSaveConsumer(val -> config.indicatorSize = val)
                                                        .build());

                        general.addEntry(
                                        entryBuilder.startDoubleField(
                                                        Text.translatable("option.soundvisualizer.radius"),
                                                        config.radius)
                                                        .setDefaultValue(50.0)
                                                        .setSaveConsumer(val -> config.radius = val)
                                                        .build());

                        general.addEntry(entryBuilder
                                        .startStringDropdownMenu(Text.translatable("option.soundvisualizer.style"),
                                                        config.style)
                                        .setSelections(java.util.List.of("ARCH", "DOT", "ICON"))
                                        .setDefaultValue("ARCH")
                                        .setSaveConsumer(val -> config.style = val)
                                        .build());

                        general.addEntry(entryBuilder
                                        .startFloatField(Text.translatable("option.soundvisualizer.fadeTime"),
                                                        config.fadeTimeSeconds)
                                        .setDefaultValue(2.0f)
                                        .setSaveConsumer(val -> config.fadeTimeSeconds = val)
                                        .build());

                        general.addEntry(entryBuilder
                                        .startBooleanToggle(Text.translatable("option.soundvisualizer.distanceScaling"),
                                                        config.distanceScaling)
                                        .setDefaultValue(true)
                                        .setSaveConsumer(val -> config.distanceScaling = val)
                                        .build());

                        general.addEntry(entryBuilder
                                        .startBooleanToggle(Text.translatable("option.soundvisualizer.subtitleOnly"),
                                                        config.subtitleOnly)
                                        .setDefaultValue(true)
                                        .setSaveConsumer(val -> config.subtitleOnly = val)
                                        .build());

                        general.addEntry(entryBuilder
                                        .startBooleanToggle(Text.translatable("option.soundvisualizer.showIcons"),
                                                        config.showIcons)
                                        .setDefaultValue(true)
                                        .setSaveConsumer(val -> config.showIcons = val)
                                        .build());

                        builder.setSavingRunnable(config::save);
                        return builder.build();
                };
        }
}
