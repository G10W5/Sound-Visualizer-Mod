package com.example.soundvisualizer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

public class SoundVisualizerCommon {
    public static final Logger LOGGER = LoggerFactory.getLogger("soundvisualizer");
    public static final List<SoundVisualizerHit> HITS = new CopyOnWriteArrayList<>();
    private static final int MAX_ACTIVE_HITS = 24;

    public static void processSound(SoundInstance sound) {
        if (sound == null) return;
        SoundVisualizerConfig config = SoundVisualizerConfig.INSTANCE;
        if (!config.enabled) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        Identifier id;
        try {
            id = sound.getIdentifier();
        } catch (Exception e) {
            return;
        }
        if (id == null) return;

        // 1. Non-positional sounds (UI clicks, music, global events) have no direction
        if (sound.isRelative() || sound.getAttenuation() == SoundInstance.Attenuation.NONE) return;
        SoundSource source = sound.getSource();
        if (source == SoundSource.MUSIC || source.name().equals("UI") || source.name().equals("MASTER")) return;

        String fullId = id.toString().toLowerCase(Locale.ROOT);
        if (!passesFilters(config, fullId)) return;

        double x = sound.getX();
        double y = sound.getY();
        double z = sound.getZ();
        if (x == 0.0 && y == 0.0 && z == 0.0) return;

        // 2. Hearing range (3D)
        double distSqr = client.player.distanceToSqr(x, y, z);
        float range = config.maxHearingDistance;
        if (distSqr > (double) range * range) return;

        float volume;
        try {
            volume = sound.getVolume();
        } catch (Exception e) {
            volume = 1.0f;
        }
        if (volume <= 0.01f) return;

        SoundCategory category = SoundClassifier.classify(id, source);
        if (config.disabledCategories.contains(category)) return;

        // 3. Own sounds: footsteps, swings, eating, armor etc. emitted at the local player
        if (config.hideOwnSounds && distSqr < 2.25
                && (source == SoundSource.PLAYERS || category == SoundCategory.NEUTRAL
                || category == SoundCategory.PLAYER)) {
            return;
        }

        Vec3 pos = new Vec3(x, y, z);

        // 4. Merge into an existing indicator pointing the same way
        double newAngle = worldAngle(client, x, z);
        for (SoundVisualizerHit hit : HITS) {
            if (hit.category != category || hit.isExpired()) continue;
            double diff = Math.abs(Mth.wrapDegrees(newAngle - worldAngle(client, hit.position.x, hit.position.z)));
            if (diff < config.mergeAngle || hit.position.distanceToSqr(pos) < 9.0) {
                hit.refresh(pos, volume);
                return;
            }
        }

        // 5. Capacity: evict the least important / most faded indicator
        if (HITS.size() >= MAX_ACTIVE_HITS) {
            SoundVisualizerHit victim = HITS.stream()
                    .min(Comparator.comparingInt((SoundVisualizerHit h) -> h.category.getPriority())
                            .thenComparingDouble(h -> h.alpha))
                    .orElse(null);
            if (victim == null || (victim.category.getPriority() > category.getPriority() && victim.alpha > 0.3f)) {
                return;
            }
            HITS.remove(victim);
        }

        HITS.add(new SoundVisualizerHit(id, pos, volume, category));
    }

    private static boolean passesFilters(SoundVisualizerConfig config, String fullId) {
        if (!config.whitelist.isEmpty()) {
            boolean match = false;
            for (String pattern : config.whitelist) {
                if (matches(pattern, fullId)) {
                    match = true;
                    break;
                }
            }
            if (!match) return false;
        }
        for (String pattern : config.blacklist) {
            if (matches(pattern, fullId)) return false;
        }
        return true;
    }

    /** Supports exact ids, bare paths, substrings and '*' wildcards. */
    static boolean matches(String rawPattern, String fullId) {
        if (rawPattern == null) return false;
        String pattern = rawPattern.trim().toLowerCase(Locale.ROOT);
        if (pattern.isEmpty()) return false;
        if (pattern.indexOf('*') >= 0) {
            String regex = ("\\Q" + pattern + "\\E").replace("*", "\\E.*\\Q");
            String target = pattern.contains(":") ? fullId : fullId.substring(fullId.indexOf(':') + 1);
            return target.matches(regex);
        }
        return fullId.equals(pattern) || fullId.endsWith(":" + pattern) || fullId.contains(pattern);
    }

    private static double worldAngle(Minecraft client, double x, double z) {
        if (client.player == null) return 0.0;
        return Math.toDegrees(Math.atan2(z - client.player.getZ(), x - client.player.getX()));
    }
}
