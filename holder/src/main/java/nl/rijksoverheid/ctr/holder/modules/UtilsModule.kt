package nl.rijksoverheid.ctr.holder.modules

import nl.rijksoverheid.ctr.holder.persistence.database.util.YourEventFragmentEndStateUtil
import nl.rijksoverheid.ctr.holder.persistence.database.util.YourEventFragmentEndStateUtilImpl
import nl.rijksoverheid.ctr.holder.ui.create_qr.widgets.YourEventWidgetUtil
import nl.rijksoverheid.ctr.holder.ui.create_qr.widgets.YourEventWidgetUtilImpl
import nl.rijksoverheid.ctr.holder.ui.create_qr.util.*
import nl.rijksoverheid.ctr.holder.ui.myoverview.items.MyOverViewGreenCardAdapterUtil
import nl.rijksoverheid.ctr.holder.ui.myoverview.items.MyOverViewGreenCardAdapterUtilImpl
import nl.rijksoverheid.ctr.holder.ui.myoverview.items.MyOverviewInfoCardItemUtil
import nl.rijksoverheid.ctr.holder.ui.myoverview.items.MyOverviewInfoCardItemUtilImpl
import nl.rijksoverheid.ctr.holder.ui.myoverview.utils.*
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import java.time.Clock

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
fun utilsModule(versionCode: Int) = module {
    factory<MyOverViewGreenCardAdapterUtil> {
        MyOverViewGreenCardAdapterUtilImpl(
            Clock.systemUTC(),
            androidContext(),
            get(),
            get(),
            get()
        )
    }
    factory<TokenValidatorUtil> { TokenValidatorUtilImpl() }
    factory<CredentialUtil> { CredentialUtilImpl(Clock.systemUTC(), get(), get(), get()) }
    factory<OriginUtil> { OriginUtilImpl(Clock.systemUTC()) }
    factory<RemoteEventHolderUtil> { RemoteEventHolderUtilImpl(get(), get(), get(), get()) }
    factory<RemoteProtocol3Util> { RemoteProtocol3UtilImpl() }
    factory<RemoteEventUtil> { RemoteEventUtilImpl(get()) }
    factory<ReadEuropeanCredentialUtil> { ReadEuropeanCredentialUtilImpl(get()) }
    factory<DashboardItemUtil> { DashboardItemUtilImpl(get(), get(), get(), get(), get(), get(), get()) }
    factory<CountryUtil> { CountryUtilImpl() }
    factory<MultipleQrCodesUtil> { MultipleQrCodesUtilImpl() }
    factory<MyOverviewFragmentInfoItemHandlerUtil> { MyOverviewFragmentInfoItemHandlerUtilImpl(get(), get(), get()) }
    factory<YourEventFragmentEndStateUtil> {
        YourEventFragmentEndStateUtilImpl(get())
    }
    factory<QrCodesFragmentUtil> { QrCodesFragmentUtilImpl(Clock.systemUTC()) }
    factory<YourEventsFragmentUtil> { YourEventsFragmentUtilImpl() }
    factory<YourEventWidgetUtil> { YourEventWidgetUtilImpl() }
    factory<MyOverviewInfoCardItemUtil> { MyOverviewInfoCardItemUtilImpl() }
    factory<DashboardItemEmptyStateUtil> { DashboardItemEmptyStateUtilImpl(get()) }
    factory<MenuUtil> { MenuUtilImpl(get(), get()) }
    factory<ScopeUtil> { ScopeUtilImpl() }
    factory<HeaderItemTextUtil> { HeaderItemTextUtilImpl(get()) }
    factory<CardItemUtil> { CardItemUtilImpl(get(), get()) }
}
