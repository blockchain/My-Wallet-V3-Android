package com.blockchain.kycui.countryselection.util

import org.amshove.kluent.`should equal`
import org.junit.Test

class AcronymTest {

    @Test
    fun `correctly abbreviates country`() {
        "United Kingdom".acronym() `should equal` "UK"
    }

    @Test
    fun `ignores trailing whitespace`() {
        "United Kingdom ".acronym() `should equal` "UK"
    }

    @Test
    fun `ignores leading whitespace`() {
        " United Kingdom".acronym() `should equal` "UK"
    }

    @Test
    fun `ignores extra middle whitespace`() {
        "United  Kingdom".acronym() `should equal` "UK"
    }

    @Test
    fun `already an acronym`() {
        "UK".acronym() `should equal` "UK"
    }
}