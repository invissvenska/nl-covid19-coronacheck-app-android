package nl.rijksoverheid.ctr.holder.ui.myoverview.items

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import nl.rijksoverheid.ctr.holder.persistence.database.entities.*
import nl.rijksoverheid.ctr.holder.persistence.database.models.GreenCard
import nl.rijksoverheid.ctr.holder.ui.create_qr.util.CredentialUtil
import nl.rijksoverheid.ctr.holder.ui.create_qr.util.GreenCardUtil
import nl.rijksoverheid.ctr.holder.ui.create_qr.util.OriginState
import nl.rijksoverheid.ctr.holder.ui.myoverview.utils.TestResultAdapterItemUtil
import nl.rijksoverheid.ctr.holder.ui.myoverview.utils.TestResultAdapterItemUtilImpl
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.AutoCloseKoinTest
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
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
class MyOverViewGreenCardAdapterUtilImplTest: AutoCloseKoinTest() {
    private val context: Context by lazy {
        ApplicationProvider.getApplicationContext()
    }

    private val credentialUtil = mockk<CredentialUtil>(relaxed = true)
    private val testResultAdapterItemUtil: TestResultAdapterItemUtil = mockk(relaxed = true)
    private val greenCardUtil: GreenCardUtil = mockk(relaxed = true)

    private val myOverViewGreenCardAdapterUtil: MyOverViewGreenCardAdapterUtil by lazy {
        MyOverViewGreenCardAdapterUtilImpl(context, credentialUtil, testResultAdapterItemUtil, greenCardUtil)
    }

    private val viewBinding = object: ViewBindingWrapper {

        private val p1Title = TextView(context)
        private val p2Title = TextView(context)
        private val p3Title = TextView(context)
        private val p1Subtitle = TextView(context)
        private val p2Subtitle = TextView(context)
        private val p3Subtitle = TextView(context)
        private val lastText = TextView(context)

        override val proof1Title: TextView
            get() = p1Title
        override val proof2Title: TextView
            get() = p2Title
        override val proof3Title: TextView
            get() = p3Title
        override val proof1Subtitle: TextView
            get() = p1Subtitle
        override val proof2Subtitle: TextView
            get() = p2Subtitle
        override val proof3Subtitle: TextView
            get() = p3Subtitle
        override val expiresIn: TextView
            get() = lastText
    }

    @Before
    fun setup() {
        viewBinding.expiresIn.visibility = View.GONE
        viewBinding.proof1Subtitle.visibility = View.GONE
        viewBinding.proof1Title.visibility = View.GONE
        viewBinding.proof2Subtitle.visibility = View.GONE
        viewBinding.proof2Title.visibility = View.GONE
        viewBinding.proof3Subtitle.visibility = View.GONE
        viewBinding.proof3Title.visibility = View.GONE
    }

    @Test
    fun europeanTest() {
        every { credentialUtil.getTestTypeForEuropeanCredentials(any()) } returns "NAAT"
        val greenCard = greenCard(GreenCardType.Eu)
        myOverViewGreenCardAdapterUtil.setContent(greenCard, listOf(OriginState.Valid(greenCard.origins.first())), viewBinding)

        assertEquals("Testbewijs: PCR (NAAT)", viewBinding.proof1Title.text)
        assertEquals("Testdatum: dinsdag 27 juli 11:10", viewBinding.proof1Subtitle.text)
    }

    @Test
    fun domesticTest() {
        val greenCard = greenCard(GreenCardType.Domestic)
        myOverViewGreenCardAdapterUtil.setContent(greenCard, listOf(OriginState.Valid(greenCard.origins.first())), viewBinding)

        assertEquals("Testbewijs:", viewBinding.proof3Title.text)
        assertEquals("geldig t/m woensdag 28 juli 21:06", viewBinding.proof3Subtitle.text)
    }

    @Test
    fun europeanVaccination() {
        every { credentialUtil.getVaccinationDosesForEuropeanCredentials(any(), any()) } returns "dosis 2 van 2"
        val greenCard = greenCard(GreenCardType.Eu, OriginType.Vaccination)
        myOverViewGreenCardAdapterUtil.setContent(greenCard, listOf(OriginState.Valid(greenCard.origins.first())), viewBinding)

        assertEquals("Vaccinatiebewijs: dosis 2 van 2", viewBinding.proof1Title.text)
        assertEquals("Vaccinatiedatum: 27 juli 2021", viewBinding.proof1Subtitle.text)
    }

    @Test
    fun europeanVaccinationFuture() {
        every { credentialUtil.getVaccinationDosesForEuropeanCredentials(any(), any()) } returns "dosis 2 van 2"
        val greenCard = greenCard(GreenCardType.Eu, OriginType.Vaccination)
        myOverViewGreenCardAdapterUtil.setContent(greenCard, listOf(OriginState.Future(greenCard.origins.first())), viewBinding)

        assertEquals("Vaccinatiebewijs: dosis 2 van 2", viewBinding.proof1Title.text)
        assertEquals("Vaccinatiedatum: 27 juli 2021", viewBinding.proof1Subtitle.text)
        assertEquals(View.GONE, viewBinding.expiresIn.visibility)
    }

    @Test
    fun domesticVaccination() {
        val greenCard = greenCard(GreenCardType.Domestic, OriginType.Vaccination)
        myOverViewGreenCardAdapterUtil.setContent(greenCard, listOf(OriginState.Valid(greenCard.origins.first())), viewBinding)

        assertEquals("Vaccinatiebewijs:", viewBinding.proof1Title.text)
        assertEquals("geldig vanaf 27 juli 2021   ", viewBinding.proof1Subtitle.text)
        assertEquals(View.GONE, viewBinding.expiresIn.visibility)
    }

    @Test
    fun domesticVaccinationFuture() {
        val greenCard = greenCard(GreenCardType.Domestic, OriginType.Vaccination)
        myOverViewGreenCardAdapterUtil.setContent(greenCard, listOf(OriginState.Future(greenCard.origins.first())), viewBinding)

        assertEquals("Vaccinatiebewijs:", viewBinding.proof1Title.text)
        assertEquals("geldig vanaf 27 juli 2021   ", viewBinding.proof1Subtitle.text)
        assertEquals("Wordt automatisch geldig", viewBinding.expiresIn.text)
        assertEquals(View.VISIBLE, viewBinding.expiresIn.visibility)
    }

    @Test
    fun europeanRecovery() {
        val greenCard = greenCard(GreenCardType.Eu, OriginType.Recovery)
        myOverViewGreenCardAdapterUtil.setContent(greenCard, listOf(OriginState.Valid(greenCard.origins.first())), viewBinding)

        assertEquals("Herstelbewijs:", viewBinding.proof1Title.text)
        assertEquals("geldig t/m 28 jul 2021", viewBinding.proof1Subtitle.text)
        assertEquals(View.GONE, viewBinding.expiresIn.visibility)
    }

    @Test
    fun europeanRecoveryFuture() {
        val greenCard = greenCard(GreenCardType.Eu, OriginType.Recovery)
        myOverViewGreenCardAdapterUtil.setContent(greenCard, listOf(OriginState.Future(greenCard.origins.first())), viewBinding)

        assertEquals("Herstelbewijs:", viewBinding.proof1Title.text)
        assertEquals("geldig vanaf 27 juli 2021 t/m 28 juli 2021", viewBinding.proof1Subtitle.text)
        assertEquals(View.VISIBLE, viewBinding.expiresIn.visibility)
    }

    @Test
    fun domesticRecovery() {
        val greenCard = greenCard(GreenCardType.Domestic, OriginType.Recovery)
        myOverViewGreenCardAdapterUtil.setContent(greenCard, listOf(OriginState.Valid(greenCard.origins.first())), viewBinding)

        assertEquals("Herstelbewijs:", viewBinding.proof2Title.text)
        assertEquals("geldig t/m 28 jul 2021", viewBinding.proof2Subtitle.text)
        assertEquals(View.GONE, viewBinding.expiresIn.visibility)
    }

    @Test
    fun domesticRecoveryFuture() {
        val greenCard = greenCard(GreenCardType.Domestic, OriginType.Recovery)
        myOverViewGreenCardAdapterUtil.setContent(greenCard, listOf(OriginState.Future(greenCard.origins.first())), viewBinding)

        assertEquals("Herstelbewijs:", viewBinding.proof2Title.text)
        assertEquals("geldig vanaf 27 juli 2021 t/m 28 juli 2021", viewBinding.proof2Subtitle.text)
        assertEquals(View.VISIBLE, viewBinding.expiresIn.visibility)
    }

    @Test
    fun domesticTestExpiringIn6Minutes() {
        val testResultAdapterItemUtil = TestResultAdapterItemUtilImpl(Clock.fixed(Instant.ofEpochSecond(1627495600), ZoneId.of("UTC")))
        val greenCard = greenCard(GreenCardType.Domestic)
        every { greenCardUtil.getExpireDate(greenCard) } returns greenCard.credentialEntities.first().expirationTime
        val myOverViewGreenCardAdapterUtil = MyOverViewGreenCardAdapterUtilImpl(context, credentialUtil, testResultAdapterItemUtil, greenCardUtil)

        myOverViewGreenCardAdapterUtil.setContent(greenCard, listOf(OriginState.Valid(greenCard.origins.first())), viewBinding)

        assertEquals("Testbewijs:", viewBinding.proof3Title.text)
        assertEquals("geldig t/m woensdag 28 juli 21:06", viewBinding.proof3Subtitle.text)
        assertEquals(View.VISIBLE, viewBinding.expiresIn.visibility)
        assertEquals("Verloopt in 1 uur 1 min", viewBinding.expiresIn.text)
    }

    private fun greenCard(greenCardType: GreenCardType, originType: OriginType = OriginType.Test, expiringSoon: Boolean = false): GreenCard {
        // 2021-07-27T09:10Z
        val eventTime = OffsetDateTime.now(Clock.fixed(Instant.ofEpochSecond(1627377000), ZoneId.of("UTC")))
        // 2021-07-27T09:11:40Z
        val validFrom = OffsetDateTime.now(Clock.fixed(Instant.ofEpochSecond(1627377100), ZoneId.of("UTC")))
        // 2021-07-28T21:06:20Z
        val expirationTime = OffsetDateTime.now(Clock.fixed(Instant.ofEpochSecond(1627499200), ZoneId.of("UTC")))
        val credentialEntity = CredentialEntity(
            id = 1,
            greenCardId = 1,
            data = "".toByteArray(),
            credentialVersion = 2,
            validFrom = validFrom,
            expirationTime = expirationTime,
        )

        val originEntity = OriginEntity(
            id = 1,
            greenCardId = 1,
            type = originType,
            eventTime = eventTime,
            expirationTime = expirationTime,
            validFrom = validFrom,
        )

        val greenCard = GreenCard(
            greenCardEntity = GreenCardEntity(
                id = 1,
                walletId = 1,
                type = greenCardType
            ),
            origins = listOf(originEntity),
            credentialEntities = listOf(credentialEntity),
        )

        return greenCard
    }
}
