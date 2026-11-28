package com.example.soundvisualizer;

import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Lossless render-time clustering of nearby {@link SoundVisualizerHit}s.
 * Tracks per-hit visual offsets and wing unfurl factors so merging and
 * demerging animate smoothly without popping or double-arc flashes.
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
        public final float leftWingFactor;
        public final float rightWingFactor;
        public final float iconScaleMultiplier;
        public final float[] iconSlotAngles;
        public final float[] iconSlotScales;

        private Cluster(List<SoundVisualizerHit> hits, float angle,
                        List<SoundCategory> orderedCategories, List<SoundCategory> priorityCategories,
                        float alpha, float scale, float intensity, float ripple, float flash, int maxPriority,
                        float leftWingFactor, float rightWingFactor, float iconScaleMultiplier,
                        float[] iconSlotAngles, float[] iconSlotScales) {
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
            this.leftWingFactor = leftWingFactor;
            this.rightWingFactor = rightWingFactor;
            this.iconScaleMultiplier = iconScaleMultiplier;
            this.iconSlotAngles = iconSlotAngles;
            this.iconSlotScales = iconSlotScales;
        }
    }

    private SoundClusterer() {
    }

    public static List<Cluster> cluster(List<SoundVisualizerHit> hits, SoundVisualizerConfig cfg, boolean updateHitState) {
        if (hits.isEmpty()) return List.of();

        List<SoundVisualizerHit> sorted = new ArrayList<>(hits);
        sorted.sort(Comparator.comparingInt((SoundVisualizerHit h) -> h.category.getPriority()).reversed()
                .thenComparing(Comparator.comparingDouble((SoundVisualizerHit h) -> h.alpha).reversed()));

        float iconSize = 16.0f * cfg.iconScale;
        float dualOffsetDeg = (float) Math.toDegrees(Math.atan((iconSize * 0.82f * 0.54f) / Math.max(8.0f, (float) cfg.radius)));

        if (!cfg.stackSounds || cfg.stackAngle <= 0.5f) {
            List<Cluster> singles = new ArrayList<>(sorted.size());
            for (SoundVisualizerHit h : sorted) {
                singles.add(buildSoloCluster(h, cfg, updateHitState));
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

            if (updateHitState) {
                for (SoundVisualizerHit m : members) {
                    m.lastClusterSeedId = seed.id;
                }
            }

            if (members.size() == 1) {
                result.add(buildSoloCluster(seed, cfg, updateHitState));
            } else {
                result.add(buildMultiCluster(members, clusterAngle, dualOffsetDeg, cfg, updateHitState));
            }
        }

        // Draw lowest priority first so high-priority clusters appear on top
        result.sort(Comparator.comparingInt(c -> c.maxPriority));
        return result;
    }

    private static Cluster buildSoloCluster(SoundVisualizerHit hit, SoundVisualizerConfig cfg, boolean updateHitState) {
        float renderAngle = hit.angle;
        float lWing = 1.0f;
        float rWing = 1.0f;
        float iconMult = 1.0f;

        if (updateHitState) {
            if (hit.wasInMultiCluster) {
                hit.beginAngleTransition(hit.angle, cfg.smoothTracking);
                hit.wasInMultiCluster = false;
            }
            hit.decayAngleOffset(7.5f, cfg.smoothTracking);
            hit.easeMorphTo(1.0f, 1.0f, 1.0f, 7.5f, cfg.smoothTracking);
            renderAngle = Mth.wrapDegrees(hit.angle + hit.visualAngleOffset);
            hit.lastRenderedAngle = renderAngle;
            hit.hasRenderedAngle = true;
            lWing = hit.leftWingFactor;
            rWing = hit.rightWingFactor;
            iconMult = hit.visualIconScale;
        }

        List<SoundCategory> cat = List.of(hit.category);
        return new Cluster(List.of(hit), renderAngle, cat, cat,
                hit.alpha, hit.scale, hit.intensity, hit.ripple, hit.flash, hit.category.getPriority(),
                lWing, rWing, iconMult, new float[]{renderAngle}, new float[]{iconMult});
    }

    private static Cluster buildMultiCluster(List<SoundVisualizerHit> members, float clusterAngle,
                                             float dualOffsetDeg, SoundVisualizerConfig cfg, boolean updateHitState) {
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
            float localOffset = Mth.wrapDegrees(h.angle - clusterAngle);
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

        List<SoundCategory> shownCats = selectShownCategories(orderedCats, priorityCats);
        SoundCategory shown0 = shownCats.get(0);
        SoundCategory shown1 = shownCats.size() >= 2 ? shownCats.get(1) : null;

        float[] slotAngles = new float[shownCats.size()];
        float[] slotScales = new float[shownCats.size()];

        if (updateHitState) {
            SoundVisualizerHit rep0 = null;
            SoundVisualizerHit rep1 = null;

            for (SoundVisualizerHit m : members) {
                float slotTarget;
                float targetScale;
                float targetL;
                float targetR;

                if (shownCats.size() == 1) {
                    slotTarget = clusterAngle;
                    targetScale = 1.0f;
                    targetL = 1.0f;
                    targetR = 1.0f;
                } else if (m.category == shown0) {
                    slotTarget = Mth.wrapDegrees(clusterAngle - dualOffsetDeg);
                    targetScale = 0.82f;
                    targetL = 1.0f;
                    targetR = 0.0f;
                } else if (m.category == shown1) {
                    slotTarget = Mth.wrapDegrees(clusterAngle + dualOffsetDeg);
                    targetScale = 0.82f;
                    targetL = 0.0f;
                    targetR = 1.0f;
                } else {
                    slotTarget = clusterAngle;
                    targetScale = 0.45f;
                    targetL = 0.0f;
                    targetR = 0.0f;
                }

                if (!m.wasInMultiCluster) {
                    m.beginAngleTransition(slotTarget, cfg.smoothTracking);
                    m.wasInMultiCluster = true;
                }
                m.decayAngleOffset(10.5f, cfg.smoothTracking);
                m.easeMorphTo(targetScale, targetL, targetR, 11.0f, cfg.smoothTracking);

                float currentSlot = Mth.wrapDegrees(slotTarget + Mth.clamp(m.visualAngleOffset, -28.0f, 28.0f));
                m.lastRenderedAngle = currentSlot;
                m.hasRenderedAngle = true;

                if (m.category == shown0 && (rep0 == null || m.alpha > rep0.alpha)) rep0 = m;
                if (shown1 != null && m.category == shown1 && (rep1 == null || m.alpha > rep1.alpha)) rep1 = m;
            }

            if (shownCats.size() == 1) {
                slotAngles[0] = rep0 != null ? rep0.lastRenderedAngle : clusterAngle;
                slotScales[0] = rep0 != null ? rep0.visualIconScale : 1.0f;
            } else {
                slotAngles[0] = rep0 != null ? rep0.lastRenderedAngle : Mth.wrapDegrees(clusterAngle - dualOffsetDeg);
                slotScales[0] = rep0 != null ? rep0.visualIconScale : 0.82f;
                slotAngles[1] = rep1 != null ? rep1.lastRenderedAngle : Mth.wrapDegrees(clusterAngle + dualOffsetDeg);
                slotScales[1] = rep1 != null ? rep1.visualIconScale : 0.82f;
            }
        } else {
            if (shownCats.size() == 1) {
                slotAngles[0] = clusterAngle;
                slotScales[0] = 1.0f;
            } else {
                slotAngles[0] = Mth.wrapDegrees(clusterAngle - dualOffsetDeg);
                slotScales[0] = 0.82f;
                slotAngles[1] = Mth.wrapDegrees(clusterAngle + dualOffsetDeg);
                slotScales[1] = 0.82f;
            }
        }

        return new Cluster(members, clusterAngle, orderedCats, priorityCats,
                maxAlpha, maxScale, maxIntensity, minRipple, maxFlash, maxPriority,
                1.0f, 1.0f, 1.0f, slotAngles, slotScales);
    }

    private static List<SoundCategory> selectShownCategories(List<SoundCategory> ordered, List<SoundCategory> priority) {
        if (ordered.size() <= 2) return ordered;
        SoundCategory p0 = priority.get(0);
        SoundCategory p1 = priority.get(1);
        return ordered.indexOf(p0) <= ordered.indexOf(p1) ? List.of(p0, p1) : List.of(p1, p0);
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
