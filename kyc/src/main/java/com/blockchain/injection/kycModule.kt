package com.blockchain.injection

import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.datamanagers.onfido.OnfidoDataManager
import com.blockchain.kyc.services.nabu.NabuService
import com.blockchain.kyc.services.onfido.OnfidoService
import com.blockchain.kyc.stores.NabuSessionTokenStore
import com.blockchain.kycui.countryselection.KycCountrySelectionPresenter
import com.blockchain.kycui.profile.KycProfilePresenter
import org.koin.dsl.module.applicationContext

val kycModule = applicationContext {

    bean { NabuSessionTokenStore() }

    bean { OnfidoService(get("kotlin")) }

    bean { NabuService(get(), get("kotlin")) }

    factory { NabuDataManager(get(), get(), get("app-version"), get("device-id")) }

    factory { OnfidoDataManager(get()) }

    factory { KycCountrySelectionPresenter(get()) }

    factory { KycProfilePresenter() }
}