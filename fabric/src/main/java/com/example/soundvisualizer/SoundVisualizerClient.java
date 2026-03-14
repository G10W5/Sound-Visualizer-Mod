package com.example.soundvisualizer;

import net.fabricmc.api.ClientModInitializer;

public class SoundVisualizerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SoundVisualizerClientCommon.init();
    }
}
