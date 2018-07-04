package piuk.blockchain.android.ui.shapeshift.confirmation

import android.support.annotation.VisibleForTesting
import info.blockchain.api.data.UnspentOutputs
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payment.SpendableUnspentOutputs
import info.blockchain.wallet.shapeshift.data.Quote
import info.blockchain.wallet.shapeshift.data.Trade
import info.blockchain.wallet.util.FormatsUtil
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import org.bitcoinj.core.ECKey
import org.web3j.crypto.RawTransaction
import org.web3j.utils.Convert
import piuk.blockchain.android.R
import piuk.blockchain.androidcoreui.utils.logging.Logging
import piuk.blockchain.androidcoreui.utils.logging.ShapeShiftEvent
import piuk.blockchain.android.data.bitcoincash.BchDataManager
import piuk.blockchain.android.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.ethereum.EthereumAccountWrapper
import piuk.blockchain.android.data.payments.SendDataManager
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcore.data.currency.CryptoCurrencies
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.shapeshift.ShapeShiftDataManager
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ShapeShiftConfirmationPresenter @Inject constructor(
        private val shapeShiftDataManager: ShapeShiftDataManager,
        private val payloadDataManager: PayloadDataManager,
        private val sendDataManager: SendDataManager,
        private val ethDataManager: EthDataManager,
        private val bchDataManager: BchDataManager,
        private val stringUtils: StringUtils,
        private val ethereumAccountWrapper: EthereumAccountWrapper
) : BasePresenter<ShapeShiftConfirmationView>() {

    @VisibleForTesting
    internal val decimalFormat by unsafeLazy {
        DecimalFormat.getNumberInstance(view.locale).apply {
            minimumIntegerDigits = 1
            minimumFractionDigits = 1
            maximumFractionDigits = 8
        }
    }
    private var termsAccepted = false
    private var verifiedSecondPassword: String? = null

    override fun onViewReady() {
        with(view.shapeShiftData) {
            // Render data
            updateDeposit(fromCurrency, depositAmount)
            updateReceive(toCurrency, withdrawalAmount)
            updateTotal(fromCurrency, depositAmount, transactionFee)
            updateExchangeRate(exchangeRate, fromCurrency, toCurrency)
            updateTransactionFee(fromCurrency, transactionFee)
            updateNetworkFee(toCurrency, networkFee)

            // Include countdown
            startCountdown(expiration)
        }
    }

    internal fun onAcceptTermsClicked() {
        termsAccepted = !termsAccepted
        view.setButtonState(termsAccepted)
    }

    internal fun onConfirmClicked() {
        if (!termsAccepted) {
            // Show warning
        } else {
            if (payloadDataManager.isDoubleEncrypted && verifiedSecondPassword == null) {
                // Show password dialog
                view.showSecondPasswordDialog()
                return
            }

            with(view.shapeShiftData) {
                when (fromCurrency) {
                    CryptoCurrencies.BTC -> sendBtcTransaction(
                            xPub,
                            depositAddress,
                            changeAddress,
                            depositAmount,
                            transactionFee,
                            feePerKb
                    )
                    CryptoCurrencies.ETHER -> sendEthTransaction(
                            gasPrice,
                            depositAddress,
                            depositAmount,
                            gasLimit
                    )
                    CryptoCurrencies.BCH -> sendBchTransaction(
                            xPub,
                            depositAddress,
                            changeAddress,
                            depositAmount,
                            transactionFee,
                            feePerKb
                    )
                }
            }
        }
    }

    internal fun onSecondPasswordVerified(secondPassword: String) {
        verifiedSecondPassword = secondPassword
        onConfirmClicked()
    }

    //region Observables
    private fun updateMetadata(completedTxHash: String): Completable {
        val trade = Trade().apply {
            hashIn = completedTxHash
            timestamp = System.currentTimeMillis()
            status = Trade.STATUS.NO_DEPOSITS
            quote = Quote().apply {
                // See https://github.com/blockchain/my-wallet-v3/blob/master/src/shift/trade.js#L132
                val shapeShiftData = view.shapeShiftData
                // toPartialJSON
                orderId = shapeShiftData.orderId
                quotedRate = shapeShiftData.exchangeRate
                deposit = shapeShiftData.depositAddress
                minerFee = shapeShiftData.networkFee
                // toJSON
                pair =
                        """${shapeShiftData.fromCurrency.symbol.toLowerCase()}_${shapeShiftData.toCurrency.symbol.toLowerCase()}"""
                depositAmount = shapeShiftData.depositAmount
                withdrawal = shapeShiftData.withdrawalAddress
                withdrawalAmount = shapeShiftData.withdrawalAmount
            }
        }

        return shapeShiftDataManager.addTradeToList(trade)
                .addToCompositeDisposable(this)
    }

    private fun sendBtcTransaction(
            xPub: String,
            depositAddress: String,
            changeAddress: String,
            depositAmount: BigDecimal,
            transactionFee: BigInteger,
            feePerKb: BigInteger
    ) {
        require(FormatsUtil.isValidBitcoinAddress(depositAddress)) { "Attempting to send BTC to a non-BTC address" }

        val account = payloadDataManager.getAccountForXPub(xPub)
        val satoshis = depositAmount.multiply(BigDecimal.valueOf(BTC_DEC)).toBigInteger()

        if (payloadDataManager.isDoubleEncrypted) {
            payloadDataManager.decryptHDWallet(verifiedSecondPassword)
        }

        getUnspentBtcApiResponse(xPub)
                .doOnSubscribe { view.showProgressDialog(R.string.please_wait) }
                .map { sendDataManager.getSpendableCoins(it, satoshis, feePerKb) }
                .flatMap { unspent ->
                    getBitcoinKeys(account, unspent)
                            .flatMap {
                                sendDataManager.submitBtcPayment(
                                        unspent,
                                        it,
                                        depositAddress,
                                        changeAddress,
                                        transactionFee,
                                        satoshis
                                )
                            }
                }
                .flatMapCompletable { updateMetadata(it) }
                .doOnTerminate { view.dismissProgressDialog() }
                .doOnError(Timber::e)
                .addToCompositeDisposable(this)
                .subscribe(
                        { handleSuccess(depositAddress) },
                        { handleFailure() }
                )
    }

    private fun sendEthTransaction(
            gasPrice: BigInteger,
            depositAddress: String,
            depositAmount: BigDecimal,
            gasLimit: BigInteger
    ) {
        createEthTransaction(gasPrice, depositAddress, depositAmount, gasLimit)
                .addToCompositeDisposable(this)
                .doOnSubscribe { view.showProgressDialog(R.string.please_wait) }
                .doOnTerminate { view.dismissProgressDialog() }
                .flatMap {
                    if (payloadDataManager.isDoubleEncrypted) {
                        payloadDataManager.decryptHDWallet(verifiedSecondPassword)
                    }

                    val ecKey = ethereumAccountWrapper.deriveECKey(
                            payloadDataManager.wallet!!.hdWallets[0].masterKey,
                            0
                    )
                    return@flatMap ethDataManager.signEthTransaction(it, ecKey)
                }
                .flatMap { ethDataManager.pushEthTx(it) }
                .flatMap { ethDataManager.setLastTxHashObservable(it, System.currentTimeMillis()) }
                .flatMapCompletable { updateMetadata(it) }
                .doOnError(Timber::e)
                .subscribe(
                        { handleSuccess(depositAddress) },
                        { handleFailure() }
                )
    }

    private fun sendBchTransaction(
            xPub: String,
            depositAddress: String,
            changeAddress: String,
            depositAmount: BigDecimal,
            transactionFee: BigInteger,
            feePerKb: BigInteger
    ) {
        // Address wil be in Base58 (ie legacy Bitcoin) format here
        require(FormatsUtil.isValidBitcoinAddress(depositAddress)) { "Attempting to send BCH to a non-BCH address" }

        // Use BTC accounts + keys for signing
        // TODO: This will cause issues if we change the derivation path in the future
        val account = payloadDataManager.getAccountForXPub(xPub)
        val satoshis = depositAmount.multiply(BigDecimal.valueOf(BTC_DEC)).toBigInteger()

        if (payloadDataManager.isDoubleEncrypted) {
            payloadDataManager.decryptHDWallet(verifiedSecondPassword)
        }

        getUnspentBchApiResponse(xPub)
                .doOnSubscribe { view.showProgressDialog(R.string.please_wait) }
                .map { sendDataManager.getSpendableCoins(it, satoshis, feePerKb) }
                .flatMap { unspent ->
                    getBitcoinKeys(account, unspent)
                            .flatMap {
                                sendDataManager.submitBchPayment(
                                        unspent,
                                        it,
                                        depositAddress,
                                        changeAddress,
                                        transactionFee,
                                        satoshis
                                )
                            }
                }
                .flatMapCompletable { updateMetadata(it) }
                .doOnTerminate { view.dismissProgressDialog() }
                .doOnError(Timber::e)
                .addToCompositeDisposable(this)
                .subscribe(
                        { handleSuccess(depositAddress) },
                        { handleFailure() }
                )
    }

    private fun handleSuccess(depositAddress: String) {
        view.launchProgressPage(depositAddress)
        Logging.logCustom(
                ShapeShiftEvent()
                        .putPair(
                                view.shapeShiftData.fromCurrency.symbol,
                                view.shapeShiftData.toCurrency.symbol
                        )
                        .putSuccess(true)
        )
    }

    private fun handleFailure() {
        view.showToast(R.string.transaction_failed, ToastCustom.TYPE_ERROR)
        Logging.logCustom(
                ShapeShiftEvent()
                        .putPair(
                                view.shapeShiftData.fromCurrency.symbol,
                                view.shapeShiftData.toCurrency.symbol
                        )
                        .putSuccess(true)
        )
    }

    private fun getUnspentBtcApiResponse(address: String): Observable<UnspentOutputs> {
        return if (payloadDataManager.getAddressBalance(address).toLong() > 0) {
            sendDataManager.getUnspentOutputs(address)
                    .applySchedulers()
        } else {
            Observable.error(Throwable("No funds - skipping call to unspent API"))
        }
    }

    private fun getUnspentBchApiResponse(address: String): Observable<UnspentOutputs> {
        return if (bchDataManager.getAddressBalance(address).toLong() > 0) {
            sendDataManager.getUnspentBchOutputs(address)
                    .applySchedulers()
        } else {
            Observable.error(Throwable("No funds - skipping call to unspent API"))
        }
    }

    private fun createEthTransaction(
            gasPrice: BigInteger,
            depositAddress: String,
            depositAmount: BigDecimal,
            gasLimit: BigInteger
    ): Observable<RawTransaction> {
        require(FormatsUtil.isValidEthereumAddress(depositAddress)) { "Attempting to send ETH to a non-ETH address" }

        val feeGwei = BigDecimal(gasPrice)
        val feeWei = Convert.toWei(feeGwei, Convert.Unit.GWEI)

        return ethDataManager.fetchEthAddress()
                .map { ethDataManager.getEthResponseModel()!!.getNonce() }
                .map { nonce ->
                    ethDataManager.createEthTransaction(
                            nonce = nonce,
                            to = depositAddress,
                            gasPrice = feeWei.toBigInteger(),
                            gasLimit = gasLimit,
                            weiValue = Convert.toWei(
                                    depositAmount,
                                    Convert.Unit.ETHER
                            ).toBigInteger()
                    )
                }
    }

    private fun getBitcoinKeys(
            account: Account,
            unspent: SpendableUnspentOutputs
    ): Observable<List<ECKey>> =
            Observable.just(payloadDataManager.getHDKeysForSigning(account, unspent))
    //endregion

    //region View Updates
    private fun updateDeposit(fromCurrency: CryptoCurrencies, depositAmount: BigDecimal) {
        val label =
                stringUtils.getFormattedString(R.string.shapeshift_deposit_title, fromCurrency.unit)
        val amount = "${depositAmount.toLocalisedString()} ${fromCurrency.symbol}"

        view.updateDeposit(label, amount)
    }

    private fun updateReceive(toCurrency: CryptoCurrencies, receiveAmount: BigDecimal) {
        val label =
                stringUtils.getFormattedString(R.string.shapeshift_receive_title, toCurrency.unit)
        val amount = "${receiveAmount.toLocalisedString()} ${toCurrency.symbol}"

        view.updateReceive(label, amount)
    }

    private fun updateTotal(
            toCurrency: CryptoCurrencies,
            depositAmount: BigDecimal,
            transactionFee: BigInteger
    ) {
        val label = stringUtils.getFormattedString(R.string.shapeshift_total_title, toCurrency.unit)
        val fee = getFeeForCurrency(toCurrency, transactionFee)
        val totalSent = depositAmount.plus(fee)
        val amount = "${totalSent.toLocalisedString()} ${toCurrency.symbol}"

        view.updateTotalAmount(label, amount)
    }

    private fun updateExchangeRate(
            exchangeRate: BigDecimal,
            fromCurrency: CryptoCurrencies,
            toCurrency: CryptoCurrencies
    ) {
        val formattedExchangeRate = exchangeRate.setScale(8, RoundingMode.HALF_DOWN)
                .toLocalisedString()
        val formattedString = stringUtils.getFormattedString(
                R.string.shapeshift_exchange_rate_formatted,
                fromCurrency.symbol,
                formattedExchangeRate,
                toCurrency.symbol
        )

        view.updateExchangeRate(formattedString)
    }

    private fun updateTransactionFee(fromCurrency: CryptoCurrencies, transactionFee: BigInteger) {
        val fee = getFeeForCurrency(fromCurrency, transactionFee)
        val displayString = "${fee.toLocalisedString()} ${fromCurrency.symbol}"

        view.updateTransactionFee(displayString)
    }

    private fun updateNetworkFee(toCurrency: CryptoCurrencies, networkFee: BigDecimal) {
        val displayString = "${networkFee.toLocalisedString()} ${toCurrency.symbol}"

        view.updateNetworkFee(displayString)
    }
    //endregion

    private fun startCountdown(endTime: Long) {
        var remaining = (endTime - System.currentTimeMillis()) / 1000
        if (remaining <= 0) {
            // Finish page with error
            view.showQuoteExpiredDialog()
        } else {
            Observable.interval(1, TimeUnit.SECONDS)
                    .addToCompositeDisposable(this)
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnEach { remaining-- }
                    .map { return@map remaining }
                    .doOnNext {
                        val readableTime = String.format(
                                "%2d:%02d",
                                TimeUnit.SECONDS.toMinutes(it),
                                TimeUnit.SECONDS.toSeconds(it) -
                                        TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(it))
                        )
                        view.updateCounter(readableTime)
                    }
                    .doOnNext { if (it < 5 * 60) view.showTimeExpiring() }
                    .takeUntil { it <= 0 }
                    .doOnComplete { view.showQuoteExpiredDialog() }
                    .subscribe()
        }
    }

    private fun getFeeForCurrency(
            toCurrency: CryptoCurrencies,
            transactionFee: BigInteger
    ): BigDecimal =
            when (toCurrency) {
                CryptoCurrencies.BTC, CryptoCurrencies.BCH -> BigDecimal(transactionFee).divide(
                        BigDecimal.valueOf(BTC_DEC),
                        8,
                        RoundingMode.HALF_DOWN
                )
                CryptoCurrencies.ETHER -> Convert.fromWei(
                        BigDecimal(transactionFee),
                        Convert.Unit.ETHER
                )
            }

    private fun BigDecimal.toLocalisedString(): String = decimalFormat.format(this)

    companion object {

        private const val BTC_DEC = 1e8

    }

}