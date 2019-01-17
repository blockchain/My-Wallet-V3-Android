package piuk.blockchain.android.data.balance.adapters

import com.blockchain.balance.AsyncBalanceReporter
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.Single
import piuk.blockchain.androidcore.data.ethereum.EthDataManager

fun EthDataManager.toAsyncBalanceReporter(): AsyncBalanceReporter =
    EthereumAsyncBalanceReportAdapter(this)

private class EthereumAsyncBalanceReportAdapter(
    private val ethDataManager: EthDataManager
) : AsyncBalanceReporter {

    override fun entireBalance(): Single<CryptoValue> =
        ethDataManager.fetchEthAddress()
            .singleOrError()
            .map {
                CryptoValue(
                    CryptoCurrency.ETHER,
                    it.getTotalBalance()
                )
            }

    private val zero = Single.just(CryptoValue.ZeroEth)

    override fun watchOnlyBalance(): Single<CryptoValue> = zero

    override fun importedAddressBalance(): Single<CryptoValue> = zero

    override fun addressBalance(address: String): Single<CryptoValue> =
        ethDataManager.getBalance(address)
            .map { CryptoValue.etherFromWei(it) }
}
