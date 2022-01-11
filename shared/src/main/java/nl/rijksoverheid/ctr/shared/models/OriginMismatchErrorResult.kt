package nl.rijksoverheid.ctr.shared.models

import java.lang.Exception

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
data class OriginMismatchErrorResult(val step: Step): ErrorResult {
    override fun getCurrentStep(): Step {
        return step
    }

    override fun getException(): Exception {
        return OriginMismatchException()
    }
}

class OriginMismatchException: Exception("origin mismatch exception")
