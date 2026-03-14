package com.example.soundvisualizer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix3x2f;

public class SoundIndicatorRenderer {
    private static final Identifier ARC_TEXTURE = Identifier.fromNamespaceAndPath("soundvisualizer", "textures/gui/arc.png");
    private static final Identifier HOSTILE_ICON = Identifier.fromNamespaceAndPath("soundvisualizer", "textures/gui/hostile.png");
    private static final Identifier FRIENDLY_ICON = Identifier.fromNamespaceAndPath("soundvisualizer", "textures/gui/friendly.png");
    private static final Identifier FOOTSTEPS_ICON = Identifier.fromNamespaceAndPath("soundvisualizer", "textures/gui/footsteps.png");
    private static final Identifier BLOCKS_ICON = Identifier.fromNamespaceAndPath("soundvisualizer", "textures/gui/block.png");
    private static final Identifier PLAYER_ICON = Identifier.fromNamespaceAndPath("soundvisualizer", "textures/gui/player.png");
    private static final Identifier AMBIENT_ICON = Identifier.fromNamespaceAndPath("soundvisualizer", "textures/gui/ambient.png");

    public static void render(GuiGraphics ctx, DeltaTracker delta) {
        float alpha = 1.0f; // Base alpha for the entire HUD
        for (SoundVisualizerHit hit : SoundVisualizerCommon.HITS) {
            hit.update();
            if (hit.isExpired()) {
                SoundVisualizerCommon.HITS.remove(hit);
                continue;
            }
            renderSoundIndicator(ctx, hit, alpha, delta);
        }
    }

    public static void renderSoundIndicator(GuiGraphics ctx, SoundVisualizerHit hit, float alpha, DeltaTracker delta) {
        if (alpha <= 0.05f) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        Vec3 pos = hit.position;
        double dx = pos.x - client.player.getX();
        double dz = pos.z - client.player.getZ();
        
        double angleToSound = Math.atan2(dz, dx) * (180.0 / Math.PI) - 90.0;
        float relativeAngle = (float) (angleToSound - client.player.getViewYRot(delta.getGameTimeDeltaTicks()));

        ctx.pose().pushMatrix();
        Matrix3x2f pose = ctx.pose(); // Get pose after push
        
        float centerX = ctx.guiWidth() / 2.0f;
        float centerY = ctx.guiHeight() / 2.0f;
        pose.translate(centerX, centerY);

        // Rotate towards sound (Matrix3x2f rotate takes angle in radians)
        pose.rotate((float) Math.toRadians(relativeAngle));

        float size = SoundVisualizerConfig.INSTANCE.arcThickness * hit.scale;
        float distance = (float) SoundVisualizerConfig.INSTANCE.radius;
        
        pose.translate(0, -distance);

        int catColor = hit.category.getDefaultColor();
        float r = ((catColor >> 16) & 0xFF) / 255.0f;
        float g = ((catColor >> 8) & 0xFF) / 255.0f;
        float b = (catColor & 0xFF) / 255.0f;
        float baseAlpha = alpha * hit.alpha;
        
        // 1. Draw Arc (No glow)
        drawArc(ctx, size, r, g, b, baseAlpha);

        // 2. Icon Rendering (Counter-rotate so it stays upright)
        if (SoundVisualizerConfig.INSTANCE.showIcons) {
            ctx.pose().pushMatrix();
            pose.rotate((float) Math.toRadians(-relativeAngle));
            pose.translate(0, -10);

            Identifier iconTex = getIconForCategory(hit.category);
            int iconAlphaInt = (int) (baseAlpha * 255);
            int iconColor = (iconAlphaInt << 24) | 0xFFFFFF;

            float iconScaleFactor = 0.8f * hit.scale * SoundVisualizerConfig.INSTANCE.iconScale;
            pose.scale(iconScaleFactor, iconScaleFactor);
            ctx.blit(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, iconTex, -8, -8, 0, 0, 16, 16, 16, 16, iconColor);

            ctx.pose().popMatrix();
        }
        
        ctx.pose().popMatrix();
    }

    private static Identifier getIconForCategory(SoundCategory category) {
        switch (category) {
            case HOSTILE: return HOSTILE_ICON;
            case FRIENDLY: return FRIENDLY_ICON;
            case NEUTRAL: return FOOTSTEPS_ICON;
            case BLOCKS: return BLOCKS_ICON;
            case PLAYER: return PLAYER_ICON;
            case AMBIENT: return AMBIENT_ICON;
            default: return FOOTSTEPS_ICON;
        }
    }

    private static void drawArc(GuiGraphics ctx, float size, float r, float g, float b, float alpha) {
        int halfSize = (int)(size / 2);
        int colorInt = ((int)(alpha * 255) << 24) | ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
        ctx.blit(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, ARC_TEXTURE, -halfSize, -halfSize, 0f, 0f, (int)size, (int)size, (int)size, (int)size, colorInt);
    }
}
