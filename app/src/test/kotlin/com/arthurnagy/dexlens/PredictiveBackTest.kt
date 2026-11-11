package com.arthurnagy.dexlens

import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.width
import androidx.navigation3.runtime.NavKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arthurnagy.dexlens.core.navigation.EntryProviderInstaller
import com.arthurnagy.dexlens.core.navigation.Navigator
import com.arthurnagy.dexlens.feature.pokedex.api.PokedexKey
import kotlinx.serialization.Serializable
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

/** The back gesture plays the back arrow's transition, scrubbed by the gesture, instead of shrinking the leaving screen. */
@RunWith(AndroidJUnit4::class)
class PredictiveBackTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Serializable
    private data object DetailKey : NavKey

    // Scenario: Back gesture matches the back arrow
    @Test
    fun theLeavingScreenKeepsItsSizeHalfwayThroughTheGesture() {
        lateinit var navigator: Navigator
        val feature: EntryProviderInstaller = { nav ->
            navigator = nav
            entry<PokedexKey> { Text("The list") }
            entry<DetailKey> { Box(Modifier.fillMaxSize().testTag("detail")) }
        }
        composeRule.setContent { AppNavigation(entryProviderInstallers = setOf(feature)) }
        composeRule.runOnIdle { navigator.navigate(DetailKey) }
        composeRule.waitForIdle()
        val fullWidth = composeRule.onRoot().getBoundsInRoot().width

        composeRule.runOnUiThread {
            val dispatcher = composeRule.activity.onBackPressedDispatcher
            dispatcher.dispatchOnBackStarted(BackEventCompat(0f, HALF_HEIGHT, 0f, BackEventCompat.EDGE_LEFT))
            dispatcher.dispatchOnBackProgressed(BackEventCompat(HALFWAY_X, HALF_HEIGHT, HALFWAY, BackEventCompat.EDGE_LEFT))
        }
        composeRule.waitForIdle()

        assertEquals(fullWidth, composeRule.onNodeWithTag("detail").getBoundsInRoot().width)
    }

    private companion object {
        const val HALF_HEIGHT = 800f
        const val HALFWAY = 0.5f
        const val HALFWAY_X = 300f
    }
}
