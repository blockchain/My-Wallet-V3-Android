package com.blockchain.kycui.countryselection

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.kycui.countryselection.adapter.CountryCodeAdapter
import com.jakewharton.rxbinding2.support.v7.widget.RxSearchView
import io.reactivex.android.schedulers.AndroidSchedulers
import piuk.blockchain.androidcore.utils.CountryHelper
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.toast
import piuk.blockchain.kyc.R
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.android.synthetic.main.fragment_kyc_country_selection.recycler_view_country_selection as recyclerView
import kotlinx.android.synthetic.main.fragment_kyc_country_selection.search_view_kyc as searchView

class KycCountrySelectionFragment : Fragment() {

    private val countryList = CountryHelper(Locale.getDefault()).countryList
    private val countryCodeAdapter = CountryCodeAdapter {
        toast("Country selected $it")
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
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(AndroidSchedulers.mainThread())
            .doOnNext { query ->
                countryCodeAdapter.items =
                    countryList.filter {
                        it.name.contains(query, ignoreCase = true) ||
                            it.countryCode.contains(query, ignoreCase = true)
                    }
            }
            .doOnNext { recyclerView.scrollToPosition(0) }
            .subscribe()
    }
}