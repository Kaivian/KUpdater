# Development Environment Setup

This document details how to set up your local development environment for building and contributing to **KUpdater**.

---

## Environment Requirements

1. **Java Development Kit (JDK)**: JDK 25.
2. **Build System**: Gradle Wrapper (`./gradlew`) included in the project repository.
3. **IDE**: IntelliJ IDEA (recommended), Eclipse, or VS Code with Java extension pack.

---

## Setting Up JDK 25

Ensure `JAVA_HOME` points to a JDK 25 installation.

### Verification Command

```bash
java -version
```

Output should indicate Java version 25 (e.g. OpenJDK 25).

---

## Building the Project

Run the Gradle build command:

**Linux / macOS:**
```bash
./gradlew build
```

**Windows:**
```powershell
$env:JAVA_HOME="<path-to-jdk-25>"; .\gradlew.bat build
```

The output artifact is generated at `build/libs/KUpdater-0.1.0-SNAPSHOT.jar`.

---

## Running a Test Server

KUpdater uses the `xyz.jpenilla.run-paper` Gradle plugin for easy local testing.

To launch a Paper test server with the plugin automatically loaded:

```bash
./gradlew runServer
```
