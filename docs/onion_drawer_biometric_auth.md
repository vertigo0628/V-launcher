# V-Launcher Onion Hidden Drawer & Biometric Authentication

This document details the architecture, features, and layout details for the multi-layered **Onion Hidden Drawer** and **Biometric (Fingerprint/Credential) Authentication** layers in V-Launcher.

---

## 1. Multi-Layered "Onion" Compartments
Instead of a single hidden apps list, V-Launcher organizes hidden apps into sequential layers (Compartments) resembling an onion:
- **Layer 0**: The base level containing the default hidden apps.
- **Layer 1, 2, ... N**: Custom sub-compartments created dynamically by the user.

### Peeling Navigation Gestures
- **Double-Tap (Empty Space on Desktop)**: Initiates entrance to the Onion Drawer.
- **Double-Tap (Empty Space in Compartment)**: Dives ("peels") deeper into the next compartment level. If at the deepest level, it prompts to create a new compartment.
- **Back Button / Arrow**: Navigates outward to the shallower compartment level. Back-pressing from Layer 0 exits the drawer.

---

## 2. Biometric & PIN Security

### Auto-Trigger & Fallback
- Entering any **Protected/Locked** compartment automatically checks system biometric capability (Weak Biometric or Device Credentials).
- If supported and a PIN is configured, the system biometric dialog pops up immediately.
- If cancelled, dismissed, or biometrics are unavailable, the manual custom PIN dialog is shown as fallback.
- A manual **"Fingerprint"** button is placed on the custom PIN dialog to re-trigger biometrics at any time.

### Per-Compartment Optional Security
Every layer can be configured to be either **Public (Unlocked)** or **Secure (Locked)**:
1. **Creation Choice**: The "New Compartment" dialog includes a toggle switch to create it as secure or public.
2. **On-the-Fly Toggle**: Tapping the lock/unlock icon (**🔒/🔓**) in a compartment's header instantly toggles its protection state.
3. **No-Friction Navigation**: Transitioning into public compartments does not prompt for authentication, allowing quick access to less sensitive items.

---

## 3. Screen Layout & System UI Safeness
To prevent overlays from overlapping with system elements (such as network/wifi/battery status, screen notches, or navigation buttons):
- **Full-Screen Canvas**: The root Box matches the full screen to draw a seamless dark background color (`0xFF0B0F19`).
- **Safe Area Insets**: The content column uses Jetpack Compose `.windowInsetsPadding(WindowInsets.safeDrawing)`. This dynamically computes the safe rendering space across all device cutouts, aspect ratios, and screen orientations.

---

## 4. Storage & State Management
- **`PreferencesManager`**:
  - `hidden_layers`: JSON mapping layer names to their set of package names. Empty layers are preserved.
  - `protected_onion_layers`: String set storing names of layers requiring PIN verification (Layer 0 defaults to `"__base__"`).
- **`HomeViewModel`**:
  - `hiddenLayers`: StateFlow of the existing layers.
  - `protectedLayers`: StateFlow of the protected layer keys.
  - CRUD operations: `createHiddenLayer(name, isProtected)`, `deleteHiddenLayer(name)`, `setOnionLayerProtected(depth, isProtected)`.
