package piuk.blockchain.androidcore.data.access

import android.content.Context

interface LogoutTimer {

    fun start(context: Context)

    fun stop(context: Context)
}
