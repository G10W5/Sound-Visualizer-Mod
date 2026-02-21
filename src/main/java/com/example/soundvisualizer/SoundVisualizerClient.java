package com.example.soundvisualizer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SoundVisualizerClient implements ClientModInitializer {
    public static final String MOD_ID = "soundvisualizer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Queue<SoundVisualizerHit> HITS = new ConcurrentLinkedQueue<>();
    public static final SoundIndicatorRenderer RENDERER = new SoundIndicatorRenderer();

    @Override
    public void onInitializeClient() {
        LOGGER.info("Sound Visualizer initialized! [Version 1.3.1]");
        HudRenderCallback.EVENT.register(RENDERER);
    }

    public static void processSound(net.minecraft.client.sound.SoundInstance soundInstance) {
        if (soundInstance == null)
            return;

        try {
            net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
            if (client == null || client.player == null)
                return;

            // Defensively check relative
            boolean isRelative = false;
            try {
                isRelative = soundInstance.isRelative();
            } catch (Throwable t) {
            }
            if (isRelative)
                return;

            // DEFENSIVE ATTRIBUTE GRABBING (GHOST DETECTION)
            double x = 0, y = 0, z = 0;
            float vol = 1.0f;
            net.minecraft.util.Identifier id = null;

            try {
                id = soundInstance.getId();
            } catch (Throwable t) {
            }
            if (id == null)
                return;

            // --- Whitelist / Blacklist Filtering ---
            SoundVisualizerConfig config = SoundVisualizerConfig.INSTANCE;
            String soundStr = id.toString();

            // Blacklist check
            if (config.blacklist != null && !config.blacklist.isEmpty()) {
                for (String b : config.blacklist) {
                    if (soundStr.equalsIgnoreCase(b.trim()))
                        return;
                }
            }

            // Whitelist check (only apply when non-empty)
            if (config.whitelist != null && !config.whitelist.isEmpty()) {
                boolean found = false;
                for (String w : config.whitelist) {
                    if (soundStr.equalsIgnoreCase(w.trim())) {
                        found = true;
                        break;
                    }
                }
                if (!found)
                    return;
            }

            try {
                x = soundInstance.getX();
            } catch (Throwable t) {
            }
            try {
                y = soundInstance.getY();
            } catch (Throwable t) {
            }
            try {
                z = soundInstance.getZ();
            } catch (Throwable t) {
            }
            try {
                vol = soundInstance.getVolume();
            } catch (Throwable t) {
            }

            // FILTER: Skip local player footsteps
            String path = id.getPath();
            if (path.contains("step") || path.contains("footstep")) {
                double distSq = client.player.squaredDistanceTo(x, y, z);
                if (distSq < 0.25)
                    return;
            }

            // SUBTITLE FILTER: Only show sounds that have a subtitle defined.
            // Vanilla key format: subtitles.entity.zombie.ambient (NO namespace in key)
            // Mod key format: subtitles.[modid].[path]
            if (config.subtitleOnly) {
                // Manual key check (reliable fallback)
                String pathDots = path.replace("/", ".");
                String subtitleKey = "minecraft".equals(id.getNamespace())
                        ? "subtitles." + pathDots
                        : "subtitles." + id.getNamespace() + "." + pathDots;

                if (!net.minecraft.util.Language.getInstance().hasTranslation(subtitleKey)) {
                    // Special case: block break/hit sounds are important and sometimes have
                    // specific keys
                    if (!path.contains("block")) {
                        return;
                    }
                }
            }

            // DISTANCE FILTER: if the sound is beyond hearing range, discard it.
            // Uses the user-defined maxHearingDistance (default 16).
            double actualDist = Math.sqrt(client.player.squaredDistanceTo(x, y, z));
            float hearingRange = Math.max(8.0f, vol * config.maxHearingDistance);
            if (actualDist > hearingRange) {
                return;
            }

            // PUSH TO THE CENTRAL VAULT
            SoundCategory category = determineCategory(id, soundInstance, client);
            HITS.add(new SoundVisualizerHit(id, new net.minecraft.util.math.Vec3d(x, y, z), null, (float) hearingRange,
                    vol, category));

        } catch (Throwable t) {
            // SILENT FAIL
        }
    }

    private static SoundCategory determineCategory(net.minecraft.util.Identifier id,
            net.minecraft.client.sound.SoundInstance sound, net.minecraft.client.MinecraftClient client) {
        String path = id.getPath();

        if (path.contains(".zombie") || path.contains(".skeleton") || path.contains(".creeper") ||
                path.contains(".spider") || path.contains(".enderman") || path.contains(".witch") ||
                path.contains(".ghast") || path.contains(".blaze") || path.contains(".slime") ||
                path.contains(".magma_cube") || path.contains(".wither") || path.contains(".ender_dragon") ||
                path.contains(".pillager") || path.contains(".ravager") || path.contains(".evoker") ||
                path.contains(".vindicator") || path.contains(".shulker") || path.contains(".guardian") ||
                path.contains(".elder_guardian") || path.contains(".hoglin") || path.contains(".piglin") ||
                path.contains(".zoglin") || path.contains(".warden") || path.contains(".breeze")) {
            return SoundCategory.HOSTILE;
        }

        if (path.contains(".cow") || path.contains(".pig") || path.contains(".sheep") ||
                path.contains(".chicken") || path.contains(".rabbit") || path.contains(".horse") ||
                path.contains(".donkey") || path.contains(".mule") || path.contains(".llama") ||
                path.contains(".trader_llama") || path.contains(".villager") || path.contains(".wandering_trader") ||
                path.contains(".bee") || path.contains(".fox") || path.contains(".cat") ||
                path.contains(".wolf") || path.contains(".parrot") || path.contains(".panda") ||
                path.contains(".polar_bear") || path.contains(".strider") || path.contains(".axolotl") ||
                path.contains(".glow_squid") || path.contains(".goat") || path.contains(".frog") ||
                path.contains(".tadpole") || path.contains(".camel") || path.contains(".sniffer") ||
                path.contains(".armadillo")) {
            return SoundCategory.FRIENDLY;
        }

        if (path.contains("block.")) {
            return SoundCategory.BLOCKS;
        }

        if (path.contains("ambient.")) {
            return SoundCategory.AMBIENT;
        }

        if (path.contains("entity.player.")) {
            return SoundCategory.PLAYER;
        }

        // Default based on sound source if possible (some sound instances have it)
        try {
            net.minecraft.sound.SoundCategory mcCat = sound.getCategory();
            if (mcCat == net.minecraft.sound.SoundCategory.HOSTILE)
                return SoundCategory.HOSTILE;
            if (mcCat == net.minecraft.sound.SoundCategory.NEUTRAL)
                return SoundCategory.NEUTRAL;
            if (mcCat == net.minecraft.sound.SoundCategory.PLAYERS)
                return SoundCategory.PLAYER;
            if (mcCat == net.minecraft.sound.SoundCategory.BLOCKS)
                return SoundCategory.BLOCKS;
            if (mcCat == net.minecraft.sound.SoundCategory.AMBIENT)
                return SoundCategory.AMBIENT;
        } catch (Throwable ignored) {
        }

        return SoundCategory.NEUTRAL;
    }
}
