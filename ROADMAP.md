# 🗺️ Waller Roadmap

This document outlines the planned development path for **Waller**, starting from the current stable release **v1.6**.

The roadmap is structured for fast, clean, incremental development.

---

## 📍 Current Version — v1.6 (Stable)
Foundation complete:
- Gradient generation (Linear, Radial, Angular, Diamond)
- Noise, Stripes, and **Nothing-style glass effect**
- Portrait / Landscape previews
- Custom colors (1–5)
- Apply wallpaper (Home / Lock / Both)
- Download to MediaStore
- Modern Compose UI

---

# 🚀 Upcoming Versions

---

## **v1.7 — Settings & About (Next Update)**

### 🎯 Goals
Introduce missing screens & preference system.

### Features
- **Settings screen**
  - Theme toggle (Light/Dark/System)
  - UI mode selector (Simple / Advanced)
  - Basic preferences saved using DataStore

- **About screen**
  - App name, version
  - GitHub link
  - License (GPL)
  - Short description

- **First-run welcome dialog**
  - “Configure UI in Settings”

This update adds foundation for future UI features but does not change the main screen yet.

---

## **v1.8 — UI Modes (Simple vs Advanced)**

### 🎯 Goals  
Give users control over how much UI they want.

### Features
- First-run popup (Tasker-style):
  - **Simple & Clean**  
  - **Advanced / Technical**
- Save user mode permanently
- **Simple Mode**
  - Most sections collapsed  
  - Clean, minimal interface
  - **Nothing-style effect always visible** (signature feature)
- **Advanced Mode**
  - All cards open by default

---

## **v1.9 — Collapsible Cards Update**

### 🎯 Goals  
Reduce clutter while keeping power features.

### Features
- All cards become **collapsible**
- Smooth animation for expand/collapse
- “Reset UI layout” in Settings
- Mode-specific defaults:
  - **Simple Mode**
    - Orientation + Tone → expanded  
    - Others collapsed  
    - **Effects partially visible so Nothing-style toggle is always shown**
  - **Advanced Mode**
    - All expanded

---

## **v2.0 — 3-Step Tone Slider**

### 🎯 Goals  
Enhance control over color brightness.

### Features
- Replace Light/Dark with:
  - **Dark • Original • Light**
- Updated random color generator
- Updated shade variations
- Default tone option in Settings

---

## **v2.1 — Multi-Color Gradients**

### 🎯 Goals  
Unlock richer gradients.

### Features
- Support 2–5 color stops in gradient drawing
- Compose UI supports multi-stop brushes
- Android shader supports full stop arrays
- Shuffle color order button

---

## **v2.2 — Favorites & Collections**

### 🎯 Goals  
Let users save and organize their best creations.

### Features
- Save wallpapers as presets
- Favorites screen (grid)
- Apply / Download / Rename / Delete
- Optional named Collections (future enhancement)

---

## **v2.3 — UX Polish & Enhancements**

### Features
- Share wallpaper button (Intent chooser)
- Haptic feedback
- Default orientation setting
- Grid size selector (12 / 20 / 30 previews)
- Optional palette presets (Pastel / Neon / AMOLED)

---

## 📝 Notes
- Roadmap may evolve based on user feedback.
---
