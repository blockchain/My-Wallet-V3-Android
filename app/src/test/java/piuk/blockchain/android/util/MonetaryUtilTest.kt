// package piuk.blockchain.android.util
//
// import org.amshove.kluent.`should be instance of`
// import org.amshove.kluent.`should equal to`
// import org.amshove.kluent.`should equal`
// import org.junit.Test
// import java.text.NumberFormat
// import java.util.*
//
// class MonetaryUtilTest {
//
//    val subject = MonetaryUtil()
//
//    @Test
//    fun getBTCFormat() {
//        // Arrange
//
//        // Act
//        val result = subject.getBtcFormat()
//        // Assert
//        result `should be instance of` NumberFormat::class.java
//    }
//
//    @Test
//    fun getFiatFormat() {
//        // Arrange
//        val currency = "GBP"
//        // Act
//        val result = subject.getFiatFormat(currency)
//        // Assert
//        result.currency.currencyCode `should equal to` currency
//    }
//
//    @Test
//    fun `getDisplayAmount BTC`() {
//        // Arrange
//        // Act
//        val result = subject.getDisplayAmount(10_000L)
//        // Assert
//        result `should equal to` "0.0001"
//    }
//
//
//    @Test
//    fun `getDisplayAmountWithFormatting long BTC`() {
//        // Arrange
//        // Act
//        val result = subject.getDisplayAmountWithFormatting(10_000_000_000L)
//        // Assert
//        result `should equal to` "100.0"
//    }
//
//    @Test
//    fun `getDisplayAmountWithFormatting double BTC`() {
//        // Arrange
//        // Act
//        val result = subject.getDisplayAmountWithFormatting(10_000_000_000.0)
//        // Assert
//        result `should equal` "100.0"
//    }
//
//    @Test
//    fun `getFiatDisplayString GBP in UK`() {
//        // Arrange
//
//        // Act
//        val result = subject.getFiatDisplayString(1.2345, "GBP", Locale.UK)
//        // Assert
//        result `should equal` "£1.23"
//    }
//
//    @Test
//    fun `getCurrencySymbol GBP in UK`() {
//        // Arrange
//
//        // Act
//        val result = subject.getCurrencySymbol("GBP", Locale.UK)
//        // Assert
//        result `should equal` "£"
//    }
//
// }