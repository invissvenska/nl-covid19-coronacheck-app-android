package nl.rijksoverheid.ctr.holder.paper_proof

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import nl.rijksoverheid.ctr.design.fragments.info.DescriptionData
import nl.rijksoverheid.ctr.design.fragments.info.InfoFragmentData
import nl.rijksoverheid.ctr.design.utils.DialogUtil
import nl.rijksoverheid.ctr.design.utils.InfoFragmentUtil
import nl.rijksoverheid.ctr.holder.*
import nl.rijksoverheid.ctr.holder.databinding.FragmentPaperProofConsentBinding
import nl.rijksoverheid.ctr.holder.models.HolderFlow
import nl.rijksoverheid.ctr.persistence.database.entities.OriginType
import nl.rijksoverheid.ctr.holder.your_events.YourEventsFragmentType
import nl.rijksoverheid.ctr.holder.paper_proof.usecases.ValidatePaperProofResult
import nl.rijksoverheid.ctr.shared.models.ErrorResultFragmentData
import nl.rijksoverheid.ctr.shared.models.Flow
import nl.rijksoverheid.ctr.shared.livedata.EventObserver
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class PaperProofConsentFragment: BaseFragment(R.layout.fragment_paper_proof_consent) {

    private val dialogUtil: DialogUtil by inject()
    private val infoFragmentUtil: InfoFragmentUtil by inject()

    private val args: PaperProofConsentFragmentArgs by navArgs()
    private val holderMainActivityViewModel: HolderMainActivityViewModel by sharedViewModel()

    override fun onButtonClickWithRetryAction() {
        Navigation.findNavController(requireActivity(), R.id.main_nav_host_fragment)
            .navigate(R.id.action_paper_proof_qr_scanner, bundleOf(PaperProofQrScannerFragment.EXTRA_COUPLING_CODE to args.couplingCode))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentPaperProofConsentBinding.bind(view)
        binding.button.setOnClickListener {
            infoFragmentUtil.presentAsBottomSheet(
                fragmentManager = childFragmentManager,
                data = InfoFragmentData.TitleDescription(
                    title = getString(R.string.add_paper_proof_why_only_international_title),
                    descriptionData = DescriptionData(htmlTextString = getString(R.string.add_paper_proof_why_only_international_description))
                )
            )
        }

        binding.bottom.setButtonClick {
            onButtonClickWithRetryAction()
        }

        holderMainActivityViewModel.eventsLiveData.observe(viewLifecycleOwner, EventObserver {
            findNavController().navigate(PaperProofConsentFragmentDirections.actionYourEvents(
                toolbarTitle = getString(R.string.your_dcc_event_toolbar_title),
                type = YourEventsFragmentType.DCC(
                    remoteEvents = it,
                    originType = OriginType.fromTypeString(it.keys.first().events!!.first().type!!)
                ),
                flow = HolderFlow.HkviScan
            ))
        })

        holderMainActivityViewModel.validatePaperProofError.observe(viewLifecycleOwner, EventObserver {
            when (it) {
                is ValidatePaperProofResult.Invalid.DutchQr -> {
                    dialogUtil.presentDialog(
                        context = requireContext(),
                        title = R.string.add_paper_proof_qr_error_dutch_qr_code_dialog_title,
                        message = getString(R.string.add_paper_proof_qr_error_dutch_qr_code_dialog_description),
                        positiveButtonText = R.string.ok,
                        positiveButtonCallback = {}
                    )
                }
                is ValidatePaperProofResult.Invalid.InvalidQr -> {
                    dialogUtil.presentDialog(
                        context = requireContext(),
                        title = R.string.add_paper_proof_qr_error_invalid_qr_dialog_title,
                        message = getString(R.string.add_paper_proof_qr_error_invalid_qr_dialog_description),
                        positiveButtonText = R.string.ok,
                        positiveButtonCallback = {}
                    )
                }
                is ValidatePaperProofResult.Invalid.BlockedQr -> {
                    presentError(
                        data = ErrorResultFragmentData(
                            title = getString(R.string.add_paper_proof_limit_reached_paper_proof_title),
                            description = getString(R.string.add_paper_proof_limit_reached_paper_proof_description),
                            buttonTitle = getString(R.string.back_to_overview),
                            ErrorResultFragmentData.ButtonAction.Destination(R.id.action_my_overview)
                        )
                    )
                }
                is ValidatePaperProofResult.Invalid.ExpiredQr -> {
                    presentError(
                        data = ErrorResultFragmentData(
                            title = getString(R.string.add_paper_proof_expired_paper_proof_title),
                            description = getString(R.string.add_paper_proof_expired_paper_proof_description),
                            buttonTitle = getString(R.string.back_to_overview),
                            ErrorResultFragmentData.ButtonAction.Destination(R.id.action_my_overview)
                        )
                    )
                }
                is ValidatePaperProofResult.Invalid.RejectedQr -> {
                    presentError(
                        data = ErrorResultFragmentData(
                            title = getString(R.string.add_paper_proof_invalid_combination_title),
                            description = getString(R.string.add_paper_proof_invalid_combination_),
                            buttonTitle = getString(R.string.back_to_overview),
                            ErrorResultFragmentData.ButtonAction.Destination(R.id.action_my_overview)
                        )
                    )
                }
                is ValidatePaperProofResult.Invalid.Error -> {
                    presentError(
                        errorResult = it.errorResult
                    )
                }
            }
        })
    }

    override fun getFlow(): Flow {
        return HolderFlow.HkviScan
    }
}