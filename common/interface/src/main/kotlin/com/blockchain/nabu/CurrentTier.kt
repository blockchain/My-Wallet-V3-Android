package com.blockchain.nabu

import io.reactivex.Single

interface CurrentTier {

    fun usersCurrentTier(): Single<Int>
}
