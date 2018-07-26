package info.blockchain.wallet.api

import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WalletApiEndpoints {

    @GET("v2/randombytes")
    fun getRandomBytesCall(
        @Query("bytes") bytes: Int,
        @Query("format") format: String
    ): Call<ResponseBody>

    @GET("v2/randombytes")
    fun getRandomBytes(
        @Query("bytes") bytes: Int,
        @Query("format") format: String
    ): Observable<ResponseBody>
}