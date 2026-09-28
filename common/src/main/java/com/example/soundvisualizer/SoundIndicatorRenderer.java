package com.example.soundvisualizer;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2f;

public class SoundIndicatorRenderer {
    private static final Identifier ARC_TEXTURE = Identifier.fromNamespaceAndPath("soundvisualizer", "textures/gui/arc.png");
    private static final Identifier CHEVRON_TEXTURE = Identifier.fromNamespaceAndPath("soundvisualizer", "textures/gui/chevron.png");
    private static final Identifier RING_TEXTURE = Identifier.fromNamespaceAndPath("soundvisualizer", "textures/gui/ring.png");
    private static final Identifier SHOCKWAVE_TEXTURE = Identifier.fromNamespaceAndPath("soundvisualizer", "textures/gui/shockwave.png");

    private static final Identifier HOSTILE_ICON = Identifier.fromNamespaceAndPath("soundvisualizer", "textures/gui/hostile.png");
    private static final Identifier FRIENDLY_ICON = Identifier.fromNamespaceAndPath("soundvisualizer", "textures/gui/friendly.png");
    private static final Identifier FOOTSTEPS_ICON = Identifier.fromNamespaceAndPath("soundvisualizer", "textures/gui/footsteps.png");
    private static final Identifier BLOCKS_ICON = Identifier.fromNamespaceAndPath("soundvisualizer", "textures/gui/block.png");
    private static final Identifier PLAYER_ICON = Identifier.fromNamespaceAndPath("soundvisualizer", "textures/gui/player.png");
    private static final Identifier AMBIENT_ICON = Identifier.fromNamespaceAndPath("soundvisualizer", "textures/gui/ambient.png");

    public static void render(GuiGraphicsExtractor ctx, DeltaTracker delta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        float partialTick = delta.getGameTimeDeltaTicks();

        for (SoundVisualizerHit hit : SoundVisualizerCommon.HITS) {
            hit.update(client, partialTick);
            if (hit.isExpired()) {
                SoundVisualizerCommon.HITS.remove(hit);
                continue;
            }
            if (SoundVisualizerConfig.INSTANCE.disabledCategories.contains(hit.category)) {
                continue;
            }
            renderSoundIndicator(ctx, hit, delta);
        }
    }

    public static void renderSoundIndicator(GuiGraphicsExtractor ctx, SoundVisualizerHit hit, DeltaTracker delta) {
        if (hit.alpha <= 0.005f) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        SoundVisualizerConfig config = SoundVisualizerConfig.INSTANCE;
        float baseAlpha = hit.alpha * config.opacity;
        if (baseAlpha <= 0.005f) return;

        float relativeAngle = hit.smoothedRelativeAngle;

        ctx.pose().pushMatrix();
        Matrix3x2f pose = ctx.pose();

        float centerX = ctx.guiWidth() / 2.0f;
        float centerY = ctx.guiHeight() / 2.0f;
        pose.translate(centerX, centerY);
        pose.rotate((float) Math.toRadians(relativeAngle));

        float baseDistance = (float) config.radius;
        float size = config.arcThickness * hit.scale * hit.distanceFactor;

        // Color calculations
        int catColor = getCategoryColor(config, hit.category);
        float r = ((catColor >> 16) & 0xFF) / 255.0f;
        float g = ((catColor >> 8) & 0xFF) / 255.0f;
        float b = (catColor & 0xFF) / 255.0f;

        // Merge flash: pulse towards bright white
        if (hit.mergeFlash > 0.01f) {
            r = Mth.lerp(hit.mergeFlash * 0.45f, r, 1.0f);
            g = Mth.lerp(hit.mergeFlash * 0.45f, g, 1.0f);
            b = Mth.lerp(hit.mergeFlash * 0.45f, b, 1.0f);
        }

        Identifier indicatorTex = getTextureForStyle(config.indicatorStyle);

        // 1. Layer 1: Expanding Acoustic Shockwave Ripple
        if (config.showRipples && hit.rippleProgress < 0.95f) {
            float rippleDist = baseDistance + (hit.rippleProgress * 12.0f);
            float rippleAlpha = baseAlpha * (1.0f - hit.rippleProgress) * 0.6f;
            float rippleSize = size * (1.0f + hit.rippleProgress * 0.2f);
            drawTextureAt(ctx, SHOCKWAVE_TEXTURE, 0, -rippleDist, rippleSize, r, g, b, rippleAlpha);
        }

        // 2. Layer 2: Ambient Bloom / Glow Halo
        if (config.glowIntensity > 0.05f) {
            float glowAlpha = baseAlpha * 0.4f * config.glowIntensity;
            float glowSize = size * 1.25f;
            drawTextureAt(ctx, indicatorTex, 0, -baseDistance, glowSize, r, g, b, glowAlpha);
        }

        // 3. Layer 3: Sharp Core Indicator
        drawTextureAt(ctx, indicatorTex, 0, -baseDistance, size, r, g, b, baseAlpha);

        // 4. Layer 4: Category Icon (Centered in the cradle on the line!)
        if (config.showIcons) {
            ctx.pose().pushMatrix();
            pose.translate(0, -baseDistance); // Right on the arc line!
            pose.rotate((float) Math.toRadians(-relativeAngle)); // Keep icon upright

            Identifier iconTex = getIconForCategory(hit.category);
            float iconScale = 0.85f * hit.scale * config.iconScale;

            int aInt = Math.max(0, Math.min(255, (int) (baseAlpha * 255.0f)));
            int rInt = Math.max(0, Math.min(255, (int) (r * baseAlpha * 255.0f)));
            int gInt = Math.max(0, Math.min(255, (int) (g * baseAlpha * 255.0f)));
            int bInt = Math.max(0, Math.min(255, (int) (b * baseAlpha * 255.0f)));
            int iconColor = (aInt << 24) | (rInt << 16) | (gInt << 8) | bInt;

            pose.scale(iconScale, iconScale);
            ctx.blit(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, iconTex, -8, -8, 0, 0, 16, 16, 16, 16, iconColor);

            ctx.pose().popMatrix();
        }

        ctx.pose().popMatrix();
    }

    private static Identifier getTextureForStyle(SoundVisualizerConfig.IndicatorStyle style) {
        return switch (style) {
            case CHEVRON -> CHEVRON_TEXTURE;
            case RING -> RING_TEXTURE;
            case ARC -> ARC_TEXTURE;
        };
    }

    private static Identifier getIconForCategory(SoundCategory category) {
        return switch (category) {
            case HOSTILE -> HOSTILE_ICON;
            case FRIENDLY -> FRIENDLY_ICON;
            case NEUTRAL -> FOOTSTEPS_ICON;
            case BLOCKS -> BLOCKS_ICON;
            case PLAYER -> PLAYER_ICON;
            case AMBIENT -> AMBIENT_ICON;
        };
    }

    private static int getCategoryColor(SoundVisualizerConfig config, SoundCategory category) {
        return switch (category) {
            case HOSTILE -> config.colorHostile;
            case FRIENDLY -> config.colorFriendly;
            case NEUTRAL -> config.colorNeutral;
            case BLOCKS -> config.colorBlocks;
            case PLAYER -> config.colorPlayer;
            case AMBIENT -> config.colorAmbient;
        };
    }

    private static void drawTextureAt(GuiGraphicsExtractor ctx, Identifier texture, float x, float y, float size,
                                      float r, float g, float b, float alpha) {
        if (alpha <= 0.005f) return;
        int halfSize = (int) (size / 2);
        int aInt = Math.max(0, Math.min(255, (int) (alpha * 255.0f)));
        int rInt = Math.max(0, Math.min(255, (int) (r * alpha * 255.0f)));
        int gInt = Math.max(0, Math.min(255, (int) (g * alpha * 255.0f)));
        int bInt = Math.max(0, Math.min(255, (int) (b * alpha * 255.0f)));
        int colorInt = (aInt << 24) | (rInt << 16) | (gInt << 8) | bInt;
        ctx.blit(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, texture, (int) x - halfSize, (int) y - halfSize, 0f, 0f, (int) size, (int) size, (int) size, (int) size, colorInt);
    }
}
