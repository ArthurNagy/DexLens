package com.arthurnagy.dexlens

import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation3.runtime.NavKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arthurnagy.dexlens.core.navigation.EntryProviderInstaller
import com.arthurnagy.dexlens.core.navigation.Navigator
import com.arthurnagy.dexlens.feature.pokedex.api.PokedexKey
import kotlinx.serialization.Serializable
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The app's back stack: starts on the start destination and renders what the installed features contribute. */
@RunWith(AndroidJUnit4::class)
class AppNavigationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Serializable
    private data object TestKey : NavKey

    @Test
    fun startsOnPokedexAndShowsContributedEntries() {
        lateinit var navigator: Navigator
        val testFeature: EntryProviderInstaller = { nav ->
            navigator = nav
            entry<PokedexKey> { Text("Pokedex start") }
            entry<TestKey> { Text("Contributed entry") }
        }
        composeRule.setContent { AppNavigation(entryProviderInstallers = setOf(testFeature)) }

        composeRule.onNodeWithText("Pokedex start").assertExists()

        composeRule.runOnIdle { navigator.navigate(TestKey) }
        composeRule.onNodeWithText("Contributed entry").assertExists()

        composeRule.runOnIdle { navigator.goBack() }
        composeRule.onNodeWithText("Pokedex start").assertExists()
    }

    // Scenario: Side by side on wide window
    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    @Test
    @Config(qualifiers = "w700dp-h1000dp")
    fun listAndDetailShowTogetherOnAWideWindow() {
        lateinit var navigator: Navigator
        val feature: EntryProviderInstaller = { nav ->
            navigator = nav
            entry<PokedexKey>(metadata = ListDetailSceneStrategy.listPane()) { Text("The list") }
            entry<TestKey>(metadata = ListDetailSceneStrategy.detailPane()) { Text("The entry") }
        }
        composeRule.setContent { AppNavigation(entryProviderInstallers = setOf(feature)) }

        composeRule.runOnIdle { navigator.navigate(TestKey) }

        composeRule.onNodeWithText("The list").assertExists()
        composeRule.onNodeWithText("The entry").assertExists()
    }
}
