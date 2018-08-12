package com.blockchain.kyc.stores

import com.blockchain.kyc.models.nabu.NabuOfflineTokenResponse
import io.reactivex.Observable
import piuk.blockchain.androidcore.utils.Optional

interface NabuTokenStore {

    fun getAccessToken(): Observable<Optional<NabuOfflineTokenResponse>>
}