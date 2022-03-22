/*
 *  Copyright (c) 2022 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC    LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.ctr.holder.usecases

import androidx.annotation.StringRes
import nl.rijksoverheid.ctr.holder.R
import nl.rijksoverheid.ctr.persistence.PersistenceManager
import nl.rijksoverheid.ctr.introduction.IntroductionData
import nl.rijksoverheid.ctr.introduction.persistance.IntroductionPersistenceManager
import nl.rijksoverheid.ctr.introduction.onboarding.models.OnboardingItem
import nl.rijksoverheid.ctr.introduction.status.models.IntroductionStatus
import nl.rijksoverheid.ctr.introduction.status.models.IntroductionStatus.OnboardingNotFinished
import nl.rijksoverheid.ctr.introduction.status.usecases.IntroductionStatusUseCase
import nl.rijksoverheid.ctr.shared.models.DisclosurePolicy

class HolderIntroductionStatusUseCaseImpl(
    private val introductionPersistenceManager: IntroductionPersistenceManager,
    private val introductionData: IntroductionData,
    private val persistenceManager: PersistenceManager,
    private val holderFeatureFlagUseCase: HolderFeatureFlagUseCase
) : IntroductionStatusUseCase {

    override fun get(): IntroductionStatus {
        return when {
            setupIsNotFinished() -> IntroductionStatus.SetupNotFinished
            onboardingIsNotFinished() -> getOnboardingNotFinished()
            else -> IntroductionStatus.IntroductionFinished
        }
    }

    private fun setupIsNotFinished() = !introductionPersistenceManager.getSetupFinished()

    /**
     * Add the current disclosure policy info as onboarding item
     *
     * @return Onboarding not finished state with disclosure policy onboarding item added
     */
    private fun getOnboardingNotFinished(): OnboardingNotFinished {
        val policy = holderFeatureFlagUseCase.getDisclosurePolicy()
        return OnboardingNotFinished(
            introductionData.copy(
                onboardingItems = getOnboardingItems(policy)
            ).apply {
                setSavePolicyChange { persistenceManager.setPolicyScreenSeen(policy) }
            }
        )
    }

    @StringRes
    private fun getPolicyOnboardingTitle(newPolicy: DisclosurePolicy): Int {
        return when (newPolicy) {
            DisclosurePolicy.ZeroG -> R.string.holder_onboarding_content_TravelSafe_0G_title
            DisclosurePolicy.OneG -> R.string.holder_onboarding_disclosurePolicyChanged_only1GAccess_title
            DisclosurePolicy.ThreeG -> R.string.holder_onboarding_disclosurePolicyChanged_only3GAccess_title
            DisclosurePolicy.OneAndThreeG -> R.string.holder_onboarding_disclosurePolicyChanged_3Gand1GAccess_title
        }
    }

    @StringRes
    private fun getPolicyOnboardingBody(newPolicy: DisclosurePolicy): Int {
        return when (newPolicy) {
            DisclosurePolicy.ZeroG -> R.string.holder_onboarding_content_TravelSafe_0G_message
            DisclosurePolicy.OneG -> R.string.holder_onboarding_disclosurePolicyChanged_only1GAccess_message
            DisclosurePolicy.ThreeG -> R.string.holder_onboarding_disclosurePolicyChanged_only3GAccess_message
            DisclosurePolicy.OneAndThreeG -> R.string.holder_onboarding_disclosurePolicyChanged_3Gand1GAccess_message
        }
    }

    private fun onboardingIsNotFinished() =
        !introductionPersistenceManager.getIntroductionFinished()

    private fun getOnboardingItems(policy: DisclosurePolicy): List<OnboardingItem> {
        return listOfNotNull(
            OnboardingItem(
                if (policy == DisclosurePolicy.ZeroG) {
                    R.drawable.illustration_onboarding_1_0g
                } else {
                    R.drawable.illustration_onboarding_1
                },
                if (policy == DisclosurePolicy.ZeroG) {
                    R.string.holder_onboarding_content_TravelSafe_0G_title
                } else {
                    R.string.onboarding_screen_1_title
                },
                if (policy == DisclosurePolicy.ZeroG) {
                    R.string.holder_onboarding_content_TravelSafe_0G_message
                } else {
                    R.string.onboarding_screen_1_description
                }
            ),
            OnboardingItem(
                R.drawable.illustration_onboarding_2,
                R.string.onboarding_screen_2_title,
                R.string.onboarding_screen_2_description,
            ),
            OnboardingItem(
                R.drawable.illustration_onboarding_3,
                if (policy == DisclosurePolicy.ZeroG) {
                    R.string.holder_onboarding_content_onlyInternationalQR_0G_title
                } else {
                    R.string.onboarding_screen_4_title
                },
                if (policy == DisclosurePolicy.ZeroG) {
                    R.string.holder_onboarding_content_onlyInternationalQR_0G_message
                } else {
                    R.string.onboarding_screen_4_description
                }
            ),
            if (policy != DisclosurePolicy.ZeroG) {
                OnboardingItem(
                    R.drawable.illustration_onboarding_4,
                    R.string.onboarding_screen_3_title,
                    R.string.onboarding_screen_3_description
                )
            } else null,
            if (policy != DisclosurePolicy.ZeroG) {
                OnboardingItem(
                    imageResource = R.drawable.illustration_onboarding_disclosure_policy,
                    titleResource = getPolicyOnboardingTitle(policy),
                    description = getPolicyOnboardingBody(policy)
                )
            } else null
        )
    }
}
