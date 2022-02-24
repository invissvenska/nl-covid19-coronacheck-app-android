package nl.rijksoverheid.ctr.verifier.usecase

import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import nl.rijksoverheid.ctr.appconfig.usecases.FeatureFlagUseCase
import nl.rijksoverheid.ctr.introduction.IntroductionData
import nl.rijksoverheid.ctr.introduction.persistance.IntroductionPersistenceManager
import nl.rijksoverheid.ctr.introduction.ui.status.models.IntroductionStatus
import org.junit.Test

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC    LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
class VerifierIntroductionStatusUseCaseImplTest {

    private val introductionPersistenceManager: IntroductionPersistenceManager = mockk()
    private val introductionData: IntroductionData = mockk()
    private val featureFlagUseCase: FeatureFlagUseCase = mockk()

    private val introductionStatusUseCase = VerifierIntroductionStatusUseCaseImpl(
        introductionPersistenceManager, introductionData, featureFlagUseCase
    )

    @Test
    fun `when setup isn't finished, the status is setup not finished`() {
        every { introductionPersistenceManager.getSetupFinished() } returns false

        assertEquals(
            introductionStatusUseCase.get(),
            IntroductionStatus.SetupNotFinished
        )
    }

    @Test
    fun `when introduction isn't finished, the status is introduction not finished`() {
        every { introductionPersistenceManager.getSetupFinished() } returns true
        every { introductionPersistenceManager.getIntroductionFinished() } returns false

        assertEquals(
            introductionStatusUseCase.get(),
            IntroductionStatus.OnboardingNotFinished(introductionData)
        )
    }

    @Test
    fun `when intro is finished and new features are available, the status is new features`() {
        every { introductionPersistenceManager.getSetupFinished() } returns true
        every { introductionPersistenceManager.getIntroductionFinished() } returns true
        every { introductionData.newFeatures } returns listOf(mockk())
        every { introductionData.newFeatureVersion } returns 2
        every { introductionPersistenceManager.getNewFeaturesSeen(2) } returns false
        every { featureFlagUseCase.isVerificationPolicySelectionEnabled() } answers { true }

        assertEquals(
            introductionStatusUseCase.get(),
            IntroductionStatus.OnboardingFinished.NewFeatures(introductionData)
        )
    }

    @Test
    fun `when intro is finished and new terms are available, the status is consent needed`() {
        every { introductionPersistenceManager.getSetupFinished() } returns true
        every { introductionPersistenceManager.getIntroductionFinished() } returns true
        every { introductionData.newFeatures } returns emptyList()
        every { introductionData.newFeatureVersion } returns 2
        every { introductionPersistenceManager.getNewFeaturesSeen(2) } returns true
        every { introductionData.newTerms } returns mockk { every { version } returns 1 }
        every { introductionPersistenceManager.getNewTermsSeen(1) } returns false

        assertEquals(
            introductionStatusUseCase.get(),
            IntroductionStatus.OnboardingFinished.ConsentNeeded(introductionData)
        )
    }

    @Test
    fun `when intro is finished and there are no new features or terms, the status is no action required`() {
        every { introductionPersistenceManager.getSetupFinished() } returns true
        every { introductionPersistenceManager.getIntroductionFinished() } returns true
        every { introductionData.newFeatures } returns listOf(mockk())
        every { introductionData.newFeatureVersion } returns 2
        every { introductionPersistenceManager.getNewFeaturesSeen(2) } returns true
        every { introductionData.newTerms } returns mockk { every { version } returns 1 }
        every { introductionPersistenceManager.getNewTermsSeen(1) } returns true

        assertEquals(
            introductionStatusUseCase.get(),
            IntroductionStatus.IntroductionFinished
        )
    }
}