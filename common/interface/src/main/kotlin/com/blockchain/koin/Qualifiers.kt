package com.blockchain.koin

import org.koin.core.qualifier.StringQualifier
import org.koin.core.qualifier.named

val applicationScope = StringQualifier("applicationScope")
val featureFlagsPrefs = StringQualifier("FeatureFlagsPrefs")
val feynmanEnterAmountFeatureFlag = StringQualifier("ff_enter_amount_feynman")
val feynmanCheckoutFeatureFlag = StringQualifier("ff_checkout_feynman")
val googlePayFeatureFlag = StringQualifier("ff_gpay")
val superAppMvpFeatureFlag = StringQualifier("ff_super_app")
val intercomChatFeatureFlag = StringQualifier("ff_intercom_chat")
val plaidFeatureFlag = StringQualifier("ff_plaid")
val bindFeatureFlag = StringQualifier("ff_bind")
val proveFeatureFlag = StringQualifier("ff_provedotcom")
val buyRefreshQuoteFeatureFlag = StringQualifier("ff_buy_refresh_quote")
val assetOrderingFeatureFlag = StringQualifier("ff_asset_list_ordering")
val cowboysPromoFeatureFlag = StringQualifier("ff_cowboys_promo")
val cardPaymentAsyncFeatureFlag = StringQualifier("ff_card_payment_async")
val rbFrequencyFeatureFlag = StringQualifier("ff_rb_frequency")
val vgsFeatureFlag = StringQualifier("ff_vgs")
val rbExperimentFeatureFlag = StringQualifier("ff_rb_experiment")
val superappFeatureFlag = StringQualifier("android_ff_superapp_redesign")
val sessionIdFeatureFlag = StringQualifier("ff_x_session_id")
val sardineFeatureFlag = StringQualifier("ff_sardine")
val paymentUxTotalDisplayBalanceFeatureFlag = StringQualifier("ff_payment_ux_total_display_balance")
val paymentUxAssetDisplayBalanceFeatureFlag = StringQualifier("ff_payment_ux_asset_display_balance")
val googleWalletFeatureFlag = StringQualifier("ff_google_wallet")
val blockchainMembershipsFeatureFlag = StringQualifier("ff_bcdc_memberships")
val improvedPaymentUxFeatureFlag = StringQualifier("ff_improved_payment_ux")
val earnTabFeatureFlag = StringQualifier("ff_earn_tab_nav")
val exchangeWAPromptFeatureFlag = StringQualifier("exchange_wa_prompt")
val nabu = StringQualifier("nabu")
val status = StringQualifier("status")
val authOkHttpClient = StringQualifier("authOkHttpClient")
val kotlinApiRetrofit = StringQualifier("kotlin-api")
val explorerRetrofit = StringQualifier("explorer")
val everypayRetrofit = StringQualifier("everypay")
val apiRetrofit = StringQualifier("api")
val kotlinXApiRetrofit = StringQualifier("kotlinx-api")
val evmNodesApiRetrofit = StringQualifier("evm-nodes-api")
val kotlinXCoinApiRetrofit = StringQualifier("kotlinx-coin-api")
val serializerExplorerRetrofit = StringQualifier("serializer_explorer")
val gbp = StringQualifier("GBP")
val usd = StringQualifier("USD")
val eur = StringQualifier("EUR")
val ars = StringQualifier("ARS")
val priorityFee = StringQualifier("Priority")
val regularFee = StringQualifier("Regular")
val bigDecimal = StringQualifier("BigDecimal")
val kotlinJsonConverterFactory = StringQualifier("KotlinJsonConverterFactory")
val kotlinJsonAssetTicker = StringQualifier("KotlinJsonAssetTicker")
val bigInteger = StringQualifier("BigInteger")
val interestLimits = StringQualifier("InterestLimits")
val kyc = StringQualifier("kyc")
val uniqueId = StringQualifier("unique_id")
val uniqueUserAnalytics = StringQualifier("unique_user_analytics")
val userAnalytics = StringQualifier("user_analytics")
val walletAnalytics = StringQualifier("wallet_analytics")
val embraceLogger = StringQualifier("embrace_logger")
val payloadScopeQualifier = named("Payload")
val ioDispatcher = named("io_dispatcher")
val defaultOrder = named("default_order")
val swapSourceOrder = named("swap_source_order")
val swapTargetOrder = named("swap_target_order")
val sellOrder = named("sell_order")
val buyOrder = named("buy_order")
val interestBalanceStore = StringQualifier("interestBalanceStore")
val stakingBalanceStore = StringQualifier("stakingBalanceStore")
