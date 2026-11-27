package com.example.soundvisualizer;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Procedural HUD renderer. Arcs are built from tangent-aligned strips whose
 * texture only carries a radial alpha profile, so span, thickness and the
 * icon gap remain exact at any size.
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

        // Higher-priority categories draw last so they stay on top
        visible.sort(Comparator.comparingInt(h -> h.category.getPriority()));
        float cx = ctx.guiWidth() / 2.0f;
        float cy = ctx.guiHeight() / 2.0f;
        for (SoundVisualizerHit hit : visible) {
            drawIndicator(ctx, cx, cy, cfg, hit.category, hit.angle, hit.alpha,
                    hit.scale, hit.intensity, hit.ripple, hit.flash);
        }
    }

    /**
     * Renders a single indicator for the config screen's live preview.
     */
    public static void renderPreviewIndicator(GuiGraphicsExtractor ctx, float centerX, float centerY,
                                              SoundVisualizerConfig previewCfg, SoundCategory category,
                                              float angleDeg, float alpha, float scale,
                                              float ripple, float flash) {
        drawIndicator(ctx, centerX, centerY, previewCfg, category, angleDeg, alpha,
                scale, 1.0f, ripple, flash);
    }

    private static void drawIndicator(GuiGraphicsExtractor ctx, float centerX, float centerY,
                                      SoundVisualizerConfig cfg, SoundCategory category,
                                      float angleDeg, float hitAlpha, float scale,
                                      float intensity, float ripple, float flash) {
        float alpha = hitAlpha * cfg.opacity;
        if (alpha <= 0.004f) return;

        int rgb = cfg.getColor(category);
        if (flash > 0.01f) rgb = mix(rgb, 0xFFFFFF, flash * 0.5f);

        float radius = (float) cfg.radius;
        float emphasis = intensity * scale;
        float thickness = Math.max(1.0f, cfg.lineThickness * emphasis);
        float wingSpan = Mth.clamp(cfg.arcSpanDegrees * (0.85f + 0.15f * intensity), 8.0f, 180.0f);

        float iconSize = ICON_BASE * cfg.iconScale * scale;
        float gapDeg = cfg.showIcons
                ? (float) Math.toDegrees(Math.atan((iconSize * 0.5f + 3.0f) / Math.max(8.0f, radius)))
                : 0.0f;

        Matrix3x2fStack pose = ctx.pose();
        pose.pushMatrix();
        pose.translate(centerX, centerY);
        pose.rotate((float) Math.toRadians(angleDeg));

        boolean dashed = cfg.indicatorStyle == SoundVisualizerConfig.IndicatorStyle.DASHED;

        // 1. Expanding ripple echo on refresh
        if (cfg.showRipples && ripple < 1.0f) {
            float echoAlpha = alpha * (1.0f - ripple) * (1.0f - ripple) * 0.55f;
            drawArc(ctx, BAND_CORE, radius + ripple * 14.0f, wingSpan * (1.0f + 0.3f * ripple),
                    thickness * (1.0f - 0.35f * ripple), gapDeg, rgb, echoAlpha, false);
        }

        // 2. Soft bloom + sharp core arc wings
        if (cfg.glowIntensity > 0.01f) {
            drawArc(ctx, BAND_GLOW, radius, wingSpan * 1.1f, thickness * 4.0f, gapDeg * 0.8f, rgb,
                    alpha * Math.min(1.0f, 0.34f * cfg.glowIntensity), dashed);
        }
        drawArc(ctx, BAND_CORE, radius, wingSpan, thickness, gapDeg, rgb, alpha, dashed);

        // 3. Tactical outward chevron pointer
        if (cfg.indicatorStyle == SoundVisualizerConfig.IndicatorStyle.CHEVRON) {
            float chevronBaseR = radius + (cfg.showIcons ? iconSize * 0.5f : 0.0f) + 4.0f + thickness;
            drawChevron(ctx, chevronBaseR, thickness, rgb, alpha);
        }

        // 4. Upright category icon centered on the arc
        if (cfg.showIcons) {
            pose.pushMatrix();
            pose.translate(0.0f, -radius);
            pose.rotate((float) Math.toRadians(-angleDeg));

            if (cfg.iconBackdrop) {
                drawCentered(ctx, DISC, iconSize * 1.7f, argb(0x000000, alpha * 0.55f));
                drawCentered(ctx, DISC, iconSize * 1.3f, argb(rgb, alpha * 0.18f));
            }
            int iconRgb = cfg.tintIcons ? mix(rgb, 0xFFFFFF, 0.35f) : 0xFFFFFF;
            drawCentered(ctx, category.getIcon(), iconSize, argb(iconRgb, alpha));
            pose.popMatrix();
        }

        pose.popMatrix();
    }

    /**
     * Draws symmetric arc wings starting cleanly outside {@code gapDeg}.
     * {@code wingSpan} controls total visible wing sweep so the icon gap never
     * swallows the arc at small span settings.
     */
    private static void drawArc(GuiGraphicsExtractor ctx, Identifier tex, float radius, float wingSpan,
                                float thickness, float gapDeg, int rgb, float alpha, boolean dashed) {
        if (alpha <= 0.004f || thickness <= 0.05f) return;

        float wingHalf = Math.max(8.0f, wingSpan * 0.5f);
        float outerHalf = Math.min(165.0f, gapDeg + wingHalf);
        float totalSpan = outerHalf * 2.0f;
        int steps = Math.max(12, Mth.ceil(totalSpan / STEP_DEGREES));
        float step = totalSpan / steps;

        for (int i = 0; i < steps; i++) {
            float a0 = -outerHalf + i * step;
            float a1 = a0 + step;
            float mid = Math.abs((a0 + a1) * 0.5f);
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

            drawStrip(ctx, tex, polarX(radius, a0), polarY(radius, a0), polarX(radius, a1), polarY(radius, a1),
                    thickness * taper, argb(rgb, segAlpha));
        }
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
