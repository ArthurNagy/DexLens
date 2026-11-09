package com.arthurnagy.dexlens.core.domain.scan

import com.arthurnagy.dexlens.core.domain.species.SpeciesNameNormalizer
import com.arthurnagy.dexlens.core.model.SpeciesId

/**
 * Finds species names in recognised text. Tries each run of one to [MAX_NGRAM] words, longest first: exact matches,
 * then folded characters OCR confuses, then a bounded edit distance. Words already used by a match are not reused.
 */
class TextNameSignal(
    private val index: ScanNameIndex,
) : SpeciesSignal {
    override fun analyze(observation: FrameObservation): SignalResult {
        val candidates = LinkedHashMap<SpeciesId, SpeciesCandidate>()
        val decisions = ArrayList<MatchDecision>()
        observation.lines.forEach { line -> analyzeLine(line, candidates, decisions) }
        return SignalResult(candidates.values.toList(), decisions)
    }

    private fun analyzeLine(
        line: String,
        candidates: MutableMap<SpeciesId, SpeciesCandidate>,
        decisions: MutableList<MatchDecision>,
    ) {
        val tokens = SpeciesNameNormalizer.tokens(line)
        val used = BooleanArray(tokens.size)
        var matchedAny = false
        for (stage in Stage.entries) {
            for (size in minOf(MAX_NGRAM, tokens.size) downTo 1) {
                for (start in 0..tokens.size - size) {
                    if ((start until start + size).any { used[it] }) continue
                    val words = tokens.subList(start, start + size)
                    if (isIgnored(words)) continue
                    val key = words.joinToString("")
                    if (key.length < ScanNameIndex.MIN_KEY_LENGTH) continue
                    val outcome = match(stage, key) ?: continue
                    (start until start + size).forEach { used[it] = true }
                    matchedAny = true
                    val text = words.joinToString(" ")
                    decisions += MatchDecision(text, outcome.outcome, outcome.ids)
                    if (outcome.outcome != MatchOutcome.AMBIGUOUS) {
                        outcome.ids.forEach { id -> candidates.putIfAbsent(id, SpeciesCandidate(id, text, outcome.kind)) }
                    }
                }
            }
        }
        if (!matchedAny && line.isNotBlank()) {
            val allIgnored = tokens.isNotEmpty() && tokens.all { it in UI_WORDS || it.all(Char::isDigit) }
            decisions += MatchDecision(line, if (allIgnored) MatchOutcome.IGNORED else MatchOutcome.NO_MATCH, emptySet())
        }
    }

    private fun isIgnored(words: List<String>): Boolean =
        words.any { word -> word.all(Char::isDigit) } || (words.size == 1 && words.single() in UI_WORDS)

    private fun match(
        stage: Stage,
        key: String,
    ): StageMatch? = when (stage) {
        Stage.EXACT -> {
            index.exact(key).takeIf { it.isNotEmpty() }?.let { StageMatch(it, MatchKind.EXACT, MatchOutcome.EXACT) }
        }

        Stage.GENDERED -> {
            index.genderedFamily(key).takeIf { it.isNotEmpty() }?.let { StageMatch(it, MatchKind.CONFUSABLE, MatchOutcome.CONFUSABLE) }
        }

        Stage.CONFUSABLE -> {
            index
                .confusable(key)
                .takeIf { it.isNotEmpty() && key.length > SHORTEST_FOLDED_NAME }
                ?.let { StageMatch(it, MatchKind.CONFUSABLE, MatchOutcome.CONFUSABLE) }
        }

        Stage.FUZZY -> {
            index.nearest(key)?.let { near ->
                if (near.speciesIds.size == 1) {
                    StageMatch(near.speciesIds, MatchKind.FUZZY, MatchOutcome.FUZZY)
                } else {
                    StageMatch(near.speciesIds, MatchKind.FUZZY, MatchOutcome.AMBIGUOUS)
                }
            }
        }
    }

    private enum class Stage { EXACT, GENDERED, CONFUSABLE, FUZZY }

    private data class StageMatch(
        val ids: Set<SpeciesId>,
        val kind: MatchKind,
        val outcome: MatchOutcome,
    )

    private companion object {
        const val MAX_NGRAM = 3
        const val SHORTEST_FOLDED_NAME = 3

        /** Words every game screen shows. They can sit within edit distance of a name, so they never count as one. */
        val UI_WORDS = setOf(
            "fight",
            "bag",
            "run",
            "pokemon",
            "pkmn",
            "item",
            "items",
            "party",
            "move",
            "moves",
            "attack",
            "attacks",
            "defense",
            "defence",
            "speed",
            "special",
            "stats",
            "stat",
            "trainer",
            "wild",
            "appeared",
            "used",
            "foe",
            "enemy",
            "normal",
            "fire",
            "water",
            "grass",
            "electric",
            "ice",
            "fighting",
            "poison",
            "ground",
            "flying",
            "psychic",
            "bug",
            "rock",
            "ghost",
            "dragon",
            "dark",
            "steel",
            "fairy",
            "level",
            "lvl",
            "exp",
            "hp",
            "pp",
            "type",
            "what",
            "will",
            "you",
            "the",
            "and",
            "yes",
            "cancel",
            "save",
            "options",
            "option",
            "menu",
            "exit",
            "back",
            "next",
            "switch",
            "summary",
            "pack",
            "box",
            "gym",
            "center",
            "centre",
            "mart",
            "potion",
            "ball",
            "balls",
            "start",
            "select",
            "continue",
            "new",
            "game",
            "battle",
            "gained",
            "grew",
            "learned",
            "forgot",
            "effective",
            "super",
            "critical",
            "hit",
            "fainted",
            "sent",
            "out",
            "come",
            "want",
            "have",
            "with",
            "your",
            "this",
            "that",
            "from",
            "for",
            "are",
            "can",
            "not",
            "but",
            "has",
            "was",
            "were",
            "been",
            "his",
            "her",
            "its",
            "they",
            "them",
            "there",
            "here",
            "where",
            "when",
            "who",
            "how",
            "why",
            "yeah",
            "okay",
            "good",
            "great",
            "very",
            "much",
            "more",
            "most",
            "some",
            "many",
            "all",
            "any",
            "one",
            "two",
            "three",
            "day",
            "night",
            "time",
            "now",
            "today",
            "name",
            "gender",
            "ability",
            "nature",
            "held",
            "none",
            "money",
            "points",
            "world",
            "map",
            "town",
            "city",
            "route",
            "road",
            "cave",
            "forest",
            "lake",
            "sea",
            "mountain",
            "house",
            "shop",
            "buy",
            "sell",
            "use",
            "give",
            "take",
            "toss",
            "held",
        )
    }
}
