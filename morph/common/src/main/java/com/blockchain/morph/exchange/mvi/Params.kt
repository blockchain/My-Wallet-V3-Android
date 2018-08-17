package com.blockchain.morph.exchange.mvi

import info.blockchain.balance.CryptoCurrency
import io.reactivex.Observable

sealed class ParamChangeIntent {
    class ChangeFromCrypto(val newFromCryptoCurrency: CryptoCurrency) : ParamChangeIntent()
    class ChangeToCrypto(val newToCryptoCurrency: CryptoCurrency) : ParamChangeIntent()
    class ChangeFiat(val newFiatSymbol: String) : ParamChangeIntent()
}

data class Params(
    val from: CryptoCurrency,
    val to: CryptoCurrency,
    val fiat: String
)

/**
 * MVI dialog that streams [ParamChangeIntent] to a stream of [Params] states
 */
fun Observable<ParamChangeIntent>.paramsDialog(initial: Params): Observable<Params> =
    this.scan(initial) { current, intent ->
        when (intent) {
            is ParamChangeIntent.ChangeFromCrypto -> current.copy(from = intent.newFromCryptoCurrency)
            is ParamChangeIntent.ChangeToCrypto -> current.copy(to = intent.newToCryptoCurrency)
            is ParamChangeIntent.ChangeFiat -> current.copy(fiat = intent.newFiatSymbol)
        }
    }
