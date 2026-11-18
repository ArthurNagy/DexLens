package com.arthurnagy.dexlens.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * [isDetailShownBesideList] follows the scene `ListDetailSceneStrategy` draws: the directive stands in for the window
 * (two horizontal partitions on a wide window, one on a narrow one), so flipping it is what a fold or resize does to the
 * strategy.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w841dp-h701dp-xxhdpi")
class ListDetailPanesTest {
    @get:Rule
    val composeRule = createComposeRule()

    private data object Root

    private data object TheList

    private data class Detail(
        val id: Int,
    )

    private var partitions by mutableIntStateOf(2)

    /** The detail stand-in shows its id, and a Back marker only when it fills the window on its own. */
    private fun show(
        backStack: List<Any>,
        sceneKey: Any,
    ) {
        composeRule.setContent {
            val strategy = rememberListDetailSceneStrategy<Any>(
                directive = PaneScaffoldDirective.Default.copy(maxHorizontalPartitions = partitions),
            )
            NavDisplay(
                backStack = backStack,
                onBack = {},
                sceneStrategies = listOf(strategy),
                entryProvider = entryProvider {
                    entry<Root> { Text("Root") }
                    entry<TheList>(metadata = ListDetailSceneStrategy.listPane(sceneKey = sceneKey)) { Text("The list") }
                    entry<Detail>(metadata = ListDetailSceneStrategy.detailPane(sceneKey = sceneKey)) { key ->
                        val besideList = isDetailShownBesideList()
                        Column {
                            Text("Detail ${key.id}")
                            if (!besideList) Text("Back", Modifier.testTag("detail:back"))
                        }
                    }
                },
            )
        }
    }

    @Test
    fun aDetailAloneShowsItsBackMarker() {
        partitions = 1
        show(listOf(TheList, Detail(7)), sceneKey = Unit)

        composeRule.onNodeWithText("Detail 7").assertExists()
        composeRule.onNodeWithTag("detail:back").assertExists()
        composeRule.onNodeWithText("The list").assertDoesNotExist()
    }

    @Test
    fun aDetailBesideItsListHidesItsBackMarker() {
        show(listOf(TheList, Detail(7)), sceneKey = Unit)

        composeRule.onNodeWithText("The list").assertExists()
        composeRule.onNodeWithText("Detail 7").assertExists()
        composeRule.onNodeWithTag("detail:back").assertDoesNotExist()
    }

    // Scenario: Licence Back arrow follows the window size
    @Test
    fun aDetailInItsOwnSceneFollowsTheScene() {
        show(listOf(Root, TheList, Detail(7)), sceneKey = TheList)

        assertFollowsTheScene()
        composeRule.onNodeWithText("Root").assertDoesNotExist()
    }

    // Scenario: Entry Back arrow follows the window size
    @Test
    fun aDetailInTheDefaultSceneFollowsTheScene() {
        show(listOf(TheList, Detail(7)), sceneKey = Unit)

        assertFollowsTheScene()
    }

    private fun assertFollowsTheScene() {
        composeRule.onNodeWithText("Detail 7").assertExists()
        composeRule.onNodeWithTag("detail:back").assertDoesNotExist()

        composeRule.runOnIdle { partitions = 1 }
        composeRule.onNodeWithText("Detail 7").assertExists()
        composeRule.onNodeWithTag("detail:back").assertExists()
        composeRule.onNodeWithText("The list").assertDoesNotExist()

        composeRule.runOnIdle { partitions = 2 }
        composeRule.onNodeWithText("The list").assertExists()
        composeRule.onNodeWithText("Detail 7").assertExists()
        composeRule.onNodeWithTag("detail:back").assertDoesNotExist()
    }
}
