package com.blockchain.ui.extensions

import android.view.View
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit

fun View.throttledClicks(): Observable<Unit> =
    clicks()
        .throttleFirst(500, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())