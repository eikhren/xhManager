package xhman.storage

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import xhman.core.Asset
import xhman.core.AssetRoot
import xhman.core.LibraryState

class LibraryRepository {
    private val _state = MutableStateFlow(LibraryState())

    fun state(): StateFlow<LibraryState> = _state.asStateFlow()

    suspend fun addRoot(root: AssetRoot) {
        _state.update { current ->
            if (current.roots.any { it.id == root.id }) {
                current
            } else {
                current.copy(roots = current.roots + root)
            }
        }
    }

    suspend fun removeRoot(rootId: String) {
        _state.update { current ->
            val remainingAssets = current.assets.filterNot { it.rootId == rootId }
            current.copy(
                roots = current.roots.filterNot { it.id == rootId },
                assets = remainingAssets,
                selectedAssetId = current.selectedAssetId.takeIf { id ->
                    remainingAssets.any { it.id == id }
                },
            )
        }
    }

    suspend fun setAssetsForRoot(rootId: String, assetsForRoot: List<Asset>) {
        _state.update { current ->
            val retained = current.assets.filterNot { it.rootId == rootId }
            val merged = (retained + assetsForRoot).distinctBy { it.id }
            current.copy(assets = merged)
        }
    }

    suspend fun setRootActive(rootId: String, active: Boolean) {
        _state.update { current ->
            val updatedRoots = current.roots.map { root ->
                if (root.id == rootId) {
                    root.copy(isActive = active)
                } else {
                    root
                }
            }
            val filteredAssets = if (active) {
                current.assets
            } else {
                current.assets.filterNot { it.rootId == rootId }
            }
            val selected = current.selectedAssetId.takeIf { id ->
                filteredAssets.any { it.id == id }
            }
            current.copy(
                roots = updatedRoots,
                assets = filteredAssets,
                selectedAssetId = selected,
            )
        }
    }

    suspend fun selectAsset(assetId: String?) {
        _state.update { current ->
            current.copy(
                selectedAssetId = assetId?.takeIf { id -> current.assets.any { it.id == id } },
            )
        }
    }

    suspend fun setScanning(scanning: Boolean) {
        _state.update { current -> current.copy(isScanning = scanning) }
    }
}
