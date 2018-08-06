package com.blockchain.kycui.countryselection

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.NavHostFragment.findNavController
import com.blockchain.injection.getKycComponent
import com.blockchain.kycui.KycProgressListener
import com.blockchain.kycui.countryselection.adapter.CountryCodeAdapter
import com.jakewharton.rxbinding2.support.v7.widget.RxSearchView
import io.reactivex.android.schedulers.AndroidSchedulers
import piuk.blockchain.androidcore.utils.CountryHelper
import piuk.blockchain.androidcoreui.ui.base.BaseFragment
import piuk.blockchain.androidcoreui.ui.customviews.MaterialProgressDialog
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.ParentActivityDelegate
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.toast
import piuk.blockchain.kyc.R
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_kyc_country_selection.recycler_view_country_selection as recyclerView
import kotlinx.android.synthetic.main.fragment_kyc_country_selection.search_view_kyc as searchView

class KycCountrySelectionFragment :
    BaseFragment<KycCountrySelectionView, KycCountrySelectionPresenter>(), KycCountrySelectionView {

    @Inject
    lateinit var presenter: KycCountrySelectionPresenter
    private val progressListener: KycProgressListener by ParentActivityDelegate(this)
    private val countryList = CountryHelper(Locale.getDefault()).countryList
    private val countryCodeAdapter = CountryCodeAdapter { presenter.onCountrySelected(it) }
    private var progressDialog: MaterialProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        getKycComponent().inject(this)
        super.onCreate(savedInstanceState)
    }

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
            adapter = countryCodeAdapter.apply { items = countryList }
        }

        RxSearchView.queryTextChanges(searchView)
            .debounce(100, TimeUnit.MILLISECONDS)
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { query ->
                countryCodeAdapter.items =
                    countryList.filter {
                        it.name.contains(query, ignoreCase = true) ||
                            it.countryCode.contains(query, ignoreCase = true)
                    }
            }
            .doOnNext { recyclerView.scrollToPosition(0) }
            .subscribe()

        progressListener.onProgressUpdated(10, R.string.kyc_country_selection_title)
    }

    override fun continueFlow() {
        findNavController(this).navigate(R.id.kycProfileFragment)
    }

    override fun invalidCountry() {
        AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
            .setTitle(R.string.kyc_country_selection_invalid_country_title)
            .setMessage(R.string.kyc_country_selection_invalid_country_message)
            .setPositiveButton(android.R.string.ok) { _, _ -> requireActivity().finish() }
            .show()
    }

    override fun showErrorToast(errorMessage: Int) {
        toast(errorMessage, ToastCustom.TYPE_ERROR)
    }

    override fun showProgress() {
        progressDialog = MaterialProgressDialog(
            activity
        ).apply {
            setMessage(R.string.kyc_country_selection_please_wait)
            setOnCancelListener { presenter.onRequestCancelled() }
            show()
        }
    }

    override fun hideProgress() {
        if (progressDialog != null && progressDialog!!.isShowing) {
            progressDialog?.dismiss()
        }
    }

    override fun createPresenter(): KycCountrySelectionPresenter = presenter

    override fun getMvpView(): KycCountrySelectionView = this
}