package com.example.soundvisualizer;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.resources.sounds.SoundInstance;
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

        // Exclude sounds played at the player's exact location (own footsteps, own
        // eating, etc.)
        // We allow footstep sounds from very close to the player if they are NOT at 0,0
        // pos
        // Only skip if within 0.5 blocks AND it's a footstep-type sound
        String soundPath = soundInstance.getIdentifier() == null ? "" : soundInstance.getIdentifier().getPath();
        boolean isFootstep = soundPath.contains(".step") || soundPath.contains("footstep");
        if (isFootstep && client.player.distanceToSqr(x, y, z) < 1.0) {
            return; // Skip own footsteps
        }
        // Exclude very exactly on-player sounds (at position 0,0,0 or exactly at
        // player)
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
        } catch (Exception e) {
            // Sound might not be resolved yet (field_5444 is null in AbstractSoundInstance)
            // We use the base volume if possible, or 1.0f
        }

        SoundCategory category = determineCategory(id, soundInstance, client);
        HITS.add(new SoundVisualizerHit(id, new Vec3(x, y, z), null, hearingRange, vol, category));
    }

    private static SoundCategory determineCategory(Identifier id, SoundInstance sound, Minecraft client) {
        String p = id.getPath();
        if (p.contains("entity.zombie") || p.contains("entity.creeper") || p.contains("entity.skeleton") ||
                p.contains("entity.spider") || p.contains("entity.enderman") || p.contains("entity.ghast") ||
                p.contains("entity.blaze") || p.contains("entity.warden") || p.contains("entity.breeze") ||
                p.contains("entity.hostile")) {
            return SoundCategory.HOSTILE;
        }
        if (p.contains("entity.pig") || p.contains("entity.cow") || p.contains("entity.chicken") ||
                p.contains("entity.villager") || p.contains("entity.sheep") || p.contains("entity.friendly")) {
            return SoundCategory.FRIENDLY;
        }
        if (p.contains("ambient.") || p.contains("music.")) {
            return SoundCategory.AMBIENT;
        }
        if (p.contains("block.")) {
            return SoundCategory.BLOCKS;
        }
        if (p.contains("entity.player")) {
            return SoundCategory.PLAYER;
        }

        // Fallback to Minecraft Sound Category
        switch (sound.getSource()) {
            case HOSTILE:
                return SoundCategory.HOSTILE;
            case NEUTRAL:
                return SoundCategory.FRIENDLY;
            case AMBIENT:
                return SoundCategory.AMBIENT;
            case BLOCKS:
                return SoundCategory.BLOCKS;
            case PLAYERS:
                return SoundCategory.PLAYER;
            default:
                return SoundCategory.NEUTRAL;
        }
    }
}
