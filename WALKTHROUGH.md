# V-Launcher Premium Edition - Features Walkthrough 🚀

## Overview
V-launcher has been upgraded with a comprehensive suite of premium features, combining a futuristic neon aesthetic with powerful productivity tools inspired by Smart Launcher 6.

## 📱 Phase 1: Core UX & Gestures
New interaction model for fluid navigation.

- **Gestures**:
  - `Double-tap`: Lock screen (requires Admin permission) or custom action
  - `Swipe Up/Down/Left/Right`: Configurable shortcuts
  - `Pinch`: Reveal hidden apps
  - `Long-press`: Access widget/dock settings
  
- **Dock**:
  - Customizable bottom dock
  - Toggle icon labels
  - Context menu management

- **Security**:
  - **PinDialog**: Protects hidden apps
  - **Screen Lock**: Secure locking mechanism

## 🧩 Phase 2: Widget Power Features
Advanced widget management system.

- **Widget Stacking**: Drag widgets onto each other to create a stack. Swipe left/right to switch.
- **Popup Widgets**: Tap an app icon to temporarily show its widget in a floating popup.
- **Smart Widgets**: Define rules to auto-change widgets based on:
  - Time of day (Morning/Night)
  - Weekends vs Weekdays

## 🎨 Phase 3: Visual Customization
Deep visual controls in **Settings -> Visual**.

- **Icon Customization**:
  - **Icon Packs**: Support for ADW, Nova, Go packs
  - **Shapes**: 6 options (Circle, Squircle, Hexagon, etc.)
  - **Sizing**: Slider from Tiny (0.6x) to Huge (1.4x)
  
- **Blur System**:
  - Real-time background blur (RenderEffect on Android 12+)
  - Adjustable intensity slider

- **Adaptive Icons**: Force non-adaptive legacy icons to match your chosen shape.

## 🗂️ Phase 4: Organization & Search
Tools to keep your digital life sorted.

- **Editable Categories**:
  - Create/Rename/Delete categories
  - Auto-classification for new apps
  - Reorder tabs via settings

- **Folders**:
  - **Create**: Drag apps together
  - **Customize**: Rename and set neon color (Cyan, Pink, Purple, etc.)
  - **Grid**: Adjustable folder grid size

- **Universal Search**:
  - Search Apps, **Contacts**, Settings, and Web
  - Quick actions: "Call Mom", "Wifi settings"

## ⚡ Phase 5: Power User Features
For the ultimate control.

- **Multiple Desktops**:
  - Swipe horizontally for unlimited pages
  - **5 Transition Effects**: Slide, Cube, Stack, Fade, Zoom

- **Backup & Restore**:
  - Full JSON backup of layout and settings
  - Restore anytime to switch setups

- **Smart Features**:
  - **Immersive Mode**: Hide system bars for cleaner look
  - **Keep Screen On**: Prevent sleep while on home screen
  - **Battery Info**: Live charging status

## 💎 Recent Refinements
- **Mutable Flower Grid**: Long press apps in the drawer to "Add to Home", or long press grid icons to "Remove from Home".
- **Density Upgrade**: Flower grid now supports up to 61 apps (4 rings) with optimized icon scaling.
- **System Integration**:
  - Full support for system navigation bars (Edge-to-Edge).
  - Native wallpaper picker integration.
  - Removed accidental "Open Settings" gesture.

## 🌌 Phase 6: The Neural Hub
A futuristic "Minus 1" dashboard for system intelligence.
- **Access**: Swipe Right from the home screen.
- **Power Core**: Real-time holographic battery monitor.
- **Pulse Monitor**: Visualizers for RAM and Storage usage.
- **Control**: Quick toggles for system settings.
- **Immersive**: Automatically fades out home screen distractions.

## 🛠️ How to Test
1. **Build & Run** the app.
2. **Swipe Right**: Slide your finger from left to right on the home screen to open the Neural Hub.
3. **Check Vitals**: Verify battery level matches your status bar.
4. **Try the Grid**: Open App Drawer -> Long Press App -> Add to Home.
5. **Change Wallpaper**: Long press Clock -> Visual Settings -> Change Wallpaper.
6. **Check Layout**: Verify the dock buttons are above your phone's navigation bar.

---

## 🔒 Recent Upgrades: Security, Voice, and Performance

### 🛡️ Stealth Onion Drawer & Lock Toggle
- **Stealth Rename**: Removed visible titles containing "onion", "hidden", or "compartment". The Onion Drawer is now disguised as "SYSTEM STORAGE".
- **Lock/Unlock Icon**: Replaced the lock icon in the Onion Drawer header to toggle the layer's protected status.
  - **Locked (Protected)**: Glows in cyan color.
  - **Unlocked (Public)**: Diminished gray color.

### 🗣️ Flexible Speech Recognition Voice Commands
- Natural speech processing added in `HomeViewModel` mapping to:
  - Opening/closing the drawer (*"open drawer"*, *"show apps"*).
  - Launching apps (*"run Chrome"*, *"launch Settings"*).
  - Music playback (*"play some tunes"*, *"stop music"*).
  - Weather details (*"what's the weather"*).
  - Stream volume management (*"volume up"*, *"mute"*).
- Exact app match checks resolve names (e.g. *"calculator"*) directly to the installed package before falling back to local LLM processing.
- Assistant chat provides visual action confirmations.

### ⚡ Performance & Smooth Scrolling Optimizations
- **Asynchronous Icon Loading (`AsyncAppIcon`)**: Uses background coroutines on `Dispatchers.IO` and memory cache fast-path checks (`getIconIfCached`) in `PerformanceOptimizer` to load app icons. This removes heavy system binder calls and bitmap drawing from the main UI thread during scroll/recomposition.
- **Lightweight Folder Icon Previews**: Replaced nested `LazyVerticalGrid` inside folder icons with lightweight static `Column`/`Row` templates.
- **Cache-Optimized Filtering**: Wrapped folder and app filtering logic inside `remember` blocks in `AppDrawer` to prevent CPU allocations and GC pressure during scrolling.
- **Unique Recycler Keys**: Assigned unique keys to grid item containers (`key = "folder_$name"`) to maximize Compose grid recycling performance.
