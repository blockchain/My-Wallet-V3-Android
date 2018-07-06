package piuk.blockchain.android.ui.customviews

import android.content.Context
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet

class SquareImageView : AppCompatImageView {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec)

        val width = measuredWidth
        val height = measuredHeight

        // Optimization so we don't measure twice unless we need to
        if (width != height) {
            setMeasuredDimension(width, width)
        }
    }
}