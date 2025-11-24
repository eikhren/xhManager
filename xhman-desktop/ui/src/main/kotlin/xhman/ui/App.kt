package xhman.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import xhman.core.ConfigIntent
import xhman.core.LibraryIntent
import xhman.core.RgbaColor
import xhman.core.ProfileIntent
import xhman.render.Renderer
import xhman.ui.viewmodel.ConfigVM
import xhman.ui.viewmodel.LibraryVM
import xhman.ui.viewmodel.ProfileVM
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image as SkiaImage
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Bitmap
import kotlin.math.max
import kotlin.math.roundToInt
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.swing.JFileChooser
import java.nio.file.Files
import java.nio.file.Path

@Composable
fun XhManApp(
    libraryVM: LibraryVM,
    configVM: ConfigVM,
    profileVM: ProfileVM,
    renderer: Renderer,
) {
    val libraryState by libraryVM.state.collectAsState()
    val configState by configVM.stateFlow.collectAsState()
    val profileState by profileVM.stateFlow.collectAsState()

    MaterialTheme {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(modifier = Modifier.weight(0.35f).fillMaxHeight()) {
                LibraryPanel(
                    onAddRoot = { path -> libraryVM.onIntent(LibraryIntent.AddRoot(path)) },
                    onRefresh = { libraryVM.onIntent(LibraryIntent.Refresh) },
                    onToggle = { id, active -> libraryVM.onIntent(LibraryIntent.ToggleRoot(id, active)) },
                    onRemove = { id -> libraryVM.onIntent(LibraryIntent.RemoveRoot(id)) },
                    onSelectAsset = { id -> libraryVM.onIntent(LibraryIntent.SelectAsset(id)) },
                    isScanning = libraryState.isScanning,
                    roots = libraryState.roots,
                    assets = libraryState.assets,
                    selectedAssetId = libraryState.selectedAssetId,
                )
            }
            Column(modifier = Modifier.weight(0.35f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val selectedAsset = libraryState.assets.firstOrNull { it.id == libraryState.selectedAssetId }
                Box(modifier = Modifier.weight(1f, fill = true)) {
                    PreviewPane(
                        selected = selectedAsset,
                        config = configState.config,
                        renderer = renderer,
                    )
                }
                Box(modifier = Modifier.weight(1f, fill = true)) {
                    ProfilePanel(
                        profiles = profileState.profiles,
                        activeProfileId = profileState.activeProfileId,
                        error = profileState.lastError,
                        onSave = { profileVM.onIntent(ProfileIntent.Save(it)) },
                        onLoad = { id -> profileVM.onIntent(ProfileIntent.Load(id)) },
                        onDelete = { id -> profileVM.onIntent(ProfileIntent.Delete(id)) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Column(modifier = Modifier.weight(0.3f).fillMaxHeight()) {
                ConfigPanel(
                    scale = configState.config.scale,
                    intrinsic = configState.config.intrinsicScale,
                    rotation = configState.config.rotationDeg,
                    opacity = configState.config.opacity,
                    glow = configState.config.glowWidth,
                    outline = configState.config.outlineWidth,
                    tintColor = configState.config.tint,
                    outlineColor = configState.config.outlineColor,
                    onScale = { configVM.onIntent(ConfigIntent.UpdateScale(it)) },
                    onIntrinsic = { configVM.onIntent(ConfigIntent.ToggleIntrinsic(it)) },
                    onRotation = { configVM.onIntent(ConfigIntent.UpdateRotation(it)) },
                    onOpacity = { configVM.onIntent(ConfigIntent.UpdateOpacity(it)) },
                    onGlow = { configVM.onIntent(ConfigIntent.UpdateGlow(it)) },
                    onOutline = { configVM.onIntent(ConfigIntent.UpdateOutline(it, configState.config.outlineColor)) },
                    onTintColor = { color -> configVM.onIntent(ConfigIntent.UpdateTint(color, configState.config.tintMode)) },
                    onOutlineColor = { color -> configVM.onIntent(ConfigIntent.UpdateOutline(configState.config.outlineWidth, color)) },
                    onReset = { configVM.onIntent(ConfigIntent.Reset()) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun LibraryPanel(
    onAddRoot: (String) -> Unit,
    onRefresh: () -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onRemove: (String) -> Unit,
    onSelectAsset: (String?) -> Unit,
    isScanning: Boolean,
    roots: List<xhman.core.AssetRoot>,
    assets: List<xhman.core.Asset>,
    selectedAssetId: String?,
) {
    var path by remember { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Import Roots", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = path,
                onValueChange = { path = it },
                label = { Text("Folder path") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { if (path.isNotBlank()) onAddRoot(path.trim()) }) { Text("Add root") }
                OutlinedButton(onClick = {
                    browseForFolder()?.let { chosen -> path = chosen }
                }) { Text("Browse") }
                OutlinedButton(onClick = onRefresh) { Text(if (isScanning) "Scanning..." else "Rescan") }
            }
            Divider()
            Text("Roots", style = MaterialTheme.typography.titleSmall)
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(roots, key = { it.id }) { root ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(root.displayName, style = MaterialTheme.typography.bodyMedium)
                            Text(root.path, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Switch(checked = root.isActive, onCheckedChange = { onToggle(root.id, it) })
                            OutlinedButton(onClick = { onRemove(root.id) }) { Text("Remove") }
                        }
                    }
                }
            }
            Divider()
            Text("Assets", style = MaterialTheme.typography.titleSmall)
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(assets, key = { it.id }) { asset ->
                    AssetRow(asset, isSelected = asset.id == selectedAssetId, onSelect = onSelectAsset)
                }
            }
        }
    }
}

@Composable
private fun PreviewPane(
    selected: xhman.core.Asset?,
    config: xhman.core.Config,
    renderer: Renderer,
) {
    Card(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Preview", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            PreviewCanvas(selected, config, renderer)
        }
    }
}

@Composable
private fun ConfigPanel(
    scale: Float,
    intrinsic: Boolean,
    rotation: Float,
    opacity: Float,
    glow: Float,
    outline: Float,
    tintColor: RgbaColor,
    outlineColor: RgbaColor,
    onScale: (Float) -> Unit,
    onIntrinsic: (Boolean) -> Unit,
    onRotation: (Float) -> Unit,
    onOpacity: (Float) -> Unit,
    onGlow: (Float) -> Unit,
    onOutline: (Float) -> Unit,
    onTintColor: (RgbaColor) -> Unit,
    onOutlineColor: (RgbaColor) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Config", style = MaterialTheme.typography.titleMedium)

            LabeledSlider(
                label = "Scale (${String.format("%.2f", scale)}x)",
                value = scale,
                valueRange = 0.1f..5f,
                onValueChange = onScale,
                enabled = !intrinsic,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Use intrinsic scale")
                Switch(checked = intrinsic, onCheckedChange = onIntrinsic)
            }
            LabeledSlider(
                label = "Rotation (${rotation.toInt()}°)",
                value = rotation,
                valueRange = 0f..360f,
                onValueChange = onRotation,
            )
            LabeledSlider(
                label = "Opacity (${(opacity * 100).toInt()}%)",
                value = opacity,
                valueRange = 0.1f..1f,
                onValueChange = onOpacity,
            )
            ColorField(
                label = "Tint",
                color = tintColor,
                onColor = onTintColor,
            )
            ColorField(
                label = "Outline color",
                color = outlineColor,
                onColor = onOutlineColor,
            )
            LabeledSlider(
                label = "Glow width (${glow.toInt()}px) (coming soon)",
                value = glow,
                valueRange = 0f..32f,
                onValueChange = onGlow,
                enabled = false,
            )
            LabeledSlider(
                label = "Outline width (${outline.toInt()}px) (coming soon)",
                value = outline,
                valueRange = 0f..16f,
                onValueChange = onOutline,
                enabled = false,
            )
            Button(onClick = onReset, modifier = Modifier.align(Alignment.End)) {
                Text("Reset defaults")
            }
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    enabled: Boolean = true,
) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange, enabled = enabled)
    }
}

@Composable
private fun ColorField(
    label: String,
    color: RgbaColor,
    onColor: (RgbaColor) -> Unit,
) {
    var hex by remember(color) { mutableStateOf(color.toHex()) }
    LaunchedEffect(color) { hex = color.toHex() }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color.toComposeColor(), shape = MaterialTheme.shapes.small)
            )
            OutlinedTextField(
                value = hex,
                onValueChange = {
                    hex = it
                    parseHexColor(it)?.let(onColor)
                },
                singleLine = true,
                label = { Text("Hex #RRGGBB[AA]") },
                modifier = Modifier.weight(1f),
            )
        }
        SwatchRow(onColor = onColor)
    }
}

@Composable
private fun SwatchRow(onColor: (RgbaColor) -> Unit) {
    val swatches = listOf(
        RgbaColor(0xFFu, 0xFFu, 0xFFu),
        RgbaColor(0x00u, 0x00u, 0x00u),
        RgbaColor(0xFFu, 0x00u, 0x00u),
        RgbaColor(0x00u, 0x80u, 0xFFu),
        RgbaColor(0xFFu, 0xC1u, 0x07u),
        RgbaColor(0x7Du, 0x5Cu, 0xFFu),
        RgbaColor(0x00u, 0xA3u, 0x68u),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        swatches.forEach { swatch ->
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(swatch.toComposeColor(), shape = MaterialTheme.shapes.small)
                    .clickable { onColor(swatch) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfilePanel(
    profiles: List<xhman.core.Profile>,
    activeProfileId: String?,
    error: String?,
    onSave: (String) -> Unit,
    onLoad: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf("") }
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Profiles", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Profile name") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = { onSave(name) }) { Text("Save profile") }
            if (error != null) {
                Text(error, color = MaterialTheme.colorScheme.error)
            }
            Divider()
            LazyColumn(
                modifier = Modifier.fillMaxWidth().height(180.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(profiles, key = { it.id }) { profile ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(profile.name, style = MaterialTheme.typography.bodyMedium)
                            Text("v${profile.version}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onLoad(profile.id) }) {
                                Text(if (profile.id == activeProfileId) "Active" else "Load")
                            }
                            OutlinedButton(onClick = { onDelete(profile.id) }) { Text("Delete") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewCanvas(
    selected: xhman.core.Asset?,
    config: xhman.core.Config,
    renderer: Renderer,
) {
    var source by remember { mutableStateOf<ImageBitmap?>(null) }
    var rendered by remember { mutableStateOf<ImageBitmap?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selected?.id) {
        loading = true
        error = null
        rendered = null
        source = null
        try {
            if (selected == null) return@LaunchedEffect
            val path = Path.of(selected.path)
            if (!Files.exists(path)) {
                error = "File missing"
                return@LaunchedEffect
            }
            source = withContext(Dispatchers.IO) { loadImage(path) }
        } catch (ce: CancellationException) {
            return@LaunchedEffect
        } catch (err: Throwable) {
            error = err.message ?: "Failed to load"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(selected?.id, source, config) {
        val src = source ?: return@LaunchedEffect
        if (selected == null) return@LaunchedEffect
        loading = true
        error = null
        try {
            rendered = renderer.renderCrosshair(selected.path, src, config)
        } catch (ce: CancellationException) {
            return@LaunchedEffect
        } catch (err: Throwable) {
            error = err.message ?: "Render failed"
        } finally {
            loading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1115))
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            loading -> Text("Rendering...", color = Color.LightGray)
            error != null -> Text(error ?: "Error", color = MaterialTheme.colorScheme.error)
            rendered != null -> Image(bitmap = rendered!!, contentDescription = "Preview")
            selected == null -> Text("Select an asset to preview", color = Color.Gray)
            else -> Text("Loading image...", color = Color.Gray)
        }
    }
}

@Composable
private fun AssetRow(
    asset: xhman.core.Asset,
    isSelected: Boolean,
    onSelect: (String?) -> Unit,
) {
    var thumbnail by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(asset.id) {
        val path = Path.of(asset.path)
        if (!Files.exists(path)) return@LaunchedEffect
        try {
            thumbnail = withContext(Dispatchers.IO) { loadThumbnail(path, 64) }
        } catch (ce: CancellationException) {
            return@LaunchedEffect
        } catch (_: Throwable) {
            // Ignore thumbnail errors for now
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(asset.id) }
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color(0xFF1A1D22), shape = MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center,
        ) {
            if (thumbnail != null) {
                Image(bitmap = thumbnail!!, contentDescription = asset.label)
            } else {
                Text("—", color = Color.Gray)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(asset.label, style = MaterialTheme.typography.bodyMedium)
            Text(asset.path, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        if (isSelected) {
            Text("Selected", color = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun loadImage(path: Path): ImageBitmap {
    val bytes = Files.readAllBytes(path)
    val img = SkiaImage.makeFromEncoded(bytes)
    return img.toComposeImageBitmap()
}

private fun loadThumbnail(path: Path, size: Int): ImageBitmap {
    val bytes = Files.readAllBytes(path)
    val image = SkiaImage.makeFromEncoded(bytes)
    val scale = size.toFloat() / max(image.width, image.height).coerceAtLeast(1)
    val targetWidth = (image.width * scale).roundToInt().coerceAtLeast(1)
    val targetHeight = (image.height * scale).roundToInt().coerceAtLeast(1)

    val bitmap = Bitmap().apply { allocN32Pixels(targetWidth, targetHeight, false) }
    val pixmap = bitmap.peekPixels()
    val scaled =
        if (pixmap != null) image.scalePixels(pixmap, SamplingMode.DEFAULT, true) else false

    return if (scaled) {
        SkiaImage.makeFromBitmap(bitmap).toComposeImageBitmap()
    } else {
        image.toComposeImageBitmap()
    }
}

private fun browseForFolder(): String? {
    return runCatching {
        val chooser = JFileChooser().apply {
            dialogTitle = "Choose folder"
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            isMultiSelectionEnabled = false
            isAcceptAllFileFilterUsed = false
        }
        val result = chooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile?.takeIf { it.isDirectory }?.absolutePath
        } else {
            null
        }
    }.getOrNull()
}

private fun RgbaColor.toComposeColor(): Color =
    Color(
        red = r.toInt() / 255f,
        green = g.toInt() / 255f,
        blue = b.toInt() / 255f,
        alpha = a.toInt() / 255f,
    )

private fun RgbaColor.toHex(): String =
    String.format("#%02X%02X%02X%02X", r.toInt(), g.toInt(), b.toInt(), a.toInt())

private fun parseHexColor(input: String): RgbaColor? {
    val raw = input.removePrefix("#")
    return when (raw.length) {
        6 -> {
            val r = raw.substring(0, 2).toInt(16).toUByte()
            val g = raw.substring(2, 4).toInt(16).toUByte()
            val b = raw.substring(4, 6).toInt(16).toUByte()
            RgbaColor(r, g, b, 0xFFu)
        }
        8 -> {
            val r = raw.substring(0, 2).toInt(16).toUByte()
            val g = raw.substring(2, 4).toInt(16).toUByte()
            val b = raw.substring(4, 6).toInt(16).toUByte()
            val a = raw.substring(6, 8).toInt(16).toUByte()
            RgbaColor(r, g, b, a)
        }
        else -> null
    }
}
