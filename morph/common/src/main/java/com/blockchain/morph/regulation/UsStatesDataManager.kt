package com.blockchain.morph.regulation

import io.reactivex.Completable
import io.reactivex.Observable

interface UsStatesDataManager {

    fun setState(state: UsState): Completable

    fun isStateWhitelisted(usState: UsState): Observable<Boolean>
}

data class UsState(
    val name: String,
    val code: String
)
