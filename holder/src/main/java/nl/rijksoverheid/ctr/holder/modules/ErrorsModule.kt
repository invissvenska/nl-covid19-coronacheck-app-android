package nl.rijksoverheid.ctr.holder.modules

import nl.rijksoverheid.ctr.api.factory.NetworkRequestResultFactory
import nl.rijksoverheid.ctr.holder.modules.qualifier.ErrorResponseQualifier
import nl.rijksoverheid.ctr.shared.factories.ErrorCodeStringFactory
import nl.rijksoverheid.ctr.shared.factories.ErrorCodeStringFactoryImpl
import org.koin.core.qualifier.named
import org.koin.dsl.module

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
fun errorsModule(flavor: String) = module {
    factory {
        NetworkRequestResultFactory(
            get(named(ErrorResponseQualifier.CORONA_CHECK)),
            get(),
            get(named(ErrorResponseQualifier.MIJN_CN))
        )
    }

    factory<ErrorCodeStringFactory> {
        ErrorCodeStringFactoryImpl(!flavor.contains("fdroid"))
    }
}
