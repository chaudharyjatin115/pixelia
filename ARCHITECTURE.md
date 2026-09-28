# Pixelia Technical Architecture

Pixelia is a local-first, privacy-focused Android media gallery built using modern Android development practices, Material 3 Expressive styling, and Jetpack Compose.

---

## 🏛️ Architecture Overview

Pixelia follows modern Android architecture recommendations, structured into three clean layers:

```
┌────────────────────────────────────────────────────────┐
│                   UI Layer (Compose)                   │
│  PhotosScreen, FoldersScreen, FavouritesScreen, Bin,   │
│  PhotoViewerScreen, PhotoEditorScreen, FloatingPillBar │
└───────────────────────────▲────────────────────────────┘
                            │ StateFlow / UI State
┌───────────────────────────┴────────────────────────────┐
│                    ViewModel Layer                     │
│                   GalleryViewModel                     │
└───────────────────────────▲────────────────────────────┘
                            │ Flow / Data Streams
┌───────────────────────────┴────────────────────────────┐
│                      Data Layer                        │
│   MediaRepository, MediaStoreScanner, LocalMediaStateStore │
└────────────────────────────────────────────────────────┘
```

---

## 🔑 Core Features & System Design

1. **MediaStore Scanner (`MediaStoreScanner`)**:
   - Queries system `MediaStore.Images.Media` and `MediaStore.Video.Media`.
   - Maps raw content provider cursors into strongly-typed `MediaItem` models.
   - Converts Unix epoch timestamps into date-grouped headers (`DateGroupedMedia`).

2. **Reactive Data Repository (`MediaRepository`)**:
   - Observes MediaStore URI changes using `ContentObserver` for real-time updates.
   - Merges system media with user favorites and soft-deleted bin state stored in `LocalMediaStateStore`.

3. **Material 3 Expressive UI & Frosted Glass (`FloatingPillBar`)**:
   - Floating pill navigation bar utilizing GPU-accelerated blur (`dev.chrisbanes.haze:haze`).
   - Adaptive layouts for phone and tablet screens (`GalleryNavigationRail`).

4. **Media Editor (`PhotoEditorScreen`)**:
   - Built-in photo editor supporting cropping, rotation, flip, and color adjustments (brightness, contrast, saturation).
   - Exports edited images directly back to system storage.

5. **Metadata Inspection (`MediaInfoSheet`)**:
   - Inspects EXIF metadata including ISO, aperture, exposure time, focal length, camera model, and GPS location.

---

## 🧪 Testing Strategy

- **Domain Model Tests**: Validates date formatting, URI construction, and folder grouping logic (`MediaItemTest`, `MediaFolderTest`).
- **ViewModel & Repository Tests**: Verifies StateFlow emissions, destination selection, folder navigation, batch selection, and local preference persistence (`GalleryViewModelTest`, `LocalMediaStateStoreTest`).
