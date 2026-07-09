# Sound Visualizer

> Never miss a sound again.

Sound Visualizer translates in-game audio into intuitive HUD indicators. See where sounds come from before you see the source.

## Features

- **Color-coded indicators** — Hostile (red), Friendly (green), Footsteps (gray), Blocks (yellow), Player (white), Ambient (cyan)
- **Smart merging** — Same-direction sounds merge into one indicator to reduce clutter
- **Fully configurable** — Transparency, arc size, radius, icons, fade time, and more
- **Custom colors** — Pick your own color for each sound category
- **Whitelist/Blacklist** — Filter specific sound events
- **Client-side only** — Works on any server, no install required
- **Cross-loader** — Fabric and NeoForge via Architectury

## Configuration

Open via Mod Menu (Fabric) or the Mods button (NeoForge). Settings include:

| Setting | Default | Description |
|---------|---------|-------------|
| Transparency | 100% | Overall indicator opacity |
| Arc Thickness | 32 | Size of the directional arcs |
| Orbit Radius | 50 | Distance from crosshair |
| Icon Scale | 1.0 | Size of category icons |
| Fade Time | 2.0s | How long indicators linger |
| Distance Scaling | On | Indicators shrink with distance |
| Show Icons | On | Display category icons on arcs |

## Requirements

- Minecraft 26.1+ (tested on 26.1.2 and 26.2)
- Java 25
- Fabric Loader 0.19+ or NeoForge 26.1+
- Architectury API
- Cloth Config
- Mod Menu (optional, for config screen)

## Changelog

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
