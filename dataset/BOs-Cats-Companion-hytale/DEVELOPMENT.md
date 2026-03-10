# Development Guide 🛠️🚀

This guide is for developers who want to build, modify, or contribute to the Cats plugin.

## Prerequisites

- Valid Hytale installation
- Java 21 or higher
- Gradle 8.x or higher

## Setup

1. Clone this repository:
   ```bash
   git clone https://github.com/MarkusBordihn/BOs-Cats-Companion.git
   cd BOs-Cats-Companion/BOs-Cats-Companion-Hytale
   ```

2. Configure paths in `gradle.properties`:
   ```properties
   hytaleServerJar=/path/to/HytaleServer.jar
   hytaleHome=/path/to/Hytale/install
   ```

3. (Optional) Extract Hytale API for development:
   ```bash
   ./gradlew decompile
   ```
   **Note:** This extracts Hytale's API and resources locally for reference. Output is not included
   in the repository.

4. Build the plugin:
   ```bash
   ./gradlew build
   ```

## Available Gradle Tasks

- `./gradlew build` - Build the plugin JAR
- `./gradlew decompile` - Extract Hytale API and resources for development (optional)
- `./gradlew verifyHytaleInstallation` - Verify Hytale installation paths
- `./gradlew extractHytaleApi` - Extract only API classes
- `./gradlew extractHytaleResources` - Extract only resource files

## Development Notes

- The `decompile` task is **optional** and only needed for API exploration
- Decompiled files are automatically excluded from git (`.gitignore`)
- Use your IDE's decompiler or the extracted sources for API reference
- All modifications to your plugin code in `src/` are tracked by git

## Project Structure

```
BOs-Cats-Companion-Hytale/
├── src/main/
│   ├── java/                 # Java source code
│   │   └── de/markusbordihn/cats/
│   └── resources/            # Game resources (models, configs, etc.)
│       ├── Server/           # Server-side resources
│       │   ├── NPC/         # Cat NPC definitions and roles
│       │   └── Languages/   # Translation files
│       └── Common/          # Client & Server resources
├── gradle/                   # Gradle configuration
│   ├── hytale-config.gradle # Hytale-specific settings
│   └── hytale-tasks.gradle  # Custom Gradle tasks
├── build.gradle             # Main build configuration
└── gradle.properties        # Project properties and paths
```

## Building the Plugin

### Standard Build

```bash
./gradlew build
```

The compiled JAR will be in `build/libs/`.

### Clean Build

```bash
./gradlew clean build
```

## Testing

1. Open the project in IntelliJ IDEA
2. Start the **HytaleServer** run configuration (auto-generated from `build.gradle`)
3. Authenticate when prompted by the server
4. Connect with your Hytale client to `localhost`
5. Test your changes in-game

**Note:** The plugin source files in `src/main/` are automatically loaded via the `--mods` parameter
in the IntelliJ run configuration. No need to copy JAR files manually during development.

## License

This project is open source under the MIT License (source code only).
Assets (models, textures, sounds) are excluded from the license.

See [LICENSE.md](LICENSE.md) for full details.
