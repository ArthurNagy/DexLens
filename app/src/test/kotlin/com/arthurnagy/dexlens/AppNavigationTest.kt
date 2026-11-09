package com.arthurnagy.dexlens

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation3.runtime.NavKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arthurnagy.dexlens.core.navigation.EntryProviderInstaller
import com.arthurnagy.dexlens.core.navigation.Navigator
import kotlinx.serialization.Serializable
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The app's back stack: starts on the start destination and renders what the installed features contribute. */
@RunWith(AndroidJUnit4::class)
class AppNavigationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Serializable
    private data object TestKey : NavKey

    @Test
    fun startsOnHomeAndShowsContributedEntries() {
        lateinit var navigator: Navigator
        val testFeature: EntryProviderInstaller = { nav ->
            navigator = nav
            entry<TestKey> { Text("Contributed entry") }
        }
        composeRule.setContent { AppNavigation(entryProviderInstallers = setOf(testFeature)) }

        composeRule.onNodeWithText("DexLens").assertExists()

        composeRule.runOnIdle { navigator.navigate(TestKey) }
        composeRule.onNodeWithText("Contributed entry").assertExists()

        composeRule.runOnIdle { navigator.goBack() }
        composeRule.onNodeWithText("DexLens").assertExists()
    }
}
