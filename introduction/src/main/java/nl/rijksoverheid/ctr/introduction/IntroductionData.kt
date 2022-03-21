package nl.rijksoverheid.ctr.introduction

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import nl.rijksoverheid.ctr.appconfig.models.NewFeatureItem
import nl.rijksoverheid.ctr.appconfig.models.NewTerms
import nl.rijksoverheid.ctr.introduction.ui.onboarding.models.OnboardingItem
import nl.rijksoverheid.ctr.introduction.ui.privacy_consent.models.PrivacyPolicyItem
import java.io.Serializable

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
@Parcelize
data class IntroductionData(
    val onboardingItems: List<OnboardingItem> = listOf(),
    val privacyPolicyItems: List<PrivacyPolicyItem> = listOf(),
    val newFeatures: List<NewFeatureItem> = listOf(),
    val newTerms: NewTerms,
    val newFeatureVersion: Int? = null,
    val hideConsent: Boolean = false
) : Parcelable {

    @IgnoredOnParcel
    var savePolicyChangeSerialized: Serializable? = null
        private set

    fun setSavePolicyChange(f: () -> Unit) {
        savePolicyChangeSerialized = f as Serializable
    }

    @Suppress("UNCHECKED_CAST")
    fun savePolicyChange() = (savePolicyChangeSerialized as? () -> Unit)?.invoke()
}