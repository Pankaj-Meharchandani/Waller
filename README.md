# Waller — Gradient Wallpaper Generator for Android

[https://github.com/Pankaj-Meharchandani/Waller](https://github.com/Pankaj-Meharchandani/Waller)

> Generate beautiful gradient wallpapers (linear / radial / sweep / diamond), preview them in portrait or landscape, tweak colors, add noise, and apply or save them right from the app.

A compact, modern Jetpack Compose app that demonstrates generating Android `Bitmap`s from Compose `Brush` concepts, saving to `MediaStore`, and applying wallpapers (home/lock/both) using `WallpaperManager`.

---

## Table of contents

- [Highlights](#highlights)
- [Features](#features)
- [Preview](#preview)
- [Requirements](#requirements)
- [Getting started (build & run)](#getting-started-build--run)
- [Permissions & behaviour by Android version](#permissions--behaviour-by-android-version)
- [How it works (high level)](#how-it-works-high-level)
- [Key files / structure](#key-files--structure)
- [Customization & extension points](#customization--extension-points)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

---

## Highlights

- Jetpack Compose UI with responsive grid previews.
- Multiple gradient styles: **Linear**, **Radial**, **Angular (sweep)**, and **Diamond (linear fallback)**.
- Random color generation with pleasing light/dark bias and on-demand user color picking (HSV + hex).
- Create high-resolution bitmaps based on the device/window size to apply as wallpaper.
- Save generated images to `Pictures/Waller` via `MediaStore` (handles API differences).
- Apply wallpaper to Home, Lock, or Both (where supported).
- Optional noise/grain effect for filmic texture.

---

## Features

- Portrait / Landscape preview and bitmap generation.
- Select up to 5 custom colors or let the app pick random colors.
- Choose available gradient styles (multi-select).
- Add noise/grain overlay.
- Preview grid of generated variations (20 by default).
- Apply wallpaper to Home, Lock, or Both screens.
- Download wallpaper to `Pictures/Waller` (visible in gallery).
- Simple, focused UI implemented in Compose.

---

## Preview

<p align="center">
  <img src="https://github.com/user-attachments/assets/26c4ced6-31f0-4d41-a1c1-fcd55c3c684c" width="20%" /> 
  <img src="https://github.com/user-attachments/assets/bcf204df-6e09-4703-83cd-5894077f2150" width="20%" /> <br>
  <img src="https://github.com/user-attachments/assets/0630110d-3081-4a6d-8566-128f1125c3c5" width="20%" /> 
  <img src="https://github.com/user-attachments/assets/f315c1db-a85f-400e-adeb-fdb896173da3" width="20%" /> 
  <img src="https://github.com/user-attachments/assets/4a07d64e-e58e-42e8-a45f-804b87535fc7" width="20%" /> 
  <img src="https://github.com/user-attachments/assets/f92c5adb-550c-491f-937d-a6b7653a2778" width="20%" /> 
  <img src="https://github.com/user-attachments/assets/28b2f110-d9db-4393-bd3e-ba33eac62aae" width="20%" /> 
  <img src="https://github.com/user-attachments/assets/03485adc-132e-41b6-9a57-955e9a65d6a7" width="20%" /> 
</p>

---

## Requirements

- Android Studio (Arctic Fox / Bumblebee / Chipmunk or newer recommended).
- Kotlin (matching project Kotlin version).
- Jetpack Compose (project uses Compose UI components).
- Tested on Android 11 (R) and above — backwards-compatible handling is implemented for older devices where possible.

> **Notes:**  
> The app handles `MediaStore` differences for Android Q (API 29) and above vs. legacy devices. Some behavior (applying to lock screen) requires API level >= N (Android 7.0).

---

## Getting started (build & run)

1. Clone the repo:
   ```bash
   git clone https://github.com/Pankaj-Meharchandani/Waller.git
   cd Waller
   ```
2. Open the project in Android Studio.
3. Let Android Studio sync Gradle and download required dependencies.
4. Run on a device or emulator:
   - Connect a device or start an emulator.
   - `Run > Run 'app'` or use the green play button.

OR from command line:
```bash
./gradlew assembleDebug
./gradlew installDebug
```

---

## Permissions & behaviour by Android version

- **WRITE_EXTERNAL_STORAGE**  
  - Only requested on **legacy** devices (SDK < Q / API 29) so the app can save images to the public Pictures folder.
  - On Android Q+ (API 29+), the app uses `MediaStore` `RELATIVE_PATH` and does **not** need `WRITE_EXTERNAL_STORAGE`.
- **SET_WALLPAPER** (normal)  
  - No explicit runtime permission is required to set wallpaper via `WallpaperManager`.
- **Applying to Lock Screen**  
  - Available where `WallpaperManager.FLAG_LOCK` is supported (API >= N). On older devices, the app falls back to applying to home screen.
- The UI warns the user to re-tap "Download" after granting storage permissions on legacy devices (simple UX flow implemented).

**Manifest snippet** (example):
```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="28"/>
```

---

## How it works (high level)

1. **Preview generation**  
   - Compose UI shows a grid of preview cards. Each card paints a Compose `Brush` (linear / radial / sweep) with generated colors.
2. **Bitmap creation**  
   - When user chooses to apply/download, `createGradientBitmap(context, wallpaper, isPortrait, addNoise)` is called.
   - It computes a bitmap sized to the device/window (uses `currentWindowMetrics` on API >= R), creates an Android `Shader` (`LinearGradient`, `RadialGradient`, `SweepGradient`) and draws into an Android `Canvas`.
   - Optional noise is drawn on top using random white specks.
3. **Apply wallpaper**  
   - `tryApplyWallpaper(context, bitmap, flags)` uses `WallpaperManager.setBitmap()`; on API >= N it passes flags for system/lock.
4. **Save image**  
   - `saveBitmapToMediaStore` inserts into `MediaStore.Images.Media.EXTERNAL_CONTENT_URI`, uses `RELATIVE_PATH` and `IS_PENDING` on Q+ and notifies media scanner on older versions.

---

## Key files / structure

- `MainActivity.kt` — contains `WallerApp`, `WallpaperGeneratorScreen`, Compose UI and core functionality.
- UI components:
  - `ColorPickerDialog`, `SaturationValuePicker`
  - `WallpaperItem`, `WallpaperItemCard`
  - Selectors: `OrientationSelector`, `ColorSelector`, `GradientStylesSelector`, `EffectsSelector`
- Utility functions:
  - `createGradientBitmap(...)`
  - `saveBitmapToMediaStore(...)`
  - `tryApplyWallpaper(...)`
  - `getScreenSizeForBitmap(...)`
  - Color helpers: `colorToHsv`, `createShade`, `generateRandomColor`, `.toHexString()`
- Theme: `ui.theme` (Compose Material3 theme wrapper)

---

## Customization & extension points

- **Add new gradient styles**  
  Add a new `GradientType` case and implement Android shader drawing in `createGradientBitmap` and Compose brush in `WallpaperItem`.
- **Adjust noise algorithm**  
  Modify `numNoisePoints` or the alpha distribution to get different grain feels.
- **Export options**  
  Add share intent to share PNG directly:
  ```kotlin
  val uri = saveBitmapAndGetUri(...)
  val share = Intent(Intent.ACTION_SEND).apply {
      type = "image/png"
      putExtra(Intent.EXTRA_STREAM, uri)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  }
  context.startActivity(Intent.createChooser(share, "Share wallpaper"))
  ```
- **Higher-resolution assets**  
  The current bitmap matches display size. For ultra-high-res exports, scale the computed width/height before drawing while keeping aspect ratio.

---

## Troubleshooting

- **Bitmap appears low-quality on some devices**  
  - Confirm `getScreenSizeForBitmap` returns expected large dimensions. For some OEMs or edge cases, window metrics/insets can differ.
- **Save fails on older devices**  
  - Ensure `WRITE_EXTERNAL_STORAGE` is granted (only necessary < API 29).
- **Lock screen apply does nothing**  
  - Many OEMs restrict lock-screen wallpaper changing. Check `Build.VERSION.SDK_INT` and fall back to system/home wallpaper if unsupported.
- **Gallery doesn't show saved image**  
  - On legacy devices the code sends `Intent.ACTION_MEDIA_SCANNER_SCAN_FILE`. On modern devices, `MediaStore` insertion should make it immediately available. If not, reboot or re-scan media.

---

## Contributing

Contributions welcome!

1. Fork the repo.
2. Create a feature branch: `git checkout -b feat/new-gradient`
3. Commit your changes: `git commit -m "Add fancy gradient"`
4. Push and open a pull request.

Please follow these guidelines:
- Keep UI changes accessible and responsive.
- Add tests or manual test instructions for rendering on different API levels.
- Update `README` and add screenshots where helpful.

---

## Changelog (example)

- **v1.0** — Initial Compose implementation: linear/radial/sweep gradients, noise, save & apply features, color picker.
- Future: more gradient presets, tileable patterns, multi-color gradients, animated previews.

---

## License

This project is licensed under the GPL License - see the [LICENSE](LICENSE) file for details.

---

## Contact

If you have questions, feature requests, or bug reports, open an issue on the repository: https://github.com/Pankaj-Meharchandani/Waller


---

*README generated with help from ChatGPT*

