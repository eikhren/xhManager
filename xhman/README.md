# xhMan — Wayland Crosshair Manager for KDE

Lightweight crosshair overlay that uses the `wlr-layer-shell` protocol on KDE Wayland (KWin). It draws a transparent, click-through surface on the selected outputs and renders a simple configurable crosshair. No GPU is needed; rendering is done once per configure event to keep CPU/GPU usage minimal.

## Status and limitations
- Targets KDE Wayland (Ubuntu 24.04). Other wlroots compositors may work but are untested here.
- Uses a full-output transparent surface with an empty input region, so it will not steal focus and remains click-through.
- Wayland does not allow clients to pick arbitrary foreign windows. The crosshair is placed on entire outputs; per-window targeting would require compositor-specific APIs (e.g., a KWin script) that are not implemented yet.
- Exit with `Ctrl+C` from the terminal that launched it.

## Build
```bash
cargo build --release
```

The binary will be at `target/release/xhman`.

## Run
```bash
./target/release/xhman \
  --color "#00FF00CC" \
  --length 20 \
  --thickness 2 \
  --gap 6 \
  --output HDMI-A-1
```

If `--output` is omitted, the crosshair is shown on all detected outputs. Color accepts `#RRGGBB` or `#RRGGBBAA`, or `r,g,b,a` with 0–255 components. Thickness, length, and gap are in physical pixels (they respect the output scale factor).

## Notes on resource use
- The overlay only redraws after compositor configures/resizes; otherwise it is static.
- Uses shared-memory buffers sized to the output resolution; no continuous animation loop or GPU work.

## Future extensions
- Optional KWin script/DBus bridge to follow the active window or a specific app.
- Hotkey to toggle visibility.
