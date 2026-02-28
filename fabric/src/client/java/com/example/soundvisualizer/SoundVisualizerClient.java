package com.example.soundvisualizer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoundVisualizerClient implements ClientModInitializer {
    public static final String MOD_ID = "soundvisualizer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Sound Visualizer initialized! [Version 1.3.1]");
        SoundVisualizerConfig.INSTANCE
                .init(FabricLoader.getInstance().getConfigDir().resolve("soundvisualizer.properties"));
        HudRenderCallback.EVENT.register(
                (guiGraphics, deltaTracker) -> SoundIndicatorRenderer.INSTANCE.render(guiGraphics, deltaTracker));
    }

    public static void processSound(net.minecraft.client.resources.sounds.SoundInstance soundInstance) {
        SoundVisualizerCommon.processSound(soundInstance);
    }
}
