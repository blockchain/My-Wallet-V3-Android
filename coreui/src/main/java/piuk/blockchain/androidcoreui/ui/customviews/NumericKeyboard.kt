package piuk.blockchain.androidcoreui.ui.customviews

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.view_numeric_keyboard.view.*
import piuk.blockchain.androidcoreui.R
import piuk.blockchain.androidcoreui.utils.ViewUtils
import piuk.blockchain.androidcoreui.utils.extensions.getTextString
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.visible

class NumericKeyboard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), View.OnClickListener {

    val isVisible: Boolean
        get() = visibility == View.VISIBLE

    private val viewList: MutableList<EditText> = mutableListOf()
    private var decimalSeparator = "."
    private var callback: NumericKeyboardCallback? = null

    init {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.BOTTOM

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.view_numeric_keyboard, this, true)

        button_0.setOnClickListener(this)
        button_1.setOnClickListener(this)
        button_2.setOnClickListener(this)
        button_3.setOnClickListener(this)
        button_4.setOnClickListener(this)
        button_5.setOnClickListener(this)
        button_6.setOnClickListener(this)
        button_7.setOnClickListener(this)
        button_8.setOnClickListener(this)
        button_9.setOnClickListener(this)
        button_separator.setOnClickListener(this)
        button_delete.setOnClickListener(this)
        button_done.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        var pad: String? = null
        when (v.id) {
            R.id.button_separator -> pad = decimalSeparator
            R.id.button_delete -> deleteFromFocusedView()
            R.id.button_done -> setNumpadVisibility(View.GONE)
            else -> pad = v.tag.toString().substring(0, 1)
        }

        // Append tapped #
        pad?.let { appendToFocusedView(it) }
    }

    fun enableOnView(view: EditText) {
        if (!viewList.contains(view)) viewList.add(view)

        view.setTextIsSelectable(true)
        view.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                val view1 = (context as Activity).currentFocus
                if (view1 != null) {
                    val inputManager =
                        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputManager.hideSoftInputFromWindow(
                        view1.windowToken,
                        InputMethodManager.HIDE_NOT_ALWAYS
                    )
                }
                setNumpadVisibility(View.VISIBLE)
            }
        }

        view.setOnClickListener {
            decimal_point.text = decimalSeparator
            setNumpadVisibility(View.VISIBLE)
        }
    }

    fun setCallback(callback: NumericKeyboardCallback) {
        this.callback = callback
    }

    fun setDecimalSeparator(passedDecimalSeparator: String) {
        decimalSeparator = passedDecimalSeparator
    }

    @SuppressLint("SwitchIntDef")
    fun setNumpadVisibility(@ViewUtils.Visibility visibility: Int) {
        when (visibility) {
            View.VISIBLE -> showKeyboard()
            else -> hideKeyboard()
        }
    }

    private fun showKeyboard() {
        if (!isVisible) {
            startAnimation(AnimationUtils.loadAnimation(context, R.anim.bottom_up).apply {
                setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation) {
                        // No-op
                    }

                    override fun onAnimationEnd(animation: Animation) {
                        callback?.onKeypadOpenCompleted()
                    }

                    override fun onAnimationRepeat(animation: Animation) {
                        // No-op
                    }
                })
            })
            visible()
            callback?.onKeypadOpen()
        }
    }

    fun hideKeyboard() {
        if (isVisible) {
            startAnimation(AnimationUtils.loadAnimation(context, R.anim.top_down))
            gone()
            callback?.onKeypadClose()
        }
    }

    private fun appendToFocusedView(pad: String) {
        for (view in viewList) {
            if (view.hasFocus()) {
                // Don't allow multiple decimals
                if (pad == decimalSeparator && view.text.toString().contains(decimalSeparator))
                    return

                val startSelection = view.selectionStart
                val endSelection = view.selectionEnd
                if (endSelection - startSelection > 0) {
                    val selectedText = view.text.toString().substring(startSelection, endSelection)
                    view.setText(view.getTextString().replace(selectedText, pad))
                } else {
                    val currentString = view.text.toString()
                    val newString =
                        "${currentString.substring(0, startSelection)}$pad${currentString.substring(
                            endSelection,
                            currentString.length
                        )}"
                    view.setText(newString)
                }

                if (view.text.isNotEmpty()) {
                    if (endSelection < view.text.length) {
                        view.setSelection(endSelection + 1)
                    } else {
                        view.setSelection(view.text.length)
                    }
                }
            }
        }
    }

    private fun deleteFromFocusedView() {
        for (view in viewList) {
            if (view.hasFocus() && view.text.isNotEmpty()) {
                view.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                view.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
            }
        }
    }
}