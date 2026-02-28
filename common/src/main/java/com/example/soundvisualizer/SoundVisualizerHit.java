package com.example.soundvisualizer;

import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;

public class SoundVisualizerHit {
    public final Identifier soundId;
    public final Vec3 position;
    public final Component subtitle;
    public final float range;
    public final float volume;
    public final SoundCategory category;
    public final long startTime;
    public float alpha = 1.0f;

    public SoundVisualizerHit(Identifier soundId, Vec3 position, Component subtitle, float range, float volume,
            SoundCategory category) {
        this.soundId = soundId;
        this.position = position;
        this.subtitle = subtitle;
        this.range = range;
        this.volume = volume;
        this.category = category;
        this.startTime = System.currentTimeMillis();
    }

    public void update() {
        long elapsed = System.currentTimeMillis() - startTime;
        float fadeTimeMs = SoundVisualizerConfig.INSTANCE.fadeTimeSeconds * 1000.0f;
        alpha = Math.max(0.0f, 1.0f - (elapsed / fadeTimeMs));
    }

    public boolean isExpired() {
        return alpha <= 0.0f;
    }
}
