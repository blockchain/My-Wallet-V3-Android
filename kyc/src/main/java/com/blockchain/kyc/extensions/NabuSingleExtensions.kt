package com.blockchain.kyc.extensions

import com.blockchain.kyc.models.nabu.NabuApiException
import io.reactivex.Single
import retrofit2.HttpException

internal fun <T> Single<T>.wrapErrorMessage(): Single<T> = this.onErrorResumeNext {
    when (it) {
        is HttpException -> Single.error(NabuApiException.fromResponseBody(it.response()))
        else -> Single.error(it)
    }
}