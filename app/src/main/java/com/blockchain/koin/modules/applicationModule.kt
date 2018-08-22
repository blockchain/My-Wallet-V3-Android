package com.blockchain.koin.modules

import android.content.Context
import com.blockchain.koin.getActivity
import com.blockchain.ui.chooser.AccountListing
import com.blockchain.ui.password.SecondPasswordHandler
import org.koin.dsl.module.applicationContext
import piuk.blockchain.android.data.bitcoincash.BchDataManager
import piuk.blockchain.android.data.ethereum.EthDataManager
import piuk.blockchain.android.ui.account.SecondPasswordHandlerDialog
import piuk.blockchain.android.ui.chooser.WalletAccountHelperAccountListingAdapter
import piuk.blockchain.android.ui.receive.WalletAccountHelper
import piuk.blockchain.android.util.StringUtils
import java.util.Locale

val applicationModule = applicationContext {

    factory { StringUtils(get()) }

    factory { get<Context>().resources }

    factory { Locale.getDefault() }

    context("Payload") {

        factory { EthDataManager(get(), get(), get(), get(), get(), get(), get()) }

        factory { BchDataManager(get(), get(), get(), get(), get(), get(), get()) }

        factory { WalletAccountHelper(get(), get(), get(), get(), get(), get(), get()) }

        factory { WalletAccountHelperAccountListingAdapter(get()) }
            .bind(AccountListing::class)

        factory { params -> SecondPasswordHandlerDialog(params.getActivity(), get()) as SecondPasswordHandler }
    }
}