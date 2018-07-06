package piuk.blockchain.androidcoreui.utils

import android.app.Activity
import android.content.Context
import android.support.v7.app.AlertDialog
import android.view.MotionEvent
import piuk.blockchain.androidcore.utils.PersistentPrefs.KEY_OVERLAY_TRUSTED
import piuk.blockchain.androidcore.utils.PrefsUtil
import piuk.blockchain.androidcoreui.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OverlayDetection @Inject constructor(private val preferences: PrefsUtil) {

    private var alertDialog: AlertDialog? = null

    fun detectObscuredWindow(context: Context, event: MotionEvent): Boolean {
        // Detect if touch events are being obscured by hidden overlays - These could be used for tapjacking
        if (!preferences.getValue(KEY_OVERLAY_TRUSTED, false) &&
            event.flags and MotionEvent.FLAG_WINDOW_IS_OBSCURED != 0
        ) {

            // Prevent multiple popups
            alertDialog?.dismiss()

            alertDialog = AlertDialog.Builder(context, R.style.AlertDialogStyle)
                .setTitle(R.string.screen_overlay_warning)
                .setMessage(R.string.screen_overlay_note)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_continue) { _, _ ->
                    preferences.setValue(
                        KEY_OVERLAY_TRUSTED,
                        true
                    )
                }
                .setNegativeButton(R.string.exit) { _, _ -> (context as Activity).finish() }
                .show()
            return true
        } else {
            return false
        }
    }
}