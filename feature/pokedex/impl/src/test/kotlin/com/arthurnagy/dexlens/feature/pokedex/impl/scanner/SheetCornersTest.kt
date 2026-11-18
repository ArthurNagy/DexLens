package com.arthurnagy.dexlens.feature.pokedex.impl.scanner

import androidx.activity.ComponentActivity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arthurnagy.dexlens.core.designsystem.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
internal class SheetCornersTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val density = Density(2.625f)
    private val sheet = Size(1080f, 1600f)

    // Scenario: Camera behind the sheet corners
    @Test
    fun theTopRadiusComesFromTheShape() {
        val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        assertEquals(73.5f, topCornerRadius(shape, sheet, LayoutDirection.Ltr, density), 0.01f)
        assertEquals(73.5f, topCornerRadius(shape, sheet, LayoutDirection.Rtl, density), 0.01f)
    }

    // Scenario: Camera behind the sheet corners
    @Test
    fun theLargerTopCornerWins() {
        val shape = RoundedCornerShape(topStart = 12.dp, topEnd = 28.dp)
        assertEquals(73.5f, topCornerRadius(shape, sheet, LayoutDirection.Ltr, density), 0.01f)
        assertEquals(73.5f, topCornerRadius(shape, sheet, LayoutDirection.Rtl, density), 0.01f)
    }

    // Scenario: Camera behind the sheet corners
    @Test
    fun aSquareShapeLeavesNothingUncovered() {
        assertEquals(0f, topCornerRadius(RectangleShape, sheet, LayoutDirection.Ltr, density))
    }

    // Scenario: Camera behind the sheet corners
    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun theSheetsRealCornersAre28dp() {
        var radius = 0f
        var expected = 0f
        composeRule.setContent {
            AppTheme {
                val local = LocalDensity.current
                expected = with(local) { 28.dp.toPx() }
                radius = topCornerRadius(BottomSheetDefaults.ExpandedShape, sheet, LocalLayoutDirection.current, local)
            }
        }
        composeRule.waitForIdle()
        assertEquals(expected, radius, 0.01f)
    }
}
