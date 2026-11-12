package com.arthurnagy.dexlens.benchmarks

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records :app's profiles. `./gradlew :app:generateReleaseBaselineProfile` runs both tests and writes
 * app/src/release/generated/baselineProfiles/: the startup test also feeds startup-prof.txt, which R8 uses to put the
 * startup classes first in the DEX files.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    /** Cold start to the home screen, with the scanner running. */
    @Test
    fun startup() = rule.collect(packageName = TARGET_PACKAGE, includeInStartupProfile = true) {
        grantCamera()
        startToHome()
    }

    /** The journeys after startup: scrolling the list, searching, an entry and Settings. */
    @Test
    fun journeys() = rule.collect(packageName = TARGET_PACKAGE) {
        grantCamera()
        startToHome()
        scrollList()
        searchAndOpenEntry()
        openSettings()
    }
}
