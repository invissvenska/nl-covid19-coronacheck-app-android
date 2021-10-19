/*
 *
 *  *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *  *
 *  *   SPDX-License-Identifier: EUPL-1.2
 *  *
 *
 */

package nl.rijksoverheid.ctr.verifier.ui.scanner

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import nl.rijksoverheid.ctr.design.utils.BottomSheetData
import nl.rijksoverheid.ctr.design.utils.BottomSheetDialogUtil
import nl.rijksoverheid.ctr.shared.ext.findNavControllerSafety
import nl.rijksoverheid.ctr.shared.utils.PersonalDetailsUtil
import nl.rijksoverheid.ctr.verifier.R
import nl.rijksoverheid.ctr.verifier.databinding.FragmentScanResultValidPersonalDetailsBinding
import nl.rijksoverheid.ctr.verifier.ui.scanner.utils.ScannerUtil
import org.koin.android.ext.android.inject

class ScanResultPersonalDetailsFragment :
    Fragment(R.layout.fragment_scan_result_valid_personal_details) {

    private var _binding: FragmentScanResultValidPersonalDetailsBinding? = null
    private val binding get() = _binding!!

    private val bottomSheetDialogUtil: BottomSheetDialogUtil by inject()
    private val scannerUtil: ScannerUtil by inject()
    private val personalDetailsUtil: PersonalDetailsUtil by inject()

    private val args: ScanResultPersonalDetailsFragmentArgs by navArgs()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentScanResultValidPersonalDetailsBinding.bind(view)
        bindButtons()
        presentPersonalDetails()
    }

    private fun bindButtons() {
        binding.bottom.setButtonClick {
            findNavControllerSafety()?.navigate(
                ScanResultPersonalDetailsFragmentDirections.actionNavScanResultValid(args.validData)
            )
        }
        binding.bottom.setSecondaryButtonClick {
            bottomSheetDialogUtil.present(childFragmentManager, BottomSheetData.TitleDescription(
                title = getString(R.string.scan_result_valid_reason_title),
                applyOnDescription = {
                    it.setHtmlText(R.string.scan_result_valid_reason_description)
                }
            ))
        }
        binding.toolbar.setNavigationOnClickListener { findNavControllerSafety()?.popBackStack() }
    }

    private fun presentPersonalDetails() {
        val testResultAttributes = args.validData.verifiedQr.details
        val personalDetails = personalDetailsUtil.getPersonalDetails(
            testResultAttributes.firstNameInitial,
            testResultAttributes.lastNameInitial,
            testResultAttributes.birthDay,
            testResultAttributes.birthMonth,
            includeBirthMonthNumber = true
        )
        binding.personalDetailsLastname.setContent(personalDetails.lastNameInitial)
        binding.personalDetailsFirstname.setContent(personalDetails.firstNameInitial)
        binding.personalDetailsBirthmonth.setContent(personalDetails.birthMonth)
        binding.personalDetailsBirthdate.setContent(personalDetails.birthDay)
    }
}