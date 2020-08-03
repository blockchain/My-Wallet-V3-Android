package piuk.blockchain.android.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannedString
import android.text.style.ClickableSpan
import android.view.View
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

class StringUtils(private val context: Context) {

    fun getString(@StringRes stringId: Int): String {
        return context.getString(stringId)
    }

    @Deprecated("Hides warnings/errors about mismatched number of arguments TODO: Inline, AND-1297")
    fun getQuantityString(@PluralsRes pluralId: Int, size: Int): String {
        return context.resources.getQuantityString(pluralId, size, size)
    }

    @Deprecated("Hides warnings/errors about mismatched number of arguments TODO: Inline, AND-1297")
    fun getFormattedString(@StringRes stringId: Int, vararg formatArgs: Any): String {
        return context.resources.getString(stringId, *formatArgs)
    }

    fun getStringWithMappedLinks(
        @StringRes stringId: Int,
        linksMap: Map<String, Uri?>,
        launchActivity: Activity,
        onClick: () -> Unit = {}
    ): CharSequence {

        val text = context.getText(stringId)
        val rawText = text as? SpannedString ?: return text
        val out = SpannableString(rawText)

        for (annotation in rawText.getSpans(0, rawText.length, android.text.Annotation::class.java)) {
            if (annotation.key == "link") {
                out.setSpan(
                    object : ClickableSpan() {
                        override fun onClick(widget: View?) {
                            linksMap[annotation.value]?.let {
                                launchActivity.startActivity(Intent(Intent.ACTION_VIEW,
                                    it))
                            }
                            onClick()
                        }
                    },
                    rawText.getSpanStart(annotation),
                    rawText.getSpanEnd(annotation),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        return out
    }
}
