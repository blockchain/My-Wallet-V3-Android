package com.blockchain.kycui.mobile.entry

import android.annotation.SuppressLint
import android.os.Bundle
import android.telephony.PhoneNumberFormattingTextWatcher
import android.telephony.PhoneNumberUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.fragment.NavHostFragment.findNavController
import com.blockchain.kycui.navhost.KycProgressListener
import com.blockchain.kycui.navhost.models.KycStep
import com.jakewharton.rx.replayingShare
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.afterTextChangeEvents
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import org.koin.android.ext.android.inject
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseFragment
import piuk.blockchain.androidcoreui.ui.customviews.MaterialProgressDialog
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.AndroidUtils
import piuk.blockchain.androidcoreui.utils.ParentActivityDelegate
import piuk.blockchain.androidcoreui.utils.extensions.getTextString
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.toast
import piuk.blockchain.kyc.R
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.android.synthetic.main.fragment_kyc_add_phone_number.button_kyc_phone_number_next as buttonNext
import kotlinx.android.synthetic.main.fragment_kyc_add_phone_number.edit_text_kyc_mobile_number as editTextPhoneNumber
import kotlinx.android.synthetic.main.fragment_kyc_add_phone_number.input_layout_kyc_mobile_number as inputLayoutPhoneNumber

class KycMobileEntryFragment : BaseFragment<KycMobileEntryView, KycMobileEntryPresenter>(),
    KycMobileEntryView {

    private val presenter: KycMobileEntryPresenter by inject()
    private val progressListener: KycProgressListener by ParentActivityDelegate(this)
    private val compositeDisposable = CompositeDisposable()
    private val phoneNumberSubject = PublishSubject.create<String>()
    override val phoneNumber: Observable<String> = phoneNumberSubject
        .replayingShare()
    private var progressDialog: MaterialProgressDialog? = null
    private val prefixGuess by unsafeLazy {
        "+" + PhoneNumberUtil.createInstance(context)
            .getCountryCodeForRegion(Locale.getDefault().country)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = container?.inflate(R.layout.fragment_kyc_add_phone_number)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressListener.setHostTitle(R.string.kyc_phone_number_title)
        progressListener.incrementProgress(KycStep.MobileNumberPage)

        editTextPhoneNumber.addTextChangedListener(PhoneNumberFormattingTextWatcher())
        editTextPhoneNumber.setOnFocusChangeListener { _, hasFocus ->
            inputLayoutPhoneNumber.hint = if (hasFocus) {
                getString(R.string.kyc_phone_number_hint_focused)
            } else {
                getString(R.string.kyc_phone_number_hint_unfocused)
            }

            // Insert our best guess for the device's dialling code
            if (hasFocus && editTextPhoneNumber.getTextString().isEmpty()) {
                editTextPhoneNumber.setText(prefixGuess)
            }
        }

        onViewReady()
    }

    @SuppressLint("SetTextI18n")
    override fun onResume() {
        super.onResume()
        compositeDisposable +=
            editTextPhoneNumber.afterTextChangeEvents()
                .skipInitialValue()
                .debounce(300, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                .doOnNext {
                    val string = it.editable().toString()
                    // Force plus sign even if user deletes it
                    if (string.firstOrNull() != '+') {
                        editTextPhoneNumber.apply {
                            setText("+$string")
                            setSelection(getTextString().length)
                        }
                    }
                    phoneNumberSubject.onNext(editTextPhoneNumber.getTextString())
                }
                .subscribe()

        compositeDisposable +=
            editTextPhoneNumber
                .onDelayedChange(KycStep.MobileNumberEntered)
                .subscribe()

        compositeDisposable +=
            buttonNext.clicks()
                .debounce(500, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                .doOnNext { presenter.onContinueClicked() }
                .subscribe()
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    override fun preFillPhoneNumber(phoneNumber: String) {
        val formattedNumber = if (AndroidUtils.is21orHigher()) {
            PhoneNumberUtils.formatNumber(phoneNumber, Locale.getDefault().isO3Country)
        } else {
            PhoneNumberUtils.formatNumber(phoneNumber)
        }
        editTextPhoneNumber.setText(formattedNumber)
    }

    override fun setButtonEnabled(enabled: Boolean) {
        buttonNext.isEnabled = enabled
    }

    override fun finishPage() {
        findNavController(this).popBackStack()
    }

    override fun showErrorToast(message: Int) {
        toast(message, ToastCustom.TYPE_ERROR)
    }

    override fun continueSignUp() {
        toast("onContinue SignUp")
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
            .doOnNext { updateProgress(it, kycStep) }

    // 5 is the minimum phone number length + area code + "+" symbol
    private fun mapToCompleted(text: String): Boolean = text.length >= 9

    private fun updateProgress(stepCompleted: Boolean, kycStep: KycStep) {
        if (stepCompleted) {
            progressListener.incrementProgress(kycStep)
        } else {
            progressListener.decrementProgress(kycStep)
        }
    }

    override fun createPresenter(): KycMobileEntryPresenter = presenter

    override fun getMvpView(): KycMobileEntryView = this
}