package com.arthurnagy.dexlens.benchmarks

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until

/** The app under test: :app's applicationId. */
internal const val TARGET_PACKAGE = "com.arthurnagy.dexlens"

private const val WAIT_MS = 5_000L

/**
 * The scanner starts on the home screen, so the camera permission is granted up front: the system dialog would
 * otherwise cover the app and keep the camera and recognition code out of the profile.
 */
internal fun MacrobenchmarkScope.grantCamera() {
    device.executeShellCommand("pm grant $packageName android.permission.CAMERA")
}

/** A cold start to the home screen, closing the first-launch download offer if it shows. */
internal fun MacrobenchmarkScope.startToHome() {
    pressHome()
    startActivityAndWait()
    device.wait(Until.hasObject(By.res("pokedex:list")), WAIT_MS)
    device.wait(Until.findObject(By.res("offer:skip")), 1_000)?.click()
}

/** Flings the species list down and back up: the list rows, their art and the type chips. */
internal fun MacrobenchmarkScope.scrollList() {
    val list = device.wait(Until.findObject(By.res("pokedex:list")), WAIT_MS) ?: return
    list.setGestureMargin(device.displayWidth / 5)
    repeat(3) {
        list.fling(Direction.DOWN)
        device.waitForIdle()
    }
    repeat(3) {
        list.fling(Direction.UP)
        device.waitForIdle()
    }
}

/** Searches by name and opens the match's entry, scrolls it and goes back. */
internal fun MacrobenchmarkScope.searchAndOpenEntry() {
    val search = device.wait(Until.findObject(By.res("pokedex:search")), WAIT_MS) ?: return
    search.click()
    search.text = "pikachu"
    device.wait(Until.findObject(By.res("pokedex:row:25")), WAIT_MS)?.click() ?: return
    val entry = device.wait(Until.findObject(By.res("entry:list")), WAIT_MS) ?: return
    entry.setGestureMargin(device.displayWidth / 5)
    entry.fling(Direction.DOWN)
    device.waitForIdle()
    entry.fling(Direction.UP)
    device.waitForIdle()
    device.pressBack()
    device.wait(Until.findObject(By.res("pokedex:clear")), WAIT_MS)?.click()
    device.pressBack() // closes the keyboard if it is still open
    device.wait(Until.hasObject(By.res("pokedex:list")), WAIT_MS)
}

/** Opens Settings and comes back. */
internal fun MacrobenchmarkScope.openSettings() {
    device.wait(Until.findObject(By.res("pokedex:settings")), WAIT_MS)?.click() ?: return
    device.wait(Until.hasObject(By.res("settings:list")), WAIT_MS)
    device.pressBack()
    device.wait(Until.hasObject(By.res("pokedex:list")), WAIT_MS)
}
