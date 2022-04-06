/*
 * Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 * Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 * SPDX-License-Identifier: EUPL-1.2
 */

package nl.rijksoverheid.ctr.holder.saved_events

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import nl.rijksoverheid.ctr.design.ext.formatDateTime
import nl.rijksoverheid.ctr.design.ext.formatDayMonthYear
import nl.rijksoverheid.ctr.design.ext.formatDayMonthYearTime
import nl.rijksoverheid.ctr.holder.get_events.models.*
import nl.rijksoverheid.ctr.holder.paper_proof.usecases.GetEventsFromPaperProofQrUseCase
import nl.rijksoverheid.ctr.holder.your_events.utils.EventGroupEntityUtil
import nl.rijksoverheid.ctr.holder.your_events.utils.InfoScreenUtil
import nl.rijksoverheid.ctr.holder.your_events.utils.RemoteEventUtil
import nl.rijksoverheid.ctr.holder.your_events.utils.YourEventsFragmentUtil
import nl.rijksoverheid.ctr.persistence.database.HolderDatabase
import nl.rijksoverheid.ctr.persistence.database.entities.EventGroupEntity
import nl.rijksoverheid.ctr.shared.livedata.Event
import org.json.JSONObject

class SavedEventsViewModel(
    private val application: Application,
    private val holderDatabase: HolderDatabase,
    private val remoteEventUtil: RemoteEventUtil,
    private val eventGroupEntityUtil: EventGroupEntityUtil,
    private val getEventsFromPaperProofQrUseCase: GetEventsFromPaperProofQrUseCase,
    private val infoScreenUtil: InfoScreenUtil,
    private val yourEventsFragmentUtil: YourEventsFragmentUtil
): ViewModel() {

    val savedEventsLiveData: LiveData<Event<List<SavedEvents>>> = MutableLiveData()
    val removedSavedEventsLiveData: LiveData<Unit> = MutableLiveData()

    fun getSavedEvents() {
        viewModelScope.launch {
            val eventGroups = holderDatabase.eventGroupDao().getAll().asReversed()

            val savedEvents = eventGroups.map { eventGroupEntity ->
                val isDccEvent = remoteEventUtil.isDccEvent(
                    providerIdentifier = eventGroupEntity.providerIdentifier
                )
                val remoteProtocol = if (isDccEvent) {
                    val credential =
                        JSONObject(eventGroupEntity.jsonData.decodeToString()).getString("credential")
                    getEventsFromPaperProofQrUseCase.get(credential)
                } else {
                    remoteEventUtil.getRemoteProtocol3FromNonDcc(
                        eventGroupEntity = eventGroupEntity
                    )
                }

                val fullName = yourEventsFragmentUtil.getFullName(remoteProtocol?.holder)
                val birthDate = yourEventsFragmentUtil.getBirthDate(remoteProtocol?.holder)

                SavedEvents(
                    providerName = eventGroupEntityUtil.getProviderName(
                        providerIdentifier = eventGroupEntity.providerIdentifier
                    ),
                    eventGroupEntity = eventGroupEntity,
                    events = remoteProtocol?.events?.mapNotNull { remoteEvent ->
                        val infoScreen = when (remoteEvent) {
                            is RemoteEventVaccination -> {
                                infoScreenUtil.getForVaccination(
                                    event = remoteEvent,
                                    fullName = fullName,
                                    birthDate = birthDate,
                                    providerIdentifier = eventGroupEntity.providerIdentifier,
                                    isPaperProof = isDccEvent
                                )
                            }
                            is RemoteEventRecovery -> {
                                infoScreenUtil.getForRecovery(
                                    event = remoteEvent,
                                    testDate = remoteEvent.recovery?.sampleDate?.formatDayMonthYear()
                                        ?: "",
                                    fullName = fullName,
                                    birthDate = birthDate,
                                    isPaperProof = isDccEvent
                                )
                            }
                            is RemoteEventPositiveTest -> {
                                infoScreenUtil.getForPositiveTest(
                                    event = remoteEvent,
                                    testDate = remoteEvent.positiveTest?.sampleDate?.formatDayMonthYearTime(
                                        application
                                    ) ?: "",
                                    fullName = fullName,
                                    birthDate = birthDate
                                )
                            }
                            is RemoteEventNegativeTest -> {
                                infoScreenUtil.getForNegativeTest(
                                    event = remoteEvent,
                                    fullName = fullName,
                                    testDate = remoteEvent.negativeTest?.sampleDate?.formatDateTime(
                                        application
                                    ) ?: "",
                                    birthDate = birthDate,
                                    isPaperProof = isDccEvent
                                )
                            }
                            is RemoteEventVaccinationAssessment -> {
                                infoScreenUtil.getForVaccinationAssessment(
                                    event = remoteEvent,
                                    fullName = fullName,
                                    birthDate = birthDate
                                )
                            }
                            else -> {
                                null
                            }
                        }

                        if (infoScreen != null) {
                            SavedEvents.SavedEvent(
                                remoteEvent = remoteEvent,
                                infoScreen = infoScreen
                            )
                        } else {
                            null
                        }
                    } ?: listOf()
                )
            }
            (savedEventsLiveData as MutableLiveData).postValue(Event(savedEvents))
        }
    }

    fun removeSavedEvents(eventGroupEntity: EventGroupEntity) {
        viewModelScope.launch {
            holderDatabase.eventGroupDao().delete(eventGroupEntity)
            (removedSavedEventsLiveData as MutableLiveData).postValue(Unit)
        }
    }
}