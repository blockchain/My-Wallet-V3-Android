package com.blockchain.morph.ui.homebrew.exchange

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.blockchain.morph.exchange.mvi.FloatKeyboardIntent
import com.blockchain.morph.exchange.mvi.FloatKeyboardDialog
import com.blockchain.morph.ui.R
import io.reactivex.subjects.BehaviorSubject

class FloatKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val values = BehaviorSubject.create<FloatKeyboardIntent>()

    val viewStates = FloatKeyboardDialog(values).states

    init {
        inflate(context)
        wireUpNumbers()
        wireUpBackSpace()
        wireUpPeriod()
    }

    private fun inflate(context: Context) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.float_keyboard, this, true)
    }

    private fun wireUpBackSpace() {
        findViewById<View>(R.id.numberBackSpace)
            .apply {
                setOnClickListener(backspaceClick())
                setOnLongClickListener(clearValue())
            }
    }

    private fun wireUpPeriod() {
        findViewById<View>(R.id.numberPeriod)
            .apply {
                setOnClickListener(periodClick())
            }
    }

    private fun wireUpNumbers() {
        numberMap.forEach { (id, value) ->
            findViewById<View>(id)
                .setOnClickListener(numberClick(value))
        }
    }

    private fun numberClick(intent: FloatKeyboardIntent.NumericKey) = OnClickListener { values.onNext(intent) }

    private fun backspaceClick() = OnClickListener { values.onNext(FloatKeyboardIntent.Backspace()) }

    private fun periodClick() = OnClickListener { values.onNext(FloatKeyboardIntent.Period()) }

    private fun clearValue() = OnLongClickListener {
        values.onNext(FloatKeyboardIntent.Clear())
        true
    }
}

private val numberMap = listOf(
    R.id.number0 to 0,
    R.id.number1 to 1,
    R.id.number2 to 2,
    R.id.number3 to 3,
    R.id.number4 to 4,
    R.id.number5 to 5,
    R.id.number6 to 6,
    R.id.number7 to 7,
    R.id.number8 to 8,
    R.id.number9 to 9
).map { (id, value) -> id to FloatKeyboardIntent.NumericKey(value) }
    .toMap()
