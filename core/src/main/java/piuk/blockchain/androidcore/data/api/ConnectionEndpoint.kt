package piuk.blockchain.androidcore.data.api

import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.http.GET

interface ConnectionEndpoint {

    @GET(".")
    fun pingExplorer(): Observable<ResponseBody>
}
