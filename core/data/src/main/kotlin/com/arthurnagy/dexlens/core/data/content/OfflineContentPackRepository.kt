package com.arthurnagy.dexlens.core.data.content

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.arthurnagy.dexlens.core.data.catchStorage
import com.arthurnagy.dexlens.core.database.ContentDao
import com.arthurnagy.dexlens.core.database.SpeciesDao
import com.arthurnagy.dexlens.core.domain.DispatcherProvider
import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.content.ContentPackRepository
import com.arthurnagy.dexlens.core.model.ArtworkLocation
import com.arthurnagy.dexlens.core.model.ContentPackState
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOr
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class OfflineContentPackRepository
    @Inject
    constructor(
        private val workManager: WorkManager,
        private val files: ContentFileStore,
        private val contentDao: ContentDao,
        private val speciesDao: SpeciesDao,
        private val dispatchers: DispatcherProvider,
    ) : ContentPackRepository {
        /** Bumped after a deletion, because no work state changes then. */
        private val refresh = MutableStateFlow(0)

        override fun observeState(): Flow<ContentPackState> =
            combine(workManager.getWorkInfosForUniqueWorkFlow(WORK_NAME), refresh) { infos, _ -> infos.firstOrNull() }
                .map { info -> stateFor(info) }
                .distinctUntilChanged()
                .flowOn(dispatchers.io)

        private suspend fun stateFor(info: WorkInfo?): ContentPackState {
            val total = catchStorage { speciesDao.speciesCount() * ArtKind.entries.size + 1 }.getOr(UNKNOWN_TOTAL)
            val completed = files.completedFileCount() + if (files.isFlavorImported()) 1 else 0
            return when {
                info != null && (info.state == WorkInfo.State.RUNNING || info.state == WorkInfo.State.ENQUEUED) -> {
                    ContentPackState.Downloading(
                        completed = info.progress.getInt(ContentPackWorker.KEY_COMPLETED, completed),
                        total = info.progress.getInt(ContentPackWorker.KEY_TOTAL, total),
                    )
                }

                files.isComplete() -> {
                    ContentPackState.Ready
                }

                completed > 0 -> {
                    ContentPackState.Interrupted(completed, total)
                }

                else -> {
                    ContentPackState.NotDownloaded
                }
            }
        }

        override fun observeArtwork(): Flow<ArtworkLocation> = observeState()
            .map { state ->
                val hasFiles = state !is ContentPackState.NotDownloaded
                ArtworkLocation(if (hasFiles) files.root.path else null)
            }.distinctUntilChanged()

        override fun start() {
            val request = OneTimeWorkRequestBuilder<ContentPackWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
        }

        override fun cancel() {
            workManager.cancelUniqueWork(WORK_NAME)
        }

        override suspend fun delete(): Result<Unit, DomainError> = catchStorage {
            workManager.cancelUniqueWork(WORK_NAME)
            withContext(dispatchers.io) { files.clear() }
            contentDao.deleteAll()
            refresh.value += 1
        }

        private companion object {
            const val WORK_NAME = "content-pack-download"

            /** Only reached when the bundled database cannot be read. The state still follows the files on disk. */
            const val UNKNOWN_TOTAL = 0
        }
    }
