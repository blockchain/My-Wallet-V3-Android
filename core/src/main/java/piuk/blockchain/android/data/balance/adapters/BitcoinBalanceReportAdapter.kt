package piuk.blockchain.android.data.balance.adapters

import com.blockchain.balance.AsyncBalanceReporter
import info.blockchain.balance.BalanceReporter
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.payload.PayloadManager
import io.reactivex.Single
import java.math.BigInteger

fun PayloadManager.toBalanceReporter(): BalanceReporter = BitcoinBalanceReportAdapter(this)

fun BalanceReporter.toAsync(): AsyncBalanceReporter =
    object : AsyncBalanceReporter {

        override fun entireBalance() =
            Single.just(this@toAsync.entireBalance())

        override fun watchOnlyBalance() =
            Single.just(this@toAsync.watchOnlyBalance())

        override fun importedAddressBalance() =
            Single.just(this@toAsync.importedAddressBalance())

        override fun addressBalance(address: String) =
            Single.just(this@toAsync.addressBalance(address))
    }

private class BitcoinBalanceReportAdapter(
    private val payloadManager: PayloadManager
) : BalanceReporter {
    override fun entireBalance() =
        payloadManager.walletBalance.toBalance()

    override fun watchOnlyBalance() =
        payloadManager.walletWatchOnlyBalance.toBalance()

    override fun importedAddressBalance() =
        payloadManager.importedAddressesBalance.toBalance()

    override fun addressBalance(address: String) =
        payloadManager.getAddressBalance(address).toBalance()

    private fun BigInteger.toBalance() =
        CryptoValue(CryptoCurrency.BTC, this)
}