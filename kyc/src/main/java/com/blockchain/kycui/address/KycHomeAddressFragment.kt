package com.blockchain.kycui.address

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.SearchView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import com.blockchain.extensions.nextAfterOrNull
import com.blockchain.kycui.navhost.KycProgressListener
import com.blockchain.kycui.navhost.models.KycStep
import com.blockchain.kycui.profile.models.ProfileModel
import com.blockchain.ui.countryselection.CountryDialog
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.location.places.AutocompleteFilter
import com.google.android.gms.location.places.ui.PlaceAutocomplete
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.afterTextChangeEvents
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import org.koin.android.ext.android.inject
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseMvpFragment
import piuk.blockchain.androidcoreui.ui.customviews.MaterialProgressDialog
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.ParentActivityDelegate
import piuk.blockchain.androidcoreui.utils.ViewUtils
import piuk.blockchain.androidcoreui.utils.extensions.getTextString
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.toast
import piuk.blockchain.kyc.R
import timber.log.Timber
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.android.synthetic.main.fragment_kyc_home_address.button_kyc_address_next as buttonNext
import kotlinx.android.synthetic.main.fragment_kyc_home_address.edit_text_kyc_address_apt_name as editTextAptName
import kotlinx.android.synthetic.main.fragment_kyc_home_address.edit_text_kyc_address_city as editTextCity
import kotlinx.android.synthetic.main.fragment_kyc_home_address.edit_text_kyc_address_country as editTextCountry
import kotlinx.android.synthetic.main.fragment_kyc_home_address.edit_text_kyc_address_first_line as editTextFirstLine
import kotlinx.android.synthetic.main.fragment_kyc_home_address.edit_text_kyc_address_state as editTextState
import kotlinx.android.synthetic.main.fragment_kyc_home_address.edit_text_kyc_address_zip_code as editTextZipCode
import kotlinx.android.synthetic.main.fragment_kyc_home_address.input_layout_kyc_address_country as textInputLayoutCountry
import kotlinx.android.synthetic.main.fragment_kyc_home_address.input_layout_kyc_address_state as textInputLayoutState
import kotlinx.android.synthetic.main.fragment_kyc_home_address.input_layout_kyc_address_zip_code as textInputLayoutZipCode
import kotlinx.android.synthetic.main.fragment_kyc_home_address.search_view_kyc_address as searchViewAddress

class KycHomeAddressFragment : BaseMvpFragment<KycHomeAddressView, KycHomeAddressPresenter>(),
    KycHomeAddressView {

    private val presenter: KycHomeAddressPresenter by inject()
    private val progressListener: KycProgressListener by ParentActivityDelegate(this)
    private val compositeDisposable = CompositeDisposable()
    private var progressDialog: MaterialProgressDialog? = null
    override val profileModel: ProfileModel by unsafeLazy {
        arguments!!.getParcelable(ARGUMENT_PROFILE_MODEL) as ProfileModel
    }
    override val firstLine: String
        get() = editTextAptName.getTextString()
    override val secondLine: String
        get() = editTextFirstLine.getTextString()
    override val city: String
        get() = editTextCity.getTextString()
    override val state: String
        get() = editTextState.getTextString()
    override val zipCode: String
        get() = editTextZipCode.getTextString()
    private var _countryCode: String? = null
    override var countryCode: String
        get() = if (_countryCode == null) profileModel.countryCode else _countryCode!!
        set(value) {
            _countryCode = value
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_kyc_home_address)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressListener.setHostTitle(R.string.kyc_address_title)
        progressListener.incrementProgress(KycStep.AddressPage)

        buttonNext.setOnClickListener { presenter.onContinueClicked() }
        editTextCountry.setOnClickListener { displayCountryDialog() }
        textInputLayoutCountry.setOnClickListener { displayCountryDialog() }

        setupImeOptions()
        localiseUi()

        searchViewAddress.getEditText()
            .apply { isFocusable = false }
            .clicks()
            .debounce(300, TimeUnit.MILLISECONDS)
            .subscribeBy(
                onNext = {
                    try {
                        startPlacesActivityForResult()
                    } catch (e: GooglePlayServicesRepairableException) {
                        showRecoverableErrorDialog()
                    } catch (e: GooglePlayServicesNotAvailableException) {
                        showUnrecoverableErrorDialog()
                    }
                }
            )
    }

    override fun continueSignUp() {
        // TODO: Navigate to the phone number fragment
        toast("Continue flow")
    }

    private fun displayCountryDialog() {
        CountryDialog(
            requireContext(),
            presenter.countryCodeSingle,
            object :
                CountryDialog.CountryCodeSelectionListener {
                override fun onCountrySelected(code: String, name: String) {
                    countryCode = code
                    editTextCountry.setText(name)
                }
            }).show()
    }

    private fun startPlacesActivityForResult() {
        val typeFilter = AutocompleteFilter.Builder()
            .setTypeFilter(AutocompleteFilter.TYPE_FILTER_ADDRESS)
            .build()

        PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
            .setFilter(typeFilter)
            .build(requireActivity())
            .run { startActivityForResult(this, REQUEST_CODE_PLACE_AUTOCOMPLETE) }
    }

    private fun showRecoverableErrorDialog() {
        GoogleApiAvailability.getInstance()
            .getErrorDialog(
                requireActivity(),
                GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                    context
                ),
                REQUEST_CODE_PLAY_SERVICES_RESOLUTION
            )
            .show()
    }

    private fun showUnrecoverableErrorDialog() {
        AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
            .setTitle(R.string.app_name)
            .setMessage(R.string.kyc_address_google_not_available)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PLACE_AUTOCOMPLETE) {
            when (resultCode) {
                RESULT_OK -> {
                    val place = PlaceAutocomplete.getPlace(requireActivity(), data)
                    Timber.d("Place: " + place.name)
                    // TODO: map to Address object, update UI. This isn't possible just yet as we're missing
                    // the API key.
                }
                PlaceAutocomplete.RESULT_ERROR -> {
                    val status = PlaceAutocomplete.getStatus(requireActivity(), data)
                    Timber.e(status.statusMessage)
                }
                RESULT_CANCELED -> {
                    // User cancelled search
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        compositeDisposable += editTextAptName
            .onDelayedChange(KycStep.AptNameOrNumber) { presenter.firstLineSet = it }
            .subscribe()
        compositeDisposable += editTextCity
            .onDelayedChange(KycStep.City) { presenter.citySet = it }
            .subscribe()
        compositeDisposable += editTextFirstLine
            .onDelayedChange(KycStep.AddressFirstLine) { /* No-op */ }
            .subscribe()
        compositeDisposable += editTextState
            .onDelayedChange(KycStep.State) { /* No-op */ }
            .subscribe()
        compositeDisposable += editTextZipCode
            .onDelayedChange(KycStep.ZipCode) { presenter.zipCodeSet = it }
            .subscribe()
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    override fun setButtonEnabled(enabled: Boolean) {
        buttonNext.isEnabled = enabled
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

    private fun localiseUi() {
        if (profileModel.countryCode.equals("US", ignoreCase = true)) {
            searchViewAddress.queryHint = getString(
                R.string.kyc_address_search_hint,
                getString(R.string.kyc_address_search_hint_zipcode)
            )
            textInputLayoutState.hint = getString(R.string.kyc_address_address_state_hint)
            textInputLayoutZipCode.hint = getString(R.string.kyc_address_address_zip_code_hint)
        } else {
            searchViewAddress.queryHint = getString(
                R.string.kyc_address_search_hint,
                getString(R.string.kyc_address_search_hint_postcode)
            )
            textInputLayoutState.hint = getString(R.string.kyc_address_address_county_hint)
            textInputLayoutZipCode.hint = getString(R.string.kyc_address_address_postcode_hint)
        }

        editTextCountry.setText(
            Locale(
                Locale.getDefault().displayLanguage,
                profileModel.countryCode
            ).displayCountry
        )
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

    private fun mapToCompleted(text: String): Boolean = !text.isEmpty()

    private fun updateProgress(stepCompleted: Boolean, kycStep: KycStep) {
        if (stepCompleted) {
            progressListener.incrementProgress(kycStep)
        } else {
            progressListener.decrementProgress(kycStep)
        }
    }

    private fun setupImeOptions() {
        val editTexts = listOf(
            editTextFirstLine,
            editTextAptName,
            editTextCity,
            editTextState,
            editTextZipCode
        )

        editTexts.forEach { editText ->
            editText.setOnEditorActionListener { _, i, _ ->
                consume {
                    when (i) {
                        EditorInfo.IME_ACTION_NEXT ->
                            editTexts.nextAfterOrNull { it === editText }?.requestFocus()
                        EditorInfo.IME_ACTION_DONE ->
                            ViewUtils.hideKeyboard(requireActivity())
                    }
                }
            }
        }
    }

    override fun createPresenter(): KycHomeAddressPresenter = presenter

    override fun getMvpView(): KycHomeAddressView = this

    private fun SearchView.getEditText(): EditText = this.findViewById(R.id.search_src_text)

    companion object {

        private const val REQUEST_CODE_PLACE_AUTOCOMPLETE = 707
        private const val REQUEST_CODE_PLAY_SERVICES_RESOLUTION = 708

        private const val ARGUMENT_PROFILE_MODEL = "ARGUMENT_PROFILE_MODEL"

        fun bundleArgs(profileModel: ProfileModel): Bundle = Bundle().apply {
            putParcelable(ARGUMENT_PROFILE_MODEL, profileModel)
        }
    }
}
