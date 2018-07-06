package piuk.blockchain.androidbuysell.repositories

import io.reactivex.Observable
import piuk.blockchain.androidbuysell.models.coinify.AuthResponse
import piuk.blockchain.androidcore.utils.Optional

interface TokenStore {

    fun getAccessToken(): Observable<Optional<AuthResponse>>
}