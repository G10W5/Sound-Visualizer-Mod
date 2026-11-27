package com.example.soundvisualizer;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/**
 * Key mappings. Category registration differs per loader (Fabric hooks
 * {@code KeyMapping.Category.register}, NeoForge uses its own event), so the
 * platform entrypoint supplies the category before keys are created.
 */
public final class SoundVisualizerKeys {
    public static final Identifier CATEGORY_ID = Identifier.fromNamespaceAndPath("soundvisualizer", "main");

    /** Toggles the HUD on/off. Unbound by default to avoid conflicts; set it in Controls. */
    public static KeyMapping TOGGLE;

    private SoundVisualizerKeys() {
    }

    public static KeyMapping create(KeyMapping.Category category) {
        TOGGLE = new KeyMapping("key.soundvisualizer.toggle", InputConstants.Type.KEYBOARD,
                InputConstants.UNKNOWN.getValue(), category);
        return TOGGLE;
    }
}
