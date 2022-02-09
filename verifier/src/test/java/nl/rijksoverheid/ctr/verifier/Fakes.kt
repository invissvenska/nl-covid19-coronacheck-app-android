package nl.rijksoverheid.ctr.verifier

import androidx.lifecycle.MutableLiveData
import mobilecore.Mobilecore
import nl.rijksoverheid.ctr.appconfig.AppConfigViewModel
import nl.rijksoverheid.ctr.appconfig.api.model.AppConfig
import nl.rijksoverheid.ctr.appconfig.api.model.VerifierConfig
import nl.rijksoverheid.ctr.appconfig.models.AppStatus
import nl.rijksoverheid.ctr.appconfig.usecases.CachedAppConfigUseCase
import nl.rijksoverheid.ctr.introduction.IntroductionData
import nl.rijksoverheid.ctr.introduction.IntroductionViewModel
import nl.rijksoverheid.ctr.introduction.ui.status.models.IntroductionStatus
import nl.rijksoverheid.ctr.shared.MobileCoreWrapper
import nl.rijksoverheid.ctr.shared.livedata.Event
import nl.rijksoverheid.ctr.shared.models.*
import nl.rijksoverheid.ctr.verifier.ui.scanner.models.VerifiedQrResultState
import nl.rijksoverheid.ctr.verifier.ui.scanner.usecases.TestResultValidUseCase
import nl.rijksoverheid.ctr.verifier.ui.scanner.usecases.VerifyQrUseCase
import org.json.JSONObject

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

fun fakeAppConfigViewModel(appStatus: AppStatus = AppStatus.NoActionRequired) =
    object : AppConfigViewModel() {
        override fun refresh(mobileCoreWrapper: MobileCoreWrapper, force: Boolean) {
            appStatusLiveData.value = appStatus
        }
    }

fun fakeIntroductionViewModel(
    introductionStatus: IntroductionStatus? = null,
): IntroductionViewModel {

    return object : IntroductionViewModel() {

        init {
            if (introductionStatus != null) {
                (introductionStatusLiveData as MutableLiveData).postValue(Event(introductionStatus))
            }
        }

        override fun getIntroductionStatus(): IntroductionStatus {
            return introductionStatus ?: IntroductionStatus.IntroductionFinished.NoActionRequired
        }

        override fun saveNewFeaturesFinished(newFeaturesVersion: Int) {

        }

        override fun saveIntroductionFinished(introductionData: IntroductionData) {

        }
    }
}

fun fakeTestResultValidUseCase(
    result: VerifiedQrResultState = VerifiedQrResultState.Valid(
        verifiedQr = fakeVerifiedQr()
    )
) = object : TestResultValidUseCase {
    override suspend fun validate(qrContent: String): VerifiedQrResultState {
        return result
    }
}

fun fakeVerifiedQr(
    error: Boolean = false,
    isNLDCC: Boolean = false,
    isSpecimen: String = "0",
    birthDay: String = "dummy",
    birthMonth: String = "dummy",
    firstNameInitial: String = "dummy",
    lastNameInitial: String = "dummy",
) = VerificationResult(
    status = when {
        error -> Mobilecore.VERIFICATION_FAILED_ERROR
        isNLDCC -> Mobilecore.VERIFICATION_FAILED_IS_NL_DCC
        else -> Mobilecore.VERIFICATION_SUCCESS
    },
    details = VerificationResultDetails(birthDay, birthMonth, firstNameInitial, lastNameInitial, isSpecimen, "2", ""),
    error = if (error) {
        "error"
    } else {
        ""
    }
)

fun fakeVerifyQrUseCase(
    isNLDCC: Boolean = false,
    isSpecimen: String = "0",
    error: Boolean = false,
    result: VerifyQrUseCase.VerifyQrResult = VerifyQrUseCase.VerifyQrResult.Success(
        fakeVerifiedQr(isSpecimen = isSpecimen, isNLDCC = isNLDCC, error = error)
    )
) = object : VerifyQrUseCase {
    override suspend fun get(content: String): VerifyQrUseCase.VerifyQrResult {
        return result
    }
}

fun fakeCachedAppConfigUseCase(
    appConfig: AppConfig = VerifierConfig.default(),
): CachedAppConfigUseCase = object : CachedAppConfigUseCase {
    override fun isCachedAppConfigValid(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getCachedAppConfig(): AppConfig {
        return appConfig
    }

    override fun getCachedAppConfigHash(): String {
        return ""
    }
}

fun fakeMobileCoreWrapper(): MobileCoreWrapper {
    return object : MobileCoreWrapper {
        override fun createCredentials(body: ByteArray): String {
            return ""
        }

        override fun readCredential(credentials: ByteArray): ByteArray {
            return ByteArray(0)
        }

        override fun createCommitmentMessage(secretKey: ByteArray, prepareIssueMessage: ByteArray): String {
            return ""
        }

        override fun disclose(secretKey: ByteArray, credential: ByteArray, currentTimeMillis: Long): String {
            return ""
        }

        override fun generateHolderSk(): String {
            return ""
        }

        override fun createDomesticCredentials(createCredentials: ByteArray): List<DomesticCredential> {
            return listOf()
        }

        override fun readEuropeanCredential(credential: ByteArray): JSONObject {
            return JSONObject()
        }

        override fun initializeHolder(configFilesPath: String): String? = null

        override fun initializeVerifier(configFilesPath: String) = ""

        override fun verify(credential: ByteArray, policy: VerificationPolicy): VerificationResult {
            TODO("Not yet implemented")
        }

        override fun readDomesticCredential(credential: ByteArray): ReadDomesticCredential {
            return ReadDomesticCredential(
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "2"
            )
        }
    }
}
