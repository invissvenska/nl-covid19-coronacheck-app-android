package nl.rijksoverheid.ctr.introduction.status

import android.os.Bundle
import androidx.fragment.app.Fragment
import nl.rijksoverheid.ctr.introduction.status.models.IntroductionStatus
import nl.rijksoverheid.ctr.shared.ext.findNavControllerSafety

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
class IntroductionStatusFragment : Fragment() {

    companion object {
        private const val EXTRA_INTRODUCTION_STATUS = "EXTRA_INTRODUCTION_STATUS"

        fun getBundle(
            introductionStatus: IntroductionStatus
        ): Bundle {
            val bundle = Bundle()
            bundle.putParcelable(EXTRA_INTRODUCTION_STATUS, introductionStatus)
            return bundle
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val introductionStatus = arguments?.getParcelable<IntroductionStatus>(
            EXTRA_INTRODUCTION_STATUS
        )

        when (introductionStatus) {
            is IntroductionStatus.SetupNotFinished -> {
                findNavControllerSafety()?.navigate(
                    IntroductionStatusFragmentDirections.actionSetup()
                )
            }
            is IntroductionStatus.OnboardingNotFinished -> {
                findNavControllerSafety()?.navigate(
                    IntroductionStatusFragmentDirections.actionOnboarding(
                        introductionStatus.introductionData
                    )
                )
            }
            IntroductionStatus.IntroductionFinished -> { }
        }
    }
}
