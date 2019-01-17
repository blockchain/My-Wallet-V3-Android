package com.blockchain.kycui.email.entry

import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.blockchain.kycui.extensions.skipFirstUnless
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
import org.koin.android.ext.android.inject
import piuk.blockchain.androidcoreui.ui.base.BaseFragment
import piuk.blockchain.androidcoreui.ui.customviews.MaterialProgressDialog
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.ParentActivityDelegate
import piuk.blockchain.androidcoreui.utils.extensions.getTextString
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.toast
import piuk.blockchain.kyc.R
import java.util.concurrent.TimeUnit
import kotlinx.android.synthetic.main.fragment_kyc_add_email.button_kyc_email_next as buttonNext
import kotlinx.android.synthetic.main.fragment_kyc_add_email.edit_text_kyc_email as editTextEmail
import kotlinx.android.synthetic.main.fragment_kyc_add_email.input_layout_kyc_email as inputLayoutEmail

class KycEmailEntryFragment : BaseFragment<KycEmailEntryView, KycEmailEntryPresenter>(),
    KycEmailEntryView {

    private val presenter: KycEmailEntryPresenter by inject()
    private val progressListener: KycProgressListener by ParentActivityDelegate(this)
    private val compositeDisposable = CompositeDisposable()
    private val emailObservable
        get() = editTextEmail.afterTextChangeEvents()
            .skipInitialValue()
            .debounce(300, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .map { editTextEmail.getTextString() }

    override val uiStateObservable: Observable<Pair<String, Unit>>
        get() = Observables.combineLatest(
            emailObservable.cache(),
            buttonNext.throttledClicks()
        )

    private var progressDialog: MaterialProgressDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = container?.inflate(R.layout.fragment_kyc_add_email)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressListener.setHostTitle(R.string.kyc_email_title)
        progressListener.incrementProgress(KycStep.EmailPage)

        editTextEmail.setOnFocusChangeListener { _, hasFocus ->
            inputLayoutEmail.hint = if (hasFocus) {
                getString(R.string.kyc_email_hint_focused)
            } else {
                getString(R.string.kyc_email_hint_unfocused)
            }
        }

        onViewReady()
    }

    override fun onResume() {
        super.onResume()

        compositeDisposable +=
            editTextEmail
                .onDelayedChange(KycStep.EmailEntered)
                .subscribe()
    }

    override fun onPause() {
        compositeDisposable.clear()
        super.onPause()
    }

    override fun preFillEmail(email: String) {
        editTextEmail.setText(email)
    }

    override fun showErrorToast(message: Int) {
        toast(message, ToastCustom.TYPE_ERROR)
    }

    override fun continueSignUp(email: String) {
        navigate(KycEmailEntryFragmentDirections.ActionValidateEmail(email))
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
            .skipFirstUnless { !it.isEmpty() }
            .observeOn(AndroidSchedulers.mainThread())
            .map { mapToCompleted(it) }
            .distinctUntilChanged()
            .doOnNext {
                updateProgress(it, kycStep)
                buttonNext.isEnabled = it
            }

    private fun mapToCompleted(text: String): Boolean = emailIsValid(text)

    private fun updateProgress(stepCompleted: Boolean, kycStep: KycStep) {
        if (stepCompleted) {
            progressListener.incrementProgress(kycStep)
        } else {
            progressListener.decrementProgress(kycStep)
        }
    }

    override fun createPresenter(): KycEmailEntryPresenter = presenter

    override fun getMvpView(): KycEmailEntryView = this
}

private fun emailIsValid(target: String) =
    !TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches()
