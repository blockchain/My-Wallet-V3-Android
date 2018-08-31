package com.blockchain.notifications.koin

import android.app.NotificationManager
import android.content.Context
import com.blockchain.notifications.NotificationService
import com.blockchain.notifications.NotificationTokenManager
import com.google.firebase.iid.FirebaseInstanceId
import org.koin.dsl.module.applicationContext

val notificationModule = applicationContext {

    context("Payload") {

        bean { NotificationTokenManager(get(), get(), get(), get(), get()) }
    }

    bean { FirebaseInstanceId.getInstance() }

    factory { NotificationService(get()) }

    factory { get<Context>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
}