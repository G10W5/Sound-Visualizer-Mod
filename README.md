# Sound Visualizer

> Never miss a sound again.

Sound Visualizer translates in-game audio into intuitive HUD indicators. See where sounds come from before you see the source. Works on both **Fabric** and **NeoForge**.

---

## Features

### Smart Sound Categorization
Sounds are identified by entity/event name (with a sound-source fallback for modded content) and color-coded:
| Category | Color | Examples |
|----------|-------|----------|
| Hostile | Red | Zombies, creepers, skeletons — including their footsteps |
| Friendly | Green | Villagers, passive mobs |
| Footsteps | Gray | Walking on blocks, Presence Footsteps |
| Blocks | Yellow | Mining, pistons, doors, explosions |
| Player | White | Other players, projectiles, armor |
| Ambient | Cyan | Weather, caves, environment |

Non-positional sounds (UI, music) and your own footsteps / swings are filtered out automatically.

### Procedural HUD
- Smooth tapered arcs drawn procedurally, so length and thickness are independent and always sharp
- Category icon sits on the arc in its own soft gap, with an optional backdrop for readability
- Soft bloom, ripple echo when a source repeats, spawn pop and smooth view tracking
- Styles: **Arc**, **Dashed**, **Chevron**
- Presets: **Minimal**, **Balanced**, **Cinematic**, **Tactical**
- Animations pause with the game and the HUD hides with F1

### Anti-Spam Merging
Sounds from the same direction and category merge into a single indicator (merge angle is configurable).

### Configurable
- **Toggle key** — bind it under Controls › Sound Visualizer
- **Style / Preset / Radius / Line Thickness / Arc Length / Glow / Opacity / Fade Time**
- **Icons** — size, backdrop, category tint
- **Detection** — hearing distance, hide own sounds, merge angle, distance emphasis, tracking speed
- **Whitelist/Blacklist** — full ids, partial matches or wildcards (`entity.*.step`)
- **Category Toggles & Colors**

---

## Requirements

- Minecraft **26.3**
- Java **25**
- **Fabric Loader** 0.19.5+ or **NeoForge** 26.3+
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

## License

MIT License — free to use in any modpack.

Developed with ❤️ by **G10W5**.
