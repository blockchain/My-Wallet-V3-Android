package piuk.blockchain.android.coincore.impl

import com.blockchain.core.interest.InterestBalanceDataManager
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.service.TierService
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.preferences.WalletStatus
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.BankAccount
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.InterestAccount
import piuk.blockchain.android.coincore.TradingAccount
import piuk.blockchain.android.coincore.TransactionProcessor
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.coincore.TransferError
import piuk.blockchain.android.coincore.fiat.LinkedBankAccount
import piuk.blockchain.android.coincore.impl.txEngine.BitpayTxEngine
import piuk.blockchain.android.coincore.impl.txEngine.FiatDepositTxEngine
import piuk.blockchain.android.coincore.impl.txEngine.FiatWithdrawalTxEngine
import piuk.blockchain.android.coincore.impl.txEngine.LunuTxEngine
import piuk.blockchain.android.coincore.impl.txEngine.OnChainTxEngineBase
import piuk.blockchain.android.coincore.impl.txEngine.TradingToOnChainTxEngine
import piuk.blockchain.android.coincore.impl.txEngine.TransferQuotesEngine
import piuk.blockchain.android.coincore.impl.txEngine.interest.InterestDepositOnChainTxEngine
import piuk.blockchain.android.coincore.impl.txEngine.interest.InterestDepositTradingEngine
import piuk.blockchain.android.coincore.impl.txEngine.interest.InterestWithdrawOnChainTxEngine
import piuk.blockchain.android.coincore.impl.txEngine.interest.InterestWithdrawTradingTxEngine
import piuk.blockchain.android.coincore.impl.txEngine.sell.OnChainSellTxEngine
import piuk.blockchain.android.coincore.impl.txEngine.sell.TradingSellTxEngine
import piuk.blockchain.android.coincore.impl.txEngine.swap.OnChainSwapTxEngine
import piuk.blockchain.android.coincore.impl.txEngine.swap.TradingToTradingSwapTxEngine
import piuk.blockchain.android.data.api.bitpay.BitPayDataManager

class TxProcessorFactory(
    private val bitPayManager: BitPayDataManager,
    private val exchangeRates: ExchangeRatesDataManager,
    private val walletManager: CustodialWalletManager,
    private val interestBalances: InterestBalanceDataManager,
    private val walletPrefs: WalletStatus,
    private val quotesEngine: TransferQuotesEngine,
    private val analytics: Analytics,
    private val kycTierService: TierService
) {
    fun createProcessor(
        source: BlockchainAccount,
        target: TransactionTarget,
        action: AssetAction
    ): Single<TransactionProcessor> =
        when (source) {
            is CryptoNonCustodialAccount -> createOnChainProcessor(source, target, action)
            is CustodialTradingAccount -> createTradingProcessor(source, target, action)
            is CryptoInterestAccount -> createInterestWithdrawalProcessor(source, target, action)
            is BankAccount -> createFiatDepositProcessor(source, target, action)
            is FiatAccount -> createFiatWithdrawalProcessor(source, target, action)
            else -> Single.error(NotImplementedError())
        }

    private fun createInterestWithdrawalProcessor(
        source: CryptoInterestAccount,
        target: TransactionTarget,
        action: AssetAction
    ): Single<TransactionProcessor> =
        when (target) {
            is CustodialTradingAccount -> {
                Single.just(
                    TransactionProcessor(
                        exchangeRates = exchangeRates,
                        sourceAccount = source,
                        txTarget = target,
                        engine = InterestWithdrawTradingTxEngine(
                            walletManager = walletManager,
                            interestBalances = interestBalances
                        )
                    )
                )
            }
            is CryptoNonCustodialAccount -> {
                Single.just(
                    TransactionProcessor(
                        exchangeRates = exchangeRates,
                        sourceAccount = source,
                        txTarget = target,
                        engine = InterestWithdrawOnChainTxEngine(
                            walletManager = walletManager,
                            interestBalances = interestBalances
                        )
                    )
                )
            }
            else -> Single.error(IllegalStateException("$target is not supported yet"))
        }

    private fun createFiatDepositProcessor(
        source: BlockchainAccount,
        target: TransactionTarget,
        action: AssetAction
    ): Single<TransactionProcessor> =
        when (target) {
            is FiatAccount -> {
                Single.just(
                    TransactionProcessor(
                        exchangeRates = exchangeRates,
                        sourceAccount = source,
                        txTarget = target,
                        engine = FiatDepositTxEngine(
                            walletManager = walletManager
                        )
                    )
                )
            }
            else -> {
                Single.error(IllegalStateException("not supported yet"))
            }
        }

    private fun createFiatWithdrawalProcessor(
        source: BlockchainAccount,
        target: TransactionTarget,
        action: AssetAction
    ): Single<TransactionProcessor> =
        when (target) {
            is LinkedBankAccount -> {
                Single.just(
                    TransactionProcessor(
                        exchangeRates = exchangeRates,
                        sourceAccount = source,
                        txTarget = target,
                        engine = FiatWithdrawalTxEngine(
                            walletManager = walletManager
                        )
                    )
                )
            }
            else -> {
                Single.error(IllegalStateException("not supported yet"))
            }
        }

    private fun createOnChainProcessor(
        source: CryptoNonCustodialAccount,
        target: TransactionTarget,
        action: AssetAction
    ): Single<TransactionProcessor> {
        val engine = source.createTxEngine(target) as OnChainTxEngineBase

        return when (target) {
            is BitPayInvoiceTarget -> Single.just(
                TransactionProcessor(
                    exchangeRates = exchangeRates,
                    sourceAccount = source,
                    txTarget = target,
                    engine = BitpayTxEngine(
                        bitPayDataManager = bitPayManager,
                        walletPrefs = walletPrefs,
                        assetEngine = engine,
                        analytics = analytics
                    )
                )
            )
            is LunuInvoiceTarget -> Single.just(
                TransactionProcessor(
                    exchangeRates = exchangeRates,
                    sourceAccount = source,
                    txTarget = target,
                    engine = LunuTxEngine(
                        bitPayDataManager = bitPayManager,
                        walletPrefs = walletPrefs,
                        assetEngine = engine,
                        analytics = analytics
                    )
                )
            )
            is CryptoInterestAccount ->
                target.receiveAddress
                    .map {
                        TransactionProcessor(
                            exchangeRates = exchangeRates,
                            sourceAccount = source,
                            txTarget = it,
                            engine = InterestDepositOnChainTxEngine(
                                walletManager = walletManager,
                                interestBalances = interestBalances,
                                onChainEngine = engine
                            )
                        )
                    }
            is CryptoAddress -> Single.just(
                TransactionProcessor(
                    exchangeRates = exchangeRates,
                    sourceAccount = source,
                    txTarget = target,
                    engine = engine
                )
            )
            is CryptoAccount ->
                if (action != AssetAction.Swap)
                    target.receiveAddress.map {
                        TransactionProcessor(
                            exchangeRates = exchangeRates,
                            sourceAccount = source,
                            txTarget = it,
                            engine = engine
                        )
                    } else {
                    Single.just(
                        TransactionProcessor(
                            exchangeRates = exchangeRates,
                            sourceAccount = source,
                            txTarget = target,
                            engine = OnChainSwapTxEngine(
                                quotesEngine = quotesEngine,
                                walletManager = walletManager,
                                kycTierService = kycTierService,
                                engine = engine
                            )
                        )
                    )
                }
            is FiatAccount -> Single.just(
                TransactionProcessor(
                    exchangeRates = exchangeRates,
                    sourceAccount = source,
                    txTarget = target,
                    engine = OnChainSellTxEngine(
                        quotesEngine = quotesEngine,
                        walletManager = walletManager,
                        kycTierService = kycTierService,
                        engine = engine
                    )
                )
            )
            else -> Single.error(TransferError("Cannot send non-custodial crypto to a non-crypto target"))
        }
    }

    private fun createTradingProcessor(
        source: CustodialTradingAccount,
        target: TransactionTarget,
        action: AssetAction
    ) = when (target) {
        is CryptoAddress ->
            Single.just(
                TransactionProcessor(
                    exchangeRates = exchangeRates,
                    sourceAccount = source,
                    txTarget = target,
                    engine = TradingToOnChainTxEngine(
                        walletManager = walletManager,
                        isNoteSupported = source.isNoteSupported
                    )
                )
            )
        is InterestAccount ->
            Single.just(
                TransactionProcessor(
                    exchangeRates = exchangeRates,
                    sourceAccount = source,
                    txTarget = target,
                    engine = InterestDepositTradingEngine(
                        walletManager = walletManager,
                        interestBalances = interestBalances
                    )
                )
            )
        is TradingAccount ->
            Single.just(
                TransactionProcessor(
                    exchangeRates = exchangeRates,
                    sourceAccount = source,
                    txTarget = target,
                    engine = TradingToTradingSwapTxEngine(
                        walletManager = walletManager,
                        quotesEngine = quotesEngine,
                        kycTierService = kycTierService
                    )
                )
            )
        is CryptoAccount -> target.receiveAddress
            .map {
                TransactionProcessor(
                    exchangeRates = exchangeRates,
                    sourceAccount = source,
                    txTarget = it,
                    engine = TradingToOnChainTxEngine(
                        walletManager = walletManager,
                        isNoteSupported = source.isNoteSupported
                    )
                )
            }
        is FiatAccount ->
            Single.just(
                TransactionProcessor(
                    exchangeRates = exchangeRates,
                    sourceAccount = source,
                    txTarget = target,
                    engine = TradingSellTxEngine(
                        walletManager = walletManager,
                        quotesEngine = quotesEngine,
                        kycTierService = kycTierService
                    )
                )
            )
        else -> Single.error(TransferError("Cannot send custodial crypto to a non-crypto target"))
    }
}
