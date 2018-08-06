package com.blockchain.kycui.profile

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import com.blockchain.kycui.KycProgressListener
import com.jakewharton.rxbinding2.widget.TextViewAfterTextChangeEvent
import com.jakewharton.rxbinding2.widget.afterTextChangeEvents
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcoreui.utils.ParentActivityDelegate
import piuk.blockchain.androidcoreui.utils.ViewUtils
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.kyc.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlinx.android.synthetic.main.fragment_kyc_profile.edit_text_date_of_birth as editTextDob
import kotlinx.android.synthetic.main.fragment_kyc_profile.edit_text_kyc_first_name as editTextFirstName
import kotlinx.android.synthetic.main.fragment_kyc_profile.edit_text_kyc_last_name as editTextLastName
import kotlinx.android.synthetic.main.fragment_kyc_profile.input_layout_kyc_date_of_birth as inputLayoutDob

class KycProfileFragment : Fragment() {

    private val progressListener: KycProgressListener by ParentActivityDelegate(this)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = container?.inflate(R.layout.fragment_kyc_profile)

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
                    inputLayoutDob.performClick()
                    ViewUtils.hideKeyboard(requireActivity())
                }
            }
        }

        editTextFirstName.afterTextChangeEvents()
            .debounce(300, TimeUnit.MILLISECONDS)
            .watchTextChangeEvents()
            .subscribe()

        editTextLastName.afterTextChangeEvents()
            .debounce(300, TimeUnit.MILLISECONDS)
            .watchTextChangeEvents()
            .subscribe()

        editTextDob.setOnClickListener { onDateOfBirthClicked() }
    }

    private fun Observable<TextViewAfterTextChangeEvent>.watchTextChangeEvents() =
        this.debounce(300, TimeUnit.MILLISECONDS)
            .map { it.editable()?.toString() ?: "" }
            .skip(1)
            .map { mapToProgress(it) }
            .distinctUntilChanged()
            .observeOn(AndroidSchedulers.mainThread())
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
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(year, month, dayOfMonth)

            val format = SimpleDateFormat("MMMM dd, yyyy")
            val dateString = format.format(selectedCalendar.time)
            editTextDob.setText(dateString)
        }

    private fun mapToProgress(text: String): Int = if (text.isEmpty()) -5 else 5
}