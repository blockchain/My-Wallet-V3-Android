@file:JvmName("ViewExtensions")

package piuk.blockchain.androidcoreui.utils.extensions

import android.annotation.SuppressLint
import android.support.animation.DynamicAnimation
import android.support.animation.SpringAnimation
import android.support.animation.SpringForce
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import piuk.blockchain.androidcoreui.utils.helperfunctions.CustomFont
import piuk.blockchain.androidcoreui.utils.helperfunctions.loadFont
import timber.log.Timber

/**
 * Sets the visibility of a [View] to [View.VISIBLE]
 */
fun View?.visible() {
    if (this != null) visibility = View.VISIBLE
}

/**
 * Sets the visibility of a [View] to [View.INVISIBLE]
 */
fun View?.invisible() {
    if (this != null) visibility = View.INVISIBLE
}

/**
 * Sets the visibility of a [View] to [View.GONE]
 */
fun View?.gone() {
    if (this != null) visibility = View.GONE
}

/**
 * Sets the visibility of a [View] to [View.GONE] depending on a predicate
 *
 * @param func If true, the visibility of the [View] will be set to [View.GONE], else [View.VISIBLE]
 */
fun View?.goneIf(func: () -> Boolean) {
    if (this != null) {
        visibility = if (func()) View.GONE else View.VISIBLE
    }
}

/**
 * Sets the visibility of a [View] to [View.GONE] depending on a value
 *
 * @param value If true, the visibility of the [View] will be set to [View.GONE], else [View.VISIBLE]
 */
fun View?.goneIf(value: Boolean) {
    if (this != null) {
        visibility = if (value) View.GONE else View.VISIBLE
    }
}

/**
 * Sets the visibility of a [View] to [View.INVISIBLE] depending on a predicate
 *
 * @param func If true, the visibility of the [View] will be set to [View.INVISIBLE], else [View.VISIBLE]
 */
fun View?.invisibleIf(func: () -> Boolean) {
    if (this != null) {
        visibility = if (func()) View.INVISIBLE else View.VISIBLE
    }
}

/**
 * Sets the visibility of a [View] to [View.INVISIBLE] depending on a value
 *
 * @param value If true, the visibility of the [View] will be set to [View.INVISIBLE], else [View.VISIBLE]
 */
fun View?.invisibleIf(value: Boolean) {
    if (this != null) {
        visibility = if (value) View.INVISIBLE else View.VISIBLE
    }
}

/**
 * Allows a [ViewGroup] to inflate itself without all of the unneeded ceremony of getting a
 * [LayoutInflater] and always passing the [ViewGroup] + false. True can optionally be passed if
 * needed.
 *
 * @param layoutId The layout ID as an [Int]
 * @return The inflated [View]
 */
fun ViewGroup.inflate(layoutId: Int, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(context).inflate(layoutId, this, attachToRoot)
}

/**
 * Returns the current [String] entered into an [EditText]. Non-null, ie can return an empty String.
 */
fun EditText?.getTextString(): String {
    return this?.text?.toString() ?: ""
}

/**
 * This disables the soft keyboard as an input for a given [EditText]. The method
 * [EditText.setShowSoftInputOnFocus] is officially only available on >API21, but is actually hidden
 * from >API16. Here, we attempt to set that field to false, and catch any exception that might be
 * thrown if the Android implementation doesn't include it for some reason.
 */
@SuppressLint("NewApi")
fun EditText.disableSoftKeyboard() {
    try {
        showSoftInputOnFocus = false
    } catch (e: Exception) {
        Timber.e(e)
    }
}

/**
 * Returns a physics-based [SpringAnimation] for a given [View].
 *
 * @param property The [DynamicAnimation.ViewProperty] you wish to animate, such as rotation,
 * X or Y position etc.
 * @param finalPosition The end position for the [View] after animation complete
 * @param stiffness The stiffness of the animation, see [SpringForce]
 * @param dampingRatio The damping ratio of the animation, see [SpringForce]
 */
fun View.createSpringAnimation(
    property: DynamicAnimation.ViewProperty,
    finalPosition: Float,
    stiffness: Float,
    dampingRatio: Float
) = SpringAnimation(this, property).apply {
    spring = SpringForce(finalPosition).apply {
        this.stiffness = stiffness
        this.dampingRatio = dampingRatio
    }
}

/**
 * Loads a font via the AppCompat downloadable font system and applies it to a TextView. If this
 * call fails, it will do so silently.
 *
 * @param customFont A [CustomFont] object that encapsulates the query to be sent to the fonts provider
 */
fun TextView.setCustomFont(customFont: CustomFont) {
    loadFont(context, customFont) {
        this.typeface = it
    }
}
