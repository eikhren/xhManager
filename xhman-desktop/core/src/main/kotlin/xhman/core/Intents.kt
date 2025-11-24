package xhman.core

sealed interface LibraryIntent {
    data object Refresh : LibraryIntent
    data class AddRoot(val path: String, val displayName: String? = null) : LibraryIntent
    data class RemoveRoot(val rootId: String) : LibraryIntent
    data class ToggleRoot(val rootId: String, val isActive: Boolean) : LibraryIntent
    data class SelectAsset(val assetId: String?) : LibraryIntent
}

sealed interface ConfigIntent {
    data class UpdateScale(val value: Float) : ConfigIntent
    data class ToggleIntrinsic(val enabled: Boolean) : ConfigIntent
    data class UpdateOffset(val x: Int, val y: Int) : ConfigIntent
    data class UpdateRotation(val degrees: Float) : ConfigIntent
    data class UpdateOpacity(val fraction: Float) : ConfigIntent
    data class UpdateTint(val color: RgbaColor, val mode: TintMode) : ConfigIntent
    data class UpdateOutline(val width: Float, val color: RgbaColor) : ConfigIntent
    data class UpdateGlow(val width: Float) : ConfigIntent
    data class UpdateBlend(val mode: BlendMode) : ConfigIntent
    data class Reset(val toDefaults: Config = Config()) : ConfigIntent
}

sealed interface ProfileIntent {
    data class Save(val name: String) : ProfileIntent
    data class Load(val profileId: String) : ProfileIntent
    data class Delete(val profileId: String) : ProfileIntent
    data object ClearSelection : ProfileIntent
}
