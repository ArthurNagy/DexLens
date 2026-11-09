package com.arthurnagy.dexlens.core.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NavigatorTest {
    private data object Root : NavKey

    private data object Detail : NavKey

    private data class Entry(
        val id: Int,
    ) : NavKey

    @Test
    fun navigatePushesAndGoBackPops() {
        val backStack = NavBackStack<NavKey>(Root)
        val navigator = Navigator(backStack)

        navigator.navigate(Detail)
        assertEquals(listOf(Root, Detail), backStack.toList())
        assertTrue(navigator.canGoBack)

        navigator.goBack()
        assertEquals(listOf(Root), backStack.toList())
    }

    @Test
    fun theRootEntryCannotBePopped() {
        val backStack = NavBackStack<NavKey>(Root)
        val navigator = Navigator(backStack)

        assertFalse(navigator.canGoBack)
        assertNull(navigator.backOrNull(Root))
        navigator.goBack()
        assertEquals(listOf(Root), backStack.toList())
    }

    @Test
    fun onlyTheRootEntryHasNoBackAction() {
        // A list-detail scene shows the root list beside its detail: the list still gets no Back.
        val backStack = NavBackStack<NavKey>(Root, Detail)
        val navigator = Navigator(backStack)

        assertNull(navigator.backOrNull(Root))
        val back = assertNotNull(navigator.backOrNull(Detail))
        back()
        assertEquals(listOf(Root), backStack.toList())
    }

    // Scenario: Replacing the entry on wide window
    @Test
    fun navigateDetailReplacesADetailOnTop() {
        val backStack = NavBackStack<NavKey>(Root)
        val navigator = Navigator(backStack)

        navigator.navigateDetail(Entry(1))
        navigator.navigateDetail(Entry(2))

        assertEquals(listOf(Root, Entry(2)), backStack.toList())
        navigator.goBack()
        assertEquals(listOf(Root), backStack.toList())
    }
}
