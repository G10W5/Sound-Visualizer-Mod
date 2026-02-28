package com.example.soundvisualizer;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = "soundvisualizer", dist = Dist.CLIENT)
public class SoundVisualizerNeoForge {

    public SoundVisualizerNeoForge(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(this::onClientSetup);
        NeoForge.EVENT_BUS.addListener(this::onRenderGui);

        // Register Config Screen
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (client, parent) -> SoundVisualizerNeoForgeConfig.create(parent));
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        SoundVisualizerCommon.LOGGER.info("Sound Visualizer NeoForge initialized!");
        SoundVisualizerConfig.INSTANCE.init(FMLPaths.CONFIGDIR.get().resolve("soundvisualizer.properties"));
    }

    private void onRenderGui(RenderGuiEvent.Post event) {
        // NeoForge 1.21.x RenderGuiEvent.Post has getGuiGraphics() and getPartialTick()
        // SoundIndicatorRenderer.render expects GuiGraphics and DeltaTracker.
        // For now, passing null for DeltaTracker as it's not strictly used for logic in
        // the renderer yet (just alpha decay)
        SoundIndicatorRenderer.INSTANCE.render(event.getGuiGraphics(), null);
    }
}
