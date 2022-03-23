package nl.rijksoverheid.ctr.holder.api.repositories

import nl.rijksoverheid.ctr.api.factory.NetworkRequestResultFactory
import nl.rijksoverheid.ctr.api.interceptors.SigningCertificate
import nl.rijksoverheid.ctr.holder.models.HolderStep
import nl.rijksoverheid.ctr.holder.api.TestProviderApiClient
import nl.rijksoverheid.ctr.holder.get_events.models.RemoteOriginType
import nl.rijksoverheid.ctr.holder.get_events.models.RemoteProtocol3
import nl.rijksoverheid.ctr.holder.get_events.models.RemoteUnomi
import nl.rijksoverheid.ctr.holder.api.models.SignedResponseWithModel
import nl.rijksoverheid.ctr.shared.models.NetworkRequestResult

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
interface EventProviderRepository {
    companion object {
        /**
         * Get filter for backend endpoints
         */
        fun getFilter(originType: RemoteOriginType): String {
            return when (originType) {
                is RemoteOriginType.Vaccination -> {
                    "vaccination"
                }
                is RemoteOriginType.Recovery -> {
                    "positivetest"
                }
                is RemoteOriginType.Test -> {
                    "negativetest"
                }
            }
        }
    }

    suspend fun getUnomi(
        url: String,
        token: String,
        filter: String,
        scope: String?,
        signingCertificateBytes: List<ByteArray>,
        provider: String,
    ): NetworkRequestResult<RemoteUnomi>

    suspend fun getEvents(
        url: String,
        token: String,
        signingCertificateBytes: List<ByteArray>,
        filter: String,
        scope: String?,
        provider: String,
    ): NetworkRequestResult<SignedResponseWithModel<RemoteProtocol3>>
}

class EventProviderRepositoryImpl(
    private val testProviderApiClient: TestProviderApiClient,
    private val networkRequestResultFactory: NetworkRequestResultFactory,
) : EventProviderRepository {

    override suspend fun getUnomi(
        url: String,
        token: String,
        filter: String,
        scope: String?,
        signingCertificateBytes: List<ByteArray>,
        provider: String,
    ): NetworkRequestResult<RemoteUnomi> {
        val params = mutableMapOf<String, String>()
        params["filter"] = filter
        scope?.let {
            params["scope"] = scope
        }

        return networkRequestResultFactory.createResult(
            step = HolderStep.UnomiNetworkRequest,
            provider = provider,
        ) {
            testProviderApiClient.getUnomi(
                url = url,
                authorization = "Bearer $token",
                params = params,
                certificate = SigningCertificate(signingCertificateBytes)
            ).model
        }
    }

    override suspend fun getEvents(
        url: String,
        token: String,
        signingCertificateBytes: List<ByteArray>,
        filter: String,
        scope: String?,
        provider: String,
    ): NetworkRequestResult<SignedResponseWithModel<RemoteProtocol3>> {
        val params = mutableMapOf<String, String>()
        params["filter"] = filter
        scope?.let {
            params["scope"] = scope
        }

        return networkRequestResultFactory.createResult(
            step = HolderStep.EventNetworkRequest,
            provider = provider,
        ) {
            testProviderApiClient.getEvents(
                url = url,
                authorization = "Bearer $token",
                params = params,
                certificate = SigningCertificate(signingCertificateBytes)
            )
        }
    }
}
