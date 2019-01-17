package com.blockchain.veriff

import android.app.Activity
import android.support.v4.content.ContextCompat
import mobi.lab.veriff.data.ColorSchema
import mobi.lab.veriff.data.Veriff
import timber.log.Timber

class VeriffLauncher {

    fun launchVeriff(activity: Activity, applicant: VeriffApplicantAndToken, requestCode: Int) {
        val sessionToken = applicant.token
        Timber.d("Veriff session token: $sessionToken")
        val veriffSDK = Veriff.Builder("https://magic.veriff.me/v1/", sessionToken)
        val schema = ColorSchema.Builder()
            .setControlsColor(ContextCompat.getColor(activity, R.color.primary_blue_accent))
            .build()
        veriffSDK.setCustomColorSchema(schema)
        veriffSDK.setBackgroundImage(R.drawable.city_tartu)
        veriffSDK.launch(activity, requestCode)
    }
}
