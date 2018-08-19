package com.blockchain.ui.countryselection

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.jakewharton.rxbinding2.support.v7.widget.queryTextChanges
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.androidcoreui.R
import piuk.blockchain.androidcoreui.utils.extensions.gone
import timber.log.Timber
import java.util.SortedMap
import java.util.concurrent.TimeUnit
import kotlinx.android.synthetic.main.dialog_select_country.list_view_countries as listView
import kotlinx.android.synthetic.main.dialog_select_country.progress_bar_select_country_dialog as progressBar
import kotlinx.android.synthetic.main.dialog_select_country.search_view_country as searchView

class CountryDialog(
    context: Context,
    private val countryListSource: Single<SortedMap<String, String>>,
    private val listener: CountryCodeSelectionListener
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_select_country)

        countryListSource
            .subscribeBy(
                onSuccess = { renderCountryMap(it) },
                onError = {
                    Timber.e(it)
                    cancel()
                }
            )
    }

    private fun renderCountryMap(countryMap: SortedMap<String, String>) {
        val arrayAdapter = ArrayAdapter<String>(
            context,
            android.R.layout.simple_list_item_1,
            android.R.id.text1,
            countryMap.keys.toTypedArray()
        )
        listView.adapter = arrayAdapter
        progressBar.gone()

        listView.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                val item = parent!!.getItemAtPosition(position).toString()
                val code = countryMap[item]!!
                listener.onCountrySelected(code, item)
                dismiss()
            }

        searchView.apply {
            queryHint = context.getString(R.string.search_country)

            this.queryTextChanges()
                .skipInitialValue()
                .debounce(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(AndroidSchedulers.mainThread())
                .doOnNext { arrayAdapter.filter.filter(it) }
                .subscribe()
        }
    }

    interface CountryCodeSelectionListener {

        fun onCountrySelected(code: String, name: String)
    }
}