package nl.rijksoverheid.ctr.verifier.ui.scanner

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import nl.rijksoverheid.ctr.design.utils.DialogUtil
import nl.rijksoverheid.ctr.qrscanner.QrCodeScannerFragment
import nl.rijksoverheid.ctr.shared.ext.navigateSafety
import nl.rijksoverheid.ctr.shared.livedata.EventObserver
import nl.rijksoverheid.ctr.verifier.R
import nl.rijksoverheid.ctr.verifier.ui.policy.ConfigVerificationPolicyUseCase
import nl.rijksoverheid.ctr.verifier.ui.policy.VerificationPolicySelectionState
import nl.rijksoverheid.ctr.verifier.ui.scanner.models.ScanResultInvalidData
import nl.rijksoverheid.ctr.verifier.ui.scanner.models.ScanResultValidData
import nl.rijksoverheid.ctr.verifier.ui.scanner.models.VerifiedQrResultState
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
class VerifierQrScannerFragment : QrCodeScannerFragment() {

    private val scannerViewModel: ScannerViewModel by viewModel()
    private val dialogUtil: DialogUtil by inject()
    private val configVerificationPolicyUseCase: ConfigVerificationPolicyUseCase by inject()

    override fun onQrScanned(content: String) {
        scannerViewModel.log()
        scannerViewModel.validate(
            qrContent = content,
            returnUri = arguments?.getString("returnUri"),
        )
    }

    override fun getCopy(): Copy {
        return Copy(
            title = getString(R.string.scanner_custom_title),
            message = getString(R.string.scan_qr_instructions_button),
            onMessageClicked = {
                navigateSafety(
                    VerifierQrScannerFragmentDirections.actionScanInstructions()
                )
            },
            rationaleDialog = Copy.RationaleDialog(
                title = getString(R.string.camera_rationale_dialog_title),
                description = getString(R.string.camera_rationale_dialog_description),
                okayButtonText = getString(R.string.ok)
            ),
            verificationPolicy = configVerificationPolicyUseCase.get().let {
                //TODO fix copies for 1G
                Copy.VerificationPolicy(
                    title = when (it) {
                        is VerificationPolicySelectionState.Policy1G -> R.string.verifier_scanner_policy_indication_2g
                        else -> R.string.verifier_scanner_policy_indication_3g
                    },
                    indicatorColor = when (it) {
                        is VerificationPolicySelectionState.Policy1G -> R.color.primary_blue
                        else -> R.color.secondary_green
                    }
                )
            }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        scannerViewModel.loadingLiveData.observe(viewLifecycleOwner, EventObserver {
            binding.progress.visibility = if (it) View.VISIBLE else View.GONE
        })

        scannerViewModel.qrResultLiveData.observe(viewLifecycleOwner, EventObserver {
            val (qrResultState, externalReturnAppData) = it
            when (qrResultState) {
                is VerifiedQrResultState.Valid -> {
                    navigateSafety(
                        VerifierQrScannerFragmentDirections.actionScanResultPersonalDetails(
                            validData = ScanResultValidData.Valid(
                                verifiedQr = qrResultState.verifiedQr,
                                externalReturnAppData = externalReturnAppData
                            )
                        )
                    )
                }
                is VerifiedQrResultState.Demo -> {
                    navigateSafety(
                        VerifierQrScannerFragmentDirections.actionScanResultPersonalDetails(
                            validData = ScanResultValidData.Demo(
                                verifiedQr = qrResultState.verifiedQr,
                                externalReturnAppData = externalReturnAppData
                            )
                        )
                    )
                }
                is VerifiedQrResultState.Error -> {
                    navigateSafety(
                        VerifierQrScannerFragmentDirections.actionScanResultInvalid(
                            invalidData = ScanResultInvalidData.Error(
                                error = qrResultState.error
                            )
                        )
                    )
                }
                is VerifiedQrResultState.InvalidInNL -> presentDialog(
                    R.string.scan_result_european_in_nl_dialog_title,
                    getString(R.string.scan_result_european_in_nl_dialog_message)
                )
                is VerifiedQrResultState.UnknownQR -> presentDialog(
                    R.string.scan_result_unknown_qr_dialog_title,
                    getString(R.string.scan_result_unknown_qr_dialog_message)
                )
                is VerifiedQrResultState.PartiallyValid -> {
                    navigateSafety(VerifierQrScannerFragmentDirections.actionDccScanResult(
                        data = if (qrResultState.isTestResult) {
                            DccScanResultFragmentData.ScanVaccinationOrRecovery(
                                previousScanVaccinationOrRecoveryResult = qrResultState.verifiedQr,
                            )
                        } else {
                            DccScanResultFragmentData.ScanTest(
                                previousScanTextResult = qrResultState.verifiedQr,
                            )
                        }
                    ))
                }
                is VerifiedQrResultState.PersonalDataMismatch -> {
                    navigateSafety(VerifierQrScannerFragmentDirections.actionScanResultInvalid(
                        invalidData = ScanResultInvalidData.Error("personal data mismatch"),
                        title = getString(R.string.verifier_result_denied_personal_data_mismatch_title),
                    ))
                }
            }
        })
    }

    private fun presentDialog(@StringRes title: Int, message: String) {
        dialogUtil.presentDialog(
            context = requireContext(),
            title = title,
            message = message,
            positiveButtonText = R.string.ok,
            positiveButtonCallback = {
                setUpScanner()
            },
            onDismissCallback = {
                setUpScanner()
            }
        )
    }
}
