package piuk.blockchain.androidbuysell.api

import io.reactivex.Completable
import io.reactivex.Single
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
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

internal interface Coinify {

    @POST
    fun signUp(
        @Url url: String,
        @Body signUpDetails: SignUpDetails
    ): Single<TraderResponse>

    @GET
    fun getTrader(
        @Url url: String,
        @Header("Authorization") accessToken: String
    ): Single<Trader>

    @GET
    fun getSubscriptions(
        @Url url: String,
        @Header("Authorization") accessToken: String
    ): Single<List<Subscription>>

    @GET
    fun getTrades(
        @Url url: String,
        @Header("Authorization") accessToken: String
    ): Single<List<CoinifyTrade>>

    @POST
    fun createTrade(
        @Url path: String,
        @Body coinifyTrade: CoinifyTradeRequest,
        @Header("Authorization") accessToken: String
    ): Single<CoinifyTrade>

    @PATCH
    fun cancelTrade(
        @Url path: String,
        @Header("Authorization") accessToken: String
    ): Single<CoinifyTrade>

    @GET
    fun getTradeStatus(
        @Url url: String,
        @Header("Authorization") accessToken: String
    ): Single<CoinifyTrade>

    @POST
    fun auth(
        @Url url: String,
        @Body authRequest: AuthRequest
    ): Single<AuthResponse>

    @POST
    fun startKycReview(
        @Url url: String,
        @Header("Authorization") accessToken: String
    ): Single<KycResponse>

    @GET
    fun getKycReviews(
        @Url url: String,
        @Header("Authorization") accessToken: String
    ): Single<List<KycResponse>>

    @POST
    fun getKycReviewStatus(
        @Url url: String,
        @Header("Authorization") accessToken: String
    ): Single<KycResponse>

    @POST
    fun getQuote(
        @Url url: String,
        @Body quoteRequest: QuoteRequest,
        @Header("Authorization") accessToken: String
    ): Single<Quote>

    @GET
    fun getPaymentMethods(
        @Url path: String,
        @Query("inCurrency") inCurrency: String?,
        @Query("outCurrency") outCurrency: String?,
        @Header("Authorization") accessToken: String
    ): Single<List<PaymentMethod>>

    @GET
    fun getBankAccounts(
        @Url path: String,
        @Header("Authorization") accessToken: String
    ): Single<List<BankAccount>>

    @GET
    fun getBankAccount(
        @Url path: String,
        @Header("Authorization") accessToken: String
    ): Single<BankAccount>

    @DELETE
    fun deleteBankAccount(
        @Url path: String,
        @Header("Authorization") accessToken: String
    ): Completable

    @POST
    fun addBankAccount(
        @Url path: String,
        @Body bankAccount: BankAccount,
        @Header("Authorization") accessToken: String
    ): Single<BankAccount>
}