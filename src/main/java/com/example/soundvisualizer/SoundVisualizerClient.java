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
        LOGGER.info("Sound Visualizer initialized! [Version 1.2.0]");
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
                double distSq = client.player.getPos().squaredDistanceTo(x, y, z);
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
            double actualDist = client.player.getPos().distanceTo(new net.minecraft.util.math.Vec3d(x, y, z));
            float hearingRange = Math.max(8.0f, vol * config.maxHearingDistance);
            if (actualDist > hearingRange) {
                return;
            }

            // PUSH TO THE CENTRAL VAULT
            HITS.add(new SoundVisualizerHit(id, new net.minecraft.util.math.Vec3d(x, y, z), null, (float) hearingRange,
                    vol));

        } catch (Throwable t) {
            // SILENT FAIL
        }
    }
}
