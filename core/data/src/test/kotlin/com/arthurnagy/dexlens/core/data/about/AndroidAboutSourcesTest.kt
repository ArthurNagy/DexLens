package com.arthurnagy.dexlens.core.data.about

import android.content.Context
import android.content.pm.PackageInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.testing.TestDispatcherProvider
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import kotlin.test.assertEquals
import kotlin.test.assertIs

@RunWith(AndroidJUnit4::class)
internal class AndroidAboutSourcesTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun installVersion(versionName: String?) {
        shadowOf(context.packageManager).installPackage(
            PackageInfo().apply {
                packageName = context.packageName
                this.versionName = versionName
            },
        )
    }

    // Scenario: Version shown
    @Test
    fun theInstalledVersionNameIsReturned() = runTest {
        installVersion("0.1.0")

        assertEquals(Ok("0.1.0"), PackageAppInfoRepository(context).versionName())
    }

    // Scenario: Version unavailable
    @Test
    fun aMissingVersionNameIsUnknown() = runTest {
        installVersion(null)

        assertIs<DomainError.Unknown>(PackageAppInfoRepository(context).versionName().component2())
    }

    // Scenario: Licences listed offline
    @Test
    fun withoutTheGeneratedResourceTheListIsUnknown() = runTest {
        val repository = BundledOpenSourceLibraryRepository(AndroidNoticesSource(context), TestDispatcherProvider(testScheduler))

        assertIs<DomainError.Unknown>(repository.libraries().component2())
    }
}
