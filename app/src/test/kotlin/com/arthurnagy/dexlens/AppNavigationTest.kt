package com.arthurnagy.dexlens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arthurnagy.dexlens.core.designsystem.AppTheme
import com.arthurnagy.dexlens.core.navigation.EntryProviderInstaller
import com.arthurnagy.dexlens.core.navigation.Navigator
import com.arthurnagy.dexlens.feature.pokedex.api.PokedexKey
import com.arthurnagy.dexlens.feature.settings.api.LicenseDetailKey
import com.arthurnagy.dexlens.feature.settings.api.LicensesKey
import com.arthurnagy.dexlens.feature.settings.api.SettingsKey
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.serialization.Serializable
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The app's back stack: starts on the start destination and renders what the installed features contribute. */
@RunWith(AndroidJUnit4::class)
class AppNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

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

    // Scenario: Side by side on wide window
    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(qualifiers = "w841dp-h701dp-xxhdpi")
    fun foldableShowsBothPanes() {
        lateinit var navigator: Navigator
        val feature: EntryProviderInstaller = { nav ->
            navigator = nav
            entry<PokedexKey>(metadata = ListDetailSceneStrategy.listPane()) { Text("The list") }
            entry<TestKey>(metadata = ListDetailSceneStrategy.detailPane()) { Text("The entry") }
        }
        composeRule.setContent { AppTheme { AppNavigation(entryProviderInstallers = setOf(feature)) } }

        composeRule.runOnIdle { navigator.navigate(TestKey) }

        composeRule.onRoot().captureRoboImage()
    }

    /**
     * Settings' entries as :feature:settings:impl declares them (same keys, pane metadata and Back actions), with stand-in
     * content: the home list is a list pane of the default scene, Settings a plain entry, and the licences their own
     * list-detail scene keyed by [LicensesKey].
     */
    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    private fun showLicencesFeature(): Navigator {
        lateinit var navigator: Navigator
        val feature: EntryProviderInstaller = { nav ->
            navigator = nav
            entry<PokedexKey>(metadata = ListDetailSceneStrategy.listPane(detailPlaceholder = { Text("Pick a Pokémon") })) { Text("Home list") }
            entry<SettingsKey> { Text("Settings screen") }
            entry<LicensesKey>(
                metadata = ListDetailSceneStrategy.listPane(sceneKey = LicensesKey, detailPlaceholder = { Text("Pick a library") }),
            ) { key ->
                val back = nav.backFromOrNull(key)
                Column {
                    Text("Licences list")
                    if (back != null) Text("Back", Modifier.testTag("licenses:back").clickable { back() })
                }
            }
            entry<LicenseDetailKey>(metadata = ListDetailSceneStrategy.detailPane(sceneKey = LicensesKey)) { key ->
                Text("Licence of ${key.libraryId}")
            }
        }
        composeRule.setContent { AppTheme { AppNavigation(entryProviderInstallers = setOf(feature)) } }
        return navigator
    }

    private fun Navigator.openLicence(id: String) {
        composeRule.runOnIdle {
            navigate(SettingsKey)
            navigate(LicensesKey)
            navigateDetail(LicenseDetailKey(id))
        }
    }

    // Scenario: Licence beside the list
    @Test
    @Config(qualifiers = "w841dp-h701dp-xxhdpi")
    fun aLicenceShowsBesideTheLicencesListOnAFoldable() {
        val navigator = showLicencesFeature()

        navigator.openLicence("androidx.core:core")

        composeRule.onNodeWithText("Licences list").assertExists()
        composeRule.onNodeWithText("Licence of androidx.core:core").assertExists()
        composeRule.onNodeWithText("Home list").assertDoesNotExist()
        composeRule.onNodeWithText("Settings screen").assertDoesNotExist()

        composeRule.runOnIdle { navigator.navigateDetail(LicenseDetailKey("io.coil-kt.coil3:coil")) }

        composeRule.onNodeWithText("Licence of io.coil-kt.coil3:coil").assertExists()
        composeRule.onNodeWithText("Licence of androidx.core:core").assertDoesNotExist()
        composeRule.onNodeWithText("Licences list").assertExists()
    }

    // Scenario: Licence placeholder on a wide window
    @Test
    @Config(qualifiers = "w841dp-h701dp-xxhdpi")
    fun thePlaceholderShowsBesideTheListBeforeALibraryIsPicked() {
        val navigator = showLicencesFeature()

        composeRule.runOnIdle {
            navigator.navigate(SettingsKey)
            navigator.navigate(LicensesKey)
        }

        composeRule.onNodeWithText("Licences list").assertExists()
        composeRule.onNodeWithText("Pick a library").assertExists()
        composeRule.onNodeWithText("Home list").assertDoesNotExist()
    }

    // Scenario: Back to the licences list on a phone
    @Test
    fun backFromALicenceOnAPhoneShowsTheList() {
        val navigator = showLicencesFeature()
        navigator.openLicence("androidx.core:core")
        composeRule.onNodeWithText("Licence of androidx.core:core").assertExists()
        composeRule.onNodeWithText("Licences list").assertDoesNotExist()

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }

        composeRule.onNodeWithText("Licences list").assertExists()
        composeRule.onNodeWithText("Licence of androidx.core:core").assertDoesNotExist()
    }

    // Scenario: Leaving the licences on a wide window
    @Test
    @Config(qualifiers = "w841dp-h701dp-xxhdpi")
    fun theListsBackLeavesTheLicencesInOneTapOnAFoldable() {
        val navigator = showLicencesFeature()
        navigator.openLicence("androidx.core:core")

        composeRule.onNodeWithTag("licenses:back").performClick()

        composeRule.onNodeWithText("Settings screen").assertExists()
        composeRule.onNodeWithText("Licences list").assertDoesNotExist()
        composeRule.onNodeWithText("Licence of androidx.core:core").assertDoesNotExist()
    }
}
