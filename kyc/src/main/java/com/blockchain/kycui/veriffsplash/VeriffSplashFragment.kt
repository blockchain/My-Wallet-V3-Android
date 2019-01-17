package com.blockchain.kycui.veriffsplash

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment.findNavController
import com.blockchain.kycui.navhost.KycProgressListener
import com.blockchain.kycui.navhost.models.KycStep
import com.blockchain.notifications.analytics.LoggableEvent
import com.blockchain.notifications.analytics.logEvent
import com.blockchain.ui.extensions.throttledClicks
import com.blockchain.veriff.VeriffApplicantAndToken
import com.blockchain.veriff.VeriffLauncher
import io.reactivex.Observable
import org.koin.android.ext.android.inject
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseFragment
import piuk.blockchain.androidcoreui.ui.customviews.MaterialProgressDialog
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.ParentActivityDelegate
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.toast
import piuk.blockchain.kyc.R
import timber.log.Timber
import kotlinx.android.synthetic.main.fragment_kyc_veriff_splash.button_kyc_veriff_splash_next as buttonNext

class VeriffSplashFragment : BaseFragment<VeriffSplashView, VeriffSplashPresenter>(),
    VeriffSplashView {

    private val presenter: VeriffSplashPresenter by inject()
    private val progressListener: KycProgressListener by ParentActivityDelegate(this)
    private val countryCode by unsafeLazy { VeriffSplashFragmentArgs.fromBundle(arguments).countryCode }
    private var progressDialog: MaterialProgressDialog? = null
    override val uiState: Observable<String>
        get() = buttonNext.throttledClicks()
            .map { countryCode }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_kyc_veriff_splash)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logEvent(LoggableEvent.KycVerifyIdentity)

        progressListener.setHostTitle(R.string.kyc_onfido_splash_title)
        progressListener.incrementProgress(KycStep.VeriffSplashPage)

        onViewReady()
    }

    override fun showProgressDialog(cancelable: Boolean) {
        progressDialog = MaterialProgressDialog(activity).apply {
            setOnCancelListener { presenter.onProgressCancelled() }
            setMessage(R.string.kyc_country_selection_please_wait)
            setCancelable(cancelable)
            show()
        }
    }

    override fun dismissProgressDialog() {
        progressDialog?.apply { dismiss() }
        progressDialog = null
    }

    @SuppressLint("InflateParams")
    override fun continueToVeriff(
        applicant: VeriffApplicantAndToken
    ) {
        launchVeriff(applicant)
    }

    private fun launchVeriff(applicant: VeriffApplicantAndToken) {
        VeriffLauncher().launchVeriff(requireActivity(), applicant, REQUEST_CODE_VERIFF)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_VERIFF) {
            Timber.d("Veriff result code $resultCode")
            if (resultCode == RESULT_OK) {
                presenter.submitVerification()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun showErrorToast(message: Int) {
        toast(message, ToastCustom.TYPE_ERROR)
    }

    override fun continueToCompletion() {
        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.kyc_nav_xml, true)
            .build()
        findNavController(this).navigate(R.id.applicationCompleteFragment, null, navOptions)
    }

    override fun createPresenter(): VeriffSplashPresenter = presenter

    override fun getMvpView(): VeriffSplashView = this

    companion object {

        private const val REQUEST_CODE_VERIFF = 1440
    }
}