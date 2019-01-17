package com.blockchain.balance

import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.bitcoinCash
import com.blockchain.testutils.ether
import com.nhaarman.mockito_kotlin.KStubbing
import com.nhaarman.mockito_kotlin.mock
import info.blockchain.api.data.Balance
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.amshove.kluent.`it returns`
import org.junit.Test
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

class BtcBalanceAdapterTest {

    @Test
    fun `get btc balance`() {
        val address = "address"
        BtcBalanceAdapter(
            mock {
                givenBalance(address, 1.bitcoin())
            }
        ).getBalance(address)
            .test() `should just be single value` 1.bitcoin()
    }

    @Test
    fun `get btc balance by account reference`() {
        val address = "address2"
        BtcBalanceAdapter(
            mock {
                givenBalance(address, 2.bitcoin())
            }
        ).balanceOf(
            AccountReference.BitcoinLike(CryptoCurrency.BTC, "", address)
        ).test() `should just be single value` 2.bitcoin()
    }

    @Test
    fun `get btc balance by account reference, non-BTC should have no entry`() {
        val address = "address3"
        BtcBalanceAdapter(
            mock {
                givenBalance(address, 2.bitcoin())
            }
        ).balanceOf(
            AccountReference.BitcoinLike(CryptoCurrency.BCH, "", address)
        ).test()
            .shouldBeEmpty()
    }

    @Test
    fun `get btc balance by account reference, non-bitcoin-like should have no entry`() {
        val address = "address3"
        BtcBalanceAdapter(
            mock {
                givenBalance(address, 2.bitcoin())
            }
        ).balanceOf(
            AccountReference.Ethereum("", address)
        ).test()
            .shouldBeEmpty()
    }

    private fun KStubbing<PayloadDataManager>.givenBalance(
        address: String,
        cryptoValue: CryptoValue
    ) {
        on { getBalanceOfAddresses(listOf(address)) } `it returns` Observable.just(
            LinkedHashMap<String, Balance>().apply {
                put(address, Balance().apply { finalBalance = cryptoValue.amount })
            }
        )
    }
}

class BchBalanceAdapterTest {

    @Test
    fun `get bch balance`() {
        val address = "address"
        BchBalanceAdapter(
            mock {
                givenBalance(address, 1.bitcoinCash())
            }
        ).getBalance(address)
            .test() `should just be single value` 1.bitcoinCash()
    }

    @Test
    fun `get bch balance via AccountReference`() {
        val address = "address2"
        BchBalanceAdapter(
            mock {
                givenBalance(address, 2.bitcoinCash())
            }
        ).balanceOf(AccountReference.BitcoinLike(CryptoCurrency.BCH, "", address))
            .test() `should just be single value` 2.bitcoinCash()
    }

    @Test
    fun `get bch balance via AccountReference, non-BCH should have no entry`() {
        val address = "address2"
        BchBalanceAdapter(
            mock {
                givenBalance(address, 2.bitcoinCash())
            }
        ).balanceOf(AccountReference.BitcoinLike(CryptoCurrency.BTC, "", address))
            .test()
            .shouldBeEmpty()
    }

    @Test
    fun `get bch balance via AccountReference, non-bitcoin-like should have no entry`() {
        val address = "address3"
        BchBalanceAdapter(
            mock {
                givenBalance(address, 2.bitcoinCash())
            }
        ).balanceOf(AccountReference.Ethereum("", address))
            .test()
            .shouldBeEmpty()
    }

    private fun KStubbing<BchDataManager>.givenBalance(
        address: String,
        cryptoValue: CryptoValue
    ) {
        on { getBalance(address) } `it returns` Single.just(cryptoValue.amount)
    }
}

class EthBalanceAdapterTest {

    @Test
    fun `get eth balance`() {
        val address = "address"
        EthBalanceAdapter(
            mock {
                givenBalance(address, 1.ether())
            }
        ).getBalance(address)
            .test() `should just be single value` 1.ether()
    }

    @Test
    fun `get eth balance via account reference`() {
        val address = "address2"
        EthBalanceAdapter(
            mock {
                givenBalance(address, 2.ether())
            }
        ).balanceOf(AccountReference.Ethereum("", address))
            .test() `should just be single value` 2.ether()
    }

    @Test
    fun `get eth balance via account reference, non-Ether should have no entry`() {
        val address = "address3"
        EthBalanceAdapter(
            mock {
                givenBalance(address, 2.ether())
            }
        ).balanceOf(AccountReference.BitcoinLike(CryptoCurrency.BTC, "", address))
            .test().shouldBeEmpty()
    }

    private fun KStubbing<EthDataManager>.givenBalance(
        address: String,
        cryptoValue: CryptoValue
    ) {
        on { getBalance(address) } `it returns` Single.just(cryptoValue.amount)
    }
}

private infix fun <T> TestObserver<T>.`should just be single value`(expected: T) =
    assertComplete()
        .assertNoErrors()
        .assertValue(expected)

private fun <T> TestObserver<T>.shouldBeEmpty() =
    assertComplete()
        .assertNoErrors()
        .assertValueCount(0)
