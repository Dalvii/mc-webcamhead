# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Fabric mod for Minecraft 1.21.3 called "Webcam Head" that displays webcam video on player heads in-game. The mod captures webcam feed using JavaCV (OpenCV) and renders it as a 3D panel above the player's head using OpenGL. The project uses Fabric Loom for building and follows the standard Fabric mod structure with split source sets for common and client-only code.

### Features
- Real-time webcam capture and rendering
- 3D billboard panel above player's head
- Toggle webcam on/off with keybinding (default: V key)
- Multiple camera support with device selection
- Asynchronous webcam startup (non-blocking)
- Configurable resolution and FPS
- Local rendering (P2P networking to be implemented later)

### In-Game Commands
- `/webcam list` - List all available webcam devices
- `/webcam device <index>` - Select a specific camera (0, 1, 2, etc.)
- `/webcam info` - Show current webcam configuration

## Build System

**Build the mod:**
```bash
./gradlew build
```

**Run Minecraft client with the mod:**
```bash
./gradlew runClient
```

**Run data generation:**
```bash
./gradlew runDatagen
```

**Clean build artifacts:**
```bash
./gradlew clean
```

## Hot Reload Setup (Development Mode)

The project is configured for hot reload using Java's Enhanced Class Redefinition feature. This allows you to modify code while the game is running and apply changes without restarting.

### Prerequisites
- Java 21 or higher
- IntelliJ IDEA (recommended)

### Setup in IntelliJ IDEA

1. **Generate run configurations:**
   ```bash
   ./gradlew genSources
   ```

2. **Configure the run configuration:**
   - Open Run → Edit Configurations
   - Select the "Minecraft Client" run configuration
   - Verify that the VM option `-XX:+AllowEnhancedClassRedefinition` is present (already configured in build.gradle)

3. **Start the game in debug mode:**
   - Click the debug button (bug icon) instead of the regular run button
   - This enables hot swapping

4. **Apply code changes:**
   - Make changes to your Java code
   - Build the project: `Cmd+F9` (macOS) or `Ctrl+F9` (Windows/Linux)
   - IntelliJ will automatically hot swap the modified classes
   - Changes take effect immediately in the running game

### Limitations
- **Works with:** Method body changes, adding/modifying fields, adding methods
- **Doesn't work with:** Adding/removing classes, changing class hierarchy, modifying method signatures, changes to mixins
- For changes that can't be hot swapped, you'll need to restart the game

### Troubleshooting
If hot reload doesn't work:
- Ensure you're running in debug mode (not regular run mode)
- Check that Java 21+ is being used
- Verify the VM argument is present in the run configuration
- Some changes (like mixin modifications) always require a full restart

## Project Structure

### Source Organization
The project uses Fabric Loom's split environment source sets:
- `src/main/java/` - Common code that runs on both client and server
- `src/client/java/` - Client-only code
- `src/main/resources/` - Common resources
- `src/client/resources/` - Client-only resources

Both source sets are configured under the same mod ID "webcamhead" in `build.gradle`.

### Key Entry Points
- **Main entry point:** `com.dalvi.webcamhead.Webcamhead` - Implements `ModInitializer`
- **Client entry point:** `com.dalvi.webcamhead.client.WebcamheadClient` - Implements `ClientModInitializer`
- **Data generation:** `com.dalvi.webcamhead.client.WebcamheadDataGenerator` - Implements `DataGeneratorEntrypoint`

### Mixin Configuration
Two mixin configuration files:
- `webcamhead.mixins.json` - Common mixins (package: `com.dalvi.webcamhead.mixin`)
- `webcamhead.client.mixins.json` - Client-only mixins (package: `com.dalvi.webcamhead.mixin.client`)

Both are configured for Java 21 compatibility with strict annotation requirements.

## Technical Requirements

- **Java Version:** 21
- **Minecraft Version:** 1.21.3
- **Fabric Loader:** 0.17.3
- **Yarn Mappings:** 1.21.3+build.2
- **Fabric API:** 0.114.1+1.21.3

## Package Structure

Base package: `com.dalvi.webcamhead`
- Main mod code in base package
- Client code in `.client` subpackage
- Mixins in `.mixin` (common) and `.mixin.client` (client-only) subpackages

### Client Package Structure
```
com.dalvi.webcamhead.client/
├── WebcamheadClient.java - Main client initialization and keybinding
├── webcam/
│   ├── WebcamManager.java - Webcam capture using JavaCV
│   └── WebcamTextureManager.java - OpenGL texture management
├── video/
│   ├── PlayerVideoState.java - Per-player video state
│   └── VideoStateManager.java - Global video state manager
├── render/
│   └── VideoPanelRenderer.java - 3D panel rendering
└── config/
    └── ModConfig.java - Configuration (resolution, FPS, render mode)

com.dalvi.webcamhead.mixin.client/
└── PlayerEntityRendererMixin.java - Injects video rendering into player render
```

## Dependencies

- **JavaCV 1.5.10** - Webcam capture (OpenCV wrapper)
- **OpenCV 4.9.0** - Computer vision library for frame processing
- Native binaries included for macOS ARM64 (add other platforms as needed)

## How It Works

1. **Webcam Capture**: `WebcamManager` uses JavaCV to capture frames from the default webcam in a background thread
2. **Texture Upload**: `WebcamTextureManager` converts BufferedImage frames to NativeImage and uploads to GPU as OpenGL textures
3. **State Management**: `VideoStateManager` tracks which players have active video feeds
4. **Rendering**: `PlayerEntityRendererMixin` hooks into player rendering and calls `VideoPanelRenderer` to draw a textured quad above the player's head
5. **Billboard Effect**: The panel always faces the camera for better visibility

## Development Notes

When adding new mixins, place them in the appropriate package and register them in the corresponding JSON configuration file (`mixins` array for common, `client` array for client-only).

The webcam runs in a separate thread to avoid blocking the game loop. Frame updates happen every client tick in `WebcamheadClient.onClientTick()`.

### Performance Considerations
- Default resolution is 320x240 @ 15fps for balance between quality and performance
- Texture upload happens on the main thread but should be fast enough for real-time updates
- JavaCV natives are ~50MB, included in the mod JAR

### Future Work
- WebRTC integration for P2P video streaming between players
- Skin overlay mode (render video on player face texture)
- Server-side signaling for WebRTC connections
- Privacy controls (whitelist/blacklist players)
