package com.arthurnagy.dexlens.feature.pokedex.impl.scanner

import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.util.Rational
import android.util.Size
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.InitializationException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import java.util.concurrent.Executors

/**
 * The live preview and the analysis stream, tied together by a [ViewPort] so that `ImageProxy.cropRect` is exactly
 * what the user sees. Rebinds whenever the viewfinder's size or the display rotation changes.
 */
@Composable
internal fun CameraViewfinder(
    analyzer: ImageAnalysis.Analyzer,
    torchOn: Boolean,
    onTorchAvailabilityChange: (Boolean) -> Unit,
    onCameraUnavailable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val rotation = rememberDisplayRotation()
    val executor = remember { Executors.newSingleThreadExecutor() }
    var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val currentOnTorchAvailability by rememberUpdatedState(onTorchAvailabilityChange)
    val currentOnUnavailable by rememberUpdatedState(onCameraUnavailable)

    BoxWithConstraints(modifier) {
        val viewWidth = constraints.maxWidth
        val viewHeight = constraints.maxHeight
        LaunchedEffect(viewWidth, viewHeight, rotation, lifecycleOwner) {
            val preview = Preview.Builder().setTargetRotation(rotation).build().apply { setSurfaceProvider { surfaceRequest = it } }
            val analysis = ImageAnalysis
                .Builder()
                .setTargetRotation(rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .setResolutionSelector(highResolutionSelector())
                .build()
                .apply { setAnalyzer(executor, analyzer) }
            val viewPort = ViewPort.Builder(Rational(viewWidth, viewHeight), rotation).setScaleType(ViewPort.FILL_CENTER).build()
            val group = UseCaseGroup.Builder().setViewPort(viewPort).addUseCase(preview).addUseCase(analysis).build()
            try {
                val provider = ProcessCameraProvider.awaitInstance(context)
                cameraProvider = provider
                provider.unbindAll()
                val bound = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, group)
                camera = bound
                currentOnTorchAvailability(bound.cameraInfo.hasFlashUnit())
            } catch (e: CancellationException) {
                // Not a camera failure: the sheet expanded while the camera was starting. It extends IllegalStateException.
                throw e
            } catch (e: IllegalArgumentException) {
                Timber.w(e, "no usable back camera")
                currentOnUnavailable()
            } catch (e: IllegalStateException) {
                Timber.w(e, "camera could not be bound")
                currentOnUnavailable()
            } catch (e: InitializationException) {
                Timber.w(e, "camera could not be initialised")
                currentOnUnavailable()
            }
        }
        // CameraX turns the torch off when the app stops, so the torch is re-applied on every start.
        LifecycleStartEffect(camera, torchOn) {
            camera?.takeIf { it.cameraInfo.hasFlashUnit() }?.cameraControl?.enableTorch(torchOn)
            onStopOrDispose {}
        }
        surfaceRequest?.let { request -> CameraXViewfinder(surfaceRequest = request, modifier = Modifier.fillMaxSize()) }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Turns the camera and its privacy indicator off as soon as the viewfinder leaves the screen.
            cameraProvider?.unbindAll()
            executor.shutdown()
        }
    }
}

/** Tracks the display's rotation. A 180-degree flip changes it without a configuration change. */
@Composable
private fun rememberDisplayRotation(): Int {
    val view = LocalView.current
    val context = LocalContext.current
    var rotation by remember { mutableIntStateOf(view.display.rotation) }
    DisposableEffect(view, context) {
        val displayManager = context.getSystemService(DisplayManager::class.java)
        val listener = object : DisplayManager.DisplayListener {
            override fun onDisplayChanged(displayId: Int) {
                view.display?.takeIf { it.displayId == displayId }?.let { rotation = it.rotation }
            }

            override fun onDisplayAdded(displayId: Int) = Unit

            override fun onDisplayRemoved(displayId: Int) = Unit
        }
        displayManager.registerDisplayListener(listener, Handler(Looper.getMainLooper()))
        onDispose { displayManager.unregisterDisplayListener(listener) }
    }
    return rotation
}

/** Asks for a large frame: small on-screen text needs about 16×16 pixels per character after the crop. */
private fun highResolutionSelector(): ResolutionSelector = ResolutionSelector
    .Builder()
    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
    .setResolutionStrategy(
        ResolutionStrategy(Size(TARGET_WIDTH, TARGET_HEIGHT), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER),
    )
    .build()

private const val TARGET_WIDTH = 2560
private const val TARGET_HEIGHT = 1440
