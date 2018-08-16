package com.blockchain.kycui.search

import com.blockchain.kycui.countryselection.util.CountryDisplayModel
import io.reactivex.Observable
import org.amshove.kluent.`should equal`
import org.junit.Test

class CountrySelectionFilterTest {

    @Test
    fun empty() {
        val querySubject = Observable.empty<CharSequence>()
        val countries = Observable.just(emptyList<CountryDisplayModel>())
        countries.filterCountries(querySubject)
            .test()
            .values() `should equal` listOf(emptyList())
    }

    @Test
    fun `without user input, whole list is shown`() {
        val querySubject = Observable.empty<CharSequence>()
        val countries = Observable.just(
            listOf(
                country("UK"),
                country("US")
            )
        )
        countries.filterCountries(querySubject)
            .test()
            .values() `should equal` listOf(
            listOf(
                country("UK"),
                country("US")
            )
        )
    }

    @Test
    fun `search for country, full text, correct case`() {
        val querySubject = Observable.just<CharSequence>(
            "United Kingdom"
        )
        val countries = Observable.just(
            listOf(
                country("United Kingdom"),
                country("United States"),
                country("Canada")
            )
        )
        countries.filterCountries(querySubject)
            .test()
            .values() `should equal` listOf(
            listOf(
                country("United Kingdom"),
                country("United States"),
                country("Canada")
            ),
            listOf(
                country("United Kingdom")
            )
        )
    }

    @Test
    fun `search for country, partial country name, wrong case`() {
        val querySubject = Observable.just<CharSequence>(
            "united"
        )
        val countries = Observable.just(
            listOf(
                country("United Kingdom"),
                country("United States"),
                country("Canada")
            )
        )
        countries.filterCountries(querySubject)
            .test()
            .values() `should equal` listOf(
            listOf(
                country("United Kingdom"),
                country("United States"),
                country("Canada")
            ),
            listOf(
                country("United Kingdom"),
                country("United States")
            )
        )
    }

    @Test
    fun `search for country, partial country name, middle of name`() {
        val querySubject = Observable.just<CharSequence>(
            "ana"
        )
        val countries = Observable.just(
            listOf(
                country("United Kingdom"),
                country("United States"),
                country("Canada")
            )
        )
        countries.filterCountries(querySubject)
            .test()
            .values().last() `should equal`
            listOf(
                country("Canada")
            )
    }

    @Test
    fun `search for country, full country code`() {
        val querySubject = Observable.just<CharSequence>(
            "UK"
        )
        val countries = Observable.just(
            listOf(
                country("United Kingdom", "UK"),
                country("United States", "US"),
                country("Canada", "CA")
            )
        )
        countries.filterCountries(querySubject)
            .test()
            .values().last() `should equal`
            listOf(
                country("United Kingdom", "UK")
            )
    }

    @Test
    fun `search for country, full country code, wrong case`() {
        val querySubject = Observable.just<CharSequence>(
            "us"
        )
        val countries = Observable.just(
            listOf(
                country("United Kingdom", "UK"),
                country("United States", "US"),
                country("Canada", "CA")
            )
        )
        countries.filterCountries(querySubject)
            .test()
            .values().last() `should equal`
            listOf(
                country("United States", "US")
            )
    }

    @Test
    fun `search for country, partial country code not at start`() {
        val querySubject = Observable.just<CharSequence>(
            "a"
        )
        val countries = Observable.just(
            listOf(
                country("", "UK"),
                country("", "US"),
                country("", "CA")
            )
        )
        countries.filterCountries(querySubject)
            .test()
            .values().last() `should equal`
            listOf(
                country("", "CA")
            )
    }

    @Test
    fun `incremental search results`() {
        val querySubject = Observable.just<CharSequence>(
            "U",
            "United",
            "United K"
        )
        val countries = Observable.just(
            listOf(
                country("United Kingdom"),
                country("United States"),
                country("Canada")
            )
        )
        countries.filterCountries(querySubject)
            .test()
            .values() `should equal` listOf(
            listOf(
                country("United Kingdom"),
                country("United States"),
                country("Canada")
            ),
            listOf(
                country("United Kingdom"),
                country("United States")
            ),
            listOf(
                country("United Kingdom"),
                country("United States")
            ),
            listOf(
                country("United Kingdom")
            )
        )
    }

    private fun country(country: String, countryCode: String = "") =
        CountryDisplayModel(name = country, countryCode = countryCode, flag = "")
}
