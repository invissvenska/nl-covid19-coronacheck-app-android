package nl.rijksoverheid.ctr.appconfig.usecases

import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import nl.rijksoverheid.ctr.api.json.DisclosurePolicyJsonAdapter
import nl.rijksoverheid.ctr.appconfig.api.model.HolderConfig
import nl.rijksoverheid.ctr.appconfig.api.model.VerifierConfig
import nl.rijksoverheid.ctr.appconfig.fakeAppConfig
import nl.rijksoverheid.ctr.appconfig.fakeAppConfigPersistenceManager
import nl.rijksoverheid.ctr.appconfig.fakeCachedAppConfigUseCase
import nl.rijksoverheid.ctr.appconfig.models.AppStatus
import nl.rijksoverheid.ctr.appconfig.models.ConfigResult
import nl.rijksoverheid.ctr.appconfig.persistence.RecommendedUpdatePersistenceManager
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
class AppStatusUseCaseImplTest {

    private val moshi = Moshi
        .Builder()
        .add(DisclosurePolicyJsonAdapter())
        .build()

    private val publicKeys =
        "{\"cl_keys\":[]}".toResponseBody("application/json".toMediaType()).source().readUtf8()

    private fun getHolderConfig(
        minimumVersion: Int = 1,
        appDeactivated: Boolean = false,
        recommendedVersion: Int = 1,
    ): String =
        HolderConfig.default(
            holderMinimumVersion = minimumVersion,
            holderAppDeactivated = appDeactivated,
            holderInformationURL = "dummy",
            holderRecommendedVersion = recommendedVersion
        ).toJson(moshi).toResponseBody("application/json".toMediaType()).source()
            .readUtf8()

    private fun getVerifierConfig(
        recommendedVersion: Int = 1,
        recommendedInterval: Int = 1,
        verifierAppDeactivated: Boolean = false,
        verifierMinimumVersion: Int = 1000,
    ): String =
        VerifierConfig.default(
            verifierRecommendedVersion = recommendedVersion,
            upgradeRecommendationIntervalHours = recommendedInterval,
            verifierAppDeactivated = verifierAppDeactivated,
            verifierMinimumVersion = verifierMinimumVersion,
        ).toJson(moshi).toResponseBody("application/json".toMediaType()).source()
            .readUtf8()

    @Test
    fun `status returns Deactivated when app is deactivated remotely`() =
        runBlocking {
            val appStatusUseCase = AppStatusUseCaseImpl(
                clock = Clock.fixed(Instant.ofEpochSecond(0), ZoneId.of("UTC")),
                cachedAppConfigUseCase = fakeCachedAppConfigUseCase(),
                appConfigPersistenceManager = fakeAppConfigPersistenceManager(),
                moshi = moshi,
                isVerifierApp = false,
                recommendedUpdatePersistenceManager = mockk(relaxed = true)
            )

            val appStatus = appStatusUseCase.get(
                config = ConfigResult.Success(
                    appConfig = getHolderConfig(appDeactivated = true),
                    publicKeys = publicKeys
                ),
                currentVersionCode = 1
            )
            Assert.assertEquals(AppStatus.Deactivated("dummy"), appStatus)
        }

    @Test
    fun `status returns UpdateRequired when remote version code is higher than current`() =
        runBlocking {
            val appStatusUseCase = AppStatusUseCaseImpl(
                clock = Clock.fixed(Instant.ofEpochSecond(0), ZoneId.of("UTC")),
                cachedAppConfigUseCase = fakeCachedAppConfigUseCase(),
                appConfigPersistenceManager = fakeAppConfigPersistenceManager(),
                moshi = moshi,
                isVerifierApp = false,
                recommendedUpdatePersistenceManager = mockk(relaxed = true)
            )

            val appStatus = appStatusUseCase.get(
                config = ConfigResult.Success(
                    appConfig = getHolderConfig(minimumVersion = 2),
                    publicKeys = publicKeys
                ),
                currentVersionCode = 1
            )
            Assert.assertEquals(AppStatus.UpdateRequired, appStatus)
        }

    @Test
    fun `status returns NoActionRequired when app is up to date`() =
        runBlocking {
            val appStatusUseCase = AppStatusUseCaseImpl(
                clock = Clock.fixed(Instant.ofEpochSecond(0), ZoneId.of("UTC")),
                cachedAppConfigUseCase = fakeCachedAppConfigUseCase(),
                appConfigPersistenceManager = fakeAppConfigPersistenceManager(),
                moshi = moshi,
                isVerifierApp = false,
                recommendedUpdatePersistenceManager = mockk(relaxed = true)
            )

            val appStatus = appStatusUseCase.get(
                config = ConfigResult.Success(
                    appConfig = getHolderConfig(),
                    publicKeys = publicKeys
                ),
                currentVersionCode = 1
            )
            Assert.assertEquals(AppStatus.NoActionRequired, appStatus)
        }

    @Test
    fun `status returns InternetRequired when config is Error and cached app config is no longer valid`() =
        runBlocking {

            // Current time is 100 seconds
            // Max offline time is set to 50 seconds
            // Last time config fetched was 20 seconds
            val appStatusUseCase = AppStatusUseCaseImpl(
                clock = Clock.fixed(Instant.ofEpochSecond(100), ZoneId.of("UTC")),
                cachedAppConfigUseCase = fakeCachedAppConfigUseCase(
                    appConfig = fakeAppConfig(
                        configTtlSeconds = 50
                    )
                ),
                appConfigPersistenceManager = fakeAppConfigPersistenceManager(
                    lastFetchedSeconds = 20
                ),
                moshi = moshi,
                isVerifierApp = false,
                recommendedUpdatePersistenceManager = mockk(relaxed = true)
            )

            val appStatus = appStatusUseCase.get(
                config = ConfigResult.Error,
                currentVersionCode = 1
            )
            Assert.assertEquals(AppStatus.Error, appStatus)
        }

    @Test
    fun `status returns NoActionRequired when config is Error and cached app config is still valid`() =
        runBlocking {

            // Current time is 100 seconds
            // Max offline time is set to 50 seconds
            // Last time config fetched was 70 seconds
            val appStatusUseCase = AppStatusUseCaseImpl(
                clock = Clock.fixed(Instant.ofEpochSecond(100), ZoneId.of("UTC")),
                cachedAppConfigUseCase = fakeCachedAppConfigUseCase(
                    appConfig = fakeAppConfig(
                        configTtlSeconds = 50
                    )
                ),
                appConfigPersistenceManager = fakeAppConfigPersistenceManager(
                    lastFetchedSeconds = 70
                ),
                moshi = moshi,
                isVerifierApp = false,
                recommendedUpdatePersistenceManager = mockk(relaxed = true)
            )

            val appStatus = appStatusUseCase.get(
                config = ConfigResult.Error,
                currentVersionCode = 1026
            )
            Assert.assertEquals(AppStatus.NoActionRequired, appStatus)
        }

    @Test
    fun `status is recommended update when verifier version is higher and it's shown after upgrade interval`() =
        runBlocking {
            val recommendedUpdatePersistenceManager: RecommendedUpdatePersistenceManager =
                mockk(relaxed = true) {
                    every { getRecommendedUpdateShownSeconds() } returns 0
                }
            val appStatusUseCase = AppStatusUseCaseImpl(
                clock = Clock.fixed(Instant.ofEpochSecond(10000), ZoneId.of("UTC")),
                cachedAppConfigUseCase = fakeCachedAppConfigUseCase(),
                appConfigPersistenceManager = fakeAppConfigPersistenceManager(),
                moshi = moshi,
                isVerifierApp = true,
                recommendedUpdatePersistenceManager = recommendedUpdatePersistenceManager
            )

            val appStatus = appStatusUseCase.get(
                config = ConfigResult.Success(
                    appConfig = getVerifierConfig(recommendedVersion = 2001),
                    publicKeys = publicKeys
                ),
                currentVersionCode = 2000
            )

            verify { recommendedUpdatePersistenceManager.saveRecommendedUpdateShownSeconds(10000) }
            Assert.assertEquals(AppStatus.UpdateRecommended, appStatus)
        }

    @Test
    fun `status is no action required when verifier recommend update was shown in upgrade interval`() {
        runBlocking {
            val recommendedUpdatePersistenceManager: RecommendedUpdatePersistenceManager =
                mockk(relaxed = true) {
                    every { getRecommendedUpdateShownSeconds() } returns 3601
                }
            val appStatusUseCase = AppStatusUseCaseImpl(
                clock = Clock.fixed(Instant.ofEpochSecond(0), ZoneId.of("UTC")),
                cachedAppConfigUseCase = fakeCachedAppConfigUseCase(),
                appConfigPersistenceManager = fakeAppConfigPersistenceManager(),
                moshi = moshi,
                isVerifierApp = true,
                recommendedUpdatePersistenceManager = recommendedUpdatePersistenceManager
            )

            val appStatus = appStatusUseCase.get(
                config = ConfigResult.Success(
                    appConfig = getVerifierConfig(
                        recommendedVersion = 2001,
                        recommendedInterval = 1
                    ),
                    publicKeys = publicKeys
                ),
                currentVersionCode = 2000
            )

            Assert.assertEquals(AppStatus.NoActionRequired, appStatus)
        }
    }

    @Test
    fun `status is update recommended when holder version is higher and not shown before`() =
        runBlocking {
            val recommendedUpdatePersistenceManager: RecommendedUpdatePersistenceManager =
                mockk(relaxed = true) {
                    every { getHolderVersionUpdateShown() } returns 0
                }
            val appStatusUseCase = AppStatusUseCaseImpl(
                clock = Clock.fixed(Instant.ofEpochSecond(10000), ZoneId.of("UTC")),
                cachedAppConfigUseCase = fakeCachedAppConfigUseCase(),
                appConfigPersistenceManager = fakeAppConfigPersistenceManager(),
                moshi = moshi,
                isVerifierApp = false,
                recommendedUpdatePersistenceManager = recommendedUpdatePersistenceManager
            )

            val appStatus = appStatusUseCase.get(
                config = ConfigResult.Success(
                    appConfig = getHolderConfig(recommendedVersion = 2001),
                    publicKeys = publicKeys
                ),
                currentVersionCode = 2000
            )

            verify { recommendedUpdatePersistenceManager.saveHolderVersionShown(2001) }
            Assert.assertEquals(AppStatus.UpdateRecommended, appStatus)
        }

    @Test
    fun `status is no action required recommended version was shown before`() =
        runBlocking {
            val recommendedUpdatePersistenceManager: RecommendedUpdatePersistenceManager =
                mockk(relaxed = true) {
                    every { getHolderVersionUpdateShown() } returns 2001
                }
            val appStatusUseCase = AppStatusUseCaseImpl(
                clock = Clock.fixed(Instant.ofEpochSecond(10000), ZoneId.of("UTC")),
                cachedAppConfigUseCase = fakeCachedAppConfigUseCase(),
                appConfigPersistenceManager = fakeAppConfigPersistenceManager(),
                moshi = moshi,
                isVerifierApp = false,
                recommendedUpdatePersistenceManager = recommendedUpdatePersistenceManager
            )

            val appStatus = appStatusUseCase.get(
                config = ConfigResult.Success(
                    appConfig = getHolderConfig(recommendedVersion = 2001),
                    publicKeys = publicKeys
                ),
                currentVersionCode = 2000
            )

            Assert.assertEquals(AppStatus.NoActionRequired, appStatus)
        }

    private fun appStatusUseCase(appDeactivated: Boolean, minimumVersion: Int) =
        AppStatusUseCaseImpl(
            clock = Clock.fixed(Instant.ofEpochSecond(10000), ZoneId.of("UTC")),
            cachedAppConfigUseCase = mockk<CachedAppConfigUseCase>().apply {
                every { getCachedAppConfig().appDeactivated } returns appDeactivated
                every { getCachedAppConfig().minimumVersion } returns minimumVersion
            },
            appConfigPersistenceManager = mockk(),
            moshi = moshi,
            isVerifierApp = true,
            recommendedUpdatePersistenceManager = mockk()
        )

    @Test
    fun `isAppActive returns false if is app is deactivated from the config`() {
        val appStatusUseCase = appStatusUseCase(true, 1000)

        assertFalse(appStatusUseCase.isAppActive(1000))
    }

    @Test
    fun `isAppActive returns false if app has forced update`() {
        val appStatusUseCase = appStatusUseCase(false, 1001)

        assertFalse(appStatusUseCase.isAppActive(1000))
    }

    @Test
    fun `isAppActive returns true if app is not deactivated and no forced update needed`() {
        val appStatusUseCase = appStatusUseCase(false, 1000)

        assertTrue(appStatusUseCase.isAppActive(1000))
    }

    @Test
    fun `given app is deactivated and needs a forced update, when config refreshes, then shows forced update status`() = runBlocking {
        val currentVersionCode = 1000
        val verifierMinimumVersion = 1001
        val configResult = ConfigResult.Success(
            getVerifierConfig(verifierAppDeactivated = true, verifierMinimumVersion = verifierMinimumVersion),
            publicKeys,
        )

        val appStatus = appStatusUseCase(true, verifierMinimumVersion).get(configResult, currentVersionCode)

        assertTrue(appStatus is AppStatus.UpdateRequired)
    }

    @Test
    fun `given app is deactivated and doesn't need a forced update, when config refreshes, then shows app deactivated status`() = runBlocking {
        val currentVersionCode = 1000
        val verifierMinimumVersion = 1000
        val configResult = ConfigResult.Success(
            getVerifierConfig(verifierAppDeactivated = true, verifierMinimumVersion = verifierMinimumVersion),
            publicKeys,
        )

        val appStatus = appStatusUseCase(true, verifierMinimumVersion).get(configResult, currentVersionCode)

        assertTrue(appStatus is AppStatus.Deactivated)
    }
}
