package com.blockchain.kyc.stores

import com.blockchain.kyc.models.nabu.NabuSessionTokenResponse
import io.reactivex.Observable
import com.blockchain.utils.Optional

interface NabuTokenStore {

    fun getAccessToken(): Observable<Optional<NabuSessionTokenResponse>>
}