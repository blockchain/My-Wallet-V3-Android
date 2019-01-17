package com.blockchain.kycui.onfidosplash

import android.content.Intent
import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.annotation.IdRes
import android.support.design.widget.BottomSheetDialog
import android.support.graphics.drawable.VectorDrawableCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment.findNavController
import com.blockchain.kyc.models.nabu.SupportedDocuments
import com.blockchain.notifications.analytics.logEvent
import com.blockchain.kycui.navhost.KycProgressListener
import com.blockchain.kycui.navhost.models.KycStep
import com.blockchain.notifications.analytics.LoggableEvent
import com.blockchain.ui.extensions.throttledClicks
import com.onfido.android.sdk.capture.DocumentType
import com.onfido.android.sdk.capture.ExitCode
import com.onfido.android.sdk.capture.Onfido
import com.onfido.android.sdk.capture.OnfidoConfig
import com.onfido.android.sdk.capture.OnfidoFactory
import com.onfido.android.sdk.capture.errors.OnfidoException
import com.onfido.android.sdk.capture.ui.camera.face.FaceCaptureStep
import com.onfido.android.sdk.capture.ui.camera.face.FaceCaptureVariant
import com.onfido.android.sdk.capture.ui.options.CaptureScreenStep
import com.onfido.android.sdk.capture.upload.Captures
import com.onfido.android.sdk.capture.utils.CountryCode
import com.onfido.api.client.data.Applicant
import io.reactivex.Observable
import org.koin.android.ext.android.inject
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseFragment
import piuk.blockchain.androidcoreui.ui.customviews.MaterialProgressDialog
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.ParentActivityDelegate
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedColor
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.toast
import piuk.blockchain.androidcoreui.utils.extensions.visible
import piuk.blockchain.kyc.R
import timber.log.Timber
import kotlinx.android.synthetic.main.fragment_kyc_onfido_splash.button_kyc_onfido_splash_next as buttonNext

class OnfidoSplashFragment : BaseFragment<OnfidoSplashView, OnfidoSplashPresenter>(),
    OnfidoSplashView {

    private val presenter: OnfidoSplashPresenter by inject()
    private val progressListener: KycProgressListener by ParentActivityDelegate(this)
    private val countryCode by unsafeLazy { OnfidoSplashFragmentArgs.fromBundle(arguments).countryCode }
    private val onfido by unsafeLazy { OnfidoFactory.create(requireActivity()).client }
    private var progressDialog: MaterialProgressDialog? = null
    override val uiState: Observable<String>
        get() = buttonNext.throttledClicks()
            .map { countryCode }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_kyc_onfido_splash)

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

    override fun continueToOnfido(
        apiKey: String,
        applicantId: String,
        supportedDocuments: List<SupportedDocuments>
    ) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val sheetView = requireActivity().layoutInflater.inflate(R.layout.bottom_sheet_onfido, null)

        supportedDocuments
            .map { it.toUiData() }
            .forEach {
                sheetView.findViewById<TextView>(it.textView)
                    .apply {
                        visible()
                        setLeftDrawable(it.icon)
                        launchOnfidoOnClick(
                            apiKey,
                            applicantId,
                            it.documentType,
                            bottomSheetDialog
                        )
                    }
            }

        bottomSheetDialog.setContentView(sheetView)
        bottomSheetDialog.show()
    }

    private fun launchOnfido(apiKey: String, applicantId: String, screenStep: CaptureScreenStep) {
        // We only require document capture and video face capture
        val kycFlowSteps = arrayOf(
            screenStep,
            FaceCaptureStep(FaceCaptureVariant.VIDEO)
        )

        OnfidoConfig.builder()
            .withToken(apiKey)
            .withApplicant(applicantId)
            .withCustomFlow(kycFlowSteps)
            .build()
            .also { onfido.startActivityForResult(requireActivity(), REQUEST_CODE_ONFIDO, it) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_ONFIDO) {
            onfido.handleActivityResult(resultCode, data, object : Onfido.OnfidoResultListener {
                // Send result to backend, continue to exchange
                override fun userCompleted(applicant: Applicant, captures: Captures) {
                    presenter.submitVerification(applicant.id)
                }

                // User left the sdk flow without completing it
                override fun userExited(exitCode: ExitCode, applicant: Applicant) = Unit

                // An exception occurred during the flow
                override fun onError(exception: OnfidoException, applicant: Applicant?) {
                    showErrorToast(R.string.kyc_onfido_splash_verification_error)
                    Timber.e(exception)
                }
            })
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

    override fun createPresenter(): OnfidoSplashPresenter = presenter

    override fun getMvpView(): OnfidoSplashView = this

    private fun TextView.launchOnfidoOnClick(
        apiKey: String,
        applicantId: String,
        documentType: DocumentType,
        bottomSheetDialog: BottomSheetDialog
    ) {
        this.setOnClickListener {
            launchOnfido(
                apiKey,
                applicantId,
                CaptureScreenStep(
                    documentType,
                    CountryCode.getByCode(countryCode) ?: CountryCode.GB
                )
            )
            bottomSheetDialog.cancel()
        }
    }

    private fun TextView.setLeftDrawable(@DrawableRes drawable: Int) {
        VectorDrawableCompat.create(
            resources,
            drawable,
            ContextThemeWrapper(requireActivity(), R.style.AppTheme).theme
        )?.run {
            DrawableCompat.wrap(this)
            DrawableCompat.setTint(this, getResolvedColor(R.color.primary_gray_medium))
            this@setLeftDrawable.setCompoundDrawablesWithIntrinsicBounds(this, null, null, null)
        }
    }

    private fun SupportedDocuments.toUiData(): SupportedDocumentUiData = when (this) {
        SupportedDocuments.PASSPORT -> SupportedDocumentUiData(
            R.drawable.vector_plane,
            R.id.text_view_document_passport,
            DocumentType.PASSPORT
        )
        SupportedDocuments.DRIVING_LICENCE -> SupportedDocumentUiData(
            R.drawable.vector_car,
            R.id.text_view_document_drivers_license,
            DocumentType.DRIVING_LICENCE
        )
        SupportedDocuments.NATIONAL_IDENTITY_CARD -> SupportedDocumentUiData(
            R.drawable.vector_government,
            R.id.text_view_document_id_card,
            DocumentType.NATIONAL_IDENTITY_CARD
        )
        SupportedDocuments.RESIDENCE_PERMIT -> SupportedDocumentUiData(
            R.drawable.vector_government,
            R.id.text_view_document_residence_permit,
            DocumentType.RESIDENCE_PERMIT
        )
    }

    private data class SupportedDocumentUiData(
        @DrawableRes val icon: Int,
        @IdRes val textView: Int,
        val documentType: DocumentType
    )

    companion object {

        private const val REQUEST_CODE_ONFIDO = 1337
    }
}