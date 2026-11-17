package com.arthurnagy.dexlens.core.data.about

/** [NoticesSource] over in-memory files in #22's format. Counts reads; [failure] makes every read throw it. */
internal class FakeNoticesSource(
    private val librariesJson: String = LIBRARIES_JSON,
    private val assets: Map<String, String> = NOTICE_ASSETS,
) : NoticesSource {
    var failure: Exception? = null
    var listReads = 0
    val assetReads = mutableListOf<String>()

    override fun librariesJson(): String {
        failure?.let { throw it }
        listReads++
        return librariesJson
    }

    override fun asset(path: String): String {
        failure?.let { throw it }
        assetReads += path
        return assets[path] ?: throw java.io.FileNotFoundException(path)
    }
}

const val LITERT_NOTICE = "embedded_notices/aaa.txt"
const val MLKIT_NOTICE = "embedded_notices/bbb.txt"
const val MLKIT_OFFSETS = "embedded_notices/ccc.json"

val NOTICE_ASSETS = mapOf(
    "embedded_notices/index.json" to
        """
        {
            "com.google.ai.edge.litert:litert-api:2.3.2": { "THIRD_PARTY_NOTICE.txt": "$LITERT_NOTICE" },
            "com.google.ai.edge.litert:litert:2.3.2": { "THIRD_PARTY_NOTICE.txt": "$LITERT_NOTICE" },
            "com.google.mlkit:text-recognition:16.0.1": {
                "third_party_licenses.json": "$MLKIT_OFFSETS",
                "third_party_licenses.txt": "$MLKIT_NOTICE"
            }
        }
        """.trimIndent(),
    LITERT_NOTICE to "abseil-cpp\n\nApache License 2.0",
    MLKIT_NOTICE to "protobuf\n\nCopyright 2008 Google Inc.",
    MLKIT_OFFSETS to """{ "protobuf": "0:10" }""",
)

/** AboutLibraries' generated list, trimmed to five libraries, out of name order, with the BSD and ISC template texts. */
val LIBRARIES_JSON =
    """
    {
      "libraries": [
        {
          "uniqueId": "com.michael-bull.kotlin-result:kotlin-result",
          "artifactVersion": "2.3.1",
          "name": "kotlin-result",
          "developers": [{ "name": "Michael Bull" }],
          "licenses": ["ISC"],
          "funding": []
        },
        {
          "uniqueId": "androidx.camera:camera-core",
          "artifactVersion": "1.6.2",
          "name": "Camera Core",
          "developers": [{ "name": "The Android Open Source Project" }],
          "organization": { "name": "The Android Open Source Project" },
          "licenses": ["Apache-2.0", "BSD-3-Clause"],
          "funding": []
        },
        {
          "uniqueId": "com.google.ai.edge.litert:litert",
          "artifactVersion": "2.3.2",
          "name": "LiteRT Android Implementation",
          "developers": [{ "name": "The MediaPipe Authors" }, { "name": "Google" }],
          "licenses": ["Apache-2.0"],
          "funding": []
        },
        {
          "uniqueId": "com.google.mlkit:text-recognition",
          "artifactVersion": "16.0.1",
          "name": "text-recognition",
          "description": "",
          "developers": [],
          "licenses": ["9962f44671a86e167c028415c7e21548"],
          "funding": []
        },
        {
          "uniqueId": "org.example:real-copyright",
          "artifactVersion": "1.0",
          "name": "alpha",
          "developers": [],
          "licenses": ["BSD-3-Clause-real"],
          "funding": []
        }
      ],
      "licenses": {
        "Apache-2.0": {
          "name": "Apache License 2.0",
          "url": "https://spdx.org/licenses/Apache-2.0.html",
          "content": "Apache License\nVersion 2.0, January 2004\n\nTERMS AND CONDITIONS",
          "hash": "Apache-2.0"
        },
        "BSD-3-Clause": {
          "name": "BSD 3-Clause \"New\" or \"Revised\" License",
          "url": "https://spdx.org/licenses/BSD-3-Clause.html",
          "content": "Copyright (c) <year> <owner>. \n\nRedistribution and use in source and binary forms are permitted.",
          "hash": "BSD-3-Clause"
        },
        "ISC": {
          "name": "ISC License",
          "url": "https://spdx.org/licenses/ISC.html",
          "content": "ISC License\n\nCopyright <year> <owner> \n\nPermission to use, copy, modify, and/or distribute this software is granted.",
          "hash": "ISC"
        },
        "BSD-3-Clause-real": {
          "name": "BSD 3-Clause License",
          "content": "Copyright (c) 2020 Alpha Authors.\n\nRedistribution and use in source and binary forms are permitted.",
          "hash": "BSD-3-Clause-real"
        },
        "9962f44671a86e167c028415c7e21548": {
          "name": "ML Kit Terms of Service",
          "url": "https://developers.google.com/ml-kit/terms",
          "content": "ML Kit Terms of Service\n\nBy using ML Kit you agree to these terms.",
          "hash": "9962f44671a86e167c028415c7e21548"
        }
      }
    }
    """.trimIndent()
