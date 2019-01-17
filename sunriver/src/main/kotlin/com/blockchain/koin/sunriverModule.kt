package com.blockchain.koin

import com.blockchain.account.DefaultAccountDataManager
import com.blockchain.accounts.AsyncAccountList
import com.blockchain.accounts.XlmAsyncAccountListAdapter
import com.blockchain.balance.AsyncAccountBalanceReporter
import com.blockchain.balance.AsyncAddressBalanceReporter
import com.blockchain.sunriver.HorizonProxy
import com.blockchain.sunriver.MemoMapper
import com.blockchain.sunriver.XlmDataManager
import com.blockchain.sunriver.XlmSecretAccess
import com.blockchain.sunriver.datamanager.XlmMetaDataInitializer
import com.blockchain.transactions.logMemoType
import com.blockchain.transactions.updateLastTxOnSend
import org.koin.dsl.module.applicationContext

val sunriverModule = applicationContext {

    context("Payload") {

        factory { XlmSecretAccess(get()) }

        factory { XlmDataManager(get(), get(), get(), get()) }
            .bind(DefaultAccountDataManager::class)
            .bind(AsyncAddressBalanceReporter::class)

        factory("XLM") { get<XlmDataManager>() as AsyncAccountBalanceReporter }

        factory { get<XlmDataManager>().updateLastTxOnSend(get()).logMemoType(get()) }

        factory { HorizonProxy(getProperty("HorizonURL")) }

        bean { XlmMetaDataInitializer(get(), get(), get(), get()) }

        factory("XLM") { XlmAsyncAccountListAdapter(get()) as AsyncAccountList }
    }

    factory { MemoMapper() }
}
