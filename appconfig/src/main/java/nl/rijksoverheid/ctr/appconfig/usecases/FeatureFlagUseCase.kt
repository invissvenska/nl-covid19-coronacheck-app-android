/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.ctr.appconfig.usecases

import nl.rijksoverheid.ctr.shared.BuildConfigUseCase
import nl.rijksoverheid.ctr.shared.models.VerificationPolicy
import java.lang.IllegalStateException

interface FeatureFlagUseCase {
    fun isVerificationPolicyEnabled(): Boolean
    fun isVerificationPolicySelectionEnabled(): Boolean
}

class FeatureFlagUseCaseImpl(
    private val buildConfigUseCase: BuildConfigUseCase,
    private val appConfigUseCase: CachedAppConfigUseCase,
): FeatureFlagUseCase {

    override fun isVerificationPolicyEnabled(): Boolean {
        return if (appConfigUseCase.getCachedAppConfig().enableVerificationPolicyVersion == 0) {
            false
        } else {
            buildConfigUseCase.getVersionCode() >= appConfigUseCase.getCachedAppConfig().enableVerificationPolicyVersion
        }
    }

    override fun isVerificationPolicySelectionEnabled(): Boolean {
        return appConfigUseCase.getCachedAppConfig().verificationPoliciesEnabled.size > 1
    }
}