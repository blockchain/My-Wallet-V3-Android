package piuk.blockchain.androidcoreui.utils

import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.text.style.TypefaceSpan

class CustomTypefaceSpan(private val newType: Typeface?) : TypefaceSpan("") {

    override fun updateDrawState(textPaint: TextPaint) {
        applyCustomTypeFace(textPaint, newType)
    }

    override fun updateMeasureState(paint: TextPaint) {
        applyCustomTypeFace(paint, newType)
    }

    private fun applyCustomTypeFace(paint: Paint, typeface: Typeface?) {
        val oldStyle: Int
        val old = paint.typeface
        oldStyle = old?.style ?: 0

        if (typeface != null) {
            val fake = oldStyle and typeface.style.inv()
            if (fake and Typeface.BOLD != 0) {
                paint.isFakeBoldText = true
            }

            if (fake and Typeface.ITALIC != 0) {
                paint.textSkewX = -0.25f
            }
        }

        paint.typeface = typeface
    }
}
