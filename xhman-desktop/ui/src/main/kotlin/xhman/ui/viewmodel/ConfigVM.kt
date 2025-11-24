package xhman.ui.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import xhman.core.Config
import xhman.core.ConfigIntent
import xhman.core.ConfigState
import xhman.core.ConfigValidator

class ConfigVM(
    initial: Config = Config(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val state = MutableStateFlow(ConfigState(ConfigValidator.normalize(initial)))
    val stateFlow: StateFlow<ConfigState> = state.asStateFlow()

    fun onIntent(intent: ConfigIntent) {
        when (intent) {
            is ConfigIntent.UpdateScale -> update { it.copy(scale = intent.value) }
            is ConfigIntent.ToggleIntrinsic -> update { it.copy(intrinsicScale = intent.enabled) }
            is ConfigIntent.UpdateOffset -> update { it.copy(offsetX = intent.x, offsetY = intent.y) }
            is ConfigIntent.UpdateRotation -> update { it.copy(rotationDeg = intent.degrees) }
            is ConfigIntent.UpdateOpacity -> update { it.copy(opacity = intent.fraction) }
            is ConfigIntent.UpdateTint -> update { it.copy(tint = intent.color, tintMode = intent.mode) }
            is ConfigIntent.UpdateOutline -> update {
                it.copy(outlineWidth = intent.width, outlineColor = intent.color)
            }
            is ConfigIntent.UpdateGlow -> update { it.copy(glowWidth = intent.width) }
            is ConfigIntent.UpdateBlend -> update { it.copy(blendMode = intent.mode) }
            is ConfigIntent.Reset -> reset(intent.toDefaults)
        }
    }

    private fun update(mutator: (Config) -> Config) {
        state.update { current ->
            val next = ConfigValidator.normalize(mutator(current.config))
            current.copy(config = next)
        }
    }

    private fun reset(config: Config) {
        state.update { it.copy(config = ConfigValidator.normalize(config)) }
    }
}
