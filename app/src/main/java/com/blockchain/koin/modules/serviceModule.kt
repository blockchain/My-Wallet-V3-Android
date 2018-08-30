package com.blockchain.koin.modules

import info.blockchain.wallet.ApiCode
import info.blockchain.wallet.BlockchainFramework
import info.blockchain.wallet.api.FeeApi
import info.blockchain.wallet.api.FeeEndpoints
import info.blockchain.wallet.api.WalletApi
import info.blockchain.wallet.api.WalletExplorerEndpoints
import info.blockchain.wallet.ethereum.EthAccountApi
import info.blockchain.wallet.payment.Payment
import info.blockchain.wallet.settings.SettingsManager
import org.koin.dsl.module.applicationContext
import piuk.blockchain.android.data.fingerprint.FingerprintAuth
import piuk.blockchain.android.data.fingerprint.FingerprintAuthImpl
import retrofit2.Retrofit

val serviceModule = applicationContext {

    bean { SettingsManager(get()) }

    bean { get<Retrofit>("explorer").create(WalletExplorerEndpoints::class.java) }

    bean { get<Retrofit>("api").create(FeeEndpoints::class.java) }

    factory { WalletApi(get(), get()) }

    factory { Payment() }

    factory { FeeApi(get()) }

    factory {
        object : ApiCode {
            override val apiCode: String
                get() = BlockchainFramework.getApiCode()
        } as ApiCode
    }

    factory { FingerprintAuthImpl() as FingerprintAuth }

    factory { EthAccountApi() }
}
