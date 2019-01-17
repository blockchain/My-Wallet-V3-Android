package com.blockchain.balance

import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.Maybe
import io.reactivex.Single
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

internal class BtcBalanceAdapter(
    private val payloadDataManager: PayloadDataManager
) : AsyncAddressBalanceReporter,
    AsyncAccountBalanceReporter {

    override fun balanceOf(accountReference: AccountReference): Maybe<CryptoValue> {
        if (accountReference.cryptoCurrency != CryptoCurrency.BTC) {
            return Maybe.empty()
        }
        return getBalance((accountReference as AccountReference.BitcoinLike).xpub).toMaybe()
    }

    override fun getBalance(address: String): Single<CryptoValue> =
        payloadDataManager.getBalanceOfAddresses(listOf(address))
            .map { CryptoValue.bitcoinFromSatoshis(it[address]!!.finalBalance) }
            .singleOrError()
}

internal class BchBalanceAdapter(
    private val bchDataManager: BchDataManager
) : AsyncAddressBalanceReporter,
    AsyncAccountBalanceReporter {

    override fun balanceOf(accountReference: AccountReference): Maybe<CryptoValue> {
        if (accountReference.cryptoCurrency != CryptoCurrency.BCH) {
            return Maybe.empty()
        }
        return getBalance((accountReference as AccountReference.BitcoinLike).xpub).toMaybe()
    }

    override fun getBalance(address: String): Single<CryptoValue> =
        bchDataManager.getBalance(address)
            .map { CryptoValue.bitcoinCashFromSatoshis(it) }
}

internal class EthBalanceAdapter(
    private val ethDataManager: EthDataManager
) : AsyncAddressBalanceReporter,
    AsyncAccountBalanceReporter {

    override fun balanceOf(accountReference: AccountReference): Maybe<CryptoValue> {
        if (accountReference.cryptoCurrency != CryptoCurrency.ETHER) {
            return Maybe.empty()
        }
        return getBalance((accountReference as AccountReference.Ethereum).address).toMaybe()
    }

    override fun getBalance(address: String): Single<CryptoValue> =
        ethDataManager.getBalance(address)
            .map { CryptoValue.etherFromWei(it) }
}
