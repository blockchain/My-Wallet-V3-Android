package com.blockchain.notifications

import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService
import org.koin.android.ext.android.inject
import timber.log.Timber

class InstanceIdService : FirebaseInstanceIdService() {

    private val notificationTokenManager: NotificationTokenManager by inject()

    init {
        Timber.d("InstanceIdService: constructor instantiated")
    }

    override fun onTokenRefresh() {
        // Get updated InstanceID token.
        val refreshedToken = FirebaseInstanceId.getInstance().token
        Timber.d("Refreshed token: %s", refreshedToken)

        if (refreshedToken != null) {
            notificationTokenManager.storeAndUpdateToken(refreshedToken)
        }
    }
}
