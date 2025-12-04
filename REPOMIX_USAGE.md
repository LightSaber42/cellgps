# Repomix Configuration Usage

This repository includes configuration files for [Repomix](https://github.com/toptal/repomix), a tool that exports your codebase while excluding unnecessary files.

## Configuration Files

- **`.repomixrc`** - JSON format configuration
- **`repomix.config.yaml`** - YAML format configuration (alternative)

Both configurations are set up to:
- ✅ Include all source code (Kotlin, XML, Gradle files)
- ✅ Include documentation (Markdown, HTML)
- ✅ Include scripts (PowerShell)
- ✅ Include configuration examples
- ❌ Exclude build artifacts (`build/`, `.gradle/`, etc.)
- ❌ Exclude sensitive files (keystores, `local.properties`)
- ❌ Exclude IDE files (`.idea/`, `*.iml`)
- ❌ Exclude screenshots and test data
- ❌ Exclude generated files and logs

## Installation

Install Repomix globally:
```bash
npm install -g repomix
```

Or use it directly with npx:
```bash
npx repomix
```

## Usage

### Using the JSON config (`.repomixrc`):
```bash
repomix --config .repomixrc
```

### Using the YAML config:
```bash
repomix --config repomix.config.yaml
```

### Without specifying config (uses `.repomixrc` by default):
```bash
repomix
```

## Output

The exported codebase will be saved to `./repomix-output/` directory by default.

## What Gets Exported

### Included:
- All Kotlin source files (`*.kt`)
- All Gradle build files (`*.gradle.kts`, `*.gradle`)
- Android resources (`*.xml` in `res/`)
- Documentation (`*.md`, `*.html`)
- PowerShell scripts (`*.ps1`)
- Configuration examples (`keystore.properties.example`)
- Gradle wrapper properties

### Excluded:
- Build artifacts (`app/build/`, `build/`)
- Gradle cache (`.gradle/`)
- IDE files (`.idea/`, `*.iml`)
- Sensitive files (keystores, `local.properties`)
- Screenshots and test CSV files
- Generated files and logs
- Binary files (APK, AAB, JAR, etc.)

## Customization

You can modify either configuration file to:
- Add more file patterns to include/exclude
- Change the output path
- Adjust ignore rules

For more information, see the [Repomix documentation](https://github.com/toptal/repomix).
