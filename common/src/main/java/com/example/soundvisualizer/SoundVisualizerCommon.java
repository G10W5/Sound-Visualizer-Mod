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
import java.util.concurrent.CopyOnWriteArrayList;

public class SoundVisualizerCommon {
    public static final Logger LOGGER = LoggerFactory.getLogger("soundvisualizer-common");
    public static final List<SoundVisualizerHit> HITS = new CopyOnWriteArrayList<>();
    private static final int MAX_ACTIVE_HITS = 16;

    public static void processSound(SoundInstance soundInstance) {
        if (soundInstance == null) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null)
            return;

        Identifier id = soundInstance.getIdentifier();
        if (id == null) return;

        String fullId = id.toString();
        String path = id.getPath();

        // 1. Whitelist / Blacklist Filtering
        SoundVisualizerConfig config = SoundVisualizerConfig.INSTANCE;
        if (!config.whitelist.isEmpty()) {
            boolean matchesWhitelist = false;
            for (String allowed : config.whitelist) {
                if (allowed != null && !allowed.isBlank()) {
                    if (fullId.equalsIgnoreCase(allowed.trim()) || path.equalsIgnoreCase(allowed.trim()) || fullId.contains(allowed.trim())) {
                        matchesWhitelist = true;
                        break;
                    }
                }
            }
            if (!matchesWhitelist) return;
        }

        if (!config.blacklist.isEmpty()) {
            for (String blocked : config.blacklist) {
                if (blocked != null && !blocked.isBlank()) {
                    if (fullId.equalsIgnoreCase(blocked.trim()) || path.equalsIgnoreCase(blocked.trim()) || fullId.contains(blocked.trim())) {
                        return;
                    }
                }
            }
        }

        // 2. Spatial coordinates check
        double x = soundInstance.getX();
        double y = soundInstance.getY();
        double z = soundInstance.getZ();

        // Ignore UI / global sounds without valid 3D space coordinates
        if (x == 0.0 && y == 0.0 && z == 0.0) {
            return;
        }

        double distSqr = client.player.distanceToSqr(x, y, z);

        // 3. Local player self-sound filtering
        boolean isFootstep = isFootstepSound(id.getNamespace(), path);
        if (distSqr < 0.64) {
            // Very close to player: filter own footsteps, eating, mining, swimming, item use
            if (isFootstep || path.contains("player") || path.contains("item.") || path.contains("swim") || path.contains("step")) {
                return;
            }
        }

        // 4. Hearing range check
        float hearingRange = config.maxHearingDistance;
        if (distSqr > (hearingRange * hearingRange))
            return;

        float vol = 1.0f;
        try {
            vol = Math.max(0.1f, Math.min(2.0f, soundInstance.getVolume()));
        } catch (Exception ignored) {}

        SoundCategory category = determineCategory(id, soundInstance);
        if (config.disabledCategories.contains(category)) {
            return;
        }

        // 5. Smart Merging Logic
        double angleToNewSound = getAngleToSound(client, x, z);
        for (SoundVisualizerHit hit : HITS) {
            if (hit.category == category && !hit.isExpired()) {
                double hitAngle = getAngleToSound(client, hit.position.x, hit.position.z);
                double angleDiff = Math.abs(Mth.wrapDegrees(angleToNewSound - hitAngle));
                double posDistSqr = hit.position.distanceToSqr(x, y, z);
                
                // Merge if in same general direction or close spatial location
                if (angleDiff < 18.0 || posDistSqr < 9.0) {
                    hit.refresh(new Vec3(x, y, z), vol);
                    return;
                }
            }
        }

        // 6. Capacity & Priority Queue Management
        if (HITS.size() >= MAX_ACTIVE_HITS) {
            // Find lowest priority or most faded hit to evict
            SoundVisualizerHit candidate = HITS.stream()
                    .min(Comparator.comparingInt((SoundVisualizerHit h) -> h.category.getPriority())
                            .thenComparingDouble(h -> h.alpha))
                    .orElse(null);

            if (candidate != null && (candidate.category.getPriority() <= category.getPriority() || candidate.alpha < 0.3f)) {
                HITS.remove(candidate);
            } else {
                // If existing sounds all have higher priority, discard new lower priority sound
                return;
            }
        }

        HITS.add(new SoundVisualizerHit(id, new Vec3(x, y, z), null, hearingRange, vol, category));
    }

    private static double getAngleToSound(Minecraft client, double x, double z) {
        if (client.player == null) return 0.0;
        double dx = x - client.player.getX();
        double dz = z - client.player.getZ();
        return Mth.atan2(dz, dx) * (180.0 / Math.PI) - 90.0;
    }

    private static boolean isFootstepSound(String namespace, String path) {
        return path.contains(".step") || path.contains("footstep") || path.startsWith("step.") ||
                path.contains(".fall") || namespace.equals("presence_footsteps") || path.startsWith("pf/");
    }

    private static SoundCategory determineCategory(Identifier id, SoundInstance sound) {
        String ns = id.getNamespace();
        String p = id.getPath().toLowerCase();

        // 1. Footstep / movement
        if (isFootstepSound(ns, p)) {
            return SoundCategory.NEUTRAL;
        }

        // 2. Hostile entities
        if (p.contains("entity.zombie") || p.contains("entity.creeper") || p.contains("entity.skeleton") ||
                p.contains("entity.spider") || p.contains("entity.enderman") || p.contains("entity.ghast") ||
                p.contains("entity.blaze") || p.contains("entity.warden") || p.contains("entity.witch") ||
                p.contains("entity.slime") || p.contains("entity.magma_cube") || p.contains("entity.wither") ||
                p.contains("entity.phantom") || p.contains("entity.pillager") || p.contains("entity.vindicator") ||
                p.contains("entity.evoker") || p.contains("entity.ravager") || p.contains("entity.vex") ||
                p.contains("entity.drowned") || p.contains("entity.husk") || p.contains("entity.stray") ||
                p.contains("entity.piglin_brute") || p.contains("entity.hoglin") || p.contains("entity.zoglin") ||
                p.contains("entity.guardian") || p.contains("entity.elder_guardian") || p.contains("entity.breeze") ||
                p.contains("entity.bogged") || p.contains("entity.hostile")) {
            return SoundCategory.HOSTILE;
        }

        // 3. Friendly / Passive entities
        if (p.contains("entity.pig") || p.contains("entity.cow") || p.contains("entity.chicken") ||
                p.contains("entity.villager") || p.contains("entity.sheep") || p.contains("entity.horse") ||
                p.contains("entity.donkey") || p.contains("entity.mule") || p.contains("entity.llama") ||
                p.contains("entity.cat") || p.contains("entity.wolf") || p.contains("entity.fox") ||
                p.contains("entity.bee") || p.contains("entity.parrot") || p.contains("entity.panda") ||
                p.contains("entity.turtle") || p.contains("entity.axolotl") || p.contains("entity.allay") ||
                p.contains("entity.frog") || p.contains("entity.camel") || p.contains("entity.armadillo") ||
                p.contains("entity.sniffer") || p.contains("entity.iron_golem") || p.contains("entity.friendly")) {
            return SoundCategory.FRIENDLY;
        }

        // 4. Ambient & Music
        if (p.contains("ambient.") || p.contains("music.") || p.contains("weather.") || p.contains("water.") || p.contains("lava.")) {
            return SoundCategory.AMBIENT;
        }

        // 5. Player actions
        if (p.contains("entity.player") || p.contains("item.armor") || p.contains("item.elytra")) {
            return SoundCategory.PLAYER;
        }

        // 6. Blocks
        if (p.contains("block.") || p.contains("door") || p.contains("chest") || p.contains("piston") || p.contains("anvil")) {
            return SoundCategory.BLOCKS;
        }

        // Fallback to sound source
        SoundSource source = sound.getSource();
        if (source == SoundSource.HOSTILE) return SoundCategory.HOSTILE;
        if (source == SoundSource.PLAYERS) return SoundCategory.PLAYER;
        if (source == SoundSource.NEUTRAL) return SoundCategory.NEUTRAL;
        if (source == SoundSource.AMBIENT || source == SoundSource.MUSIC || source == SoundSource.WEATHER) return SoundCategory.AMBIENT;
        if (source == SoundSource.BLOCKS) return SoundCategory.BLOCKS;
        
        return SoundCategory.NEUTRAL;
    }
}

