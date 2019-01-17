package com.blockchain.nabu

import io.reactivex.Completable

interface NabuUserSync {

    /**
     * Syncs the current wallet state with Nabu. This works by requesting a signed
     * retail token from the wallet (via getSignedRetailToken()) followed by sending
     * that token to nabu. Syncing is typically performed when nabu should be updated
     * when there is a state change in the wallet (e.g. email/phone verification).
     */
    fun syncUser(): Completable
}
