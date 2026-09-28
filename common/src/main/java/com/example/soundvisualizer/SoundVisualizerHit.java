package com.example.soundvisualizer;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class SoundVisualizerHit {
    public Identifier soundId;
    public Vec3 position;
    public final Component subtitle;
    public final float range;
    public float volume;
    public final SoundCategory category;
    
    public long startTime;
    public long lastMergeTime;
    public long lastFrameTime;
    
    public float smoothedRelativeAngle = 0.0f;
    public boolean hasInitializedAngle = false;
    
    public float alpha = 1.0f;
    public float scale = 1.0f;
    public float rippleProgress = 0.0f;
    public float mergeFlash = 0.0f;
    public double elevationDelta = 0.0;
    public float distanceFactor = 1.0f;

    public SoundVisualizerHit(Identifier soundId, Vec3 position, Component subtitle, float range, float volume,
            SoundCategory category) {
        this.soundId = soundId;
        this.position = position;
        this.subtitle = subtitle;
        this.range = Math.max(range, 1.0f);
        this.volume = Math.max(volume, 0.1f);
        this.category = category;
        
        long now = System.currentTimeMillis();
        this.startTime = now;
        this.lastMergeTime = now;
        this.lastFrameTime = now;
    }

    public void refresh(Vec3 newPos, float newVol) {
        this.position = newPos;
        this.volume = Math.max(this.volume, newVol);
        long now = System.currentTimeMillis();
        this.startTime = now;
        this.lastMergeTime = now;
    }

    public void update(Minecraft client, float partialTick) {
        long now = System.currentTimeMillis();
        long elapsed = now - startTime;
        float fadeTimeMs = Math.max(100.0f, SoundVisualizerConfig.INSTANCE.fadeTimeSeconds * 1000.0f);
        
        float progress = Math.min(1.0f, (float) elapsed / fadeTimeMs);
        // Smooth continuous fade out
        alpha = Math.max(0.0f, (float) Math.pow(1.0f - progress, 1.15));
        
        // Elastic pop-in & merge pulse
        float baseScale = 1.0f;
        if (elapsed < 300) {
            float popT = elapsed / 300.0f;
            baseScale = 1.0f + (float) (Math.sin(popT * Math.PI) * 0.35f * (1.0f - popT));
        } else {
            baseScale = 1.0f - (progress * 0.15f);
        }

        // Merge animation & ripple wave
        if (lastMergeTime > 0) {
            long mergeElapsed = now - lastMergeTime;
            if (mergeElapsed < 400) {
                float mergeT = mergeElapsed / 400.0f;
                rippleProgress = mergeT;
                mergeFlash = (float) Math.pow(1.0f - mergeT, 2.0);
                baseScale += (float) (Math.sin(mergeT * Math.PI) * 0.2f);
            } else {
                rippleProgress = 1.0f;
                mergeFlash = 0.0f;
            }
        }

        this.scale = baseScale;

        // Player distance and elevation
        if (client.player != null) {
            double dx = position.x - client.player.getX();
            double dy = position.y - (client.player.getY() + client.player.getEyeHeight(client.player.getPose()));
            double dz = position.z - client.player.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            
            this.elevationDelta = dy;
            
            if (SoundVisualizerConfig.INSTANCE.distanceScaling) {
                float maxDist = Math.max(1.0f, SoundVisualizerConfig.INSTANCE.maxHearingDistance);
                float normalizedDist = (float) Math.min(1.0, dist / maxDist);
                this.distanceFactor = 1.25f - (normalizedDist * 0.5f);
            } else {
                this.distanceFactor = 1.0f;
            }

            // Smooth angle interpolation
            double angleToSound = Math.atan2(dz, dx) * (180.0 / Math.PI) - 90.0;
            float playerYaw = client.player.getViewYRot(partialTick);
            float targetAngle = (float) Mth.wrapDegrees(angleToSound - playerYaw);

            if (!hasInitializedAngle || !SoundVisualizerConfig.INSTANCE.smoothTracking) {
                smoothedRelativeAngle = targetAngle;
                hasInitializedAngle = true;
            } else {
                long frameDtMs = Math.min(100, Math.max(1, now - lastFrameTime));
                float dtSec = frameDtMs / 1000.0f;
                float speed = SoundVisualizerConfig.INSTANCE.smoothingSpeed;
                float angleDiff = (float) Mth.wrapDegrees(targetAngle - smoothedRelativeAngle);
                float lerpFactor = (float) (1.0 - Math.exp(-speed * dtSec));
                smoothedRelativeAngle = (float) Mth.wrapDegrees(smoothedRelativeAngle + angleDiff * lerpFactor);
            }
        }
        
        lastFrameTime = now;
    }

    public boolean isExpired() {
        return alpha <= 0.001f;
    }
}
