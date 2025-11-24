package xhman.core

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

@Serializable
data class AssetRoot(
    val id: String,
    val path: String,
    val displayName: String,
    val isActive: Boolean = true,
    val lastSeenEpochMillis: Long? = null,
)

@Serializable
data class Asset(
    val id: String,
    val rootId: String,
    val path: String,
    val label: String,
    val width: Int? = null,
    val height: Int? = null,
    val sizeBytes: Long? = null,
)

@Serializable
data class Profile(
    val id: String,
    val name: String,
    val config: Config = Config(),
    val selectedAssetIds: List<String> = emptyList(),
    val rootAssociations: List<String> = emptyList(),
    val version: Int = 1,
)

@Serializable
data class Config(
    val scale: Float = 1f,
    val intrinsicScale: Boolean = false,
    val offsetX: Int = 0,
    val offsetY: Int = 0,
    val rotationDeg: Float = 0f,
    val opacity: Float = 1f,
    val tint: RgbaColor = RgbaColor.white(),
    val tintMode: TintMode = TintMode.Multiply,
    val glowWidth: Float = 0f,
    val outlineWidth: Float = 0f,
    val outlineColor: RgbaColor = RgbaColor.black(),
    val blendMode: BlendMode = BlendMode.SrcOver,
)

@Serializable
data class RgbaColor(
    val r: UByte,
    val g: UByte,
    val b: UByte,
    val a: UByte = 0xFFu,
) {
    fun withAlpha(alpha: UByte): RgbaColor = copy(a = alpha)

    companion object {
        fun white(alpha: UByte = 0xFFu) = RgbaColor(0xFFu, 0xFFu, 0xFFu, alpha)
        fun black(alpha: UByte = 0xFFu) = RgbaColor(0x00u, 0x00u, 0x00u, alpha)
        fun transparent() = RgbaColor(0x00u, 0x00u, 0x00u, 0x00u)
    }
}

enum class TintMode {
    Multiply,
    Screen,
    Overlay,
    Replace,
}

enum class BlendMode {
    SrcOver,
    DstOver,
    Lighten,
    Darken,
    Add,
}

object ConfigValidator {
    const val MIN_SCALE = 0.1f
    const val MAX_SCALE = 5f
    const val MIN_OPACITY = 0.1f
    const val MAX_OPACITY = 1f

    fun normalize(config: Config): Config {
        val clampedScale = config.scale.coerceIn(MIN_SCALE, MAX_SCALE)
        val clampedOpacity = config.opacity.coerceIn(MIN_OPACITY, MAX_OPACITY)
        val glow = max(0f, config.glowWidth)
        val outline = max(0f, config.outlineWidth)

        return config.copy(
            scale = clampedScale,
            opacity = clampedOpacity,
            glowWidth = glow,
            outlineWidth = outline,
            rotationDeg = wrapRotation(config.rotationDeg),
        )
    }

    fun wrapRotation(value: Float): Float {
        val normalized = value % 360f
        return if (normalized < 0) normalized + 360f else normalized
    }
}

fun Config.cacheFingerprint(): Int = ConfigValidator.normalize(this).hashCode()

fun Float.scaleLabel(): String = "${formatScale()}x"

fun Float.formatScale(): String = formatToRange(ConfigValidator.MIN_SCALE, ConfigValidator.MAX_SCALE)

fun Float.formatToRange(min: Float, max: Float): String {
    val clamped = min(max(this, min), max)
    return String.format("%.2f", clamped)
}
