# V-Launcher RAM Usage Analysis

This document provides a detailed breakdown of the memory footprint of V-Launcher when running on a mobile device under different operational states.

---

## 📊 Estimated RAM Footprint Summary

The table below outlines the estimated memory usage of V-Launcher across different application states:

| State | Estimated RAM Usage | Description |
| :--- | :--- | :--- |
| **Baseline (Idle)** | 45 MB – 70 MB | App is running in the background as launcher, displaying the home screen and monitoring basic system statistics. |
| **Active Home Screen** | 80 MB – 110 MB | Flower grid is loaded, displaying app icons, performing music state polling, and updating the CPU/RAM monitor widget. |
| **Neural AI Chat (Active)** | 120 MB – 180 MB | AI Chat window open, streaming responses from local/remote Ollama instance, holding conversation history. |
| **Vision Analysis (Camera/HUD)** | 160 MB – 240 MB | Real-time camera stream or screen parsing active, invoking OCR or image classification models. |

---

## 🔍 Core Memory Consumers & Leak Vectors

### 1. App Icon Caching (App Drawer & Flower Grid)
* **How it uses RAM:** Each installed app has an icon `Drawable` or `Bitmap`. A typical launcher loads icons in-memory to prevent lag during scrolling. Loading 100+ app icons in high-res format (e.g., 144x144 or 192x192 pixels) can consume 20-40 MB of RAM if caching isn't optimized.
* **Optimization Status:** V-Launcher uses dynamic loading and filters/caps the Flower Grid size (now set to a max of 10 by default) to keep the home screen memory usage extremely low.

### 2. Ollama Client & AI Engine
* **How it uses RAM:** Chat history (`List<ChatMessage>`), HTTP request buffers, and streaming JSON response parsers.
* **Leak Vector:** Retaining long conversation sessions in memory when the UI is hidden.
* **Mitigation:** The launcher should clear chat history or serialize it to disk, and release active network stream connections when the AI panel is closed.

### 3. Vision Analyzer
* **How it uses RAM:** Real-time frames from the CameraX API or Screen Projection. High-resolution YUV/RGBA byte arrays must be processed and garbage collected.
* **Leak Vector:** Failing to release the `ImageProxy` inside the `VisionAnalyzer` callback, or keeping strong references to processed Bitmaps.
* **Mitigation:** Ensure all frames call `.close()` inside a `finally` block and run within bounded coroutine contexts.

### 4. Background Polling Jobs
* **How it uses RAM:** `HomeViewModel` runs several repeating loops:
  - `hubUpdateJob` (System stats monitoring)
  - `musicPollJob` (Media state sync)
* **Leak Vector:** These loops run indefinitely if they aren't cancelled on ViewModel lifecycle destruction (`onCleared`), preventing garbage collection of the entire `HomeViewModel`.
* **Mitigation:** Loops are strictly bound to `viewModelScope`, ensuring they auto-terminate when the ViewModel is destroyed.

---

## 💡 Recommended Memory Optimization Strategy

> [!TIP]
> 1. **Lazy Icon Loading:** Utilize libraries like Coil for loading and caching app drawer icons with automatic memory recycling.
> 2. **Ducking & Release on Background:** Hook into `onTrimMemory()` in the launcher Application/Activity class to release caches, clear unused bitmaps, and stop visual polling jobs when the launcher is not visible.
> 3. **Manual GC Suggestions:** Call `System.gc()` after closing intensive visual or AI components to prompt immediate cleanup of transient bitmap/vision allocations.
