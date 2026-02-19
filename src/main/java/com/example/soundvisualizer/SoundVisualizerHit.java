package com.example.soundvisualizer;

import net.minecraft.util.math.Vec3d;

public class SoundVisualizerHit {
    public final net.minecraft.util.Identifier soundId;
    public final net.minecraft.util.math.Vec3d position;
    public final net.minecraft.text.Text subtitle;
    public final float range;
    public final float volume;
    public final long startTime;
    public float alpha = 1.0f;

    public SoundVisualizerHit(net.minecraft.util.Identifier soundId, net.minecraft.util.math.Vec3d position,
            net.minecraft.text.Text subtitle, float range, float volume) {
        this.soundId = soundId;
        this.position = position;
        this.subtitle = subtitle;
        this.range = range;
        this.volume = volume;
        this.startTime = System.currentTimeMillis();
    }

    public void update() {
        float fadeTime = com.example.soundvisualizer.SoundVisualizerConfig.INSTANCE.fadeTimeSeconds;
        long lived = System.currentTimeMillis() - startTime;
        float livedSecs = lived / 1000.0f;

        if (livedSecs > fadeTime) {
            alpha = 0;
        } else if (livedSecs > fadeTime * 0.5f) {
            alpha = 1.0f - (livedSecs - fadeTime * 0.5f) / (fadeTime * 0.5f);
        }
    }

    public boolean isExpired() {
        return alpha <= 0;
    }
}
