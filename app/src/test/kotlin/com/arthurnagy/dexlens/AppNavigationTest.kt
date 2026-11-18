package com.arthurnagy.dexlens

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.Composable
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
import com.arthurnagy.dexlens.core.ui.isDetailShownBesideList
import com.arthurnagy.dexlens.feature.entry.api.EntryKey
import com.arthurnagy.dexlens.feature.pokedex.api.PokedexKey
import com.arthurnagy.dexlens.feature.settings.api.LicenseDetailKey
import com.arthurnagy.dexlens.feature.settings.api.LicensesKey
import com.arthurnagy.dexlens.feature.settings.api.SettingsKey
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.serialization.Serializable
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The app's back stack: starts on the start destination and renders what the installed features contribute. */
@RunWith(AndroidJUnit4::class)
class AppNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Serializable
    private data object TestKey : NavKey

    /** The callback [showApp] registered and the Application it registered it with, so it is unregistered there. */
    private var recreatedContent: Pair<Application, Application.ActivityLifecycleCallbacks>? = null

    @After
    fun stopSettingContentOnRecreation() {
        stopSettingRecreatedContent()
    }

    private fun stopSettingRecreatedContent() {
        recreatedContent?.let { (application, callbacks) -> application.unregisterActivityLifecycleCallbacks(callbacks) }
        recreatedContent = null
    }

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
     *
     * Keep in sync with `feature/settings/impl/.../SettingsEntries.kt`: these stand-ins copy its metadata by hand.
     */
    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    private fun showLicencesFeature(): Navigator {
        lateinit var navigator: Navigator
        val feature: EntryProviderInstaller = { nav ->
            navigator = nav
            entry<PokedexKey>(
                metadata = ListDetailSceneStrategy.listPane(detailPlaceholder = {
                    Text("Pick a Pokémon")
                }),
            ) { Text("Home list") }
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
                val besideList = isDetailShownBesideList()
                val back = nav.backOrNull(key).takeUnless { besideList }
                Column {
                    Text("Licence of ${key.libraryId}")
                    if (back != null) Text("Back", Modifier.testTag("licence:back").clickable { back() })
                }
            }
        }
        showApp(feature)
        return navigator
    }

    /**
     * The home list and the entry as :feature:pokedex:impl and :feature:entry:impl declare them (same keys, pane metadata
     * and Back wiring), with stand-in content: the entry shows its Back arrow unless it sits right above the list and
     * beside it; opened from another entry's evolution line it keeps the arrow.
     *
     * Keep in sync with `feature/entry/impl/.../EntryEntries.kt`: these stand-ins copy its metadata and wiring by hand.
     */
    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    private fun showEntryFeature(): Navigator {
        lateinit var navigator: Navigator
        val feature: EntryProviderInstaller = { nav ->
            navigator = nav
            entry<PokedexKey>(
                metadata = ListDetailSceneStrategy.listPane(detailPlaceholder = { Text("Pick a Pokémon") }),
            ) { Text("Home list") }
            entry<EntryKey>(metadata = ListDetailSceneStrategy.detailPane()) { key ->
                val besideList = isDetailShownBesideList()
                val openedFromAnEntry = nav.entryBelow(key) is EntryKey
                val back = nav.backOrNull(key)
                Column {
                    Text("Entry #${key.speciesId}")
                    if ((!besideList || openedFromAnEntry) && back != null) {
                        Text("Back", Modifier.testTag("entry:back").clickable { back() })
                    }
                }
            }
        }
        showApp(feature)
        return navigator
    }

    /**
     * Shows the app, and shows it again on every activity recreated during the test: the activity declares no
     * `configChanges`, so a fold or resize recreates it and `rememberNavBackStack` restores the back stack. An
     * Application activity-lifecycle callback sets the same content on each recreated activity (the compose rule only
     * sets it on the first one); a callback from an earlier call is unregistered first.
     *
     * The Navigator a `show…Feature()` returns belongs to the first activity's back stack: after [resizeTo] the
     * recreated activity has its own, so don't navigate through the returned Navigator after a resize.
     */
    private fun showApp(feature: EntryProviderInstaller) {
        stopSettingRecreatedContent()
        val content: @Composable () -> Unit = { AppTheme { AppNavigation(entryProviderInstallers = setOf(feature)) } }
        val callbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(
                activity: Activity,
                savedInstanceState: Bundle?,
            ) {
                if (savedInstanceState != null) (activity as ComponentActivity).setContent(content = content)
            }

            override fun onActivityStarted(activity: Activity) = Unit

            override fun onActivityResumed(activity: Activity) = Unit

            override fun onActivityPaused(activity: Activity) = Unit

            override fun onActivityStopped(activity: Activity) = Unit

            override fun onActivitySaveInstanceState(
                activity: Activity,
                outState: Bundle,
            ) = Unit

            override fun onActivityDestroyed(activity: Activity) = Unit
        }
        val application = composeRule.activity.application
        application.registerActivityLifecycleCallbacks(callbacks)
        recreatedContent = application to callbacks
        composeRule.setContent(content)
    }

    /** What folding, unfolding or resizing does: the window changes size and the activity is recreated. */
    private fun resizeTo(qualifiers: String) {
        RuntimeEnvironment.setQualifiers(qualifiers)
        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()
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

    // Scenario: No Back arrow on a licence beside the list
    @Test
    @Config(qualifiers = FOLDABLE)
    fun aLicenceBesideTheListHasNoBackArrowOnAFoldable() {
        showLicencesFeature().openLicence("androidx.core:core")

        composeRule.onNodeWithText("Licence of androidx.core:core").assertExists()
        composeRule.onNodeWithTag("licence:back").assertDoesNotExist()
        composeRule.onNodeWithTag("licenses:back").assertExists()
    }

    // Scenario: No Back arrow on a licence beside the list
    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.MediumTablet)
    fun aLicenceBesideTheListHasNoBackArrowOnATablet() {
        showLicencesFeature().openLicence("androidx.core:core")

        composeRule.onNodeWithText("Licence of androidx.core:core").assertExists()
        composeRule.onNodeWithTag("licence:back").assertDoesNotExist()
        composeRule.onNodeWithTag("licenses:back").assertExists()
    }

    // Scenario: Licence Back arrow on a phone
    @Test
    fun theLicenceBackArrowReturnsToTheListOnAPhone() {
        showLicencesFeature().openLicence("androidx.core:core")

        composeRule.onNodeWithTag("licence:back").performClick()

        composeRule.onNodeWithText("Licences list").assertExists()
        composeRule.onNodeWithText("Licence of androidx.core:core").assertDoesNotExist()
    }

    // Scenario: No Back arrow on an entry beside the list
    @Test
    @Config(qualifiers = FOLDABLE)
    fun anEntryBesideTheListHasNoBackArrowOnAFoldable() {
        val navigator = showEntryFeature()
        composeRule.runOnIdle { navigator.navigateDetail(EntryKey(1)) }

        composeRule.onNodeWithText("Home list").assertExists()
        composeRule.onNodeWithText("Entry #1").assertExists()
        composeRule.onNodeWithTag("entry:back").assertDoesNotExist()
    }

    // Scenario: No Back arrow on an entry beside the list
    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.MediumTablet)
    fun anEntryBesideTheListHasNoBackArrowOnATablet() {
        val navigator = showEntryFeature()
        composeRule.runOnIdle { navigator.navigateDetail(EntryKey(1)) }

        composeRule.onNodeWithText("Home list").assertExists()
        composeRule.onNodeWithText("Entry #1").assertExists()
        composeRule.onNodeWithTag("entry:back").assertDoesNotExist()
    }

    // Scenario: Entry Back arrow on a phone
    @Test
    fun theEntryBackArrowReturnsToTheHomeListOnAPhone() {
        val navigator = showEntryFeature()
        composeRule.runOnIdle { navigator.navigateDetail(EntryKey(1)) }
        composeRule.onNodeWithText("Home list").assertDoesNotExist()

        composeRule.onNodeWithTag("entry:back").performClick()

        composeRule.onNodeWithText("Home list").assertExists()
        composeRule.onNodeWithText("Entry #1").assertDoesNotExist()
    }

    // Scenario: Licence Back arrow follows the window size
    @Test
    @Config(qualifiers = FOLDABLE)
    fun theLicenceBackArrowFollowsAFoldAndUnfold() {
        showLicencesFeature().openLicence("androidx.core:core")
        composeRule.onNodeWithTag("licence:back").assertDoesNotExist()

        resizeTo(PHONE_PORTRAIT)
        composeRule.onNodeWithText("Licence of androidx.core:core").assertExists()
        composeRule.onNodeWithText("Licences list").assertDoesNotExist()
        composeRule.onNodeWithTag("licence:back").assertExists()

        resizeTo(FOLDABLE)
        composeRule.onNodeWithText("Licences list").assertExists()
        composeRule.onNodeWithText("Licence of androidx.core:core").assertExists()
        composeRule.onNodeWithTag("licence:back").assertDoesNotExist()
    }

    // Scenario: Back arrow to the previous species beside the list
    @Test
    @Config(qualifiers = FOLDABLE)
    fun anEntryOpenedFromTheEvolutionLineKeepsItsBackArrowOnAFoldable() {
        val navigator = showEntryFeature()
        composeRule.runOnIdle {
            navigator.navigateDetail(EntryKey(1))
            // The evolution line pushes the next species on top of the current entry.
            navigator.navigate(EntryKey(2))
        }

        composeRule.onNodeWithText("Home list").assertExists()
        composeRule.onNodeWithText("Entry #2").assertExists()

        composeRule.onNodeWithTag("entry:back").performClick()

        composeRule.onNodeWithText("Entry #1").assertExists()
        composeRule.onNodeWithText("Entry #2").assertDoesNotExist()
        composeRule.onNodeWithText("Home list").assertExists()
        composeRule.onNodeWithTag("entry:back").assertDoesNotExist()
    }

    // Scenario: Entry Back arrow follows the window size
    @Test
    @Config(qualifiers = FOLDABLE)
    fun theEntryBackArrowFollowsAFoldAndUnfold() {
        val navigator = showEntryFeature()
        composeRule.runOnIdle { navigator.navigateDetail(EntryKey(25)) }
        composeRule.onNodeWithTag("entry:back").assertDoesNotExist()

        resizeTo(PHONE_PORTRAIT)
        composeRule.onNodeWithText("Entry #25").assertExists()
        composeRule.onNodeWithText("Home list").assertDoesNotExist()
        composeRule.onNodeWithTag("entry:back").assertExists()

        resizeTo(FOLDABLE)
        composeRule.onNodeWithText("Home list").assertExists()
        composeRule.onNodeWithText("Entry #25").assertExists()
        composeRule.onNodeWithTag("entry:back").assertDoesNotExist()
    }

    private companion object {
        const val FOLDABLE = "w841dp-h701dp-xxhdpi"
        const val PHONE_PORTRAIT = "w411dp-h914dp-xxhdpi"
    }
}
