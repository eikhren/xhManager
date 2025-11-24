package xhman.core

data class LibraryState(
    val roots: List<AssetRoot> = emptyList(),
    val assets: List<Asset> = emptyList(),
    val selectedAssetId: String? = null,
    val isScanning: Boolean = false,
)

data class ConfigState(
    val config: Config = Config(),
)

data class ProfileState(
    val profiles: List<Profile> = emptyList(),
    val activeProfileId: String? = null,
    val lastError: String? = null,
)
