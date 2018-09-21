package com.blockchain.morph.ui.homebrew.exchange

import com.blockchain.accounts.AccountList
import com.blockchain.accounts.AllAccountList
import com.blockchain.morph.exchange.mvi.Fix
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.junit.Test
import java.math.BigDecimal

class ExchangeFragmentConfigurationChangePersistenceTest {

    @Test
    fun `initial values`() {
        ExchangeFragmentConfigurationChangePersistence(allAccountList())
            .apply {
                fieldMode `should be` Fix.BASE_FIAT
                from `should equal` anyAccountReference(CryptoCurrency.BTC)
                to `should equal` anyAccountReference(CryptoCurrency.ETHER)
                currentValue `should equal` BigDecimal.ZERO
            }
    }

    @Test
    fun `can set "from" and it doesn't affect "to"`() {
        ExchangeFragmentConfigurationChangePersistence(allAccountList())
            .apply {
                val accountReference = anyAccountReference(CryptoCurrency.BCH)
                fromReference = accountReference
                from `should be` accountReference
                to `should equal` anyAccountReference(CryptoCurrency.ETHER)
            }
    }

    @Test
    fun `can set "to" and it doesn't affect "from"`() {
        ExchangeFragmentConfigurationChangePersistence(allAccountList())
            .apply {
                val accountReference = anyAccountReference(CryptoCurrency.BCH)
                toReference = accountReference
                to `should be` accountReference
                from `should equal` anyAccountReference(CryptoCurrency.BTC)
            }
    }

    @Test
    fun `setting the "from" clears the current value`() {
        ExchangeFragmentConfigurationChangePersistence(allAccountList())
            .apply {
                currentValue = 1000L.toBigDecimal()
                fromReference = anyAccountReference(CryptoCurrency.BCH)
                currentValue `should equal` BigDecimal.ZERO
            }
    }

    @Test
    fun `setting the "to" clears the current value`() {
        ExchangeFragmentConfigurationChangePersistence(allAccountList())
            .apply {
                currentValue = 1000L.toBigDecimal()
                toReference = anyAccountReference(CryptoCurrency.BCH)
                currentValue `should equal` BigDecimal.ZERO
            }
    }

    @Test
    fun `setting the "from" to what it is already does not clear the current value`() {
        ExchangeFragmentConfigurationChangePersistence(allAccountList())
            .apply {
                fromReference = anyAccountReference(CryptoCurrency.BCH)
                currentValue = 1000L.toBigDecimal()
                fromReference = anyAccountReference(CryptoCurrency.BCH)
                currentValue `should equal` 1000L.toBigDecimal()
            }
    }

    @Test
    fun `setting the "to" to what it is already does not clear the current value`() {
        ExchangeFragmentConfigurationChangePersistence(allAccountList())
            .apply {
                toReference = anyAccountReference(CryptoCurrency.BCH)
                currentValue = 1000L.toBigDecimal()
                toReference = anyAccountReference(CryptoCurrency.BCH)
                currentValue `should equal` 1000L.toBigDecimal()
            }
    }

    private fun allAccountList(): AllAccountList =
        object : AllAccountList {
            override fun get(cryptoCurrency: CryptoCurrency): AccountList =
                object : AccountList {
                    override fun defaultAccountReference(): AccountReference {
                        return anyAccountReference(cryptoCurrency)
                    }
                }
        }
}

fun anyAccountReference(cryptoCurrency: CryptoCurrency): AccountReference {
    return when (cryptoCurrency) {
        CryptoCurrency.BTC, CryptoCurrency.BCH -> AccountReference.BitcoinLike(cryptoCurrency, "", "")
        CryptoCurrency.ETHER -> AccountReference.Ethereum("", "")
    }
}
