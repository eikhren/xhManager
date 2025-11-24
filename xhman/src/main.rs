use std::{collections::HashMap, env, error::Error, fmt, num::ParseIntError};

use smithay_client_toolkit::{
    compositor::{CompositorHandler, CompositorState, Region},
    delegate_compositor, delegate_layer, delegate_output, delegate_registry, delegate_shm,
    output::{OutputHandler, OutputInfo, OutputState},
    registry::{ProvidesRegistryState, RegistryState},
    registry_handlers,
    shell::{
        wlr_layer::{
            Anchor, KeyboardInteractivity, Layer, LayerShell, LayerShellHandler, LayerSurface,
            LayerSurfaceConfigure,
        },
        WaylandSurface,
    },
    shm::{
        slot::{Buffer, SlotPool},
        Shm, ShmHandler,
    },
};
use wayland_client::{
    globals::registry_queue_init,
    protocol::{wl_output, wl_shm, wl_surface},
    Connection, Proxy, QueueHandle,
};

fn main() -> Result<(), Box<dyn Error>> {
    let config = Config::from_env()?;

    let conn = Connection::connect_to_env()?;
    let (globals, mut event_queue) = registry_queue_init(&conn)?;
    let qh = event_queue.handle();

    let compositor = CompositorState::bind(&globals, &qh)?;
    let layer_shell = LayerShell::bind(&globals, &qh)?;
    let shm = Shm::bind(&globals, &qh)?;

    let mut app = App {
        registry_state: RegistryState::new(&globals),
        compositor_state: compositor,
        output_state: OutputState::new(&globals, &qh),
        layer_shell,
        shm,
        config,
        surfaces: HashMap::new(),
    };

    loop {
        event_queue.blocking_dispatch(&mut app)?;
    }
}

#[derive(Debug, Clone, Copy)]
struct Rgba {
    r: u8,
    g: u8,
    b: u8,
    a: u8,
}

#[derive(Debug, Clone)]
struct Config {
    outputs: Vec<String>,
    color: Rgba,
    thickness: u32,
    length: u32,
    gap: u32,
}

impl Config {
    fn from_env() -> Result<Self, Box<dyn Error>> {
        let mut outputs = Vec::new();
        let mut color = Rgba {
            r: 255,
            g: 0,
            b: 0,
            a: 200,
        };
        let mut thickness = 2;
        let mut length = 18;
        let mut gap = 6;

        let mut args = env::args().skip(1).peekable();
        while let Some(arg) = args.next() {
            match arg.as_str() {
                "--output" | "-o" => {
                    let name = args
                        .next()
                        .ok_or_else(|| CliError("missing value for --output".into()))?;
                    outputs.push(name);
                }
                "--color" | "-c" => {
                    let raw = args
                        .next()
                        .ok_or_else(|| CliError("missing value for --color".into()))?;
                    color = parse_color(&raw)?;
                }
                "--thickness" | "-t" => {
                    thickness = parse_u32(&mut args, "--thickness")?;
                }
                "--length" | "-l" => {
                    length = parse_u32(&mut args, "--length")?;
                }
                "--gap" | "-g" => {
                    gap = parse_u32(&mut args, "--gap")?;
                }
                "--help" | "-h" => {
                    print_help();
                    std::process::exit(0);
                }
                other => {
                    return Err(Box::new(CliError(format!("unknown argument: {other}"))));
                }
            }
        }

        Ok(Self {
            outputs,
            color,
            thickness,
            length,
            gap,
        })
    }
}

fn parse_u32(args: &mut impl Iterator<Item = String>, name: &str) -> Result<u32, Box<dyn Error>> {
    let raw = args
        .next()
        .ok_or_else(|| CliError(format!("missing value for {name}")))?;
    let value = raw.parse::<u32>()?;
    if value == 0 {
        return Err(Box::new(CliError(format!("{name} must be > 0"))));
    }
    Ok(value)
}

fn parse_color(raw: &str) -> Result<Rgba, Box<dyn Error>> {
    if let Some(hex) = raw.strip_prefix('#') {
        let bytes = match hex.len() {
            6 => hex_to_bytes(hex, 3)?,
            8 => hex_to_bytes(hex, 4)?,
            _ => {
                return Err(Box::new(CliError(
                    "hex color must be #RRGGBB or #RRGGBBAA".into(),
                )));
            }
        };

        return Ok(Rgba {
            r: bytes[0],
            g: bytes[1],
            b: bytes[2],
            a: bytes[3],
        });
    }

    let parts: Vec<_> = raw.split(',').collect();
    if parts.len() == 4 {
        let r = parse_component(parts[0])?;
        let g = parse_component(parts[1])?;
        let b = parse_component(parts[2])?;
        let a = parse_component(parts[3])?;
        return Ok(Rgba { r, g, b, a });
    }

    Err(Box::new(CliError(
        "color must be #RRGGBB[#AA] or r,g,b,a (0-255)".into(),
    )))
}

fn hex_to_bytes(input: &str, components: usize) -> Result<[u8; 4], Box<dyn Error>> {
    let mut bytes = [255u8; 4];
    for (idx, chunk) in input.as_bytes().chunks(2).enumerate() {
        let s = std::str::from_utf8(chunk)?;
        bytes[idx] = u8::from_str_radix(s, 16)?;
    }

    if components == 3 {
        bytes[3] = 255;
    }
    Ok(bytes)
}

fn parse_component(raw: &str) -> Result<u8, ParseIntError> {
    raw.trim().parse::<u8>()
}

#[derive(Debug)]
struct CliError(String);

impl fmt::Display for CliError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(&self.0)
    }
}

impl Error for CliError {}

fn print_help() {
    println!(
        "xhMan - KDE Wayland crosshair overlay

Options:
  -o, --output <name>   Target a specific output (repeatable). Defaults to all.
  -c, --color  <color>  Crosshair color (#RRGGBB[#AA] or r,g,b,a). Default #FF0000CC.
  -t, --thickness <px>  Line thickness in physical pixels. Default 2.
  -l, --length    <px>  Line length per arm in physical pixels. Default 18.
  -g, --gap       <px>  Gap at the center in physical pixels. Default 6.
  -h, --help            Show this help and exit.
"
    );
}

struct App {
    registry_state: RegistryState,
    compositor_state: CompositorState,
    output_state: OutputState,
    layer_shell: LayerShell,
    shm: Shm,
    config: Config,
    surfaces: HashMap<u32, OverlaySurface>,
}

struct OverlaySurface {
    layer: LayerSurface,
    pool: SlotPool,
    scale: i32,
    logical_size: (u32, u32),
}

impl App {
    fn ensure_surface_for_output(&mut self, qh: &QueueHandle<Self>, output: wl_output::WlOutput) {
        let info = match self.output_state.info(&output) {
            Some(info) => info.clone(),
            None => return,
        };

        if !self.config.outputs.is_empty()
            && info
                .name
                .as_ref()
                .map(|name| !self.config.outputs.iter().any(|wanted| wanted == name))
                .unwrap_or(true)
        {
            return;
        }

        let output_id = output.id().protocol_id();
        if self.surfaces.contains_key(&output_id) {
            return;
        }

        let surface = self.compositor_state.create_surface(qh);
        let layer = self.layer_shell.create_layer_surface(
            qh,
            surface.clone(),
            Layer::Overlay,
            Some("xhman"),
            Some(&output),
        );

        // Full-screen transparent surface that does not steal input.
        layer.set_anchor(Anchor::TOP | Anchor::BOTTOM | Anchor::LEFT | Anchor::RIGHT);
        layer.set_size(0, 0);
        layer.set_keyboard_interactivity(KeyboardInteractivity::None);
        layer.set_exclusive_zone(0);

        if let Ok(region) = Region::new(&self.compositor_state) {
            layer
                .wl_surface()
                .set_input_region(Some(region.wl_region()));
        }

        layer
            .wl_surface()
            .set_buffer_scale(info.scale_factor.max(1));
        layer.commit();

        let pool = SlotPool::new(1024, &self.shm).expect("failed to create shm pool");
        let logical = preferred_logical_size(&info);

        self.surfaces.insert(
            output_id,
            OverlaySurface {
                layer,
                pool,
                scale: info.scale_factor,
                logical_size: logical,
            },
        );
    }
}

fn render_crosshair(
    surface: &mut OverlaySurface,
    configure: LayerSurfaceConfigure,
    config: &Config,
) {
    let logical_width = if configure.new_size.0 == 0 {
        surface.logical_size.0
    } else {
        configure.new_size.0
    };

    let logical_height = if configure.new_size.1 == 0 {
        surface.logical_size.1
    } else {
        configure.new_size.1
    };

    let scale = surface.scale.max(1);
    let width = logical_width.max(1) * scale as u32;
    let height = logical_height.max(1) * scale as u32;
    let stride = (width as i32) * 4;

    let (buffer, canvas) = match surface.pool.create_buffer(
        width as i32,
        height as i32,
        stride,
        wl_shm::Format::Argb8888,
    ) {
        Ok(res) => res,
        Err(err) => {
            eprintln!("failed to allocate buffer: {err}");
            return;
        }
    };

    // Transparent background
    canvas.fill(0);
    paint_crosshair(canvas, width as usize, height as usize, stride as usize, config);

    surface.layer.wl_surface().set_buffer_scale(scale);
    surface
        .layer
        .wl_surface()
        .damage_buffer(0, 0, width as i32, height as i32);
    attach_and_commit(&surface.layer, buffer);

    surface.logical_size = (logical_width, logical_height);
}

fn preferred_logical_size(info: &OutputInfo) -> (u32, u32) {
    if let Some(size) = info.logical_size {
        return (size.0 as u32, size.1 as u32);
    }

    info.modes
        .iter()
        .find(|m| m.current)
        .map(|m| (m.dimensions.0 as u32, m.dimensions.1 as u32))
        .unwrap_or((1920, 1080))
}

fn attach_and_commit(layer: &LayerSurface, buffer: Buffer) {
    if let Err(err) = buffer.attach_to(layer.wl_surface()) {
        eprintln!("attach error: {err}");
        return;
    }
    layer.commit();
}

fn paint_crosshair(canvas: &mut [u8], width: usize, height: usize, stride: usize, config: &Config) {
    let cx = (width / 2) as i32;
    let cy = (height / 2) as i32;

    let length = config.length as i32;
    let gap = config.gap as i32;
    let thickness = config.thickness as i32;

    let color = premultiplied_argb(&config.color);
    let stride_px = stride / 4;

    // Horizontal arms
    for dy in -(thickness / 2)..=(thickness / 2) {
        let y = cy + dy;
        if y < 0 || y >= height as i32 {
            continue;
        }

        // Left
        for x in (cx - gap - length)..(cx - gap) {
            plot(canvas, stride_px, x, y, color, width as i32, height as i32);
        }
        // Right
        for x in (cx + gap)..(cx + gap + length) {
            plot(canvas, stride_px, x, y, color, width as i32, height as i32);
        }
    }

    // Vertical arms
    for dx in -(thickness / 2)..=(thickness / 2) {
        let x = cx + dx;
        if x < 0 || x >= width as i32 {
            continue;
        }

        // Top
        for y in (cy - gap - length)..(cy - gap) {
            plot(canvas, stride_px, x, y, color, width as i32, height as i32);
        }
        // Bottom
        for y in (cy + gap)..(cy + gap + length) {
            plot(canvas, stride_px, x, y, color, width as i32, height as i32);
        }
    }
}

fn plot(canvas: &mut [u8], stride_px: usize, x: i32, y: i32, color: u32, width: i32, height: i32) {
    if x < 0 || y < 0 || x >= width || y >= height {
        return;
    }

    let idx = (y as usize) * stride_px + x as usize;
    let start = idx * 4;
    canvas[start..start + 4].copy_from_slice(&color.to_le_bytes());
}

fn premultiplied_argb(color: &Rgba) -> u32 {
    let a = color.a as u32;
    let r = (color.r as u32 * a + 127) / 255;
    let g = (color.g as u32 * a + 127) / 255;
    let b = (color.b as u32 * a + 127) / 255;

    (a << 24) | (r << 16) | (g << 8) | b
}

impl CompositorHandler for App {
    fn scale_factor_changed(
        &mut self,
        _: &Connection,
        _: &QueueHandle<Self>,
        surface: &wl_surface::WlSurface,
        new_factor: i32,
    ) {
        for entry in self.surfaces.values_mut() {
            if entry.layer.wl_surface() == surface {
                entry.scale = new_factor.max(1);
            }
        }
    }

    fn transform_changed(
        &mut self,
        _: &Connection,
        _: &QueueHandle<Self>,
        _: &wl_surface::WlSurface,
        _: wl_output::Transform,
    ) {
    }

    fn frame(&mut self, _: &Connection, _: &QueueHandle<Self>, _: &wl_surface::WlSurface, _: u32) {
        // Static overlay; no continuous redraw needed.
    }
}

impl OutputHandler for App {
    fn output_state(&mut self) -> &mut OutputState {
        &mut self.output_state
    }

    fn new_output(&mut self, _: &Connection, qh: &QueueHandle<Self>, output: wl_output::WlOutput) {
        self.ensure_surface_for_output(qh, output);
    }

    fn update_output(
        &mut self,
        _: &Connection,
        qh: &QueueHandle<Self>,
        output: wl_output::WlOutput,
    ) {
        self.ensure_surface_for_output(qh, output);
    }

    fn output_destroyed(
        &mut self,
        _: &Connection,
        _: &QueueHandle<Self>,
        output: wl_output::WlOutput,
    ) {
        let id = output.id().protocol_id();
        self.surfaces.remove(&id);
    }
}

impl LayerShellHandler for App {
    fn closed(&mut self, _: &Connection, _: &QueueHandle<Self>, layer: &LayerSurface) {
        self.surfaces.retain(|_, entry| entry.layer != *layer);
    }

    fn configure(
        &mut self,
        _: &Connection,
        _: &QueueHandle<Self>,
        layer: &LayerSurface,
        configure: LayerSurfaceConfigure,
        _: u32,
    ) {
        let target = self
            .surfaces
            .iter()
            .find(|(_, entry)| entry.layer == *layer)
            .map(|(id, _)| *id);

        if let Some(id) = target {
            if let Some(entry) = self.surfaces.get_mut(&id) {
                render_crosshair(entry, configure, &self.config);
            }
        }
    }
}

impl ShmHandler for App {
    fn shm_state(&mut self) -> &mut Shm {
        &mut self.shm
    }
}

delegate_compositor!(App);
delegate_output!(App);
delegate_shm!(App);
delegate_layer!(App);
delegate_registry!(App);

impl ProvidesRegistryState for App {
    fn registry(&mut self) -> &mut RegistryState {
        &mut self.registry_state
    }

    registry_handlers![OutputState];
}
