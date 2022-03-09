package com.wwt.jetflowlibrary.navigation

import android.app.Application
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import kotlin.properties.Delegates

class NavigationComponentEntry<T>(
    private val entry: NavigationEntry<T>,
    private val saveableStateHolder: SaveableStateHolder,
    private val viewModelStore: ViewModelStore,
    private val application: Application?
) : ViewModelStoreOwner,
    LifecycleOwner,
    SavedStateRegistryOwner,
    HasDefaultViewModelProviderFactory {

    val id get() = entry.id

    val destination get() = entry.destination

    private val lifecycleRegistry = LifecycleRegistry(this)

    internal var navHostLifecycleState by Delegates.observable(Lifecycle.State.INITIALIZED) { _, _, _ ->
        updateLifecycleRegistry()
    }

    internal var maxLifecycleState by Delegates.observable(Lifecycle.State.INITIALIZED) { _, _, _ ->
        updateLifecycleRegistry()
    }

    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    internal val savedStateProvider = SavedStateRegistry.SavedStateProvider {
        Bundle().also { bundle ->
            savedStateRegistryController.performSave(bundle)
        }
    }

    private val defaultFactory by lazy {
        SavedStateViewModelFactory(application, this, null)
    }

    override fun getViewModelStore() = viewModelStore

    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    private fun updateLifecycleRegistry() {
        val currentState = lifecycleRegistry.currentState
        val newState = minOf(maxLifecycleState, navHostLifecycleState)

        if (currentState != newState) {
            if (currentState == Lifecycle.State.DESTROYED) {
                error("Moving from DESTROYED state is not allowed")
            }
            if (currentState == Lifecycle.State.INITIALIZED && newState == Lifecycle.State.DESTROYED) {
                // Lifecycle should not move straight from INITIALIZED to DESTROYED,
                // only INITIALIZED -> STARTED -> DESTROYED is allowed
                lifecycleRegistry.currentState = Lifecycle.State.STARTED
            }
            lifecycleRegistry.currentState = newState
        }
    }

    override fun getSavedStateRegistry(): SavedStateRegistry =
        savedStateRegistryController.savedStateRegistry

    internal fun restoreState(savedState: Bundle) {
        savedStateRegistryController.performRestore(savedState)
    }

    override fun getDefaultViewModelProviderFactory() = defaultFactory

    @Composable
    internal fun SaveableStateProvider(content: @Composable () -> Unit) =
        saveableStateHolder.SaveableStateProvider(
            key = id,
            content = content
        )

}

@Composable
internal fun <T> NavigationComponentEntry<T>.ComponentProvider(
    content: @Composable () -> Unit
) = CompositionLocalProvider(
    LocalViewModelStoreOwner provides this,
    LocalLifecycleOwner provides this,
    LocalSavedStateRegistryOwner provides this
) {
    this.SaveableStateProvider(content)
}
