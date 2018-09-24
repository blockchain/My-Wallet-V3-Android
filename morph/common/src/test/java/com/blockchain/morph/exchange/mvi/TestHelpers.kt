package com.blockchain.morph.exchange.mvi

import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal

internal fun Given.on(vararg intent: ExchangeIntent, assert: TestObserver<ExchangeViewModel>.() -> Unit) =
    on(intent.toList(), assert)

internal fun Given.onLastStateAfter(vararg intent: ExchangeIntent, assert: ExchangeViewState.() -> Unit) =
    onLastStateAfter(intent.toList(), assert)

internal fun Given.on(intent: List<ExchangeIntent>, assert: TestObserver<ExchangeViewModel>.() -> Unit) {
    val subject = PublishSubject.create<ExchangeIntent>()
    val testObserver = ExchangeDialog(subject, this.initial)
        .viewModels
        .skip(intent.size.toLong())
        .test()
    intent.forEach(subject::onNext)
    assert(testObserver)
}

internal fun Given.onLastStateAfter(intent: List<ExchangeIntent>, assert: ExchangeViewState.() -> Unit) {
    val subject = PublishSubject.create<ExchangeIntent>()
    val testObserver = ExchangeDialog(subject, this.initial)
        .viewStates
        .skip(intent.size.toLong())
        .test()
    intent.forEach(subject::onNext)
    testObserver.assertValue { assert(it); true }
}

fun given(initial: ExchangeViewModel) =
    Given(initial)

class Given(val initial: ExchangeViewModel)

fun initial(fiatCode: String, pair: Pair<CryptoCurrency, CryptoCurrency> = CryptoCurrency.BTC to CryptoCurrency.ETHER) =
    initial(fiatCode, fakeAccountReference(pair.first), fakeAccountReference(pair.second))

fun fakeAccountReference(cryptoCurrency: CryptoCurrency): AccountReference {
    return when (cryptoCurrency) {
        CryptoCurrency.BTC, CryptoCurrency.BCH -> AccountReference.BitcoinLike(cryptoCurrency, "", "")
        CryptoCurrency.ETHER -> AccountReference.Ethereum("", "")
    }
}

fun zeroFiat(currencyCode: String) = FiatValue.fromMajor(currencyCode, BigDecimal.ZERO)

fun outOfDate(fiat: FiatValue) = fiat to Value.Mode.OutOfDate
fun outOfDate(cryptoValue: CryptoValue) = cryptoValue to Value.Mode.OutOfDate

fun userEntered(cryptoValue: CryptoValue) =
    cryptoValue to Value.Mode.UserEntered

fun userEntered(fiat: FiatValue) =
    fiat to Value.Mode.UserEntered

fun upToDate(cryptoValue: CryptoValue) =
    cryptoValue to Value.Mode.UpToDate

fun upToDate(fiat: FiatValue) =
    fiat to Value.Mode.UpToDate

fun value(
    crypto: Pair<CryptoValue, Value.Mode>,
    fiat: Pair<FiatValue, Value.Mode>
): Value {
    return Value(
        crypto.first,
        fiat.first,
        cryptoMode = crypto.second,
        fiatMode = fiat.second
    )
}
