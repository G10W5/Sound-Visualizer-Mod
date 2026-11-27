package com.example.soundvisualizer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

public class SoundVisualizerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KeyMappingHelper.registerKeyMapping(SoundVisualizerKeys.create(
                KeyMapping.Category.register(SoundVisualizerKeys.CATEGORY_ID)));
        SoundVisualizerClientCommon.init();
    }
}
