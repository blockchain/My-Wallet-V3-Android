package piuk.blockchain.android.data.notifications

import android.app.LauncherActivity
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import com.blockchain.notifications.NotificationsUtil
import com.blockchain.notifications.R
import com.blockchain.notifications.models.NotificationPayload
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.koin.android.ext.android.inject
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.PrefsUtil
import piuk.blockchain.androidcoreui.ApplicationLifeCycle
import timber.log.Timber

class FcmCallbackService : FirebaseMessagingService() {

    private val notificationManager: NotificationManager by inject()
    private val prefsUtil: PrefsUtil by inject()
    private val rxBus: RxBus by inject()

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        // Check if message contains a data payload.
        if (remoteMessage?.data?.isEmpty() == false) {
            Timber.d("Message data payload: %s", remoteMessage.data)

            // Parse data, emit events
            val payload = NotificationPayload(remoteMessage.data)
            rxBus.emitEvent(NotificationPayload::class.java, payload)
            sendNotification(payload)
        }
    }

    private fun sendNotification(payload: NotificationPayload) {
        if (ApplicationLifeCycle.getInstance().isForeground &&
            AccessState.getInstance().isLoggedIn
        ) {
            sendForegroundNotification(payload)
        } else {
            sendBackgroundNotification(payload)
        }
    }

    /**
     * Redirects the user to the [LauncherActivity] which will then handle the routing
     * appropriately - ie if accepted Contact, show
     * [piuk.blockchain.android.ui.contacts.list.ContactsListActivity], otherwise show [MainActivity].
     */
    private fun sendBackgroundNotification(payload: NotificationPayload) {
        val notifyIntent = Intent(applicationContext, LauncherActivity::class.java)
        if (payload.type != null &&
            payload.type == NotificationPayload.NotificationType.CONTACT_REQUEST
        ) {
            notifyIntent.putExtra(EXTRA_CONTACT_ACCEPTED, true)
        }

        val intent = PendingIntent.getActivity(
            applicationContext,
            0,
            notifyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        triggerHeadsUpNotification(
            payload, intent,
            ID_BACKGROUND_NOTIFICATION
        )
    }

    /**
     * Redirects the user to the [MainActivity] which will then launch the balance fragment by
     * default. If notification is from an accepted Contact, [MainActivity] will then launch
     * [piuk.blockchain.android.ui.contacts.list.ContactsListActivity] once startup checks
     * have finished.
     */
    private fun sendForegroundNotification(payload: NotificationPayload) {
        val notifyIntent = Intent(applicationContext, MainActivity::class.java)
        if (payload.type != null &&
            payload.type == NotificationPayload.NotificationType.CONTACT_REQUEST
        ) {
            prefsUtil.setValue(PrefsUtil.KEY_CONTACTS_NOTIFICATION, true)
        }
        val intent = PendingIntent.getActivity(
            applicationContext,
            0,
            notifyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        triggerHeadsUpNotification(
            payload, intent,
            ID_FOREGROUND_NOTIFICATION
        )
    }

    /**
     * Triggers a notification with the "Heads Up" feature on >21, with the "beep" sound and a short
     * vibration enabled.
     *
     * @param payload A [NotificationPayload] object from the Notification Service
     * @param pendingIntent The [PendingIntent] that you wish to be called when the
     * notification is selected
     * @param notificationId The ID of the notification
     */
    private fun triggerHeadsUpNotification(
        payload: NotificationPayload,
        pendingIntent: PendingIntent,
        notificationId: Int
    ) {

        NotificationsUtil(applicationContext, notificationManager).triggerNotification(
            payload.title ?: "",
            payload.title ?: "",
            payload.body ?: "",
            R.drawable.ic_notification_white,
            pendingIntent,
            notificationId
        )
    }

    companion object {

        const val EXTRA_CONTACT_ACCEPTED = "contact_accepted"
        const val ID_BACKGROUND_NOTIFICATION = 1337
        const val ID_FOREGROUND_NOTIFICATION = 1338
    }
}
