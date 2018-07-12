package piuk.blockchain.android.data.balance.adapters

import info.blockchain.balance.CryptoValue
import info.blockchain.balance.BalanceReporter
import info.blockchain.wallet.payload.PayloadManager
import piuk.blockchain.androidcore.data.currency.CryptoCurrencies
import java.math.BigInteger

fun PayloadManager.toBalanceReporter(): BalanceReporter = BitcoinBalanceReportAdapter(this)

private class BitcoinBalanceReportAdapter(
    private val payloadManager: PayloadManager
) : BalanceReporter {
    override fun entireBalance() =
        payloadManager.walletBalance.toBalance()

    override fun importedAddressBalance() =
        payloadManager.importedAddressesBalance.toBalance()

    override fun addressBalance(address: String) =
        payloadManager.getAddressBalance(address).toBalance()

    private fun BigInteger.toBalance() =
        CryptoValue(CryptoCurrencies.BTC, this.toLong())
}