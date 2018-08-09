package com.blockchain.koin.modules

import org.koin.dsl.module.applicationContext
import piuk.blockchain.android.data.bitcoincash.BchDataManager
import piuk.blockchain.android.data.ethereum.EthDataManager
import piuk.blockchain.android.ui.chooser.AccountListing
import piuk.blockchain.android.ui.chooser.WalletAccountHelperAccountListingAdapter
import piuk.blockchain.android.ui.receive.WalletAccountHelper
import piuk.blockchain.android.util.StringUtils
import java.util.Locale

val applicationModule = applicationContext {

    factory { StringUtils(get()) }

    factory { EthDataManager(get(), get(), get(), get(), get(), get(), get()) }

    factory { BchDataManager(get(), get(), get(), get(), get(), get(), get()) }

    factory { Locale.getDefault() }

    factory { WalletAccountHelper(get(), get(), get(), get(), get(), get(), get()) }

    factory { WalletAccountHelperAccountListingAdapter(get()) }

    factory { get<WalletAccountHelperAccountListingAdapter>() as AccountListing }
}
