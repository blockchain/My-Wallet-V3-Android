package piuk.blockchain.androidbuysell.services

import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.androidbuysell.api.Coinify
import piuk.blockchain.androidbuysell.api.PATH_COINFY_AUTH
import piuk.blockchain.androidbuysell.api.PATH_COINFY_BANK_ACCOUNTS
import piuk.blockchain.androidbuysell.api.PATH_COINFY_CANCEL
import piuk.blockchain.androidbuysell.api.PATH_COINFY_GET_TRADER
import piuk.blockchain.androidbuysell.api.PATH_COINFY_KYC
import piuk.blockchain.androidbuysell.api.PATH_COINFY_PREP_KYC
import piuk.blockchain.androidbuysell.api.PATH_COINFY_SIGNUP_TRADER
import piuk.blockchain.androidbuysell.api.PATH_COINFY_SUBSCRIPTIONS
import piuk.blockchain.androidbuysell.api.PATH_COINFY_TRADES
import piuk.blockchain.androidbuysell.api.PATH_COINFY_TRADES_PAYMENT_METHODS
import piuk.blockchain.androidbuysell.api.PATH_COINFY_TRADES_QUOTE
import piuk.blockchain.androidbuysell.models.coinify.AuthRequest
import piuk.blockchain.androidbuysell.models.coinify.AuthResponse
import piuk.blockchain.androidbuysell.models.coinify.BankAccount
import piuk.blockchain.androidbuysell.models.coinify.CoinifyTrade
import piuk.blockchain.androidbuysell.models.coinify.CoinifyTradeRequest
import piuk.blockchain.androidbuysell.models.coinify.KycResponse
import piuk.blockchain.androidbuysell.models.coinify.PaymentMethod
import piuk.blockchain.androidbuysell.models.coinify.Quote
import piuk.blockchain.androidbuysell.models.coinify.QuoteRequest
import piuk.blockchain.androidbuysell.models.coinify.SignUpDetails
import piuk.blockchain.androidbuysell.models.coinify.Subscription
import piuk.blockchain.androidbuysell.models.coinify.Trader
import piuk.blockchain.androidbuysell.models.coinify.TraderResponse
import piuk.blockchain.androidbuysell.models.coinify.exceptions.wrapErrorMessage
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.data.rxjava.RxPinning
import piuk.blockchain.androidcore.utils.annotations.Mockable
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Mockable
@Singleton
class CoinifyService @Inject constructor(
    environmentConfig: EnvironmentConfig,
    @Named("kotlin") retrofit: Retrofit,
    rxBus: RxBus
) {

    private val service: Coinify = retrofit.create(Coinify::class.java)
    private val rxPinning: RxPinning = RxPinning(rxBus)
    private val baseUrl: String = environmentConfig.coinifyUrl

    internal fun signUp(
        path: String = "$baseUrl$PATH_COINFY_SIGNUP_TRADER",
        signUpDetails: SignUpDetails
    ): Single<TraderResponse> = rxPinning.callSingle {
        service.signUp(path, signUpDetails)
            .wrapErrorMessage()
    }

    internal fun getTrader(
        path: String = "$baseUrl$PATH_COINFY_GET_TRADER",
        accessToken: String
    ): Single<Trader> = rxPinning.callSingle {
        service.getTrader(path, getFormattedToken(accessToken))
            .wrapErrorMessage()
    }

    internal fun getSubscriptions(
        path: String = "$baseUrl$PATH_COINFY_SUBSCRIPTIONS",
        accessToken: String
    ): Single<List<Subscription>> = rxPinning.callSingle {
        service.getSubscriptions(path, getFormattedToken(accessToken))
            .wrapErrorMessage()
    }

    internal fun getTrades(
        path: String = "$baseUrl$PATH_COINFY_TRADES",
        accessToken: String
    ): Single<List<CoinifyTrade>> = rxPinning.callSingle {
        service.getTrades(path, getFormattedToken(accessToken))
            .wrapErrorMessage()
    }

    internal fun createTrade(
        path: String = "$baseUrl$PATH_COINFY_TRADES",
        tradeRequest: CoinifyTradeRequest,
        accessToken: String
    ): Single<CoinifyTrade> = rxPinning.callSingle {
        service.createTrade(path, tradeRequest, getFormattedToken(accessToken))
            .wrapErrorMessage()
    }

    internal fun cancelTrade(
        path: String = "$baseUrl$PATH_COINFY_TRADES",
        id: Int,
        accessToken: String
    ): Single<CoinifyTrade> = rxPinning.callSingle {
        service.cancelTrade("$path/$id/$PATH_COINFY_CANCEL", getFormattedToken(accessToken))
            .wrapErrorMessage()
    }

    internal fun getTradeStatus(
        path: String = "$baseUrl$PATH_COINFY_TRADES",
        tradeId: Int,
        accessToken: String
    ): Single<CoinifyTrade> = rxPinning.callSingle {
        service.getTradeStatus("$path/$tradeId", getFormattedToken(accessToken))
            .wrapErrorMessage()
    }

    internal fun auth(
        path: String = "$baseUrl$PATH_COINFY_AUTH",
        authRequest: AuthRequest
    ): Single<AuthResponse> = rxPinning.callSingle {
        service.auth(path, authRequest)
            .wrapErrorMessage()
    }

    internal fun startKycReview(
        path: String = "$baseUrl$PATH_COINFY_PREP_KYC",
        accessToken: String
    ): Single<KycResponse> = rxPinning.callSingle {
        service.startKycReview(path, getFormattedToken(accessToken))
            .wrapErrorMessage()
    }

    internal fun getKycReviewStatus(
        path: String = "$baseUrl$PATH_COINFY_KYC",
        id: Int,
        accessToken: String
    ): Single<KycResponse> = rxPinning.callSingle {
        service.getKycReviewStatus("$path/$id", getFormattedToken(accessToken))
            .wrapErrorMessage()
    }

    internal fun getKycReviews(
        path: String = "$baseUrl$PATH_COINFY_KYC",
        accessToken: String
    ): Single<List<KycResponse>> = rxPinning.callSingle {
        service.getKycReviews(path, getFormattedToken(accessToken))
            .wrapErrorMessage()
    }

    internal fun getQuote(
        path: String = "$baseUrl$PATH_COINFY_TRADES_QUOTE",
        quoteRequest: QuoteRequest,
        accessToken: String
    ): Single<Quote> = rxPinning.callSingle {
        service.getQuote(path, quoteRequest, getFormattedToken(accessToken))
            .wrapErrorMessage()
    }

    internal fun getPaymentMethods(
        path: String = "$baseUrl$PATH_COINFY_TRADES_PAYMENT_METHODS",
        inCurrency: String?,
        outCurrency: String?,
        accessToken: String
    ): Single<List<PaymentMethod>> = rxPinning.callSingle {
        service.getPaymentMethods(path, inCurrency, outCurrency, getFormattedToken(accessToken))
            .wrapErrorMessage()
    }

    internal fun getBankAccounts(
        path: String = "$baseUrl$PATH_COINFY_BANK_ACCOUNTS",
        accessToken: String
    ): Single<List<BankAccount>> = rxPinning.callSingle {
        service.getBankAccounts(path, getFormattedToken(accessToken))
            .wrapErrorMessage()
    }

    internal fun getBankAccount(
        path: String = "$baseUrl$PATH_COINFY_BANK_ACCOUNTS",
        accountId: Int,
        accessToken: String
    ): Single<BankAccount> = rxPinning.callSingle {
        service.getBankAccount("$path/$accountId", getFormattedToken(accessToken))
            .wrapErrorMessage()
    }

    internal fun deleteBankAccount(
        path: String = "$baseUrl$PATH_COINFY_BANK_ACCOUNTS",
        accountId: Int,
        accessToken: String
    ): Completable = rxPinning.call {
        service.deleteBankAccount("$path/$accountId", getFormattedToken(accessToken))
            .wrapErrorMessage()
    }

    internal fun addBankAccount(
        path: String = "$baseUrl$PATH_COINFY_BANK_ACCOUNTS",
        bankAccount: BankAccount,
        accessToken: String
    ): Single<BankAccount> = rxPinning.callSingle {
        service.addBankAccount(path, bankAccount, getFormattedToken(accessToken))
            .wrapErrorMessage()
    }

    private fun getFormattedToken(accessToken: String) = "Bearer $accessToken"
}