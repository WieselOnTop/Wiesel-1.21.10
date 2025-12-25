# Wiesel Client

A powerful Minecraft 1.21.1 Fabric client mod with advanced pathfinding capabilities.

## Features

- **Advanced Pathfinding**: Rust-powered pathfinder with HTTP API
- **Automatic Map Loading**: Detects current area from tablist and loads the correct map automatically
- **Visual Path Rendering**: Green line rendering along paths with block highlights every 15 nodes
- **Automatic Walking**: Smart rotation and movement with jump detection
- **Config System**: Clean JSON config in instance directory
- **Welcome Message**: Gradient green welcome message on server join
- **Background Processing**: Map extraction and loading happen in separate threads

## Setup

### 1. Install the Mod

Place `wiesel-client-1.0.0.jar` in your `.minecraft/mods` folder.

### 2. Setup Pathfinder (Required)

1. Download `Pathfinding.exe` and place it in your **Downloads** folder: `C:\Users\YourName\Downloads\Pathfinding.exe`

2. Download map files and place them in your **Downloads** folder:
   - `hub.zip`
   - `mines.zip`
   - `galatea.zip`

The mod will automatically:
- Extract maps to `.minecraft/maps/` on first launch (in background thread)
- Start the Pathfinding.exe process
- Send keepalive requests every 60 seconds
- **Detect your current area from the tablist** (looks for "Area: Hub", "Area: Mines", etc.)
- **Automatically load the correct map** when you change areas

## Usage

### Configuration

Config file: `.minecraft/config/wiesel/config.json`

```json
{
  "pathfinder": {
    "autoStart": true,
    "defaultMap": "hub",
    "keepaliveInterval": 60000
  },
  "render": {
    "enabled": true,
    "pathLineColor": 43520,
    "nodeHighlightColor": 65280,
    "nodeHighlightInterval": 15,
    "pathLineWidth": 2.0,
    "nodeAlpha": 0.5
  }
}
```

### How It Works

The mod automatically detects your current area by reading the tablist:
- When it sees "Area: Hub" → Loads `hub` map
- When it sees "Area: Mines" → Loads `mines` map
- When it sees "Area: Galatea" → Loads `galatea` map

**You don't need to manually load maps!** Just join the server and the mod handles everything.

### API Usage (For Developers)

```java
// Maps are loaded automatically, but you can manually load if needed
PathfinderManager.loadMap("hub");

// Find a path
PathfindResponse path = PathfinderManager.pathfind(x1, y1, z1, x2, y2, z2);

// Advanced pathfinding with options
PathfindResponse path = PathfinderManager.pathfind(
    x1, y1, z1,
    x2, y2, z2,
    false, // useWarpPoints
    false, // useEtherwarp
    true,  // useKeynodes
    false, // useSpline
    false  // isPerfectPath
);

// Start auto-walking
PathWalker.startWalking(path);

// Stop walking
PathWalker.stopWalking();

// Clear rendered path
PathfinderManager.clearPath();
```

### Pathfinder API Details

The pathfinder runs locally on `http://localhost:3000` and provides these endpoints:

- `GET /api/loadmap?map={mapname}` - Load a map
- `POST /api/pathfind` - Calculate a path
- `GET /keepalive` - Keep the process alive (sent automatically)

**Request Body** for `/api/pathfind`:
```json
{
  "start": "x,y,z",
  "end": "x,y,z",
  "use_warp_points": false,
  "use_etherwarp": false,
  "use_keynodes": true,
  "use_spline": false,
  "is_perfect_path": false
}
```

## Project Structure

```
wiesel-client/
├── src/main/java/com/wiesel/client/
│   ├── WieselClient.java               # Main mod entry
│   ├── config/
│   │   ├── ConfigManager.java          # Config system
│   │   └── WieselConfig.java           # Config data class
│   ├── pathfinder/
│   │   ├── PathfinderManager.java      # HTTP client & process management
│   │   ├── PathNode.java               # Path node data
│   │   ├── PathfindResponse.java       # API response
│   │   └── PathWalker.java             # Automatic walking & rotation
│   ├── rendering/
│   │   └── PathRenderer.java           # Path visualization
│   └── mixin/
│       ├── MixinClientPlayerEntity.java
│       └── MixinWorldRenderer.java     # Rendering injection
```

## Building

```bash
./gradlew build
```

Output: `build/libs/wiesel-client-1.0.0.jar`

## Development

```bash
./gradlew runClient
```

## Troubleshooting

**Pathfinder not starting:**
- Ensure `Pathfinding.exe` is in your Downloads folder
- Check logs for errors

**Maps not loading:**
- Ensure map zip files are in Downloads folder
- Maps will be extracted to `.minecraft/maps/`
- Check that map JSON files exist in `.minecraft/maps/{mapname}/`

**Path not rendering:**
- Check that rendering is enabled in config
- Ensure you have a valid path calculated

## License

MIT
