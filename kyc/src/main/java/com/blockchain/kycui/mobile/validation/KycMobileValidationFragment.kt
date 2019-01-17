package com.blockchain.kycui.mobile.validation

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.fragment.NavHostFragment.findNavController
import com.blockchain.kycui.extensions.skipFirstUnless
import com.blockchain.kycui.hyperlinks.insertSingleLink
import com.blockchain.kycui.mobile.entry.models.PhoneVerificationModel
import com.blockchain.kycui.mobile.validation.models.VerificationCode
import com.blockchain.kycui.navhost.KycProgressListener
import com.blockchain.kycui.navhost.models.KycStep
import com.blockchain.kycui.navigate
import com.blockchain.ui.extensions.throttledClicks
import com.jakewharton.rxbinding2.widget.afterTextChangeEvents
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import org.koin.android.ext.android.inject
import piuk.blockchain.androidcore.data.settings.PhoneNumber
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseMvpFragment
import piuk.blockchain.androidcoreui.ui.customviews.MaterialProgressDialog
import piuk.blockchain.androidcoreui.utils.ParentActivityDelegate
import piuk.blockchain.androidcoreui.utils.ViewUtils
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.kyc.BuildConfig
import piuk.blockchain.kyc.KycNavXmlDirections
import piuk.blockchain.kyc.R
import java.util.concurrent.TimeUnit
import kotlinx.android.synthetic.main.fragment_kyc_mobile_validation.button_kyc_mobile_validation_next as buttonNext
import kotlinx.android.synthetic.main.fragment_kyc_mobile_validation.edit_text_kyc_mobile_validation_code as editTextVerificationCode
import kotlinx.android.synthetic.main.fragment_kyc_mobile_validation.text_view_mobile_validation_message as textViewPhoneNumber
import kotlinx.android.synthetic.main.fragment_kyc_mobile_validation.text_view_resend_prompt as textViewResend

class KycMobileValidationFragment :
    BaseMvpFragment<KycMobileValidationView, KycMobileValidationPresenter>(),
    KycMobileValidationView {

    private val presenter: KycMobileValidationPresenter by inject()
    private val progressListener: KycProgressListener by ParentActivityDelegate(this)
    private val compositeDisposable = CompositeDisposable()
    private var progressDialog: MaterialProgressDialog? = null
    private val args by unsafeLazy { KycMobileValidationFragmentArgs.fromBundle(arguments) }
    private val displayModel by unsafeLazy { args.mobileNumber }
    private val countryCode by unsafeLazy { args.countryCode }
    private val verificationCodeObservable by unsafeLazy {
        editTextVerificationCode.afterTextChangeEvents()
            .skipInitialValue()
            .debounce(300, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .map {
                PhoneVerificationModel(
                    displayModel.sanitizedString,
                    VerificationCode(it.editable().toString())
                )
            }
    }

    private val resend = PublishSubject.create<Unit>()

    override val resendObservable: Observable<Pair<PhoneNumber, Unit>> by unsafeLazy {
        Observables.combineLatest(
            Observable.just(PhoneNumber(displayModel.formattedString)),
            resend.throttledClicks()
        )
    }

    override val uiStateObservable: Observable<Pair<PhoneVerificationModel, Unit>> by unsafeLazy {
        Observables.combineLatest(
            verificationCodeObservable.cache(),
            buttonNext.throttledClicks()
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

        textViewResend.insertSingleLink(
            R.string.kyc_phone_didnt_see_sms,
            R.string.kyc_phone_send_again_hyperlink
        ) {
            resend.onNext(Unit)
        }

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

    override fun continueSignUp() {
        ViewUtils.hideKeyboard(requireActivity())
        findNavController(this).apply {
            // Remove phone entry and validation pages from back stack as it would be confusing for the user
            popBackStack(R.id.kycPhoneNumberFragment, true)
            if (BuildConfig.VERIFF) {
                navigate(KycNavXmlDirections.ActionStartVeriff(countryCode))
            } else {
                navigate(KycNavXmlDirections.ActionStartOnfido(countryCode))
            }
        }
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
            .skipFirstUnless { !it.isEmpty() }
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

    override fun theCodeWasResent() {
        Toast.makeText(requireContext(), R.string.kyc_phone_number_code_was_resent, Toast.LENGTH_SHORT).show()
    }
}