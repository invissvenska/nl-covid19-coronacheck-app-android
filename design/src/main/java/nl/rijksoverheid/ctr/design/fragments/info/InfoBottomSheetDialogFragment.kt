package nl.rijksoverheid.ctr.design.fragments.info

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.material.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import nl.rijksoverheid.ctr.design.databinding.FragmentInfoBottomsheetBinding
import nl.rijksoverheid.ctr.design.utils.InfoFragmentUtil
import nl.rijksoverheid.ctr.shared.ext.findNavControllerSafety
import nl.rijksoverheid.ctr.shared.ext.launchUrl

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
open class InfoBottomSheetDialogFragment : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.setOnShowListener {
            val bottomSheetDialog = it as BottomSheetDialog
            val sheetInternal: View = bottomSheetDialog.findViewById(R.id.design_bottom_sheet)!!
            BottomSheetBehavior.from(sheetInternal).state = BottomSheetBehavior.STATE_EXPANDED
        }
        super.onCreateView(inflater, container, savedInstanceState)
        return FragmentInfoBottomsheetBinding.inflate(inflater).root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentInfoBottomsheetBinding.bind(view)

        binding.close.setOnClickListener {
            dismiss()
        }

        ViewCompat.setAccessibilityDelegate(binding.close, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View?,
                info: AccessibilityNodeInfoCompat?
            ) {
                info?.setTraversalBefore(binding.description)
                super.onInitializeAccessibilityNodeInfo(host, info)
            }
        })

        val expandedBottomSheetData =
            arguments?.get(InfoFragmentUtil.EXTRA_INFO_FRAGMENT_DATA) as? InfoFragmentData ?: return
        binding.title.text = expandedBottomSheetData.title
        binding.description.apply {
            expandedBottomSheetData.descriptionData.run {
                htmlText?.let {
                    setHtmlText(it, htmlLinksEnabled)
                }
                htmlTextString?.let {
                    setHtmlText(
                        htmlText = it,
                        htmlLinksEnabled = htmlLinksEnabled
                    )
                }
                customLinkIntent?.let { enableCustomLinks { context.startActivity(it) } }
            }
        }
        when (expandedBottomSheetData) {
            is InfoFragmentData.TitleDescription -> {
            }
            is InfoFragmentData.TitleDescriptionWithButton -> {
                when (val buttonData = expandedBottomSheetData.secondaryButtonData) {
                    is ButtonData.LinkButton -> {
                        binding.linkButton.run {
                            text = buttonData.text
                            setOnClickListener { buttonData.link.launchUrl(context) }
                            binding.linkButton.visibility = View.VISIBLE
                        }
                    }
                    is ButtonData.NavigationButton -> {
                        binding.navigationButton.run {
                            text = buttonData.text
                            setOnClickListener {
                                findNavControllerSafety()?.navigate(
                                    buttonData.navigationActionId,
                                    buttonData.navigationArguments
                                )
                            }
                            binding.navigationButton.visibility = View.VISIBLE
                        }
                    }
                }
            }
            is InfoFragmentData.TitleDescriptionWithFooter -> {
                binding.footer.text = expandedBottomSheetData.footerText
            }
        }
    }

    companion object {
        const val dataKey = "infoFragmentData"
    }
}
