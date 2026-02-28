package com.example.soundvisualizer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.client.DeltaTracker;

public class SoundIndicatorRenderer {
    public static final SoundIndicatorRenderer INSTANCE = new SoundIndicatorRenderer();

    public static void addStaticHit(Identifier id, Vec3 pos, Component subtitle, float range,
            float volume) {
        SoundVisualizerCommon.HITS.add(new SoundVisualizerHit(id, pos, subtitle, range, volume, SoundCategory.NEUTRAL));
    }

    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null)
            return;

        SoundVisualizerCommon.HITS.removeIf(SoundVisualizerHit::isExpired);
        if (SoundVisualizerCommon.HITS.size() > 100) {
            SoundVisualizerCommon.HITS.clear();
        }
        if (SoundVisualizerCommon.HITS.isEmpty())
            return;

        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();
        int centerX = width / 2;
        int centerY = height / 2;
        SoundVisualizerConfig config = SoundVisualizerConfig.INSTANCE;

        try {
            for (SoundVisualizerHit hit : SoundVisualizerCommon.HITS) {
                renderHit(guiGraphics, client, hit, centerX, centerY, config);
            }
        } catch (Throwable ignored) {
        }
    }

    private void renderHit(GuiGraphics ctx, Minecraft client, SoundVisualizerHit hit,
            int centerX, int centerY, SoundVisualizerConfig config) {
        hit.update();

        double px = client.player.getX();
        double py = client.player.getY();
        double pz = client.player.getZ();
        Vec3 toSound = hit.position.subtract(px, py, pz).normalize();
        float yaw = client.player.getYRot();

        double angleToSound = Mth.atan2(toSound.z, toSound.x) * (180.0 / Math.PI) - 90.0;
        double relativeAngle = Mth.wrapDegrees(angleToSound - yaw);

        double radius = config.radius;
        double rad = Math.toRadians(relativeAngle);
        int ix = (int) (centerX + Math.sin(rad) * radius);
        int iy = (int) (centerY - Math.cos(rad) * radius);

        float distScale = 1.0f;
        if (config.distanceScaling) {
            double dist = hit.position.distanceTo(new Vec3(px, py, pz));
            float range = hit.range > 0 ? hit.range : 48.0f;
            distScale = (float) Mth.clamp(1.0 - dist / (range * 1.5), 0.1, 1.0);
        }
        float volScale = Mth.clamp(hit.volume * 1.5f, 0.3f, 1.0f);
        float alpha = hit.alpha * distScale * volScale;
        if (alpha < 0.01f)
            return;

        // Dynamic pop-scale animation: starts big, bounces back using elastic ease-out
        float popScale = hitPopScale(hit);

        int a = (int) (alpha * 255) & 0xFF;
        int categoryColor = getCategoryColor(hit.category, config);
        int r = (categoryColor >> 16) & 0xFF;
        int g = (categoryColor >> 8) & 0xFF;
        int b = categoryColor & 0xFF;
        int color = (a << 24) | (r << 16) | (g << 8) | b;
        int shadow = ((int) (a * 0.5f) << 24);

        int sz = Math.max(2, (int) (config.indicatorSize * distScale * popScale));
        int strokeW = Math.max(1, (int) (config.indicatorWidth * distScale));

        if ("ARCH".equals(config.style)) {
            drawArc(ctx, centerX, centerY, (float) relativeAngle, (float) radius, sz, strokeW, shadow, 1);
            drawArc(ctx, centerX, centerY, (float) relativeAngle, (float) radius, sz, strokeW, color, 0);
        } else if ("DOT".equals(config.style)) {
            ctx.fill(ix - sz, iy - sz, ix + sz, iy + sz, shadow);
            ctx.fill(ix - sz + 1, iy - sz + 1, ix + sz - 1, iy + sz - 1, color);
        } else {
            drawChevronFill(ctx, ix, iy, (float) relativeAngle, color, shadow, sz, strokeW);
        }

        if (config.showIcons) {
            try {
                ItemStack stack = getIconForItem(hit.soundId);

                double iRad = Math.toRadians(relativeAngle);
                double iDist = config.radius + config.iconOffset;
                int iX = (int) (centerX + Math.sin(iRad) * iDist);
                int iY = (int) (centerY - Math.cos(iRad) * iDist);

                if (stack != null && !stack.isEmpty()) {
                    // renderItem renders a full 3D item model
                    ctx.renderItem(stack, iX - 8, iY - 8);
                } else {
                    String note = "\u266A";
                    ctx.drawString(client.font, note, iX - 3, iY - 4, color, true);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    /**
     * Returns a pop scale that starts large and snaps back elastically to 1.0
     * over the first 500ms of a hit's life.
     */
    private float hitPopScale(SoundVisualizerHit hit) {
        long elapsed = System.currentTimeMillis() - hit.startTime;
        float t = Math.min(1.0f, elapsed / 500.0f); // 0 -> 1 over 500ms
        if (t < 1.0f) {
            // Elastic ease-out
            double c4 = (2 * Math.PI) / 3;
            double bounce = Math.pow(2, -10 * t) * Math.sin((t * 10 - 0.75) * c4) + 1;
            return 1.0f + (float) (bounce * 0.5);
        }
        return 1.0f;
    }

    private void drawArc(GuiGraphics ctx, int cx, int cy, float angleDeg, float radius,
            int spanSize, int strokeW, int color, int offset) {
        float halfSpan = spanSize * 2.5f;
        float startA = angleDeg - halfSpan;
        float endA = angleDeg + halfSpan;
        int segments = 24;
        int t = Math.max(1, strokeW);

        for (int i = 0; i < segments; i++) {
            float f1 = (float) i / segments;
            float f2 = (float) (i + 1) / segments;
            double a1 = Math.toRadians(startA + (endA - startA) * f1);
            double a2 = Math.toRadians(startA + (endA - startA) * f2);

            int x1 = (int) (cx + Math.sin(a1) * (radius + offset));
            int y1 = (int) (cy - Math.cos(a1) * (radius + offset));
            int x2 = (int) (cx + Math.sin(a2) * (radius + offset));
            int y2 = (int) (cy - Math.cos(a2) * (radius + offset));

            drawLine(ctx, x1, y1, x2, y2, t, color);
        }
    }

    private void drawLine(GuiGraphics ctx, int x1, int y1, int x2, int y2, int thickness, int color) {
        if (thickness <= 1) {
            ctx.fill(x1, y1, x2 + 1, y2 + 1, color);
            return;
        }
        int dx = x2 - x1;
        int dy = y2 - y1;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < 0.1)
            return;

        double ux = dx / dist;
        double uy = dy / dist;
        double nx = -uy * (thickness / 2.0);
        double ny = ux * (thickness / 2.0);

        ctx.fill((int) (x1 + nx), (int) (y1 + ny), (int) (x2 + nx) + 1, (int) (y2 + ny) + 1, color);
    }

    private int getCategoryColor(SoundCategory category, SoundVisualizerConfig config) {
        return switch (category) {
            case HOSTILE -> config.colorHostile;
            case FRIENDLY -> config.colorFriendly;
            case AMBIENT -> config.colorAmbient;
            case BLOCKS -> config.colorBlocks;
            case PLAYER -> config.colorPlayer;
            default -> config.colorNeutral;
        };
    }

    private void drawChevronFill(GuiGraphics ctx, int ox, int oy, float angle, int color, int shadow, int sz,
            int strokeW) {
        float wingLen = sz * 3.0f;
        float left = (float) Math.toRadians(angle + 145);
        float right = (float) Math.toRadians(angle - 145);
        int t = Math.max(1, strokeW);

        drawThickLine(ctx, ox, oy, (int) (ox + Math.sin(left) * wingLen), (int) (oy - Math.cos(left) * wingLen), t,
                shadow, 1);
        drawThickLine(ctx, ox, oy, (int) (ox + Math.sin(left) * wingLen), (int) (oy - Math.cos(left) * wingLen), t,
                color, 0);
        drawThickLine(ctx, ox, oy, (int) (ox + Math.sin(right) * wingLen), (int) (oy - Math.cos(right) * wingLen), t,
                shadow, 1);
        drawThickLine(ctx, ox, oy, (int) (ox + Math.sin(right) * wingLen), (int) (oy - Math.cos(right) * wingLen), t,
                color, 0);
    }

    private void drawThickLine(GuiGraphics ctx, int x1, int y1, int x2, int y2, int t, int color, int offset) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (steps == 0)
            return;
        for (int i = 0; i <= steps; i++) {
            int px = x1 + (x2 - x1) * i / steps + offset;
            int py = y1 + (y2 - y1) * i / steps + offset;
            ctx.fill(px - t, py - t, px + t, py + t, color);
        }
    }

    private ItemStack getIconForItem(Identifier soundId) {
        String p = soundId.getPath();

        // --- Hostile mobs ---
        if (p.contains("zombie"))
            return new ItemStack(Items.ROTTEN_FLESH);
        if (p.contains("skeleton"))
            return new ItemStack(Items.BONE);
        if (p.contains("creeper"))
            return new ItemStack(Items.GUNPOWDER);
        if (p.contains("spider"))
            return new ItemStack(Items.SPIDER_EYE);
        if (p.contains("enderman"))
            return new ItemStack(Items.ENDER_PEARL);
        if (p.contains("warden"))
            return new ItemStack(Items.ECHO_SHARD);
        if (p.contains("blaze"))
            return new ItemStack(Items.BLAZE_ROD);
        if (p.contains("ghast"))
            return new ItemStack(Items.GHAST_TEAR);
        if (p.contains("witch"))
            return new ItemStack(Items.GLASS_BOTTLE);
        if (p.contains("slime"))
            return new ItemStack(Items.SLIME_BALL);
        if (p.contains("phantom"))
            return new ItemStack(Items.PHANTOM_MEMBRANE);
        if (p.contains("drowned") || p.contains("elder_guardian") || p.contains("guardian"))
            return new ItemStack(Items.COD);

        // --- Friendly mobs ---
        if (p.contains("villager"))
            return new ItemStack(Items.EMERALD);
        if (p.contains("pig"))
            return new ItemStack(Items.PORKCHOP);
        if (p.contains("cow"))
            return new ItemStack(Items.BEEF);
        if (p.contains("chicken"))
            return new ItemStack(Items.FEATHER);
        if (p.contains("sheep"))
            return new ItemStack(Items.WHITE_WOOL);
        if (p.contains("wolf"))
            return new ItemStack(Items.BONE);
        if (p.contains("cat") || p.contains("ocelot"))
            return new ItemStack(Items.COD);
        if (p.contains("horse") || p.contains("donkey") || p.contains("mule"))
            return new ItemStack(Items.SADDLE);
        if (p.contains("bee"))
            return new ItemStack(Items.HONEYCOMB);

        // --- Player footsteps ---
        if (p.contains("footstep") || p.contains(".step"))
            return new ItemStack(Items.LEATHER_BOOTS);

        // --- Other player sounds ---
        if (p.contains("entity.player"))
            return new ItemStack(Items.NOTE_BLOCK);

        // --- Block breaking/hitting (pickaxe) ---
        if (p.contains("block.") && (p.contains(".break") || p.contains(".hit")))
            return new ItemStack(Items.DIAMOND_PICKAXE);

        // --- Block placement ---
        if (p.contains("block.") && p.contains(".place"))
            return new ItemStack(Items.BRICKS);

        // --- Generic block sound ---
        if (p.contains("block."))
            return new ItemStack(Items.DIAMOND_PICKAXE);

        // --- Ambient / music ---
        if (p.contains("ambient.") || p.contains("music."))
            return new ItemStack(Items.OAK_LEAVES);

        return ItemStack.EMPTY;
    }
}
