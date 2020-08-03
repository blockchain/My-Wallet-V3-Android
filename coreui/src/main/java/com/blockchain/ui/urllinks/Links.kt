package com.blockchain.ui.urllinks

import info.blockchain.balance.CryptoCurrency

const val URL_BLOCKCHAIN_SUPPORT_PORTAL =
    "https://support.blockchain.com/"

const val URL_BLOCKCHAIN_PAX_FAQ =
    "https://support.blockchain.com/hc/en-us/sections/360004368351-USD-Pax-FAQ"

const val URL_BLOCKCHAIN_PAX_NEEDS_ETH_FAQ =
    "https://support.blockchain.com/hc/en-us/articles/360027492092-Why-do-I-need-ETH-to-send-my-PAX-"

const val URL_BLOCKCHAIN_RAISE_SUPPORT_TICKET =
    "https://blockchain.zendesk.com/hc/en-us/requests/new?ticket_form_id=360000180551"

const val URL_BLOCKCHAIN_ORDER_FAILED_BELOW_MIN =
    "https://support.blockchain.com/hc/en-us/articles/360023587292-Order-failed-due-to-market-movements"

const val URL_BLOCKCHAIN_ORDER_ABOVE_MAX =
    "https://support.blockchain.com/hc/en-us/articles/360023587292-Order-failed-due-to-market-movements"

const val URL_BLOCKCHAIN_ORDER_LIMIT_EXCEED =
    "https://support.blockchain.com/hc/en-us/articles/360018353031-Exchange-Limit-Amounts"

const val URL_BLOCKCHAIN_ORDER_EXPIRED =
    "https://support.blockchain.com/hc/en-us/articles/360023587592-Why-has-my-order-expired-"

const val URL_BLOCKCHAIN_GOLD_UNAVAILABLE_SUPPORT =
    "https://support.blockchain.com/hc/en-us/categories/360001135512-Identity-Verification"

const val URL_BLOCKCHAIN_KYC_SUPPORTED_COUNTRIES_LIST =
    "https://support.blockchain.com/hc/en-us/articles/360018751932"

const val URL_THE_PIT_LANDING_LEARN_MORE = "https://exchange.blockchain.com"
const val URL_THE_PIT_LAUNCH_SUPPORT = "https://exchange-support.blockchain.com/hc/en-us"

const val URL_COINIFY_POLICY = "https://coinify.com/legal"
const val URL_TOS_POLICY = "https://blockchain.com/terms"
const val URL_PRIVACY_POLICY = "https://blockchain.com/privacy"
const val URL_CONTACT_SUPPORT = "https://support.blockchain.com/hc/requests/new"
const val URL_LEARN_MORE_REJECTED =
    "https://support.blockchain.com/hc/articles/360018080352-Why-has-my-ID-submission-been-rejected-"

const val URL_SUPPORTED_COUNTRIES =
    "https://support.blockchain.com/hc/en-us/articles/360000804146-What-countries-are-supported-for-buying-selling-"

const val STX_STACKS_LEARN_MORE =
    "https://support.blockchain.com/hc/en-us/articles/360038745191"

const val MODULAR_TERMS_AND_CONDITIONS =
    "https://exchange.blockchain.com/legal#modulr"

fun makeBlockExplorerUrl(
    cryptoCurrency: CryptoCurrency,
    transactionHash: String
) = when (cryptoCurrency) {
    CryptoCurrency.BTC -> "https://www.blockchain.com/btc/tx/"
    CryptoCurrency.BCH -> "https://www.blockchain.com/bch/tx/"
    CryptoCurrency.XLM -> "https://stellarchain.io/tx/"
    CryptoCurrency.ETHER,
    CryptoCurrency.PAX,
    CryptoCurrency.USDT -> "https://www.blockchain.com/eth/tx/"
    CryptoCurrency.ALGO -> "https://algoexplorer.io/tx/"
    CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
} + transactionHash