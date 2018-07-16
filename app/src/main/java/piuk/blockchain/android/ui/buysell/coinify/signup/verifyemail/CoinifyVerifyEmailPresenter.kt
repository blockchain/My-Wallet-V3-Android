package piuk.blockchain.android.ui.buysell.coinify.signup.verifyemail

import android.support.annotation.VisibleForTesting
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.android.R
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager
import piuk.blockchain.androidbuysell.models.CoinifyData
import piuk.blockchain.androidbuysell.models.coinify.KycResponse
import piuk.blockchain.androidbuysell.models.coinify.TraderResponse
import piuk.blockchain.androidbuysell.models.coinify.exceptions.CoinifyApiException
import piuk.blockchain.androidbuysell.services.ExchangeService
import piuk.blockchain.androidcore.data.currency.CurrencyState
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsDataManager
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import piuk.blockchain.androidcore.utils.extensions.toSerialisedString
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class CoinifyVerifyEmailPresenter @Inject constructor(
    private val settingsDataManager: SettingsDataManager,
    private val walletOptionsDataManager: WalletOptionsDataManager,
    private val payloadDataManager: PayloadDataManager,
    private val exchangeService: ExchangeService,
    private val coinifyDataManager: CoinifyDataManager,
    private val metadataManager: MetadataManager,
    private val currencyState: CurrencyState,
    private val stringUtils: StringUtils
) : BasePresenter<CoinifyVerifyEmailView>() {

    private var verifiedEmailAddress: String? = null

    override fun onViewReady() {
        settingsDataManager.fetchSettings()
            .delay(300, TimeUnit.MILLISECONDS, Schedulers.computation())
            .applySchedulers()
            .addToCompositeDisposable(this)
            .doOnSubscribe { view.showLoading(true) }
            .doAfterTerminate { view.showLoading(false) }
            .subscribeBy(
                onNext = {
                    view.onEnableContinueButton(it.isEmailVerified)

                    if (it.isEmailVerified) {
                        setVerifiedEmailAndDisplay(it.email)
                    } else {
                        view.onShowUnverifiedEmail(it.email)
                        resendVerificationLink(it.email)
                    }
                },
                onError = {
                    Timber.e(it)
                    view.onShowErrorAndClose()
                }
            )
    }

    private fun resendVerificationLink(emailAddress: String) {
        settingsDataManager.updateEmail(emailAddress)
            .applySchedulers()
            .addToCompositeDisposable(this)
            .subscribeBy(
                onNext = { pollForEmailVerified() },
                onError = {
                    Timber.e(it)
                    view.onShowErrorAndClose()
                }
            )
    }

    private fun pollForEmailVerified() {
        Observable.interval(10, TimeUnit.SECONDS, Schedulers.io())
            .flatMap { settingsDataManager.fetchSettings() }
            .applySchedulers()
            .addToCompositeDisposable(this)
            .doOnNext {
                view.onEnableContinueButton(it.isEmailVerified)

                if (it.isEmailVerified) {
                    setVerifiedEmailAndDisplay(it.email)
                }
            }
            .takeUntil { it.isEmailVerified }
            .subscribeBy(
                onNext = { /* No-op */ },
                onError = { Timber.e(it) }
            )
    }

    @VisibleForTesting
    fun setVerifiedEmailAndDisplay(verifiedEmail: String) {
        verifiedEmailAddress = verifiedEmail
        view.onShowVerifiedEmail(verifiedEmail)
    }

    fun onContinueClicked(countryCode: String) {
        require(countryCode.isNotEmpty()) { "Country code should not be empty" }

        verifiedEmailAddress?.run {
            createCoinifyAccount(this, countryCode)
                .applySchedulers()
                .doOnSubscribe { view.showLoading(true) }
                .doAfterTerminate { view.showLoading(false) }
                .subscribe(
                    {
                        view.onStartSignUpSuccess()
                    },
                    {
                        Timber.e(it)
                        if (it is CoinifyApiException) {
                            view.showErrorDialog(it.getErrorDescription())
                        } else {
                            view.showErrorDialog(stringUtils.getString(R.string.buy_sell_confirmation_unexpected_error))
                        }
                    }
                )
        } ?: view.onShowErrorAndClose()
    }

    fun onTermsCheckChanged() {
        view.onEnableContinueButton(!verifiedEmailAddress.isNullOrEmpty())
    }

    /**
     * Creates Coinify account.
     * Saves Coinify metadata.
     * Starts KYC review process and navigates to iSignThis webview.
     *
     * @return [Completable]
     */
    private fun createCoinifyAccount(
        verifiedEmailAddress: String,
        countryCode: String
    ): Observable<KycResponse> = walletOptionsDataManager.getCoinifyPartnerId()
        .flatMap {
            coinifyDataManager.getEmailTokenAndSignUp(
                payloadDataManager.guid,
                payloadDataManager.sharedKey,
                verifiedEmailAddress,
                getDefaultCurrency(),
                countryCode,
                it
            ).toObservable()
                .applySchedulers()
        }
        .flatMap {
            saveCoinifyMetadata(it).toObservable()
        }
        .flatMap {
            coinifyDataManager.startKycReview(it.offlineToken)
                .toObservable()
                .applySchedulers()
        }

    private fun getDefaultCurrency(): String = when (currencyState.fiatUnit) {
        "GBP", "USD", "EUR", "DKK" -> currencyState.fiatUnit
        else -> "EUR"
    }

    /**
     * Saves user/trader's offline token and user id.
     *
     * @param traderResponse to be saved in metadata store
     * @return [Single] wrapping a [TraderResponse] object
     */
    private fun saveCoinifyMetadata(traderResponse: TraderResponse): Single<TraderResponse> =
        exchangeService.getExchangeMetaData()
            .applySchedulers()
            .doOnNext {
                it.coinify = CoinifyData(
                    traderResponse.trader.id,
                    traderResponse.offlineToken
                )
            }
            .flatMapCompletable {
                metadataManager.saveToMetadata(
                    it.toSerialisedString(),
                    ExchangeService.METADATA_TYPE_EXCHANGE
                )
            }
            .toSingle { traderResponse }
}