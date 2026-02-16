package com.example.soundvisualizer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoundVisualizerClient implements ClientModInitializer {
    public static final String MOD_ID = "soundvisualizer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final SoundIndicatorRenderer RENDERER = new SoundIndicatorRenderer();

    @Override
    public void onInitializeClient() {
        LOGGER.info("Sound Visualizer initialized!");
        HudRenderCallback.EVENT.register(RENDERER);
    }
}
