package com.blockchain.koin

import android.app.Activity
import android.support.v4.app.Fragment
import org.koin.android.ext.android.inject
import org.koin.dsl.context.ParameterProvider

const val ACTIVITY_PARAMETER = "_param_activity"

inline fun <reified T> Activity.injectActivity(): Lazy<T> =
    inject(parameters = { toInjectionParameters() })

inline fun <reified T> Fragment.injectActivity(): Lazy<T> =
    inject(parameters = { this.activity!!.toInjectionParameters() })

fun Activity.toInjectionParameters() = mapOf(ACTIVITY_PARAMETER to this)

fun <T : Activity> ParameterProvider.getActivity(): T = this[ACTIVITY_PARAMETER]
