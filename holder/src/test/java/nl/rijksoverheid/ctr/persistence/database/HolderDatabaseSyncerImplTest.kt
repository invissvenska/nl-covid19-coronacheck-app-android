package nl.rijksoverheid.ctr.persistence.database

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import nl.rijksoverheid.ctr.persistence.database.dao.EventGroupDao
import nl.rijksoverheid.ctr.persistence.database.dao.GreenCardDao
import nl.rijksoverheid.ctr.persistence.database.entities.EventGroupEntity
import nl.rijksoverheid.ctr.persistence.database.entities.OriginType
import nl.rijksoverheid.ctr.persistence.database.models.YourEventFragmentEndState.NotApplicable
import nl.rijksoverheid.ctr.persistence.database.usecases.RemoteGreenCardsResult
import nl.rijksoverheid.ctr.persistence.database.usecases.SyncRemoteGreenCardsResult
import nl.rijksoverheid.ctr.holder.your_events.models.RemoteGreenCards
import nl.rijksoverheid.ctr.holder.usecases.HolderFeatureFlagUseCase
import nl.rijksoverheid.ctr.holder.fakeGetRemoteGreenCardUseCase
import nl.rijksoverheid.ctr.holder.fakeGreenCardUtil
import nl.rijksoverheid.ctr.holder.fakeRemoveExpiredEventsUseCase
import nl.rijksoverheid.ctr.holder.fakeSyncRemoteGreenCardUseCase
import nl.rijksoverheid.ctr.shared.MobileCoreWrapper
import nl.rijksoverheid.ctr.shared.models.AppErrorResult
import nl.rijksoverheid.ctr.shared.models.DisclosurePolicy
import nl.rijksoverheid.ctr.shared.models.NetworkRequestResult
import nl.rijksoverheid.ctr.shared.models.Step
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.time.OffsetDateTime

class HolderDatabaseSyncerImplTest {

    private val mobileCoreWrapper: MobileCoreWrapper = mockk(relaxed = true)
    private val greenCardDao = mockk<GreenCardDao>(relaxed = true)
    private val eventGroupDao = mockk<EventGroupDao>(relaxed = true)
    private val holderDatabase = mockk<HolderDatabase>(relaxed = true).apply {
        coEvery { eventGroupDao() } returns eventGroupDao
        coEvery { greenCardDao() } returns greenCardDao
    }
    private val events = listOf(
        EventGroupEntity(
            id = 1,
            walletId = 1,
            providerIdentifier = "1",
            type = OriginType.Test,
            expiryDate = OffsetDateTime.now(),
            scope = "",
            jsonData = "".toByteArray()
        )
    )

    @Test
    fun `sync returns Success if has no events`() = runBlocking {
        coEvery { eventGroupDao.getAll() } answers { listOf() }

        val holderDatabaseSyncer = HolderDatabaseSyncerImpl(
            holderDatabase = holderDatabase,
            workerManagerUtil = mockk(relaxed = true),
            greenCardUtil = fakeGreenCardUtil(),
            getRemoteGreenCardsUseCase = fakeGetRemoteGreenCardUseCase(
                result = successResult(
                    originType = OriginType.Test
                )
            ),
            syncRemoteGreenCardsUseCase = fakeSyncRemoteGreenCardUseCase(),
            removeExpiredEventsUseCase = fakeRemoveExpiredEventsUseCase(),
            featureFlagUseCase = mockk(),
            yourEventFragmentEndStateUtil = mockk { every { getResult(any(), any(), any(), any()) } returns NotApplicable},
            updateEventExpirationUseCase = mockk(relaxed = true),
            mobileCoreWrapper = mobileCoreWrapper
        )

        val databaseSyncerResult = holderDatabaseSyncer.sync(
            expectedOriginType = OriginType.Test,
            syncWithRemote = true
        )

        assertEquals(DatabaseSyncerResult.Success(), databaseSyncerResult)
    }

    @Test
    fun `sync returns Success if has events and nothing errors`() = runBlocking {
        coEvery { eventGroupDao.getAll() } answers { events }

        val holderDatabaseSyncer = HolderDatabaseSyncerImpl(
            holderDatabase = holderDatabase,
            workerManagerUtil = mockk(relaxed = true),
            greenCardUtil = fakeGreenCardUtil(),
            getRemoteGreenCardsUseCase = fakeGetRemoteGreenCardUseCase(
                result = successResult(
                    originType = OriginType.Test
                )
            ),
            syncRemoteGreenCardsUseCase = fakeSyncRemoteGreenCardUseCase(),
            removeExpiredEventsUseCase = fakeRemoveExpiredEventsUseCase(),
            featureFlagUseCase = mockk(relaxed = true),
            yourEventFragmentEndStateUtil = mockk { every { getResult(any(), any(), any(), any()) } returns NotApplicable },
            updateEventExpirationUseCase = mockk(relaxed = true),
            mobileCoreWrapper = mobileCoreWrapper
        )

        val databaseSyncerResult = holderDatabaseSyncer.sync(
            expectedOriginType = OriginType.Test,
            syncWithRemote = true
        )

        assertEquals(DatabaseSyncerResult.Success(), databaseSyncerResult)
    }

    @Test
    fun `sync returns Success with missingOrigin if not 0G and expected origin is not in domestic and eu green cards`() = runBlocking {
        coEvery { eventGroupDao.getAll() } answers { events }

        val holderDatabaseSyncer = HolderDatabaseSyncerImpl(
            holderDatabase = holderDatabase,
            workerManagerUtil = mockk(relaxed = true),
            greenCardUtil = fakeGreenCardUtil(),
            getRemoteGreenCardsUseCase = fakeGetRemoteGreenCardUseCase(
                result = RemoteGreenCardsResult.Success(
                    remoteGreenCards = RemoteGreenCards(
                        domesticGreencard = RemoteGreenCards.DomesticGreenCard(
                            origins = listOf(
                                RemoteGreenCards.Origin(
                                type = OriginType.Vaccination,
                                eventTime = OffsetDateTime.now(),
                                expirationTime = OffsetDateTime.now(),
                                validFrom = OffsetDateTime.now(),
                                doseNumber = 1,
                            )),
                            createCredentialMessages = "".toByteArray()
                        ),
                        euGreencards = null,
                        blobExpireDates = listOf()
                    )
                )
            ),
            syncRemoteGreenCardsUseCase = fakeSyncRemoteGreenCardUseCase(),
            removeExpiredEventsUseCase = fakeRemoveExpiredEventsUseCase(),
            featureFlagUseCase = mockk<HolderFeatureFlagUseCase>(relaxed = true).apply {
                every { getDisclosurePolicy() } answers { DisclosurePolicy.ThreeG }
            },
            yourEventFragmentEndStateUtil = mockk { every { getResult(any(), any(), any(), any()) } returns NotApplicable },
            updateEventExpirationUseCase = mockk(relaxed = true),
            mobileCoreWrapper = mobileCoreWrapper
        )

        val databaseSyncerResult = holderDatabaseSyncer.sync(
            expectedOriginType = OriginType.Test,
            syncWithRemote = true
        )

        assertEquals(DatabaseSyncerResult.Success(true), databaseSyncerResult)
    }

    @Test
    fun `sync returns Success with missingOrigin if 0G and expected origin is not in eu green cards`() = runBlocking {
        coEvery { eventGroupDao.getAll() } answers { events }

        val holderDatabaseSyncer = HolderDatabaseSyncerImpl(
            holderDatabase = holderDatabase,
            workerManagerUtil = mockk(relaxed = true),
            greenCardUtil = fakeGreenCardUtil(),
            getRemoteGreenCardsUseCase = fakeGetRemoteGreenCardUseCase(
                result = RemoteGreenCardsResult.Success(
                    remoteGreenCards = RemoteGreenCards(
                        domesticGreencard = RemoteGreenCards.DomesticGreenCard(
                            origins = listOf(
                                RemoteGreenCards.Origin(
                                type = OriginType.Test,
                                eventTime = OffsetDateTime.now(),
                                expirationTime = OffsetDateTime.now(),
                                validFrom = OffsetDateTime.now(),
                                doseNumber = 1,
                            )),
                            createCredentialMessages = "".toByteArray()
                        ),
                        euGreencards = null,
                        blobExpireDates = listOf()
                    )
                )
            ),
            syncRemoteGreenCardsUseCase = fakeSyncRemoteGreenCardUseCase(),
            removeExpiredEventsUseCase = fakeRemoveExpiredEventsUseCase(),
            featureFlagUseCase = mockk<HolderFeatureFlagUseCase>(relaxed = true).apply {
                every { getDisclosurePolicy() } answers { DisclosurePolicy.ZeroG }
            },
            yourEventFragmentEndStateUtil = mockk { every { getResult(any(), any(), any(), any()) } returns NotApplicable },
            updateEventExpirationUseCase = mockk(relaxed = true),
            mobileCoreWrapper = mobileCoreWrapper
        )

        val databaseSyncerResult = holderDatabaseSyncer.sync(
            expectedOriginType = OriginType.Test,
            syncWithRemote = true
        )

        assertEquals(DatabaseSyncerResult.Success(true), databaseSyncerResult)
    }

    @Test
    fun `sync returns Success if returned origins is vaccination assessment and does not match`() = runBlocking {
        coEvery { eventGroupDao.getAll() } answers { events }

        val holderDatabaseSyncer = HolderDatabaseSyncerImpl(
            holderDatabase = holderDatabase,
            workerManagerUtil = mockk(relaxed = true),
            greenCardUtil = fakeGreenCardUtil(),
            getRemoteGreenCardsUseCase = fakeGetRemoteGreenCardUseCase(
                result = RemoteGreenCardsResult.Success(
                    remoteGreenCards = RemoteGreenCards(
                        domesticGreencard = RemoteGreenCards.DomesticGreenCard(
                            origins = listOf(
                                RemoteGreenCards.Origin(
                                type = OriginType.VaccinationAssessment,
                                eventTime = OffsetDateTime.now(),
                                expirationTime = OffsetDateTime.now(),
                                validFrom = OffsetDateTime.now(),
                                doseNumber = 1,
                            )),
                            createCredentialMessages = "".toByteArray()
                        ),
                        euGreencards = null,
                        blobExpireDates = listOf()
                    )
                )
            ),
            syncRemoteGreenCardsUseCase = fakeSyncRemoteGreenCardUseCase(),
            removeExpiredEventsUseCase = fakeRemoveExpiredEventsUseCase(),
            featureFlagUseCase = mockk(relaxed = true),
            yourEventFragmentEndStateUtil = mockk { every { getResult(any(), any(), any(), any()) } returns NotApplicable },
            updateEventExpirationUseCase = mockk(relaxed = true),
            mobileCoreWrapper = mobileCoreWrapper
        )

        val databaseSyncerResult = holderDatabaseSyncer.sync(
            expectedOriginType = OriginType.VaccinationAssessment,
            syncWithRemote = true
        )

        assertEquals(DatabaseSyncerResult.Success(false), databaseSyncerResult)
    }

    @Test
    fun `sync returns NetworkError if getting remote green cards returns NetworkError`() = runBlocking {
        coEvery { eventGroupDao.getAll() } answers { events }
        coEvery { greenCardDao.getAll() } answers { listOf() }

        val holderDatabaseSyncer = HolderDatabaseSyncerImpl(
            holderDatabase = holderDatabase,
            workerManagerUtil = mockk(relaxed = true),
            greenCardUtil = fakeGreenCardUtil(),
            getRemoteGreenCardsUseCase = fakeGetRemoteGreenCardUseCase(
                result = RemoteGreenCardsResult.Error(NetworkRequestResult.Failed.ServerNetworkError(Step(1), IllegalStateException("Some error")))),
            syncRemoteGreenCardsUseCase = fakeSyncRemoteGreenCardUseCase(
                result = SyncRemoteGreenCardsResult.Success
            ),
            removeExpiredEventsUseCase = fakeRemoveExpiredEventsUseCase(),
            featureFlagUseCase = mockk(relaxed = true),
            yourEventFragmentEndStateUtil = mockk { every { getResult(any(), any(), any(), any()) } returns NotApplicable },
            updateEventExpirationUseCase = mockk(relaxed = true),
            mobileCoreWrapper = mobileCoreWrapper
        )

        val databaseSyncerResult = holderDatabaseSyncer.sync(
            expectedOriginType = OriginType.Test,
            syncWithRemote = true
        )

        assertTrue(databaseSyncerResult is DatabaseSyncerResult.Failed.NetworkError)
    }

    @Test
    fun `sync returns ServerError if getting remote green cards returns ServerError`() = runBlocking {
        coEvery { eventGroupDao.getAll() } answers { events }
        coEvery { greenCardDao.getAll() } answers { listOf() }

        val holderDatabaseSyncer = HolderDatabaseSyncerImpl(
            holderDatabase = holderDatabase,
            workerManagerUtil = mockk(relaxed = true),
            greenCardUtil = fakeGreenCardUtil(),
            getRemoteGreenCardsUseCase = fakeGetRemoteGreenCardUseCase(
                result = RemoteGreenCardsResult.Error(
                    NetworkRequestResult.Failed.CoronaCheckHttpError(
                        Step(1),
                        HttpException(Response.error<String>(400, "".toResponseBody())),
                        null,
                    )
                )
            ),
            syncRemoteGreenCardsUseCase = fakeSyncRemoteGreenCardUseCase(
                result = SyncRemoteGreenCardsResult.Success
            ),
            removeExpiredEventsUseCase = fakeRemoveExpiredEventsUseCase(),
            featureFlagUseCase = mockk(relaxed = true),
            yourEventFragmentEndStateUtil = mockk { every { getResult(any(), any(), any(), any()) } returns NotApplicable },
            updateEventExpirationUseCase = mockk(relaxed = true),
            mobileCoreWrapper = mobileCoreWrapper
        )

        val databaseSyncerResult = holderDatabaseSyncer.sync(
            expectedOriginType = OriginType.Test,
            syncWithRemote = true
        )

        assertTrue(databaseSyncerResult is DatabaseSyncerResult.Failed.ServerError)
    }

    @Test
    fun `sync returns Error if getting remote green cards returns Error`() = runBlocking {
        coEvery { eventGroupDao.getAll() } answers { events }
        coEvery { greenCardDao.getAll() } answers { listOf() }

        val holderDatabaseSyncer = HolderDatabaseSyncerImpl(
            holderDatabase = holderDatabase,
            workerManagerUtil = mockk(relaxed = true),
            greenCardUtil = fakeGreenCardUtil(),
            getRemoteGreenCardsUseCase = fakeGetRemoteGreenCardUseCase(
                result = RemoteGreenCardsResult.Error(NetworkRequestResult.Failed.Error(Step(1), IllegalStateException("Some error")))),
            syncRemoteGreenCardsUseCase = fakeSyncRemoteGreenCardUseCase(
                result = SyncRemoteGreenCardsResult.Success
            ),
            removeExpiredEventsUseCase = fakeRemoveExpiredEventsUseCase(),
            featureFlagUseCase = mockk(relaxed = true),
            yourEventFragmentEndStateUtil = mockk { every { getResult(any(), any(), any(), any()) } returns NotApplicable },
            updateEventExpirationUseCase = mockk(relaxed = true),
            mobileCoreWrapper = mobileCoreWrapper
        )

        val databaseSyncerResult = holderDatabaseSyncer.sync(
            expectedOriginType = OriginType.Test,
            syncWithRemote = true
        )

        assertTrue(databaseSyncerResult is DatabaseSyncerResult.Failed.Error)
    }

    @Test
    fun `sync returns Error if syncing remote green cards failed`() = runBlocking {
        coEvery { eventGroupDao.getAll() } answers { events }

        val holderDatabaseSyncer = HolderDatabaseSyncerImpl(
            holderDatabase = holderDatabase,
            workerManagerUtil = mockk(relaxed = true),
            greenCardUtil = fakeGreenCardUtil(),
            getRemoteGreenCardsUseCase = fakeGetRemoteGreenCardUseCase(
                result = RemoteGreenCardsResult.Success(
                    remoteGreenCards = RemoteGreenCards(
                        domesticGreencard = RemoteGreenCards.DomesticGreenCard(
                            origins = listOf(
                                RemoteGreenCards.Origin(
                                type = OriginType.Test,
                                eventTime = OffsetDateTime.now(),
                                expirationTime = OffsetDateTime.now(),
                                validFrom = OffsetDateTime.now(),
                                doseNumber = 1
                            )),
                            createCredentialMessages = "".toByteArray()
                        ),
                        euGreencards = null,
                        blobExpireDates = listOf()
                    )
                )
            ),
            syncRemoteGreenCardsUseCase = fakeSyncRemoteGreenCardUseCase(
                result = SyncRemoteGreenCardsResult.Failed(AppErrorResult(Step(1), IllegalStateException("Some error")))
            ),
            removeExpiredEventsUseCase = fakeRemoveExpiredEventsUseCase(),
            featureFlagUseCase = mockk(relaxed = true),
            yourEventFragmentEndStateUtil = mockk { every { getResult(any(), any(), any(), any()) } returns NotApplicable },
            updateEventExpirationUseCase = mockk(relaxed = true),
            mobileCoreWrapper = mobileCoreWrapper
        )

        val databaseSyncerResult = holderDatabaseSyncer.sync(
            expectedOriginType = OriginType.Test,
            syncWithRemote = true
        )

        assertTrue(databaseSyncerResult is DatabaseSyncerResult.Failed.Error)
    }

    @Test
    fun `sync returns Success with missingOrigin if there are no returned origins`() = runBlocking {
        coEvery { eventGroupDao.getAll() } answers { events }

        val holderDatabaseSyncer = HolderDatabaseSyncerImpl(
            holderDatabase = holderDatabase,
            workerManagerUtil = mockk(relaxed = true),
            greenCardUtil = fakeGreenCardUtil(),
            getRemoteGreenCardsUseCase = fakeGetRemoteGreenCardUseCase(
                result = RemoteGreenCardsResult.Success(
                    remoteGreenCards = RemoteGreenCards(
                        domesticGreencard = RemoteGreenCards.DomesticGreenCard(
                            origins = listOf(),
                            createCredentialMessages = "".toByteArray()
                        ),
                        euGreencards = null,
                        blobExpireDates = listOf()
                    )
                )
            ),
            syncRemoteGreenCardsUseCase = fakeSyncRemoteGreenCardUseCase(),
            removeExpiredEventsUseCase = fakeRemoveExpiredEventsUseCase(),
            featureFlagUseCase = mockk(relaxed = true),
            yourEventFragmentEndStateUtil = mockk { every { getResult(any(), any(), any(), any()) } returns NotApplicable },
            updateEventExpirationUseCase = mockk(relaxed = true),
            mobileCoreWrapper = mobileCoreWrapper
        )

        val databaseSyncerResult = holderDatabaseSyncer.sync(
            expectedOriginType = null,
            syncWithRemote = true
        )

        assertEquals(DatabaseSyncerResult.Success(true), databaseSyncerResult)
    }

    private fun successResult(originType: OriginType): RemoteGreenCardsResult = RemoteGreenCardsResult.Success(
        remoteGreenCards = RemoteGreenCards(
            domesticGreencard = RemoteGreenCards.DomesticGreenCard(
                origins = listOf(
                    RemoteGreenCards.Origin(
                    type = originType,
                    eventTime = OffsetDateTime.now(),
                    expirationTime = OffsetDateTime.now(),
                    validFrom = OffsetDateTime.now(),
                    doseNumber = 1
                )),
                createCredentialMessages = "".toByteArray()
            ),
            euGreencards = null,
            blobExpireDates = listOf()
        )
    )
}