package com.example.soundvisualizer;

import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.platform.Platform;

public class SoundVisualizerClientCommon {
    public static void init() {
        SoundVisualizerCommon.LOGGER.info("Sound Visualizer Client Initializing...");

        SoundVisualizerConfig.INSTANCE.init(Platform.getConfigFolder().resolve("soundvisualizer.properties"));

        ClientGuiEvent.RENDER_HUD.register((guiGraphics, deltaTracker) -> {
            SoundIndicatorRenderer.render(guiGraphics, deltaTracker);
        });
    }
}
