/*
 * Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 * Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 * SPDX-License-Identifier: EUPL-1.2
 */

package nl.rijksoverheid.ctr.holder.your_events.usecases

import nl.rijksoverheid.ctr.holder.models.HolderFlow
import nl.rijksoverheid.ctr.holder.models.HolderStep
import nl.rijksoverheid.ctr.persistence.database.HolderDatabase
import nl.rijksoverheid.ctr.persistence.database.entities.EventGroupEntity
import nl.rijksoverheid.ctr.persistence.database.entities.OriginType
import nl.rijksoverheid.ctr.holder.get_events.models.RemoteEvent
import nl.rijksoverheid.ctr.holder.get_events.models.RemoteProtocol3
import nl.rijksoverheid.ctr.holder.get_events.models.RemoteTestResult2
import nl.rijksoverheid.ctr.holder.your_events.utils.RemoteEventHolderUtil
import nl.rijksoverheid.ctr.holder.your_events.utils.RemoteEventUtil
import nl.rijksoverheid.ctr.holder.get_events.utils.ScopeUtil
import nl.rijksoverheid.ctr.shared.models.AppErrorResult
import nl.rijksoverheid.ctr.shared.models.ErrorResult
import nl.rijksoverheid.ctr.shared.models.Flow
import java.time.OffsetDateTime

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
interface SaveEventsUseCase {
    suspend fun saveNegativeTest2(
        negativeTest2: RemoteTestResult2,
        rawResponse: ByteArray
    ): SaveEventsUseCaseImpl.SaveEventResult

    suspend fun saveRemoteProtocols3(
        remoteProtocols3: Map<RemoteProtocol3, ByteArray>,
        removePreviousEvents: Boolean,
        flow: Flow,
    ): SaveEventsUseCaseImpl.SaveEventResult

    suspend fun remoteProtocols3AreConflicting(remoteProtocols3: Map<RemoteProtocol3, ByteArray>): Boolean
}

class SaveEventsUseCaseImpl(
    private val holderDatabase: HolderDatabase,
    private val remoteEventHolderUtil: RemoteEventHolderUtil,
    private val scopeUtil: ScopeUtil,
    private val remoteEventUtil: RemoteEventUtil
) : SaveEventsUseCase {

    override suspend fun saveNegativeTest2(
        negativeTest2: RemoteTestResult2,
        rawResponse: ByteArray
    ): SaveEventResult {
        try {
            // Make remote test results to event group entities to save in the database
            val entity = EventGroupEntity(
                walletId = 1,
                providerIdentifier = negativeTest2.providerIdentifier,
                type = OriginType.Test,
                maxIssuedAt = negativeTest2.result?.sampleDate!!,
                jsonData = rawResponse,
                scope = ""
            )

            // Save entity in database
            holderDatabase.eventGroupDao().insertAll(listOf(entity))

            return SaveEventResult.Success
        } catch (e: Exception) {
            return SaveEventResult.Failed(
                errorResult = AppErrorResult(
                    step = HolderStep.StoringEvents,
                    e = e
                )
            )
        }
    }

    override suspend fun remoteProtocols3AreConflicting(remoteProtocols3: Map<RemoteProtocol3, ByteArray>): Boolean {
        val storedEventHolders = holderDatabase.eventGroupDao().getAll()
            .mapNotNull { remoteEventHolderUtil.holder(it.jsonData, it.providerIdentifier) }
            .distinct()
        val incomingEventHolders = remoteProtocols3.map { it.key.holder!! }.distinct()

        return remoteEventHolderUtil.conflicting(storedEventHolders, incomingEventHolders)
    }

    override suspend fun saveRemoteProtocols3(
        remoteProtocols3: Map<RemoteProtocol3, ByteArray>,
        removePreviousEvents: Boolean,
        flow: Flow,
    ): SaveEventResult {
        try {
            if (removePreviousEvents) {
                holderDatabase.eventGroupDao().deleteAll()
            }

            val entities = remoteProtocols3.map {
                val remoteEvents = it.key.events ?: listOf()
                val originType = remoteEventUtil.getOriginType(remoteEvents.first())
                EventGroupEntity(
                    walletId = 1,
                    providerIdentifier = it.key.providerIdentifier,
                    type = originType,
                    maxIssuedAt = getMaxIssuedAt(remoteEvents),
                    jsonData = it.value,
                    scope = scopeUtil.getScopeForOriginType(
                        originType = originType,
                        getPositiveTestWithVaccination = flow == HolderFlow.VaccinationAndPositiveTest
                    )
                )
            }

            // Save entity in database
            holderDatabase.eventGroupDao().insertAll(entities)
        } catch (e: Exception) {
            return SaveEventResult.Failed(
                errorResult = AppErrorResult(
                    step = HolderStep.StoringEvents,
                    e = e
                )
            )
        }
        return SaveEventResult.Success
    }

    private fun getMaxIssuedAt(remoteEvents: List<RemoteEvent>): OffsetDateTime {
        return remoteEvents.map { event -> event.getDate() }
            .maxByOrNull { date -> date?.toEpochSecond() ?: error("Date should not be null") }
            ?: error("At least one event must be present with a date")
    }

    sealed class SaveEventResult {
        object Success : SaveEventResult()
        data class Failed(val errorResult: ErrorResult) : SaveEventResult()
    }
}
