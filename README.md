# 🌌 V-Launcher

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white&style=for-the-badge)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-7F52FF?logo=kotlin&logoColor=white&style=for-the-badge)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Modern_UI-4285F4?logo=android&logoColor=white&style=for-the-badge)](https://developer.android.com/jetpack/compose)

A premium, highly customizable Android launcher featuring a futuristic **Cyberpunk Neon aesthetic**, state-of-the-art gesture engines, and a **locally hosted AI Assistant ("Sunday")** powered by Ollama on Termux. 

Designed for power users who demand strict  privacy, system-wide ambient assistance, and a fluid, lag-free experience.

---

## 🚀 Key Features

### 🧠 1. The Neural Hub (Minus-One Screen)
Access a futuristic control deck by swiping right from the home screen:
*   **Holographic Vitals**: Real-time interactive CPU, RAM, and storage visualizers.
*   **Power Core**: Dynamic circular battery monitor with custom charging status animations.
*   **Quick Toggles**: Fast access to system settings (Wi-Fi, Bluetooth, Location, etc.) styled as holographic terminals.

### 🕵️‍♂️ 2. Stealth "SYSTEM STORAGE" (The Onion Drawer)
A covert vault for apps requiring absolute privacy:
*   **Clever Disguise**: Masked completely as a system diagnostic screen titled "SYSTEM STORAGE". All labels referencing "hidden", "private", or "compartment" are fully redacted.
*   **Secure Authentication**: Protected by device biometrics (Fingerprint/Face) or custom PIN layouts.
*   **Lock/Unlock Toggle**: A glowing cybernetic lock icon toggles visibility. When locked, hidden apps are completely invisible and inaccessible to standard search queries.

### 🗣️ 3. Natural Voice Command Parser
Control your device using natural spoken phrasing, with parity across both the launcher and the floating terminal:
*   **App Navigation**: *"open drawer"*, *"show apps"*, *"hide apps"*, *"go home"*
*   **App Launching**: *"run Chrome"*, *"launch Settings"*, *"start Spotify"* (Exact matches resolved locally first)
*   **System Controls**: *"volume up"*, *"decrease volume"*, *"mute"*, *"unmute"*
*   **Shortcuts**: *"open wifi"*, *"open bluetooth"*, *"launcher settings"*
*   **Ambient Queries**: *"what's the weather?"*, *"weather forecast"*

### 🏝️ 4. Dynamic Island Floating Terminal Overlay
A system-wide AI terminal overlay that floats on top of other applications, enabling prompt assistant access without returning to the homescreen:
*   **Edge-Snapping Physics**: A draggable microphone bubble that snaps to screen edges and persists position across reboots.
*   **Idle Fading**: Automatically dims to 35% opacity after 5 seconds of inactivity.
*   **Cyberpunk Terminal UI**: Expands into a dark-blue holographic terminal (`0xF00F172A`) with green command-line prompts.
*   **Live Text Streaming**: Direct integrations with local `OllamaClient` showcasing live response generation and `<think>` block parsing.
*   **Quick Clear (`🗑`)**: Instant button to wipe chat history, cancel active AI generations, and reset speech recognition state.

### 📁 5. File Hunter (Shizuku-Powered File Manager)
A powerful, integrated File Manager designed to uncover hidden media vaults and browse restricted application data without requiring root access.
*   **Shizuku Integration:** Bypasses Android's Scoped Storage limitations, allowing you to browse, manage, and share restricted internal app folders like `/Android/data`.
*   **Deep Vault Scanner:** Instantly searches storage for folders containing `.nomedia` files, immediately aggregating hidden vaults into an easy-to-navigate list.
*   **Media Hunter:** Automatically scans "disguised" cache files (like `.bin` or `.tmp`) and reads their binary Hex headers. If it detects a hidden JPEG photo (`FF D8 FF`), it tags it and provides a visual thumbnail!
*   **Multi-Select & Batch Actions:** Advanced multi-select mode to Batch Delete, Share, Copy, and Move files seamlessly.
*   **Expert Mode Safety Dots:** Visual indicators (🟢 Safe, 🟡 Caution, ⚪ Unknown) help identify which system cache files are safe to delete and which might affect app settings.

### 🎨 6. Premium Layout & Customization
*   **Widget Stacking**: Drag and drop widgets on top of each other to stack them. Swipe horizontally to cycle.
*   **Popup Widgets**: Double-tap or swipe on app icons to temporarily reveal their corresponding widget.
*   **Smart Widgets**: Rules-based widgets that transition automatically depending on the time of day or weekend schedule.
*   **Custom Shapes & Packs**: Broad support for icon packs with custom shaping algorithms (Squircle, Hexagon, Circle) and RenderEffect-based background blurs.

---

## ⚡ Performance Optimization & Scrolling Architecture
To maintain a locked 60/120 FPS interface, V-Launcher segregates blocking operations from the main UI thread:
*   **`AsyncAppIcon` Loader**: Employs coroutines on `Dispatchers.IO` to load package icons, avoiding heavy Android system binder calls during scroll passes.
*   **Memory Fast-Path Cache**: Uses `PerformanceOptimizer` to retrieve cached drawable bitmaps instantaneously.
*   **Recomposition Caching**: Wraps complex lists and filtered apps inside `remember` blocks to minimize CPU garbage collection spikes.
*   **Static Grid Previews**: Folder previews render with static rows/columns rather than nested lazy-grids, lowering layout pass overhead.

---

## 🛠️ Getting Started & Setup

### Prerequisites
*   Android SDK 33+
*   Kotlin 1.9.0+
*   Gradle 8.0+

### Step 1: Clone and Build
```bash
git clone https://github.com/vertigo0628/V-launcher.git
cd V-launcher
# Build Debug APK
./gradlew assembleDebug
```

### Step 2: Set Up Sunday (Local Ollama LLM) on Android via Termux
To run the AI Assistant entirely offline and locally on your phone:
1. Install [Termux](https://termux.dev/) on your device.
2. Update packages and install Ollama:
   ```bash
   pkg update && pkg upgrade
   pkg install ollama
   ```
3. Pull your preferred model (e.g., `llama3` or `qwen2.5:0.5b` for resource efficiency):
   ```bash
   ollama run qwen2.5:0.5b
   ```
4. Within **V-Launcher Settings** -> **AI & Voice**:
   * Set your preferred Ollama server address (typically `http://localhost:11434` or Termux IP).
   * Enter the exact model name under **Model Selection**.

> [!NOTE]
> V-Launcher manages the Termux lifecycle autonomously. When starting, it executes a clean pre-kill script (`pkill -9 -f ollama`) via Termux RUN_COMMAND permission to prevent socket conflicts before initializing the fresh server.

### Step 3: Enable Accessibility Service
For the **Floating Assistant Overlay** to draw over other apps:
1. Navigate to **V-Launcher Settings** -> **AI & Voice**.
2. Toggle **Floating Assistant Button**.
3. If prompt appears, allow **V-Launcher Accessibility Service** in system settings.

---

## 📂 Project Structure
```
V-launcher/
│
├── app/src/main/java/com/vertigo/launcher/
│   ├── compose/                # Jetpack Compose Screens (HomeScreen, InlineSearch)
│   ├── logic/                  # App categorization and local command execution
│   ├── model/                  # Data structures (App models, widgets, layout configs)
│   ├── service/                # FloatingTerminalOverlay & AccessibilityService
│   ├── ui/                     # HomeViewModel & UI State Management
│   └── utils/                  # Async loaders, biometrics, settings managers
│
└── docs/                       # Technical Walkthroughs & Design Documentation
```

---

## 🤝 Contributing
Contributions are welcome! Please create feature branches pointing to target changes:
```bash
git checkout -b feature/cool-new-effect
# Staging and committing
git add .
git commit -m "feat: add cyber-glitch transition effect"
git push origin feature/cool-new-effect
```

---

## 📄 License
This project is licensed under the Apache License 2.0. See [LICENSE](LICENSE) for details.
