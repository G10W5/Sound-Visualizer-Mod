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

    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At("HEAD"))
    private void onPlay(net.minecraft.client.sound.SoundInstance soundInstance, CallbackInfo ci) {
        if (soundInstance == null)
            return;

        // Use a simple log to avoid any potential toString() recursion/NPE
        System.out.println("[SoundVisualizer 1.0.9] MIXIN START");

        try {
            net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
            if (client == null || client.player == null)
                return;

            // Defensively check relative
            boolean isRelative = false;
            try {
                isRelative = soundInstance.isRelative();
            } catch (Throwable t) {
            }
            if (isRelative)
                return;

            // DEFENSIVE ATTRIBUTE GRABBING (GHOST DETECTION)
            double x = 0, y = 0, z = 0;
            float vol = 1.0f;
            net.minecraft.util.Identifier id = null;

            try {
                id = soundInstance.getId();
            } catch (Throwable t) {
                System.out.println("[SoundVisualizer 1.0.9] ID GET FAILED");
            }
            if (id == null)
                return;

            try {
                x = soundInstance.getX();
            } catch (Throwable t) {
                System.out.println("[SoundVisualizer 1.0.9] X GET FAILED");
            }
            try {
                y = soundInstance.getY();
            } catch (Throwable t) {
                System.out.println("[SoundVisualizer 1.0.9] Y GET FAILED");
            }
            try {
                z = soundInstance.getZ();
            } catch (Throwable t) {
                System.out.println("[SoundVisualizer 1.0.9] Z GET FAILED");
            }
            try {
                vol = soundInstance.getVolume();
            } catch (Throwable t) {
                System.out.println("[SoundVisualizer 1.0.9] VOL GET FAILED");
            }

            System.out.println("[SoundVisualizer 1.0.9] Pushing to vault: " + id + " at " + x + "," + y + "," + z);

            // PUSH TO THE CENTRAL VAULT (GHOST MODE)
            com.example.soundvisualizer.SoundVisualizerClient.HITS.add(
                    new com.example.soundvisualizer.SoundVisualizerHit(
                            id,
                            new net.minecraft.util.math.Vec3d(x, y, z),
                            null, // Still no subtitle for stability
                            64.0f,
                            vol));

            System.out.println("[SoundVisualizer 1.0.9] PUSH SUCCESS. Vault: "
                    + com.example.soundvisualizer.SoundVisualizerClient.HITS.size());

        } catch (Throwable t) {
            System.out.println("[SoundVisualizer 1.0.9] MIXIN TERMINAL ERROR: " + t.toString());
        }
    }
}
