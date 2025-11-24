package xhman.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import xhman.core.Profile
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

interface ProfilePersistence {
    suspend fun load(): List<Profile>
    suspend fun saveAll(items: List<Profile>)
}

class JsonProfilePersistence(
    private val file: Path = defaultPath(),
    private val json: Json = Json { prettyPrint = true; ignoreUnknownKeys = true },
) : ProfilePersistence {
    override suspend fun load(): List<Profile> = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext emptyList()
        file.inputStream().use { stream ->
            val doc = json.decodeFromString(ProfileDocument.serializer(), stream.readBytes().toString(Charsets.UTF_8))
            doc.profiles
        }
    }

    override suspend fun saveAll(items: List<Profile>) = withContext(Dispatchers.IO) {
        file.parent?.createDirectories()
        val doc = ProfileDocument(version = 1, profiles = items)
        file.outputStream().use { stream ->
            stream.write(json.encodeToString(ProfileDocument.serializer(), doc).toByteArray())
        }
    }

    companion object {
        private fun defaultPath(): Path {
            val home = System.getProperty("user.home")
            return Path.of(home, ".config", "xhman", "profiles.json")
        }
    }
}

@Serializable
private data class ProfileDocument(
    val version: Int = 1,
    val profiles: List<Profile> = emptyList(),
)
