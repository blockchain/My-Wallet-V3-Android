package piuk.blockchain.androidbuysell.models.coinify

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.amshove.kluent.`should be instance of`
import org.amshove.kluent.`should equal to`
import org.amshove.kluent.`should not be`
import org.junit.Test

class CannotTradeReasonAdapterTest {

    private val moshi: Moshi = Moshi.Builder()
        .add(CannotTradeReasonAdapter())
        .build()
    private val type = Types.newParameterizedType(List::class.java, CannotTradeReason::class.java)
    private val adapter: JsonAdapter<List<CannotTradeReason>> = moshi.adapter(type)

    @Test
    fun `test forced delay from json`() {
        // Arrange

        // Act
        val result = adapter.fromJson(FORCED_DELAY_JSON)!!
        // Assert
        val reason = result.first()
        reason `should be instance of` ForcedDelay::class
        reason as ForcedDelay
        reason.delayEnd `should not be` null
        reason.reasonCode `should equal to` "forced_delay"
    }

    @Test
    fun `test forced delay to json`() {
        // Arrange
        val forcedDelay = ForcedDelay(delayEnd = "2016-04-01T12:27:36Z")
        // Act
        val result = adapter.toJson(listOf(forcedDelay))!!
        // Assert
        result `should equal to` FORCED_DELAY_JSON
    }

    @Test
    fun `test trade in progress from json`() {
        // Arrange

        // Act
        val result = adapter.fromJson(TRADE_IN_PROGRESS_JSON)!!
        // Assert
        val reason = result.first()
        reason `should be instance of` TradeInProgress::class
        reason as TradeInProgress
        reason.tradeId `should equal to` 12345
    }

    @Test
    fun `test trade in progress to json`() {
        // Arrange
        val tradeInProgress = TradeInProgress(tradeId = 12345)
        // Act
        val result = adapter.toJson(listOf(tradeInProgress))!!
        // Assert
        result `should equal to` TRADE_IN_PROGRESS_JSON
    }

    @Test
    fun `test limits exceeded from json`() {
        // Arrange

        // Act
        val result = adapter.fromJson(LIMITS_EXCEEDED_JSON)!!
        // Assert
        val reason = result.first()
        reason `should be instance of` LimitsExceeded::class
    }

    @Test
    fun `test limits exceeded to json`() {
        // Arrange
        val tradeInProgress = LimitsExceeded()
        // Act
        val result = adapter.toJson(listOf(tradeInProgress))!!
        // Assert
        result `should equal to` LIMITS_EXCEEDED_JSON
    }

    companion object {
        private const val FORCED_DELAY_JSON =
            "[{\"delayEnd\":\"2016-04-01T12:27:36Z\",\"reasonCode\":\"forced_delay\"}]"
        private const val TRADE_IN_PROGRESS_JSON =
            "[{\"reasonCode\":\"trade_in_progress\",\"tradeId\":12345}]"
        private const val LIMITS_EXCEEDED_JSON = "[{\"reasonCode\":\"limits_exceeded\"}]"
    }
}