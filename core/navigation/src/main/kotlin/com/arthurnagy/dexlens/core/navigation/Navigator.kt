package com.arthurnagy.dexlens.core.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/**
 * The app's navigation actions over its one back stack. :app creates it in composition around a
 * `rememberNavBackStack(...)` (so the stack survives configuration changes and process death) and
 * hands it to every feature's entry builder. Features push other features' keys from their `:api`
 * modules — never their screens.
 *
 * ViewModels never see the Navigator: a ViewModel sets a consumable `pendingNavigation` slot, and
 * the screen calls the lambda its entry builder passed in (`onNavigate = navigator::navigate`).
 */
class Navigator(
    private val backStack: NavBackStack<NavKey>,
) {
    /** False on the root entry: there is nothing to go back to. */
    val canGoBack: Boolean get() = backStack.size > 1

    fun navigate(key: NavKey) {
        backStack.add(key)
    }

    /**
     * Opens [key] as a list-detail detail: it replaces a detail of the same kind already on top, so Back on a wide
     * window goes straight to the list rather than through every entry the list opened.
     */
    fun navigateDetail(key: NavKey) {
        val top = backStack.lastOrNull()
        if (top != null && top::class == key::class) backStack[backStack.lastIndex] = key else backStack.add(key)
    }

    /** Pops the top entry; never the last one (NavDisplay needs a non-empty back stack). */
    fun goBack() {
        if (canGoBack) backStack.removeAt(backStack.lastIndex)
    }

    /**
     * The Back action for the entry showing [key] — pass the key the entry's content lambda receives:
     * `entry<GalleryKey> { key -> GalleryScreen(onNavigateBack = navigator.backOrNull(key)) }`. Null when
     * [key] is the root (bottom) entry, so that screen shows no Back control — also while a list-detail
     * scene shows the root list beside its detail, which the back-stack size alone can't tell.
     */
    fun backOrNull(key: NavKey): (() -> Unit)? = if (backStack.firstOrNull() == key) null else ::goBack

    /**
     * Leaves [key]: removes it and every entry above it, such as the detail a list-detail list shows beside it, so the
     * list's Back control leaves in one step. Does nothing when [key] is the root (bottom) entry or isn't on the stack.
     */
    fun backFrom(key: NavKey) {
        val index = backStack.lastIndexOf(key)
        if (index > 0) repeat(backStack.size - index) { backStack.removeAt(backStack.lastIndex) }
    }

    /**
     * The entry directly below [key] on the back stack — what Back from [key] returns to. Null when [key] is the root
     * (bottom) entry or isn't on the stack. A list-detail detail uses it to tell whether it sits right above its list or
     * above another detail it was opened from.
     */
    fun entryBelow(key: NavKey): NavKey? = backStack.lastIndexOf(key).takeIf { it > 0 }?.let { backStack[it - 1] }

    /** Like [backOrNull], but the action is [backFrom]: null for the root entry, which shows no Back control. */
    fun backFromOrNull(key: NavKey): (() -> Unit)? = if (backStack.firstOrNull() == key) null else ({ backFrom(key) })
}
