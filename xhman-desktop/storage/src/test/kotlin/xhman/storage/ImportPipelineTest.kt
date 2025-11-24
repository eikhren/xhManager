package xhman.storage

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.nio.file.Files
import java.nio.file.Path

class ImportPipelineTest {
    @Test
    fun `scan emits assets for allowed extensions`() = runBlocking {
        val tempDir = Files.createTempDirectory("xhman-import")
        val file = tempDir.resolve("sample.png")
        Files.writeString(file, "fake")
        val root = xhman.core.AssetRoot(id = "root", path = tempDir.toString(), displayName = "tmp")

        val pipeline = ImportPipeline { "id-${it.fileName}" }
        val events = pipeline.scan(root).toList()

        assertTrue(events.any { it is ImportEvent.AssetFound })
        val completed = events.last() as ImportEvent.Completed
        assertEquals(root.id, completed.root.id)
    }
}
