package piuk.blockchain.android.ui.activity.detail

import com.blockchain.sunriver.XlmDataManager
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money
import info.blockchain.wallet.multiaddress.MultiAddressFactory
import info.blockchain.wallet.util.FormatsUtil
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.NonCustodialActivitySummaryItem
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

class TransactionInOutMapper(
    private val transactionHelper: TransactionHelper,
    private val payloadDataManager: PayloadDataManager,
    private val stringUtils: StringUtils,
    private val ethDataManager: EthDataManager,
    private val bchDataManager: BchDataManager,
    private val xlmDataManager: XlmDataManager,
    private val environmentSettings: EnvironmentConfig,
    private val labels: DefaultLabels
) {

    fun transformInputAndOutputs(item: NonCustodialActivitySummaryItem): Single<TransactionInOutDetails> =
        when (item.cryptoCurrency) {
            CryptoCurrency.BTC -> handleBtcToAndFrom(item)
            CryptoCurrency.BCH -> handleBchToAndFrom(item)
            CryptoCurrency.XLM -> handleXlmToAndFrom(item)
            CryptoCurrency.ETHER,
            CryptoCurrency.PAX,
            CryptoCurrency.USDT -> handleErc20ToAndFrom(item)
            else -> throw IllegalArgumentException("${item.cryptoCurrency} is not currently supported")
        }

    private fun handleXlmToAndFrom(activitySummaryItem: NonCustodialActivitySummaryItem) =
        xlmDataManager.defaultAccount()
            .map { account ->
                var fromAddress = activitySummaryItem.inputsMap.keys.first()
                var toAddress = activitySummaryItem.outputsMap.keys.first()
                if (fromAddress == account.accountId) {
                    fromAddress = account.label
                }
                if (toAddress == account.accountId) {
                    toAddress = account.label
                }

                TransactionInOutDetails(
                    inputs = listOf(
                        TransactionDetailModel(
                            fromAddress
                        )
                    ),
                    outputs = listOf(
                        TransactionDetailModel(
                            toAddress
                        )
                    )
                )
            }

    private fun handleErc20ToAndFrom(activitySummaryItem: NonCustodialActivitySummaryItem) =
        Single.fromCallable {
            var fromAddress = activitySummaryItem.inputsMap.keys.first()
            var toAddress = activitySummaryItem.outputsMap.keys.first()

            val ethAddress = ethDataManager.getEthResponseModel()!!.getAddressResponse()!!.account
            if (fromAddress == ethAddress) {
                fromAddress = labels.getDefaultNonCustodialWalletLabel(activitySummaryItem.cryptoCurrency)
            }
            if (toAddress == ethAddress) {
                toAddress = labels.getDefaultNonCustodialWalletLabel(activitySummaryItem.cryptoCurrency)
            }

            TransactionInOutDetails(
                inputs = listOf(
                    TransactionDetailModel(
                        fromAddress
                    )
                ),
                outputs = listOf(
                    TransactionDetailModel(
                        toAddress
                    )
                )
            )
        }

    private fun handleBtcToAndFrom(activitySummaryItem: NonCustodialActivitySummaryItem) =
        Single.fromCallable {
            val (inputs, outputs) = transactionHelper.filterNonChangeBtcAddresses(activitySummaryItem)
            setToAndFrom(CryptoCurrency.BTC, inputs, outputs)
        }

    private fun handleBchToAndFrom(activitySummaryItem: NonCustodialActivitySummaryItem) =
        Single.fromCallable {
            val (inputs, outputs) = transactionHelper.filterNonChangeBchAddresses(activitySummaryItem)
            setToAndFrom(CryptoCurrency.BCH, inputs, outputs)
        }

    private fun setToAndFrom(
        cryptoCurrency: CryptoCurrency,
        inputs: Map<String, Money>,
        outputs: Map<String, Money>
    ) = TransactionInOutDetails(
        inputs = getFromList(cryptoCurrency, inputs),
        outputs = getToList(cryptoCurrency, outputs)
    )

    private fun getFromList(
        currency: CryptoCurrency,
        inputMap: Map<String, Money>
    ): List<TransactionDetailModel> {
        val inputs = handleTransactionMap(inputMap, currency)
        // No inputs = coinbase transaction
        if (inputs.isEmpty()) {
            val coinbase =
                TransactionDetailModel(
                    address = stringUtils.getString(R.string.transaction_detail_coinbase),
                    displayUnits = currency.displayTicker
                )
            inputs.add(coinbase)
        }
        return inputs.toList()
    }

    private fun getToList(
        currency: CryptoCurrency,
        outputMap: Map<String, Money>
    ): List<TransactionDetailModel> = handleTransactionMap(outputMap, currency)

    private fun handleTransactionMap(
        inputMap: Map<String, Money>,
        currency: CryptoCurrency
    ): MutableList<TransactionDetailModel> {
        val inputs = mutableListOf<TransactionDetailModel>()
        for ((key, value) in inputMap) {
            val label = if (currency == CryptoCurrency.BTC) {
                payloadDataManager.addressToLabel(key)
            } else {
                bchDataManager.getLabelFromBchAddress(key)
                    ?: FormatsUtil.toShortCashAddress(environmentSettings.bitcoinCashNetworkParameters, key)
            }

            val transactionDetailModel = buildTransactionDetailModel(label, value, currency)
            inputs.add(transactionDetailModel)
        }
        return inputs
    }

    private fun buildTransactionDetailModel(
        label: String,
        value: Money,
        cryptoCurrency: CryptoCurrency
    ): TransactionDetailModel =
        TransactionDetailModel(
            label,
            value.toStringWithoutSymbol(),
            cryptoCurrency.displayTicker
        ).apply {
            if (address == MultiAddressFactory.ADDRESS_DECODE_ERROR) {
                address = stringUtils.getString(R.string.tx_decode_error)
                addressDecodeError = true
            }
        }
}
