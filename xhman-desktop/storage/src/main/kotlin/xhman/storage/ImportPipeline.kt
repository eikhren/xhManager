package xhman.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import xhman.core.Asset
import xhman.core.AssetRoot
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

data class ImportConfig(
    val allowedExtensions: Set<String> = setOf("png", "bmp", "svg"),
    val maxBytes: Long? = null,
)

sealed interface ImportEvent {
    data class RootIndexed(val root: AssetRoot) : ImportEvent
    data class AssetFound(val asset: Asset) : ImportEvent
    data class RootMissing(val root: AssetRoot) : ImportEvent
    data class Completed(val root: AssetRoot) : ImportEvent
}

class ImportPipeline(
    private val idBuilder: (Path) -> String = { path -> stableAssetId(path) },
) {
    fun scan(root: AssetRoot, config: ImportConfig = ImportConfig()): Flow<ImportEvent> = flow {
        emit(ImportEvent.RootIndexed(root))
        val rootPath = Path.of(root.path)
        if (!Files.exists(rootPath)) {
            emit(ImportEvent.RootMissing(root))
            return@flow
        }

        Files.walk(rootPath).use { stream ->
            val iter = stream.iterator()
            while (iter.hasNext()) {
                val path = iter.next()
                if (!path.isRegularFile()) continue

                val ext = extension(path)
                if (ext !in config.allowedExtensions) continue

                val size = runCatching { Files.size(path) }.getOrNull()
                if (config.maxBytes != null && (size ?: Long.MAX_VALUE) > config.maxBytes) {
                    continue
                }

                emit(
                    ImportEvent.AssetFound(
                        Asset(
                            id = idBuilder(path),
                            rootId = root.id,
                            path = path.toAbsolutePath().normalize().toString(),
                            label = path.name,
                            width = null,
                            height = null,
                            sizeBytes = size,
                        ),
                    ),
                )
            }
        }
        emit(ImportEvent.Completed(root))
    }.flowOn(Dispatchers.IO)
}

fun stableAssetId(path: Path): String {
    val canonical = path.toAbsolutePath().normalize().toString()
    return UUID.nameUUIDFromBytes(canonical.toByteArray()).toString()
}

private fun extension(path: Path): String =
    path.fileName.toString().substringAfterLast('.', missingDelimiterValue = "").lowercase()
