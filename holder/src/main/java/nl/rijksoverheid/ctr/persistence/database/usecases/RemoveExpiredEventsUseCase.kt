package nl.rijksoverheid.ctr.persistence.database.usecases

import nl.rijksoverheid.ctr.persistence.HolderCachedAppConfigUseCase
import nl.rijksoverheid.ctr.persistence.database.HolderDatabase
import nl.rijksoverheid.ctr.persistence.database.entities.EventGroupEntity
import nl.rijksoverheid.ctr.persistence.database.entities.OriginType
import java.time.Clock
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

interface RemoveExpiredEventsUseCase {
    suspend fun execute(events: List<EventGroupEntity>)
}

class RemoveExpiredEventsUseCaseImpl(
    private val clock: Clock,
    private val cachedAppConfigUseCase: HolderCachedAppConfigUseCase,
    private val holderDatabase: HolderDatabase
): RemoveExpiredEventsUseCase {

    override suspend fun execute(events: List<EventGroupEntity>) {
        events.forEach {
            val cachedAppConfig = cachedAppConfigUseCase.getCachedAppConfig()
            val expirationTimeInHours = when (it.type) {
                is OriginType.Vaccination -> {
                    TimeUnit.DAYS.toHours(cachedAppConfig.vaccinationEventValidityDays.toLong())
                }
                is OriginType.Test -> {
                    cachedAppConfig.testEventValidityHours.toLong()
                }
                is OriginType.Recovery -> {
                    TimeUnit.DAYS.toHours(cachedAppConfig.recoveryEventValidityDays.toLong())
                }
                is OriginType.VaccinationAssessment -> {
                    TimeUnit.DAYS.toHours(cachedAppConfig.vaccinationAssessmentEventValidityDays.toLong())
                }
            }

            if (it.maxIssuedAt.plusHours(expirationTimeInHours) <= OffsetDateTime.now(clock)) {
                holderDatabase.eventGroupDao().delete(it)
            }
        }
    }
}