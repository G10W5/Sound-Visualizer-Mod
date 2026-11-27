package com.example.soundvisualizer;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.gui.entries.ColorEntry;
import me.shedaniel.clothconfig2.gui.entries.EnumListEntry;
import me.shedaniel.clothconfig2.gui.entries.IntegerSliderEntry;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SoundVisualizerConfigScreen {
    private static final String P = "option.soundvisualizer.";

    private static Component name(String key) {
        return Component.translatable(P + key);
    }

    private static Component tip(String key) {
        return Component.translatable(P + key + ".tooltip");
    }

    public static Screen create(Screen parent) {
        SoundVisualizerConfig cfg = SoundVisualizerConfig.INSTANCE;
        SoundVisualizerConfig.Preset[] pendingPreset = {SoundVisualizerConfig.Preset.CUSTOM};

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("title.soundvisualizer.config"));
        ConfigEntryBuilder eb = builder.entryBuilder();

        // ------------------------------------------------------------ Appearance
        ConfigCategory look = builder.getOrCreateCategory(Component.translatable("category.soundvisualizer.appearance"));

        BooleanListEntry enabledEntry = eb.startBooleanToggle(name("enabled"), cfg.enabled)
                .setDefaultValue(true).setTooltip(tip("enabled"))
                .setSaveConsumer(v -> cfg.enabled = v).build();

        EnumListEntry<SoundVisualizerConfig.Preset> presetEntry = eb.startEnumSelector(
                        name("preset"), SoundVisualizerConfig.Preset.class, SoundVisualizerConfig.Preset.CUSTOM)
                .setDefaultValue(SoundVisualizerConfig.Preset.CUSTOM).setTooltip(tip("preset"))
                .setEnumNameProvider(e -> Component.translatable("preset.soundvisualizer." + e.name().toLowerCase()))
                .setSaveConsumer(v -> pendingPreset[0] = v).build();

        EnumListEntry<SoundVisualizerConfig.IndicatorStyle> styleEntry = eb.startEnumSelector(
                        name("indicatorStyle"), SoundVisualizerConfig.IndicatorStyle.class, cfg.indicatorStyle)
                .setDefaultValue(SoundVisualizerConfig.IndicatorStyle.ARC).setTooltip(tip("indicatorStyle"))
                .setEnumNameProvider(e -> Component.translatable("style.soundvisualizer." + e.name().toLowerCase()))
                .setSaveConsumer(v -> cfg.indicatorStyle = v).build();

        IntegerSliderEntry radiusEntry = eb.startIntSlider(name("radius"), (int) cfg.radius, 24, 200)
                .setDefaultValue(50).setTooltip(tip("radius"))
                .setTextGetter(v -> Component.literal(v + " px"))
                .setSaveConsumer(v -> cfg.radius = v).build();

        IntegerSliderEntry thicknessEntry = eb.startIntSlider(name("lineThickness"), Math.round(cfg.lineThickness * 2), 2, 16)
                .setDefaultValue(5).setTooltip(tip("lineThickness"))
                .setTextGetter(v -> Component.literal((v / 2.0f) + " px"))
                .setSaveConsumer(v -> cfg.lineThickness = v / 2.0f).build();

        IntegerSliderEntry spanEntry = eb.startIntSlider(name("arcSpan"), (int) cfg.arcSpanDegrees, 10, 120)
                .setDefaultValue(28).setTooltip(tip("arcSpan"))
                .setTextGetter(v -> Component.literal(v + "°"))
                .setSaveConsumer(v -> cfg.arcSpanDegrees = v).build();

        IntegerSliderEntry glowEntry = eb.startIntSlider(name("glowIntensity"), Math.round(cfg.glowIntensity * 100), 0, 200)
                .setDefaultValue(35).setTooltip(tip("glowIntensity"))
                .setTextGetter(v -> Component.literal(v + "%"))
                .setSaveConsumer(v -> cfg.glowIntensity = v / 100.0f).build();

        IntegerSliderEntry opacityEntry = eb.startIntSlider(name("opacity"), Math.round(cfg.opacity * 100), 10, 100)
                .setDefaultValue(100).setTooltip(tip("opacity"))
                .setTextGetter(v -> Component.literal(v + "%"))
                .setSaveConsumer(v -> cfg.opacity = v / 100.0f).build();

        IntegerSliderEntry fadeEntry = eb.startIntSlider(name("fadeTime"), Math.round(cfg.fadeTimeSeconds * 10), 5, 80)
                .setDefaultValue(15).setTooltip(tip("fadeTime"))
                .setTextGetter(v -> Component.literal((v / 10.0f) + " s"))
                .setSaveConsumer(v -> cfg.fadeTimeSeconds = v / 10.0f).build();

        BooleanListEntry ripplesEntry = eb.startBooleanToggle(name("showRipples"), cfg.showRipples)
                .setDefaultValue(false).setTooltip(tip("showRipples"))
                .setSaveConsumer(v -> cfg.showRipples = v).build();

        BooleanListEntry showIconsEntry = eb.startBooleanToggle(name("showIcons"), cfg.showIcons)
                .setDefaultValue(true).setTooltip(tip("showIcons"))
                .setSaveConsumer(v -> cfg.showIcons = v).build();

        IntegerSliderEntry iconScaleEntry = eb.startIntSlider(name("iconScale"), Math.round(cfg.iconScale * 10), 5, 20)
                .setDefaultValue(9).setTooltip(tip("iconScale"))
                .setTextGetter(v -> Component.literal((v / 10.0f) + "x"))
                .setSaveConsumer(v -> cfg.iconScale = v / 10.0f).build();

        BooleanListEntry backdropEntry = eb.startBooleanToggle(name("iconBackdrop"), cfg.iconBackdrop)
                .setDefaultValue(false).setTooltip(tip("iconBackdrop"))
                .setSaveConsumer(v -> cfg.iconBackdrop = v).build();

        BooleanListEntry tintEntry = eb.startBooleanToggle(name("tintIcons"), cfg.tintIcons)
                .setDefaultValue(false).setTooltip(tip("tintIcons"))
                .setSaveConsumer(v -> cfg.tintIcons = v).build();

        // ------------------------------------------------------------ Categories (built early so preview tracks live colors)
        ConfigCategory cats = builder.getOrCreateCategory(Component.translatable("category.soundvisualizer.categories"));
        Map<SoundCategory, ColorEntry> colorEntries = new EnumMap<>(SoundCategory.class);
        for (SoundCategory cat : SoundCategory.values()) {
            cats.addEntry(eb.startBooleanToggle(name("enable" + cat.getName()), !cfg.disabledCategories.contains(cat))
                    .setDefaultValue(true)
                    .setSaveConsumer(v -> {
                        if (v) cfg.disabledCategories.remove(cat);
                        else cfg.disabledCategories.add(cat);
                    }).build());
            ColorEntry colorEntry = eb.startColorField(name("color" + cat.getName()), cfg.getColor(cat))
                    .setDefaultValue(cat.getDefaultColor())
                    .setSaveConsumer(v -> cfg.setColor(cat, v)).build();
            colorEntries.put(cat, colorEntry);
            cats.addEntry(colorEntry);
        }

        // Live preview sits at the top of Appearance
        HudPreviewEntry previewEntry = new HudPreviewEntry(
                cfg, enabledEntry, presetEntry, styleEntry, radiusEntry, thicknessEntry,
                spanEntry, glowEntry, opacityEntry, ripplesEntry, showIconsEntry,
                iconScaleEntry, backdropEntry, tintEntry, colorEntries);
        look.addEntry(previewEntry);
        look.addEntry(enabledEntry);
        look.addEntry(presetEntry);
        look.addEntry(styleEntry);
        look.addEntry(radiusEntry);
        look.addEntry(thicknessEntry);
        look.addEntry(spanEntry);
        look.addEntry(glowEntry);
        look.addEntry(opacityEntry);
        look.addEntry(fadeEntry);
        look.addEntry(ripplesEntry);
        look.addEntry(showIconsEntry);
        look.addEntry(iconScaleEntry);
        look.addEntry(backdropEntry);
        look.addEntry(tintEntry);

        // ------------------------------------------------------------ Detection
        ConfigCategory detect = builder.getOrCreateCategory(Component.translatable("category.soundvisualizer.detection"));

        detect.addEntry(eb.startIntSlider(name("maxHearingDistance"), (int) cfg.maxHearingDistance, 4, 64)
                .setDefaultValue(16).setTooltip(tip("maxHearingDistance"))
                .setTextGetter(v -> Component.literal(v + " blocks"))
                .setSaveConsumer(v -> cfg.maxHearingDistance = v).build());

        detect.addEntry(eb.startBooleanToggle(name("hideOwnSounds"), cfg.hideOwnSounds)
                .setDefaultValue(true).setTooltip(tip("hideOwnSounds"))
                .setSaveConsumer(v -> cfg.hideOwnSounds = v).build());

        detect.addEntry(eb.startIntSlider(name("mergeAngle"), (int) cfg.mergeAngle, 0, 60)
                .setDefaultValue(18).setTooltip(tip("mergeAngle"))
                .setTextGetter(v -> Component.literal(v + "°"))
                .setSaveConsumer(v -> cfg.mergeAngle = v).build());

        detect.addEntry(eb.startBooleanToggle(name("distanceScaling"), cfg.distanceScaling)
                .setDefaultValue(true).setTooltip(tip("distanceScaling"))
                .setSaveConsumer(v -> cfg.distanceScaling = v).build());

        detect.addEntry(eb.startBooleanToggle(name("smoothTracking"), cfg.smoothTracking)
                .setDefaultValue(true).setTooltip(tip("smoothTracking"))
                .setSaveConsumer(v -> cfg.smoothTracking = v).build());

        detect.addEntry(eb.startIntSlider(name("smoothingSpeed"), (int) cfg.smoothingSpeed, 4, 40)
                .setDefaultValue(14).setTooltip(tip("smoothingSpeed"))
                .setSaveConsumer(v -> cfg.smoothingSpeed = v).build());

        detect.addEntry(eb.startStrList(name("whitelist"), new ArrayList<>(cfg.whitelist))
                .setDefaultValue(new ArrayList<>()).setTooltip(tip("whitelist"))
                .setSaveConsumer(v -> cfg.whitelist = new ArrayList<>(v)).build());

        detect.addEntry(eb.startStrList(name("blacklist"), new ArrayList<>(cfg.blacklist))
                .setDefaultValue(new ArrayList<>(List.of("minecraft:weather.rain"))).setTooltip(tip("blacklist"))
                .setSaveConsumer(v -> cfg.blacklist = new ArrayList<>(v)).build());

        builder.setSavingRunnable(() -> {
            // Presets are applied last so they override the sliders above
            if (pendingPreset[0] != SoundVisualizerConfig.Preset.CUSTOM) {
                cfg.applyPreset(pendingPreset[0]);
            }
            if (!cfg.enabled) SoundVisualizerCommon.HITS.clear();
            cfg.save();
        });
        return builder.build();
    }

    /**
     * Interactive live HUD preview rendered inside the Cloth Config Appearance list.
     */
    private static final class HudPreviewEntry extends TooltipListEntry<Object> {
        private static final int BOX_HEIGHT = 132;
        private static final int PADDING_Y = 4;

        private final SoundVisualizerConfig baseCfg;
        private final BooleanListEntry enabledEntry;
        private final EnumListEntry<SoundVisualizerConfig.Preset> presetEntry;
        private final EnumListEntry<SoundVisualizerConfig.IndicatorStyle> styleEntry;
        private final IntegerSliderEntry radiusEntry;
        private final IntegerSliderEntry thicknessEntry;
        private final IntegerSliderEntry spanEntry;
        private final IntegerSliderEntry glowEntry;
        private final IntegerSliderEntry opacityEntry;
        private final BooleanListEntry ripplesEntry;
        private final BooleanListEntry showIconsEntry;
        private final IntegerSliderEntry iconScaleEntry;
        private final BooleanListEntry backdropEntry;
        private final BooleanListEntry tintEntry;
        private final Map<SoundCategory, ColorEntry> colorEntries;

        private long lastPulseMs = System.currentTimeMillis();
        private int lastBoxX, lastBoxY, lastBoxW, lastBoxH;

        HudPreviewEntry(
                SoundVisualizerConfig baseCfg,
                BooleanListEntry enabledEntry,
                EnumListEntry<SoundVisualizerConfig.Preset> presetEntry,
                EnumListEntry<SoundVisualizerConfig.IndicatorStyle> styleEntry,
                IntegerSliderEntry radiusEntry,
                IntegerSliderEntry thicknessEntry,
                IntegerSliderEntry spanEntry,
                IntegerSliderEntry glowEntry,
                IntegerSliderEntry opacityEntry,
                BooleanListEntry ripplesEntry,
                BooleanListEntry showIconsEntry,
                IntegerSliderEntry iconScaleEntry,
                BooleanListEntry backdropEntry,
                BooleanListEntry tintEntry,
                Map<SoundCategory, ColorEntry> colorEntries) {
            super(name("preview"), () -> Optional.empty());
            this.baseCfg = baseCfg;
            this.enabledEntry = enabledEntry;
            this.presetEntry = presetEntry;
            this.styleEntry = styleEntry;
            this.radiusEntry = radiusEntry;
            this.thicknessEntry = thicknessEntry;
            this.spanEntry = spanEntry;
            this.glowEntry = glowEntry;
            this.opacityEntry = opacityEntry;
            this.ripplesEntry = ripplesEntry;
            this.showIconsEntry = showIconsEntry;
            this.iconScaleEntry = iconScaleEntry;
            this.backdropEntry = backdropEntry;
            this.tintEntry = tintEntry;
            this.colorEntries = colorEntries;
        }

        private SoundVisualizerConfig snapshot() {
            SoundVisualizerConfig snap = baseCfg.copyVisual();
            snap.enabled = Boolean.TRUE.equals(enabledEntry.getValue());
            snap.opacity = opacityEntry.getValue() / 100.0f;
            snap.showIcons = Boolean.TRUE.equals(showIconsEntry.getValue());
            snap.tintIcons = Boolean.TRUE.equals(tintEntry.getValue());

            for (Map.Entry<SoundCategory, ColorEntry> e : colorEntries.entrySet()) {
                snap.setColor(e.getKey(), e.getValue().getValue());
            }

            SoundVisualizerConfig.Preset preset = presetEntry.getValue();
            if (preset != null && preset != SoundVisualizerConfig.Preset.CUSTOM) {
                snap.applyPreset(preset);
            } else {
                snap.indicatorStyle = styleEntry.getValue();
                snap.radius = radiusEntry.getValue();
                snap.lineThickness = thicknessEntry.getValue() / 2.0f;
                snap.arcSpanDegrees = spanEntry.getValue();
                snap.glowIntensity = glowEntry.getValue() / 100.0f;
                snap.showRipples = Boolean.TRUE.equals(ripplesEntry.getValue());
                snap.iconScale = iconScaleEntry.getValue() / 10.0f;
                snap.iconBackdrop = Boolean.TRUE.equals(backdropEntry.getValue());
            }
            return snap;
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor ctx, int index, int y, int x, int entryWidth,
                                       int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
            super.extractRenderState(ctx, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);

            int boxX = x + 4;
            int boxY = y + PADDING_Y;
            int boxW = entryWidth - 8;
            int boxH = BOX_HEIGHT;
            this.lastBoxX = boxX;
            this.lastBoxY = boxY;
            this.lastBoxW = boxW;
            this.lastBoxH = boxH;

            // Dark sky-to-horizon backdrop so both white/pastel and dark outlines are easy to judge
            ctx.fillGradient(boxX, boxY, boxX + boxW, boxY + boxH, 0xCC121722, 0xCC1E2634);
            ctx.outline(boxX, boxY, boxW, boxH, isHovered ? 0x99FFFFFF : 0x558899AA);

            ctx.enableScissor(boxX + 1, boxY + 1, boxX + boxW - 1, boxY + boxH - 1);

            Minecraft client = Minecraft.getInstance();
            ctx.text(client.font, getFieldName(), boxX + 8, boxY + 6, 0xFFD8DEE9, true);

            float cx = boxX + boxW / 2.0f;
            float cy = boxY + boxH / 2.0f + 4.0f;

            // Subtle crosshair
            ctx.fill((int) cx - 5, (int) cy, (int) cx + 6, (int) cy + 1, 0x99FFFFFF);
            ctx.fill((int) cx, (int) cy - 5, (int) cx + 1, (int) cy + 6, 0x99FFFFFF);

            SoundVisualizerConfig snap = snapshot();
            if (!snap.enabled) {
                Component off = Component.translatable("message.soundvisualizer.off");
                ctx.centeredText(client.font, off, (int) cx, (int) cy + 12, 0xFFFF5555);
                ctx.disableScissor();
                return;
            }

            // Periodic gentle pulse every 2.2s, or immediate on click
            long now = System.currentTimeMillis();
            if (now - lastPulseMs > 2200L) {
                lastPulseMs = now;
            }
            float sincePulse = now - lastPulseMs;
            float ripple = Mth.clamp(sincePulse / 520.0f, 0.0f, 1.0f);
            float flash = sincePulse < 260.0f ? (1.0f - sincePulse / 260.0f) * (1.0f - sincePulse / 260.0f) : 0.0f;
            float pop = sincePulse < 260.0f
                    ? 1.0f + (float) Math.sin((sincePulse / 260.0f) * Math.PI) * 0.14f
                    : 1.0f;

            // Scale down smoothly only when radius would exceed the preview frame
            float maxPreviewRadius = (boxH * 0.5f) - 18.0f;
            float needed = (float) snap.radius + 12.0f;
            float fitScale = needed > maxPreviewRadius ? maxPreviewRadius / needed : 1.0f;

            Matrix3x2fStack pose = ctx.pose();
            pose.pushMatrix();
            pose.translate(cx, cy);
            pose.scale(fitScale, fitScale);

            SoundIndicatorRenderer.renderPreviewIndicator(
                    ctx, 0.0f, 0.0f, snap, SoundCategory.HOSTILE, -48.0f, 1.0f, pop, ripple, flash);
            SoundIndicatorRenderer.renderPreviewIndicator(
                    ctx, 0.0f, 0.0f, snap, SoundCategory.FRIENDLY, 62.0f, 0.9f, 1.0f, 1.0f, 0.0f);
            SoundIndicatorRenderer.renderPreviewIndicator(
                    ctx, 0.0f, 0.0f, snap, SoundCategory.NEUTRAL, 158.0f, 0.72f, 1.0f, 1.0f, 0.0f);

            pose.popMatrix();
            ctx.disableScissor();
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (event.button() == 0
                    && event.x() >= lastBoxX && event.x() <= lastBoxX + lastBoxW
                    && event.y() >= lastBoxY && event.y() <= lastBoxY + lastBoxH) {
                lastPulseMs = System.currentTimeMillis();
                return true;
            }
            return super.mouseClicked(event, doubleClick);
        }

        @Override
        public int getItemHeight() {
            return BOX_HEIGHT + PADDING_Y * 2;
        }

        @Override
        public Object getValue() {
            return null;
        }

        @Override
        public Optional<Object> getDefaultValue() {
            return Optional.empty();
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return Collections.emptyList();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return Collections.emptyList();
        }
    }
}
