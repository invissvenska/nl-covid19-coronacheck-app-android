package nl.rijksoverheid.ctr.holder.usecase

import androidx.annotation.StringRes
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.rijksoverheid.ctr.appconfig.api.model.AppConfig
import nl.rijksoverheid.ctr.appconfig.api.model.HolderConfig
import nl.rijksoverheid.ctr.appconfig.api.model.VerifierConfig
import nl.rijksoverheid.ctr.appconfig.models.*
import nl.rijksoverheid.ctr.appconfig.persistence.AppConfigPersistenceManager
import nl.rijksoverheid.ctr.appconfig.persistence.RecommendedUpdatePersistenceManager
import nl.rijksoverheid.ctr.holder.R
import nl.rijksoverheid.ctr.holder.persistence.CachedAppConfigUseCase
import nl.rijksoverheid.ctr.holder.persistence.PersistenceManager
import nl.rijksoverheid.ctr.introduction.persistance.IntroductionPersistenceManager
import nl.rijksoverheid.ctr.introduction.ui.status.models.IntroductionStatus
import nl.rijksoverheid.ctr.shared.ext.toObject
import nl.rijksoverheid.ctr.shared.models.DisclosurePolicy
import java.time.Clock
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
interface AppStatusUseCase {
    suspend fun get(config: ConfigResult, currentVersionCode: Int): AppStatus
    fun isAppActive(currentVersionCode: Int): Boolean
    fun checkIfActionRequired(currentVersionCode: Int, appConfig: AppConfig): AppStatus
}

class HolderAppStatusUseCaseImpl(
    private val clock: Clock,
    private val cachedAppConfigUseCase: CachedAppConfigUseCase,
    private val appConfigPersistenceManager: AppConfigPersistenceManager,
    private val recommendedUpdatePersistenceManager: RecommendedUpdatePersistenceManager,
    private val moshi: Moshi,
    private val showNewDisclosurePolicyUseCase: ShowNewDisclosurePolicyUseCase,
    private val appUpdateData: AppUpdateData,
    private val persistenceManager: PersistenceManager,
    private val introductionPersistenceManager: IntroductionPersistenceManager,
) : AppStatusUseCase {

    override suspend fun get(config: ConfigResult, currentVersionCode: Int): AppStatus =
        withContext(Dispatchers.IO) {
            when (config) {
                is ConfigResult.Success -> {
                    checkIfActionRequired(
                        currentVersionCode = currentVersionCode,
                        appConfig = config.appConfig.toObject<HolderConfig>(moshi)
                    )
                }
                is ConfigResult.Error -> {
                    val cachedAppConfig = cachedAppConfigUseCase.getCachedAppConfig()
                    if (appConfigPersistenceManager.getAppConfigLastFetchedSeconds() + cachedAppConfig.configTtlSeconds
                        >= OffsetDateTime.now(clock).toEpochSecond()
                    ) {
                        checkIfActionRequired(
                            currentVersionCode = currentVersionCode,
                            appConfig = cachedAppConfig
                        )
                    } else {
                        AppStatus.Error
                    }
                }
            }
        }

    private fun updateRequired(currentVersionCode: Int, appConfig: AppConfig) = currentVersionCode < appConfig.minimumVersion

    override fun checkIfActionRequired(currentVersionCode: Int, appConfig: AppConfig): AppStatus {
        val newPolicy = showNewDisclosurePolicyUseCase.get()
        return when {
            updateRequired(currentVersionCode, appConfig) -> AppStatus.UpdateRequired
            appConfig.appDeactivated -> AppStatus.Deactivated
            currentVersionCode < appConfig.recommendedVersion -> getUpdateRecommendedStatus(appConfig)
            newFeaturesAvailable() || newPolicy != null -> getNewFeatures(newPolicy)
            newTermsAvailable() -> AppStatus.ConsentNeeded(appUpdateData)
            else -> AppStatus.NoActionRequired
        }
    }

    private fun getUpdateRecommendedStatus(appConfig: AppConfig): AppStatus {
        return getHolderRecommendUpdateStatus(appConfig)
    }

    private fun getHolderRecommendUpdateStatus(appConfig: AppConfig) =
        if (appConfig.recommendedVersion > recommendedUpdatePersistenceManager.getHolderVersionUpdateShown()) {
            recommendedUpdatePersistenceManager.saveHolderVersionShown(appConfig.recommendedVersion)
            AppStatus.UpdateRecommended
        } else {
            AppStatus.NoActionRequired
        }

    override fun isAppActive(currentVersionCode: Int): Boolean {
        val config = cachedAppConfigUseCase.getCachedAppConfig()
        return !config.appDeactivated && !updateRequired(currentVersionCode, config)
    }

    private fun newFeaturesAvailable(): Boolean {
        val newFeatureVersion = appUpdateData.newFeatureVersion
        return appUpdateData.newFeatures.isNotEmpty() &&
                newFeatureVersion != null &&
                !introductionPersistenceManager.getNewFeaturesSeen(newFeatureVersion)
    }

    /**
     * Get the new feature and/or new policy rules as new feature based on policy change
     *
     * @param[newPolicy] New policy or null if policy is not changed
     * @return New features and/or policy change as new features
     */
    private fun getNewFeatures(newPolicy: DisclosurePolicy?): AppStatus.NewFeatures {
        return when {
            newFeaturesAvailable() && newPolicy != null -> AppStatus.NewFeatures(
                appUpdateData.copy(
                    newFeatures = appUpdateData.newFeatures + listOf(
                        getNewPolicyFeatureItem(newPolicy)
                    ),
                ).apply {
                    setSavePolicyChange { persistenceManager.setPolicyScreenSeen(newPolicy) }
                })
            !newFeaturesAvailable() && newPolicy != null -> AppStatus.NewFeatures(
                appUpdateData.copy(
                    newFeatures = listOf(getNewPolicyFeatureItem(newPolicy)),
                    newFeatureVersion = null,
                ).apply {
                    setSavePolicyChange { persistenceManager.setPolicyScreenSeen(newPolicy) }
                })
            else -> AppStatus.NewFeatures(appUpdateData)
        }
    }

    private fun newTermsAvailable() =
        !introductionPersistenceManager.getNewTermsSeen(appUpdateData.newTerms.version)

    private fun getNewPolicyFeatureItem(newPolicy: DisclosurePolicy): NewFeatureItem {
        return NewFeatureItem(
            imageResource = R.drawable.illustration_new_disclosure_policy,
            titleResource = getPolicyFeatureTitle(newPolicy),
            description = getPolicyFeatureBody(newPolicy),
            subTitleColor = R.color.primary_blue,
            subtitleResource = getNewPolicySubtitle(newPolicy)
        )
    }

    @StringRes
    private fun getPolicyFeatureTitle(newPolicy: DisclosurePolicy): Int {
        return when (newPolicy) {
            DisclosurePolicy.ZeroG -> R.string.holder_newintheapp_content_onlyInternationalCertificates_0G_title
            DisclosurePolicy.OneG -> R.string.holder_newintheapp_content_only1G_title
            DisclosurePolicy.ThreeG -> R.string.holder_newintheapp_content_only3G_title
            DisclosurePolicy.OneAndThreeG -> R.string.holder_newintheapp_content_3Gand1G_title
        }
    }

    @StringRes
    private fun getPolicyFeatureBody(newPolicy: DisclosurePolicy): Int {
        return when (newPolicy) {
            DisclosurePolicy.ZeroG -> R.string.holder_newintheapp_content_onlyInternationalCertificates_0G_body
            DisclosurePolicy.OneG -> R.string.holder_newintheapp_content_only1G_body
            DisclosurePolicy.ThreeG -> R.string.holder_newintheapp_content_only3G_body
            DisclosurePolicy.OneAndThreeG -> R.string.holder_newintheapp_content_3Gand1G_body
        }
    }
    private fun getNewPolicySubtitle(newPolicy: DisclosurePolicy) =
        if (newPolicy == DisclosurePolicy.OneG || newPolicy == DisclosurePolicy.ThreeG) {
            R.string.general_newpolicy
        } else {
            R.string.new_in_app_subtitle
        }
}
