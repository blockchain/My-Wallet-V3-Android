package piuk.blockchain.androidcore.data.api

import com.blockchain.network.EnvironmentUrls
import info.blockchain.wallet.api.Environment

interface EnvironmentConfig : EnvironmentUrls {
    val environment: Environment
    val bitpayUrl: String
    val lunuUrl: String

    fun isRunningInDebugMode(): Boolean

    fun isCompanyInternalBuild(): Boolean
}
