package com.blockchain.koin.modules

import android.content.Context
import com.blockchain.balance.TotalBalance
import com.blockchain.balance.plus
import com.blockchain.kycui.settings.KycStatusHelper
import com.blockchain.ui.CurrentContextAccess
import com.blockchain.ui.chooser.AccountListing
import com.blockchain.ui.password.SecondPasswordHandler
import info.blockchain.wallet.util.PrivateKeyFactory
import org.koin.dsl.module.applicationContext
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.data.cache.DynamicFeeCache
import piuk.blockchain.android.data.datamanagers.TransactionListDataManager
import piuk.blockchain.android.sunriver.SunriverDeepLinkHelper
import piuk.blockchain.android.ui.account.SecondPasswordHandlerDialog
import piuk.blockchain.android.ui.chooser.WalletAccountHelperAccountListingAdapter
import piuk.blockchain.android.ui.receive.WalletAccountHelper
import piuk.blockchain.android.ui.send.OriginalSendPresenterStrategy
import piuk.blockchain.android.ui.send.SendPresenterXSendView
import piuk.blockchain.android.ui.send.SendView
import piuk.blockchain.android.ui.send.external.PerCurrencySendPresenter
import piuk.blockchain.android.ui.send.external.SendPresenterStrategy
import piuk.blockchain.android.ui.send.send2.XlmSendPresenterStrategy
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper
import piuk.blockchain.android.util.OSUtil
import piuk.blockchain.android.util.PrngHelper
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidbuysell.datamanagers.BuyDataManager
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.utils.PrngFixer
import piuk.blockchain.androidcoreui.utils.DateUtil
import java.util.Locale

val applicationModule = applicationContext {

    factory { OSUtil(get()) }

    factory { StringUtils(get()) }

    factory { get<Context>().resources }

    factory { Locale.getDefault() }

    bean { CurrentContextAccess() }

    context("Payload") {

        factory {
            EthDataManager(get(), get(), get(), get(), get(), get(), get(), get())
        }

        factory {
            BchDataManager(get(), get(), get(), get(), get(), get(), get())
        }

        factory {
            BuyDataManager(get(), get(), get(), get(), get())
        }

        factory {
            SwipeToReceiveHelper(get(), get(), get(), get(), get(), get(), get())
        }

        factory { WalletAccountHelper(get(), get(), get(), get(), get(), get(), get(), get()) }

        factory { WalletAccountHelperAccountListingAdapter(get()) }
            .bind(AccountListing::class)

        factory {
            SecondPasswordHandlerDialog(get(), get()) as SecondPasswordHandler
        }

        factory { KycStatusHelper(get(), get(), get(), get()) }

        factory { TransactionListDataManager(get(), get(), get(), get(), get(), get()) }

        factory("spendable") { get<TransactionListDataManager>() as TotalBalance }

        factory("all") {
            if (BuildConfig.SHOW_LOCKBOX_BALANCE) {
                get<TotalBalance>("lockbox") + get("spendable")
            } else {
                get("spendable")
            }
        }

        factory {
            val originalStrategy: SendPresenterStrategy<SendView> = OriginalSendPresenterStrategy(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get()
            )
            SendPresenterXSendView(
                PerCurrencySendPresenter(
                    originalStrategy = originalStrategy,
                    xlmStrategy = XlmSendPresenterStrategy(get(), get(), get(), get(), get()),
                    currencyState = get(),
                    exchangeRates = get(),
                    stringUtils = get(),
                    exchangeRateFactory = get()
                )
            )
        }

        factory { SunriverDeepLinkHelper(get()) }
    }

    factory { DateUtil(get()) }

    bean { PrngHelper(get(), get()) as PrngFixer }

    factory { PrivateKeyFactory() }

    bean { DynamicFeeCache() }
}
