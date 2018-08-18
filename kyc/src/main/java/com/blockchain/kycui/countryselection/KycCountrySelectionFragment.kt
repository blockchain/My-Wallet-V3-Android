package com.blockchain.kycui.countryselection

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.NavHostFragment.findNavController
import com.blockchain.kycui.countryselection.adapter.CountryCodeAdapter
import com.blockchain.kycui.countryselection.models.CountrySelectionState
import com.blockchain.kycui.countryselection.util.CountryDisplayModel
import com.blockchain.kycui.countryselection.util.toDisplayList
import com.blockchain.kycui.invalidcountry.KycInvalidCountryFragment
import com.blockchain.kycui.navhost.KycProgressListener
import com.blockchain.kycui.navhost.models.KycStep
import com.blockchain.kycui.profile.KycProfileFragment
import com.blockchain.kycui.search.filterCountries
import com.jakewharton.rxbinding2.support.v7.widget.queryTextChanges
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.ReplaySubject
import org.koin.android.ext.android.inject
import piuk.blockchain.androidcoreui.ui.base.BaseFragment
import piuk.blockchain.androidcoreui.ui.customviews.MaterialProgressDialog
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.ParentActivityDelegate
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.toast
import piuk.blockchain.kyc.R
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.android.synthetic.main.fragment_kyc_country_selection.recycler_view_country_selection as recyclerView
import kotlinx.android.synthetic.main.fragment_kyc_country_selection.search_view_kyc as searchView

class KycCountrySelectionFragment :
    BaseFragment<KycCountrySelectionView, KycCountrySelectionPresenter>(), KycCountrySelectionView {

    private val presenter: KycCountrySelectionPresenter by inject()
    private val progressListener: KycProgressListener by ParentActivityDelegate(this)
    private val countryCodeAdapter = CountryCodeAdapter { presenter.onCountrySelected(it) }
    private var countryList = ReplaySubject.create<List<CountryDisplayModel>>(1)
    private var progressDialog: MaterialProgressDialog? = null
    private val compositeDisposable = CompositeDisposable()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = container?.inflate(R.layout.fragment_kyc_country_selection)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            adapter = countryCodeAdapter
        }

        progressListener.setHostTitle(R.string.kyc_country_selection_title)
        progressListener.incrementProgress(KycStep.CountrySelection)

        onViewReady()
    }

    override fun onResume() {
        super.onResume()

        compositeDisposable += countryList
            .filterCountries(
                searchView.queryTextChanges().skipInitialValue()
                    .debounce(100, TimeUnit.MILLISECONDS)
            )
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                countryCodeAdapter.items = it
                recyclerView.scrollToPosition(0)
            }
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    override fun continueFlow(countryCode: String) {
        val args = KycProfileFragment.bundleArgs(countryCode)
        findNavController(this).navigate(R.id.kycProfileFragment, args)
    }

    override fun invalidCountry(countryCode: String) {
        val bundleArgs = KycInvalidCountryFragment.bundleArgs(countryCode)
        findNavController(this).navigate(R.id.kycInvalidCountryFragment, bundleArgs)
    }

    override fun renderUiState(state: CountrySelectionState) {
        when (state) {
            CountrySelectionState.Loading -> showProgress()
            is CountrySelectionState.Error -> showErrorToast(state.errorMessage)
            is CountrySelectionState.Data -> renderCountriesList(state)
        }
    }

    private fun renderCountriesList(state: CountrySelectionState.Data) {
        countryList.onNext(state.countriesList.toDisplayList(Locale.getDefault()))
        hideProgress()
    }

    private fun showErrorToast(errorMessage: Int) {
        hideProgress()
        toast(errorMessage, ToastCustom.TYPE_ERROR)
    }

    private fun showProgress() {
        progressDialog = MaterialProgressDialog(
            activity
        ).apply {
            setMessage(R.string.kyc_country_selection_please_wait)
            setOnCancelListener { presenter.onRequestCancelled() }
            show()
        }
    }

    private fun hideProgress() {
        if (progressDialog != null && progressDialog!!.isShowing) {
            progressDialog?.dismiss()
        }
    }

    override fun createPresenter(): KycCountrySelectionPresenter = presenter

    override fun getMvpView(): KycCountrySelectionView = this

    companion object {
        const val EXTRA_COUNTRY_CODE = "com.blockchain.kycui.countryselection.EXTRA_COUNTRY_CODE"
    }
}