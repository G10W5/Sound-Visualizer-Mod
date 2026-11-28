package com.example.soundvisualizer;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.atomic.AtomicLong;

public class SoundVisualizerHit {
    private static final float FADE_IN_MS = 90.0f;
    private static final float HOLD_FRACTION = 0.25f;
    private static final AtomicLong NEXT_ID = new AtomicLong(1L);

    public final long id = NEXT_ID.getAndIncrement();
    public final Identifier soundId;
    public final SoundCategory category;
    public Vec3 position;
    public float volume;
    public int mergeCount = 1;

    private final long spawnTime;
    private long refreshTime;
    private boolean angleInitialized = false;
    private boolean displayAngleInitialized = false;

    // Animated state (read by clusterer & renderer)
    public float angle = 0.0f;        // true direction relative to camera yaw
    public float displayAngle = 0.0f; // visual angle (eased on cluster merge/split)
    public long lastClusterSeedId = -1L;
    public float alpha = 0.0f;
    public float scale = 1.0f;
    public float intensity = 1.0f;    // distance/volume driven emphasis
    public float ripple = 1.0f;       // 0..1 progress of the refresh shockwave
    public float flash = 0.0f;
    private boolean expired = false;

    public SoundVisualizerHit(Identifier soundId, Vec3 position, float volume, SoundCategory category) {
        this.soundId = soundId;
        this.position = position;
        this.volume = Math.max(0.1f, volume);
        this.category = category;
        this.spawnTime = HudClock.now();
        this.refreshTime = spawnTime;
    }

    public void refresh(Vec3 newPos, float newVolume) {
        this.position = position.lerp(newPos, 0.65);
        this.volume = Math.max(volume * 0.8f, newVolume);
        this.refreshTime = HudClock.now();
        this.mergeCount++;
    }

    public void update(Minecraft client, float partialTick) {
        SoundVisualizerConfig cfg = SoundVisualizerConfig.INSTANCE;
        long now = HudClock.now();
        float sinceSpawn = now - spawnTime;
        float sinceRefresh = now - refreshTime;
        float fadeMs = Math.max(250.0f, cfg.fadeTimeSeconds * 1000.0f);

        // Opacity: quick fade-in, hold, then smoothstep fade-out
        float life = Mth.clamp(sinceRefresh / fadeMs, 0.0f, 1.0f);
        float fadeOut = life < HOLD_FRACTION ? 1.0f
                : 1.0f - smoothstep((life - HOLD_FRACTION) / (1.0f - HOLD_FRACTION));
        float fadeIn = easeOutCubic(Mth.clamp(sinceSpawn / FADE_IN_MS, 0.0f, 1.0f));
        alpha = fadeOut * fadeIn;
        expired = life >= 1.0f;

        // Scale: elastic pop on spawn, softer pulse on refresh
        float s = 1.0f;
        if (sinceSpawn < 280.0f) {
            float p = sinceSpawn / 280.0f;
            s += (float) Math.sin(p * Math.PI) * 0.22f * (1.0f - p);
        }
        if (sinceRefresh < 220.0f && refreshTime != spawnTime) {
            float p = sinceRefresh / 220.0f;
            s += (float) Math.sin(p * Math.PI) * 0.10f;
        }
        scale = s;

        ripple = Mth.clamp(sinceRefresh / 480.0f, 0.0f, 1.0f);
        flash = sinceRefresh < 300.0f ? (1.0f - sinceRefresh / 300.0f) * (1.0f - sinceRefresh / 300.0f) : 0.0f;

        if (client.player == null) return;

        double dx = position.x - client.player.getX();
        double dz = position.z - client.player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        if (cfg.distanceScaling) {
            float norm = (float) Mth.clamp(dist / Math.max(1.0f, cfg.maxHearingDistance), 0.0, 1.0);
            float vol = Mth.clamp(volume, 0.3f, 1.5f);
            intensity = Mth.lerp(norm, 1.2f, 0.7f) * (0.9f + 0.1f * vol);
        } else {
            intensity = 1.0f;
        }

        // Direction relative to camera yaw, exponentially smoothed (frame-rate independent)
        float yaw = client.player.getViewYRot(partialTick);
        float target = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f - yaw);
        if (!angleInitialized || !cfg.smoothTracking) {
            angle = target;
            angleInitialized = true;
        } else {
            float k = 1.0f - (float) Math.exp(-cfg.smoothingSpeed * HudClock.frameSeconds());
            angle = Mth.wrapDegrees(angle + Mth.wrapDegrees(target - angle) * k);
        }

        if (!displayAngleInitialized) {
            displayAngle = angle;
            displayAngleInitialized = true;
        }
    }

    /**
     * Smoothly glides {@link #displayAngle} toward {@code targetAngle} so hits
     * separate visually when a cluster splits on look.
     */
    public void easeDisplayAngle(float targetAngle, float speed, boolean smooth) {
        if (!displayAngleInitialized || !smooth) {
            displayAngle = targetAngle;
            displayAngleInitialized = true;
            return;
        }
        float k = 1.0f - (float) Math.exp(-speed * HudClock.frameSeconds());
        displayAngle = Mth.wrapDegrees(displayAngle + Mth.wrapDegrees(targetAngle - displayAngle) * k);
    }

    public boolean isExpired() {
        return expired;
    }

    private static float smoothstep(float t) {
        return t * t * (3.0f - 2.0f * t);
    }

    private static float easeOutCubic(float t) {
        float inv = 1.0f - t;
        return 1.0f - inv * inv * inv;
    }
}
