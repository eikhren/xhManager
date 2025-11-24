package xhman.ui.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xhman.core.ConfigIntent
import xhman.core.Profile
import xhman.core.ProfileIntent
import xhman.core.ProfileState
import xhman.storage.ProfileStore
import java.util.UUID

class ProfileVM(
    private val profiles: ProfileStore,
    private val configVM: ConfigVM,
    private val libraryVM: LibraryVM,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val state = MutableStateFlow(ProfileState())
    val stateFlow: StateFlow<ProfileState> = state.asStateFlow()

    init {
        scope.launch {
            profiles.profiles().collectLatest { entries ->
                state.update { current ->
                    current.copy(
                        profiles = entries,
                        activeProfileId = current.activeProfileId?.takeIf { id -> entries.any { it.id == id } },
                    )
                }
            }
        }
    }

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            is ProfileIntent.Save -> saveProfile(intent.name)
            is ProfileIntent.Load -> loadProfile(intent.profileId)
            is ProfileIntent.Delete -> scope.launch { profiles.delete(intent.profileId) }
            ProfileIntent.ClearSelection -> libraryVM.onIntent(xhman.core.LibraryIntent.SelectAsset(null))
        }
    }

    private fun saveProfile(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            state.update { it.copy(lastError = "Profile name cannot be empty") }
            return
        }

        scope.launch {
            val libraryState = libraryVM.state.value
            val config = configVM.stateFlow.value.config
            val id = UUID.nameUUIDFromBytes(trimmed.lowercase().toByteArray()).toString()
            val profile = Profile(
                id = id,
                name = trimmed,
                config = config,
                selectedAssetIds = libraryState.selectedAssetId?.let { listOf(it) } ?: emptyList(),
                rootAssociations = libraryState.roots.map { it.id },
                version = (profiles.find(id)?.version ?: 0) + 1,
            )
            profiles.save(profile)
            state.update { it.copy(activeProfileId = profile.id, lastError = null) }
        }
    }

    private fun loadProfile(profileId: String) {
        scope.launch {
            val profile = profiles.find(profileId)
            if (profile == null) {
                state.update { it.copy(lastError = "Profile not found") }
                return@launch
            }

            configVM.onIntent(ConfigIntent.Reset(profile.config))
            libraryVM.onIntent(xhman.core.LibraryIntent.SelectAsset(profile.selectedAssetIds.firstOrNull()))
            state.update { it.copy(activeProfileId = profile.id, lastError = null) }
        }
    }
}
