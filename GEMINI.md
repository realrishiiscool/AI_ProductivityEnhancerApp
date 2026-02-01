## Project Overview

This is an Android application named "AI Productivity Enhancer". The main goal of the app is to help users reduce distractions by monitoring and blocking certain applications. The project is built with Kotlin and uses Jetpack Compose for the user interface.

## Key Technologies

*   **UI:** Jetpack Compose
*   **Authentication:** Firebase Authentication with Google Sign-In
*   **App Usage Tracking:** `UsageStatsManager`
*   **App Blocking:** Accessibility Service
*   **Asynchronous Operations:** Kotlin Coroutines and WorkManager

## Building and Running

### Build

To build the project, run the following command:

```bash
./gradlew build
```

### Assemble a Debug Build

To create a debug APK, run:

```bash
./gradlew assembleDebug
```

The output APK will be located in `app/build/outputs/apk/debug/`.

### Install on a Connected Device/Emulator

To install the debug app on a connected device or a running emulator, use:

```bash
./gradlew installDebug
```

### Running Tests

To run unit tests:

```bash
./gradlew test
```

To run instrumented tests (requires a connected device or emulator):

```bash
./gradlew connectedAndroidTest
```

## Development Conventions

*   The project follows standard Android development conventions.
*   The UI is built with Jetpack Compose.
*   The business logic is separated from the UI.
*   The app uses an Accessibility Service for app blocking, which requires special user permission.

## Project Structure

*   `app/src/main/java/com/example/ai_productivityenhancer/`: Main source code.
    *   `MainActivity.kt`: The entry point of the app, handling user authentication.
    *   `HomeActivity.kt`: The main screen after login, displaying app usage and controls.
    *   `MyAccessibilityService.kt`: An Accessibility Service to block applications.
    *   `AIBlockingWorker.kt`: A WorkManager worker for background tasks related to app blocking.
    *   `AiApi.kt`: A mock API for fetching AI-based rules. This suggests a potential feature for dynamic, AI-driven blocking rules.
*   `app/src/main/AndroidManifest.xml`: Declares all app components like Activities and Services.
*   `build.gradle.kts` & `app/build.gradle.kts`: Gradle build scripts for managing dependencies and build settings.

## Future Improvements (from README.md)

*   Consolidate accessibility services and remove unused files.
*   Complete the accessibility service to block all selected apps.
*   Refine the blocking mechanism.
*   Improve UI/UX, especially for the blocking screen.
*   Enhance error handling.
*   Add new features like blocking schedules, detailed usage statistics, whitelisting, and a "Focus Mode".
