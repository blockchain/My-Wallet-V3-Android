package com.blockchain.kycui.search

import com.blockchain.kycui.countryselection.util.CountryDisplayModel
import io.reactivex.Observable
import org.junit.Test

class CountrySelectionFilterKtTest {

    @Test
    fun `returns entire list on initial subscription`() {
        countryList.just()
            .filterCountries(Observable.empty())
            .test()
            .assertValue(countryList)
    }

    @Test
    fun `filters out all countries if string doesn't match`() {
        countryList.just()
            .filterCountries("Denmark".just())
            .skip(1)
            .test()
            .assertValue(emptyList())
    }

    @Test
    fun `returns exact match for country code`() {
        countryList.just()
            .filterCountries("GB".just())
            .skip(1)
            .test()
            .assertValue(listOf(countryList[2]))
    }

    @Test
    fun `returns two matches but prioritises acronym`() {
        countryList.just()
            .filterCountries("UK".just())
            .skip(1)
            .test()
            .assertValue(listOf(countryList[2], countryList[0]))
    }

    private val countryList = listOf(
        CountryDisplayModel(
            name = "Ukraine",
            countryCode = "UKR",
            flag = ""
        ),
        CountryDisplayModel(
            name = "United States",
            countryCode = "US",
            flag = ""
        ),
        CountryDisplayModel(
            name = "United Kingdom",
            countryCode = "GB",
            flag = ""
        ),
        CountryDisplayModel(
            name = "Germany",
            countryCode = "DE",
            flag = ""
        ),
        CountryDisplayModel(
            name = "France",
            countryCode = "FR",
            flag = ""
        )
    )

    private fun <T> T.just(): Observable<T> = Observable.just(this)
}