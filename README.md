# Sound Visualizer

**Never miss a sound again.** Sound Visualizer is a client-side mod for Minecraft that translates in-game audio into intuitive HUD indicators. Whether you're navigating deep caves or engaged in intense PvP, this mod provides the situational awareness you need across 1.21.x versions.

---

## Key Features

### Dual-Style Indicators
Choose the aesthetic that fits your playstyle:
*   **Chevron (Classic)**: Bold, pointed indicators for maximum visibility.
*   **Arc (Modern)**: Smooth, mathematically curved arcs for a sleek HUD.

### Tactical Awareness
*   **Directional Accuracy**: Instantly see which direction a sound is coming from.
*   **Intelligent Icon System**: Automatically displays relevant icons (e.g., Zombie heads, Sword for hits, Boots for steps). 
*   **Note Glyph Fallback**: Unknown sounds now use a clean musical note symbol instead of generic blocks.
*   **Configurable Hearing Distance**: Set a custom "Max Hearing Distance" (default 16 blocks) to filter out irrelevant background noise while keeping nearby action visible.
*   **Distance Scaling**: Indicators grow and fade naturally based on proximity.

### Intelligent Filtering
*   **Local Footstep Suppression**: Automatically ignores your own movement sounds.
*   **Mining Sound Detection**: Special handling for block breaking and hitting to ensure you never miss nearby mining activity.
*   **Ghost Detection Engine**: Robust, crash-proof architecture compatible with complex modpacks and multiple Minecraft versions (1.21.1 to 1.21.11+).

### Complete Customization
Full support for Mod Menu and Cloth Config, allowing you to tune:
*   **Indicator Color**: Custom HEX support with a built-in color picker.
*   **Granular Layout**: Independent sliders for Icon Size, Icon Offset, Indicator Size, Indicator Width, and Radius.
*   **Fade Timing**: Control exactly how long sound hits linger on your screen.

---

## Performance
*   **Client-Side Only**: Works on any server without local installation required.
*   **Optimized Rendering**: Minimal impact on FPS using modern Fabric rendering APIs.
*   **Clean Logs**: Production-ready performance with zero console spam.

---

## Requirements
*   **Fabric Loader** (1.21.1+)
*   **Fabric API**
*   **Cloth Config API**
*   **Mod Menu** (Recommended for settings)

---

## License
Licensed under the **MIT License** — Free to use in any modpack.
