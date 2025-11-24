package xhman.storage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import xhman.core.Profile
import kotlinx.coroutines.launch

class ProfileStore(
    private val persistence: ProfilePersistence? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val profiles = MutableStateFlow<List<Profile>>(emptyList())

    init {
        scope.launch {
            val loaded = persistence?.load().orEmpty()
            if (loaded.isNotEmpty()) {
                profiles.value = loaded
            }
        }
    }

    fun profiles(): StateFlow<List<Profile>> = profiles.asStateFlow()

    suspend fun save(profile: Profile) {
        profiles.update { current ->
            current
                .filterNot { it.id == profile.id }
                .plus(profile)
                .sortedBy { it.name.lowercase() }
        }
        persist()
    }

    suspend fun delete(profileId: String) {
        profiles.update { current -> current.filterNot { it.id == profileId } }
        persist()
    }

    fun find(profileId: String): Profile? = profiles.value.firstOrNull { it.id == profileId }

    private suspend fun persist() {
        persistence?.saveAll(profiles.value)
    }
}
