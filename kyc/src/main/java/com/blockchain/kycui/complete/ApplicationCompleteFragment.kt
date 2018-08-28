package com.blockchain.kycui.complete

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.kycui.navhost.KycProgressListener
import com.blockchain.kycui.navhost.models.KycStep
import piuk.blockchain.androidcoreui.utils.ParentActivityDelegate
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.toast
import piuk.blockchain.kyc.R
import kotlinx.android.synthetic.main.fragment_kyc_complete.button_kyc_complete_next as buttonNext

class ApplicationCompleteFragment : Fragment() {

    private val progressListener: KycProgressListener by ParentActivityDelegate(this)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = container?.inflate(R.layout.fragment_kyc_complete)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buttonNext.setOnClickListener {
            toast("Application complete!")
        }

        progressListener.setHostTitle(R.string.kyc_complete_title)
        progressListener.incrementProgress(KycStep.CompletePage)
    }
}