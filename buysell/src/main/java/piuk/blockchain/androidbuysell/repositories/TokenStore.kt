package piuk.blockchain.androidbuysell.repositories

import io.reactivex.Observable
import piuk.blockchain.androidbuysell.models.coinify.AuthResponse
import com.blockchain.utils.Optional

interface TokenStore {

    fun getAccessToken(): Observable<Optional<AuthResponse>>
}