package nl.rijksoverheid.ctr.holder.modules

import nl.rijksoverheid.ctr.holder.get_events.usecases.*
import nl.rijksoverheid.ctr.holder.paper_proof.usecases.*
import nl.rijksoverheid.ctr.persistence.database.usecases.RemoveExpiredEventsUseCase
import nl.rijksoverheid.ctr.persistence.database.usecases.RemoveExpiredEventsUseCaseImpl
import nl.rijksoverheid.ctr.holder.qrcodes.usecases.QrCodeUseCase
import nl.rijksoverheid.ctr.holder.qrcodes.usecases.QrCodeUseCaseImpl
import nl.rijksoverheid.ctr.holder.your_events.usecases.SaveEventsUseCase
import nl.rijksoverheid.ctr.holder.your_events.usecases.SaveEventsUseCaseImpl
import org.koin.dsl.module
import java.time.Clock

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
val eventsUseCasesModule = module {
    factory<PaperProofCodeUseCase> {
        PaperProofCodeUseCaseImpl()
    }

    factory<GetEventProvidersWithTokensUseCase> {
        GetEventProvidersWithTokensUseCaseImpl(get())
    }
    factory<GetRemoteEventsUseCase> {
        GetRemoteEventsUseCaseImpl(get())
    }
    factory<QrCodeUseCase> {
        QrCodeUseCaseImpl(
            get(),
            get(),
            get(),
            get()
        )
    }
    factory<GetDigidEventsUseCase> { GetDigidEventsUseCaseImpl(get(), get(), get(), get(), get()) }
    factory<GetMijnCnEventsUsecase> { GetMijnCnEventsUsecaseImpl(get(), get(), get()) }
    factory<SaveEventsUseCase> { SaveEventsUseCaseImpl(get(), get(), get(), get()) }
    factory<ValidatePaperProofUseCase> {
        ValidatePaperProofUseCaseImpl(get(), get())
    }

    factory<GetEventsFromPaperProofQrUseCase> {
        GetEventsFromPaperProofQrUseCaseImpl(get(), get())
    }

    factory<RemoveExpiredEventsUseCase> {
        RemoveExpiredEventsUseCaseImpl(Clock.systemUTC(), get(), get())
    }
}
