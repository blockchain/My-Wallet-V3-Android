package com.blockchain.kycui.invalidcountry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.kycui.navhost.KycProgressListener
import com.blockchain.kycui.navhost.models.KycStep
import org.koin.android.ext.android.inject
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseMvpFragment
import piuk.blockchain.androidcoreui.ui.customviews.MaterialProgressDialog
import piuk.blockchain.androidcoreui.utils.ParentActivityDelegate
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.kyc.R
import kotlinx.android.synthetic.main.fragment_kyc_invalid_country.button_kyc_invalid_country_message_me as buttonMessageMe
import kotlinx.android.synthetic.main.fragment_kyc_invalid_country.text_view_kyc_invalid_country_header as textViewHeader
import kotlinx.android.synthetic.main.fragment_kyc_invalid_country.text_view_kyc_invalid_country_message as textViewMessage
import kotlinx.android.synthetic.main.fragment_kyc_invalid_country.text_view_kyc_no_thanks as buttonNoThanks

class KycInvalidCountryFragment :
    BaseMvpFragment<KycInvalidCountryView, KycInvalidCountryPresenter>(),
    KycInvalidCountryView {

    override val displayModel by unsafeLazy {
        KycInvalidCountryFragmentArgs.fromBundle(arguments).countryDisplayModel
    }
    private val presenter: KycInvalidCountryPresenter by inject()
    private val progressListener: KycProgressListener by ParentActivityDelegate(this)
    private var progressDialog: MaterialProgressDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = container?.inflate(R.layout.fragment_kyc_invalid_country)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressListener.setHostTitle(R.string.kyc_country_selection_title)
        progressListener.incrementProgress(KycStep.SplashPage)

        textViewHeader.text = getString(R.string.kyc_invalid_country_header, displayModel.name)
        textViewMessage.text = getString(R.string.kyc_invalid_country_message, displayModel.name)

        buttonNoThanks.setOnClickListener { presenter.onNoThanks() }
        buttonMessageMe.setOnClickListener { presenter.onNotifyMe() }

        onViewReady()
    }

    override fun finishPage() {
        requireActivity().finish()
    }

    override fun showProgressDialog() {
        progressDialog = MaterialProgressDialog(activity).apply {
            setOnCancelListener { presenter.onProgressCancelled() }
            setMessage(R.string.kyc_country_selection_please_wait)
            show()
        }
    }

    override fun dismissProgressDialog() {
        progressDialog?.apply { dismiss() }
        progressDialog = null
    }

    override fun createPresenter(): KycInvalidCountryPresenter = presenter

    override fun getMvpView(): KycInvalidCountryView = this
}
