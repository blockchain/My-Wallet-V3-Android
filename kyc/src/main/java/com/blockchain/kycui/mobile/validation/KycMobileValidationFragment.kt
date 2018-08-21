package com.blockchain.kycui.mobile.validation

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.blockchain.kycui.mobile.entry.models.PhoneDisplayModel
import com.blockchain.kycui.mobile.entry.models.PhoneVerificationModel
import com.blockchain.kycui.mobile.validation.models.VerificationCode
import com.blockchain.kycui.navhost.KycProgressListener
import com.blockchain.kycui.navhost.models.KycStep
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.afterTextChangeEvents
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.plusAssign
import org.koin.android.ext.android.inject
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseMvpFragment
import piuk.blockchain.androidcoreui.ui.customviews.MaterialProgressDialog
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.ParentActivityDelegate
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.toast
import piuk.blockchain.kyc.R
import java.util.concurrent.TimeUnit
import kotlinx.android.synthetic.main.fragment_kyc_mobile_validation.button_kyc_mobile_validation_next as buttonNext
import kotlinx.android.synthetic.main.fragment_kyc_mobile_validation.edit_text_kyc_mobile_validation_code as editTextVerificationCode
import kotlinx.android.synthetic.main.fragment_kyc_mobile_validation.text_view_mobile_validation_message as textViewPhoneNumber

class KycMobileValidationFragment :
    BaseMvpFragment<KycMobileValidationView, KycMobileValidationPresenter>(),
    KycMobileValidationView {

    private val presenter: KycMobileValidationPresenter by inject()
    private val progressListener: KycProgressListener by ParentActivityDelegate(this)
    private val compositeDisposable = CompositeDisposable()
    private var progressDialog: MaterialProgressDialog? = null
    private val displayModel by unsafeLazy {
        arguments!!.getParcelable(ARGUMENT_PHONE_DISPLAY_MODEL) as PhoneDisplayModel
    }
    private val verificationCodeObservable by unsafeLazy {
        editTextVerificationCode.afterTextChangeEvents()
            .skipInitialValue()
            .debounce(300, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .map {
                PhoneVerificationModel(
                    displayModel.sanitizedString,
                    it.editable().toString()
                )
            }
    }
    override val uiStateObservable: Observable<Pair<PhoneVerificationModel, Unit>> by unsafeLazy {
        Observables.combineLatest(
            verificationCodeObservable.cache(),
            buttonNext
                .clicks()
                .debounce(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_kyc_mobile_validation)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressListener.setHostTitle(R.string.kyc_phone_number_title)
        progressListener.incrementProgress(KycStep.MobileVerifiedPage)
        textViewPhoneNumber.text = displayModel.formattedString

        onViewReady()
    }

    override fun onResume() {
        super.onResume()

        compositeDisposable +=
            editTextVerificationCode
                .onDelayedChange(KycStep.VerificationCodeEntered)
                .subscribe()
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
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

    override fun showErrorToast(message: Int) {
        toast(message, ToastCustom.TYPE_ERROR)
    }

    override fun continueSignUp() {
        toast("Continue signup!")
    }

    override fun displayErrorDialog(message: Int) {
        AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
            .setTitle(R.string.app_name)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun TextView.onDelayedChange(
        kycStep: KycStep
    ): Observable<Boolean> =
        this.afterTextChangeEvents()
            .debounce(300, TimeUnit.MILLISECONDS)
            .map { it.editable()?.toString() ?: "" }
            .skip(1)
            .observeOn(AndroidSchedulers.mainThread())
            .map { mapToCompleted(it) }
            .distinctUntilChanged()
            .doOnNext {
                updateProgress(it, kycStep)
                buttonNext.isEnabled = it
            }

    private fun mapToCompleted(text: String): Boolean = VerificationCode(text).isValid

    private fun updateProgress(stepCompleted: Boolean, kycStep: KycStep) {
        if (stepCompleted) {
            progressListener.incrementProgress(kycStep)
        } else {
            progressListener.decrementProgress(kycStep)
        }
    }

    override fun createPresenter(): KycMobileValidationPresenter = presenter

    override fun getMvpView(): KycMobileValidationView = this

    companion object {

        private const val ARGUMENT_PHONE_DISPLAY_MODEL = "ARGUMENT_PHONE_DISPLAY_MODEL"

        fun bundleArgs(displayModel: PhoneDisplayModel): Bundle = Bundle().apply {
            putParcelable(ARGUMENT_PHONE_DISPLAY_MODEL, displayModel)
        }
    }
}
