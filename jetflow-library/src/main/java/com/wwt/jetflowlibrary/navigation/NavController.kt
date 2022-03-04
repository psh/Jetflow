package com.wwt.jetflowlibrary.navigation

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.MainThread
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.SavedStateHandle
import kotlin.properties.ReadOnlyProperty

/**
 * Creates and remembers a new [NavController] instance. Its backstack will be prefilled with
 * destinations from the [initialBackstack].
 *
 * The order of the items in the list is interpreted as going from the bottom of the backstack
 * to the top. It means that the last item of the list will become the currently displayed item
 * in [NavHost]. The first item of the list will be the last item that can be popped of
 * the backstack.
 *
 * The [initialBackstack] list may be empty.
 *
 * This method uses [rememberSaveable] internally and restores NavController state in case of
 * activity or process recreation.
 */
@Composable
fun <T> rememberNavController(initialBackstack: List<T>) = rememberSaveable {
    navController(initialBackstack = initialBackstack)
}

/**
 * Creates and remembers a new [NavController] instance. Its backstack will contain a single item -
 * [startDestination].
 *
 * This method uses [rememberSaveable] internally and restores NavController state in case of
 * activity or process recreation.
 */
@Composable
fun <T> rememberNavController(startDestination: T) =
    rememberNavController(initialBackstack = listOf(startDestination))

/**
 * Creates a new [NavController] instance. Its backstack will be prefilled with destinations
 * from the [initialBackstack].
 *
 * The order of the items in the list is interpreted as going from the bottom of the backstack
 * to the top. It means that the last item of the list will become the currently displayed item
 * in [NavHost]. The first item of the list will be the last item that can be popped of
 * the backstack.
 *
 * The [initialBackstack] list may be empty.
 */
fun <T> navController(initialBackstack: List<T>) =
    NavController(initialEntries = initialBackstack.map(::navEntry))

/**
 * Creates a new [NavController] instance. Its backstack will contain a single item -
 * [startDestination].
 */
fun <T> navController(startDestination: T) =
    navController(initialBackstack = listOf(startDestination))

/**
 * Returns a property delegate that creates and saves a new [NavController] instance in
 * a receiver [SavedStateHandle]. NavController's backstack will be prefilled with destinations
 * from the [initialBackstack].
 *
 * The order of the items in the list is interpreted as going from the bottom of the backstack
 * to the top. It means that the last item of the list will become the currently displayed item
 * in [NavHost]. The first item of the list will be the last item that can be popped of
 * the backstack.
 *
 * The [initialBackstack] list may be empty.
 *
 * @param key an optional key to use for saving NavController instance inside a SavedStateHandle.
 * By default, the name of the property will be used as a key.
 */
fun <T> SavedStateHandle.navController(
    key: String? = null,
    initialBackstack: List<T>
) = ReadOnlyProperty<Any, NavController<T>> { _, property ->
    val navControllerKey = key ?: property.name
    this.get<NavController<T>>(navControllerKey) ?: navController(initialBackstack).also {
        this[navControllerKey] = it
    }
}

/**
 * Returns a property delegate that creates and saves a new [NavController] instance in
 * a receiver [SavedStateHandle]. NavController's backstack will contain a single item -
 * [startDestination].
 *
 * @param key an optional key to use for saving NavController instance inside a SavedStateHandle.
 * By default, the name of the property will be used as a key.
 */
fun <T> SavedStateHandle.navController(
    key: String? = null,
    startDestination: T
) = navController(key, listOf(startDestination))

/**
 * A backstack controller which is used for all navigation.
 *
 * Backstack modifications are done with [setNewBackstackEntries] method. More specific
 * operation such as [navigate], [pop], [replaceLast] and their variants are provided as
 * extension methods.
 *
 * Implements [Parcelable] interface so it may be stored in [SavedStateHandle] or remembered with
 * [rememberSaveable]. Use [rememberNavController] for the latter.
 *
 * The type specified in the type parameter must comply with the next requirements:
 *
 * 1) It must be possible to write the instances of the type into [Parcel]. Which means
 * it should be either [Parcelable], or [Serializable], or of any natively supported data type
 * (e.g. primitive or string).
 *
 * 2) It must be [Stable] or [Immutable].
 */
@Stable
class NavController<T> internal constructor(
    initialEntries: List<NavEntry<T>>,
    initialAction: NavAction = NavAction.Idle
) : Parcelable {

    private val entries = mutableStateOf(initialEntries)

    private val action = mutableStateOf<NavAction>(initialAction)

    /**
     * A property to access the current backstack entries.
     *
     * The instance of [NavBackstack] in the same [NavController] never changes. However,
     * the properties of NavBackstack itself are backed up by [MutableState] and they will change
     * and notify Compose about.
     *
     * If you want to listen for changes outside of composition, you may set [onBackstackChange]
     * listener.
     */
    val backstack = NavBackstack(entries, action)

    /**
     * Optional listener of backstack changes. It will be invoked after every call
     * to [setNewBackstackEntries].
     */
    @Suppress("MemberVisibilityCanBePrivate")
    var onBackstackChange: ((backstack: NavBackstack<T>) -> Unit)? = null

    /**
     * Sets new backstack [entries] and the [action] describing the change. You should use
     * existing entries from [backstack] to preserve their identities and associated components
     * (lifecycles, saved states, view models). New entries can be created with [navEntry] method.
     *
     * Any new rearrangement, duplication and removal of existing entries is a valid change.
     *
     * Use this method when none of the built-in extension methods ([navigate], [pop],
     * [replaceLast] and their variations) suits your need.
     *
     * This function does not guarantee thread-safety and is intended to be called only
     * from the main thread.
     *
     * @param action an optional parameter, used as a hint for [AnimatedNavHost] to select
     * a transition animation. In all other cases it doesn't affect anything. Existing types
     * of actions may be used: [NavAction.Navigate], [NavAction.Replace] or [NavAction.Pop].
     * You may also extend [NavAction] interface to create new actions appropriate for your use
     * case.
     */
    @MainThread
    fun setNewBackstackEntries(entries: List<NavEntry<T>>, action: NavAction = NavAction.Navigate) {
        this.entries.value = entries.toList() // protection from outer modifications
        this.action.value = action
        onBackstackChange?.invoke(backstack)
    }

    override fun toString(): String {
        return "NavController(entries=${entries.value}, action=${action.value})"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        entries.value.let { backstack ->
            // This handles the case when some NavEntries instance appear in the backstack several
            // times. After the restoration their instances must not be duplicated.
            val ids = hashSetOf<NavId>()
            parcel.writeInt(backstack.size)
            backstack.forEach {
                // write every id to preserve the order
                parcel.writeParcelable(it.id, flags)
                if (it.id !in ids) {
                    ids.add(it.id)
                    // but write a destination only once, because there is no need to duplicate
                    // existing data
                    parcel.writeValue(it.destination)
                }
            }
        }
        parcel.writeParcelable(action.value, flags)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.ClassLoaderCreator<NavController<*>> {

        override fun createFromParcel(
            parcel: Parcel,
            nullableClassLoader: ClassLoader?
        ): NavController<*> {
            val classLoader = nullableClassLoader ?: this::class.java.classLoader
            val entryMap = hashMapOf<NavId, NavEntry<Any?>>()
            return NavController(
                initialEntries = List(parcel.readInt()) {
                    val id = parcel.readParcelable<NavId>(classLoader)!!
                    entryMap.getOrPut(id) {
                        // Here we do the opposite to "writeToParcel" method and read
                        // the destination value only if its id appears for the first time.
                        //
                        // This way we don't duplicate instances with same unique ids.
                        NavEntry(
                            id = id,
                            destination = parcel.readValue(classLoader)
                        )
                    }
                },
                initialAction = parcel.readParcelable(classLoader)!!
            )
        }

        override fun createFromParcel(parcel: Parcel): NavController<*> =
            createFromParcel(parcel, null)

        override fun newArray(size: Int): Array<NavController<*>?> {
            return arrayOfNulls(size)
        }

    }

}

/**
 * A read-only class to access the current backstack [entries] and the last [action]. These
 * properties are backed up by [MutableState], so Compose will get notified about their changes.
 */
@Stable
class NavBackstack<T> internal constructor(
    entriesState: State<List<NavEntry<T>>>,
    actionState: State<NavAction>
) {
    /**
     * The list of current entries in the backstack. The last item in this list is the item that
     * will be displayed by [NavHost].
     *
     * May become empty if you pop all the items off the backstack.
     */
    val entries: List<NavEntry<T>> by entriesState

    /**
     * The action of the last [NavController.setNewBackstackEntries] call.
     *
     * The initial value of every new instance of [NavController] is [NavAction.Idle].
     */
    val action: NavAction by actionState

    override fun toString(): String {
        return "NavBackstack(entries=$entries, action=$action)"
    }

}
