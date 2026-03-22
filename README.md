<p align="center">
  <img src="https://raw.githubusercontent.com/Pankaj-Meharchandani/Waller/master/app/src/main/ic_launcher-playstore.png" width="100" />
</p>

<h1 align="center">Waller</h1>

<p align="center">
  <strong>Beautiful gradient wallpapers for Android — generated, previewed, and applied in seconds.</strong>
</p>

<p align="center">
  <a href="https://github.com/Pankaj-Meharchandani/Waller/releases/latest">
    <img src="https://img.shields.io/github/v/release/Pankaj-Meharchandani/Waller?style=for-the-badge&logo=github&label=Latest%20Release&color=6650a4" />
  </a>
  &nbsp;
  <a href="https://github.com/Pankaj-Meharchandani/Waller/releases">
    <img src="https://img.shields.io/github/v/release/Pankaj-Meharchandani/Waller?include_prereleases&style=for-the-badge&logo=github&label=Beta&color=7D5260" />
  </a>
  &nbsp;
  <a href="https://github.com/Pankaj-Meharchandani/Waller/blob/main/LICENSE">
    <img src="https://img.shields.io/github/license/Pankaj-Meharchandani/Waller?style=for-the-badge&color=625b71" />
  </a>
  &nbsp;
  <img src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
</p>

<p align="center">
  <a href="https://t.me/Waller_app">
    <img src="https://img.shields.io/badge/Telegram-Channel-26A5E4?style=for-the-badge&logo=telegram&logoColor=white" />
  </a>
  &nbsp;
  <a href="https://t.me/walllller">
    <img src="https://img.shields.io/badge/Telegram-Support%20Group-26A5E4?style=for-the-badge&logo=telegram&logoColor=white" />
  </a>
</p>

<br />

<p align="center">
  <img src="https://github.com/user-attachments/assets/bcf204df-6e09-4703-83cd-5894077f2150" width="15%" />
  <img src="https://github.com/user-attachments/assets/26c4ced6-31f0-4d41-a1c1-fcd55c3c684c" width="15%" />
  <img src="https://github.com/user-attachments/assets/0630110d-3081-4a6d-8566-128f1125c3c5" width="15%" />
  <img src="https://github.com/user-attachments/assets/f315c1db-a85f-400e-adeb-fdb896173da3" width="15%" />
  <img src="https://github.com/user-attachments/assets/4a07d64e-e58e-42e8-a45f-804b87535fc7" width="15%" />
</p>
<p align="center">
  <img src="https://github.com/user-attachments/assets/f92c5adb-550c-491f-937d-a6b7653a2778" width="15%" />
  <img src="https://github.com/user-attachments/assets/28b2f110-d9db-4393-bd3e-ba33eac62aae" width="15%" />
  <img src="https://github.com/user-attachments/assets/03485adc-132e-41b6-9a57-955e9a65d6a7" width="15%" />
  <img src="https://github.com/user-attachments/assets/57bd69e0-9fbb-4459-b10c-7a62733ae6c2" width="15%" />
  <img src="https://github.com/user-attachments/assets/6adddb5e-b22a-434b-8719-85ecaaac65bb" width="15%" />
  <img src="https://github.com/user-attachments/assets/036e2768-570f-4e70-b687-c2dcf57050b3" width="15%" />
</p>

<br />

---

## ✦ Table of Contents

- [Highlights](#-highlights)
- [Features](#-features)
- [Download](#-download)
- [Getting Started](#-getting-started)
- [Permissions](#-permissions)
- [How It Works](#-how-it-works)
- [Key Files / Structure](#-key-files--structure)
- [Adding a New Effect](#-adding-a-new-effect)
- [Troubleshooting](#-troubleshooting)
- [Contributing](#-contributing)
- [Community](#-community)
- [License](#-license)

---

## ✦ Highlights

| | |
|---|---|
| 🎨 **4 Gradient Styles** | Linear, Radial, Angular (sweep), Diamond — each with a 0°–360° rotation slider |
| ✨ **5 Visual Effects** | Snow, Stripes, Glass overlay, Geometric grid, Blur — each with individual opacity |
| 👁 **Live Preview Mode** | Fullscreen overlay to tweak style, angle & effects before applying |
| ❤️ **Favourites** | Snapshot any wallpaper including exact effects & alphas; import/export as `.wall` |
| 📤 **Share Anywhere** | Export as PNG, `.wall`, SVG, or production-ready CSS |
| 🎛 **Two Interaction Modes** | Simple (tap → apply) or Advanced (tap → live preview) |
| 🎵 **Haptic Feedback** | Toggleable, context-sensitive — snaps at 90° angle checkpoints |
| 🔔 **Auto Update Check** | Silent GitHub releases check on launch |
| 💎 **Material You** | Dynamic color, floating nav bar, animated chips, spring physics |

---

## ✦ Features

### Wallpaper Generation

- Generate grids of **12, 16, or 20** wallpapers per refresh.
- **Portrait** (9:16) and **Landscape** (16:9) preview and bitmap output.
- Select up to **5 custom colors** via HSV picker, or let the app generate random palettes.
- Three tone modes: **Dark**, **Neutral**, **Light** — applied during color generation and shading.
- **Multi-color mode** produces 3–5 stop gradients.
- Toggle any combination of gradient types per session.

### Effects

| Effect | Description |
|---|---|
| ❄️ **Snow** | Film-grain-style random white specks |
| 〰️ **Stripes** | Diagonal translucent stripe pattern |
| 🪟 **Glass** | Nothing-style subtle glass overlay |
| 🔲 **Geometric** | Grid and circle line overlay |
| 💧 **Blur** | Full-image Stack Blur — pure Kotlin, no RenderScript |

Each effect has a **0–100% opacity slider** in Advanced mode. Effects are snapshotted with favourites so the saved look is reproduced exactly on any device.

### Advanced Preview Overlay

- Fullscreen wallpaper render in a device-frame style box.
- Switch gradient style and drag the angle slider without leaving the preview.
- Angle slider **snaps to 90° increments** with haptic feedback.
- Per-effect chips with a fill-progress bar showing current alpha.
- Heart button inside preview to favourite the current configuration.
- **Done** opens the apply/download dialog directly from the overlay.

### Favourites

- Heart any wallpaper from the grid or inside the preview.
- Favourites store the exact effect flags and opacities at save time.
- Reverse-chronological grid; supports portrait/landscape toggle.
- Import individual `.wall` files or bulk-import multiple at once.
- Export all favourites as a single `.wall` file to share or back up.

### Apply & Share

- Apply to **Home screen**, **Lock screen**, or **Both**.
- Download as PNG to `Pictures/Waller` via MediaStore.
- Share as **PNG bitmap**, **`.wall` file**, **SVG**, or **CSS**.
- SVG export mirrors `BitmapUtils` rendering math exactly (gradient anchoring, stripe pattern, noise filter, base64 overlays).
- CSS export produces ready-to-use classes with `linear-gradient` / `conic-gradient` / `radial-gradient` and `filter: blur()` — paste straight into a project.

### Settings

| Setting | Options |
|---|---|
| App theme | System / Light / Dark |
| Gradient background | On / Off |
| Haptics | On / Off |
| Interaction mode | Simple / Advanced |
| Default orientation | Auto / Portrait / Landscape |
| Default gradient count | 12 / 16 / 20 |
| Default tone mode | Dark / Neutral / Light |
| Default multicolor | On / Off |
| Default effects | Snow / Stripes / Glass pre-enabled |

---

## ✦ Download

<p>
  <a href="https://github.com/Pankaj-Meharchandani/Waller/releases/latest">
    <img src="https://img.shields.io/badge/⬇ Download-Latest%20Release-6650a4?style=for-the-badge" />
  </a>
  &nbsp;
  <a href="https://github.com/Pankaj-Meharchandani/Waller/releases">
    <img src="https://img.shields.io/badge/🧪 Download-Beta%20%2F%20Pre--release-7D5260?style=for-the-badge" />
  </a>
</p>

Grab the latest `.apk` from the [**Releases page**](https://github.com/Pankaj-Meharchandani/Waller/releases). Pre-releases are clearly labelled — beta builds often ship new effects or UI experiments ahead of the stable channel.

---

## ✦ Getting Started

```bash
git clone https://github.com/Pankaj-Meharchandani/Waller.git
cd Waller
```

Open in **Android Studio** (Flamingo or newer), let Gradle sync, then run on a device or emulator.

Or from the command line:

```bash
./gradlew assembleDebug
./gradlew installDebug
```

> **Tip:** To open a `.wall` file from a file manager or another app, just tap it — Waller registers itself as a handler for `application/octet-stream` via an intent filter in the manifest.

**Requirements:**

- Android Studio Flamingo or newer
- Kotlin (matching the project Kotlin version)
- Jetpack Compose with Material3
- Min SDK **API 26** (Android 8.0) · Tested on Android 11–14
- Blur preview requires **API 31+** and degrades gracefully on older devices

---

## ✦ Permissions

| Permission | When required |
|---|---|
| `WRITE_EXTERNAL_STORAGE` | API ≤ 28 only — saving PNGs to `Pictures/Waller` |
| `SET_WALLPAPER` | Normal permission, no runtime prompt needed |
| `INTERNET` | Update checker — one GitHub API call per launch |

On API 29+ the app uses `MediaStore` with `RELATIVE_PATH` and `IS_PENDING` — no storage permission required. Lock-screen wallpaper (`WallpaperManager.FLAG_LOCK`) works on API 24+; behavior on some OEM skins (MIUI, One UI) may vary.

---

## ✦ How It Works

### Color Generation

`ColorUtils.kt` works in HSV space to apply tone-biased generation. `generateRandomColor(toneMode)` clamps the value (brightness) channel to a range matching the selected tone. `createShade(color, toneMode, subtle)` produces close variations with small hue/saturation/value deltas — making palette pairs feel cohesive rather than arbitrary.

### Bitmap Rendering (`BitmapUtils.kt`)

When applying or downloading, `createGradientBitmap()` draws into an Android `Canvas` in layer order:

1. **Gradient** — `LinearGradient`, `RadialGradient`, or `SweepGradient` shader via `drawRect`.
2. **Snow** — random white circles at ~2% pixel density with randomized alpha.
3. **Stripes** — canvas rotated −45°, soft-fade gradient rects at `width/12` spacing.
4. **Glass overlay** — `overlay_stripes.png` scaled to canvas with per-alpha `Paint`.
5. **Geometric overlay** — `overlay_geometric.png` scaled to width with per-alpha `Paint`.
6. **Blur** — pure-Kotlin Stack Blur (`stackBlur()`), radius 1–25 px scaled by `blurAlpha`. Applied last so it affects the fully composited image.

### Compose Preview Rendering

`WallpaperItemCard` and `PreviewWallpaperRender` mirror the same pipeline using Compose `Brush`, Canvas drawscope, and `Image` composables. Angular gradients use a `SweepGradient` shader drawn via `nativeCanvas` since Compose's sweep brush doesn't support rotation. Blur uses `RenderEffect.createBlurEffect()` (API 31+) on a `graphicsLayer` wrapping only the gradient/effects — keeping the bottom type/color tag sharp outside the blur group.

### Favourites Persistence

Favourites are encoded as a delimited string in `SharedPreferences`. Each entry stores: gradient type, hex color list, 5 effect flags, angle, and 5 alpha floats. The format is backward-compatible — older entries missing alpha fields default to `1f`.

### `.wall` File Format

A `.wall` file is JSON produced by `kotlinx.serialization`:

```json
{
  "version": 1,
  "walls": [
    {
      "colors": [-13421773, -6710887],
      "gradientType": "Linear",
      "angleDeg": 135.0,
      "addNoise": false,
      "addStripes": true,
      "addOverlay": false,
      "addGeometric": false,
      "addBlur": false,
      "noiseAlpha": 1.0,
      "stripesAlpha": 0.6,
      "overlayAlpha": 1.0,
      "geometricAlpha": 1.0,
      "blurAlpha": 1.0
    }
  ]
}
```

Colors are ARGB `Int` values. Multiple wallpapers can be packed into one file for bulk sharing or backup.

---

## ✦ Key Files / Structure

```
ui/
├── WallerApp.kt                     # Root composable, global state, navigation, persistence
├── wallpaper/
│   ├── WallpaperModels.kt           # Wallpaper, GradientType, ToneMode, FavoriteWallpaper
│   ├── WallpaperGeneratorScreen.kt  # Home screen — grid, options panel, refresh
│   ├── FavoritesScreen.kt           # Favourites grid, import/export, preview
│   ├── ApplyDownloadDialog.kt       # Apply / Download / Share dialog
│   ├── BitmapUtils.kt               # Bitmap rendering, save, apply, Stack Blur
│   ├── ColorUtils.kt                # HSV helpers, random color generation, shading
│   ├── WallpaperSessionState.kt     # Per-session state (colors, scroll, gradient types)
│   ├── ShareUtils.kt                # PNG / SVG / CSS share helpers
│   ├── Haptics.kt                   # Global haptic feedback wrapper
│   └── InteractionMode.kt           # SIMPLE / ADVANCED enum
│   └── components/
│       ├── WallpaperItemCard.kt     # Grid preview card (Compose Canvas rendering)
│       ├── CompactOptionsPanel.kt   # Effect/gradient/tone/color chips row
│       ├── Header.kt                # App title + orientation chip
│       ├── FloatingNavBar.kt        # Animated floating navigation bar
│       ├── EffectsSelector.kt       # Switch list (Settings context)
│       └── previewOverlay/
│           ├── WallpaperPreviewOverlay.kt    # Fullscreen preview screen
│           ├── PreviewWallpaperRender.kt     # Stateless preview renderer
│           ├── PreviewRenderer.kt            # Brush/shader factory functions
│           ├── PreviewControlsComponents.kt  # Effect chips, sliders, gradient selectors
│           └── PreviewState.kt               # Default alpha constants
├── wallfile/
│   ├── WallFile.kt                  # Serializable data classes for .wall format
│   ├── WallFileManager.kt           # Import / export / share .wall files
│   ├── WallConverters.kt            # FavoriteWallpaper ↔ WallFavorite converters
│   └── SvgExporter.kt               # SVG and CSS file generation
├── settings/
│   ├── SettingsScreen.kt            # Settings UI
│   └── AboutScreen.kt               # About screen with links
└── onboarding/
    ├── OnboardingModeDialog.kt      # First-launch mode picker
    ├── UpdateAvailableDialog.kt     # Update available dialog
    └── UpdateChecker.kt             # GitHub releases API check
```

---

## ✦ Adding a New Effect

The architecture is designed so adding a new effect touches **two core files** plus a handful of wiring files:

**Core (rendering logic):**
1. `WallpaperModels.kt` — add the boolean flag and alpha `Float` to `FavoriteWallpaper`.
2. `BitmapUtils.kt` — add a drawing block in `createGradientBitmap()`.

**Wiring (plumbing the new state through):**
- `WallFile.kt` + `WallConverters.kt` — extend the serializable format and converter.
- `CompactOptionsPanel.kt` — add an entry to the `effects` list (icon key + label).
- `WallpaperPreviewOverlay.kt` — add an `EffectConfig` entry and a slider `when` branch.
- `WallpaperItemCard.kt` + `PreviewWallpaperRender.kt` — add the Compose Canvas rendering blocks.
- `WallerApp.kt` + `WallpaperGeneratorScreen.kt` + `FavoritesScreen.kt` — thread state through the call chain.

> **Architectural note:** The dual rendering path (Compose Canvas in `WallpaperItemCard` vs. Android Canvas in `BitmapUtils`) is a known tension. A future refactor toward a shared `EffectRenderer` interface would reduce the per-effect file count to 2.

---

## ✦ Troubleshooting

**Bitmap looks low resolution on some devices**
`getScreenSizeForBitmap()` uses `currentWindowMetrics` (API 30+) and subtracts system bar insets. On unusual OEM configurations the returned dimensions can be smaller than expected.

**Blur has no visual effect in preview**
The Compose `RenderEffect` blur requires **API 31+**. On API 30 the blur chip still works but the preview render is a no-op. The bitmap blur (`stackBlur`) works on all API levels.

**Lock screen apply does nothing**
Some OEM skins (MIUI, One UI) restrict programmatic lock-screen changes. The app falls back to `FLAG_SYSTEM` silently — no crash, but the toast will show the failure message.

**Save fails on Android 8 or 9**
Grant `WRITE_EXTERNAL_STORAGE` when prompted. The app requests it at the point of saving on API ≤ 28.

**Gallery doesn't show the saved image**
On API 29+ MediaStore insertion with `IS_PENDING = 0` surfaces the file immediately. On older versions a media scanner broadcast is sent. If it still doesn't appear, reboot or navigate to `Pictures/Waller` in a file manager.

**`.wall` file not recognized by the picker**
The picker launches with `*/*` since `.wall` has no registered MIME type. The app filters to `.wall` extension after selection, falling back to all selected files if none match.

---

## ✦ Contributing

Contributions are welcome!

1. Fork the repo and create a feature branch: `git checkout -b feat/my-feature`
2. Commit with a clear message: `git commit -m "Add my feature"`
3. Push and open a pull request.

Please:
- Keep both rendering paths (Compose + Android Canvas) in sync when touching effect logic.
- Test on both API 30 and API 31+ (blur behavior differs).
- Run through portrait and landscape for any UI change.
- Prefer stdlib/regex over new third-party dependencies for simple tasks.

---

## ✦ Community

<p>
  <a href="https://t.me/Waller_app">
    <img src="https://img.shields.io/badge/📣 Telegram-@Waller__app%20(Announcements)-26A5E4?style=for-the-badge&logo=telegram&logoColor=white" />
  </a>
</p>
<p>
  <a href="https://t.me/walllller">
    <img src="https://img.shields.io/badge/💬 Telegram-@walllller%20(Support%20%26%20Chat)-26A5E4?style=for-the-badge&logo=telegram&logoColor=white" />
  </a>
</p>
<p>
  <a href="https://github.com/Pankaj-Meharchandani/Waller/issues">
    <img src="https://img.shields.io/badge/🐛 GitHub-Issues%20%26%20Feature%20Requests-181717?style=for-the-badge&logo=github&logoColor=white" />
  </a>
</p>

---

## ✦ License

This project is licensed under the **GPL License**. See the [LICENSE](LICENSE) file for details.

---

<p align="center">
  Made with ♥ by <a href="https://github.com/Pankaj-Meharchandani">Pankaj Meharchandani</a>
</p>
