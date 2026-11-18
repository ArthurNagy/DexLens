package com.arthurnagy.dexlens.feature.pokedex.impl.scanner

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowCameraCharacteristics
import org.robolectric.shadows.ShadowCameraManager
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
internal class CameraOrientationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val cameraManager = context.getSystemService(CameraManager::class.java)

    @Before
    fun forgetTheCachedCamera() = BackCameraSensor.reset()

    @After
    fun forgetTheTestCamera() = BackCameraSensor.reset()

    private fun addCamera(
        id: String,
        facing: Int,
        orientation: Int,
        backwardCompatible: Boolean = true,
    ) {
        val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        shadowOf(characteristics).apply {
            set(CameraCharacteristics.LENS_FACING, facing)
            set(CameraCharacteristics.SENSOR_ORIENTATION, orientation)
            set(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES,
                if (backwardCompatible) {
                    intArrayOf(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE)
                } else {
                    intArrayOf(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT)
                },
            )
        }
        shadowOf(cameraManager).addCamera(id, characteristics)
    }

    // Scenario: Camera behind the sheet corners
    // Scenario: Corners where filling would enlarge the preview
    @Test
    fun thePictureIsPortraitWhenTheBufferTurnsAQuarter() {
        // A phone's back camera (sensor 90) and a tablet's (sensor 0), at each display rotation.
        assertTrue(uprightPortrait(sensorOrientationDegrees = 90, displayRotationDegrees = 0))
        assertTrue(uprightPortrait(sensorOrientationDegrees = 90, displayRotationDegrees = 180))
        assertFalse(uprightPortrait(sensorOrientationDegrees = 90, displayRotationDegrees = 90))
        assertFalse(uprightPortrait(sensorOrientationDegrees = 90, displayRotationDegrees = 270))
        assertFalse(uprightPortrait(sensorOrientationDegrees = 0, displayRotationDegrees = 0))
        assertFalse(uprightPortrait(sensorOrientationDegrees = 180, displayRotationDegrees = 0))
        assertTrue(uprightPortrait(sensorOrientationDegrees = 0, displayRotationDegrees = 90))
        assertTrue(uprightPortrait(sensorOrientationDegrees = 0, displayRotationDegrees = 270))
        assertTrue(uprightPortrait(sensorOrientationDegrees = 270, displayRotationDegrees = 0))
        assertFalse(uprightPortrait(sensorOrientationDegrees = 270, displayRotationDegrees = 90))
    }

    // Scenario: Camera behind the sheet corners
    @Test
    fun aPhonesBackCameraIsPortraitAtItsNaturalRotation() {
        addCamera("0", CameraCharacteristics.LENS_FACING_BACK, orientation = 90)
        addCamera("1", CameraCharacteristics.LENS_FACING_FRONT, orientation = 270)
        var upright: Boolean? = null
        composeRule.setContent { upright = rememberBackCameraUprightPortrait() }
        composeRule.waitForIdle()
        assertEquals(true, upright)
    }

    // Scenario: Camera behind the sheet corners
    @Test
    fun theCameraCameraXPicksIsTheFirstBackwardCompatibleBackCamera() {
        addCamera("0", CameraCharacteristics.LENS_FACING_BACK, orientation = 0, backwardCompatible = false)
        addCamera("1", CameraCharacteristics.LENS_FACING_BACK, orientation = 90)
        assertEquals(90, BackCameraSensor.orientation(context))
    }

    // Scenario: Corners where filling would enlarge the preview
    @Test
    fun withoutABackCameraTheOrientationIsUnknown() {
        addCamera("1", CameraCharacteristics.LENS_FACING_FRONT, orientation = 270)
        var upright: Boolean? = true
        composeRule.setContent { upright = rememberBackCameraUprightPortrait() }
        composeRule.waitForIdle()
        assertNull(upright)
    }

    // Scenario: Corners where filling would enlarge the preview
    @Test
    fun aFailingLookupGivesAnUnknownOrientationAndIsNotRepeated() {
        // A listed id whose characteristics can't be read: the shadow throws, as a SecurityException or an OEM's would.
        @Suppress("UNCHECKED_CAST")
        val registered = ShadowCameraManager::class.java
            .getDeclaredField("cameraIdToCharacteristics")
            .apply { isAccessible = true }
            .get(null) as MutableMap<String, CameraCharacteristics?>
        registered["0"] = null
        assertNull(BackCameraSensor.orientation(context))

        registered.remove("0")
        addCamera("1", CameraCharacteristics.LENS_FACING_BACK, orientation = 90)
        assertNull(BackCameraSensor.orientation(context), "the failed lookup is cached, not retried")
    }

    @Test
    fun theOrientationIsLookedUpOncePerProcess() {
        addCamera("0", CameraCharacteristics.LENS_FACING_BACK, orientation = 90)
        assertEquals(90, BackCameraSensor.orientation(context))
        shadowOf(cameraManager).removeCamera("0")
        addCamera("2", CameraCharacteristics.LENS_FACING_BACK, orientation = 0)
        assertEquals(90, BackCameraSensor.orientation(context), "the cached lookup, not a second binder call")
    }
}
