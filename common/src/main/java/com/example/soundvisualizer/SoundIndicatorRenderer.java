package com.example.soundvisualizer;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Procedural HUD renderer. Arcs are built from tangent-aligned strips whose
 * texture only carries a radial alpha profile, so span, thickness, icon gaps,
 * and multi-color cluster gradients stay sharp at any size.
 */
public class SoundIndicatorRenderer {
    private static final Identifier BAND_CORE = id("textures/gui/band_core.png");
    private static final Identifier BAND_GLOW = id("textures/gui/band_glow.png");
    private static final Identifier DISC = id("textures/gui/disc.png");

    private static final float ICON_BASE = 16.0f;
    private static final float STEP_DEGREES = 1.0f;

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("soundvisualizer", path);
    }

    public static void render(GuiGraphicsExtractor ctx, DeltaTracker delta) {
        Minecraft client = Minecraft.getInstance();
        HudClock.tick(client.isPaused());
        if (client.player == null || client.level == null) return;

        SoundVisualizerConfig cfg = SoundVisualizerConfig.INSTANCE;
        if (!cfg.enabled) {
            SoundVisualizerCommon.HITS.clear();
            return;
        }

        float partialTick = delta.getGameTimeDeltaTicks();
        List<SoundVisualizerHit> visible = new ArrayList<>();
        for (SoundVisualizerHit hit : SoundVisualizerCommon.HITS) {
            hit.update(client, partialTick);
            if (hit.isExpired()) {
                SoundVisualizerCommon.HITS.remove(hit);
            } else if (!cfg.disabledCategories.contains(hit.category)) {
                visible.add(hit);
            }
        }
        if (visible.isEmpty() || client.gui.hud.isHidden()) return;

        List<SoundClusterer.Cluster> clusters = SoundClusterer.cluster(visible, cfg, true);
        float cx = ctx.guiWidth() / 2.0f;
        float cy = ctx.guiHeight() / 2.0f;
        for (SoundClusterer.Cluster cluster : clusters) {
            drawCluster(ctx, cx, cy, cfg, cluster);
        }
    }

    /**
     * Renders sample hits through the exact same clustering + gradient pipeline
     * for the config screen's live preview.
     */
    public static void renderPreviewClusters(GuiGraphicsExtractor ctx, float centerX, float centerY,
                                             SoundVisualizerConfig previewCfg, List<SoundVisualizerHit> sampleHits) {
        List<SoundClusterer.Cluster> clusters = SoundClusterer.cluster(sampleHits, previewCfg, false);
        for (SoundClusterer.Cluster cluster : clusters) {
            drawCluster(ctx, centerX, centerY, previewCfg, cluster);
        }
    }

    private static void drawCluster(GuiGraphicsExtractor ctx, float centerX, float centerY,
                                    SoundVisualizerConfig cfg, SoundClusterer.Cluster cluster) {
        float alpha = cluster.alpha * cfg.opacity;
        if (alpha <= 0.004f) return;

        int uniqueCats = cluster.orderedCategories.size();
        int[] arcColors = buildArcColors(cfg, cluster);

        float radius = (float) cfg.radius;
        float emphasis = cluster.intensity * cluster.scale;
        float thickness = Math.max(1.0f, cfg.lineThickness * emphasis);
        float wingBoost = 1.0f + Math.min(0.35f, 0.14f * Math.max(0, uniqueCats - 1));
        float wingSpan = Mth.clamp(cfg.arcSpanDegrees * (0.85f + 0.15f * cluster.intensity) * wingBoost, 8.0f, 180.0f);

        float iconSize = ICON_BASE * cfg.iconScale * cluster.scale;
        List<SoundCategory> shownIcons = selectShownIcons(cluster);
        float subSize = shownIcons.size() >= 2 ? iconSize * 0.82f : iconSize;
        float dualOffset = shownIcons.size() >= 2 ? subSize * 0.54f : 0.0f;

        float gapDeg = cfg.showIcons
                ? (float) Math.toDegrees(Math.atan((dualOffset + subSize * 0.5f + 2.8f) / Math.max(8.0f, radius)))
                : 0.0f;

        Matrix3x2fStack pose = ctx.pose();
        pose.pushMatrix();
        pose.translate(centerX, centerY);
        pose.rotate((float) Math.toRadians(cluster.angle));

        boolean dashed = cfg.indicatorStyle == SoundVisualizerConfig.IndicatorStyle.DASHED;

        // 1. Expanding ripple echo on refresh
        if (cfg.showRipples && cluster.ripple < 1.0f) {
            float echoAlpha = alpha * (1.0f - cluster.ripple) * (1.0f - cluster.ripple) * 0.55f;
            drawArc(ctx, BAND_CORE, radius + cluster.ripple * 14.0f,
                    wingSpan * (1.0f + 0.3f * cluster.ripple),
                    thickness * (1.0f - 0.35f * cluster.ripple), gapDeg, arcColors, echoAlpha, false);
        }

        // 2. Soft bloom + sharp core arc wings (supports multi-color gradient)
        if (cfg.glowIntensity > 0.01f) {
            drawArc(ctx, BAND_GLOW, radius, wingSpan * 1.1f, thickness * 4.0f, gapDeg * 0.8f, arcColors,
                    alpha * Math.min(1.0f, 0.34f * cfg.glowIntensity), dashed);
        }
        drawArc(ctx, BAND_CORE, radius, wingSpan, thickness, gapDeg, arcColors, alpha, dashed);

        // 3. Tactical outward chevron pointer
        if (cfg.indicatorStyle == SoundVisualizerConfig.IndicatorStyle.CHEVRON) {
            float chevronBaseR = radius + (cfg.showIcons ? subSize * 0.5f : 0.0f) + 4.0f + thickness;
            int centerRgb = sampleColor(arcColors, 0.5f);
            drawChevron(ctx, chevronBaseR, thickness, centerRgb, alpha);
        }

        // 4. Upright category icon(s) aligned with their respective color wings
        if (cfg.showIcons && !shownIcons.isEmpty()) {
            if (shownIcons.size() == 1) {
                drawCategoryIcon(ctx, cfg, shownIcons.get(0), 0.0f, -radius, cluster.angle, subSize, alpha);
            } else {
                drawCategoryIcon(ctx, cfg, shownIcons.get(0), -dualOffset, -radius, cluster.angle, subSize, alpha);
                drawCategoryIcon(ctx, cfg, shownIcons.get(1), dualOffset, -radius, cluster.angle, subSize, alpha);

                if (uniqueCats > 2) {
                    Font font = Minecraft.getInstance().font;
                    String badge = "+" + (uniqueCats - 2);
                    pose.pushMatrix();
                    pose.translate(0.0f, -radius);
                    pose.rotate((float) Math.toRadians(-cluster.angle));
                    pose.scale(0.65f, 0.65f);
                    ctx.centeredText(font, badge, 0, Math.round((subSize * 0.52f + 2.0f) / 0.65f),
                            argb(0xFFFFFF, alpha * 0.9f));
                    pose.popMatrix();
                }
            }
        }

        pose.popMatrix();
    }

    private static void drawCategoryIcon(GuiGraphicsExtractor ctx, SoundVisualizerConfig cfg, SoundCategory cat,
                                         float localX, float localY, float uprightCounterAngle,
                                         float size, float alpha) {
        int rgb = cfg.getColor(cat);
        Matrix3x2fStack pose = ctx.pose();
        pose.pushMatrix();
        pose.translate(localX, localY);
        pose.rotate((float) Math.toRadians(-uprightCounterAngle));

        if (cfg.iconBackdrop) {
            drawCentered(ctx, DISC, size * 1.65f, argb(0x000000, alpha * 0.55f));
            drawCentered(ctx, DISC, size * 1.25f, argb(rgb, alpha * 0.18f));
        }
        int iconRgb = cfg.tintIcons ? mix(rgb, 0xFFFFFF, 0.35f) : 0xFFFFFF;
        drawCentered(ctx, cat.getIcon(), size, argb(iconRgb, alpha));
        pose.popMatrix();
    }

    private static int[] buildArcColors(SoundVisualizerConfig cfg, SoundClusterer.Cluster cluster) {
        List<SoundCategory> cats = cluster.orderedCategories;
        if (cats.size() > 3) {
            // Keep the 3 highest-priority categories while preserving their left-to-right order
            List<SoundCategory> top3 = cluster.priorityCategories.subList(0, 3);
            List<SoundCategory> filtered = new ArrayList<>(3);
            for (SoundCategory c : cats) {
                if (top3.contains(c)) filtered.add(c);
            }
            cats = filtered;
        }
        int[] colors = new int[cats.size()];
        for (int i = 0; i < cats.size(); i++) {
            int c = cfg.getColor(cats.get(i));
            if (cluster.flash > 0.01f) c = mix(c, 0xFFFFFF, cluster.flash * 0.5f);
            colors[i] = c;
        }
        return colors;
    }

    private static List<SoundCategory> selectShownIcons(SoundClusterer.Cluster cluster) {
        List<SoundCategory> ordered = cluster.orderedCategories;
        if (ordered.size() <= 2) return ordered;
        SoundCategory p0 = cluster.priorityCategories.get(0);
        SoundCategory p1 = cluster.priorityCategories.get(1);
        return ordered.indexOf(p0) <= ordered.indexOf(p1) ? List.of(p0, p1) : List.of(p1, p0);
    }

    /**
     * Draws symmetric arc wings starting outside {@code gapDeg}, blending smoothly
     * across {@code colors} from left ({@code a < 0}) to right ({@code a > 0}).
     */
    private static void drawArc(GuiGraphicsExtractor ctx, Identifier tex, float radius, float wingSpan,
                                float thickness, float gapDeg, int[] colors, float alpha, boolean dashed) {
        if (alpha <= 0.004f || thickness <= 0.05f || colors.length == 0) return;

        float wingHalf = Math.max(8.0f, wingSpan * 0.5f);
        float outerHalf = Math.min(165.0f, gapDeg + wingHalf);
        float totalSpan = outerHalf * 2.0f;
        int steps = Math.max(12, Mth.ceil(totalSpan / STEP_DEGREES));
        float step = totalSpan / steps;

        for (int i = 0; i < steps; i++) {
            float a0 = -outerHalf + i * step;
            float a1 = a0 + step;
            float midSigned = (a0 + a1) * 0.5f;
            float mid = Math.abs(midSigned);
            if (mid < gapDeg) continue;

            float fromGap = mid - gapDeg;
            if (dashed) {
                float cycle = fromGap % 6.0f;
                if (cycle > 3.6f) continue;
            }

            float u = Mth.clamp(fromGap / wingHalf, 0.0f, 1.0f);
            float taper = 1.0f - 0.6f * (u * u);
            float tipFade = 1.0f - smoothstep(0.55f, 1.0f, u);
            float gapFade = gapDeg > 0.0f ? smoothstep(0.0f, 0.16f, u) : 1.0f;
            float segAlpha = alpha * tipFade * gapFade;
            if (segAlpha <= 0.004f) continue;

            float s = Mth.clamp((midSigned + outerHalf) / totalSpan, 0.0f, 1.0f);
            int rgb = sampleColor(colors, s);
            drawStrip(ctx, tex, polarX(radius, a0), polarY(radius, a0), polarX(radius, a1), polarY(radius, a1),
                    thickness * taper, argb(rgb, segAlpha));
        }
    }

    private static int sampleColor(int[] colors, float s) {
        if (colors.length == 1) return colors[0];
        if (colors.length == 2) {
            return mix(colors[0], colors[1], smoothstep(0.32f, 0.68f, s));
        }
        float scaled = Mth.clamp(s, 0.0f, 1.0f) * (colors.length - 1);
        int idx = Math.min((int) scaled, colors.length - 2);
        float local = smoothstep(0.15f, 0.85f, scaled - idx);
        return mix(colors[idx], colors[idx + 1], local);
    }

    private static void drawChevron(GuiGraphicsExtractor ctx, float r, float thickness, int rgb, float alpha) {
        float w = 4.5f + thickness;
        float h = 3.5f + thickness * 0.8f;
        int color = argb(rgb, alpha);
        float t = Math.max(1.2f, thickness * 0.85f);
        drawStrip(ctx, BAND_CORE, -w, -r, 0.0f, -r - h, t, color);
        drawStrip(ctx, BAND_CORE, 0.0f, -r - h, w, -r, t, color);
    }

    /** Straight strip from (x0,y0) to (x1,y1); texture V axis spans the thickness. */
    private static void drawStrip(GuiGraphicsExtractor ctx, Identifier tex, float x0, float y0, float x1, float y1,
                                  float thickness, int color) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.001f) return;
        Matrix3x2fStack pose = ctx.pose();
        pose.pushMatrix();
        pose.translate(x0, y0);
        pose.rotate((float) Math.atan2(dy, dx));
        pose.scale(len * 1.04f, thickness);
        pose.translate(-0.02f, -0.5f);
        ctx.blit(RenderPipelines.GUI_TEXTURED, tex, 0, 0, 0.0f, 0.0f, 1, 1, 1, 1, color);
        pose.popMatrix();
    }

    private static void drawCentered(GuiGraphicsExtractor ctx, Identifier tex, float size, int color) {
        Matrix3x2fStack pose = ctx.pose();
        pose.pushMatrix();
        pose.scale(size, size);
        pose.translate(-0.5f, -0.5f);
        ctx.blit(RenderPipelines.GUI_TEXTURED, tex, 0, 0, 0.0f, 0.0f, 1, 1, 1, 1, color);
        pose.popMatrix();
    }

    private static float polarX(float r, float deg) {
        return r * (float) Math.sin(Math.toRadians(deg));
    }

    private static float polarY(float r, float deg) {
        return -r * (float) Math.cos(Math.toRadians(deg));
    }

    private static float smoothstep(float e0, float e1, float x) {
        float t = Mth.clamp((x - e0) / (e1 - e0), 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    private static int argb(int rgb, float alpha) {
        int a = Mth.clamp((int) (alpha * 255.0f + 0.5f), 0, 255);
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    private static int mix(int a, int b, float t) {
        int r = (int) Mth.lerp(t, (a >> 16) & 0xFF, (b >> 16) & 0xFF);
        int g = (int) Mth.lerp(t, (a >> 8) & 0xFF, (b >> 8) & 0xFF);
        int bl = (int) Mth.lerp(t, a & 0xFF, b & 0xFF);
        return (r << 16) | (g << 8) | bl;
    }
}
