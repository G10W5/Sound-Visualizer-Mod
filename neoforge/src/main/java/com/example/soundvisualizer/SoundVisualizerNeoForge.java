package com.example.soundvisualizer;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.ModContainer;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod("soundvisualizer")
public class SoundVisualizerNeoForge {

    public SoundVisualizerNeoForge(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(this::onClientSetup);

        // Correct registration for NeoForge 21.1+
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (ModContainer modContainer, Screen parent) -> SoundVisualizerConfigScreen.create(parent));
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        SoundVisualizerClientCommon.init();
    }
}
