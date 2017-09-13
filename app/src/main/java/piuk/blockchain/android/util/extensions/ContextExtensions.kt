@file:JvmName("ContextExtensions")

package piuk.blockchain.android.util.extensions

import android.app.Activity
import android.support.annotation.StringRes
import android.support.v4.app.Fragment
import piuk.blockchain.android.ui.customviews.ToastCustom

/**
 * Shows a [ToastCustom] from a given [Activity]. By default, the Toast is of type
 * [ToastCustom.TYPE_GENERAL] but can be overloaded if needed.
 *
 * @param text The text to display, as a [String]
 * @param type An optional [ToastCustom.ToastType] which can be omitted for general Toasts
 */
fun Activity.toast(text: String, @ToastCustom.ToastType type: String = ToastCustom.TYPE_GENERAL) {
    ToastCustom.makeText(this, text, ToastCustom.LENGTH_SHORT, type)
}

/**
 * Shows a [ToastCustom] from a given [Activity]. By default, the Toast is of type
 * [ToastCustom.TYPE_GENERAL] but can be overloaded if needed.
 *
 * @param text The text to display, as a String resource [Int]
 * @param type An optional [ToastCustom.ToastType] which can be omitted for general Toasts
 */
fun Activity.toast(@StringRes text: Int, @ToastCustom.ToastType type: String = ToastCustom.TYPE_GENERAL) {
    ToastCustom.makeText(this, getString(text), ToastCustom.LENGTH_SHORT, type)
}

/**
 * Shows a [ToastCustom] from a given [Fragment]. By default, the Toast is of type
 * [ToastCustom.TYPE_GENERAL] but can be overloaded if needed.
 *
 * @param text The text to display, as a [String]
 * @param type An optional [ToastCustom.ToastType] which can be omitted for general Toasts
 */
fun Fragment.toast(text: String, @ToastCustom.ToastType type: String = ToastCustom.TYPE_GENERAL) {
    ToastCustom.makeText(activity, text, ToastCustom.LENGTH_SHORT, type)
}

/**
 * Shows a [ToastCustom] from a given [Fragment]. By default, the Toast is of type
 * [ToastCustom.TYPE_GENERAL] but can be overloaded if needed.
 *
 * @param text The text to display, as a String resource [Int]
 * @param type An optional [ToastCustom.ToastType] which can be omitted for general Toasts
 */
fun Fragment.toast(@StringRes text: Int, @ToastCustom.ToastType type: String = ToastCustom.TYPE_GENERAL) {
    ToastCustom.makeText(activity, getString(text), ToastCustom.LENGTH_SHORT, type)
}