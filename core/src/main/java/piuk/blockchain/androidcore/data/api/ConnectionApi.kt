package piuk.blockchain.androidcore.data.api

import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Named

class ConnectionApi @Inject constructor(@Named("explorer") retrofit: Retrofit) {

    private val connectionEndpoint: ConnectionEndpoint =
        retrofit.create(ConnectionEndpoint::class.java)

    fun getExplorerConnection(): Observable<ResponseBody> =
        connectionEndpoint.pingExplorer()
}
