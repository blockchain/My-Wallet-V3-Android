package info.blockchain.wallet.payload

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import java.math.BigInteger

data class CryptoBalanceMap(
    private val cryptoCurrency: CryptoCurrency,
    private val xpubs: Set<String>,
    private val legacy: Set<String>,
    private val watchOnlyLegacy: Set<String>,
    private val balances: Map<String, BigInteger>
) {
    val totalSpendable = CryptoValue(cryptoCurrency, (xpubs + legacy - watchOnlyLegacy).sum(balances))
    val totalSpendableLegacy = CryptoValue(cryptoCurrency, (legacy - watchOnlyLegacy).sum(balances))
    val totalWatchOnly = CryptoValue(cryptoCurrency, watchOnlyLegacy.sum(balances))

    fun subtractAmountFromAddress(address: String, cryptoValue: CryptoValue): CryptoBalanceMap {
        val value =
            balances[address] ?: throw Exception("No info for this address. updateAllBalances should be called first.")
        val newBalances = balances.toMutableMap()
            .apply {
                set(address, value - cryptoValue.amount)
            }
        return copy(balances = newBalances)
    }

    operator fun get(address: String) =
        CryptoValue(cryptoCurrency, balances[address] ?: BigInteger.ZERO)

    companion object {
        @JvmStatic
        fun zero(cryptoCurrency: CryptoCurrency) =
            CryptoBalanceMap(
                cryptoCurrency,
                emptySet(),
                emptySet(),
                emptySet(),
                emptyMap()
            )
    }
}

fun calculateCryptoBalanceMap(
    cryptoCurrency: CryptoCurrency,
    balanceQuery: BalanceQuery,
    xpubs: Set<String>,
    legacy: Set<String>,
    watchOnlyLegacy: Set<String>
): CryptoBalanceMap {

    return CryptoBalanceMap(
        cryptoCurrency,
        xpubs,
        legacy,
        watchOnlyLegacy,
        balanceQuery.getBalancesFor(xpubs + legacy + watchOnlyLegacy)
    )
}

private fun <T> Iterable<T>.sum(balances: Map<T, BigInteger>) =
    map { balances[it] ?: BigInteger.ZERO }
        .foldRight(BigInteger.ZERO, BigInteger::add)
