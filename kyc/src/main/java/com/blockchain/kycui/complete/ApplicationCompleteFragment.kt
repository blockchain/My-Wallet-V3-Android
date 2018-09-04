package com.blockchain.kycui.complete

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.kycui.navhost.KycProgressListener
import com.blockchain.kycui.navhost.models.KycStep
import com.blockchain.kycui.status.KycStatusActivity
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.androidcoreui.utils.ParentActivityDelegate
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.kyc.R
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlinx.android.synthetic.main.fragment_kyc_complete.button_kyc_complete_next as buttonNext

class ApplicationCompleteFragment : Fragment() {

    private val progressListener: KycProgressListener by ParentActivityDelegate(this)
    private val compositeDisposable = CompositeDisposable()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = container?.inflate(R.layout.fragment_kyc_complete)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressListener.setHostTitle(R.string.kyc_complete_title)
        progressListener.incrementProgress(KycStep.CompletePage)
    }

    override fun onResume() {
        super.onResume()

        compositeDisposable +=
            buttonNext
                .clicks()
                .debounce(500, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                .subscribeBy(
                    onNext = {
                        KycStatusActivity.start(requireContext())
                        // Clear entire KYC flow
                        requireActivity().finish()
                    },
                    onError = { Timber.e(it) }
                )
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }
}