package com.example.soundvisualizer;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod("soundvisualizer")
public class SoundVisualizerNeoForge {

    public SoundVisualizerNeoForge(IEventBus modEventBus, ModContainer container) {
        if (FMLEnvironment.getDist() != Dist.CLIENT) return; // client-only mod

        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::onRegisterKeys);
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (ModContainer modContainer, Screen parent) -> SoundVisualizerConfigScreen.create(parent));
    }

    private void onRegisterKeys(RegisterKeyMappingsEvent event) {
        KeyMapping.Category category = new KeyMapping.Category(SoundVisualizerKeys.CATEGORY_ID);
        event.registerCategory(category);
        event.register(SoundVisualizerKeys.create(category));
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        SoundVisualizerClientCommon.init();
    }
}
