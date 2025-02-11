/*
 * Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 * Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 * SPDX-License-Identifier: EUPL-1.2
 */

package nl.rijksoverheid.ctr

import android.content.Context
import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.navigation.findNavController
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import nl.rijksoverheid.ctr.appconfig.models.AppStatus
import nl.rijksoverheid.ctr.appconfig.models.AppUpdateData
import nl.rijksoverheid.ctr.appconfig.models.NewTerms
import nl.rijksoverheid.ctr.holder.*
import nl.rijksoverheid.ctr.persistence.database.HolderDatabase
import nl.rijksoverheid.ctr.introduction.IntroductionViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.robolectric.RobolectricTestRunner

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
@RunWith(RobolectricTestRunner::class)
class HolderMainActivityTest : AutoCloseKoinTest() {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private lateinit var scenario: ActivityScenario<HolderMainActivity>

    @After
    fun tearDown() {
        scenario.close()
    }

    @Test
    fun `If onboarding not finished navigate to introduction`() {
        launchHolderMainActivity(
            fakeIntroductionViewModel(
                introductionRequired = true
            )
        )

        scenario.onActivity {
            assertEquals(
                it.findNavController(R.id.main_nav_host_fragment).currentDestination?.id,
                R.id.nav_setup
            )
        }
    }

    @Test
    fun `If consent needed navigate to app status`() {
        val scenario = launchHolderMainActivity(
            fakeIntroductionViewModel(),
            appStatus = AppStatus.ConsentNeeded(AppUpdateData(listOf(), NewTerms(1,true)))
        )
        scenario.onActivity {
            assertEquals(
                it.findNavController(R.id.main_nav_host_fragment).currentDestination?.id,
                R.id.nav_new_terms
            )
        }
    }

    @Test
    fun `If new features navigate to app status`() {
        val scenario = launchHolderMainActivity(
            fakeIntroductionViewModel(),
            appStatus = AppStatus.NewFeatures(AppUpdateData(listOf(), NewTerms(1,true)))
        )
        scenario.onActivity {
            assertEquals(
                it.findNavController(R.id.main_nav_host_fragment).currentDestination?.id,
                R.id.nav_new_features
            )
        }
    }

    @Test
    fun `If introduction finished navigate to main`() {
        val scenario = launchHolderMainActivity(
            fakeIntroductionViewModel(
                introductionRequired = false
            )
        )
        scenario.onActivity {
            assertEquals(
                it.findNavController(R.id.main_nav_host_fragment).currentDestination?.id,
                R.id.nav_main
            )
        }
    }

    @Test
    fun `If app status is not NoActionRequired navigate to app status`() {
        val scenario = launchHolderMainActivity(
            fakeIntroductionViewModel(),
            appStatus = AppStatus.Error
        )
        scenario.onActivity {
            assertEquals(
                it.findNavController(R.id.main_nav_host_fragment).currentDestination?.id,
                R.id.nav_app_locked
            )
        }
    }

    private fun launchHolderMainActivity(
        introductionViewModel: IntroductionViewModel,
        appStatus: AppStatus = AppStatus.NoActionRequired
    ): ActivityScenario<HolderMainActivity> {

        loadKoinModules(
            module(override = true) {
                viewModel {
                    introductionViewModel
                }
                viewModel {
                    fakeAppConfigViewModel(
                        appStatus = appStatus
                    )
                }
                viewModel {
                    fakeDashboardViewModel()
                }
                viewModel {
                    fakeSetupViewModel(updateConfig = false)
                }
                factory {
                    fakeCachedAppConfigUseCase()
                }
                single {
                    val context = ApplicationProvider.getApplicationContext<Context>()
                    Room.inMemoryDatabaseBuilder(context, HolderDatabase::class.java).build()
                }
            })

        scenario = ActivityScenario.launch(
            Intent(
                ApplicationProvider.getApplicationContext(),
                HolderMainActivity::class.java
            )
        )
        return scenario
    }
}
