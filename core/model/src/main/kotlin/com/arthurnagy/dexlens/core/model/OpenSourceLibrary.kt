package com.arthurnagy.dexlens.core.model

/**
 * A third-party library bundled in the app, as the licences list shows it. [id] is its Maven `group:artifact`. [version]
 * is null when the build doesn't know it. Carries no licence texts, so the list never holds them.
 */
data class OpenSourceLibrary(
    val id: String,
    val name: String,
    val version: String?,
    val licenseNames: List<String>,
)

/** One of a library's licences: its [name], such as "Apache License 2.0", and its full [text]. */
data class LibraryLicense(
    val name: String,
    val text: String,
)

/** A notice file a library's artifact embeds: its [fileName] in the artifact, such as `THIRD_PARTY_NOTICE.txt`, and its full [text]. */
data class LibraryNotice(
    val fileName: String,
    val text: String,
)

/**
 * Everything the licence screen shows for a [library]: its [copyrightHolder] when known, the full text of each of its
 * [licenses], and the full text of each notice file its artifact embeds ([notices]).
 */
data class OpenSourceLibraryDetail(
    val library: OpenSourceLibrary,
    val copyrightHolder: String?,
    val licenses: List<LibraryLicense>,
    val notices: List<LibraryNotice>,
)
