package com.example.soundvisualizer;

import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;

public class SoundVisualizerHit {
    public Identifier soundId;
    public Vec3 position;
    public final Component subtitle;
    public final float range;
    public float volume;
    public final SoundCategory category;
    public long startTime;
    public long lastMergeTime;
    public float mergeAnimationProgress = 0.0f;
    public float alpha = 1.0f;
    public float scale = 1.0f;

    public SoundVisualizerHit(Identifier soundId, Vec3 position, Component subtitle, float range, float volume,
            SoundCategory category) {
        this.soundId = soundId;
        this.position = position;
        this.subtitle = subtitle;
        this.range = range;
        this.volume = volume;
        this.category = category;
        this.startTime = System.currentTimeMillis();
        this.lastMergeTime = 0;
    }

    public void refresh(Vec3 newPos, float newVol) {
        this.position = newPos;
        this.volume = Math.max(this.volume, newVol);
        this.startTime = System.currentTimeMillis(); // Reset main animation
        this.lastMergeTime = System.currentTimeMillis();
        this.mergeAnimationProgress = 1.0f; // Start merge animation
    }

    public void update() {
        long now = System.currentTimeMillis();
        long elapsed = now - startTime;
        float fadeTimeMs = SoundVisualizerConfig.INSTANCE.fadeTimeSeconds * 1000.0f;
        
        // Alpha calculation
        alpha = Math.max(0.0f, 1.0f - (elapsed / fadeTimeMs));
        
        // Merge animation (zoom in effect)
        if (lastMergeTime > 0) {
            long mergeElapsed = now - lastMergeTime;
            if (mergeElapsed < 300) {
                mergeAnimationProgress = 1.0f - (mergeElapsed / 300.0f);
            } else {
                mergeAnimationProgress = 0.0f;
            }
        }

        // Scale calculation (elastic pop in)
        if (elapsed < 500) {
            float t = elapsed / 500.0f;
            double c4 = (2 * Math.PI) / 3;
            scale = 1.0f + (float) (Math.pow(2, -10 * t) * Math.sin((t * 10 - 0.75) * c4) * 0.5);
        } else {
            scale = 1.0f - ((elapsed - 500) / (fadeTimeMs - 500)) * 0.5f;
            scale = Math.max(0.5f, scale);
        }
    }

    public boolean isExpired() {
        return alpha <= 0.0f;
    }
}
