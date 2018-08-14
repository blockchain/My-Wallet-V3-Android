package com.blockchain.kycui.invalidcountry

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.kycui.navhost.KycProgressListener
import com.blockchain.kycui.navhost.models.KycStep
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.utils.ParentActivityDelegate
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.kyc.R
import java.util.Locale
import kotlinx.android.synthetic.main.fragment_kyc_invalid_country.button_kyc_invalid_country_exit as buttonExit
import kotlinx.android.synthetic.main.fragment_kyc_invalid_country.text_view_kyc_invalid_country_header as textViewHeader
import kotlinx.android.synthetic.main.fragment_kyc_invalid_country.text_view_kyc_invalid_country_message as textViewMessage

class KycInvalidCountryFragment : Fragment() {

    private val progressListener: KycProgressListener by ParentActivityDelegate(this)
    private val countryCode by unsafeLazy { arguments!!.getString(ARGUMENT_COUNTRY_CODE) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = container?.inflate(R.layout.fragment_kyc_invalid_country)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressListener.setHostTitle(R.string.kyc_country_selection_title)
        progressListener.incrementProgress(KycStep.SplashPage)

        val displayCountry = Locale(Locale.getDefault().displayLanguage, countryCode).displayCountry

        textViewHeader.text = getString(R.string.kyc_invalid_country_header, displayCountry)
        textViewMessage.text = getString(R.string.kyc_invalid_country_message, displayCountry)

        buttonExit.setOnClickListener { requireActivity().finish() }
    }

    companion object {

        private const val ARGUMENT_COUNTRY_CODE = "ARGUMENT_COUNTRY_CODE"

        fun bundleArgs(countryCode: String): Bundle = Bundle().apply {
            putString(ARGUMENT_COUNTRY_CODE, countryCode)
        }
    }
}