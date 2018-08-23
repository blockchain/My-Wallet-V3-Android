package com.blockchain.nabu.stores

import com.blockchain.nabu.models.NabuSessionTokenResponse
import com.blockchain.utils.Optional
import io.reactivex.Observable

interface NabuTokenStore {

    fun getAccessToken(): Observable<Optional<NabuSessionTokenResponse>>
}