package piuk.blockchain.android.ui.send.strategy

import android.annotation.SuppressLint
import com.google.android.material.snackbar.Snackbar
import com.blockchain.swap.nabu.models.nabu.NabuApiException
import com.blockchain.swap.nabu.models.nabu.NabuErrorCodes
import com.blockchain.swap.nabu.models.nabu.State
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.SendAnalytics
import com.blockchain.swap.nabu.NabuToken
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.serialization.JsonSerializableAccount
import com.blockchain.swap.nabu.datamanagers.NabuDataManager
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.api.data.FeeOptions
import info.blockchain.wallet.ethereum.EthereumAccount
import info.blockchain.wallet.ethereum.data.Erc20AddressResponse
import info.blockchain.wallet.util.FormatsUtil
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import org.web3j.crypto.RawTransaction
import org.web3j.utils.Convert
import piuk.blockchain.android.R
import piuk.blockchain.android.data.cache.DynamicFeeCache
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.account.PaymentConfirmationDetails
import piuk.blockchain.android.ui.account.PitAccount
import piuk.blockchain.android.ui.receive.WalletAccountHelper
import piuk.blockchain.android.ui.send.SendView
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.android.data.currency.CurrencyState
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.ethereum.models.CombinedEthModel
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import piuk.blockchain.androidcore.utils.extensions.emptySubscribe
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.concurrent.TimeUnit

class PaxSendStrategy(
    private val walletAccountHelper: WalletAccountHelper,
    private val payloadDataManager: PayloadDataManager,
    private val ethDataManager: EthDataManager,
    private val paxAccount: Erc20Account,
    private val dynamicFeeCache: DynamicFeeCache,
    private val feeDataManager: FeeDataManager,
    private val exchangeRates: ExchangeRateDataManager,
    private val stringUtils: StringUtils,
    private val nabuDataManager: NabuDataManager,
    private val nabuToken: NabuToken,
    private val pitLinking: PitLinking,
    private val analytics: Analytics,
    prefs: CurrencyPrefs,
    currencyState: CurrencyState,
    environmentConfig: EnvironmentConfig
) : SendStrategy<SendView>(currencyState, prefs) {

    override fun onViewAttached() {}
    override fun onViewDetached() {}

    override val alwaysDisableScreenshots = false
    override val enableLogoutTimer = false

    private var pitAccount: PitAccount? = null

    override fun onPitAddressSelected() {
        pitAccount?.let {
            pendingTx.receivingAddress = it.address
            view?.updateReceivingAddress(it.label)
        }
    }

    override fun onPitAddressCleared() {
        pendingTx.receivingAddress = ""
        view?.updateReceivingAddress("")
    }

    private val walletName = stringUtils.getString(R.string.pax_wallet_name_1)

    override fun onCurrencySelected() {
        currencyState.cryptoCurrency = CryptoCurrency.PAX
        setupUiForPax()
    }

    private var pendingTx: PendingPaxTx = PendingPaxTx(walletName)

    private val networkParameters = environmentConfig.bitcoinNetworkParameters

    private var feeOptions: FeeOptions? = null
    private var textChangeSubject = PublishSubject.create<String>()
    private var absoluteSuggestedFee = BigInteger.ZERO
    private var maxEthAvailable = BigInteger.ZERO
    private var maxPaxAvailable = BigInteger.ZERO
    private var verifiedSecondPassword: String? = null

    override fun onBroadcastReceived() {
        resetAccountList()
    }

    override fun onViewReady() {
        resetAccountList()
        setupTextChangeSubject()
    }

    override fun onResume() {
        setupUiForPax()
    }

    private fun setupUiForPax() {
        view?.let {
            reset()
            it.hideFeePriority()
            it.setFeePrioritySelection(0)
            it.disableFeeDropdown()
            it.setCryptoMaxLength(30)
        }
    }

    override fun reset() {
        super.reset()

        pendingTx = PendingPaxTx(walletName)
        absoluteSuggestedFee = BigInteger.ZERO

        view?.let {
            resetAccountList()
            selectDefaultOrFirstFundedSendingAccount()
        }
    }

    private fun resetAccountList() {
        compositeDisposable += pitLinking.isPitLinked().filter { it }.flatMapSingle { nabuToken.fetchNabuToken() }
            .flatMap {
                nabuDataManager.fetchCryptoAddressFromThePit(it, CryptoCurrency.PAX)
            }.applySchedulers().doOnSubscribe {
                view?.updateReceivingHintAndAccountDropDowns(CryptoCurrency.PAX, 1, false)
            }.subscribeBy(onError = {
                view?.updateReceivingHintAndAccountDropDowns(CryptoCurrency.PAX, 1,
                    it is NabuApiException && it.getErrorCode() == NabuErrorCodes.Bad2fa) {
                    view?.show2FANotAvailableError()
                }
            }) {
                pitAccount = PitAccount(
                    stringUtils.getFormattedString(
                        R.string.exchange_default_account_label,
                        CryptoCurrency.PAX.displayTicker
                    ),
                    it.address
                )
                view?.updateReceivingHintAndAccountDropDowns(CryptoCurrency.PAX,
                    1,
                    it.state == State.ACTIVE && it.address.isNotEmpty()) { view?.fillOrClearAddress() }
            }
    }

    override fun processURIScanAddress(address: String) {
        pendingTx.receivingAddress = address
        view?.updateReceivingAddress(address)
    }

    override fun isAddressValid(address: String): Boolean =
        FormatsUtil.isValidEthereumAddress(address)

    @SuppressLint("CheckResult")
    override fun onContinueClicked() {

        checkManualAddressInput()

        compositeDisposable += validateTransaction()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { view?.showProgressDialog(R.string.app_name) }
            .doAfterTerminate { view?.dismissProgressDialog() }
            .doOnError { Timber.e(it) }
            .subscribeBy(
                onNext = { (validated, errorMessage) ->
                    when {
                        validated -> view?.showSecondPasswordDialog()
                        errorMessage == R.string.insufficient_eth_for_fees -> view?.showInsufficientGasDlg(
                            CryptoCurrency.PAX)
                        else -> view?.showSnackbar(errorMessage, Snackbar.LENGTH_LONG)
                    }
                },
                onError = {
                    view?.showSnackbar(R.string.unexpected_error, Snackbar.LENGTH_LONG)
                    view?.finishPage()
                }
            )
    }

    /**
     * Executes transaction
     */
    @SuppressLint("CheckResult")
    override fun submitPayment() {
        compositeDisposable += createPaxTransaction()
            .flatMap {
                if (payloadDataManager.isDoubleEncrypted) {
                    payloadDataManager.decryptHDWallet(networkParameters, verifiedSecondPassword)
                }

                val ecKey = EthereumAccount.deriveECKey(
                    payloadDataManager.wallet!!.hdWallets[0].masterKey,
                    0
                )
                return@flatMap ethDataManager.signEthTransaction(it, ecKey)
            }
            .flatMap { ethDataManager.pushEthTx(it) }
            .flatMap { ethDataManager.setLastTxHashObservable(it, System.currentTimeMillis()) }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { view?.showProgressDialog(R.string.app_name) }
            .doOnError { view?.showSnackbar(R.string.transaction_failed, Snackbar.LENGTH_LONG) }
            .doOnTerminate {
                view?.dismissProgressDialog()
                view?.dismissConfirmationDialog()
            }
            .subscribe(
                {
                    logPaymentSentEvent(true, CryptoCurrency.PAX, pendingTx.amountPax)

                    analytics.logEvent(SendAnalytics.SummarySendSuccess(CryptoCurrency.PAX))
                    handleSuccessfulPayment(it)
                },
                {
                    Timber.e(it)
                    logPaymentSentEvent(false, CryptoCurrency.PAX, pendingTx.amountPax)
                    view?.showSnackbar(R.string.transaction_failed, Snackbar.LENGTH_LONG)
                    analytics.logEvent(SendAnalytics.SummarySendFailure(CryptoCurrency.PAX))
                }
            )
    }

    private fun createPaxTransaction(): Observable<RawTransaction> {
        val feeGwei = BigDecimal.valueOf(feeOptions!!.regularFee)
        val feeWei = Convert.toWei(feeGwei, Convert.Unit.GWEI)

        return ethDataManager.fetchEthAddress()
            .map { ethDataManager.getEthResponseModel()!!.getNonce() }
            .map {
                paxAccount.createTransaction(
                    nonce = it,
                    to = pendingTx.receivingAddress,
                    contractAddress = ethDataManager.getErc20TokenData(CryptoCurrency.PAX).contractAddress,
                    gasPriceWei = feeWei.toBigInteger(),
                    gasLimitGwei = BigInteger.valueOf(feeOptions!!.gasLimitContract),
                    amount = pendingTx.amountPax
                )
            }
    }

    private fun handleSuccessfulPayment(hash: String): String {
        view?.showTransactionSuccess(CryptoCurrency.PAX)
        pendingTx = PendingPaxTx(walletName)
        return hash
    }

    override fun onNoSecondPassword() {
        showPaymentReview()
    }

    override fun onSecondPasswordValidated(secondPassword: String) {
        verifiedSecondPassword = secondPassword
        showPaymentReview()
    }

    private fun showPaymentReview() {
        view?.showPaymentDetails(getConfirmationDetails(), null, null, false)
    }

    private fun checkManualAddressInput() {
        val address = view?.getReceivingAddress() ?: return

        // Only if valid address so we don't override with a label
        if (FormatsUtil.isValidEthereumAddress(address)) {
            pendingTx.receivingAddress = address
        }
    }

    private fun getConfirmationDetails(): PaymentConfirmationDetails {
        val tx = pendingTx

        val paxValue = CryptoValue.fromMinor(CryptoCurrency.PAX, pendingTx.amountPax)
        var paxAmount = paxValue.toBigDecimal()
        paxAmount = paxAmount.setScale(8, RoundingMode.HALF_UP).stripTrailingZeros()
        val fiatValue = paxValue.toFiat(exchangeRates, fiatCurrency)
        var ethFeeValue = Convert.fromWei(pendingTx.feeEth.toString(), Convert.Unit.ETHER)
        ethFeeValue = ethFeeValue.setScale(8, RoundingMode.HALF_UP).stripTrailingZeros()
        val fiatFeeValue = CryptoValue.fromMinor(
            CryptoCurrency.ETHER, (pendingTx.feeEth)
        ).toFiat(exchangeRates, fiatCurrency)

        return PaymentConfirmationDetails(
            fromLabel = tx.sendingAccountLabel,
            toLabel = tx.receivingAddress,
            crypto = CryptoCurrency.PAX,
            cryptoAmount = paxAmount.toString(),
            fiatAmount = fiatValue.toStringWithoutSymbol(),
            fiatUnit = fiatCurrency,
            cryptoFee = ethFeeValue.toString(),
            fiatFee = fiatFeeValue.toStringWithoutSymbol(),
            showCryptoTotal = false,
            fiatTotal = (fiatFeeValue + fiatValue).toStringWithSymbol()
        ).apply {
            cryptoFeeUnit = CryptoCurrency.ETHER.displayTicker
        }
    }

    override fun clearReceivingObject() { /* no-op : no transfers in ETH/PAX */
    }

    override fun selectSendingAccount(account: JsonSerializableAccount?) {
        throw IllegalArgumentException("Multiple accounts not supported for PAX")
    }

    override fun selectReceivingAccount(account: JsonSerializableAccount?) {
        throw IllegalArgumentException("Multiple accounts not supported for PAX")
    }

    override fun selectDefaultOrFirstFundedSendingAccount() {
        view?.updateSendingAddress(pendingTx.sendingAccountLabel)

        val accountItem = walletAccountHelper.getDefaultOrFirstFundedAccount(CryptoCurrency.ETHER)
        pendingTx.sendingObject = accountItem
    }

    /**
     * Get cached dynamic fee from new Fee options endpoint
     */
    @SuppressLint("CheckResult")
    private fun getSuggestedFee() {
        compositeDisposable += feeDataManager.ethFeeOptions
            .doOnSubscribe { feeOptions = dynamicFeeCache.ethFeeOptions!! }
            .doOnNext { dynamicFeeCache.ethFeeOptions = it }
            .subscribe(
                { /* No-op */ },
                {
                    Timber.e(it)
                    view?.showSnackbar(R.string.confirm_payment_fee_sync_error, Snackbar.LENGTH_LONG)
                    view?.finishPage()
                }
            )
    }

    override fun getFeeOptions(): FeeOptions? = dynamicFeeCache.ethFeeOptions

    /**
     * Update absolute fee with smallest denomination of crypto currency (satoshi, wei, etc)
     */
    private fun updateFee(fee: BigInteger) {
        absoluteSuggestedFee = fee

        val cryptoValue = CryptoValue(CryptoCurrency.ETHER, absoluteSuggestedFee)
        view?.updateFeeAmount(cryptoValue, cryptoValue.toFiat(exchangeRates, fiatCurrency))
    }

    override fun onCryptoTextChange(cryptoText: String) {
        textChangeSubject.onNext(cryptoText)
    }

    override fun onAddressTextChange(address: String) {}

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
            .emptySubscribe()
    }

    override fun onSpendMaxClicked() {
        calculateSpendableAmounts(spendAll = true, amountToSendText = "")
    }

    private fun calculateSpendableAmounts(spendAll: Boolean, amountToSendText: String) {
        view?.setSendButtonEnabled(true)
        view?.hideMaxAvailable()
        view?.clearWarning()

        getSuggestedFee()

        getEthAccountBalance()
        getPaxAccountBalance(spendAll, amountToSendText)

        // Check if any pending ether txs exist and warn user
        compositeDisposable += isLastTxPending()
            .subscribeBy(
                onSuccess = { /* No-op */ },
                onError = { Timber.e(it) }
            )
    }

    @SuppressLint("CheckResult")
    private fun getEthAccountBalance() {
        view?.showMaxAvailable()

        if (ethDataManager.getEthResponseModel() == null) {
            compositeDisposable += ethDataManager.fetchEthAddress()
                .doOnError { view?.showSnackbar(R.string.api_fail, Snackbar.LENGTH_INDEFINITE) }
                .subscribe { calculateUnspentEth(it) }
        } else {
            ethDataManager.getEthResponseModel()?.let {
                calculateUnspentEth(it)
            }
        }
    }

    private fun calculateUnspentEth(combinedEthModel: CombinedEthModel) {
        val gwei = BigDecimal.valueOf(feeOptions!!.gasLimitContract * feeOptions!!.regularFee)
        val wei = Convert.toWei(gwei, Convert.Unit.GWEI)

        updateFee(wei.toBigInteger())
        pendingTx.feeEth = wei.toBigInteger()

        val addressResponse = combinedEthModel.getAddressResponse()
        maxEthAvailable = addressResponse!!.balance
        maxEthAvailable = maxEthAvailable.max(BigInteger.ZERO)
    }

    @SuppressLint("CheckResult")
    private fun getPaxAccountBalance(spendAll: Boolean, amountToSendText: String) {
        compositeDisposable += ethDataManager.getErc20Address(CryptoCurrency.PAX)
            .doOnError { view?.showSnackbar(R.string.api_fail, Snackbar.LENGTH_INDEFINITE) }
            .subscribe { calculateUnspentPax(it, spendAll, amountToSendText) }
    }

    private fun calculateUnspentPax(response: Erc20AddressResponse, spendAll: Boolean, amountToSendText: String) {

        val amountToSendSanitised = if (amountToSendText.isEmpty()) "0" else amountToSendText

        maxPaxAvailable = response.balance
        maxPaxAvailable = maxPaxAvailable.max(BigInteger.ZERO)

        val availablePax = maxPaxAvailable
        val cryptoValue = CryptoValue.fromMinor(CryptoCurrency.PAX, availablePax)

        if (spendAll) {
            view?.updateCryptoAmount(cryptoValue)
            pendingTx.amountPax = availablePax
        } else {
            pendingTx.amountPax = getPaxMinorFromText(amountToSendSanitised, getDefaultDecimalSeparator())
        }

        // Format for display
        val number = cryptoValue.toStringWithSymbol()
        view?.updateMaxAvailable("${stringUtils.getString(R.string.max_available)} $number")

        // No dust in Ethereum
        if (maxPaxAvailable <= BigInteger.ZERO) {
            view?.updateMaxAvailable(stringUtils.getString(R.string.insufficient_funds))
            view?.updateMaxAvailableColor(R.color.product_red_medium)
        } else {
            view?.updateMaxAvailableColor(R.color.primary_blue_accent)
        }
    }

    override fun handlePrivxScan(scanData: String?) {}

    override fun spendFromWatchOnlyBIP38(pw: String, scanData: String) {}

    private fun validateTransaction(): Observable<Pair<Boolean, Int>> {
        return if (pendingTx.receivingAddress.isEmpty()) {
            Observable.just(Pair(false, R.string.pax_invalid_address_1))
        } else {
            ethDataManager.getIfContract(pendingTx.receivingAddress)
                .map { isContract ->
                    var validated = true
                    var errorMessage = R.string.unexpected_error

                    // Validate not contract
                    if (isContract) {
                        errorMessage = R.string.eth_support_contract_not_allowed
                        validated = false
                    } else {
                        // Validate address
                        if (!FormatsUtil.isValidEthereumAddress(pendingTx.receivingAddress)) {
                            errorMessage = R.string.pax_invalid_address_1
                            validated = false
                        }

                        // Validate amount
                        if (!pendingTx.isValidAmount()) {
                            errorMessage = R.string.invalid_amount
                            validated = false
                        }

                        // Validate sufficient funds
                        if (maxPaxAvailable.compareTo(pendingTx.amountPax) == -1) {
                            errorMessage = R.string.insufficient_funds
                            validated = false
                        }

                        // Validate sufficient ETH for gas
                        if (maxEthAvailable < pendingTx.feeEth) {
                            errorMessage = R.string.insufficient_eth_for_fees
                            validated = false
                        }
                    }
                    Pair(validated, errorMessage)
                }.flatMapSingle { errorPair ->
                    if (errorPair.first) {
                        // Validate address does not have unconfirmed funds
                        isLastTxPending()
                    } else {
                        Single.just(errorPair)
                    }
                }
        }
    }

    private fun isLastTxPending() =
        ethDataManager.isLastTxPending()
            .observeOn(AndroidSchedulers.mainThread())
            .map { hasUnconfirmed: Boolean ->

                if (hasUnconfirmed) {
                    view?.disableInput()
                    view?.updateMaxAvailable(stringUtils.getString(R.string.eth_unconfirmed_wait))
                    view?.updateMaxAvailableColor(R.color.product_red_medium)
                } else {
                    view?.enableInput()
                }

                val errorMessage = R.string.eth_unconfirmed_wait
                Pair(!hasUnconfirmed, errorMessage)
            }
}

private data class PendingPaxTx(
    val sendingAccountLabel: String,
    var sendingObject: ItemAccount? = null,
    var receivingAddress: String = "",
    var amountPax: BigInteger = BigInteger.ZERO, // Amount pax as minor
    var feeEth: BigInteger = BigInteger.ZERO // wei
) {
    fun isValidAmount(): Boolean = amountPax > BigInteger.ZERO
}

private fun getPaxMinorFromText(text: String?, decimalSeparator: String): BigInteger {
    if (text == null || text.isEmpty()) return BigInteger.ZERO

    val amountToSend = stripSeparator(text, decimalSeparator)
    return Convert.toWei(amountToSend, Convert.Unit.ETHER).toBigInteger()
}

private fun stripSeparator(text: String, decimalSeparator: String) =
    text.trim { it <= ' ' }
        .replace(" ", "")
        .replace(decimalSeparator, ".")