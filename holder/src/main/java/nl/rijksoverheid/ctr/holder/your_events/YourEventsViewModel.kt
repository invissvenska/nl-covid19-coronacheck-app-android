/*
 * Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 * Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 * SPDX-License-Identifier: EUPL-1.2
 */

package nl.rijksoverheid.ctr.holder.your_events

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import nl.rijksoverheid.ctr.holder.models.HolderStep
import nl.rijksoverheid.ctr.persistence.database.DatabaseSyncerResult
import nl.rijksoverheid.ctr.persistence.database.HolderDatabase
import nl.rijksoverheid.ctr.persistence.database.HolderDatabaseSyncer
import nl.rijksoverheid.ctr.persistence.database.entities.OriginType
import nl.rijksoverheid.ctr.persistence.database.util.YourEventFragmentEndStateUtil
import nl.rijksoverheid.ctr.holder.get_events.models.RemoteEvent
import nl.rijksoverheid.ctr.holder.get_events.models.RemoteProtocol
import nl.rijksoverheid.ctr.holder.your_events.usecases.SaveEventsUseCase
import nl.rijksoverheid.ctr.holder.your_events.usecases.SaveEventsUseCaseImpl
import nl.rijksoverheid.ctr.holder.your_events.utils.RemoteEventUtil
import nl.rijksoverheid.ctr.shared.livedata.Event
import nl.rijksoverheid.ctr.shared.models.AppErrorResult
import nl.rijksoverheid.ctr.shared.models.Flow

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
abstract class YourEventsViewModel : ViewModel() {
    val loading: LiveData<Event<Boolean>> = MutableLiveData()
    val yourEventsResult: LiveData<Event<DatabaseSyncerResult>> = MutableLiveData()
    val conflictingEventsResult: LiveData<Event<Boolean>> = MutableLiveData()

    abstract fun saveRemoteProtocolEvents(
        flow: Flow,
        remoteProtocols: Map<RemoteProtocol, ByteArray>,
        removePreviousEvents: Boolean
    )

    abstract fun checkForConflictingEvents(remoteProtocols: Map<RemoteProtocol, ByteArray>)
}

data class RemoteEventInformation(
    val providerIdentifier: String,
    val holder: RemoteProtocol.Holder?,
    val remoteEvent: RemoteEvent
)

class YourEventsViewModelImpl(
    private val saveEventsUseCase: SaveEventsUseCase,
    private val holderDatabaseSyncer: HolderDatabaseSyncer,
    private val holderDatabase: HolderDatabase,
    private val yourEventFragmentEndStateUtil: YourEventFragmentEndStateUtil,
    private val remoteEventUtil: RemoteEventUtil
) : YourEventsViewModel() {

    override fun checkForConflictingEvents(remoteProtocols: Map<RemoteProtocol, ByteArray>) {
        (loading as MutableLiveData).value = Event(true)
        viewModelScope.launch {
            try {
                val conflictingEvents =
                    saveEventsUseCase.remoteProtocols3AreConflicting(remoteProtocols)

                (conflictingEventsResult as MutableLiveData).postValue(Event(conflictingEvents))
            } catch (e: Exception) {
                (yourEventsResult as MutableLiveData).value = Event(
                    DatabaseSyncerResult.Failed.Error(AppErrorResult(HolderStep.StoringEvents, e))
                )
            } finally {
                loading.value = Event(false)
            }
        }
    }

    override fun saveRemoteProtocolEvents(
        flow: Flow,
        remoteEvents: Map<RemoteProtocol, ByteArray>,
        removePreviousEvents: Boolean
    ) {
        (loading as MutableLiveData).value = Event(true)
        viewModelScope.launch {
            try {
                // Save the events in the database
                val result = saveEventsUseCase.saveRemoteProtocols3(
                    remoteProtocols = remoteEvents,
                    removePreviousEvents = removePreviousEvents,
                    flow = flow
                )

                when (result) {
                    is SaveEventsUseCaseImpl.SaveEventResult.Success -> {
                        // Send all events to database and create green cards, origins and credentials
                        val databaseSyncerResult = holderDatabaseSyncer.sync(
                            flow = flow,
                            expectedOriginType = getExpectedOriginType(
                                remoteEvents.keys
                                    .flatMap { it.events ?: emptyList() }
                                    .map { remoteEventUtil.getOriginType(it) })
                        )

                        (yourEventsResult as MutableLiveData).value = Event(
                            databaseSyncerResult
                        )
                    }
                    is SaveEventsUseCaseImpl.SaveEventResult.Failed -> {
                        (yourEventsResult as MutableLiveData).value =
                            Event(DatabaseSyncerResult.Failed.Error(result.errorResult))
                    }
                }
            } catch (e: Exception) {
                (yourEventsResult as MutableLiveData).value = Event(
                    DatabaseSyncerResult.Failed.Error(AppErrorResult(HolderStep.StoringEvents, e))
                )
            } finally {
                loading.value = Event(false)
            }
        }
    }

    /**
     * The expected origin for the green cards is the origin of which events were fetched.
     * In the case of an expired recovery event to complete vaccination there should no expected origin
     * since the the origins could be combined into a single vaccination.
     *
     * @param[originType] origin type of events fetched
     * @return origin type to be expected from signer or null if it's not expected
     */
    private suspend fun getExpectedOriginType(originType: List<OriginType>): OriginType? {
        if (originType.size > 1) return null
        val events = holderDatabase.eventGroupDao().getAll()
        return if (!yourEventFragmentEndStateUtil.hasVaccinationAndRecoveryEvents(events)) originType.firstOrNull() else null
    }
}
