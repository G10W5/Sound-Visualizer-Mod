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

/**
 * Fully state-safe renderer. Uses ONLY DrawContext.fill() for primitives.
 * Zero calls to getMatrices() so it works on both 1.21.1 and 1.21.11+.
 */
public class SoundIndicatorRenderer implements HudRenderCallback {

    public static void addStaticHit(Identifier id, Vec3d pos, net.minecraft.text.Text subtitle, float range,
            float volume) {
        // Fallback for manual/static hits
        SoundVisualizerClient.HITS.add(new SoundVisualizerHit(id, pos, subtitle, range, volume, SoundCategory.NEUTRAL));
    }

    @Override
    public void onHudRender(DrawContext drawContext, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null)
            return;

        SoundVisualizerClient.HITS.removeIf(SoundVisualizerHit::isExpired);
        if (SoundVisualizerClient.HITS.size() > 100) {
            SoundVisualizerClient.HITS.clear();
        }
        if (SoundVisualizerClient.HITS.isEmpty())
            return;

        int width = drawContext.getScaledWindowWidth();
        int height = drawContext.getScaledWindowHeight();
        int centerX = width / 2;
        int centerY = height / 2;
        SoundVisualizerConfig config = SoundVisualizerConfig.INSTANCE;

        try {
            for (SoundVisualizerHit hit : SoundVisualizerClient.HITS) {
                renderHit(drawContext, client, hit, centerX, centerY, config);
            }
        } catch (Throwable ignored) {
            // Silent fail — never crash the game
        }
    }

    private void renderHit(DrawContext ctx, MinecraftClient client, SoundVisualizerHit hit,
            int centerX, int centerY, SoundVisualizerConfig config) {
        hit.update();

        double px = client.player.getX();
        double py = client.player.getY();
        double pz = client.player.getZ();
        Vec3d toSound = hit.position.subtract(px, py, pz).normalize();
        float yaw = client.player.getYaw();

        double angleToSound = MathHelper.atan2(toSound.z, toSound.x) * (180.0 / Math.PI) - 90.0;
        double relativeAngle = MathHelper.wrapDegrees(angleToSound - yaw);

        double radius = config.radius;
        double rad = Math.toRadians(relativeAngle);
        int ix = (int) (centerX + Math.sin(rad) * radius);
        int iy = (int) (centerY - Math.cos(rad) * radius);

        float distScale = 1.0f;
        if (config.distanceScaling) {
            double dist = hit.position.distanceTo(new Vec3d(px, py, pz));
            float range = hit.range > 0 ? hit.range : 48.0f;
            distScale = (float) MathHelper.clamp(1.0 - dist / (range * 1.5), 0.1, 1.0);
        }
        float volScale = MathHelper.clamp(hit.volume * 1.5f, 0.3f, 1.0f);
        float alpha = hit.alpha * distScale * volScale;
        if (alpha < 0.01f)
            return;

        int a = (int) (alpha * 255) & 0xFF;
        int categoryColor = getCategoryColor(hit.category, config);
        int r = (categoryColor >> 16) & 0xFF;
        int g = (categoryColor >> 8) & 0xFF;
        int b = categoryColor & 0xFF;
        int color = (a << 24) | (r << 16) | (g << 8) | b;
        int shadow = ((int) (a * 0.5f) << 24); // transparent black shadow

        int sz = Math.max(2, (int) (config.indicatorSize * distScale));
        int strokeW = Math.max(1, (int) (config.indicatorWidth * distScale));

        if ("ARCH".equals(config.style)) {
            drawArc(ctx, centerX, centerY, (float) relativeAngle, (float) radius, sz, strokeW, shadow, 1);
            drawArc(ctx, centerX, centerY, (float) relativeAngle, (float) radius, sz, strokeW, color, 0);
        } else if ("DOT".equals(config.style)) {
            ctx.fill(ix - sz, iy - sz, ix + sz, iy + sz, shadow);
            ctx.fill(ix - sz + 1, iy - sz + 1, ix + sz - 1, iy + sz - 1, color);
        } else {
            // CHEVRON — hand-drawn with trig, no matrix transforms
            drawChevronFill(ctx, ix, iy, (float) relativeAngle, color, shadow, sz, strokeW);
        }

        // ICON — rendered radially offset from the indicator position
        if (config.showIcons) {
            try {
                ItemStack stack = getIconForItem(hit.soundId);

                double iRad = Math.toRadians(relativeAngle);
                double iDist = config.radius + config.iconOffset;
                int iX = (int) (centerX + Math.sin(iRad) * iDist);
                int iY = (int) (centerY - Math.cos(iRad) * iDist);

                if (stack != null && !stack.isEmpty()) {
                    // Known sound — render item icon at native 16x16
                    ctx.drawItem(stack, iX - 8, iY - 8);
                } else {
                    // Unknown/generic sound — draw a ♪ note glyph instead
                    String note = "\u266A"; // ♪
                    ctx.drawText(MinecraftClient.getInstance().textRenderer,
                            net.minecraft.text.Text.literal(note).styled(s -> s.withColor(color)),
                            iX - 3, iY - 4, color, true);
                }
            } catch (Throwable ignored) {
                // Safe no-op on 1.21.11
            }
        }
    }

    /**
     * Draw an arc as small filled circles along a circular path. No matrix stack
     * needed.
     */
    private void drawArc(DrawContext ctx, int cx, int cy, float angleDeg, float radius,
            int spanSize, int strokeW, int color, int offset) {
        float halfSpan = spanSize * 2.5f;
        float startA = angleDeg - halfSpan;
        float endA = angleDeg + halfSpan;
        int segments = 24; // Increased for smoothness
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

            // Use thin lines/rects to build the arc
            drawLine(ctx, x1, y1, x2, y2, t, color);
        }
    }

    private void drawLine(DrawContext ctx, int x1, int y1, int x2, int y2, int thickness, int color) {
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

        // Simple approximation with 4 fills for better coverage
        ctx.fill((int) (x1 + nx), (int) (y1 + ny), (int) (x2 + nx) + 1, (int) (y2 + ny) + 1, color);
    }

    private int getCategoryColor(SoundCategory category, SoundVisualizerConfig config) {
        switch (category) {
            case HOSTILE:
                return config.colorHostile;
            case FRIENDLY:
                return config.colorFriendly;
            case AMBIENT:
                return config.colorAmbient;
            case BLOCKS:
                return config.colorBlocks;
            case PLAYER:
                return config.colorPlayer;
            case NEUTRAL:
            default:
                return config.colorNeutral;
        }
    }

    /**
     * Draw a chevron (two lines at an angle) using pure trig, no matrix transforms.
     */
    private void drawChevronFill(DrawContext ctx, int ox, int oy, float angle, int color, int shadow, int sz,
            int strokeW) {
        float wingLen = sz * 3.0f;
        float left = (float) Math.toRadians(angle + 145);
        float right = (float) Math.toRadians(angle - 145);
        int t = Math.max(1, strokeW);

        // Left wing
        drawThickLine(ctx, ox, oy, (int) (ox + Math.sin(left) * wingLen), (int) (oy - Math.cos(left) * wingLen), t,
                shadow, 1);
        drawThickLine(ctx, ox, oy, (int) (ox + Math.sin(left) * wingLen), (int) (oy - Math.cos(left) * wingLen), t,
                color, 0);
        // Right wing
        drawThickLine(ctx, ox, oy, (int) (ox + Math.sin(right) * wingLen), (int) (oy - Math.cos(right) * wingLen), t,
                shadow, 1);
        drawThickLine(ctx, ox, oy, (int) (ox + Math.sin(right) * wingLen), (int) (oy - Math.cos(right) * wingLen), t,
                color, 0);
    }

    /**
     * Draw a thick line between two points by filling small squares along the path.
     */
    private void drawThickLine(DrawContext ctx, int x1, int y1, int x2, int y2, int t, int color, int offset) {
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

        // === Hostile Mobs ===
        if (p.contains("zombie") && !p.contains("pigman") && !p.contains("villager"))
            return new ItemStack(Items.ZOMBIE_HEAD);
        if (p.contains("skeleton") && !p.contains("wither"))
            return new ItemStack(Items.SKELETON_SKULL);
        if (p.contains("wither_skeleton"))
            return new ItemStack(Items.WITHER_SKELETON_SKULL);
        if (p.contains("wither"))
            return new ItemStack(Items.NETHER_STAR);
        if (p.contains("creeper"))
            return new ItemStack(Items.CREEPER_HEAD);
        if (p.contains("enderman") || p.contains("endermite"))
            return new ItemStack(Items.ENDER_EYE);
        if (p.contains("spider") || p.contains("cave_spider"))
            return new ItemStack(Items.SPIDER_EYE);
        if (p.contains("ghast"))
            return new ItemStack(Items.GHAST_TEAR);
        if (p.contains("blaze"))
            return new ItemStack(Items.BLAZE_ROD);
        if (p.contains("magma_cube"))
            return new ItemStack(Items.MAGMA_CREAM);
        if (p.contains("slime"))
            return new ItemStack(Items.SLIME_BALL);
        if (p.contains("silverfish"))
            return new ItemStack(Items.STONE_BRICKS);
        if (p.contains("drowned"))
            return new ItemStack(Items.PRISMARINE_SHARD);
        if (p.contains("husk"))
            return new ItemStack(Items.SAND);
        if (p.contains("stray"))
            return new ItemStack(Items.BONE);
        if (p.contains("phantom"))
            return new ItemStack(Items.PHANTOM_MEMBRANE);
        if (p.contains("shulker"))
            return new ItemStack(Items.SHULKER_SHELL);
        if (p.contains("guardian"))
            return new ItemStack(Items.PRISMARINE_CRYSTALS);
        if (p.contains("vex"))
            return new ItemStack(Items.IRON_SWORD);
        if (p.contains("vindicator") || p.contains("evoker") || p.contains("pillager"))
            return new ItemStack(Items.CROSSBOW);
        if (p.contains("ravager"))
            return new ItemStack(Items.SADDLE);
        if (p.contains("witch"))
            return new ItemStack(Items.GLASS_BOTTLE);
        if (p.contains("warden"))
            return new ItemStack(Items.ECHO_SHARD);
        if (p.contains("piglin") && !p.contains("zombified"))
            return new ItemStack(Items.PIGLIN_HEAD);
        if (p.contains("zombified_piglin") || p.contains("zombie_pigman"))
            return new ItemStack(Items.GOLD_NUGGET);
        if (p.contains("hoglin") || p.contains("zoglin"))
            return new ItemStack(Items.PORKCHOP);
        if (p.contains("piglin_brute"))
            return new ItemStack(Items.GOLDEN_AXE);

        // === Passive / Neutral Mobs ===
        if (p.contains("dragon"))
            return new ItemStack(Items.DRAGON_HEAD);
        if (p.contains("villager"))
            return new ItemStack(Items.EMERALD);
        if (p.contains("iron_golem"))
            return new ItemStack(Items.IRON_BLOCK);
        if (p.contains("snow_golem"))
            return new ItemStack(Items.SNOWBALL);
        if (p.contains("wolf") || p.contains("dog"))
            return new ItemStack(Items.BONE);
        if (p.contains("cat") || p.contains("ocelot"))
            return new ItemStack(Items.STRING);
        if (p.contains("horse") || p.contains("donkey") || p.contains("mule"))
            return new ItemStack(Items.SADDLE);
        if (p.contains("sheep"))
            return new ItemStack(Items.WHITE_WOOL);
        if (p.contains("cow") || p.contains("mooshroom"))
            return new ItemStack(Items.BEEF);
        if (p.contains("pig"))
            return new ItemStack(Items.PORKCHOP);
        if (p.contains("chicken"))
            return new ItemStack(Items.FEATHER);
        if (p.contains("rabbit"))
            return new ItemStack(Items.RABBIT_FOOT);
        if (p.contains("fox"))
            return new ItemStack(Items.SWEET_BERRIES);
        if (p.contains("bee"))
            return new ItemStack(Items.HONEYCOMB);
        if (p.contains("bat"))
            return new ItemStack(Items.LEATHER);
        if (p.contains("squid") || p.contains("glow_squid"))
            return new ItemStack(Items.INK_SAC);
        if (p.contains("cod") || p.contains("salmon") || p.contains("pufferfish"))
            return new ItemStack(Items.COD);
        if (p.contains("turtle"))
            return new ItemStack(Items.TURTLE_EGG);
        if (p.contains("panda"))
            return new ItemStack(Items.BAMBOO);
        if (p.contains("polar_bear"))
            return new ItemStack(Items.SNOWBALL);
        if (p.contains("llama") || p.contains("trader_llama"))
            return new ItemStack(Items.LEAD);
        if (p.contains("parrot"))
            return new ItemStack(Items.FEATHER);
        if (p.contains("dolphin"))
            return new ItemStack(Items.COD);
        if (p.contains("axolotl"))
            return new ItemStack(Items.WATER_BUCKET);
        if (p.contains("goat"))
            return new ItemStack(Items.GOAT_HORN);
        if (p.contains("frog"))
            return new ItemStack(Items.SLIME_BALL);
        if (p.contains("camel"))
            return new ItemStack(Items.YELLOW_TERRACOTTA);
        if (p.contains("sniffer"))
            return new ItemStack(Items.PITCHER_POD);
        if (p.contains("armadillo"))
            return new ItemStack(Items.ARMADILLO_SCUTE);
        if (p.contains("breeze"))
            return new ItemStack(Items.BREEZE_ROD);
        if (p.contains("bogged"))
            return new ItemStack(Items.MUSHROOM_STEW);

        // === Player Actions ===
        if (p.contains("eat") || p.contains("drink"))
            return new ItemStack(Items.COOKED_BEEF);
        if (p.contains("hurt") || p.contains("death"))
            return new ItemStack(Items.TOTEM_OF_UNDYING);
        if (p.contains("attack") || p.contains("sweep"))
            return new ItemStack(Items.IRON_SWORD);
        if (p.contains("arrow") || p.contains("bow"))
            return new ItemStack(Items.BOW);
        if (p.contains("crossbow"))
            return new ItemStack(Items.CROSSBOW);
        if (p.contains("trident"))
            return new ItemStack(Items.TRIDENT);
        if (p.contains("shield"))
            return new ItemStack(Items.SHIELD);
        if (p.contains("totem"))
            return new ItemStack(Items.TOTEM_OF_UNDYING);
        if (p.contains("step") || p.contains("footstep"))
            return new ItemStack(Items.LEATHER_BOOTS);

        // === Environment / Blocks ===
        if (p.contains("explosion") || p.contains("tnt"))
            return new ItemStack(Items.TNT);
        if (p.contains("fire") || p.contains("flint"))
            return new ItemStack(Items.FLINT_AND_STEEL);
        if (p.contains("lava"))
            return new ItemStack(Items.LAVA_BUCKET);
        if (p.contains("water") || p.contains("swim"))
            return new ItemStack(Items.WATER_BUCKET);
        if (p.contains("lightning"))
            return new ItemStack(Items.LIGHTNING_ROD);
        if (p.contains("chest") || p.contains("barrel"))
            return new ItemStack(Items.CHEST);
        if (p.contains("door"))
            return new ItemStack(Items.OAK_DOOR);
        if (p.contains("trapdoor"))
            return new ItemStack(Items.OAK_TRAPDOOR);
        if (p.contains("gate") || p.contains("fence_gate"))
            return new ItemStack(Items.OAK_FENCE_GATE);
        if (p.contains("break"))
            return new ItemStack(Items.IRON_PICKAXE);
        if (p.contains("bell"))
            return new ItemStack(Items.BELL);
        if (p.contains("anvil"))
            return new ItemStack(Items.ANVIL);
        if (p.contains("enchant"))
            return new ItemStack(Items.ENCHANTING_TABLE);
        if (p.contains("brewing") || p.contains("cauldron"))
            return new ItemStack(Items.BREWING_STAND);
        if (p.contains("campfire"))
            return new ItemStack(Items.CAMPFIRE);
        if (p.contains("portal") || p.contains("nether"))
            return new ItemStack(Items.OBSIDIAN);

        return null; // Return null to trigger the ♪ glyph fallback in render
    }
}
