# Sonar Complexity Plugin for WebStorm & IntelliJ IDEA

A WebStorm plugin inspired by the popular [sonar-complexity](https://github.com/kevinjshah2207/sonar-complexity) VS Code extension.

It calculates and displays **SonarQube Cognitive Complexity (S3776)** and **Cyclomatic Complexity** metrics directly in your WebStorm editor window.

---

## Features

- **Inline Inlay Hints**: Renders complexity scores directly above function declarations (`Cognitive Complexity: X`).
- **Color-coded Badges**:
  - 🟢 **Low Complexity**: Below warning threshold (< 15)
  - 🟡 **Moderate Complexity**: Reached warning threshold (15 - 24)
  - 🔴 **High Complexity**: Reached critical threshold (≥ 25)
- **Local Inspections**: Flags functions exceeding complexity thresholds as code smells in WebStorm's **Problems** window.
- **Customizable Preferences**: Configure threshold values, toggle inlay hints, and switch metric displays under `Settings -> Tools -> Sonar Complexity`.

---

## Building & Running Locally

### Prerequisites
- JDK 17+
- Gradle 8+ (or use wrapper)

### Build the Plugin Zip
```bash
./gradlew buildPlugin
```
The compiled plugin archive will be generated at:
`build/distributions/sonar-complexity-webstorm-1.0.0.zip`

### Run in WebStorm IDE Sandbox
To launch a live WebStorm sandbox instance with the plugin installed:
```bash
./gradlew runIde
```

---

## License
MIT
