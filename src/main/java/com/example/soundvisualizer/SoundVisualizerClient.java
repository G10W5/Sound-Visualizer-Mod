package com.example.soundvisualizer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SoundVisualizerClient implements ClientModInitializer {
    public static final String MOD_ID = "soundvisualizer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Queue<SoundVisualizerHit> HITS = new ConcurrentLinkedQueue<>();
    public static final SoundIndicatorRenderer RENDERER = new SoundIndicatorRenderer();

    @Override
    public void onInitializeClient() {
        System.out.println("========================================");
        System.out.println("   SOUND VISUALIZER - BUILD 1.0.9      ");
        System.out.println("   Status: GHOST DETECTION (SAFE)     ");
        System.out.println("========================================");
        LOGGER.info("Sound Visualizer initialized! [Build 1.0.9]");
        HudRenderCallback.EVENT.register(RENDERER);
    }
}
