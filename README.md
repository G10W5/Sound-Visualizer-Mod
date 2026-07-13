# Sound Visualizer

> Never miss a sound again.

Sound Visualizer translates in-game audio into intuitive HUD indicators. See where sounds come from before you see the source. Works on both **Fabric** and **NeoForge**.

---

## Features

### Smart Sound Categorization
Sounds are automatically identified and color-coded:
| Category | Color | Examples |
|----------|-------|----------|
| Hostile | Red | Zombies, creepers, skeletons |
| Friendly | Green | Villages, passive mobs |
| Footsteps | Gray | Entity movement |
| Blocks | Yellow | Mining, pistons, dispensers |
| Player | White | Other players |
| Ambient | Cyan | Music, weather, environment |

### Anti-Spam Merging
Sounds from the same direction and category merge into a single indicator. No more HUD clutter from rapid-fire sounds.

### Configurable
- **Transparency** — 0% to 100% slider to reduce visual intensity
- **Arc Thickness** — Adjust indicator size
- **Orbit Radius** — How far indicators sit from crosshair
- **Icon Scale** — Resize category icons
- **Fade Time** — How long indicators linger
- **Distance Scaling** — Indicators shrink with distance
- **Show/Hide Icons** — Toggle category icons on arcs
- **Whitelist/Blacklist** — Filter specific sound events
- **Custom Colors** — Per-category color picker

---

## Requirements

- Minecraft **26.1+** (tested on 26.1.2 and 26.2)
- Java **25**
- **Fabric Loader** 0.19+ or **NeoForge** 26.1+
- **Architectury API**
- **Cloth Config**
- **Mod Menu** (Fabric, optional — provides config screen)

---

## Installation

1. Install Fabric or NeoForge for your Minecraft version
2. Install Architectury API and Cloth Config
3. Drop the mod JAR into your `mods` folder
4. Launch — no server-side install needed

---

## Configuration

Open via **Mod Menu** (Fabric) or the **Mods** button (NeoForge), find **Sound Visualizer**, and click **Config**.

Settings are saved to `config/soundvisualizer.properties` and persist across restarts.

---

## Performance

- **Client-side only** — no server install required
- **Zero FPS impact** — uses optimized 2D matrix rendering
- **No ghosting artifacts** — clean indicators without post-processing

---

## Changelog

### 2.2.0
- Added category enable/disable toggles — hide categories you don't want to see
- Improved Presence Footsteps compatibility — footsteps now correctly show neutral icons
- Fixed SoundSource.NEUTRAL being misclassified as Friendly

### 2.1.0-beta.2
- Fixed config not loading on NeoForge (settings now persist across restarts)
- Renamed Transparency to Opacity for clarity
- Added single universal JAR for both Fabric and NeoForge

### 2.1.0-beta.1
- Added Transparency slider (0-100%) to reduce indicator visibility
- Fixed config not persisting across restarts

### 2.1.0-beta
- Updated to Minecraft 26.1.2 and Java 25
- Updated Architectury API, Fabric API, NeoForge, Cloth Config, and Mod Menu
- Migrated Shadow plugin for Gradle 9.x compatibility
- Fixed Fabric client source set configuration
- Verified working on Fabric (26.2) and NeoForge (26.1.2, 26.2)

### 2.0.0
- Initial cross-loader release (Fabric + NeoForge via Architectury)
- Added NeoForge support
- Added config screen via Cloth Config
- Added sound merging, distance scaling, and icon rendering

---

## License

MIT License — free to use in any modpack.

Developed with ❤️ by **G10W5**.
