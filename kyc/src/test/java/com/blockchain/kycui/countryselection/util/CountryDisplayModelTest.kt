package com.blockchain.kycui.countryselection.util

import org.amshove.kluent.`should equal`
import org.junit.Test

class CountryDisplayModelTest {

    @Test
    fun `correctly abbreviates country`() {
        CountryDisplayModel(
            name = "United Kingdom",
            countryCode = "GB",
            flag = "\uD83C\uDDEC\uD83C\uDDE7"
        ).searchCode `should equal` "UK;GB;United Kingdom"
    }

    @Test
    fun `ignores trailing whitespace`() {
        CountryDisplayModel(
            name = "United Kingdom ",
            countryCode = "GB",
            flag = "\uD83C\uDDEC\uD83C\uDDE7"
        ).searchCode `should equal` "UK;GB;United Kingdom "
    }

    @Test
    fun `ignores leading whitespace`() {
        CountryDisplayModel(
            name = " United Kingdom",
            countryCode = "GB",
            flag = "\uD83C\uDDEC\uD83C\uDDE7"
        ).searchCode `should equal` "UK;GB; United Kingdom"
    }
}