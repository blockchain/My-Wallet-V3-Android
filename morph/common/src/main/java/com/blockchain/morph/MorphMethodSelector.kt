package com.blockchain.morph

import io.reactivex.Single

interface MorphMethodSelector<T> {

    /**
     * Asynchronously decide which morph method to use.
     */
    fun getMorphMethod(): Single<T>
}
