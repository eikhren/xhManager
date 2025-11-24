# xhMan Desktop Prototype — Developer Receipt

Date: (current prototype state captured from `/xhman-desktop`)

## What Exists Now
- **Modules & stack**: Compose for Desktop (Kotlin 1.9.24, JDK 21), Skia renderer, coroutine-based storage/persistence. Modules: `core` (models/intents/validators), `render` (Skia renderer + cache), `storage` (import pipeline, library state, JSON profiles), `ui` (Compose screens + VMs), `app` (desktop entry).
- **Library/import**: Add roots via path field or Swing folder chooser; roots stored with stable UUID by path. Import walks directories on IO dispatcher, accepts `png|bmp|svg`, optional size cap (unused in UI). Missing roots are pruned. Assets are deduped by ID, stored per-root, and the active selection survives unless its asset disappears. Toggling a root off hides its assets; toggling on triggers a rescan.
- **UI layout**: Three columns — library (roots & assets), preview, config. Profiles are docked under the preview (50/50 split in the middle column). Library list shows thumbnails loaded off the UI thread; preview panel centers status text and renders selected asset.
- **Preview/rendering**: Renderer normalizes config, caches by (uri + config hash), applies scale/rotation/opacity/tint, plus optional glow/outline passes. Intrinsic scale sets scale=1.0 (no DPI metadata use). Rendering & image load run off the UI thread with cancellation handling to avoid “scope left composition”.
- **Config controls**: Scale (disabled when intrinsic is on), rotation, opacity, tint and outline color pickers with hex input + swatches. Glow/outline width sliders are present but disabled (“coming soon”). Reset restores defaults. Blend mode/offset/tint mode are defined in models but not exposed in UI.
- **Profiles**: Save/load/delete profiles persisted to `~/.config/xhman/profiles.json`. Save captures current config, selection, and root IDs; load applies config and re-selects the first stored asset ID if present. Versions increment per save.

## Not Implemented / Unplanned
- **Import UX**: No drag-and-drop, multi-root browse, progress, errors, or filters in UI. Import is synchronous per root with no cancellation or background queue management. Root dedupe is by path only; no handling of moved/renamed roots beyond full rescan.
- **Preview fidelity**: No intrinsic DPI/metadata-aware scaling; intrinsic toggle simply locks scale to 1x. No zoom/pan, no checkerboard transparency, no background selector, no multi-asset overlay/stacking, and no vector-specific rendering controls.
- **Config gaps**: Glow/outline sliders disabled in UI despite renderer support; blend mode, offset, and tint mode are not surfaced. No snapping, reset-to-profile-defaults, or per-asset overrides.
- **Profiles**: Saved root associations aren’t reapplied (roots aren’t activated/added on load); multi-selection support is absent. No import/export of profiles, no conflict or validation handling for missing assets/roots on load.
- **Library**: No search/sort/filter, no pagination, no breadcrumbing of nested folders, and no metadata (dimensions/filesize) surfaced. Thumbnails lack caching across sessions and fail silently on decode errors.
- **Error handling & UX**: Minimal user feedback (generic errors), no notifications or toasts. Loading states are coarse (single `isScanning`). No accessibility work, no keyboard shortcuts.
- **Packaging & CI**: Gradle wrapper not checked in; no CI, tests for UI/render, or packaging artifacts (deb/rpm/AppImage configs noted but not exercised). No update mechanism or telemetry.

## Known Issues / Risks
- Folder chooser is Swing-based (blocking), may not be Wayland-friendly.
- Intrinsic scale is mislabeled vs behavior (locks to 1x, not true intrinsic).
- Profile panel anchoring satisfies 50/50 with preview but shrinks preview height compared to original full-height intent.
- Renderer assumes premultiplied alpha inputs; no validation of incoming image formats.
- Import walks entire tree without limits; large roots may stall UI due to coarse scanning state.

## Suggested Next Steps
1) Decide on final column layout (preview full-height vs 50/50 with profiles) and lock UX spec.  
2) Expose remaining config fields (blend, tint mode, offsets) and either implement or hide glow/outline consistently.  
3) Make intrinsic scale metadata-aware or rename to “Lock scale to 1x”.  
4) Apply profile root associations on load (ensure roots exist/activate; prompt when missing).  
5) Add library quality-of-life: search/filter, dimension/size display, error toasts, and drag/drop import.  
6) Introduce Gradle wrapper + CI smoke build; add renderer/UI sanity tests; exercise packaging targets.
