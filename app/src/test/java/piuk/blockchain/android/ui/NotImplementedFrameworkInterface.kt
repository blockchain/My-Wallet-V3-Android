package piuk.blockchain.android.ui

import info.blockchain.wallet.FrameworkInterface
import info.blockchain.wallet.api.Environment
import org.apache.commons.lang3.NotImplementedException
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.params.BitcoinCashMainNetParams
import org.bitcoinj.params.BitcoinMainNetParams
import retrofit2.Retrofit

object NotImplementedFrameworkInterface : FrameworkInterface {

    override fun getDevice(): String {
        throw NotImplementedException("Function should not be called")
    }

    override fun getRetrofitExplorerInstance(): Retrofit {
        throw NotImplementedException("Function should not be called")
    }

    override fun getEnvironment(): Environment {
        throw NotImplementedException("Function should not be called")
    }

    override fun getRetrofitApiInstance(): Retrofit {
        throw NotImplementedException("Function should not be called")
    }

    override val apiCode: String
        get() = throw NotImplementedException("Function should not be called")

    override fun getAppVersion(): String {
        throw NotImplementedException("Function should not be called")
    }

    override fun getBitcoinParams(): NetworkParameters {
        return BitcoinMainNetParams.get()
    }

    override fun getBitcoinCashParams(): NetworkParameters {
        return BitcoinCashMainNetParams.get()
    }
}
