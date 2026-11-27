# Sound Visualizer

> Never miss a sound again.

Sound Visualizer translates in-game audio into clean, directional HUD indicators around your crosshair. See where sounds come from before you see the source. Works on both **Fabric** and **NeoForge** — one universal JAR for both.

## Features

- **Procedural HUD indicators** — Smooth tapered arcs with independent length and thickness, soft bloom glow, and optional ripple echoes when a source repeats
- **3 indicator styles & 4 presets** — Choose between **Arc**, **Dashed**, and **Chevron** styles, or apply **Minimal** (default), **Balanced**, **Cinematic**, or **Tactical** presets
- **Live config preview** — See your changes update in real time inside the Appearance settings menu (click the preview box to trigger a pulse)
- **Crisp 16×16 pixel-art icons** — Hostile (red), Friendly (green), Footsteps (gray), Blocks (yellow), Player (white), and Ambient (cyan), with optional backdrop disc and category tinting
- **Smart sound detection** — Automatically filters out non-positional UI/music sounds and your own footsteps/swings, includes hostile footsteps in Hostile, supports **Presence Footsteps**, and falls back to `SoundSource` for modded mobs
- **Anti-spam merging** — Sounds from the same direction and category merge smoothly into one indicator
- **Wildcard filters** — Whitelist and blacklist support full IDs, partial matches, and `*` wildcards (e.g. `entity.*.step`, `block.*.place`)
- **Polished behavior** — Smooth view tracking, pause-aware fade timers, automatic F1 HUD hiding, and an assignable toggle keybind (*Controls › Sound Visualizer*)
- **Client-side only** — Works on any server, no server install required

## Configuration

Open via **Mod Menu** (Fabric) or the **Mods** button (NeoForge), find **Sound Visualizer**, and click **Config**.

### Appearance tab
Includes a **Live Preview** at the top of the page.

| Setting | Default | Description |
|---------|---------|-------------|
| Enable HUD | On | Show or hide indicators (also toggleable via keybind) |
| Apply Preset | Minimal | Quickly switch between Minimal, Balanced, Cinematic, and Tactical |
| Indicator Style | Arc | `Arc`, `Dashed`, or `Chevron` |
| Orbit Radius | 50 px | Distance from the crosshair |
| Line Thickness | 2.5 px | Width of the arc line |
| Arc Length | 28° | Angular length of each indicator |
| Glow | 35% | Soft bloom around the arc (`0%` disables) |
| Opacity | 100% | Overall indicator visibility |
| Fade Time | 1.5 s | How long indicators linger (pauses with the game) |
| Ripple Echo | Off | Expanding echo when a sound source repeats |
| Category Icons | On | Show 16×16 pixel-art icon in the center of each arc |
| Icon Size | 0.9x | Scale of category icons |
| Icon Backdrop | Off | Soft dark disc behind icons for bright backgrounds |
| Tint Icons | Off | Tint icons with their category color instead of white |

### Detection tab

| Setting | Default | Description |
|---------|---------|-------------|
| Hearing Distance | 16 blocks | Maximum range for sound detection |
| Hide My Own Sounds | On | Ignore your own footsteps, swings, eating, and armor |
| Merge Angle | 18° | Merge same-category sounds within this angle (`0°` disables) |
| Distance Emphasis | On | Nearby sounds appear thicker and longer than distant ones |
| Smooth Tracking | On | Smoothly glide indicators as you turn |
| Tracking Speed | 14 | How quickly indicators follow your camera |
| Whitelist | Empty | Only show matching sound events (supports `*` wildcards) |
| Blacklist | `minecraft:weather.rain` | Hide matching sound events (supports `*` wildcards) |

### Categories tab
- **Enable / Disable** toggle for each category
- **Custom Color** picker for each category

## Requirements

- Minecraft **26.3**
- Java **25**
- Fabric Loader **0.19.5+** or NeoForge **26.3+**
- **Architectury API**
- **Cloth Config**
- **Mod Menu** (optional — provides the config screen on Fabric)

## Installation

1. Install Fabric or NeoForge for your Minecraft version
2. Install Architectury API and Cloth Config
3. Drop the universal JAR into your `mods` folder
4. Launch — no server-side install needed

## License

MIT — free to use in any modpack.

Developed with ❤️ by **G10W5**.
