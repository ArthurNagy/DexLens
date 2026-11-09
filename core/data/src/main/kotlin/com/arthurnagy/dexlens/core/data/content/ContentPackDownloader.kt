package com.arthurnagy.dexlens.core.data.content

import com.arthurnagy.dexlens.core.data.catchStorage
import com.arthurnagy.dexlens.core.database.SpeciesDao
import com.arthurnagy.dexlens.core.domain.DispatcherProvider
import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.network.catchNetwork
import com.arthurnagy.dexlens.core.network.content.ContentRemoteDataSource
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.getError
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.onErr
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/** Items done so far out of [total]. */
internal data class ContentProgress(
    val completed: Int,
    val total: Int,
)

/**
 * Downloads whatever is still missing, at most [MAX_CONCURRENT] files at a time. Finished files stay on disk, so a run
 * that fails or is killed simply continues next time. The first failure stops new requests; files in flight finish.
 */
internal class ContentPackDownloader
    @Inject
    constructor(
        private val remote: ContentRemoteDataSource,
        private val files: ContentFileStore,
        private val transcoder: ImageTranscoder,
        private val importer: FlavorTextImporter,
        private val speciesDao: SpeciesDao,
        private val dispatchers: DispatcherProvider,
    ) {
        @OptIn(ExperimentalAtomicApi::class)
        suspend fun download(progress: MutableStateFlow<ContentProgress>): Result<Unit, DomainError> {
            val ids = catchStorage { speciesDao.speciesIds() }.getOrElse { return Err(it) }
            val (stored, flavorDone) = catchStorage { files.storedIds() to files.isFlavorImported() }.getOrElse { return Err(it) }
            val total = ids.size + FLAVOR_STEP
            val pending = ids.filterNot(stored::contains)
            progress.value = ContentProgress(total - pending.size - if (flavorDone) 0 else FLAVOR_STEP, total)

            val failure = AtomicReference<DomainError?>(null)
            val semaphore = Semaphore(MAX_CONCURRENT)
            supervisorScope {
                pending
                    .map { id ->
                        async(dispatchers.io) {
                            semaphore.withPermit {
                                if (failure.load() == null) {
                                    fetch(id).onErr { failure.compareAndSet(null, it) }
                                    progress.advance()
                                }
                            }
                        }
                    }.awaitAll()
            }
            failure.load()?.let { return Err(it) }
            val flavor = if (flavorDone) Ok(Unit) else importFlavorText(progress)
            return flavor.andThen { catchStorage { files.markComplete() } }
        }

        private suspend fun fetch(id: Int): Result<Unit, DomainError> {
            val png = catchNetwork { remote.artwork(id) }
                .getOrElse { error -> return if (error == DomainError.NotFound) Ok(Unit) else Err(error) }
            return catchStorage { files.write(id, transcoder.toWebp(png, ARTWORK_SIZE)) }
        }

        private suspend fun importFlavorText(progress: MutableStateFlow<ContentProgress>): Result<Unit, DomainError> {
            val imported = catchNetwork { remote.flavorText { stream -> importer.import(stream) } }
            imported.getError()?.let { return Err(it) }
            return catchStorage {
                files.markFlavorImported()
                progress.advance()
            }
        }

        private fun MutableStateFlow<ContentProgress>.advance() = update { it.copy(completed = it.completed + 1) }

        private companion object {
            const val MAX_CONCURRENT = 6
            const val ARTWORK_SIZE = 384
            const val FLAVOR_STEP = 1
        }
    }
