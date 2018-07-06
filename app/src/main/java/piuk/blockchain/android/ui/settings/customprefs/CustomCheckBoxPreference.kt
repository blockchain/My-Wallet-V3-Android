package piuk.blockchain.android.ui.settings.customprefs

import android.content.Context
import android.graphics.Typeface
import android.support.v7.preference.CheckBoxPreference
import android.support.v7.preference.R
import android.util.AttributeSet
import piuk.blockchain.androidcoreui.utils.extensions.applyFont
import piuk.blockchain.androidcoreui.utils.helperfunctions.CustomFont
import piuk.blockchain.androidcoreui.utils.helperfunctions.loadFont

@Suppress("unused")
class CustomCheckBoxPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.checkBoxPreferenceStyle,
    defStyleRes: Int = 0
) : CheckBoxPreference(context, attrs, defStyleAttr, defStyleRes) {

    init {
        init()
    }

    private var typeface: Typeface? = null

    private fun init() {
        loadFont(
            context,
            CustomFont.MONTSERRAT_REGULAR
        ) {
            typeface = it
            // Forces setting fonts when Summary or Title are set via XMl
            this.title = title
            this.summary = summary
        }
    }

    override fun setTitle(titleResId: Int) {
        title = context.getString(titleResId)
    }

    override fun setTitle(title: CharSequence?) {
        title?.let { super.setTitle(title.applyFont(typeface)) } ?: super.setTitle(title)
    }

    override fun setSummary(summaryResId: Int) {
        summary = context.getString(summaryResId)
    }

    override fun setSummary(summary: CharSequence?) {
        summary?.let { super.setSummary(summary.applyFont(typeface)) } ?: super.setSummary(summary)
    }
}
