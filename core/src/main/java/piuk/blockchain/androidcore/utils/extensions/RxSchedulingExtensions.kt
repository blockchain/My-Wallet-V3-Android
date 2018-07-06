@file:JvmName("RxSchedulingExtensions")

package piuk.blockchain.androidcore.utils.extensions

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

/**
 * Applies standard Schedulers to an [Observable], ie IO for subscription, Main Thread for
 * onNext/onComplete/onError
 */
fun <T> Observable<T>.applySchedulers(): Observable<T> = this.subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())
    .doOnError(Timber::e)

/**
 * Applies standard Schedulers to a [Single], ie IO for subscription, Main Thread for
 * onNext/onComplete/onError
 */
fun <T> Single<T>.applySchedulers(): Single<T> = this.subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())
    .doOnError(Timber::e)

/**
 * Applies standard Schedulers to a [Completable], ie IO for subscription,
 * Main Thread for onNext/onComplete/onError
 */
fun Completable.applySchedulers(): Completable = this.subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())
    .doOnError(Timber::e)