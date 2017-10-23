package piuk.blockchain.android.ui.send

import android.content.Intent
import android.support.design.widget.Snackbar
import android.text.Editable
import android.widget.EditText
import com.fasterxml.jackson.databind.ObjectMapper
import info.blockchain.api.data.UnspentOutputs
import info.blockchain.wallet.api.WalletApi
import info.blockchain.wallet.api.data.FeeOptions
import info.blockchain.wallet.ethereum.EthereumAccount
import info.blockchain.wallet.multiaddress.TransactionSummary
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.LegacyAddress
import info.blockchain.wallet.payment.Payment
import info.blockchain.wallet.util.FormatsUtil
import info.blockchain.wallet.util.PrivateKeyFactory
import info.blockchain.wallet.util.Tools
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.PublishSubject
import org.apache.commons.lang3.tuple.Pair
import org.bitcoinj.core.ECKey
import org.web3j.protocol.core.methods.request.RawTransaction
import org.web3j.utils.Convert
import piuk.blockchain.android.R
import piuk.blockchain.android.data.answers.Logging
import piuk.blockchain.android.data.answers.PaymentSentEvent
import piuk.blockchain.android.data.api.EnvironmentSettings
import piuk.blockchain.android.data.auth.AuthService
import piuk.blockchain.android.data.cache.DynamicFeeCache
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.data.currency.CurrencyState
import piuk.blockchain.android.data.datamanagers.FeeDataManager
import piuk.blockchain.android.data.datamanagers.TransactionListDataManager
import piuk.blockchain.android.data.ethereum.EthDataManager
import piuk.blockchain.android.data.ethereum.models.CombinedEthModel
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.data.payments.SendDataManager
import piuk.blockchain.android.data.rxjava.IgnorableDefaultObserver
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.data.services.EventService
import piuk.blockchain.android.data.transactions.BtcDisplayable
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.account.PaymentConfirmationDetails
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.chooser.AccountChooserActivity
import piuk.blockchain.android.ui.receive.ReceiveCurrencyHelper
import piuk.blockchain.android.ui.receive.WalletAccountHelper
import piuk.blockchain.android.util.*
import piuk.blockchain.android.util.helperfunctions.unsafeLazy
import timber.log.Timber
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SendPresenter @Inject constructor(
        private val walletAccountHelper: WalletAccountHelper,
        private val payloadDataManager: PayloadDataManager,
        private val currencyState: CurrencyState,
        private val ethDataManager: EthDataManager,
        private val prefsUtil: PrefsUtil,
        private val exchangeRateFactory: ExchangeRateFactory,
        private val stringUtils: StringUtils,
        private val sendDataManager: SendDataManager,
        private val dynamicFeeCache: DynamicFeeCache,
        private val feeDataManager: FeeDataManager,
        private val privateKeyFactory: PrivateKeyFactory,
        private val environmentSettings: EnvironmentSettings,
        private val transactionListDataManager: TransactionListDataManager
) : BasePresenter<SendView>() {

    private val locale: Locale by unsafeLazy { Locale.getDefault() }
    private val currencyHelper by unsafeLazy {
        ReceiveCurrencyHelper(
                monetaryUtil,
                locale,
                prefsUtil,
                exchangeRateFactory,
                currencyState
        )
    }
    private val monetaryUtil: MonetaryUtil by unsafeLazy { MonetaryUtil(getBtcUnitType()) }

    private var feeOptions: FeeOptions? = null
    private val pendingTransaction by unsafeLazy { PendingTransaction() }
    private val unspentApiResponses by unsafeLazy { HashMap<String, UnspentOutputs>() }
    private var textChangeSubject = PublishSubject.create<String>()
    private var absoluteSuggestedFee = BigInteger.ZERO
    private var maxAvailable = BigInteger.ZERO
    private var verifiedSecondPassword: String? = null

    private var metricInputFlag: String? = null

    private fun getBtcUnitType() = prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)

    /**
     * External changes.
     * Possible currency change, Account/address archive, Balance change
     */
    internal fun onBroadcastReceived() {
        updateTicker()
        resetAccountList()
    }

    override fun onViewReady() {
        resetAccountList()
        setupTextChangeSubject()
        updateTicker()
        updateCurrencyUnits()
    }

    fun onResume() {
        when (currencyState.cryptoCurrency) {
            CryptoCurrencies.BTC -> onBitcoinChosen()
            CryptoCurrencies.ETHER -> onEtherChosen()
            else -> throw IllegalArgumentException("BCC is not currently supported")
        }
    }

    internal fun onBitcoinChosen() {
        view.showFeePriority()
        currencyState.cryptoCurrency = CryptoCurrencies.BTC
        view.setTabSelection(0)
        view.enableFeeDropdown()
        view.setCryptoMaxLength(17)
        resetState()
        calculateSpendableAmounts(spendAll = false, amountToSendText = "0")
        view.enableInput()
    }

    internal fun onEtherChosen() {
        view.hideFeePriority()
        currencyState.cryptoCurrency = CryptoCurrencies.ETHER
        view.setFeePrioritySelection(0)
        view.setTabSelection(1)
        view.disableFeeDropdown()
        view.setCryptoMaxLength(30)
        resetState()
    }

    private fun resetState() {
        compositeDisposable.clear()
        pendingTransaction.clear()
        view?.setSendButtonEnabled(true)
        updateTicker()
        absoluteSuggestedFee = BigInteger.ZERO
        view.updateFeeAmount("")
        resetAccountList()
        selectDefaultSendingAccount()
        view.hideMaxAvailable()
        clearCryptoAmount()
        clearReceivingAddress()
        updateCurrencyUnits()
    }

    internal fun onContinueClicked() {
        view?.showProgressDialog(R.string.app_name)

        checkManualAddressInput()

        when (currencyState.cryptoCurrency) {
            CryptoCurrencies.BTC -> {
                Observable.just(validateBitcoinTransaction())
                        .doAfterTerminate { view?.dismissProgressDialog() }
                        .compose(RxUtil.addObservableToCompositeDisposable(this))
                        .subscribe({
                            if (it.left) {
                                if (pendingTransaction.isWatchOnly) {
                                    //returns to spendFromWatchOnly*BIP38 -> showPaymentReview()
                                    view.showSpendFromWatchOnlyWarning((pendingTransaction.sendingObject.accountObject as LegacyAddress).address)
                                } else if (pendingTransaction.isWatchOnly && verifiedSecondPassword != null) {
                                    //Second password already verified
                                    showPaymentReview()
                                } else {
                                    //Checks if second pw needed then -> onNoSecondPassword()
                                    view.showSecondPasswordDialog()
                                }
                            } else {
                                view.showSnackbar(it.right, Snackbar.LENGTH_LONG)
                            }
                        }, { Timber.e(it) })
            }
            CryptoCurrencies.ETHER -> {
                validateEtherTransaction()
                        .doAfterTerminate { view?.dismissProgressDialog() }
                        .compose(RxUtil.addObservableToCompositeDisposable(this))
                        .subscribe({
                            when {
                            //  Checks if second pw needed then -> onNoSecondPassword()
                                it.left -> view.showSecondPasswordDialog()
                                it.right == R.string.eth_support_contract_not_allowed -> view.showEthContractSnackbar()
                                else -> view.showSnackbar(it.right, Snackbar.LENGTH_LONG)
                            }
                        }, { Timber.e(it) })
            }
            else -> throw IllegalArgumentException("BCC is not currently supported")
        }
    }

    /**
     * Executes transaction
     */
    internal fun submitPayment() {
        when (currencyState.cryptoCurrency) {
            CryptoCurrencies.BTC -> submitBitcoinTransaction()
            CryptoCurrencies.ETHER -> submitEthTransaction()
            else -> throw IllegalArgumentException("BCC is not currently supported")
        }
    }

    private fun submitBitcoinTransaction() {
        view.showProgressDialog(R.string.app_name)

        getBtcChangeAddress()!!
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .doOnError {
                    view.dismissProgressDialog()
                    view.dismissConfirmationDialog()
                    view.showSnackbar(R.string.transaction_failed, Snackbar.LENGTH_INDEFINITE)
                }
                .map { pendingTransaction.changeAddress = it }
                .flatMap { getBtcKeys() }
                .flatMap {
                    sendDataManager.submitPayment(pendingTransaction.unspentOutputBundle,
                            it,
                            pendingTransaction.receivingAddress,
                            pendingTransaction.changeAddress,
                            pendingTransaction.bigIntFee,
                            pendingTransaction.bigIntAmount)
                }
                .subscribe({ hash ->
                    Logging.logCustom(PaymentSentEvent()
                            .putSuccess(true)
                            .putAmountForRange(pendingTransaction.bigIntAmount))

                    clearBtcUnspentResponseCache()
                    view.dismissProgressDialog()
                    view.dismissConfirmationDialog()
                    insertBtcPlaceHolderTransaction(hash, pendingTransaction)
                    incrementBtcReceiveAddress()
                    handleSuccessfulPayment(hash, CryptoCurrencies.BTC)
                }) {
                    Timber.e(it)
                    view.dismissProgressDialog()
                    view.dismissConfirmationDialog()
                    view.showSnackbar(R.string.transaction_failed, Snackbar.LENGTH_INDEFINITE)

                    Logging.logCustom(PaymentSentEvent()
                            .putSuccess(false)
                            .putAmountForRange(pendingTransaction.bigIntAmount))
                }
    }

    private fun getBtcKeys(): Observable<MutableList<ECKey?>>? {
        return if (pendingTransaction.isHD) {
            val account = pendingTransaction.sendingObject.accountObject as Account

            if (payloadDataManager.isDoubleEncrypted) {
                payloadDataManager.decryptHDWallet(verifiedSecondPassword)
            }
            Observable.just(payloadDataManager.getHDKeysForSigning(account, pendingTransaction.unspentOutputBundle))
        } else {
            val legacyAddress = pendingTransaction.sendingObject.accountObject as LegacyAddress

            if (legacyAddress.tag == PendingTransaction.WATCH_ONLY_SPEND_TAG) {
                var eckey = Tools.getECKeyFromKeyAndAddress(legacyAddress.privateKey, legacyAddress.getAddress());
                Observable.just(mutableListOf(eckey))
            } else {
                Observable.just(mutableListOf(payloadDataManager.getAddressECKey(legacyAddress, verifiedSecondPassword)))
            }
        }
    }

    private fun getBtcChangeAddress(): Observable<String>? {
        return if (pendingTransaction.isHD) {
            val account = pendingTransaction.sendingObject.accountObject as Account
            payloadDataManager.getNextChangeAddress(account)
        } else {
            val legacyAddress = pendingTransaction.sendingObject.accountObject as LegacyAddress
            Observable.just(legacyAddress.address)
        }
    }

    private fun submitEthTransaction() {
        createEthTransaction()
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .doOnError { view.showSnackbar(R.string.transaction_failed, Snackbar.LENGTH_INDEFINITE) }
                .doOnTerminate {
                    view.dismissProgressDialog()
                    view.dismissConfirmationDialog()
                }
                .flatMap {
                    if (payloadDataManager.isDoubleEncrypted) {
                        payloadDataManager.decryptHDWallet(verifiedSecondPassword)
                    }

                    val ecKey = EthereumAccount.deriveECKey(
                            payloadDataManager.wallet.hdWallets[0].masterKey,
                            0
                    )
                    return@flatMap ethDataManager.signEthTransaction(it, ecKey)
                }
                .flatMap { ethDataManager.pushEthTx(it) }
                .flatMap { ethDataManager.setLastTxHashObservable(it) }
                .subscribe(
                        { handleSuccessfulPayment(it, CryptoCurrencies.ETHER) },
                        {
                            Timber.e(it)
                            view.showSnackbar(R.string.transaction_failed, Snackbar.LENGTH_INDEFINITE)
                        })
    }

    private fun createEthTransaction(): Observable<RawTransaction> {
        val feeGwei = BigDecimal.valueOf(feeOptions!!.regularFee)
        val feeWei = Convert.toWei(feeGwei, Convert.Unit.GWEI)

        return Observable.just(ethDataManager.getEthResponseModel()!!.getNonce())
                .map {
                    ethDataManager.createEthTransaction(
                            nonce = it,
                            to = pendingTransaction.receivingAddress,
                            gasPrice = feeWei.toBigInteger(),
                            gasLimit = BigInteger.valueOf(feeOptions!!.gasLimit),
                            weiValue = pendingTransaction.bigIntAmount
                    )
                }
    }

    private fun clearBtcUnspentResponseCache() {
        if (pendingTransaction.isHD) {
            val account = pendingTransaction.sendingObject.accountObject as Account
            unspentApiResponses.remove(account.xpub)
        } else {
            val legacyAddress = pendingTransaction.sendingObject.accountObject as LegacyAddress
            unspentApiResponses.remove(legacyAddress.address)
        }
    }

    private fun incrementBtcReceiveAddress() {
        if (pendingTransaction.isHD) {
            val account = pendingTransaction.sendingObject.accountObject as Account
            payloadDataManager.incrementChangeAddress(account)
            payloadDataManager.incrementReceiveAddress(account)
            updateInternalBalances()
        }
    }

    private fun handleSuccessfulPayment(hash: String, cryptoCurrency: CryptoCurrencies): String {
        view?.showTransactionSuccess(hash, pendingTransaction.bigIntAmount.toLong(), cryptoCurrency)

        pendingTransaction.clear()
        unspentApiResponses.clear()

        logAddressInputMetric()

        return hash
    }

    private fun logAddressInputMetric() {
        val handler = EventService(prefsUtil, AuthService(WalletApi()))
        if (metricInputFlag != null) handler.logAddressInputEvent(metricInputFlag)
    }

    /**
     * Update balance immediately after spend - until refresh from server
     */
    private fun updateInternalBalances() {
        try {
            val totalSent = pendingTransaction.bigIntAmount.add(pendingTransaction.bigIntFee)
            if (pendingTransaction.isHD) {
                val account = pendingTransaction.sendingObject.accountObject as Account
                payloadDataManager.subtractAmountFromAddressBalance(account.xpub, totalSent.toLong())
            } else {
                val address = pendingTransaction.sendingObject.accountObject as LegacyAddress
                payloadDataManager.subtractAmountFromAddressBalance(address.address, totalSent.toLong())
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    /**
     * After sending btc we create a "placeholder" tx until websocket handler refreshes list
     */
    private fun insertBtcPlaceHolderTransaction(hash: String, pendingTransaction: PendingTransaction) {
        val inputs = HashMap<String, BigInteger>()
        pendingTransaction.sendingObject.label?.let {
            inputs.put(pendingTransaction.sendingObject.label!!, pendingTransaction.bigIntAmount)
        }

        val outputs = HashMap<String, BigInteger>()
        outputs.put(pendingTransaction.displayableReceivingLabel, pendingTransaction.bigIntAmount)

        val tx = TransactionSummary()
        tx.direction = TransactionSummary.Direction.SENT
        tx.time = System.currentTimeMillis() / 1000
        tx.total = pendingTransaction.bigIntAmount.add(pendingTransaction.bigIntFee)
        tx.hash = hash
        tx.fee = pendingTransaction.bigIntFee
        tx.inputsMap = inputs
        tx.outputsMap = outputs
        tx.isPending = true

        transactionListDataManager.insertTransactionIntoListAndReturnSorted(BtcDisplayable(tx))
    }

    internal fun onNoSecondPassword() {
        showPaymentReview()
    }

    internal fun onSecondPasswordValidated(secondPassword: String) {
        verifiedSecondPassword = secondPassword
        showPaymentReview()
    }

    private fun showPaymentReview() {
        val paymentDetails = getConfirmationDetails()
        var allowFeeChange = true

        when (currencyState.cryptoCurrency) {
            CryptoCurrencies.BTC -> if (paymentDetails.isLargeTransaction) view.showLargeTransactionWarning()
            CryptoCurrencies.ETHER -> allowFeeChange = false
            else -> throw IllegalArgumentException("BCC is not currently supported")
        }

        view.showPaymentDetails(getConfirmationDetails(), null, allowFeeChange)
    }

    private fun checkManualAddressInput() {
        val address = view.getReceivingAddress()
        address?.let {
            //Input analytics
            checkClipboardPaste(address)

            //Only if valid address so we don't override with a label
            when (currencyState.cryptoCurrency) {
                CryptoCurrencies.BTC -> if (FormatsUtil.isValidBitcoinAddress(address)) pendingTransaction.receivingAddress = address
                CryptoCurrencies.ETHER -> if (FormatsUtil.isValidEthereumAddress(address)) pendingTransaction.receivingAddress = address
                else -> throw IllegalArgumentException("BCC is not currently supported")
            }
        }
    }

    private fun getConfirmationDetails(): PaymentConfirmationDetails {
        val pendingTransaction = pendingTransaction

        val details = PaymentConfirmationDetails()

        details.fromLabel = pendingTransaction.sendingObject.label
        details.toLabel = pendingTransaction.displayableReceivingLabel

        details.cryptoUnit = currencyHelper.cryptoUnit
        details.fiatUnit = currencyHelper.fiatUnit
        details.fiatSymbol = exchangeRateFactory.getSymbol(currencyHelper.fiatUnit)

        when (currencyState.cryptoCurrency) {
            CryptoCurrencies.BTC -> {
                details.isLargeTransaction = isLargeTransaction()
                details.btcSuggestedFee = currencyHelper.getTextFromSatoshis(absoluteSuggestedFee.toLong(), getDefaultDecimalSeparator())

                details.cryptoTotal = currencyHelper.getTextFromSatoshis(pendingTransaction.total.toLong(), getDefaultDecimalSeparator())
                details.cryptoAmount = currencyHelper.getTextFromSatoshis(pendingTransaction.bigIntAmount.toLong(), getDefaultDecimalSeparator())
                details.cryptoFee = currencyHelper.getTextFromSatoshis(pendingTransaction.bigIntFee.toLong(), getDefaultDecimalSeparator())

                details.fiatFee = monetaryUtil.getFiatFormat(currencyHelper.fiatUnit)
                        .format(currencyHelper.lastPrice * (pendingTransaction.bigIntFee.toDouble() / 1e8))
                details.fiatAmount = monetaryUtil.getFiatFormat(currencyHelper.fiatUnit)
                        .format(currencyHelper.lastPrice * (pendingTransaction.bigIntAmount.toDouble() / 1e8))
                details.fiatTotal = monetaryUtil.getFiatFormat(currencyHelper.fiatUnit)
                        .format(currencyHelper.lastPrice * (pendingTransaction.total.toDouble() / 1e8))
            }
            CryptoCurrencies.ETHER -> {

                var ethAmount = Convert.fromWei(pendingTransaction.bigIntAmount.toString(), Convert.Unit.ETHER)
                var ethFee = Convert.fromWei(pendingTransaction.bigIntFee.toString(), Convert.Unit.ETHER)

                ethAmount = ethAmount.setScale(8, RoundingMode.HALF_UP).stripTrailingZeros()
                ethFee = ethFee.setScale(8, RoundingMode.HALF_UP).stripTrailingZeros()

                val ethTotal = ethAmount.add(ethFee)

                details.cryptoAmount = ethAmount.toString()
                details.cryptoFee = ethFee.toString()
                details.cryptoTotal = ethTotal.toString()

                details.fiatFee = monetaryUtil.getFiatFormat(currencyHelper.fiatUnit)
                        .format(currencyHelper.lastPrice * (ethFee.toDouble()))
                details.fiatAmount = monetaryUtil.getFiatFormat(currencyHelper.fiatUnit)
                        .format(currencyHelper.lastPrice * (ethAmount.toDouble()))
                details.fiatTotal = monetaryUtil.getFiatFormat(currencyHelper.fiatUnit)
                        .format(currencyHelper.lastPrice * (ethTotal.toDouble()))
            }
            else -> throw IllegalArgumentException("BCC is not currently supported")
        }

        return details
    }

    private fun resetAccountList() {
        val list = getAddressList()
        if (list.size == 1) {
            view.hideReceivingDropdown()
            view.hideSendingFieldDropdown()
            setReceiveHint(list.size)
        } else {
            view.showSendingFieldDropdown()
            view.showReceivingDropdown()
            setReceiveHint(list.size)
        }
    }

    private fun clearReceivingAddress() {
        view.updateReceivingAddress("")
    }

    internal fun clearReceivingObject() {
        pendingTransaction.receivingObject = null
        metricInputFlag = null
    }

    private fun clearCryptoAmount() {
        view.updateCryptoAmount("")
    }

    private fun getAddressList(): List<ItemAccount> = walletAccountHelper.getAccountItems()

    private fun setReceiveHint(accountsCount: Int) {
        val hint: Int = if (accountsCount > 1) {
            when (currencyState.cryptoCurrency) {
                CryptoCurrencies.BTC -> R.string.to_field_helper
                CryptoCurrencies.ETHER -> R.string.eth_to_field_helper
                else -> throw IllegalArgumentException("BCC is not currently supported")
            }
        } else {
            when (currencyState.cryptoCurrency) {
                CryptoCurrencies.BTC -> R.string.to_field_helper_no_dropdown
                CryptoCurrencies.ETHER -> R.string.eth_to_field_helper_no_dropdown
                else -> throw IllegalArgumentException("BCC is not currently supported")
            }
        }

        view.updateReceivingHint(hint)
    }

    private fun updateCurrencyUnits() {
        view.updateFiatCurrency(currencyHelper.fiatUnit)

        when (currencyState.cryptoCurrency) {
            CryptoCurrencies.BTC -> view.updateCryptoCurrency(currencyHelper.btcUnit)
            CryptoCurrencies.ETHER -> view.updateCryptoCurrency(currencyHelper.ethUnit)
            else -> throw IllegalArgumentException("BCC is not currently supported")
        }
    }

    private fun selectDefaultSendingAccount() {
        val accountItem = walletAccountHelper.getDefaultAccount()
        view.updateSendingAddress(accountItem.label ?: accountItem.getAddressString())
        pendingTransaction.sendingObject = accountItem
    }

    internal fun selectSendingBtcAccount(accountPosition: Int) {
        if (accountPosition >= 0) {
            var label = getAddressList()[accountPosition].label
            if (label.isNullOrEmpty()) {
                label = getAddressList()[accountPosition].address
            }
            view.updateSendingAddress(label!!)
        } else {
            selectDefaultSendingAccount()
        }
    }

    internal fun getDefaultDecimalSeparator(): String =
            DecimalFormatSymbols.getInstance().decimalSeparator.toString()

    internal fun updateCryptoTextField(editable: Editable, editText: EditText) {
        val maxLength = 2
        val fiat = EditTextFormatUtil.formatEditable(editable,
                maxLength,
                editText,
                getDefaultDecimalSeparator()).toString()
        var amountString = ""

        if (!fiat.isEmpty()) {
            val fiatAmount = currencyHelper.getDoubleAmount(fiat)
            amountString = currencyHelper.getFormattedCryptoStringFromFiat(fiatAmount)
        }

        view.disableCryptoTextChangeListener()
        view.updateCryptoAmount(amountString)
        view.enableCryptoTextChangeListener()
    }

    internal fun updateFiatTextField(editable: Editable, editText: EditText) {
        val crypto = EditTextFormatUtil.formatEditable(editable,
                currencyHelper.maxCryptoDecimalLength,
                editText,
                getDefaultDecimalSeparator()).toString()

        var amountString = ""

        if (!crypto.isEmpty()) {
            val cd = currencyHelper.getDoubleAmount(crypto)
            amountString = currencyHelper.getFormattedFiatStringFromCrypto(cd)
        }

        view.disableFiatTextChangeListener()
        view.updateFiatAmount(amountString)
        view.enableFiatTextChangeListener()
    }

    /**
     * Get cached dynamic fee from new Fee options endpoint
     */
    private fun getSuggestedFee() {
        val observable = when (currencyState.cryptoCurrency) {
            CryptoCurrencies.BTC -> feeDataManager.btcFeeOptions
                    .doOnSubscribe { feeOptions = dynamicFeeCache.btcFeeOptions!! }
                    .doOnNext { dynamicFeeCache.btcFeeOptions = it }

            CryptoCurrencies.ETHER -> feeDataManager.ethFeeOptions
                    .doOnSubscribe { feeOptions = dynamicFeeCache.ethFeeOptions!! }
                    .doOnNext { dynamicFeeCache.ethFeeOptions = it }

            else -> throw IllegalArgumentException("BCC is not currently supported")
        }

        observable.compose(RxUtil.addObservableToCompositeDisposable(this))
                .subscribe(
                        { /* No-op */ },
                        {
                            Timber.e(it)
                            view.showSnackbar(R.string.confirm_payment_fee_sync_error, Snackbar.LENGTH_LONG)
                            view.finishPage()
                        }
                )
    }

    internal fun getBitcoinFeeOptions(): FeeOptions? = dynamicFeeCache.btcFeeOptions

    internal fun getFeeOptionsForDropDown(): List<DisplayFeeOptions> {
        val regular = DisplayFeeOptions(
                stringUtils.getString(R.string.fee_options_regular),
                stringUtils.getString(R.string.fee_options_regular_time)
        )
        val priority = DisplayFeeOptions(
                stringUtils.getString(R.string.fee_options_priority),
                stringUtils.getString(R.string.fee_options_priority_time)
        )
        val custom = DisplayFeeOptions(
                stringUtils.getString(R.string.fee_options_custom),
                stringUtils.getString(R.string.fee_options_custom_warning)
        )
        return listOf(regular, priority, custom)
    }

    private fun getFeePerKbFromPriority(@FeeType.FeePriorityDef feePriorityTemp: Int): BigInteger {
        getSuggestedFee()

        if (feeOptions == null) {
            // This is a stopgap in case of failure to prevent crashes.
            return BigInteger.ZERO
        }

        return when (feePriorityTemp) {
            FeeType.FEE_OPTION_CUSTOM -> BigInteger.valueOf(view.getCustomFeeValue() * 1000)
            FeeType.FEE_OPTION_PRIORITY -> BigInteger.valueOf(feeOptions!!.priorityFee * 1000)
            FeeType.FEE_OPTION_REGULAR -> BigInteger.valueOf(feeOptions!!.regularFee * 1000)
            else -> BigInteger.valueOf(feeOptions!!.regularFee * 1000)
        }
    }

    /**
     * Retrieves unspent api data in memory. If not in memory yet, it will be retrieved and added.
     */
    private fun getUnspentApiResponse(address: String): Observable<UnspentOutputs> {
        return if (payloadDataManager.getAddressBalance(address).toLong() > 0) {
            return if (unspentApiResponses.containsKey(address)) {
                Observable.just(unspentApiResponses[address])
            } else {
                sendDataManager.getUnspentOutputs(address)
            }
        } else {
            Observable.error(Throwable("No funds - skipping call to unspent API"))
        }
    }

    @Throws(UnsupportedEncodingException::class)
    private fun getSuggestedAbsoluteFee(
            coins: UnspentOutputs,
            amountToSend: BigInteger,
            feePerKb: BigInteger
    ): BigInteger {
        val spendableCoins = sendDataManager.getSpendableCoins(coins, amountToSend, feePerKb)
        return spendableCoins.absoluteFee
    }

    /**
     * Update absolute fee with smallest denomination of crypto currency (satoshi, wei, etc)
     */
    private fun updateFee(fee: BigInteger) {
        absoluteSuggestedFee = fee

        val cryptoPrice: String
        val fiatPrice: String

        when (currencyState.cryptoCurrency) {
            CryptoCurrencies.BTC -> {
                cryptoPrice = monetaryUtil.getDisplayAmount(absoluteSuggestedFee.toLong())
                fiatPrice = monetaryUtil.getFiatFormat(currencyHelper.fiatUnit)
                        .format(currencyHelper.lastPrice * (absoluteSuggestedFee.toDouble() / 1e8))
            }
            CryptoCurrencies.ETHER -> {
                val eth = Convert.fromWei(absoluteSuggestedFee.toString(), Convert.Unit.ETHER)
                cryptoPrice = eth.toString()
                fiatPrice = monetaryUtil.getFiatFormat(currencyHelper.fiatUnit)
                        .format(currencyHelper.lastPrice * (eth.toDouble()))
            }
            else -> throw IllegalArgumentException("BCC is not currently supported")
        }

        view.updateFeeAmount(
                "$cryptoPrice ${currencyHelper.cryptoUnit} ($fiatPrice${currencyHelper.fiatUnit})"
        )
    }

    private fun updateMaxAvailable(balanceAfterFee: BigInteger) {
        maxAvailable = balanceAfterFee
        view.showMaxAvailable()

        //Format for display
        if (!currencyState.isDisplayingCryptoCurrency) {
            val fiatBalance = currencyHelper.lastPrice * (Math.max(balanceAfterFee.toDouble(), 0.0) / 1e8)
            val fiatBalanceFormatted = monetaryUtil.getFiatFormat(currencyHelper.fiatUnit).format(fiatBalance)
            view.updateMaxAvailable("${stringUtils.getString(R.string.max_available)} $fiatBalanceFormatted ${currencyHelper.fiatUnit}")
        } else {
            val btcAmountFormatted = monetaryUtil.getBtcFormat().format(monetaryUtil.getDenominatedAmount(Math.max(balanceAfterFee.toDouble(), 0.0) / 1e8))
            view.updateMaxAvailable("${stringUtils.getString(R.string.max_available)} $btcAmountFormatted ${currencyHelper.cryptoUnit}")
        }

        if (balanceAfterFee <= Payment.DUST) {
            view.updateMaxAvailable(stringUtils.getString(R.string.insufficient_funds))
            view.updateMaxAvailableColor(R.color.product_red_medium)
        } else {
            view.updateMaxAvailableColor(R.color.primary_blue_accent)
        }
    }

    internal fun onCryptoTextChange(cryptoText: String) {
        textChangeSubject.onNext(cryptoText)
    }

    /**
     * Calculate amounts on crypto text change
     */
    private fun setupTextChangeSubject() {
        textChangeSubject.debounce(300, TimeUnit.MILLISECONDS)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext {
                    calculateSpendableAmounts(spendAll = false, amountToSendText = it)
                }
                .subscribe(IgnorableDefaultObserver())
    }

    internal fun onSpendMaxClicked() {
        calculateSpendableAmounts(spendAll = true, amountToSendText = null)
    }

    private fun calculateSpendableAmounts(spendAll: Boolean, amountToSendText: String?) {
        view.setSendButtonEnabled(true)
        view.hideMaxAvailable()
        view.clearWarning()

        val feePerKb = getFeePerKbFromPriority(view.getFeePriority())

        when (currencyState.cryptoCurrency) {
            CryptoCurrencies.BTC -> calculateUnspentBtc(spendAll, amountToSendText, feePerKb)
            CryptoCurrencies.ETHER -> getEthAccountResponse(spendAll, amountToSendText)
            else -> throw IllegalArgumentException("BCC is not currently supported")
        }
    }

    private fun calculateUnspentBtc(
            spendAll: Boolean,
            amountToSendText: String?,
            feePerKb: BigInteger
    ) {

        val address = pendingTransaction.sendingObject.getAddressString()

        getUnspentApiResponse(address)
                .debounce(200, TimeUnit.MILLISECONDS)
                .compose(RxUtil.applySchedulersToObservable<UnspentOutputs>())
                .subscribe(
                        { coins ->
                            val amountToSend = currencyHelper.getSatoshisFromText(amountToSendText, getDefaultDecimalSeparator())

                            // Future use. There might be some unconfirmed funds. Not displaying a warning currently (to line up with iOS and Web wallet)
                            if (coins.notice != null)
                                view.updateWarning(coins.notice)
                            else
                                view.clearWarning()

                            updateFee(getSuggestedAbsoluteFee(coins, amountToSend, feePerKb))

                            suggestedFeePayment(coins, amountToSend, spendAll, feePerKb)
                        },
                        { throwable ->
                            Timber.e(throwable)
                            // No unspent outputs
                            updateMaxAvailable(BigInteger.ZERO)
                            updateFee(BigInteger.ZERO)
                            pendingTransaction.unspentOutputBundle = null
                        })
    }

    /**
     * Payment will use suggested dynamic fee
     */
    @Throws(UnsupportedEncodingException::class)
    private fun suggestedFeePayment(
            coins: UnspentOutputs,
            amountToSend: BigInteger,
            spendAll: Boolean,
            feePerKb: BigInteger
    ) {
        var amount = amountToSend

        //Calculate sweepable amount to display max available
        val sweepBundle = sendDataManager.getSweepableCoins(coins, feePerKb)
        val sweepableAmount = sweepBundle.left

        updateMaxAvailable(sweepableAmount)

        if (spendAll) {
            amount = sweepableAmount
            view?.updateCryptoAmount(currencyHelper.getTextFromSatoshis(
                    sweepableAmount.toLong(),
                    getDefaultDecimalSeparator()
            ))
        }

        val unspentOutputBundle = sendDataManager.getSpendableCoins(coins,
                amount,
                feePerKb)

        pendingTransaction.bigIntAmount = amount
        pendingTransaction.unspentOutputBundle = unspentOutputBundle
        pendingTransaction.bigIntFee = pendingTransaction.unspentOutputBundle.absoluteFee
    }

    private fun getEthAccountResponse(spendAll: Boolean, amountToSendText: String?) {
        view.showMaxAvailable()

        if (ethDataManager.getEthResponseModel() == null) {
            ethDataManager.fetchEthAddress()
                    .compose(RxUtil.addObservableToCompositeDisposable(this))
                    .doOnError { view.showSnackbar(R.string.api_fail, Snackbar.LENGTH_INDEFINITE) }
                    .subscribe { calculateUnspentEth(it, spendAll, amountToSendText) }
        } else {
            ethDataManager.getEthResponseModel()?.let {
                calculateUnspentEth(it, spendAll, amountToSendText)
            }
        }
    }

    private fun calculateUnspentEth(
            combinedEthModel: CombinedEthModel,
            spendAll: Boolean,
            amountToSendText: String?
    ) {

        val gwei = BigDecimal.valueOf(feeOptions!!.gasLimit * feeOptions!!.regularFee)
        val wei = Convert.toWei(gwei, Convert.Unit.GWEI)

        updateFee(wei.toBigInteger())
        pendingTransaction.bigIntFee = wei.toBigInteger()

        val addressResponse = combinedEthModel.getAddressResponse()
        maxAvailable = addressResponse!!.balance.minus(wei.toBigInteger())
        maxAvailable = maxAvailable.max(BigInteger.ZERO)

        val availableEth = Convert.fromWei(maxAvailable.toString(), Convert.Unit.ETHER)
        if (spendAll) {
            view?.updateCryptoAmount(currencyHelper.getFormattedEthString(availableEth))
            pendingTransaction.bigIntAmount = availableEth.toBigInteger()
        } else {
            pendingTransaction.bigIntAmount =
                    currencyHelper.getWeiFromText(amountToSendText, getDefaultDecimalSeparator())
        }

        //Format for display
        if (!currencyState.isDisplayingCryptoCurrency) {
            val fiatBalance = currencyHelper.lastPrice * availableEth.toLong()

            val fiatBalanceFormatted = monetaryUtil.getFiatFormat(currencyHelper.fiatUnit).format(fiatBalance)
            view.updateMaxAvailable("${stringUtils.getString(R.string.max_available)} $fiatBalanceFormatted ${currencyHelper.fiatUnit}")
        } else {
            val number = currencyHelper.getFormattedEthString(availableEth)
            view.updateMaxAvailable("${stringUtils.getString(R.string.max_available)} $number")
        }

        // No dust in Ethereum
        if (maxAvailable <= BigInteger.ZERO) {
            view.updateMaxAvailable(stringUtils.getString(R.string.insufficient_funds))
            view.updateMaxAvailableColor(R.color.product_red_medium)
        } else {
            view.updateMaxAvailableColor(R.color.primary_blue_accent)
        }

        //Check if any pending ether txs exist and warn user
        checkForUnconfirmedTx()
    }

    @Suppress("CascadeIf")
    internal fun handleURIScan(untrimmedscanData: String?, scanRoute: String) {
        if (untrimmedscanData == null) return

        metricInputFlag = scanRoute

        var scanData = untrimmedscanData.trim { it <= ' ' }
                .replace("ethereum:", "")
        val address: String
        var amount: String?

        scanData = FormatsUtil.getURIFromPoorlyFormedBIP21(scanData)

        if (FormatsUtil.isValidBitcoinAddress(scanData)) {
            onBitcoinChosen()
            address = scanData
        } else if (FormatsUtil.isBitcoinUri(scanData)) {
            onBitcoinChosen()
            address = FormatsUtil.getBitcoinAddress(scanData)
            amount = FormatsUtil.getBitcoinAmount(scanData)

            // QR scan comes in as BTC - set current btc unit
            prefsUtil.setValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)

            //Convert to correct units
            try {
                amount = monetaryUtil.getDisplayAmount(amount.toLong())
                view?.updateCryptoAmount(amount)
                val fiat = currencyHelper.getFormattedFiatStringFromCrypto(amount.toDouble())
                view?.updateFiatAmount(fiat)
            } catch (e: Exception) {
                //ignore
            }
        } else if (FormatsUtil.isValidEthereumAddress(scanData)) {
            onEtherChosen()
            address = scanData
            view?.updateCryptoAmount("")
        } else {
            view.showSnackbar(R.string.invalid_bitcoin_address, Snackbar.LENGTH_LONG)
            return
        }

        if (address != "") {
            pendingTransaction.receivingObject = null
            pendingTransaction.receivingAddress = address
            view.updateReceivingAddress(address)
        }
    }

    internal fun handlePrivxScan(scanData: String?) {
        if (scanData == null) return

        val format = privateKeyFactory.getFormat(scanData)

        if (format == null) {
            view?.showSnackbar(R.string.privkey_error, Snackbar.LENGTH_LONG)
        }

        when (format) {
            PrivateKeyFactory.BIP38 -> view?.showBIP38PassphrasePrompt(scanData)//BIP38 needs passphrase
            else -> spendFromWatchOnlyNonBIP38(format, scanData)
        }
    }

    private fun spendFromWatchOnlyNonBIP38(format: String, scanData: String) {
        try {
            val key = privateKeyFactory.getKey(format, scanData)
            val legacyAddress = pendingTransaction.sendingObject.accountObject as LegacyAddress
            setTempLegacyAddressPrivateKey(legacyAddress, key)

        } catch (e: Exception) {
            view?.showSnackbar(R.string.no_private_key, Snackbar.LENGTH_LONG)
            Timber.e(e)
        }
    }

    internal fun spendFromWatchOnlyBIP38(pw: String, scanData: String) {
        sendDataManager.getEcKeyFromBip38(pw, scanData, environmentSettings.networkParameters)
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .subscribe({
                    val legacyAddress = pendingTransaction.sendingObject.accountObject as LegacyAddress
                    setTempLegacyAddressPrivateKey(legacyAddress, it)
                }) { view?.showSnackbar(R.string.bip38_error, Snackbar.LENGTH_LONG) }
    }

    private fun setTempLegacyAddressPrivateKey(legacyAddress: LegacyAddress, key: ECKey?) {
        if (key != null && key.hasPrivKey() && legacyAddress.address == key.toAddress(
                environmentSettings.networkParameters).toString()) {

            //Create copy, otherwise pass by ref will override private key in wallet payload
            val tempLegacyAddress = LegacyAddress()
            tempLegacyAddress.setPrivateKeyFromBytes(key.privKeyBytes)
            tempLegacyAddress.address = key.toAddress(environmentSettings.networkParameters).toString()
            tempLegacyAddress.label = legacyAddress.label
            tempLegacyAddress.tag = PendingTransaction.WATCH_ONLY_SPEND_TAG
            pendingTransaction.sendingObject.accountObject = tempLegacyAddress

            showPaymentReview()
        } else {
            view?.showSnackbar(R.string.invalid_private_key, Snackbar.LENGTH_LONG)
        }
    }

    private fun onSendingBtcLegacyAddressSelected(legacyAddress: LegacyAddress) {
        pendingTransaction.sendingObject = ItemAccount(
                legacyAddress.label,
                null,
                null,
                null,
                legacyAddress,
                legacyAddress.address
        )

        var label = legacyAddress.label
        if (label.isNullOrEmpty()) {
            label = legacyAddress.address
        }
        view.updateSendingAddress(label)
        calculateSpendableAmounts(false, "0")
    }

    private fun onSendingBtcAccountSelected(account: Account) {
        pendingTransaction.sendingObject = ItemAccount(
                account.label,
                null,
                null,
                null,
                account,
                null
        )

        var label = account.label
        if (label.isNullOrEmpty()) {
            label = account.xpub
        }

        view.updateSendingAddress(label)
        calculateSpendableAmounts(false, "0")
    }

    private fun onReceivingBtcLegacyAddressSelected(legacyAddress: LegacyAddress) {
        pendingTransaction.receivingObject = ItemAccount(
                legacyAddress.label,
                null,
                null,
                null,
                legacyAddress,
                legacyAddress.address
        )
        pendingTransaction.receivingAddress = legacyAddress.address

        var label = legacyAddress.label
        if (label.isNullOrEmpty()) {
            label = legacyAddress.address
        }
        view.updateReceivingAddress(label)

        if (legacyAddress.isWatchOnly && shouldWarnWatchOnly()) {
            view.showWatchOnlyWarning(legacyAddress.address)
        }
    }

    private fun shouldWarnWatchOnly(): Boolean =
            prefsUtil.getValue(PREF_WARN_WATCH_ONLY_SPEND, true)

    internal fun setWarnWatchOnlySpend(warn: Boolean) {
        prefsUtil.setValue(PREF_WARN_WATCH_ONLY_SPEND, warn)
    }

    private fun onReceivingAccountSelected(account: Account) {
        pendingTransaction.receivingObject = ItemAccount(
                account.label,
                null,
                null,
                null,
                account,
                null
        )

        var label = account.label
        if (label.isNullOrEmpty()) {
            label = account.xpub
        }
        view.updateReceivingAddress(label)

        payloadDataManager.getNextReceiveAddress(account)
                .doOnNext { pendingTransaction.receivingAddress = it }
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .subscribe({
                    /* No-op */
                }, { view.showSnackbar(R.string.unexpected_error, Snackbar.LENGTH_LONG) })
    }

    internal fun selectSendingAccount(data: Intent?) {
        
        try {
            val type: Class<*> = Class.forName(data?.getStringExtra(AccountChooserActivity.EXTRA_SELECTED_OBJECT_TYPE))
            val any = ObjectMapper().readValue(data?.getStringExtra(AccountChooserActivity.EXTRA_SELECTED_ITEM), type)

            when (any) {
                is LegacyAddress -> onSendingBtcLegacyAddressSelected(any)
                is Account -> onSendingBtcAccountSelected(any)
                else -> throw IllegalArgumentException("No method for handling $type available")
            }
        } catch (e: ClassNotFoundException) {
            Timber.e(e)
            selectDefaultSendingAccount()
        } catch (e: IOException) {
            Timber.e(e)
            selectDefaultSendingAccount()
        }
    }

    internal fun selectReceivingAccount(data: Intent?) {
        try {
            val type: Class<*> = Class.forName(data?.getStringExtra(AccountChooserActivity.EXTRA_SELECTED_OBJECT_TYPE))
            val any = ObjectMapper().readValue(data?.getStringExtra(AccountChooserActivity.EXTRA_SELECTED_ITEM), type)

            when (any) {
                is LegacyAddress -> onReceivingBtcLegacyAddressSelected(any)
                is Account -> onReceivingAccountSelected(any)
                else -> throw IllegalArgumentException("No method for handling $type available")
            }
        } catch (e: ClassNotFoundException) {
            Timber.e(e)
        } catch (e: IOException) {
            Timber.e(e)
        }
    }

    private fun updateTicker() {
        exchangeRateFactory.updateTickers()
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
                .compose(RxUtil.applySchedulersToCompletable())
                .subscribe({
                    //no-op
                }, { it.printStackTrace() })
    }

    private fun checkClipboardPaste(address: String) {
        val contents = view.getClipboardContents()
        if (contents != null && contents == address) {
            metricInputFlag = EventService.EVENT_TX_INPUT_FROM_PASTE
        }
    }

    private fun isValidBitcoinAmount(bAmount: BigInteger?): Boolean {
        if (bAmount == null) {
            return false
        }

        // Test that amount is more than dust
        if (bAmount.compareTo(Payment.DUST) == -1) {
            return false
        }

        // Test that amount does not exceed btc limit
        if (bAmount.compareTo(BigInteger.valueOf(2_100_000_000_000_000L)) == 1) {
            clearCryptoAmount()
            return false
        }

        // Test that amount is not zero
        return bAmount >= BigInteger.ZERO
    }

    private fun validateBitcoinTransaction(): Pair<Boolean, Int> {
        var validated = true
        var errorMessage = R.string.unexpected_error

        if (pendingTransaction.receivingAddress == null || !FormatsUtil.isValidBitcoinAddress(pendingTransaction.receivingAddress)) {
            errorMessage = R.string.invalid_bitcoin_address
            validated = false

        } else if (pendingTransaction.bigIntAmount == null || !isValidBitcoinAmount(pendingTransaction.bigIntAmount)) {
            errorMessage = R.string.invalid_amount
            validated = false

        } else if (pendingTransaction.unspentOutputBundle == null || pendingTransaction.unspentOutputBundle.spendableOutputs == null) {
            errorMessage = R.string.no_confirmed_funds
            validated = false

        } else if (maxAvailable == null || maxAvailable.compareTo(pendingTransaction.bigIntAmount) == -1) {
            errorMessage = R.string.insufficient_funds
            validated = false

        } else if (pendingTransaction.unspentOutputBundle.spendableOutputs.isEmpty()) {
            errorMessage = R.string.insufficient_funds
            validated = false
        }

        return Pair.of(validated, errorMessage)
    }

    private fun isValidEtherAmount(bAmount: BigInteger?): Boolean {
        return (bAmount != null && bAmount >= BigInteger.ZERO)
    }

    private fun validateEtherTransaction(): Observable<Pair<Boolean, Int>> {
        if (pendingTransaction.receivingAddress == null) {
            return Observable.just(Pair.of(false, R.string.eth_invalid_address))
        } else {
            return Observable.zip(
                    ethDataManager.getIfContract(pendingTransaction.receivingAddress),
                    ethDataManager.hasUnconfirmedEthTransactions(),
                    BiFunction { isContract, hasUnconfirmedTransactions ->
                        var validated = true
                        var errorMessage = R.string.unexpected_error

                        //Validate not contract
                        if (isContract) {
                            errorMessage = R.string.eth_support_contract_not_allowed
                            validated = false
                        }

                        //Validate address
                        if (pendingTransaction.receivingAddress == null || !FormatsUtil.isValidEthereumAddress(pendingTransaction.receivingAddress)) {
                            errorMessage = R.string.eth_invalid_address
                            validated = false
                        }

                        //Validate amount
                        if (!isValidEtherAmount(pendingTransaction.bigIntAmount) || pendingTransaction.bigIntAmount <= BigInteger.ZERO) {
                            errorMessage = R.string.invalid_amount
                            validated = false
                        }

                        // Validate sufficient funds
                        if (maxAvailable.compareTo(pendingTransaction.bigIntAmount) == -1) {
                            errorMessage = R.string.insufficient_funds
                            validated = false
                        }

                        //Validate address does not have unconfirmed funds
                        if (hasUnconfirmedTransactions) {
                            errorMessage = R.string.eth_unconfirmed_wait
                            validated = false
                        }

                        Pair.of(validated, errorMessage)
                    })
        }
    }

    private fun checkForUnconfirmedTx() {
        ethDataManager.hasUnconfirmedEthTransactions()
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .subscribe(
                        { unconfirmed ->
                            view?.setSendButtonEnabled(!unconfirmed)
                            if (unconfirmed) {
                                view?.disableInput()
                                view?.updateMaxAvailable(stringUtils.getString(R.string.eth_unconfirmed_wait))
                                view?.updateMaxAvailableColor(R.color.product_red_medium)
                            } else {
                                view.enableInput()
                            }
                        },
                        { Timber.e(it) })
    }

    /**
     * Returns true if bitcoin transaction is large by checking against 3 criteria:
     *
     * If the fee > $0.50
     * If the Tx size is over 1kB
     * If the ratio of fee/amount is over 1%
     */
    private fun isLargeTransaction(): Boolean {
        val valueString = monetaryUtil.getFiatFormat("USD")
                .format(exchangeRateFactory.getLastBtcPrice("USD") * absoluteSuggestedFee.toDouble() / 1e8)
        val usdValue = currencyHelper.stripSeparator(valueString, getDefaultDecimalSeparator()).toDouble()
        val txSize = sendDataManager.estimateSize(pendingTransaction.unspentOutputBundle.spendableOutputs.size, 2)//assume change
        val relativeFee = absoluteSuggestedFee.toDouble() / pendingTransaction.bigIntAmount.toDouble() * 100.0

        return usdValue > SendModel.LARGE_TX_FEE
                && txSize > SendModel.LARGE_TX_SIZE
                && relativeFee > SendModel.LARGE_TX_PERCENTAGE
    }

    internal fun disableAdvancedFeeWarning() {
        prefsUtil.setValue(PrefsUtil.KEY_WARN_ADVANCED_FEE, false)
    }

    internal fun shouldShowAdvancedFeeWarning(): Boolean {
        return prefsUtil.getValue(PrefsUtil.KEY_WARN_ADVANCED_FEE, true)
    }

    companion object {

        private val PREF_WARN_WATCH_ONLY_SPEND = "pref_warn_watch_only_spend"

    }
}