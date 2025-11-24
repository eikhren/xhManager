package xhman

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import xhman.storage.ImportPipeline
import xhman.storage.LibraryRepository
import xhman.storage.ProfileStore
import xhman.ui.XhManApp
import xhman.ui.viewmodel.ConfigVM
import xhman.ui.viewmodel.LibraryVM
import xhman.ui.viewmodel.ProfileVM
import androidx.compose.runtime.remember
import xhman.render.Renderer
import xhman.storage.JsonProfilePersistence

fun main() = application {
    val state = rememberWindowState()
    val importer = remember { ImportPipeline() }
    val libraryRepository = remember { LibraryRepository() }
    val libraryVM = remember { LibraryVM(libraryRepository, importer) }
    val configVM = remember { ConfigVM() }
    val profileStore = remember { ProfileStore(JsonProfilePersistence()) }
    val profileVM = remember { ProfileVM(profileStore, configVM, libraryVM) }
    val renderer = remember { Renderer() }

    Window(
        onCloseRequest = ::exitApplication,
        state = state,
        title = "xhMan Desktop",
    ) {
        XhManApp(
            libraryVM = libraryVM,
            configVM = configVM,
            profileVM = profileVM,
            renderer = renderer,
        )
    }
}
