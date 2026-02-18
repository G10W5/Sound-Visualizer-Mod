package com.example.soundvisualizer;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SoundIndicatorRenderer implements HudRenderCallback {
    private static int soundCounter = 0;

    public static void addStaticHit(net.minecraft.util.Identifier id, Vec3d pos, net.minecraft.text.Text subtitle,
            float range,
            float volume) {
        SoundVisualizerClient.HITS.add(new SoundVisualizerHit(id, pos, subtitle, range, volume));
    }

    public void addHit(net.minecraft.util.Identifier id, Vec3d pos, net.minecraft.text.Text subtitle, float range,
            float volume) {
        addStaticHit(id, pos, subtitle, range, volume);
    }

    public void addHit(net.minecraft.util.Identifier id, Vec3d pos) {
        addHit(id, pos, null, -1.0f, 1.0f);
    }

    @Override
    public void onHudRender(DrawContext drawContext, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null)
            return;

        int width = drawContext.getScaledWindowWidth();
        int height = drawContext.getScaledWindowHeight();
        int centerX = width / 2;
        int centerY = height / 2;

        SoundVisualizerConfig config = SoundVisualizerConfig.INSTANCE;

        // Auto-cleanup expired sounds
        SoundVisualizerClient.HITS.removeIf(SoundVisualizerHit::isExpired);

        // Safety cap
        if (SoundVisualizerClient.HITS.size() > 100) {
            SoundVisualizerClient.HITS.clear();
        }

        for (SoundVisualizerHit hit : SoundVisualizerClient.HITS) {
            hit.update();

            // Calculate direction relative to player
            Vec3d playerPos = client.player.getPos();
            Vec3d toSound = hit.position.subtract(playerPos).normalize();

            // Get player looking direction (yaw)
            float yaw = client.player.getYaw();

            // Angle to sound in horizontal plane
            double angleToSound = MathHelper.atan2(toSound.z, toSound.x) * (180 / Math.PI) - 90;
            double relativeAngle = MathHelper.wrapDegrees(angleToSound - yaw);

            double radius = config.radius;
            double rad = Math.toRadians(relativeAngle);
            int x = (int) (centerX + Math.sin(rad) * radius);
            int y = (int) (centerY - Math.cos(rad) * radius);

            // Smarter Alpha: combine fade alpha with distance/volume alpha
            float distScale = 1.0f;
            if (config.distanceScaling) {
                double dist = client.player.getPos().distanceTo(hit.position);
                float range = hit.range > 0 ? hit.range : 48.0f;
                // Relaxed falloff: stay visible up to 1.5x range
                distScale = (float) MathHelper.clamp(1.0 - (dist / (range * 1.5)), 0.1, 1.0);
            }

            // Volume also adjusts base visibility
            float volumeScale = MathHelper.clamp(hit.volume * 1.5f, 0.3f, 1.0f);
            float finalAlpha = hit.alpha * distScale * volumeScale;

            if (finalAlpha < 0.001f)
                continue;

            if (soundCounter % 100 == 0) {
                System.out.println("[SoundVisualizer] RENDER HIT: x=" + x + ", y=" + y + ", alpha=" + finalAlpha
                        + ", style=" + config.style);
            }

            int color = ((int) (finalAlpha * 255) << 24) | (config.indicatorColor & 0xFFFFFF);
            // Darker, more visible shadow
            int shadowColor = ((int) (finalAlpha * 0.8f * 255) << 24) | 0x000000;
            float finalSize = config.indicatorSize * distScale;

            if ("ARCH".equals(config.style)) {
                // Draw shadow first (offset by 2 pixels for depth)
                drawArc(drawContext, x + 2, y + 2, (float) relativeAngle, shadowColor, finalSize);
                drawArc(drawContext, x, y, (float) relativeAngle, color, finalSize);
            } else {
                drawContext.fill((int) (x - finalSize / 2), (int) (y - finalSize / 2), (int) (x + finalSize / 2),
                        (int) (y + finalSize / 2), color);
            }

            // ALWAYS DRAW ICONS IF ENABLED (On top of chevron/dot)
            if (config.showIcons) {
                ItemStack stack = getIconForItem(hit.soundId);
                if (stack == null || stack.isEmpty())
                    stack = new ItemStack(Items.NOTE_BLOCK);

                if (stack != null && !stack.isEmpty()) {
                    drawContext.getMatrices().push();
                    com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                    com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
                    com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();

                    // Center icon at (x, y)
                    drawContext.getMatrices().translate(x, y, 0);

                    // Icons should be slightly smaller than the chevron overall but clear
                    float iconScale = (config.indicatorSize / 4.0f) * distScale * 1.5f;
                    drawContext.getMatrices().scale(iconScale, iconScale, 1.0f);

                    // Icon Shadow (Soft)
                    drawContext.getMatrices().push();
                    drawContext.getMatrices().translate(0.5, 0.5, 0);
                    com.mojang.blaze3d.systems.RenderSystem.setShaderColor(0.0f, 0.0f, 0.0f, finalAlpha * 0.5f);
                    drawContext.drawItem(stack, -8, -8);
                    drawContext.getMatrices().pop();

                    // Actual Icon with alpha
                    com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, finalAlpha);
                    drawContext.drawItem(stack, -8, -8);

                    com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                    com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
                    drawContext.getMatrices().pop();
                }
            }
        }
    }

    private void drawArc(DrawContext context, int x, int y, float angle, int color, float size) {
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(angle));

        // POINT AT SOUND: tip at (0,0), wings go TOWARDS center (Local Y-)
        // Make it MUCH thicker
        int thickness = Math.max(4, (int) (size / 2.0)); // Thicker
        float wingLength = size * 2.8f; // Longer wings

        // Left wing (points down and in)
        context.getMatrices().push();
        context.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(-145)); // Slightly
                                                                                                               // wider
                                                                                                               // angle
        context.fill(0, -thickness / 2, (int) wingLength, thickness / 2, color);
        context.getMatrices().pop();

        // Right wing (points down and in)
        context.getMatrices().push();
        context.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(145));
        context.fill((int) -wingLength, -thickness / 2, 0, thickness / 2, color);
        context.getMatrices().pop();

        context.getMatrices().pop();
    }

    private ItemStack getIconForItem(Identifier soundId) {
        String path = soundId.getPath();

        // Mobs
        if (path.contains("zombie"))
            return new ItemStack(Items.ZOMBIE_HEAD);
        if (path.contains("skeleton"))
            return new ItemStack(Items.SKELETON_SKULL);
        if (path.contains("creeper"))
            return new ItemStack(Items.CREEPER_HEAD);
        if (path.contains("enderman"))
            return new ItemStack(Items.ENDER_EYE);
        if (path.contains("spider"))
            return new ItemStack(Items.SPIDER_EYE);
        if (path.contains("ghast"))
            return new ItemStack(Items.GHAST_TEAR);
        if (path.contains("piglin"))
            return new ItemStack(Items.PIGLIN_HEAD);
        if (path.contains("blaze"))
            return new ItemStack(Items.BLAZE_ROD);
        if (path.contains("wither"))
            return new ItemStack(Items.WITHER_SKELETON_SKULL);
        if (path.contains("ender_dragon"))
            return new ItemStack(Items.DRAGON_HEAD);
        if (path.contains("villager"))
            return new ItemStack(Items.EMERALD);
        if (path.contains("iron_golem"))
            return new ItemStack(Items.IRON_BLOCK);
        if (path.contains("wolf") || path.contains("dog"))
            return new ItemStack(Items.BONE);
        if (path.contains("cat"))
            return new ItemStack(Items.STRING);
        if (path.contains("cow"))
            return new ItemStack(Items.COW_SPAWN_EGG);
        if (path.contains("sheep"))
            return new ItemStack(Items.SHEEP_SPAWN_EGG);
        if (path.contains("pig"))
            return new ItemStack(Items.PIG_SPAWN_EGG);
        if (path.contains("chicken"))
            return new ItemStack(Items.CHICKEN_SPAWN_EGG);
        if (path.contains("horse") || path.contains("donkey") || path.contains("mule") || path.contains("llama"))
            return new ItemStack(Items.SADDLE);
        if (path.contains("slime"))
            return new ItemStack(Items.SLIME_BALL);
        if (path.contains("phantom"))
            return new ItemStack(Items.PHANTOM_MEMBRANE);
        if (path.contains("shulker"))
            return new ItemStack(Items.SHULKER_SHELL);
        if (path.contains("warden"))
            return new ItemStack(Items.ECHO_SHARD);
        if (path.contains("breeze"))
            return new ItemStack(Items.BREEZE_ROD);
        if (path.contains("bogged"))
            return new ItemStack(Items.POISONOUS_POTATO);

        // Actions & Items
        if (path.contains("step") || path.contains("footstep"))
            return new ItemStack(Items.LEATHER_BOOTS);
        if (path.contains("chest") || path.contains("barrel") || path.contains("shulker_box"))
            return new ItemStack(Items.CHEST);
        if (path.contains("door") || path.contains("fence_gate") || path.contains("trapdoor"))
            return new ItemStack(Items.OAK_DOOR);

        if (path.contains("eat") || path.contains("consume") || path.contains("drink"))
            return new ItemStack(Items.COOKED_BEEF);
        if (path.contains("potion") || path.contains("splash"))
            return new ItemStack(Items.POTION);

        if (path.contains("bow") || path.contains("arrow"))
            return new ItemStack(Items.ARROW);
        if (path.contains("trident"))
            return new ItemStack(Items.TRIDENT);
        if (path.contains("throw") || path.contains("shoot"))
            return new ItemStack(Items.BOW);

        if (path.contains("fire") || path.contains("burn") || path.contains("lava") || path.contains("extinguish"))
            return new ItemStack(Items.FLINT_AND_STEEL);
        if (path.contains("water") || path.contains("swim") || path.contains("splash") || path.contains("bubble"))
            return new ItemStack(Items.WATER_BUCKET);
        if (path.contains("explosion") || path.contains("tnt") || path.contains("firework"))
            return new ItemStack(Items.TNT);
        if (path.contains("lightning") || path.contains("thunder"))
            return new ItemStack(Items.LIGHTNING_ROD);
        if (path.contains("totem"))
            return new ItemStack(Items.TOTEM_OF_UNDYING);
        if (path.contains("armor.equip"))
            return new ItemStack(Items.IRON_CHESTPLATE);
        if (path.contains("hit") || path.contains("attack") || path.contains("damage"))
            return new ItemStack(Items.IRON_SWORD);
        if (path.contains("break") || path.contains("mine"))
            return new ItemStack(Items.IRON_PICKAXE);
        if (path.contains("place") || path.contains("dig"))
            return new ItemStack(Items.GRASS_BLOCK);

        // Default
        return new ItemStack(Items.BELL);
    }
}
