package piuk.blockchain.android.ui.customviews.callbacks

import android.view.MotionEvent
import android.view.View

interface OnTouchOutsideViewListener {

    /**
     * Called when a touch event has occurred outside a given view.
     */
    fun onTouchOutside(view: View, event: MotionEvent)
}