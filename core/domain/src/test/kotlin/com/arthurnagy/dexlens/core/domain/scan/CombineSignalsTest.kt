package com.arthurnagy.dexlens.core.domain.scan

import com.arthurnagy.dexlens.core.model.SpeciesId
import com.arthurnagy.dexlens.core.model.SpeciesNameHit
import org.junit.Assert.assertEquals
import org.junit.Test

internal class CombineSignalsTest {
    private val nidoranF = SpeciesId(29)
    private val nidoranM = SpeciesId(32)
    private val pikachu = SpeciesId(25)
    private val raichu = SpeciesId(26)
    private val eevee = SpeciesId(133)
    private val vaporeon = SpeciesId(134)

    private val signal = TextNameSignal(
        ScanNameIndex(
            listOf(
                SpeciesNameHit(nidoranF, "Nidoran♀"),
                SpeciesNameHit(nidoranM, "Nidoran♂"),
                SpeciesNameHit(pikachu, "Pikachu"),
                SpeciesNameHit(raichu, "Raichu"),
                SpeciesNameHit(eevee, "Eevee"),
            ),
        ),
    )

    private fun text(vararg lines: String) = signal.analyze(FrameObservation(lines.toList()))

    private fun scores(vararg pairs: Pair<SpeciesId, Float>) = PictureScores.of(
        *pairs.map {
            SpeciesScore(it.first, it.second)
        }.toTypedArray(),
    )

    private fun evidence(vararg pairs: Pair<SpeciesId, Evidence>) = FrameEvidence(mapOf(*pairs))

    // Scenario: Picture picks the gender
    @Test
    fun pictureChoosesBetweenTheNidoran() {
        val result = combineSignals(text("NIDORAN"), scores(nidoranM to 0.96f, nidoranF to 0.90f))
        assertEquals(evidence(nidoranM to Evidence.BOTH), result)
    }

    // Scenario: Picture settles a misread
    @Test
    fun pictureSettlesAnAmbiguousTie() {
        val tie = SignalResult(emptyList(), listOf(MatchDecision("RAICHO", MatchOutcome.AMBIGUOUS, setOf(pikachu, raichu))))
        assertEquals(evidence(raichu to Evidence.BOTH), combineSignals(tie, scores(raichu to 0.95f, pikachu to 0.92f)))
        // Without a picture, an ambiguous tie still offers nothing.
        assertEquals(FrameEvidence.Empty, combineSignals(tie, null))
    }

    // Scenario: Close picture scores keep both names
    @Test
    fun closeScoresDoNotChoose() {
        val result = combineSignals(text("NIDORAN"), scores(nidoranM to 0.950f, nidoranF to 0.945f))
        assertEquals(evidence(nidoranF to Evidence.BOTH, nidoranM to Evidence.BOTH), result)
    }

    // Scenario: No picture keeps both names
    @Test
    fun weakPictureKeepsBothNames() {
        val result = combineSignals(text("NIDORAN"), scores(nidoranM to 0.80f))
        assertEquals(evidence(nidoranF to Evidence.NAME, nidoranM to Evidence.NAME), result)
    }

    // Scenario: Name decides between close pictures
    @Test
    fun nameDecidesBetweenPictures() {
        // Raichu ranks below five other look-alikes in the picture, but its name is on screen.
        val lookAlikes = (300..304).map { SpeciesId(it) to 0.97f }.toTypedArray()
        val result = combineSignals(text("RAICHU"), scores(pikachu to 0.96f, raichu to 0.95f, *lookAlikes))
        assertEquals(evidence(raichu to Evidence.BOTH), result)
    }

    @Test
    fun nameWinsOverADisagreeingPicture() {
        assertEquals(evidence(eevee to Evidence.NAME), combineSignals(text("EEVEE"), scores(vaporeon to 0.99f)))
    }

    // Scenario: Slow picture frame keeps names working
    @Test
    fun noPictureGivesTodaysCandidates() {
        assertEquals(evidence(pikachu to Evidence.NAME), combineSignals(text("PIKACHU"), null))
        assertEquals(evidence(nidoranF to Evidence.NAME, nidoranM to Evidence.NAME), combineSignals(text("NIDORAN"), null))
    }

    @Test
    fun pictureAloneNeedsTheImageOnlyScore() {
        assertEquals(FrameEvidence.Empty, combineSignals(text("FIGHT"), scores(pikachu to 0.95f)))
        assertEquals(evidence(pikachu to Evidence.PICTURE), combineSignals(text("FIGHT"), scores(pikachu to 0.97f, raichu to 0.94f)))
    }

    @Test
    fun pictureAloneOffersCloseChoicesUpToThree() {
        val close = scores(SpeciesId(1) to 0.970f, SpeciesId(2) to 0.968f, SpeciesId(3) to 0.966f, SpeciesId(4) to 0.964f)
        assertEquals(setOf(1, 2, 3), combineSignals(text(), close).species.keys.map { it.value }.toSet())
    }

    @Test
    fun pictureAloneLeavesOutRunnersUpBelowTheImageOnlyScore() {
        val result = combineSignals(text(), scores(pikachu to 0.960f, raichu to 0.950f))
        assertEquals(evidence(pikachu to Evidence.PICTURE), result)
    }
}
