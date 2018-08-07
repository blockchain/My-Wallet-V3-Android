package com.blockchain.kycui.profile

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import com.blockchain.injection.getKycComponent
import com.blockchain.kycui.KycProgressListener
import com.blockchain.kycui.profile.models.ProfileModel
import com.jakewharton.rxbinding2.widget.TextViewAfterTextChangeEvent
import com.jakewharton.rxbinding2.widget.afterTextChangeEvents
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcoreui.ui.base.BaseFragment
import piuk.blockchain.androidcoreui.utils.ParentActivityDelegate
import piuk.blockchain.androidcoreui.utils.ViewUtils
import piuk.blockchain.androidcoreui.utils.extensions.getTextString
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.toast
import piuk.blockchain.kyc.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_kyc_profile.button_kyc_profile_next as buttonNext
import kotlinx.android.synthetic.main.fragment_kyc_profile.edit_text_date_of_birth as editTextDob
import kotlinx.android.synthetic.main.fragment_kyc_profile.edit_text_kyc_first_name as editTextFirstName
import kotlinx.android.synthetic.main.fragment_kyc_profile.edit_text_kyc_last_name as editTextLastName
import kotlinx.android.synthetic.main.fragment_kyc_profile.input_layout_kyc_date_of_birth as inputLayoutDob

class KycProfileFragment : BaseFragment<KycProfileView, KycProfilePresenter>(), KycProfileView {

    @Inject
    lateinit var presenter: KycProfilePresenter
    private val progressListener: KycProgressListener by ParentActivityDelegate(this)
    override val firstName: String
        get() = editTextFirstName.getTextString()
    override val lastName: String
        get() = editTextLastName.getTextString()
    override var dateOfBirth: Calendar? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = container?.inflate(R.layout.fragment_kyc_profile)

    override fun onCreate(savedInstanceState: Bundle?) {
        getKycComponent().inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressListener.onProgressUpdated(15, R.string.kyc_profile_title)

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

        editTextFirstName.afterTextChangeEvents()
            .debounce(300, TimeUnit.MILLISECONDS)
            .watchTextChangeEvents { presenter.firstNameSet = !it.isBlank() }
            .subscribe()

        editTextLastName.afterTextChangeEvents()
            .debounce(300, TimeUnit.MILLISECONDS)
            .watchTextChangeEvents { presenter.lastNameSet = !it.isBlank() }
            .subscribe()

        editTextDob.setOnClickListener { onDateOfBirthClicked() }
        buttonNext.setOnClickListener { presenter.onContinueClicked() }
    }

    override fun continueSignUp(profileModel: ProfileModel) {
        toast(profileModel.toString())
    }

    private fun Observable<TextViewAfterTextChangeEvent>.watchTextChangeEvents(
        presenterPropAssignment: (String) -> Unit
    ) = this.debounce(300, TimeUnit.MILLISECONDS)
        .map { it.editable()?.toString() ?: "" }
        .skip(1)
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(presenterPropAssignment)
        .map { mapToProgress(it) }
        .distinctUntilChanged()
        .doOnNext { progressListener.onIncrementProgress(it) }

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
            progressListener.onIncrementProgress(5)
            presenter.dateSet = true
            dateOfBirth = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }.also {
                val format = SimpleDateFormat("MMMM dd, yyyy")
                val dateString = format.format(it.time)
                editTextDob.setText(dateString)
            }
        }

    private fun mapToProgress(text: String): Int = if (text.isEmpty()) -5 else 5

    override fun setButtonEnabled(enabled: Boolean) {
        buttonNext.isEnabled = enabled
    }

    override fun createPresenter(): KycProfilePresenter = presenter

    override fun getMvpView(): KycProfileView = this
}