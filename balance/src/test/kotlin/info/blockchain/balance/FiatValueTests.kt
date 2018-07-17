package info.blockchain.balance

import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.amshove.kluent.`should not equal`
import org.amshove.kluent.`should throw the Exception`
import org.amshove.kluent.`with message`
import org.junit.Test
import java.util.Locale

class FiatValueTests {

    @Test
    fun `can create and read properties`() {
        FiatValue("USD", 123.45.toBigDecimal())
            .apply {
                currencyCode `should equal` "USD"
                value `should equal` 123.45.toBigDecimal()
            }
    }

    @Test
    fun `can create and read alternative properties`() {
        FiatValue("GBP", 99.99.toBigDecimal())
            .apply {
                currencyCode `should equal` "GBP"
                value `should equal` 99.99.toBigDecimal()
            }
    }

    @Test
    fun `can equate`() {
        FiatValue("CAD", 123.toBigDecimal()) `should equal` FiatValue("CAD", 123.toBigDecimal())
    }

    @Test
    fun `can not equate by symbol`() {
        FiatValue("CAD", 123.toBigDecimal()) `should not equal` FiatValue("USD", 123.toBigDecimal())
    }

    @Test
    fun `can not equate by value`() {
        FiatValue("CAD", 123.toBigDecimal()) `should not equal` FiatValue("CAD", 456.toBigDecimal())
    }

    @Test
    fun `can format GBP in UK`() {
        FiatValue("GBP", 99.99.toBigDecimal())
            .toStringWithSymbol(Locale.UK) `should equal` "£99.99"
    }

    @Test
    fun `can format GBP in UK - trailing digits`() {
        FiatValue("GBP", 99.toBigDecimal())
            .toStringWithSymbol(Locale.UK) `should equal` "£99.00"
    }

    @Test
    fun `can format GBP in UK - maximum 2 digits`() {
        FiatValue("GBP", 99.123.toBigDecimal())
            .toStringWithSymbol(Locale.UK) `should equal` "£99.12"
    }

    @Test
    fun `can format USD in US`() {
        FiatValue("USD", 1.23.toBigDecimal())
            .toStringWithSymbol(Locale.US) `should equal` "$1.23"
    }

    @Test
    fun `can format USD in Canada`() {
        FiatValue("USD", 3.45.toBigDecimal())
            .toStringWithSymbol(Locale.CANADA) `should equal` "US$3.45"
    }

    @Test
    fun `can format CAD in US`() {
        FiatValue("CAD", 4.56.toBigDecimal())
            .toStringWithSymbol(Locale.US) `should equal` "CAD4.56"
    }

    @Test
    fun `can format CAD in Canada`() {
        FiatValue("CAD", 6.78.toBigDecimal())
            .toStringWithSymbol(Locale.CANADA) `should equal` "$6.78"
    }

    @Test
    fun `can format YEN in US`() {
        FiatValue("JPY", 456.toBigDecimal())
            .toStringWithSymbol(Locale.US) `should equal` "JPY456"
    }

    @Test
    fun `can format YEN in Japan`() {
        FiatValue("JPY", 678.toBigDecimal())
            .toStringWithSymbol(Locale.JAPAN) `should equal` "￥678"
    }

    @Test
    fun `can format YEN in Japan - maximum 0 digits`() {
        FiatValue("JPY", 99.123.toBigDecimal())
            .toStringWithSymbol(Locale.JAPAN) `should equal` "￥99"
    }

    @Test
    fun `can format GBP without symbol`() {
        FiatValue("GBP", 1.1.toBigDecimal())
            .toStringWithoutSymbol(Locale.UK) `should equal` "1.10"
    }

    @Test
    fun `can format YEN without symbol`() {
        FiatValue("JPY", 678.toBigDecimal())
            .toStringWithoutSymbol(Locale.JAPAN) `should equal` "678"
    }

    @Test
    fun `isZero`() {
        FiatValue("GBP", 0.toBigDecimal()).isZero `should be` true
    }

    @Test
    fun `isZero with decimal places`() {
        FiatValue("GBP", 0.0.toBigDecimal()).isZero `should be` true
    }

    @Test
    fun `isZero negative`() {
        FiatValue("GBP", (-1).toBigDecimal()).isZero `should be` false
    }

    @Test
    fun `isZero positive`() {
        FiatValue("GBP", 0.1.toBigDecimal()).isZero `should be` false
    }

    @Test
    fun `can add`() {
        FiatValue("GBP", 1.2.toBigDecimal()) +
            FiatValue("GBP", 2.3.toBigDecimal()) `should equal`
            FiatValue("GBP", 3.5.toBigDecimal())
    }

    @Test
    fun `can add with alternative currency and values`() {
        FiatValue("USD", 10.toBigDecimal()) +
            FiatValue("USD", 20.toBigDecimal()) `should equal`
            FiatValue("USD", 30.toBigDecimal())
    }

    @Test
    fun `can't add if the currency codes don't match`() {
        {
            FiatValue("GBP", 1.2.toBigDecimal()) +
                FiatValue("USD", 2.3.toBigDecimal())
        } `should throw the Exception` MismatchedCurrencyCodeException::class `with message`
            "Mismatched currency codes during add"
    }
}
