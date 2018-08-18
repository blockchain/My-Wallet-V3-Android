package com.blockchain.kyc.extensions

import com.blockchain.kyc.models.nabu.NabuApiException
import io.reactivex.Completable
import io.reactivex.Single
import retrofit2.HttpException

internal fun <T> Single<T>.wrapErrorMessage(): Single<T> = this.onErrorResumeNext {
    when (it) {
        is HttpException -> Single.error(NabuApiException.fromResponseBody(it.response()))
        else -> Single.error(it)
    }
}

internal fun Completable.wrapErrorMessage(): Completable = this.onErrorResumeNext {
    when (it) {
        is HttpException -> Completable.error(NabuApiException.fromResponseBody(it.response()))
        else -> Completable.error(it)
    }
}