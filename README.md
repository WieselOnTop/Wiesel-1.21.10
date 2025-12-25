# Wiesel Client

A powerful Minecraft 1.21.1 Fabric client mod.

## Project Structure

```
wiesel-client/
├── src/main/
│   ├── java/com/wiesel/client/
│   │   ├── WieselClient.java           # Main mod entry point
│   │   ├── pathfinder/
│   │   │   └── PathfinderManager.java  # Rust pathfinder integration
│   │   └── mixin/
│   │       └── MixinClientPlayerEntity.java
│   ├── kotlin/com/wiesel/client/
│   │   └── rendering/                  # Rendering utilities (12 files)
│   │       ├── DeferredDrawer.kt
│   │       ├── LineDrawer.kt
│   │       ├── WorldRenderUtils.kt
│   │       ├── WieselRenderLayers.kt
│   │       └── ... (8 more)
│   └── resources/
│       ├── natives/                     # Place Rust pathfinder here
│       ├── fabric.mod.json
│       └── wieselclient.mixins.json
├── build.gradle.kts
└── gradle.properties
```

## Features

- **Welcome Message**: Toxic green gradient effect when joining servers
- **Pathfinding**: Rust pathfinder integration (cross-platform)
- **Rendering**: Professional rendering utilities with Wiesel branding

## Building

```bash
./gradlew build
```

## Development

```bash
./gradlew runClient
```

## Adding Rust Pathfinder

Place your executable in `src/main/resources/natives/`:
- `pathfinder.exe` (Windows)
- `pathfinder-mac` (macOS)
- `pathfinder-linux` (Linux)

## Collaboration

Pull changes:
```bash
git pull origin master
```

Push changes:
```bash
git add .
git commit -m "Your changes"
git push origin master
```

## License

MIT
