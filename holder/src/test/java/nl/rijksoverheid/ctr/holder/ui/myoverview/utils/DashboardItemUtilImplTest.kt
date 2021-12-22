package nl.rijksoverheid.ctr.holder.ui.myoverview.utils

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import nl.rijksoverheid.ctr.holder.*
import nl.rijksoverheid.ctr.holder.persistence.CachedAppConfigUseCase
import nl.rijksoverheid.ctr.holder.persistence.PersistenceManager
import nl.rijksoverheid.ctr.holder.persistence.database.DatabaseSyncerResult
import nl.rijksoverheid.ctr.holder.persistence.database.entities.*
import nl.rijksoverheid.ctr.holder.persistence.database.models.GreenCard
import nl.rijksoverheid.ctr.holder.ui.create_qr.models.DashboardItem.CardsItem
import nl.rijksoverheid.ctr.holder.ui.create_qr.models.DashboardItem.CardsItem.CardItem
import nl.rijksoverheid.ctr.holder.ui.create_qr.models.DashboardItem.HeaderItem
import nl.rijksoverheid.ctr.holder.ui.create_qr.models.RemoteEventVaccination
import nl.rijksoverheid.ctr.holder.ui.create_qr.util.DashboardItemUtilImpl
import nl.rijksoverheid.ctr.shared.models.AppErrorResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.lang.IllegalStateException
import java.time.OffsetDateTime
import kotlin.test.assertTrue

class DashboardItemUtilImplTest {

    @Test
    fun `getHeaderItemText returns correct text if domestic and has green cards`() {
        val util = DashboardItemUtilImpl(
            clockDeviationUseCase = fakeClockDevationUseCase(),
            greenCardUtil = fakeGreenCardUtil(
                isExpired = false
            ),
            persistenceManager = fakePersistenceManager(),
            eventGroupEntityUtil = fakeEventGroupEntityUtil(),
            appConfigFreshnessUseCase = fakeAppConfigFreshnessUseCase(),
            appConfigUseCase = mockk(),
            versionCode = 1
        )

        val headerText = util.getHeaderItemText(
            greenCardType = GreenCardType.Domestic,
            allGreenCards = listOf(fakeGreenCard)
        )

        assertEquals(R.string.my_overview_description, headerText)
    }

    @Test
    fun `getHeaderItemText returns correct text if domestic and has no green cards`() {
        val util = DashboardItemUtilImpl(
            clockDeviationUseCase = fakeClockDevationUseCase(),
            greenCardUtil = fakeGreenCardUtil(
                isExpired = false
            ),
            persistenceManager = fakePersistenceManager(),
            eventGroupEntityUtil = fakeEventGroupEntityUtil(),
            appConfigFreshnessUseCase = fakeAppConfigFreshnessUseCase(),
            appConfigUseCase = mockk(),
            versionCode = 1
        )

        val headerText = util.getHeaderItemText(
            greenCardType = GreenCardType.Domestic,
            allGreenCards = listOf()
        )

        assertEquals(R.string.my_overview_qr_placeholder_description, headerText)
    }

    @Test
    fun `getHeaderItemText returns correct text if eu and has green cards`() {
        val util = DashboardItemUtilImpl(
            clockDeviationUseCase = fakeClockDevationUseCase(),
            greenCardUtil = fakeGreenCardUtil(
                isExpired = false
            ),
            persistenceManager = fakePersistenceManager(),
            eventGroupEntityUtil = fakeEventGroupEntityUtil(),
            appConfigFreshnessUseCase = fakeAppConfigFreshnessUseCase(),
            appConfigUseCase = mockk(),
            versionCode = 1
        )

        val headerText = util.getHeaderItemText(
            greenCardType = GreenCardType.Eu,
            allGreenCards = listOf(fakeGreenCard)
        )

        assertEquals(R.string.my_overview_description_eu, headerText)
    }

    @Test
    fun `getHeaderItemText returns correct text if eu and has no green cards`() {
        val util = DashboardItemUtilImpl(
            clockDeviationUseCase = fakeClockDevationUseCase(),
            greenCardUtil = fakeGreenCardUtil(
                isExpired = false
            ),
            persistenceManager = fakePersistenceManager(),
            eventGroupEntityUtil = fakeEventGroupEntityUtil(),
            appConfigFreshnessUseCase = fakeAppConfigFreshnessUseCase(),
            appConfigUseCase = mockk(),
            versionCode = 1
        )

        val headerText = util.getHeaderItemText(
            greenCardType = GreenCardType.Eu,
            allGreenCards = listOf()
        )

        assertEquals(R.string.my_overview_qr_placeholder_description_eu, headerText)
    }

    @Test
    fun `shouldShowClockDeviationItem returns true if has deviation and has green cards`() {
        val util = DashboardItemUtilImpl(
            clockDeviationUseCase = fakeClockDevationUseCase(
                hasDeviation = true
            ),
            greenCardUtil = fakeGreenCardUtil(),
            persistenceManager = fakePersistenceManager(),
            eventGroupEntityUtil = fakeEventGroupEntityUtil(),
            appConfigFreshnessUseCase = fakeAppConfigFreshnessUseCase(),
            appConfigUseCase = mockk(),
            versionCode = 1
        )

        val shouldShowClockDeviationItem = util.shouldShowClockDeviationItem(
            allGreenCards = listOf(fakeGreenCard)
        )

        assertEquals(true, shouldShowClockDeviationItem)
    }

    @Test
    fun `shouldShowClockDeviationItem returns true if has deviation and not all green cards expired`() {
        val util = DashboardItemUtilImpl(
            clockDeviationUseCase = fakeClockDevationUseCase(
                hasDeviation = true
            ),
            greenCardUtil = fakeGreenCardUtil(
                isExpired = false
            ),
            persistenceManager = fakePersistenceManager(),
            eventGroupEntityUtil = fakeEventGroupEntityUtil(),
            appConfigFreshnessUseCase = fakeAppConfigFreshnessUseCase(),
            appConfigUseCase = mockk(),
            versionCode = 1
        )

        val shouldShowClockDeviationItem = util.shouldShowClockDeviationItem(
            allGreenCards = listOf(fakeGreenCard)
        )

        assertEquals(true, shouldShowClockDeviationItem)
    }

    @Test
    fun `shouldShowPlaceholderItem returns true if has no green cards`() {
        val util = DashboardItemUtilImpl(
            clockDeviationUseCase = fakeClockDevationUseCase(),
            greenCardUtil = fakeGreenCardUtil(),
            persistenceManager = fakePersistenceManager(),
            eventGroupEntityUtil = fakeEventGroupEntityUtil(),
            appConfigFreshnessUseCase = fakeAppConfigFreshnessUseCase(),
            appConfigUseCase = mockk(),
            versionCode = 1
        )

        val shouldShowHeaderItem = util.shouldShowPlaceholderItem(
            allGreenCards = listOf()
        )

        assertEquals(true, shouldShowHeaderItem)
    }

    @Test
    fun `shouldShowPlaceholderItem returns true if all green cards expired`() {
        val util = DashboardItemUtilImpl(
            clockDeviationUseCase = fakeClockDevationUseCase(),
            greenCardUtil = fakeGreenCardUtil(
                isExpired = true
            ),
            persistenceManager = fakePersistenceManager(),
            eventGroupEntityUtil = fakeEventGroupEntityUtil(),
            appConfigFreshnessUseCase = fakeAppConfigFreshnessUseCase(),
            appConfigUseCase = mockk(),
            versionCode = 1
        )

        val shouldShowHeaderItem = util.shouldShowPlaceholderItem(
            allGreenCards = listOf(fakeGreenCard)
        )

        assertEquals(true, shouldShowHeaderItem)
    }

    @Test
    fun `shouldAddQrButtonItem returns true if has no green cards`() {
        val util = DashboardItemUtilImpl(
            clockDeviationUseCase = fakeClockDevationUseCase(),
            greenCardUtil = fakeGreenCardUtil(),
            persistenceManager = fakePersistenceManager(),
            eventGroupEntityUtil = fakeEventGroupEntityUtil(),
            appConfigFreshnessUseCase = fakeAppConfigFreshnessUseCase(),
            appConfigUseCase = mockk(),
            versionCode = 1
        )

        val shouldAddQrButtonItem = util.shouldAddQrButtonItem(
            allGreenCards = listOf()
        )

        assertEquals(true, shouldAddQrButtonItem)
    }

    @Test
    fun `multiple vaccination card items should be combined into 1`() {
        val util = DashboardItemUtilImpl(mockk(), mockk(), mockk(), mockk(),mockk(), mockk(), 1)

        val card1 = createCardItem(OriginType.Vaccination)
        val card2 = createCardItem(OriginType.Vaccination)
        val card3 = createCardItem(OriginType.Vaccination)

        val items = listOf(
            HeaderItem(1),
            CardsItem(listOf(createCardItem(OriginType.Test))),
            CardsItem(listOf(card1)),
            CardsItem(listOf(createCardItem(OriginType.Recovery))),
            CardsItem(listOf(card2)),
            CardsItem(listOf(card3))
        )

        val combinedItems = util.combineEuVaccinationItems(items)

        // Total size of list smaller because of combined vaccination items
        assertTrue(combinedItems.size == 4)
        assertEquals((combinedItems[2] as CardsItem).cards[0], card1)
        assertEquals((combinedItems[2] as CardsItem).cards[1], card2)
        assertEquals((combinedItems[2] as CardsItem).cards[2], card3)
    }

    @Test
    fun `shouldAddSyncGreenCardsItem returns false if no vaccination events`() = runBlocking {
        val util = DashboardItemUtilImpl(
            clockDeviationUseCase = fakeClockDevationUseCase(),
            greenCardUtil = fakeGreenCardUtil(),
            persistenceManager = fakePersistenceManager(),
            eventGroupEntityUtil = fakeEventGroupEntityUtil(
                remoteEventVaccinations = listOf()
            ),
            appConfigFreshnessUseCase = fakeAppConfigFreshnessUseCase(),
            appConfigUseCase = mockk(),
            versionCode = 1
        )

        val shouldAddSyncGreenCardsItem = util.shouldAddSyncGreenCardsItem(
            allGreenCards = listOf(),
            allEventGroupEntities = listOf()
        )

        assertEquals(false, shouldAddSyncGreenCardsItem)
    }

    @Test
    fun `shouldAddSyncGreenCardsItem returns false if there is a single vaccination event`() = runBlocking {
        val util = DashboardItemUtilImpl(
            clockDeviationUseCase = fakeClockDevationUseCase(),
            greenCardUtil = fakeGreenCardUtil(),
            persistenceManager = fakePersistenceManager(),
            eventGroupEntityUtil = fakeEventGroupEntityUtil(
                remoteEventVaccinations = listOf(
                    RemoteEventVaccination(
                        type = "",
                        unique = "",
                        vaccination = fakeRemoteEventVaccination()
                    )
                )
            ),
            appConfigFreshnessUseCase = fakeAppConfigFreshnessUseCase(),
            appConfigUseCase = mockk(),
            versionCode = 1
        )

        val shouldAddSyncGreenCardsItem = util.shouldAddSyncGreenCardsItem(
            allGreenCards = listOf(),
            allEventGroupEntities = listOf()
        )

        assertEquals(false, shouldAddSyncGreenCardsItem)
    }

    @Test
    fun `shouldAddSyncGreenCardsItem returns true if there are multiple vaccination events and one european green card`() = runBlocking {
        val util = DashboardItemUtilImpl(
            clockDeviationUseCase = fakeClockDevationUseCase(),
            greenCardUtil = fakeGreenCardUtil(),
            persistenceManager = fakePersistenceManager(),
            eventGroupEntityUtil = fakeEventGroupEntityUtil(
                remoteEventVaccinations = listOf(
                    RemoteEventVaccination(
                        type = "",
                        unique = "",
                        vaccination = fakeRemoteEventVaccination()
                    ),
                    RemoteEventVaccination(
                        type = "",
                        unique = "",
                        vaccination = fakeRemoteEventVaccination()
                    )
                )
            ),
            appConfigFreshnessUseCase = fakeAppConfigFreshnessUseCase(),
            appConfigUseCase = mockk(),
            versionCode = 1
        )

        val shouldAddSyncGreenCardsItem = util.shouldAddSyncGreenCardsItem(
            allGreenCards = listOf(fakeEuropeanVaccinationGreenCard),
            allEventGroupEntities = listOf()
        )

        assertEquals(true, shouldAddSyncGreenCardsItem)
    }

    @Test
    fun `shouldAddSyncGreenCardsItem returns false if there are multiple vaccination events and two european green card`() = runBlocking {
        val util = DashboardItemUtilImpl(
            clockDeviationUseCase = fakeClockDevationUseCase(),
            greenCardUtil = fakeGreenCardUtil(),
            persistenceManager = fakePersistenceManager(),
            eventGroupEntityUtil = fakeEventGroupEntityUtil(
                remoteEventVaccinations = listOf(
                    RemoteEventVaccination(
                        type = "",
                        unique = "",
                        vaccination = fakeRemoteEventVaccination()
                    ),
                    RemoteEventVaccination(
                        type = "",
                        unique = "",
                        vaccination = fakeRemoteEventVaccination()
                    )
                )
            ),
            appConfigFreshnessUseCase = fakeAppConfigFreshnessUseCase(),
            appConfigUseCase = mockk(),
            versionCode = 1
        )

        val shouldAddSyncGreenCardsItem = util.shouldAddSyncGreenCardsItem(
            allGreenCards = listOf(fakeEuropeanVaccinationGreenCard, fakeEuropeanVaccinationGreenCard),
            allEventGroupEntities = listOf()
        )

        assertEquals(false, shouldAddSyncGreenCardsItem)
    }

    @Test
    fun `shouldAddGreenCardsSyncedItem returns false if multiple eu vaccinations and local flag set to true`() {
        val util = DashboardItemUtilImpl(
            clockDeviationUseCase = fakeClockDevationUseCase(),
            greenCardUtil = fakeGreenCardUtil(
                isExpired = true
            ),
            persistenceManager = fakePersistenceManager(
                hasDismissedUnsecureDeviceDialog = true
            ),
            eventGroupEntityUtil = fakeEventGroupEntityUtil(),
            appConfigFreshnessUseCase = fakeAppConfigFreshnessUseCase(),
            appConfigUseCase = mockk(),
            versionCode = 1
        )

        val shouldAddGreenCardsSyncedItem = util.shouldAddGreenCardsSyncedItem(
            allGreenCards = listOf(fakeEuropeanVaccinationGreenCard, fakeEuropeanVaccinationGreenCard)
        )

        assertEquals(false, shouldAddGreenCardsSyncedItem)
    }

    @Test
    fun `shouldAddGreenCardsSyncedItem returns true if multiple eu vaccinations and local flag set to false`() {
        val util = DashboardItemUtilImpl(
            clockDeviationUseCase = fakeClockDevationUseCase(),
            greenCardUtil = fakeGreenCardUtil(
                isExpired = true
            ),
            persistenceManager = fakePersistenceManager(
                hasDismissedUnsecureDeviceDialog = false
            ),
            eventGroupEntityUtil = fakeEventGroupEntityUtil(),
            appConfigFreshnessUseCase = fakeAppConfigFreshnessUseCase(),
            appConfigUseCase = mockk(),
            versionCode = 1
        )

        val shouldAddGreenCardsSyncedItem = util.shouldAddGreenCardsSyncedItem(
            allGreenCards = listOf(fakeEuropeanVaccinationGreenCard, fakeEuropeanVaccinationGreenCard)
        )

        assertEquals(true, shouldAddGreenCardsSyncedItem)
    }

    @Test
    fun `shouldShowMissingDutchVaccinationItem returns true if no nl vaccination card and there is a eu vaccination card`() {
        val util = dashboardItemUtil()

        val shouldShowMissingDutchVaccinationItem = util.shouldShowMissingDutchVaccinationItem(
            domesticGreenCards = listOf(fakeDomesticTestGreenCard),
            euGreenCards = listOf(fakeEuropeanVaccinationGreenCard),
        )

        assertTrue(shouldShowMissingDutchVaccinationItem)
    }

    @Test
    fun `shouldShowMissingDutchVaccinationItem returns false if there is a nl vaccination card`() {
        val util = dashboardItemUtil()

        val shouldShowMissingDutchVaccinationItem = util.shouldShowMissingDutchVaccinationItem(
            domesticGreenCards = listOf(fakeDomesticVaccinationGreenCard),
            euGreenCards = listOf(fakeEuropeanVaccinationGreenCard),
        )

        assertFalse(shouldShowMissingDutchVaccinationItem)
    }

    @Test
    fun `shouldShowMissingDutchVaccinationItem returns false if there is no eu vaccination card`() {
        val util = dashboardItemUtil()

        val shouldShowMissingDutchVaccinationItem = util.shouldShowMissingDutchVaccinationItem(
            domesticGreenCards = listOf(fakeDomesticTestGreenCard),
            euGreenCards = listOf(fakeEuropeanVaccinationTestCard),
        )

        assertFalse(shouldShowMissingDutchVaccinationItem)
    }

    @Test
    fun `shouldShowCoronaMelderItem returns false if there are no green cards`() {
        val util = dashboardItemUtil()

        val shouldShowCoronaMelderItem = util.shouldShowCoronaMelderItem(
            greenCards = listOf(),
            databaseSyncerResult = DatabaseSyncerResult.Success()
        )

        assertFalse(shouldShowCoronaMelderItem)
    }

    @Test
    fun `shouldShowCoronaMelderItem returns false if all green cards expired`() {
        val util = dashboardItemUtil(
            isExpired = true
        )

        val shouldShowCoronaMelderItem = util.shouldShowCoronaMelderItem(
            greenCards = listOf(fakeDomesticVaccinationGreenCard),
            databaseSyncerResult = DatabaseSyncerResult.Success()
        )

        assertFalse(shouldShowCoronaMelderItem)
    }

    @Test
    fun `shouldShowCoronaMelderItem returns false if green cards but with error DatabaseSyncerResult`() {
        val util = dashboardItemUtil(
            isExpired = false
        )

        val shouldShowCoronaMelderItem = util.shouldShowCoronaMelderItem(
            greenCards = listOf(fakeDomesticVaccinationGreenCard),
            databaseSyncerResult = DatabaseSyncerResult.Failed.Error(AppErrorResult(HolderStep.GetCredentialsNetworkRequest, IllegalStateException()))
        )

        assertFalse(shouldShowCoronaMelderItem)
    }

    @Test
    fun `shouldShowCoronaMelderItem returns true if green cards`() {
        val util = dashboardItemUtil(
            isExpired = false
        )

        val shouldShowCoronaMelderItem = util.shouldShowCoronaMelderItem(
            greenCards = listOf(fakeDomesticVaccinationGreenCard),
            databaseSyncerResult = DatabaseSyncerResult.Success()
        )

        assertTrue(shouldShowCoronaMelderItem)
    }

    @Test
    fun `App update is available when the recommended version is higher than current version`() {
        val util = DashboardItemUtilImpl(
            mockk(), mockk(), mockk(), mockk(), mockk(),
            appConfigUseCase = mockk { every { getCachedAppConfig().recommendedVersion } returns 2 },
            versionCode = 1
        )

        assertTrue(util.isAppUpdateAvailable())
    }

    @Test
    fun `App update is not available when the recommended version is current version`() {
        val util = DashboardItemUtilImpl(
            mockk(), mockk(), mockk(), mockk(), mockk(),
            appConfigUseCase = mockk { every { getCachedAppConfig().recommendedVersion } returns 1 },
            versionCode = 1
        )

        assertFalse(util.isAppUpdateAvailable())
    }

    @Test
    fun `App update is not available when the recommended version lower is current version`() {
        val util = DashboardItemUtilImpl(
            mockk(), mockk(), mockk(), mockk(), mockk(),
            appConfigUseCase = mockk { every { getCachedAppConfig().recommendedVersion } returns 1 },
            versionCode = 2
        )

        assertFalse(util.isAppUpdateAvailable())
    }

    @Test
    fun `shouldShowNewValidityItem returns true if banner needs to be shown`() {
        val cachedAppConfigUseCase = mockk<CachedAppConfigUseCase>()
        val persistenceManager = mockk<PersistenceManager>()
        every { cachedAppConfigUseCase.getCachedAppConfig().showNewValidityInfoCard } answers { true }
        every { persistenceManager.getHasDismissedNewValidityInfoCard() } answers { false }

        val util = DashboardItemUtilImpl(
            mockk(), mockk(), persistenceManager = persistenceManager, mockk(), mockk(),
            appConfigUseCase = cachedAppConfigUseCase,
            versionCode = 2
        )

        assertTrue(util.shouldShowNewValidityItem())
    }

    @Test
    fun `shouldShowNewValidityItem returns false if feature not live yet`() {
        val cachedAppConfigUseCase = mockk<CachedAppConfigUseCase>()
        val persistenceManager = mockk<PersistenceManager>()
        every { cachedAppConfigUseCase.getCachedAppConfig().showNewValidityInfoCard } answers { false }
        every { persistenceManager.getHasDismissedNewValidityInfoCard() } answers { false }

        val util = DashboardItemUtilImpl(
            mockk(), mockk(), persistenceManager = persistenceManager, mockk(), mockk(),
            appConfigUseCase = cachedAppConfigUseCase,
            versionCode = 2
        )

        assertFalse(util.shouldShowNewValidityItem())
    }

    @Test
    fun `shouldShowNewValidityItem returns false if banner does not need to show`() {
        val cachedAppConfigUseCase = mockk<CachedAppConfigUseCase>()
        val persistenceManager = mockk<PersistenceManager>()
        every { cachedAppConfigUseCase.getCachedAppConfig().showNewValidityInfoCard } answers { true }
        every { persistenceManager.getHasDismissedNewValidityInfoCard() } answers { true }

        val util = DashboardItemUtilImpl(
            mockk(), mockk(), persistenceManager = persistenceManager, mockk(), mockk(),
            appConfigUseCase = cachedAppConfigUseCase,
            versionCode = 2
        )

        assertFalse(util.shouldShowNewValidityItem())
    }

    private fun createCardItem(originType: OriginType) = CardItem(
        greenCard = GreenCard(
            greenCardEntity = fakeGreenCardEntity,
            origins = listOf(
                OriginEntity(
                    greenCardId = 1L,
                    type = originType,
                    eventTime = OffsetDateTime.now(),
                    expirationTime = OffsetDateTime.now(),
                    validFrom = OffsetDateTime.now()
                )
            ),
            credentialEntities = emptyList()
        ),
        originStates = listOf(),
        credentialState = CardsItem.CredentialState.HasCredential(mockk()),
        databaseSyncerResult = mockk()
    )

    private fun dashboardItemUtil(isExpired: Boolean = false) = DashboardItemUtilImpl(
        clockDeviationUseCase = fakeClockDevationUseCase(),
        greenCardUtil = fakeGreenCardUtil(
            isExpired = isExpired
        ),
        persistenceManager = fakePersistenceManager(
            hasDismissedUnsecureDeviceDialog = false
        ),
        eventGroupEntityUtil = fakeEventGroupEntityUtil(),
        appConfigFreshnessUseCase = fakeAppConfigFreshnessUseCase(),
        appConfigUseCase = mockk(),
        versionCode = 1
    )
}