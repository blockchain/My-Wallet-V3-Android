package com.blockchain.kycui.splash

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.kycui.hyperlinks.renderTermsLinks
import com.blockchain.notifications.analytics.logEvent
import com.blockchain.kycui.navhost.KycProgressListener
import com.blockchain.kycui.navhost.models.CampaignType
import com.blockchain.kycui.navhost.models.KycStep
import com.blockchain.kycui.navigate
import com.blockchain.kycui.reentry.KycNavigator
import com.blockchain.notifications.analytics.LoggableEvent
import com.blockchain.ui.extensions.throttledClicks
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import org.koin.android.ext.android.inject
import piuk.blockchain.androidcoreui.utils.ParentActivityDelegate
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.kyc.R
import timber.log.Timber
import kotlinx.android.synthetic.main.fragment_kyc_splash.button_kyc_splash_apply_now as buttonContinue
import kotlinx.android.synthetic.main.fragment_kyc_splash.image_view_cityscape as imageView
import kotlinx.android.synthetic.main.fragment_kyc_splash.text_view_kyc_splash_message as textViewMessage
import kotlinx.android.synthetic.main.fragment_kyc_splash.text_view_kyc_terms_and_conditions as textViewTerms

class KycSplashFragment : Fragment() {

    private val progressListener: KycProgressListener by ParentActivityDelegate(this)

    private val kycNavigator: KycNavigator by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = container?.inflate(R.layout.fragment_kyc_splash)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logEvent(
            when (progressListener.campaignType) {
                CampaignType.Swap -> LoggableEvent.KycWelcome
                CampaignType.Sunriver -> LoggableEvent.KycSunriverStart
            }
        )
        textViewTerms.renderTermsLinks(R.string.kyc_splash_terms_and_conditions)

        val title = when (progressListener.campaignType) {
            CampaignType.Swap -> R.string.kyc_splash_title
            CampaignType.Sunriver -> R.string.sunriver_splash_title
        }

        progressListener.setHostTitle(title)
        progressListener.incrementProgress(KycStep.SplashPage)

        if (progressListener.campaignType == CampaignType.Sunriver) {
            imageView.setImageResource(R.drawable.vector_stellar_rocket)
            textViewMessage.setText(R.string.sunriver_splash_message)
        }
    }

    private val disposable = CompositeDisposable()

    override fun onResume() {
        super.onResume()
        disposable += buttonContinue
            .throttledClicks()
            .flatMapSingle { kycNavigator.findNextStep() }
            .subscribeBy(
                onNext = { navigate(it) },
                onError = { Timber.e(it) }
            )
    }

    override fun onPause() {
        disposable.clear()
        super.onPause()
    }
}
