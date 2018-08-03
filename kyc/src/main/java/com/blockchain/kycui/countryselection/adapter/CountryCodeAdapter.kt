package com.blockchain.kycui.countryselection.adapter

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_country.view.*
import piuk.blockchain.androidcore.utils.Country
import piuk.blockchain.androidcoreui.utils.extensions.autoNotify
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.kyc.R
import kotlin.properties.Delegates

class CountryCodeAdapter(
    private val countrySelector: (String) -> Unit
) : RecyclerView.Adapter<CountryCodeAdapter.CountryCodeViewHolder>() {

    var items: List<Country> by Delegates.observable(emptyList()) { _, oldList, newList ->
        autoNotify(oldList, newList) { o, n -> o == n }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CountryCodeViewHolder =
        CountryCodeViewHolder(parent.inflate(R.layout.item_country))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: CountryCodeViewHolder, position: Int) {
        holder.bind(items[position], countrySelector)
    }

    class CountryCodeViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        private val flag = itemView.text_view_flag
        private val name = itemView.text_view_country_name

        fun bind(
            country: Country,
            countrySelector: (String) -> Unit
        ) {
            flag.text = country.flag
            name.text = country.name

            itemView.setOnClickListener { countrySelector(country.countryCode) }
        }
    }
}