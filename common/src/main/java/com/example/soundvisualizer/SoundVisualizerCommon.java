package com.example.soundvisualizer;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoundVisualizerCommon {
    public static final Logger LOGGER = LoggerFactory.getLogger("soundvisualizer-common");
    public static final List<SoundVisualizerHit> HITS = new CopyOnWriteArrayList<>();

    public static void processSound(SoundInstance soundInstance) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null)
            return;

        double x = soundInstance.getX();
        double y = soundInstance.getY();
        double z = soundInstance.getZ();

        String soundPath = soundInstance.getIdentifier() == null ? "" : soundInstance.getIdentifier().getPath();
        boolean isFootstep = soundPath.contains(".step") || soundPath.contains("footstep");
        if (isFootstep && client.player.distanceToSqr(x, y, z) < 1.0) {
            return; // Skip own footsteps
        }
        if (!isFootstep && x == 0 && y == 0 && z == 0) {
            return;
        }

        double distSqr = client.player.distanceToSqr(x, y, z);
        float hearingRange = SoundVisualizerConfig.INSTANCE.maxHearingDistance;

        if (distSqr > (hearingRange * hearingRange))
            return;

        Identifier id = soundInstance.getIdentifier();
        float vol = 1.0f;
        try {
            vol = soundInstance.getVolume();
        } catch (Exception e) {}

        SoundCategory category = determineCategory(id, soundInstance, client);
        
        // Merging Logic
        double angleToNewSound = getAngleToSound(client, x, z);
        for (SoundVisualizerHit hit : HITS) {
            if (hit.category == category && !hit.isExpired()) {
                double hitAngle = getAngleToSound(client, hit.position.x, hit.position.z);
                double diff = Math.abs(Mth.wrapDegrees(angleToNewSound - hitAngle));
                
                if (diff < 15.0) {
                    hit.refresh(new Vec3(x, y, z), vol);
                    return;
                }
            }
        }

        if (HITS.size() < 12) {
            HITS.add(new SoundVisualizerHit(id, new Vec3(x, y, z), null, hearingRange, vol, category));
        }
    }

    private static double getAngleToSound(Minecraft client, double x, double z) {
        double dx = x - client.player.getX();
        double dz = z - client.player.getZ();
        return Mth.atan2(dz, dx) * (180.0 / Math.PI) - 90.0;
    }

    private static SoundCategory determineCategory(Identifier id, SoundInstance sound, Minecraft client) {
        String ns = id.getNamespace();
        String p = id.getPath();

        // Footstep detection — vanilla + modded (Presence Footsteps, etc.)
        if (p.contains(".step") || p.contains("footstep") || p.startsWith("step.") ||
                ns.equals("presence_footsteps") || p.startsWith("pf/")) {
            return SoundCategory.NEUTRAL;
        }

        if (p.contains("entity.zombie") || p.contains("entity.creeper") || p.contains("entity.skeleton") ||
                p.contains("entity.spider") || p.contains("entity.enderman") || p.contains("entity.ghast") ||
                p.contains("entity.blaze") || p.contains("entity.warden") || p.contains("entity.hostile")) {
            return SoundCategory.HOSTILE;
        }
        if (p.contains("entity.pig") || p.contains("entity.cow") || p.contains("entity.chicken") ||
                p.contains("entity.villager") || p.contains("entity.sheep") || p.contains("entity.friendly")) {
            return SoundCategory.FRIENDLY;
        }
        if (p.contains("ambient.") || p.contains("music.")) {
            return SoundCategory.AMBIENT;
        }
        if (p.contains("entity.player")) {
            return SoundCategory.PLAYER;
        }
        if (p.contains("block.")) {
            return SoundCategory.BLOCKS;
        }

        SoundSource source = sound.getSource();
        if (source == SoundSource.HOSTILE) return SoundCategory.HOSTILE;
        if (source == SoundSource.NEUTRAL) return SoundCategory.NEUTRAL;
        if (source == SoundSource.AMBIENT) return SoundCategory.AMBIENT;
        if (source == SoundSource.BLOCKS) return SoundCategory.BLOCKS;
        if (source == SoundSource.PLAYERS) return SoundCategory.PLAYER;
        
        return SoundCategory.NEUTRAL;
    }
}
