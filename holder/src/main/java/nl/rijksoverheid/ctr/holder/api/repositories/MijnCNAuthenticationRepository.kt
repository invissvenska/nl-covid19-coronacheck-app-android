package nl.rijksoverheid.ctr.holder.api.repositories

import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import net.openid.appauth.*
import nl.rijksoverheid.ctr.api.factory.NetworkRequestResultFactory
import nl.rijksoverheid.ctr.holder.BuildConfig
import nl.rijksoverheid.ctr.holder.models.HolderStep
import nl.rijksoverheid.ctr.holder.api.MijnCnApiClient
import nl.rijksoverheid.ctr.holder.get_events.models.LoginType
import nl.rijksoverheid.ctr.holder.get_events.models.MijnCNTokenResponse
import nl.rijksoverheid.ctr.shared.models.NetworkRequestResult
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
class MijnCNAuthenticationRepository(
    private val mijnCnApiClient: MijnCnApiClient,
    private val networkRequestResultFactory: NetworkRequestResultFactory
) : AuthenticationRepository {

    override suspend fun authResponse(
        loginType: LoginType,
        activityResultLauncher: ActivityResultLauncher<Intent>,
        authService: AuthorizationService
    ) {
        val authServiceConfiguration = authorizationServiceConfiguration()
        val authRequest = authRequest(serviceConfiguration = authServiceConfiguration)
        val authIntent = authService.getAuthorizationRequestIntent(authRequest)
        activityResultLauncher.launch(authIntent)
    }

    private suspend fun authorizationServiceConfiguration(): AuthorizationServiceConfiguration {
        return suspendCoroutine { continuation ->
            AuthorizationServiceConfiguration.fetchFromIssuer(Uri.parse(BuildConfig.MIJNCN_BASEURL)) { serviceConfiguration, error ->
                when {
                    error != null -> continuation.resumeWithException(error)
                    serviceConfiguration != null -> continuation.resume(serviceConfiguration)
                    else -> throw Exception("Could not get service configuration")
                }
            }
        }
    }

    private fun authRequest(serviceConfiguration: AuthorizationServiceConfiguration): AuthorizationRequest {
        return AuthorizationRequest.Builder(
            serviceConfiguration,
            BuildConfig.OPEN_ID_CLIENT_ID,
            ResponseTypeValues.CODE,
            Uri.parse(BuildConfig.OPEN_ID_REDIRECT_URL)
        ).setScope("openid email profile").build()
    }

    override suspend fun jwt(
        loginType: LoginType,
        authService: AuthorizationService,
        authResponse: AuthorizationResponse
    ): String {
        val request = authResponse.createTokenExchangeRequest()
        val res = retrieveAccessToken(request)
        return suspendCoroutine { continuation ->
            when (res) {
                is NetworkRequestResult.Success -> continuation.resume(res.response.idToken)
                is NetworkRequestResult.Failed -> continuation.resumeWithException(Exception("Failed to get JWT"))
            }
        }
    }

    private suspend fun retrieveAccessToken(tokenRequest: TokenRequest): NetworkRequestResult<MijnCNTokenResponse> {
        val result =
            networkRequestResultFactory.createResult(HolderStep.AccessTokensNetworkRequest) {
                mijnCnApiClient.getAccessToken(
                    url = tokenRequest.configuration.tokenEndpoint.toString(),
                    code = tokenRequest.authorizationCode ?: "",
                    grantType = tokenRequest.grantType,
                    redirectUri = BuildConfig.OPEN_ID_REDIRECT_URL,
                    codeVerifier = tokenRequest.codeVerifier ?: "",
                    clientId = tokenRequest.clientId
                )
            }
        return result
    }

}
