@file:Suppress("unused")

package piuk.blockchain.androidcoreui.utils.extensions

import android.view.animation.Animation

/**
 * Sets an [AnimationListener] on an [Animation] and allows the passing of optional callbacks as
 * nullable high-order functions. This means that not all the callbacks need to be implemented.
 */
inline fun Animation.setAnimationListener(func: AnimationListener.() -> Unit) {
    val listener = AnimationListener()
    listener.func()
    setAnimationListener(listener)
}

class AnimationListener : Animation.AnimationListener {

    private var onAnimationRepeat: ((animation: Animation?) -> Unit)? = null
    private var onAnimationEnd: ((animation: Animation?) -> Unit)? = null
    private var onAnimationStart: ((animation: Animation?) -> Unit)? = null

    override fun onAnimationStart(animation: Animation?) {
        onAnimationStart?.invoke(animation)
    }

    override fun onAnimationEnd(animation: Animation?) {
        onAnimationEnd?.invoke(animation)
    }

    override fun onAnimationRepeat(animation: Animation?) {
        onAnimationRepeat?.invoke(animation)
    }

    fun onAnimationStart(func: (animation: Animation?) -> Unit) {
        onAnimationStart = func
    }

    fun onAnimationEnd(func: (animation: Animation?) -> Unit) {
        onAnimationEnd = func
    }

    fun onAnimationRepeat(func: (animation: Animation?) -> Unit) {
        onAnimationRepeat = func
    }
}