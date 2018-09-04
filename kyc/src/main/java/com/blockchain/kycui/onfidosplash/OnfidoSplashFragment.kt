package com.blockchain.kycui.onfidosplash

import android.content.Intent
import android.os.Bundle
import android.support.annotation.DrawableRes
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
import com.blockchain.kycui.navhost.KycProgressListener
import com.blockchain.kycui.navhost.models.KycStep
import com.jakewharton.rxbinding2.view.clicks
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
import io.reactivex.android.schedulers.AndroidSchedulers
import org.koin.android.ext.android.inject
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseFragment
import piuk.blockchain.androidcoreui.ui.customviews.MaterialProgressDialog
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.ParentActivityDelegate
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedColor
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.toast
import piuk.blockchain.kyc.R
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlinx.android.synthetic.main.fragment_kyc_onfido_splash.button_kyc_onfido_splash_next as buttonNext

class OnfidoSplashFragment : BaseFragment<OnfidoSplashView, OnfidoSplashPresenter>(),
    OnfidoSplashView {

    private val presenter: OnfidoSplashPresenter by inject()
    private val progressListener: KycProgressListener by ParentActivityDelegate(this)
    private val countryCode by unsafeLazy { arguments!!.getString(ARGUMENT_COUNTRY_CODE) }
    private val onfido by unsafeLazy { OnfidoFactory.create(requireActivity()).client }
    private var progressDialog: MaterialProgressDialog? = null
    override val uiState: Observable<Unit>
        get() = buttonNext
            .clicks()
            .debounce(500, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_kyc_onfido_splash)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressListener.setHostTitle(R.string.kyc_onfido_splash_title)
        progressListener.incrementProgress(KycStep.OnfidoSplashPage)

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

    override fun continueToOnfido(apiKey: String, applicantId: String) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val sheetView = requireActivity().layoutInflater.inflate(R.layout.bottom_sheet_onfido, null)
        sheetView.findViewById<TextView>(R.id.text_view_document_passport)
            .apply {
                setLeftDrawable(R.drawable.vector_plane)
                launchOnfidoOnClick(
                    apiKey,
                    applicantId,
                    DocumentType.PASSPORT,
                    bottomSheetDialog
                )
            }
        sheetView.findViewById<TextView>(R.id.text_view_document_drivers_license)
            .apply {
                setLeftDrawable(R.drawable.vector_car)
                launchOnfidoOnClick(
                    apiKey,
                    applicantId,
                    DocumentType.DRIVING_LICENCE,
                    bottomSheetDialog
                )
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

    companion object {

        private const val REQUEST_CODE_ONFIDO = 1337
        private const val ARGUMENT_COUNTRY_CODE = "ARGUMENT_COUNTRY_CODE"

        fun bundleArgs(countryCode: String): Bundle = Bundle().apply {
            putString(ARGUMENT_COUNTRY_CODE, countryCode)
        }
    }
}