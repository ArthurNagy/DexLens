package com.arthurnagy.dexlens.feature.pokedex.impl.scanner

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.view.Surface
import androidx.annotation.VisibleForTesting
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import timber.log.Timber

/**
 * Whether the back camera's picture, turned upright on the display, is taller than wide. The buffer is always wider
 * than tall, so it is when the buffer has to turn a quarter: `getSensorRotationDegrees(displayRotation) % 180 != 0`.
 */
internal fun uprightPortrait(
    sensorOrientationDegrees: Int,
    displayRotationDegrees: Int,
): Boolean = ((sensorOrientationDegrees - displayRotationDegrees + FULL_TURN) % HALF_TURN) != 0

/**
 * The camera picture's orientation on this display, or null while it can't be known (no back camera, or the view not
 * attached to a display yet), which never fills the sheet's corners. Read synchronously, so it is known before the
 * viewfinder binds; a 180° flip doesn't change it.
 */
@Composable
internal fun rememberBackCameraUprightPortrait(): Boolean? {
    val context = LocalContext.current
    val view = LocalView.current
    val configuration = LocalConfiguration.current
    var attached by remember(view) { mutableStateOf(view.isAttachedToWindow) }
    DisposableEffect(view) {
        val listener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                attached = true
            }

            override fun onViewDetachedFromWindow(v: View) {
                attached = false
            }
        }
        view.addOnAttachStateChangeListener(listener)
        attached = view.isAttachedToWindow
        onDispose { view.removeOnAttachStateChangeListener(listener) }
    }
    val rotation = remember(view, configuration, attached) { view.display?.rotation }
    val sensor = remember(context) { BackCameraSensor.orientation(context) }
    return if (rotation == null || sensor == null) null else uprightPortrait(sensor, rotation.toDegrees())
}

/** The default back camera's sensor orientation, looked up once per process. */
internal object BackCameraSensor {
    private var looked = false
    private var cached: Int? = null

    fun orientation(context: Context): Int? = synchronized(this) {
        if (!looked) {
            cached = lookUp(context.getSystemService(CameraManager::class.java))
            looked = true
        }
        cached
    }

    /** Forgets the cached lookup. */
    @VisibleForTesting
    fun reset() = synchronized(this) {
        looked = false
        cached = null
    }

    /**
     * The camera CameraX's `DEFAULT_BACK_CAMERA` binds: the first back-facing, backward-compatible id. Stops at that id,
     * so the main thread only asks for the characteristics it needs. Any failure, including a `SecurityException` or an
     * OEM's own, means an unknown orientation, which never fills the corners.
     */
    private fun lookUp(manager: CameraManager?): Int? = try {
        manager?.cameraIdList
            ?.asSequence()
            ?.map(manager::getCameraCharacteristics)
            ?.firstOrNull { it.isBackwardCompatibleBackCamera() }
            ?.get(CameraCharacteristics.SENSOR_ORIENTATION)
            .also { if (it == null) Timber.w("no back camera to orient the scanner by") }
    } catch (e: CameraAccessException) {
        Timber.w(e, "back camera orientation unavailable")
        null
    } catch (e: RuntimeException) {
        Timber.w(e, "back camera orientation unavailable")
        null
    }

    private fun CameraCharacteristics.isBackwardCompatibleBackCamera(): Boolean =
        get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK &&
            get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                ?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE) == true
}

private fun Int.toDegrees(): Int = when (this) {
    Surface.ROTATION_90 -> QUARTER_TURN
    Surface.ROTATION_180 -> HALF_TURN
    Surface.ROTATION_270 -> THREE_QUARTER_TURN
    else -> 0
}

private const val QUARTER_TURN = 90
private const val HALF_TURN = 180
private const val THREE_QUARTER_TURN = 270
private const val FULL_TURN = 360
