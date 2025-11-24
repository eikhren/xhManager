# xhMan Wayland Overlay — Developer Receipt

Snapshot of the current Rust/Wayland CLI (`/xhman`) status.

## What Exists Now
- **Purpose**: Draws a click-through crosshair overlay on KDE Wayland outputs using `wlr-layer-shell` via smithay-client-toolkit.
- **Entry point**: Single binary `xhman`; run from terminal. No daemon/service mode.
- **CLI options**: `--output/-o` (repeatable) to target specific outputs; defaults to all. `--color/-c` accepts `#RRGGBB[AA]` or `r,g,b,a`. `--thickness/-t`, `--length/-l`, `--gap/-g` take positive pixel values. `--help` prints usage.
- **Rendering**: Static crosshair drawn into shared-memory buffers (ARGB8888), premultiplied alpha, one surface per output. Redrawn on configure events; no animation loop. Honors output scale factor.
- **Surface behavior**: Full-output overlay, transparent background, empty input region (click-through), `KeyboardInteractivity::None`, `exclusive_zone=0`.
- **Output handling**: Creates per-output layer surfaces on detection; removes on output destroy. If `--output` list is provided, only matching names are created.

## Not Implemented / Unplanned
- **Config/persistence**: No config file, presets, or hotkeys. All settings are CLI-only and one-shot.
- **UX/controls**: No toggle visibility, no reload on config change, no IPC/DBus control, no KWin script bridge for per-window targeting.
- **Rendering features**: No shapes beyond plus-cross; no outlines/glow; no custom assets/images; no center-dot toggle; no DPI-aware length units beyond scale factor.
- **Error handling/observability**: Minimal logging (stderr only), no structured logs or metrics. No retries for buffer allocation failures.
- **Platform scope**: Only tested/aimed at KDE Wayland; no X11 support, no wlroots/other compositor quirks handled.
- **Packaging/CI**: No release artifacts, installer, or CI. Cargo.lock present; edition 2024. No tests/benches/fuzzing.
- **Performance limits**: Full-output buffers allocated per configure; no cap or reuse sizing strategy; large/high-DPI outputs may allocate large SHM without guardrails.

## Known Risks / Gaps
- Assumes compositor provides `wlr-layer-shell`; behavior on non-KDE compositors unverified.
- Arg parsing is minimal; invalid values emit simple errors and exit.
- Buffer pool is fixed-size initial allocation (1024 bytes) and grows per `create_buffer`; no backpressure.
- No visibility toggle means you must kill the process (Ctrl+C) to hide the overlay.

## Suggested Next Steps
1) Add a small control channel (DBus or Unix socket) for toggle/reload and future window-follow features; consider a KWin script for per-window placement.
2) Harden rendering: optional outline/dot, size presets, guardrails on buffer allocation for very large outputs.
3) Improve UX: config file/env defaults, log verbosity flag, clearer error messages.
4) Ship basics: add CI, `cargo fmt/clippy` gates, and packageable release build script for KDE/Ubuntu.***
