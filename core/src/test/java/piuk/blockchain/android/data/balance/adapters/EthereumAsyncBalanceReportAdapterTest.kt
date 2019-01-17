package piuk.blockchain.android.data.balance.adapters

import com.blockchain.balance.AsyncBalanceReporter
import com.blockchain.testutils.ether
import com.nhaarman.mockito_kotlin.mock
import info.blockchain.balance.CryptoValue
import io.reactivex.Observable
import io.reactivex.Single
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should equal`
import org.amshove.kluent.any
import org.junit.Test
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.ethereum.models.CombinedEthModel

class EthereumAsyncBalanceReportAdapterTest {

    private val etherModel: CombinedEthModel = mock {
        on { getTotalBalance() } `it returns` 100.ether().amount
    }
    private val ethDataManager: EthDataManager = mock {
        on { fetchEthAddress() } `it returns` Observable.just(etherModel)
        on { getBalance(any()) } `it returns` Single.just(1.ether().amount)
    }
    private val balanceReporter: AsyncBalanceReporter = ethDataManager.toAsyncBalanceReporter()

    @Test
    fun `value of entire balance comes from data manager wallet balance`() {
        balanceReporter.entireBalance()
            .test().values().single() `should equal` 100.ether()
    }

    @Test
    fun `imported addresses just returns zero as no imported addresses on ETH`() {
        balanceReporter.importedAddressBalance()
            .test().values().single() `should equal` CryptoValue.ZeroEth
    }

    @Test
    fun `single address comes just returns balance`() {
        balanceReporter.addressBalance("0xAddress")
            .test().values().single() `should equal` 1.ether()
    }

    @Test
    fun `watch only just returns zero as no watch only addresses on ETH`() {
        balanceReporter.watchOnlyBalance()
            .test().values().single() `should equal` CryptoValue.ZeroEth
    }
}
