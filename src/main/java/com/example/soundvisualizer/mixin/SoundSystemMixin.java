package com.example.soundvisualizer.mixin;

import net.minecraft.client.sound.SoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects into SoundSystem.play, which is common to all 1.21.x versions
 * and consistently returns void, avoiding signature mismatch crashes.
 */
@Mixin(net.minecraft.client.sound.SoundSystem.class)
public abstract class SoundSystemMixin {

    @Inject(method = "play", at = @At("HEAD"), require = 0)
    private void onPlay(SoundInstance soundInstance, CallbackInfo ci) {
        com.example.soundvisualizer.SoundVisualizerClient.processSound(soundInstance);
    }
}
