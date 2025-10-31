# GitHub Copilot Instructions for mc-telemetry-core

## Project Overview

mc-telemetry-core is a Minecraft mod that integrates OpenTelemetry for telemetry and observability. This project uses Architectury to support multiple mod loaders (Fabric and NeoForge) with a common codebase.

**Key Feature**: Unlike existing telemetry mods for Minecraft, this mod allows players to add world-specific instrumentation during their play sessions. For example, players can place observer blocks that track Redstone signal strength and expose it as configurable metrics. As a consequence, the registered instruments and exported metrics are dynamic and not static during runtime.

## Tech Stack

- **Language**: Kotlin 2.2.20 with Java 21
- **Build Tool**: Gradle 8.14
- **Minecraft Version**: 1.21.1
- **Mod Loaders**: Fabric and NeoForge via Architectury
- **Testing**: Minecraft GameTest framework
- **Telemetry**: OpenTelemetry SDK 1.51.0

## Project Structure

```
mc-telemetry-core/
├── common/              # Shared code for all mod loaders
│   └── src/main/kotlin/ # Common implementation
├── fabric/              # Fabric-specific implementation
│   └── src/main/kotlin/
├── neoforge/            # NeoForge-specific implementation
│   └── src/main/kotlin/
└── .github/
    └── workflows/       # CI/CD workflows
```

## Build Instructions

### Prerequisites
- Java 21 (JDK)
- Gradle (via wrapper)

### Build Commands

```bash
# Build common module
./gradlew :common:build

# Build specific platform
./gradlew :fabric:build
./gradlew :neoforge:build

# Run checks (includes GameTests)
./gradlew :fabric:check
./gradlew :neoforge:check

# Build all
./gradlew build
```

## Testing

This project uses Minecraft's GameTest framework for testing. Tests are located in:
- `common/src/main/kotlin/de/mctelemetry/core/gametest/`
- Platform-specific test extensions in each loader's directory

GameTest logs are output to `{platform}/gameTestRun/logs/` directory.

## Code Style

- **Kotlin Code Style**: Official Kotlin style guide
- **JVM Toolchain**: Java 21
- **Encoding**: UTF-8
- **Kotlin Compiler Options**: Uses context parameters (`-Xcontext-parameters`)

## Development Guidelines

### Package Structure
- Base package: `de.mctelemetry.core`
- API package: `de.mctelemetry.core.api`
- Platform-specific: `de.mctelemetry.core.{fabric|neoforge}`

### Dependencies
- Use `modApi` for Architectury API
- Use `modImplementation` for loader-specific dependencies
- Keep OpenTelemetry dependencies in `compileOnly` scope where appropriate
- Dependencies (except OpenTelemetry API) are shadowed/relocated to `de.mctelemetry.core.shadow` prefix
- **Important**: The OpenTelemetry API must NOT be relocated to maintain compatibility with SDK injection from the OpenTelemetry Java agent

### Mod Metadata
- Mod ID: `mcotelcore`
- Maven Group: `de.mctelemetry`
- Archives Base Name: `mc-telemetry-core`

## CI/CD

The project uses GitHub Actions with the following workflows:

### Build Workflow (`gradle.yml`)
1. Build common module first
2. Build and test each platform (fabric, neoforge) in parallel
3. Upload GameTest logs and worlds on failure
4. Upload build artifacts on release branches

### Copilot Setup Steps Workflow (`copilot-setup-steps.yml`)
- Automatically runs when Copilot starts a session
- Downloads Gradle dependencies for all modules
- Pre-downloads OpenTelemetry agent
- Ensures faster builds during Copilot coding sessions

## Important Notes

- Always use the Gradle wrapper (`./gradlew`) for consistency
- Platform-specific code should go in respective platform directories
- Shared code belongs in the `common` module
- GameTests run as part of the `check` task
- The project uses MIT License
- Shadow/relocate dependencies to avoid conflicts with other mods

## When Making Changes

1. **Adding Features**: 
   - Start with common implementation
   - Add platform-specific implementations as needed
   - Write GameTests to verify functionality

2. **Dependencies**:
   - Check if dependency exists in common module first
   - Add platform-specific dependencies only when necessary
   - Update version properties in `gradle.properties`

3. **Testing**:
   - Run `./gradlew :fabric:check` and `./gradlew :neoforge:check` before committing
   - Check GameTest logs if tests fail
   - Ensure both platforms work correctly

4. **Code Organization**:
   - Follow existing package structure
   - Keep API separate from implementation
   - Use Architectury's platform abstraction where possible
