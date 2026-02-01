# Technical Explanation: AI Productivity Enhancer

This document details the technical implementation and engineering concepts of the AI Productivity Enhancer application.

## 1. Project Architecture

The application is a native Android app built with Kotlin. The architecture is straightforward, designed around modern Android development practices.

*   **UI Layer:** The entire user interface is built with **Jetpack Compose**, a declarative UI toolkit. This allows for a reactive UI that automatically updates when the underlying state changes. We use `Material3` for the design components.

*   **Activity Structure:** The app uses a simple, multi-activity structure:
    *   `MainActivity`: Handles user authentication via Firebase.
    *   `HomeActivity`: The core of the application, where the user interacts with all the features after logging in.

*   **Data Persistence:** All application state, including the list of blocked apps, user settings, and AI preferences, is stored locally on the device using **`SharedPreferences`**. This provides a simple and efficient way to persist key-value data.

## 2. Core Components Explained

### `HomeActivity.kt`

This is the central hub of user interaction.

*   **State Management:** The UI state (like the list of apps, their blocked status, and the current mode) is managed using Jetpack Compose's state management system (`remember`, `mutableStateOf`, `mutableStateMapOf`). This ensures that any change in the state automatically recomposes the relevant parts of the UI.

*   **App List Generation:** To display the list of user-installed applications, we use the `PackageManager`. We filter out system apps to present a clean and relevant list to the user.

*   **Manual vs. Auto Mode:** A `Switch` composable controls a boolean state variable (`isAutoMode`). The UI conditionally displays either the "Manual Mode" screen (with the full app list) or the "Auto Mode (AI)" screen based on this state.

*   **Accessibility Service Check:** To improve user experience and reduce debugging friction, a proactive check was implemented. A `LaunchedEffect` runs a continuous loop that calls a helper function (`isAccessibilityServiceEnabled`). This function uses the `AccessibilityManager` to verify if our service is active. If it's not, a prominent warning `Card` is displayed at the top of the screen.

### `MyAccessibilityService.kt`

This is the backbone of the app's blocking functionality.

*   **Event-Driven Model:** The service is triggered by accessibility events. Specifically, it listens for `AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED`, which fires every time a new app window appears on the screen.

*   **Blocking Logic:**
    1.  When a new app window is detected, the service extracts its package name.
    2.  It opens the `blocked_apps` `SharedPreferences` file and reads the `blocked_pkg_names` string set. This set is the **single source of truth** for which apps should be blocked.
    3.  It checks if the current app's package name is present in the set.
    4.  If it is, the service executes `performGlobalAction(GLOBAL_ACTION_HOME)`, which immediately sends the user back to the device's home screen, effectively "blocking" the app from being opened.

*   **Debugging:** To aid in diagnosing issues, `Log.d` statements were added to output the current package name and the contents of the block list every time an event occurs.

### `AIBlockingWorker.kt`

This component handles the logic for the "Auto Mode (AI)".

*   **Background Processing:** It's implemented as a `Worker` using Android's **`WorkManager`** library. It is scheduled as a `PeriodicWorkRequest` that runs approximately every hour when Auto Mode is enabled.

*   **Automatic Blocking Logic:**
    1.  The worker retrieves the user-defined usage limits for "Entertainment" and "Gaming" from `SharedPreferences`.
    2.  It queries the `UsageStatsManager` to get the total foreground time for all apps on that day.
    3.  It iterates through hardcoded lists of entertainment and gaming apps.
    4.  If an app's usage exceeds the defined limit, its package name is added to the `blocked_pkg_names` set in `SharedPreferences`.

*   **Learning Mechanism:** The AI includes a simple learning feature. If a user manually unblocks an AI-blocked app, a counter (`unblockCounts`) is incremented. If an app is manually unblocked more than a few times, the AI will "learn" to ignore it and will no longer automatically block it, preventing user frustration.

## 3. Engineering Decisions and Improvements

Throughout our session, several key engineering decisions were made to improve the app:

*   **Refactored Manual Controls:** The initial `Switch` toggles for blocking apps were ambiguous. They were replaced with explicit "Block" and "Unblock" `Button`s. This provides a clearer and more intentional user experience.

*   **Single Source of Truth:** The logic in `MyAccessibilityService` was initially complex and buggy, reading from multiple `SharedPreferences` files. It was refactored to rely on a single source of truth: the `blocked_pkg_names` set. This makes the blocking logic simpler, more robust, and easier to debug.

*   **Proactive Error-Handling:** Instead of passively providing a button to enable the accessibility service, we implemented a prominent, persistent warning message that appears when the service is disabled. This directly informs the user of the critical missing requirement for the app's core feature to function.

*   **Increased Transparency:** For the AI mode, we added a section to the UI that explicitly lists which apps are being categorized as "Entertainment" and "Gaming". This builds user trust by making it clear what the AI is monitoring.
