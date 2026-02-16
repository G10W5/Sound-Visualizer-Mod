package com.example.soundvisualizer.mixin;

import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.sound.SoundManager.class)
public abstract class SoundManagerMixin {

    @Shadow
    public abstract net.minecraft.client.sound.WeightedSoundSet get(net.minecraft.util.Identifier id);

    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At("HEAD"))
    private void onPlay(net.minecraft.client.sound.SoundInstance soundInstance, CallbackInfo ci) {
        if (soundInstance == null)
            return;

        try {
            com.example.soundvisualizer.SoundVisualizerConfig config = com.example.soundvisualizer.SoundVisualizerConfig.INSTANCE;
            net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
            if (client == null || client.player == null)
                return;

            // Basic filtering
            if (soundInstance.isRelative())
                return;

            // Distance filtering
            double distSq = client.player.squaredDistanceTo(soundInstance.getX(), soundInstance.getY(),
                    soundInstance.getZ());
            if (distSq > 64 * 64)
                return; // Slightly larger range for safety

            // Volume filtering
            if (soundInstance.getVolume() < 0.1f)
                return;

            // SMART DETECTION: Check for subtitles manually
            net.minecraft.text.Text subtitle = null;
            net.minecraft.client.sound.WeightedSoundSet soundSet = this.get(soundInstance.getId());
            if (soundSet != null) {
                subtitle = soundSet.getSubtitle();
            }

            if (config.subtitleOnly && subtitle == null)
                return;

            com.example.soundvisualizer.SoundVisualizerClient.RENDERER.addHit(
                    soundInstance.getId(),
                    new net.minecraft.util.math.Vec3d(
                            soundInstance.getX(),
                            soundInstance.getY(),
                            soundInstance.getZ()),
                    subtitle,
                    32.0f, // Pass a reasonable default range
                    soundInstance.getVolume());
        } catch (Exception ignored) {
        }
    }
}
