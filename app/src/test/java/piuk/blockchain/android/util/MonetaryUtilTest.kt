package piuk.blockchain.android.util

import org.amshove.kluent.`should be instance of`
import org.amshove.kluent.`should equal to`
import org.amshove.kluent.`should equal`
import org.junit.Test
import java.math.BigInteger
import java.text.NumberFormat

class MonetaryUtilTest {

    val subject = MonetaryUtil(0)

    @Test
    fun `updateUnit bits`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.MICRO_BTC)
        // Act
        val result = subject.getBtcFormat()
        // Assert
        result.maximumFractionDigits `should equal to` 2
    }

    @Test
    fun `updateUnit mBTC`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.MILLI_BTC)
        // Act
        val result = subject.getBtcFormat()
        // Assert
        result.maximumFractionDigits `should equal to` 5
    }

    @Test
    fun `updateUnit BTC`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.UNIT_BTC)
        // Act
        val result = subject.getBtcFormat()
        // Assert
        result.maximumFractionDigits `should equal to` 8
    }

    @Test(expected = IllegalArgumentException::class)
    fun `updateUnit invalid format`() {
        // Arrange
        subject.updateUnit(7)
        // Act
        subject.getBtcFormat()
        // Assert

    }

    @Test
    fun getBTCFormat() {
        // Arrange

        // Act
        val result = subject.getBtcFormat()
        // Assert
        result `should be instance of` NumberFormat::class.java
    }

    @Test
    fun getFiatFormat() {
        // Arrange
        val currency ="GBP"
        // Act
        val result = subject.getFiatFormat(currency)
        // Assert
        result.currency.currencyCode `should equal to` currency
    }

    @Test
    fun getBTCUnits() {
        // Arrange

        // Act
        val result = subject.getBtcUnits()
        // Assert
        result.size `should equal to` 3
        result `should equal` arrayOf("BTC", "mBTC", "bits")
    }

    @Test
    fun getBtcUnit() {
        // Arrange

        // Act
        val result = subject.getBtcUnit(MonetaryUtil.MICRO_BTC)
        // Assert
        result `should equal to` "bits"
    }

    @Test
    fun `getDisplayAmount bits`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.MICRO_BTC)
        // Act
        val result = subject.getDisplayAmount(10_000L)
        // Assert
        result `should equal to` "100.0"
    }

    @Test
    fun `getDisplayAmount mBTC`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.MILLI_BTC)
        // Act
        val result = subject.getDisplayAmount(10_000L)
        // Assert
        result `should equal to` "0.1"
    }

    @Test
    fun `getDisplayAmount BTC`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.UNIT_BTC)
        // Act
        val result = subject.getDisplayAmount(10_000L)
        // Assert
        result `should equal to` "0.0001"
    }

    @Test
    fun `getUndenominatedAmount long bits`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.MICRO_BTC)
        // Act
        val result = subject.getUndenominatedAmount(1_000_000L)
        // Assert
        result `should equal` BigInteger.valueOf(1)
    }

    @Test
    fun `getUndenominatedAmount long mBTC`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.MILLI_BTC)
        // Act
        val result = subject.getUndenominatedAmount(1_000_000L)
        // Assert
        result `should equal` BigInteger.valueOf(1000)
    }

    @Test
    fun `getUndenominatedAmount long BTC`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.UNIT_BTC)
        // Act
        val result = subject.getUndenominatedAmount(1_000_000L)
        // Assert
        result `should equal` BigInteger.valueOf(1_000_000)
    }

    @Test
    fun `getUndenominatedAmount double bits`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.MICRO_BTC)
        // Act
        val result = subject.getUndenominatedAmount(1_000_000.0)
        // Assert
        result `should equal` 1.0
    }

    @Test
    fun `getUndenominatedAmount double mBTC`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.MILLI_BTC)
        // Act
        val result = subject.getUndenominatedAmount(1_000_000.0)
        // Assert
        result `should equal` 1000.0
    }

    @Test
    fun `getUndenominatedAmount double BTC`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.UNIT_BTC)
        // Act
        val result = subject.getUndenominatedAmount(1_000_000.0)
        // Assert
        result `should equal` 1_000_000.0
    }

    @Test
    fun `getDenominatedAmount bits`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.MICRO_BTC)
        // Act
        val result = subject.getDenominatedAmount(1.0)
        // Assert
        result `should equal` 1_000_000.0
    }

    @Test
    fun `getDenominatedAmount mBTC`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.MILLI_BTC)
        // Act
        val result = subject.getDenominatedAmount(1.0)
        // Assert
        result `should equal` 1_000.0
    }

    @Test
    fun `getDenominatedAmount BTC`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.UNIT_BTC)
        // Act
        val result = subject.getDenominatedAmount(1.0)
        // Assert
        result `should equal` 1.0
    }

    @Test
    fun `getDisplayAmountWithFormatting long bits`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.MICRO_BTC)
        // Act
        val result = subject.getDisplayAmountWithFormatting(10_000_000_000L)
        // Assert
        result `should equal to` "100,000,000.0"
    }

    @Test
    fun `getDisplayAmountWithFormatting long mBTC`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.MILLI_BTC)
        // Act
        val result = subject.getDisplayAmountWithFormatting(10_000_000_000L)
        // Assert
        result `should equal to` "100,000.0"
    }

    @Test
    fun `getDisplayAmountWithFormatting long BTC`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.UNIT_BTC)
        // Act
        val result = subject.getDisplayAmountWithFormatting(10_000_000_000L)
        // Assert
        result `should equal to` "100.0"
    }

    @Test
    fun `getDisplayAmountWithFormatting double bits`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.MICRO_BTC)
        // Act
        val result = subject.getDisplayAmountWithFormatting(10_000_000_000.0)
        // Assert
        result `should equal` "100,000,000.0"
    }

    @Test
    fun `getDisplayAmountWithFormatting double mBTC`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.MILLI_BTC)
        // Act
        val result = subject.getDisplayAmountWithFormatting(10_000_000_000.0)
        // Assert
        result `should equal` "100,000.0"
    }

    @Test
    fun `getDisplayAmountWithFormatting double BTC`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.UNIT_BTC)
        // Act
        val result = subject.getDisplayAmountWithFormatting(10_000_000_000.0)
        // Assert
        result `should equal` "100.0"
    }

}