package info.blockchain.wallet.payload

import java.math.BigInteger

interface BalanceQuery {
    fun getBalancesFor(addressesAndXpubs: Set<String>): Map<String, BigInteger>
}