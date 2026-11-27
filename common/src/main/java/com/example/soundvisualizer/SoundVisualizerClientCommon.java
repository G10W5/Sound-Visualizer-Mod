package com.example.soundvisualizer;

import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.platform.Platform;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class SoundVisualizerClientCommon {
    public static void init() {
        SoundVisualizerCommon.LOGGER.info("Sound Visualizer client initializing");
        SoundVisualizerConfig.INSTANCE.init(Platform.getConfigFolder().resolve("soundvisualizer.properties"));

        ClientGuiEvent.RENDER_HUD.register(SoundIndicatorRenderer::render);
        ClientTickEvent.CLIENT_POST.register(SoundVisualizerClientCommon::onClientTick);
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> SoundVisualizerCommon.HITS.clear());
    }

    private static void onClientTick(Minecraft client) {
        if (SoundVisualizerKeys.TOGGLE == null) return;
        while (SoundVisualizerKeys.TOGGLE.consumeClick()) {
            SoundVisualizerConfig cfg = SoundVisualizerConfig.INSTANCE;
            cfg.enabled = !cfg.enabled;
            cfg.save();
            if (!cfg.enabled) SoundVisualizerCommon.HITS.clear();
            Component state = Component.translatable(cfg.enabled ? "message.soundvisualizer.on" : "message.soundvisualizer.off")
                    .withStyle(cfg.enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
            client.gui.hud.setOverlayMessage(Component.translatable("message.soundvisualizer.toggle", state), false);
        }
    }
}
