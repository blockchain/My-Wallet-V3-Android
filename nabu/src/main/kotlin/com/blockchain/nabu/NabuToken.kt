package com.blockchain.nabu

import com.blockchain.nabu.models.NabuOfflineTokenResponse
import io.reactivex.Single

interface NabuToken {

    /**
     * Find or creates the token
     */
    fun fetchNabuToken(): Single<NabuOfflineTokenResponse>
}
