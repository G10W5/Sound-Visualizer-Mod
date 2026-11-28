package com.example.soundvisualizer;

import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Lossless render-time clustering of nearby {@link SoundVisualizerHit}s.
 * Hits stay separate in {@link SoundVisualizerCommon#HITS}, so peripheral
 * clusters can show merged multi-color gradients and smoothly split apart
 * into their exact individual directions when the player looks toward them.
 */
public final class SoundClusterer {

    public static final class Cluster {
        public final List<SoundVisualizerHit> hits;
        public final float angle;
        public final List<SoundCategory> orderedCategories;   // left-to-right by local angular offset
        public final List<SoundCategory> priorityCategories;  // highest priority first
        public final float alpha;
        public final float scale;
        public final float intensity;
        public final float ripple;
        public final float flash;
        public final int maxPriority;

        private Cluster(List<SoundVisualizerHit> hits, float angle,
                        List<SoundCategory> orderedCategories, List<SoundCategory> priorityCategories,
                        float alpha, float scale, float intensity, float ripple, float flash, int maxPriority) {
            this.hits = hits;
            this.angle = angle;
            this.orderedCategories = orderedCategories;
            this.priorityCategories = priorityCategories;
            this.alpha = alpha;
            this.scale = scale;
            this.intensity = intensity;
            this.ripple = ripple;
            this.flash = flash;
            this.maxPriority = maxPriority;
        }
    }

    private SoundClusterer() {
    }

    public static List<Cluster> cluster(List<SoundVisualizerHit> hits, SoundVisualizerConfig cfg, boolean updateHitState) {
        if (hits.isEmpty()) return List.of();

        List<SoundVisualizerHit> sorted = new ArrayList<>(hits);
        sorted.sort(Comparator.comparingInt((SoundVisualizerHit h) -> h.category.getPriority()).reversed()
                .thenComparing(Comparator.comparingDouble((SoundVisualizerHit h) -> h.alpha).reversed()));

        if (!cfg.stackSounds || cfg.stackAngle <= 0.5f) {
            List<Cluster> singles = new ArrayList<>(sorted.size());
            for (SoundVisualizerHit h : sorted) {
                float renderAngle = h.angle;
                if (updateHitState) {
                    h.lastClusterSeedId = h.id;
                    h.easeDisplayAngle(h.angle, 18.0f, cfg.smoothTracking);
                    renderAngle = h.displayAngle;
                }
                singles.add(buildCluster(List.of(h), renderAngle));
            }
            singles.sort(Comparator.comparingInt(c -> c.maxPriority));
            return singles;
        }

        boolean[] used = new boolean[sorted.size()];
        List<Cluster> result = new ArrayList<>();
        float maxClusterSpan = Math.max(cfg.stackAngle * 1.45f, 52.0f);

        for (int i = 0; i < sorted.size(); i++) {
            if (used[i]) continue;
            used[i] = true;
            SoundVisualizerHit seed = sorted.get(i);

            List<SoundVisualizerHit> members = new ArrayList<>();
            members.add(seed);
            float clusterAngle = seed.angle;

            // Greedily pull nearby hits into this cluster, recomputing weighted mean angle
            boolean added;
            do {
                added = false;
                for (int j = 0; j < sorted.size(); j++) {
                    if (used[j]) continue;
                    SoundVisualizerHit cand = sorted.get(j);

                    float distFromSeed = Math.abs(Mth.wrapDegrees(cand.angle - seed.angle));
                    if (distFromSeed > maxClusterSpan) continue;

                    float effectiveAngle = cfg.stackAngle;
                    if (cfg.splitOnLook) {
                        float fromForward = Math.min(Math.abs(clusterAngle), Math.abs(cand.angle));
                        float peripheral = smoothstep(14.0f, 42.0f, fromForward);
                        effectiveAngle = Mth.lerp(peripheral, 4.0f, cfg.stackAngle);
                    }

                    if (updateHitState && seed.lastClusterSeedId != -1L) {
                        boolean wereTogether = (seed.lastClusterSeedId == cand.lastClusterSeedId);
                        effectiveAngle *= wereTogether ? 1.22f : 0.88f;
                    }

                    float distFromCluster = Math.abs(Mth.wrapDegrees(cand.angle - clusterAngle));
                    if (distFromCluster <= effectiveAngle) {
                        used[j] = true;
                        members.add(cand);
                        clusterAngle = weightedMeanAngle(members);
                        added = true;
                    }
                }
            } while (added);

            float renderAngle = clusterAngle;
            if (updateHitState) {
                for (SoundVisualizerHit m : members) {
                    m.lastClusterSeedId = seed.id;
                }
                if (members.size() >= 2) {
                    for (SoundVisualizerHit m : members) {
                        m.easeDisplayAngle(clusterAngle, 22.0f, cfg.smoothTracking);
                    }
                    renderAngle = clusterAngle;
                } else {
                    seed.easeDisplayAngle(seed.angle, 18.0f, cfg.smoothTracking);
                    renderAngle = seed.displayAngle;
                }
            }

            result.add(buildCluster(members, renderAngle));
        }

        // Draw lowest priority first so high-priority clusters appear on top
        result.sort(Comparator.comparingInt(c -> c.maxPriority));
        return result;
    }

    private static Cluster buildCluster(List<SoundVisualizerHit> members, float renderAngle) {
        float maxAlpha = 0.0f;
        float maxScale = 1.0f;
        float maxIntensity = 0.0f;
        float minRipple = 1.0f;
        float maxFlash = 0.0f;
        int maxPriority = Integer.MIN_VALUE;

        Map<SoundCategory, Float> catOffsetSum = new EnumMap<>(SoundCategory.class);
        Map<SoundCategory, Float> catWeightSum = new EnumMap<>(SoundCategory.class);

        for (SoundVisualizerHit h : members) {
            maxAlpha = Math.max(maxAlpha, h.alpha);
            maxScale = Math.max(maxScale, h.scale);
            maxIntensity = Math.max(maxIntensity, h.intensity);
            minRipple = Math.min(minRipple, h.ripple);
            maxFlash = Math.max(maxFlash, h.flash);
            maxPriority = Math.max(maxPriority, h.category.getPriority());

            float w = Math.max(0.05f, h.alpha) * (h.category.getPriority() + 25.0f);
            float localOffset = Mth.wrapDegrees(h.angle - renderAngle);
            catOffsetSum.merge(h.category, localOffset * w, Float::sum);
            catWeightSum.merge(h.category, w, Float::sum);
        }

        List<SoundCategory> priorityCats = new ArrayList<>(catOffsetSum.keySet());
        priorityCats.sort(Comparator.comparingInt(SoundCategory::getPriority).reversed());

        List<SoundCategory> orderedCats = new ArrayList<>(catOffsetSum.keySet());
        orderedCats.sort((a, b) -> {
            float avgA = catOffsetSum.get(a) / catWeightSum.get(a);
            float avgB = catOffsetSum.get(b) / catWeightSum.get(b);
            int cmp = Float.compare(avgA, avgB);
            return cmp != 0 ? cmp : Integer.compare(b.getPriority(), a.getPriority());
        });

        return new Cluster(members, renderAngle, orderedCats, priorityCats,
                maxAlpha, maxScale, maxIntensity, minRipple, maxFlash, maxPriority);
    }

    private static float weightedMeanAngle(List<SoundVisualizerHit> members) {
        double sinSum = 0.0;
        double cosSum = 0.0;
        for (SoundVisualizerHit h : members) {
            double w = Math.max(0.05, h.alpha) * (h.category.getPriority() + 25.0);
            double rad = Math.toRadians(h.angle);
            sinSum += w * Math.sin(rad);
            cosSum += w * Math.cos(rad);
        }
        return (float) Math.toDegrees(Math.atan2(sinSum, cosSum));
    }

    private static float smoothstep(float e0, float e1, float x) {
        float t = Mth.clamp((x - e0) / (e1 - e0), 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }
}
