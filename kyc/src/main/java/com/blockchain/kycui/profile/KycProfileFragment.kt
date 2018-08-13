package com.blockchain.kycui.profile

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import com.blockchain.kycui.navhost.KycProgressListener
import com.blockchain.kycui.navhost.models.KycStep
import com.blockchain.kycui.profile.models.ProfileModel
import com.jakewharton.rxbinding2.widget.afterTextChangeEvents
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import org.koin.android.ext.android.inject
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcoreui.ui.base.BaseFragment
import piuk.blockchain.androidcoreui.ui.customviews.MaterialProgressDialog
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.ParentActivityDelegate
import piuk.blockchain.androidcoreui.utils.ViewUtils
import piuk.blockchain.androidcoreui.utils.extensions.getTextString
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.toast
import piuk.blockchain.kyc.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlinx.android.synthetic.main.fragment_kyc_profile.button_kyc_profile_next as buttonNext
import kotlinx.android.synthetic.main.fragment_kyc_profile.edit_text_date_of_birth as editTextDob
import kotlinx.android.synthetic.main.fragment_kyc_profile.edit_text_kyc_first_name as editTextFirstName
import kotlinx.android.synthetic.main.fragment_kyc_profile.edit_text_kyc_last_name as editTextLastName
import kotlinx.android.synthetic.main.fragment_kyc_profile.input_layout_kyc_date_of_birth as inputLayoutDob

class KycProfileFragment : BaseFragment<KycProfileView, KycProfilePresenter>(), KycProfileView {

    private val presenter: KycProfilePresenter by inject()
    private val progressListener: KycProgressListener by ParentActivityDelegate(this)
    private val compositeDisposable = CompositeDisposable()
    override val firstName: String
        get() = editTextFirstName.getTextString()
    override val lastName: String
        get() = editTextLastName.getTextString()
    override var dateOfBirth: Calendar? = null
    private var progressDialog: MaterialProgressDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = container?.inflate(R.layout.fragment_kyc_profile)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressListener.setHostTitle(R.string.kyc_profile_title)
        progressListener.incrementProgress(KycStep.ProfilePage)

        editTextFirstName.setOnEditorActionListener { _, i, _ ->
            consume { if (i == EditorInfo.IME_ACTION_NEXT) editTextLastName.requestFocus() }
        }

        editTextLastName.setOnEditorActionListener { _, i, _ ->
            consume {
                if (i == EditorInfo.IME_ACTION_NEXT) {
                    editTextLastName.clearFocus()
                    ViewUtils.hideKeyboard(requireActivity())
                    inputLayoutDob.performClick()
                }
            }
        }

        compositeDisposable += editTextFirstName
            .onDelayedChange(KycStep.FirstName) { presenter.firstNameSet = it }
            .subscribe()

        compositeDisposable += editTextLastName
            .onDelayedChange(KycStep.LastName) { presenter.lastNameSet = it }
            .subscribe()

        inputLayoutDob.setOnClickListener { onDateOfBirthClicked() }
        editTextDob.setOnClickListener { onDateOfBirthClicked() }
        buttonNext.setOnClickListener { presenter.onContinueClicked() }
    }

    override fun continueSignUp(profileModel: ProfileModel) {
        toast(profileModel.toString())
    }

    override fun showErrorToast(message: Int) {
        toast(message, ToastCustom.TYPE_ERROR)
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
        kycStep: KycStep,
        presenterPropAssignment: (Boolean) -> Unit
    ): Observable<Boolean> =
        this.afterTextChangeEvents()
            .debounce(300, TimeUnit.MILLISECONDS)
            .map { it.editable()?.toString() ?: "" }
            .skip(1)
            .observeOn(AndroidSchedulers.mainThread())
            .map { mapToCompleted(it) }
            .doOnNext(presenterPropAssignment)
            .distinctUntilChanged()
            .doOnNext { updateProgress(it, kycStep) }

    private fun onDateOfBirthClicked() {
        val calendar = Calendar.getInstance().apply { add(Calendar.YEAR, -18) }
        DatePickerDialog(
            context,
            R.style.DatePickerDialogStyle,
            datePickerCallback,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = calendar.timeInMillis
            show()
        }
    }

    private val datePickerCallback: DatePickerDialog.OnDateSetListener
        @SuppressLint("SimpleDateFormat")
        get() = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            progressListener.incrementProgress(KycStep.Birthday)
            presenter.dateSet = true
            dateOfBirth = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }.also {
                val format = SimpleDateFormat("MMMM dd, yyyy")
                val dateString = format.format(it.time)
                editTextDob.setText(dateString)
            }
        }

    private fun mapToCompleted(text: String): Boolean = !text.isEmpty()

    private fun updateProgress(stepCompleted: Boolean, kycStep: KycStep) {
        if (stepCompleted) {
            progressListener.incrementProgress(kycStep)
        } else {
            progressListener.decrementProgress(kycStep)
        }
    }

    override fun setButtonEnabled(enabled: Boolean) {
        buttonNext.isEnabled = enabled
    }

    override fun onDetach() {
        super.onDetach()
        compositeDisposable.clear()
    }

    override fun createPresenter(): KycProfilePresenter = presenter

    override fun getMvpView(): KycProfileView = this
}