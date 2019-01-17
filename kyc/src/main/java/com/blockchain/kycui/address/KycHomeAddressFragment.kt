package com.blockchain.kycui.address

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.SearchView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.navigation.fragment.NavHostFragment.findNavController
import com.blockchain.extensions.nextAfterOrNull
import com.blockchain.kycui.address.models.AddressDialog
import com.blockchain.kycui.address.models.AddressIntent
import com.blockchain.kycui.address.models.AddressModel
import com.blockchain.kycui.extensions.skipFirstUnless
import com.blockchain.kycui.hyperlinks.renderTermsLinks
import com.blockchain.notifications.analytics.logEvent
import com.blockchain.kycui.navhost.KycProgressListener
import com.blockchain.kycui.navhost.models.KycStep
import com.blockchain.kycui.navigate
import com.blockchain.kycui.profile.models.ProfileModel
import com.blockchain.notifications.analytics.LoggableEvent
import com.blockchain.ui.extensions.throttledClicks
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.location.places.AutocompleteFilter
import com.google.android.gms.location.places.ui.PlaceAutocomplete
import com.jakewharton.rx.replayingShare
import com.jakewharton.rxbinding2.widget.afterTextChangeEvents
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import org.koin.android.ext.android.inject
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseMvpFragment
import piuk.blockchain.androidcoreui.ui.customviews.MaterialProgressDialog
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.ParentActivityDelegate
import piuk.blockchain.androidcoreui.utils.ViewUtils
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.toast
import piuk.blockchain.kyc.BuildConfig
import piuk.blockchain.kyc.KycNavXmlDirections
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
import kotlinx.android.synthetic.main.fragment_kyc_home_address.input_layout_kyc_address_state as textInputLayoutState
import kotlinx.android.synthetic.main.fragment_kyc_home_address.input_layout_kyc_address_zip_code as textInputLayoutZipCode
import kotlinx.android.synthetic.main.fragment_kyc_home_address.search_view_kyc_address as searchViewAddress
import kotlinx.android.synthetic.main.fragment_kyc_home_address.text_view_kyc_terms_and_conditions as textViewTerms

class KycHomeAddressFragment : BaseMvpFragment<KycHomeAddressView, KycHomeAddressPresenter>(),
    KycHomeAddressView {

    private val presenter: KycHomeAddressPresenter by inject()
    private val progressListener: KycProgressListener by ParentActivityDelegate(this)
    private val compositeDisposable = CompositeDisposable()
    private var progressDialog: MaterialProgressDialog? = null
    override val profileModel: ProfileModel by unsafeLazy {
        KycHomeAddressFragmentArgs.fromBundle(arguments).profileModel
    }
    private val initialState by unsafeLazy {
        AddressModel(
            "",
            null,
            "",
            null,
            "",
            profileModel.countryCode
        )
    }
    private val addressSubject = PublishSubject.create<AddressIntent>()
    override val address: Observable<AddressModel> by unsafeLazy {
        AddressDialog(addressSubject, initialState).viewModel
            .replayingShare()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_kyc_home_address)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logEvent(LoggableEvent.KycAddress)
        textViewTerms.renderTermsLinks(R.string.kyc_splash_terms_and_conditions_submit)

        progressListener.setHostTitle(R.string.kyc_address_title)
        progressListener.incrementProgress(KycStep.AddressPage)

        setupImeOptions()
        localiseUi()

        onViewReady()
    }

    override fun continueToMobileVerification(countryCode: String) {
        closeKeyboard()
        navigate(KycNavXmlDirections.ActionStartMobileVerification(countryCode))
    }

    override fun continueToOnfidoSplash(countryCode: String) {
        closeKeyboard()
        if (BuildConfig.VERIFF) {
            navigate(KycNavXmlDirections.ActionStartVeriff(countryCode))
        } else {
            navigate(KycNavXmlDirections.ActionStartOnfido(countryCode))
        }
    }

    override fun tier1Complete() {
        closeKeyboard()
        navigate(KycHomeAddressFragmentDirections.ActionTier1Complete())
    }

    override fun continueToTier2MoreInfoNeeded(countryCode: String) {
        closeKeyboard()
        navigate(KycNavXmlDirections.ActionStartTier2NeedMoreInfo(countryCode))
    }

    override fun restoreUiState(
        line1: String,
        line2: String?,
        city: String,
        state: String?,
        postCode: String,
        countryName: String
    ) {
        editTextFirstLine.setText(line1)
        editTextAptName.setText(line2)
        editTextCity.setText(city)
        editTextState.setText(state)
        editTextZipCode.setText(postCode)
        editTextCountry.setText(countryName)
    }

    private fun startPlacesActivityForResult() {
        val typeFilter = AutocompleteFilter.Builder()
            .setCountry(address.blockingFirst().country)
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
                RESULT_CANCELED -> Unit
                RESULT_OK -> updateAddress(data)
                PlaceAutocomplete.RESULT_ERROR -> logPlacesError(data)
            }
        }
    }

    private fun logPlacesError(data: Intent?) {
        val status = PlaceAutocomplete.getStatus(requireActivity(), data)
        Timber.e("${status.statusMessage}")
        toast(R.string.kyc_address_error_loading_places, ToastCustom.TYPE_ERROR)
    }

    private fun updateAddress(data: Intent?) {
        subscribeToViewObservables()
        try {
            val place = PlaceAutocomplete.getPlace(requireActivity(), data)
            val address =
                Geocoder(context, Locale.getDefault())
                    .getFromLocation(place.latLng.latitude, place.latLng.longitude, 1)
                    ?.firstOrNull()

            if (address != null) {
                editTextFirstLine.setText(address.thoroughfare ?: address.subThoroughfare)
                editTextAptName.setText(address.featureName)
                editTextCity.setText(address.locality ?: address.subAdminArea)
                editTextState.setText(address.adminArea)
                editTextZipCode.setText(address.postalCode)
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    override fun onResume() {
        super.onResume()
        subscribeToViewObservables()
    }

    private fun subscribeToViewObservables() {
        if (compositeDisposable.size() == 0) {
            compositeDisposable +=
                buttonNext
                    .throttledClicks()
                    .subscribeBy(
                        onNext = { presenter.onContinueClicked() },
                        onError = { Timber.e(it) }
                    )

            compositeDisposable += editTextFirstLine
                .onDelayedChange(KycStep.AddressFirstLine)
                .doOnNext { addressSubject.onNext(AddressIntent.FirstLine(it)) }
                .subscribe()
            compositeDisposable += editTextAptName
                .onDelayedChange(KycStep.AptNameOrNumber)
                .doOnNext { addressSubject.onNext(AddressIntent.SecondLine(it)) }
                .subscribe()
            compositeDisposable += editTextCity
                .onDelayedChange(KycStep.City)
                .doOnNext { addressSubject.onNext(AddressIntent.City(it)) }
                .subscribe()
            compositeDisposable += editTextState
                .onDelayedChange(KycStep.State)
                .doOnNext { addressSubject.onNext(AddressIntent.State(it)) }
                .subscribe()
            compositeDisposable += editTextZipCode
                .onDelayedChange(KycStep.ZipCode)
                .doOnNext { addressSubject.onNext(AddressIntent.PostCode(it)) }
                .subscribe()

            compositeDisposable +=
                searchViewAddress.getEditText()
                    .apply { isFocusable = false }
                    .throttledClicks()
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
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    override fun finishPage() {
        findNavController(this).popBackStack()
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

    private fun TextView.onDelayedChange(kycStep: KycStep): Observable<String> =
        this.afterTextChangeEvents()
            .debounce(300, TimeUnit.MILLISECONDS)
            .map { it.editable()?.toString() ?: "" }
            .skipFirstUnless { !it.isEmpty() }
            .observeOn(AndroidSchedulers.mainThread())
            .distinctUntilChanged()
            .doOnNext { updateProgress(mapToCompleted(it), kycStep) }

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
                            closeKeyboard()
                    }
                }
            }
        }
    }

    private fun closeKeyboard() {
        ViewUtils.hideKeyboard(requireActivity())
    }

    override fun createPresenter(): KycHomeAddressPresenter = presenter

    override fun getMvpView(): KycHomeAddressView = this

    private fun SearchView.getEditText(): EditText = this.findViewById(R.id.search_src_text)

    companion object {

        private const val REQUEST_CODE_PLACE_AUTOCOMPLETE = 707
        private const val REQUEST_CODE_PLAY_SERVICES_RESOLUTION = 708
    }
}
