package nl.rijksoverheid.ctr.holder.ui.create_qr.usecases

import io.mockk.*
import kotlinx.coroutines.runBlocking
import nl.rijksoverheid.ctr.holder.persistence.database.HolderDatabase
import nl.rijksoverheid.ctr.holder.persistence.database.dao.EventGroupDao
import nl.rijksoverheid.ctr.holder.persistence.database.entities.EventGroupEntity
import nl.rijksoverheid.ctr.holder.persistence.database.entities.OriginType
import nl.rijksoverheid.ctr.holder.ui.create_qr.models.*
import nl.rijksoverheid.ctr.holder.ui.create_qr.util.RemoteEventHolderUtil
import nl.rijksoverheid.ctr.holder.ui.create_qr.util.ScopeUtilImpl
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
@RunWith(RobolectricTestRunner::class)
class SaveEventsUseCaseImplTest {

    private val eventGroupDao: EventGroupDao = mockk(relaxed = true)
    private val holderDatabase: HolderDatabase = mockk {
        every { eventGroupDao() } returns eventGroupDao
    }
    private val scopeUtil = ScopeUtilImpl()

    private val remoteEventHolderUtil: RemoteEventHolderUtil = mockk(relaxed = true)

    @After
    fun tearDown() {
        stopKoin()
    }

    private val saveEventsUseCaseImpl = SaveEventsUseCaseImpl(holderDatabase, remoteEventHolderUtil,
        scopeUtil
    )

    @Test
    fun `when saving vaccinations it should be inserted into the database with old one deleted`() {
        val remoteProtocol3 = createRemoteProtocol3(createVaccination())
        val byteArray = ByteArray(1)
        val remoteProtocols3 = mapOf(remoteProtocol3 to byteArray)

        runBlocking {
            saveEventsUseCaseImpl.saveRemoteProtocols3(
                listOf(ProtocolOrigin(OriginType.Vaccination, remoteProtocols3)),
                true
            )

            coVerify { eventGroupDao.deleteAll() }
            coVerify {
                eventGroupDao.insertAll(
                    remoteProtocols3.map {
                        mapEventsToEntity(
                            it.key,
                            it.value,
                            OriginType.Vaccination,
                            scopeUtil.getScopeForOriginType(OriginType.Vaccination, false)
                        )
                    }
                )
            }
        }
    }

    @Test
    fun `when saving recoveries it should be inserted into the database with old one deleted`() {
        val remoteProtocol3 = createRemoteProtocol3(createRecovery())
        val byteArray = ByteArray(1)
        val remoteProtocols3 = mapOf(remoteProtocol3 to byteArray)

        runBlocking {
            saveEventsUseCaseImpl.saveRemoteProtocols3(
                listOf(ProtocolOrigin(OriginType.Recovery, remoteProtocols3)),
                true,
            )

            coVerify { eventGroupDao.deleteAll() }
            coVerify {
                eventGroupDao.insertAll(
                    remoteProtocols3.map {
                        mapEventsToEntity(
                            it.key,
                            it.value,
                            OriginType.Recovery,
                            scopeUtil.getScopeForOriginType(OriginType.Recovery, false)
                        )
                    }
                )
            }
        }
    }

    @Test
    fun `when saving recovery and vaccination it should both be inserted into the database with old one deleted`() {
        val remoteProtocol3Recovery = createRemoteProtocol3(createRecovery())
        val byteArrayRecovery = ByteArray(1)
        val remoteProtocols3Recovery = mapOf(remoteProtocol3Recovery to byteArrayRecovery)

        val remoteProtocol3Vaccination = createRemoteProtocol3(createRecovery())
        val byteArray2Vaccination = ByteArray(1)
        val remoteProtocols3Vaccination = mapOf(remoteProtocol3Vaccination to byteArray2Vaccination)

        runBlocking {
            saveEventsUseCaseImpl.saveRemoteProtocols3(
                listOf(
                    ProtocolOrigin(OriginType.Recovery, remoteProtocols3Recovery),
                    ProtocolOrigin(OriginType.Vaccination, remoteProtocols3Vaccination)
                ),
                true,
            )

            coVerify { eventGroupDao.deleteAll() }
            coVerify {
                eventGroupDao.insertAll(
                    remoteProtocols3Recovery.map {
                        mapEventsToEntity(
                            it.key,
                            it.value,
                            OriginType.Recovery,
                            scopeUtil.getScopeForOriginType(OriginType.Recovery, true)
                        )
                    }
                )
            }
            coVerify {
                eventGroupDao.insertAll(
                    remoteProtocols3Vaccination.map {
                        mapEventsToEntity(
                            it.key,
                            it.value,
                            OriginType.Recovery,
                            scopeUtil.getScopeForOriginType(OriginType.Recovery, true)
                        )
                    }
                )
            }
        }
    }

    private fun mapEventsToEntity(
        remoteEvents: RemoteProtocol3,
        byteArray: ByteArray,
        eventType: OriginType,
        scope: String?
    ) = EventGroupEntity(
        walletId = 1,
        providerIdentifier = remoteEvents.providerIdentifier,
        type = eventType,
        maxIssuedAt = remoteEvents.events!!.first().getDate()!!,
        jsonData = byteArray,
        scope = scope
    )

    private fun createRemoteProtocol3(remoteEvent: RemoteEvent) = RemoteProtocol3(
        events = listOf(remoteEvent),
        protocolVersion = "pro",
        providerIdentifier = "ide",
        status = RemoteProtocol.Status.COMPLETE,
        holder = null
    )

    private fun createVaccination() = RemoteEventVaccination(
        type = null,
        unique = null,
        vaccination = RemoteEventVaccination.Vaccination(
            date = LocalDate.of(2000, 1, 1),
            hpkCode = null,
            type = null,
            brand = null,
            completedByMedicalStatement = null,
            doseNumber = null,
            totalDoses = null,
            country = null,
            manufacturer = null,
            completedByPersonalStatement = null,
            completionReason = null
        )
    )

    private fun createRecovery() = RemoteEventRecovery(
        type = null,
        unique = "uni",
        isSpecimen = true,
        recovery = RemoteEventRecovery.Recovery(
            sampleDate = LocalDate.of(2000, 1, 1),
            validFrom = LocalDate.of(2000, 2, 1),
            validUntil = LocalDate.of(2000, 7, 1)
        )
    )
}