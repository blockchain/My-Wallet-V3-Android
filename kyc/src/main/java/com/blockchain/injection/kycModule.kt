package com.blockchain.injection

import com.blockchain.koin.moshiInterceptor
import com.blockchain.kyc.datamanagers.nabu.NabuAuthenticator
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.datamanagers.onfido.OnfidoDataManager
import com.blockchain.kyc.models.nabu.KycStateAdapter
import com.blockchain.kyc.models.nabu.UserStateAdapter
import com.blockchain.kyc.services.nabu.NabuService
import com.blockchain.kyc.services.onfido.OnfidoService
import com.blockchain.kyc.services.wallet.RetailWalletTokenService
import com.blockchain.kycui.address.KycHomeAddressPresenter
import com.blockchain.kycui.countryselection.KycCountrySelectionPresenter
import com.blockchain.kycui.mobile.entry.KycMobileEntryPresenter
import com.blockchain.kycui.mobile.validation.KycMobileValidationPresenter
import com.blockchain.kycui.onfidosplash.OnfidoSplashPresenter
import com.blockchain.kycui.profile.KycProfilePresenter
import com.blockchain.kycui.status.KycStatusPresenter
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.stores.NabuSessionTokenStore
import org.koin.dsl.module.applicationContext

val kycModule = applicationContext {

    bean { NabuSessionTokenStore() }

    bean { OnfidoService(get("kotlin")) }

    bean { NabuService(get(), get("nabu")) }

    bean { RetailWalletTokenService(get(), getProperty("api-code"), get("kotlin")) }

    factory { OnfidoDataManager(get()) }

    context("Payload") {

        factory {
            NabuDataManager(
                get(),
                get(),
                get(),
                getProperty("app-version"),
                get("device-id"),
                get(),
                get()
            )
        }

        factory {
            NabuAuthenticator(get(), get()) as Authenticator
        }

        factory { KycCountrySelectionPresenter(get()) }

        factory { KycProfilePresenter(get(), get()) }

        factory { KycHomeAddressPresenter(get(), get(), get()) }

        factory { KycMobileEntryPresenter(get()) }

        factory { KycMobileValidationPresenter(get(), get(), get()) }

        factory { OnfidoSplashPresenter(get(), get(), get()) }

        factory { KycStatusPresenter(get(), get(), get()) }
    }

    moshiInterceptor("kyc") { builder ->
        builder
            .add(KycStateAdapter())
            .add(UserStateAdapter())
    }
}
