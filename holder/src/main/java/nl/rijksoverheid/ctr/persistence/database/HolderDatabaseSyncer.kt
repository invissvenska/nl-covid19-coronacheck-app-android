package nl.rijksoverheid.ctr.persistence.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import nl.rijksoverheid.ctr.holder.dashboard.util.GreenCardUtil
import nl.rijksoverheid.ctr.holder.models.HolderFlow
import nl.rijksoverheid.ctr.persistence.database.entities.*
import nl.rijksoverheid.ctr.persistence.database.models.YourEventFragmentEndState
import nl.rijksoverheid.ctr.persistence.database.models.YourEventFragmentEndState.*
import nl.rijksoverheid.ctr.persistence.database.usecases.*
import nl.rijksoverheid.ctr.persistence.database.util.YourEventFragmentEndStateUtil
import nl.rijksoverheid.ctr.holder.usecases.HolderFeatureFlagUseCase
import nl.rijksoverheid.ctr.shared.models.DisclosurePolicy
import nl.rijksoverheid.ctr.shared.models.ErrorResult
import nl.rijksoverheid.ctr.shared.models.Flow
import nl.rijksoverheid.ctr.shared.models.NetworkRequestResult
import java.time.OffsetDateTime

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
interface HolderDatabaseSyncer {

    /**
     * Synchronized the database. Does cleanup in the database based on expiration dates and can resync with remote
     * @param expectedOriginType If not null checks if the remote credentials contain this origin. Will return [DatabaseSyncerResult.MissingOrigin] if it's not present.
     * @param syncWithRemote If true and the data call to resync succeeds, clear all green cards in the database and re-add them
     * @param previousSyncResult The previous result outputted by this [sync] if known
     */
    suspend fun sync(
        flow: Flow = HolderFlow.Startup,
        expectedOriginType: OriginType? = null,
        syncWithRemote: Boolean = true,
        previousSyncResult: DatabaseSyncerResult? = null
    ): DatabaseSyncerResult
}

class HolderDatabaseSyncerImpl(
    private val holderDatabase: HolderDatabase,
    private val greenCardUtil: GreenCardUtil,
    private val getRemoteGreenCardsUseCase: GetRemoteGreenCardsUseCase,
    private val syncRemoteGreenCardsUseCase: SyncRemoteGreenCardsUseCase,
    private val removeExpiredEventsUseCase: RemoveExpiredEventsUseCase,
    private val featureFlagUseCase: HolderFeatureFlagUseCase,
    private val yourEventFragmentEndStateUtil: YourEventFragmentEndStateUtil
) : HolderDatabaseSyncer {

    private val mutex = Mutex()

    override suspend fun sync(
        flow: Flow,
        expectedOriginType: OriginType?,
        syncWithRemote: Boolean,
        previousSyncResult: DatabaseSyncerResult?
    ): DatabaseSyncerResult {
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                val events = holderDatabase.eventGroupDao().getAll()

                // Clean up expired events in the database
                removeExpiredEventsUseCase.execute(
                    events = events
                )

                // Sync with remote
                if (syncWithRemote && events.isNotEmpty()) {
                    val remoteGreenCardsResult = getRemoteGreenCardsUseCase.get(
                        events = events
                    )

                    when (remoteGreenCardsResult) {
                        is RemoteGreenCardsResult.Success -> {
                            val remoteGreenCards = remoteGreenCardsResult.remoteGreenCards
                            val combinedVaccinationRecovery =
                                yourEventFragmentEndStateUtil.getResult(
                                    flow = flow,
                                    storedGreenCards = holderDatabase.greenCardDao().getAll(),
                                    events = events,
                                    remoteGreenCards = remoteGreenCards
                                )

                            // If we expect the remote green cards to have a certain origin
                            // We ignore the vaccination assessment origin here because you can
                            // fetch send a vaccination assessment event origin but not get a
                            // green card vaccination assessment origin back
                            val originsToCheck = if (featureFlagUseCase.getDisclosurePolicy() is DisclosurePolicy.ZeroG) {
                                // If 0G is enabled we only have the international tab enabled
                                remoteGreenCards.getEuOrigins()
                            } else {
                                remoteGreenCards.getAllOrigins()
                            }

                            if ((expectedOriginType != null && !originsToCheck
                                    .contains(expectedOriginType) && expectedOriginType != OriginType.VaccinationAssessment)
                                || remoteGreenCards.getAllOrigins().isEmpty()
                            ) {
                                return@withContext DatabaseSyncerResult.Success(
                                    missingOrigin = true,
                                    combinedVaccinationRecovery
                                )
                            }

                            // Insert green cards in database
                            val result = syncRemoteGreenCardsUseCase.execute(
                                remoteGreenCards = remoteGreenCards
                            )

                            when (result) {
                                is SyncRemoteGreenCardsResult.Success -> {
                                    return@withContext DatabaseSyncerResult.Success(
                                        false,
                                        combinedVaccinationRecovery
                                    )
                                }
                                is SyncRemoteGreenCardsResult.Failed -> {
                                    return@withContext DatabaseSyncerResult.Failed.Error(result.errorResult)
                                }
                            }
                        }
                        is RemoteGreenCardsResult.Error -> {
                            val greenCards = holderDatabase.greenCardDao().getAll()

                            when (remoteGreenCardsResult.errorResult) {
                                is NetworkRequestResult.Failed.ClientNetworkError, is NetworkRequestResult.Failed.ServerNetworkError -> {
                                    DatabaseSyncerResult.Failed.NetworkError(
                                        errorResult = remoteGreenCardsResult.errorResult,
                                        hasGreenCardsWithoutCredentials = greenCards
                                            .any { greenCardUtil.hasNoActiveCredentials(it) }
                                    )
                                }
                                is NetworkRequestResult.Failed.CoronaCheckHttpError -> {
                                    if (previousSyncResult == null) {
                                        DatabaseSyncerResult.Failed.ServerError.FirstTime(
                                            errorResult = remoteGreenCardsResult.errorResult
                                        )
                                    } else {
                                        DatabaseSyncerResult.Failed.ServerError.MultipleTimes(
                                            errorResult = remoteGreenCardsResult.errorResult
                                        )
                                    }
                                }
                                else -> {
                                    DatabaseSyncerResult.Failed.Error(
                                        errorResult = remoteGreenCardsResult.errorResult
                                    )
                                }
                            }
                        }
                    }
                } else {
                    previousSyncResult ?: DatabaseSyncerResult.Success(false)
                }
            }
        }
    }
}

sealed class DatabaseSyncerResult {
    data class Success(
        val missingOrigin: Boolean = false,
        val yourEventFragmentEndState: YourEventFragmentEndState = NotApplicable
    ) : DatabaseSyncerResult()

    sealed class Failed(open val errorResult: ErrorResult, open val failedAt: OffsetDateTime) :
        DatabaseSyncerResult() {
        data class NetworkError(
            override val errorResult: ErrorResult,
            val hasGreenCardsWithoutCredentials: Boolean
        ) : Failed(errorResult, OffsetDateTime.now())

        sealed class ServerError(override val errorResult: ErrorResult) :
            Failed(errorResult, OffsetDateTime.now()) {
            data class FirstTime(override val errorResult: ErrorResult) : ServerError(errorResult)
            data class MultipleTimes(override val errorResult: ErrorResult) :
                ServerError(errorResult)
        }

        data class Error(override val errorResult: ErrorResult) :
            Failed(errorResult, OffsetDateTime.now())
    }
}
