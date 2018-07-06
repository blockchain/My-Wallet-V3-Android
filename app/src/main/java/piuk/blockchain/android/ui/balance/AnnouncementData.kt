package piuk.blockchain.android.ui.balance

import android.support.annotation.DrawableRes
import android.support.annotation.StringRes

data class AnnouncementData(
    @StringRes val title: Int,
    @StringRes val description: Int,
    @StringRes val link: Int,
    @DrawableRes val image: Int,
    val emoji: String?,
    val closeFunction: () -> Unit,
    val linkFunction: () -> Unit,
    val prefsKey: String
)