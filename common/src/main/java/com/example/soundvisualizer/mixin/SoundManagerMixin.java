package com.example.soundvisualizer.mixin;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundManager.class)
public abstract class SoundManagerMixin {

    @Inject(method = "play", at = @At("HEAD"))
    private void onPlay(SoundInstance soundInstance, CallbackInfoReturnable<Object> cir) {
        com.example.soundvisualizer.SoundVisualizerCommon.processSound(soundInstance);
    }
}
