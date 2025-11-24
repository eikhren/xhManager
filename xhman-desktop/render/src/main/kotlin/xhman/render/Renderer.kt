package xhman.render

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xhman.core.Config
import xhman.core.ConfigValidator
import xhman.core.cacheFingerprint
import xhman.core.BlendMode as XBlendMode
import xhman.core.RgbaColor
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.Color
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorInfo
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.Image as SkImage
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Surface
import java.util.LinkedHashMap
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

data class RenderKey(val uri: String, val configHash: Int)

class RenderCache(private val maxEntries: Int = 32) {
    private val entries =
        object : LinkedHashMap<RenderKey, ImageBitmap>(maxEntries, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<RenderKey, ImageBitmap>?): Boolean {
                return size > maxEntries
            }
        }

    fun get(key: RenderKey): ImageBitmap? = synchronized(entries) { entries[key] }

    fun put(key: RenderKey, value: ImageBitmap) {
        synchronized(entries) { entries[key] = value }
    }

    fun clear() = synchronized(entries) { entries.clear() }
}

class Renderer(private val cache: RenderCache = RenderCache()) {
    suspend fun renderCrosshair(
        uri: String,
        source: ImageBitmap,
        config: Config,
    ): ImageBitmap = withContext(Dispatchers.Default) {
        val normalized = ConfigValidator.normalize(config)
        val key = RenderKey(uri, normalized.cacheFingerprint())
        cache.get(key) ?: renderInternal(source, normalized).also { cache.put(key, it) }
    }

    private fun renderInternal(
        source: ImageBitmap,
        config: Config,
    ): ImageBitmap {
        val srcBitmap = source.asSkiaBitmap()
        val srcImage = SkImage.makeFromBitmap(srcBitmap)
        val scale = if (config.intrinsicScale) 1f else config.scale
        val scaledWidth = (srcBitmap.width * scale).roundToInt().coerceAtLeast(1)
        val scaledHeight = (srcBitmap.height * scale).roundToInt().coerceAtLeast(1)

        val radians = Math.toRadians(config.rotationDeg.toDouble())
        val cos = abs(cos(radians))
        val sin = abs(sin(radians))
        val targetWidth = ceil(scaledWidth * cos + scaledHeight * sin).toInt().coerceAtLeast(1)
        val targetHeight = ceil(scaledWidth * sin + scaledHeight * cos).toInt().coerceAtLeast(1)

        val colorInfo = ColorInfo(
            ColorType.RGBA_8888,
            ColorAlphaType.PREMUL,
            ColorSpace.sRGB,
        )
        val surface = Surface.makeRaster(ImageInfo(colorInfo, targetWidth, targetHeight))
        val canvas = surface.canvas

        // Center the image
        canvas.translate(targetWidth / 2f, targetHeight / 2f)
        canvas.rotate(config.rotationDeg)
        canvas.scale(scale, scale)
        canvas.translate(-srcBitmap.width / 2f, -srcBitmap.height / 2f)

        // Glow pass
        if (config.glowWidth > 0f) {
            val glowPaint = Paint().apply {
                color = config.tint.withAlpha((config.opacity * 255).roundToInt().coerceIn(0, 255).toUByte()).toSkiaColor()
                imageFilter = ImageFilter.makeBlur(config.glowWidth, config.glowWidth, FilterTileMode.CLAMP, null, null)
                blendMode = BlendMode.SRC_OVER
            }
            canvas.drawImage(srcImage, 0f, 0f, glowPaint)
        }

        // Outline pass: dilate mask then draw with outline color.
        if (config.outlineWidth > 0f) {
            val outlinePaint = Paint().apply {
                color = config.outlineColor.toSkiaColor()
                imageFilter = ImageFilter.makeDilate(config.outlineWidth, config.outlineWidth, null, null)
                blendMode = BlendMode.SRC_OVER
            }
            canvas.drawImage(srcImage, 0f, 0f, outlinePaint)
        }

        val mainPaint = Paint().apply {
            color = Color.makeARGB((config.opacity * 255).roundToInt().coerceIn(0, 255), 255, 255, 255)
            colorFilter = tintFilter(config.tint, config.tintMode)
            blendMode = mapBlend(config.blendMode)
        }

        canvas.drawImage(srcImage, 0f, 0f, mainPaint)
        return surface.makeImageSnapshot().toComposeImageBitmap()
    }

    private fun tintFilter(color: RgbaColor, mode: xhman.core.TintMode) =
        when (mode) {
            xhman.core.TintMode.Multiply -> org.jetbrains.skia.ColorFilter.makeBlend(color.toSkiaColor(), BlendMode.MULTIPLY)
            xhman.core.TintMode.Screen -> org.jetbrains.skia.ColorFilter.makeBlend(color.toSkiaColor(), BlendMode.SCREEN)
            xhman.core.TintMode.Overlay -> org.jetbrains.skia.ColorFilter.makeBlend(color.toSkiaColor(), BlendMode.OVERLAY)
            xhman.core.TintMode.Replace -> org.jetbrains.skia.ColorFilter.makeBlend(color.toSkiaColor(), BlendMode.SRC_ATOP)
        }
}

private fun mapBlend(blend: XBlendMode): BlendMode =
    when (blend) {
        XBlendMode.SrcOver -> BlendMode.SRC_OVER
        XBlendMode.DstOver -> BlendMode.DST_OVER
        XBlendMode.Lighten -> BlendMode.LIGHTEN
        XBlendMode.Darken -> BlendMode.DARKEN
        XBlendMode.Add -> BlendMode.PLUS
    }

private fun RgbaColor.toSkiaColor(): Int =
    Color.makeARGB(a.toInt(), r.toInt(), g.toInt(), b.toInt())
