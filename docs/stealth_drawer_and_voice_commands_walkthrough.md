# Stealth Onion Drawer, Lock Toggle, and Voice Commands Walkthrough

This walkthrough details the changes made to improve privacy, restore the lock/unlock toggle button, and add a flexible voice command system in V-launcher.

## Changes Made

### 1. Header and Icon Stealth Renames
- Removed user-facing texts containing suspicious keywords such as "hidden", "onion", and "compartment" in favor of stealthy system storage diagnostics terminology:
  - **Title**: Renamed to "SYSTEM STORAGE".
  - **New Instance / Compartment**: Renamed dialog and labels to "System Instance" and "Instance key".
  - **Biometric Prompt**: Changed titles to "System Authentication" / "Confirm device ownership to proceed".
- Removed visible drawer arrows and "hidden apps" indicators to maintain the illusion of a standard system utility drawer.

### 2. Restoring the Lock/Unlock Toggle
- Replaced the lock/unlock icon button in the top-right corner of the Onion Drawer's header inside `HomeScreen.kt`.
- Clicking the lock toggles the layer's protected status via the ViewModel:
  - **Locked (Protected)**: Highlights the icon with the theme accent color and a glowing cyan circular background.
  - **Unlocked (Public)**: Diminishes the icon with a gray color and standard translucent background.

### 3. Flexible Voice Commands (Speech Recognition)
We redesigned the voice command execution mechanism in [HomeViewModel.kt](file:///home/vertigo/AndroidStudioProjects/V-launcher/app/src/main/java/com/vertigo/launcher/ui/HomeViewModel.kt#L1817-L1987) to support natural, flexible wording and user feedback inside the assistant chat window:
- **App Drawer Navigation**: Open/close the app drawer with phrases like *"open drawer"*, *"show apps"*, *"close drawer"*, *"go home"*.
- **Flexible Play Music**: Detects variations containing play actions and music nouns (e.g. *"play some tunes"*, *"start music"*, *"play a song"*).
- **Weather Details**: Recognizes queries about weather/temperature/forecast (e.g. *"what's the weather"*, *"weather forecast"*) and outputs the current status directly to the assistant chat.
- **Volume Adjustments**: Adjusts stream volume directly using system services with commands like *"volume up"*, *"decrease volume"*, *"mute"*, and *"unmute"*.
- **Settings Shortcuts**: Opens the launcher settings (*"launcher settings"*), Wi-Fi settings (*"open wifi"*), Bluetooth settings (*"open bluetooth"*), or standard settings.
- **Flexible App Launching**: Supports verbs like *"launch [app]"*, *"run [app]"*, *"go to [app]"*, *"start [app]*.
- **Exact App Match Detection**: If a standalone name is spoken (e.g., *"chrome"*, *"settings"*, *"calculator"*), it automatically checks installed apps for a match and launches it directly, only falling back to the Local LLM if no matching app exists.
- **Visual Chat Feedback**: All matching actions append a confirmation message to the assistant chat view (e.g. *"Launching Chrome..."*, *"Muting audio."*).

## Verification

- **Build Output**: Successfully compiled using Gradle (`BUILD SUCCESSFUL` inside `./gradlew compileDebugKotlin`).

---

## Performance & Smooth Scrolling Optimizations

To resolve scrolling and selection lag, we eliminated synchronous Android system binder transactions and draw-to-bitmap conversions on the main UI thread.

### 1. Asynchronous Icon Loading (`AsyncAppIcon`)
- Created a new, reusable `AsyncAppIcon` Composable.
- Integrated a fast-path cache checker `getIconIfCached` into [PerformanceOptimizer.kt](file:///home/vertigo/AndroidStudioProjects/V-launcher/app/src/main/java/com/vertigo/launcher/utils/PerformanceOptimizer.kt) to retrieve icons instantly from memory.
- Uses `LaunchedEffect` and `withContext(Dispatchers.IO)` to offload all heavy package icon resolutions to a background thread.
- Applied `AsyncAppIcon` across the entire application interface in [HomeScreen.kt](file:///home/vertigo/AndroidStudioProjects/V-launcher/app/src/main/java/com/vertigo/launcher/compose/HomeScreen.kt):
  - **AppIcon**: Replaced synchronous `toBitmap().asImageBitmap()` blocks.
  - **Main Drawer Grid**: Replaced inline `AndroidView` package manager loads.
  - **Hidden Drawer**: Replaced synchronous image views.
  - **Folder Details Overlay**: Upgraded the grid to load icons asynchronously.

### 2. Layout Simplification inside Folder Preview
- Removed nested `LazyVerticalGrid` inside the `FolderIcon` preview. 
- Replaced it with a lightweight static `Column` and `Row` grid that fits 4 mini icons rendered with `AsyncAppIcon`. This reduces layout pass overhead and compose node tree weight significantly.

### 3. Allocation and Calculation Caching
- Caged the app list filtering operations (`appsInFolders`, `filteredApps`, and a mapped `folderAppsMap`) inside `remember` blocks inside `AppDrawer` to prevent costly recalculations and GC pressure on every scroll recomposition frame.
- Set explicit, unique item keys (`key = "folder_$name"`) on folders to enable optimal recycling of Composables in the lazy grid.

### 4. Backgrounding Shortcut Queries
- Offloaded Android LauncherApps deep shortcuts lookup and shortcut drawable retrieval inside `HomeViewModel.loadShortcutsForApp` to a background thread using `kotlinx.coroutines.Dispatchers.IO`.

---

## Dynamic Island Floating Mini Terminal Overlay

We implemented a powerful, overlay-based voice and text assistant that lets users communicate with Sunday (via the local Ollama LLM) from any screen or application, without having to return to the launcher homescreen:

### 1. Collapsed Floating Bubble
- Draggable pill-shaped floating bubble containing a microphone icon that overlays on top of other apps.
- Features smooth edge-snapping physics (snapping to left/right margins) and persists the user's preferred position to shared preferences (`fab_pos_x`, `fab_pos_y`).
- Fades to a semi-transparent state (35% alpha) after 5 seconds of inactivity to minimize distraction.

### 2. Expandable Cyberpunk Terminal (Dynamic Island Style)
- Tapping the floating bubble triggers a scale/fade animation, expanding into a sleek, dark-blue holographic terminal (`0xF00F172A`).
- Features a terminal green (`v@terminal:~$ `) query log and styled text fields matching the launcher's cyberpunk aesthetics.
- Incorporates live AI text streaming from the local Ollama LLM (`OllamaClient`), complete with clean `<think>` tag stripping and automatic scrolling.
- Includes a dedicated text input field (with keyboard action support) alongside dynamic button states (Send, Stop generation, and Mic).

### 3. Integrated Voice Input
- Built-in `SpeechRecognizer` activation directly from the floating overlay.
- Visual status indicator tracks states ("Ready", "Listening...", "Hearing...", "Thinking...", "Mic error").
- Microphone button pulses and highlights green while listening.

### 4. Toggle Setting
- Added a "Floating Assistant Button" switch to `root_preferences.xml` under the AI & Voice category.
- When toggled, checks if the launcher's accessibility service is active. If not, prompts the user directly to system accessibility settings.

## Verification
- **Build Output**: Successfully compiled debug build (`BUILD SUCCESSFUL` in `./gradlew assembleDebug` in 3m 5s).
- **Aesthetic Design**: Uses programmatic CSS-equivalent styling (custom fonts, shape drawables, animated fades, and layout constraints) to deliver a premium, theme-accurate UI.
