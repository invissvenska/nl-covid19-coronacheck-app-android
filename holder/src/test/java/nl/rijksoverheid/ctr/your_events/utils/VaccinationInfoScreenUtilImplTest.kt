package nl.rijksoverheid.ctr.your_events.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import nl.rijksoverheid.ctr.appconfig.api.model.AppConfig
import nl.rijksoverheid.ctr.appconfig.api.model.HolderConfig
import nl.rijksoverheid.ctr.persistence.CachedAppConfigUseCase
import nl.rijksoverheid.ctr.holder.qrcodes.utils.LastVaccinationDoseUtil
import nl.rijksoverheid.ctr.holder.qrcodes.utils.LastVaccinationDoseUtilImpl
import nl.rijksoverheid.ctr.holder.get_events.models.RemoteEventVaccination
import nl.rijksoverheid.ctr.holder.utils.CountryUtil
import nl.rijksoverheid.ctr.holder.your_events.utils.VaccinationInfoScreenUtilImpl
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.AutoCloseKoinTest
import org.koin.test.inject
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "nl-land")
class VaccinationInfoScreenUtilImplTest : AutoCloseKoinTest() {

    private val lastVaccinationDoseUtil: LastVaccinationDoseUtil by inject()
    private val countryUtil: CountryUtil by inject()

    @Test
    fun `get correct vaccination screen info for non paper proof`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val resources = context.resources

        val cachedAppConfigUseCase: CachedAppConfigUseCase = object : CachedAppConfigUseCase {
            override fun getCachedAppConfigOrNull(): HolderConfig? {
                return null
            }

            override fun getCachedAppConfig() = HolderConfig.default().copy(
                hpkCodes = listOf(
                    AppConfig.HpkCode(
                        code = "2924528",
                        name = "Pfizer (Comirnaty)",
                        vp = "1119349007",
                        mp = "EU/1/20/1528",
                        ma = "ORG-100030215"
                    )
                ),
                euBrands = listOf(
                    AppConfig.Code(
                        code = "EU/1/20/1528",
                        name = "Pfizer (Comirnaty)",
                    )
                ),
                euManufacturers = listOf(
                    AppConfig.Code(
                        code = "ORG-100030215",
                        name = "Biontech Manufacturing GmbH"
                    )
                )
            )
        }

        val vaccinationClock =
            Clock.fixed(Instant.parse("2021-08-01T00:00:00.00Z"), ZoneId.of("UTC"))

        val event = RemoteEventVaccination(
            type = "vaccination",
            unique = "unique",
            vaccination = RemoteEventVaccination.Vaccination(
                date = LocalDate.now(vaccinationClock),
                hpkCode = "2924528",
                type = "type",
                brand = "brand",
                completedByMedicalStatement = true,
                completedByPersonalStatement = false,
                completionReason = "completionReason",
                doseNumber = "1",
                totalDoses = "2",
                country = "NLD",
                manufacturer = "manufacturer",
            )
        )

        val vaccinationInfoScreenUtil = VaccinationInfoScreenUtilImpl(
            lastVaccinationDoseUtil,
            resources,
            countryUtil,
            cachedAppConfigUseCase
        )

        val infoScreen = vaccinationInfoScreenUtil.getForVaccination(
            event = event,
            fullName = "Surname",
            birthDate = "01-08-1982",
            providerIdentifier = "GGD",
            isPaperProof = false
        )

        assertEquals("Details", infoScreen.title)
        assertEquals(
            "Deze gegevens van je vaccinatie zijn opgehaald bij GGD:<br/><br/>Naam: <b>Surname</b><br/>Geboortedatum: <b>01-08-1982</b><br/><br/>Ziekteverwekker: <b>COVID-19</b><br/>Vaccin: <b>Pfizer (Comirnaty)</b><br/>Type vaccin: <b>type</b><br/>Producent: <b>Biontech Manufacturing GmbH</b><br/>Dosis: <b>1 / 2</b><br/>Vaccinatiedatum: <b>1 augustus 2021</b><br/>Gevaccineerd in: <b>Nederland</b><br/>Uniek vaccinatienummer: <b>unique</b><br/>",
            infoScreen.description
        )

    }

    @Test
    fun `get correct vaccination screen info for paper proof`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val resources = context.resources

        val lastVaccinationDoseUtil = LastVaccinationDoseUtilImpl(resources)
        val cachedAppConfigUseCase: CachedAppConfigUseCase = object : CachedAppConfigUseCase {
            override fun getCachedAppConfigOrNull(): HolderConfig? {
                return null
            }

            override fun getCachedAppConfig() = HolderConfig.default().copy(
                hpkCodes = listOf(
                    AppConfig.HpkCode(
                        code = "2924528",
                        name = "Pfizer (Comirnaty)",
                        vp = "1119349007",
                        mp = "EU/1/20/1528",
                        ma = "ORG-100030215"
                    )
                ),
                euBrands = listOf(
                    AppConfig.Code(
                        code = "EU/1/20/1528",
                        name = "Pfizer (Comirnaty)",
                    )
                ),
                euManufacturers = listOf(
                    AppConfig.Code(
                        code = "ORG-100030215",
                        name = "Biontech Manufacturing GmbH"
                    )
                )
            )
        }

        val vaccinationClock =
            Clock.fixed(Instant.parse("2021-08-01T00:00:00.00Z"), ZoneId.of("UTC"))

        val event = RemoteEventVaccination(
            type = "vaccination",
            unique = "unique",
            vaccination = RemoteEventVaccination.Vaccination(
                date = LocalDate.now(vaccinationClock),
                hpkCode = "2924528",
                type = "type",
                brand = "brand",
                completedByMedicalStatement = true,
                completedByPersonalStatement = false,
                completionReason = "completionReason",
                doseNumber = "1",
                totalDoses = "2",
                country = "NLD",
                manufacturer = "manufacturer",
            )
        )

        val vaccinationInfoScreenUtil = VaccinationInfoScreenUtilImpl(
            lastVaccinationDoseUtil,
            resources,
            countryUtil,
            cachedAppConfigUseCase
        )

        val infoScreen = vaccinationInfoScreenUtil.getForVaccination(
            event = event,
            fullName = "Surname",
            birthDate = "01-08-1982",
            providerIdentifier = "GGD",
            isPaperProof = true
        )

        assertEquals("Details", infoScreen.title)
        assertEquals(
            "Deze gegevens staan in je papieren bewijs:<br/><br/>Naam: <b>Surname</b><br/>Geboortedatum: <b>01-08-1982</b><br/><br/>Ziekteverwekker: <b>COVID-19</b><br/>Vaccin: <b>Pfizer (Comirnaty)</b><br/>Type vaccin: <b>type</b><br/>Producent: <b>Biontech Manufacturing GmbH</b><br/>Dosis: <b>1 / 2</b><br/>Vaccinatiedatum: <b>1 augustus 2021</b><br/>Gevaccineerd in: <b>Nederland</b><br/>Uniek vaccinatienummer: <b>unique</b><br/><br/><b>Kloppen er gegevens niet?</b><br/>Neem contact op met de zorgverlener waar je bent gevaccineerd of getest. Dat kan de GGD, je huisarts of arts van een andere zorginstelling zijn. Zij kunnen je helpen je gegevens aan te passen.",
            infoScreen.description
        )
    }
}
