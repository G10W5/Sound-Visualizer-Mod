package com.example.soundvisualizer.mixin;

import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

/**
 * Injects into SoundManager.play, which is the high-level entry point for
 * sounds.
 * This is more stable across 1.21.x updates than the lower-level SoundSystem.
 */
@Mixin(SoundManager.class)
public abstract class SoundManagerMixin {

    @Inject(method = "play", at = @At("HEAD"))
    private void onPlay(SoundInstance soundInstance,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Object> cir) {
        com.example.soundvisualizer.SoundVisualizerClient.processSound(soundInstance);
    }
}
