package xhman.ui.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import xhman.core.AssetRoot
import xhman.core.LibraryIntent
import xhman.core.LibraryState
import xhman.storage.ImportEvent
import xhman.storage.ImportPipeline
import xhman.storage.LibraryRepository
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.name

class LibraryVM(
    private val repository: LibraryRepository,
    private val importer: ImportPipeline,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    val state: StateFlow<LibraryState> = repository.state()

    fun onIntent(intent: LibraryIntent) {
        when (intent) {
            LibraryIntent.Refresh -> refreshActiveRoots()
            is LibraryIntent.AddRoot -> addRoot(intent)
            is LibraryIntent.RemoveRoot -> scope.launch { repository.removeRoot(intent.rootId) }
            is LibraryIntent.ToggleRoot -> scope.launch {
                repository.setRootActive(intent.rootId, intent.isActive)
                if (intent.isActive) {
                    state.value.roots.firstOrNull { it.id == intent.rootId }?.let { scan(it) }
                }
            }
            is LibraryIntent.SelectAsset -> scope.launch { repository.selectAsset(intent.assetId) }
        }
    }

    private fun addRoot(intent: LibraryIntent.AddRoot) {
        val path = Path.of(intent.path)
        val id = UUID.nameUUIDFromBytes(path.toAbsolutePath().normalize().toString().toByteArray()).toString()
        val root = AssetRoot(
            id = id,
            path = path.toString(),
            displayName = intent.displayName ?: path.name,
            isActive = true,
        )
        scope.launch {
            repository.addRoot(root)
            scan(root)
        }
    }

    private fun refreshActiveRoots() {
        state.value.roots.filter { it.isActive }.forEach { root ->
            scan(root)
        }
    }

    private fun scan(root: AssetRoot) = scope.launch {
        repository.setScanning(true)
        val collected = mutableListOf<xhman.core.Asset>()

        try {
            importer.scan(root).collect { event ->
                when (event) {
                    is ImportEvent.AssetFound -> collected += event.asset
                    is ImportEvent.RootMissing -> repository.removeRoot(root.id)
                    is ImportEvent.RootIndexed -> Unit
                    is ImportEvent.Completed -> repository.setAssetsForRoot(root.id, collected)
                }
            }
        } finally {
            repository.setScanning(false)
        }
    }
}
