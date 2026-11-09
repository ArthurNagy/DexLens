package com.arthurnagy.dexlens.core.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/** Gives an otherwise silent element a label for screen readers. */
internal fun Modifier.semanticsLabel(label: String): Modifier = semantics { contentDescription = label }
