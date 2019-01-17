package com.blockchain.nabu

import com.blockchain.nabu.models.NabuOfflineTokenResponse
import io.reactivex.Single

interface CreateNabuToken {

    /**
     * NB: If you use [Authenticator] or [NabuToken], token creation and persistence in metadata is done transparently
     * for you on demand.
     *
     * Creates the lifetime token for nabu.
     * This is the same as creating a nabu user.
     * After this, the rest of nabu endpoints are usable.
     */
    fun createNabuOfflineToken(): Single<NabuOfflineTokenResponse>
}
