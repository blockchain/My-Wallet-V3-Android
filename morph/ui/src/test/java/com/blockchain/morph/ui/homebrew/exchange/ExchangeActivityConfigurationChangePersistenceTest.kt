package com.blockchain.morph.ui.homebrew.exchange

import com.blockchain.morph.exchange.mvi.FieldUpdateIntent
import info.blockchain.balance.CryptoCurrency
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.junit.Test
import java.math.BigDecimal

class ExchangeActivityConfigurationChangePersistenceTest {

    @Test
    fun `initial values`() {
        ExchangeActivityConfigurationChangePersistence()
            .apply {
                fieldMode `should be` FieldUpdateIntent.Field.FROM_FIAT
                from `should be` CryptoCurrency.BTC
                to `should be` CryptoCurrency.ETHER
                currentValue `should equal` BigDecimal.ZERO
            }
    }

    @Test
    fun `can set "from" and it doesn't affect "to"`() {
        ExchangeActivityConfigurationChangePersistence()
            .apply {
                from = CryptoCurrency.BCH
                from `should be` CryptoCurrency.BCH
                to `should be` CryptoCurrency.ETHER
            }
    }

    @Test
    fun `can set "to" and it doesn't affect "from"`() {
        ExchangeActivityConfigurationChangePersistence()
            .apply {
                to = CryptoCurrency.BCH
                from `should be` CryptoCurrency.BTC
                to `should be` CryptoCurrency.BCH
            }
    }

    @Test
    fun `if "from" matches the "to", then they swap`() {
        ExchangeActivityConfigurationChangePersistence()
            .apply {
                from = CryptoCurrency.ETHER
                from `should be` CryptoCurrency.ETHER
                to `should be` CryptoCurrency.BTC
            }
    }

    @Test
    fun `if "to" matches the "from", then they swap`() {
        ExchangeActivityConfigurationChangePersistence()
            .apply {
                to = CryptoCurrency.BTC
                from `should be` CryptoCurrency.ETHER
                to `should be` CryptoCurrency.BTC
            }
    }

    @Test
    fun `setting the "from" clears the current value`() {
        ExchangeActivityConfigurationChangePersistence()
            .apply {
                currentValue = 1000L.toBigDecimal()
                from = CryptoCurrency.BCH
                currentValue `should equal` BigDecimal.ZERO
            }
    }

    @Test
    fun `setting the "to" clears the current value`() {
        ExchangeActivityConfigurationChangePersistence()
            .apply {
                currentValue = 1000L.toBigDecimal()
                to = CryptoCurrency.BCH
                currentValue `should equal` BigDecimal.ZERO
            }
    }

    @Test
    fun `setting the "from" to what it is already does not clear the current value`() {
        ExchangeActivityConfigurationChangePersistence()
            .apply {
                from = CryptoCurrency.BCH
                currentValue = 1000L.toBigDecimal()
                from = CryptoCurrency.BCH
                currentValue `should equal` 1000L.toBigDecimal()
            }
    }

    @Test
    fun `setting the "to" to what it is already does not clear the current value`() {
        ExchangeActivityConfigurationChangePersistence()
            .apply {
                to = CryptoCurrency.BCH
                currentValue = 1000L.toBigDecimal()
                to = CryptoCurrency.BCH
                currentValue `should equal` 1000L.toBigDecimal()
            }
    }
}
