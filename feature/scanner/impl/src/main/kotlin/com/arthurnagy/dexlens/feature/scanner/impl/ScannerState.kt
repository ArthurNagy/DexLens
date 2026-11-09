package com.arthurnagy.dexlens.feature.scanner.impl

import androidx.compose.runtime.Immutable
import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.scan.MatchDecision
import com.arthurnagy.dexlens.core.model.ArtworkLocation
import com.arthurnagy.dexlens.core.model.SpeciesSummary

/** A recognised line of text, in the coordinates of the cropped aiming-box image. */
@Immutable
internal data class AnalyzedLine(
    val text: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

/** One frame's recognised text, with sizes for mapping it back onto the aiming box. */
@Immutable
internal data class AnalyzedFrame(
    val lines: List<AnalyzedLine>,
    val cropWidth: Int,
    val cropHeight: Int,
    val bufferWidth: Int,
    val bufferHeight: Int,
    val processingMillis: Long,
)

/** The latest frame and its match decisions, drawn by the debug-only overlay. */
@Immutable
internal data class ScanDebug(
    val frame: AnalyzedFrame,
    val decisions: List<MatchDecision>,
)

@Immutable
internal data class ScannerState(
    val loading: Boolean,
    val error: DomainError?,
    /** Locked species: empty while scanning. Analysis is paused while this is not empty. */
    val locked: List<SpeciesSummary>,
    /** True once after a lock, until the screen has played the haptic. */
    val pendingHaptic: Boolean,
    val torchOn: Boolean,
    val debug: ScanDebug?,
    val artwork: ArtworkLocation,
) {
    val paused: Boolean get() = locked.isNotEmpty()

    companion object {
        val Initial = ScannerState(
            loading = true,
            error = null,
            locked = emptyList(),
            pendingHaptic = false,
            torchOn = false,
            debug = null,
            artwork = ArtworkLocation(null),
        )
    }
}
