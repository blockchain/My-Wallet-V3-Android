package piuk.blockchain.android.ui.balance

import android.support.annotation.DrawableRes
import android.support.annotation.StringRes

// We're moving away from this style in the future
data class ImageLeftAnnouncementCard(
    @StringRes override val title: Int,
    @StringRes override val description: Int,
    @StringRes override val link: Int,
    @DrawableRes override val image: Int,
    override val closeFunction: () -> Unit,
    override val linkFunction: () -> Unit,
    override val prefsKey: String,
    override val emoji: String? = null
) : AnnouncementData

data class ImageRightAnnouncementCard(
    @StringRes override val title: Int,
    @StringRes override val description: Int,
    @StringRes override val link: Int,
    @DrawableRes override val image: Int,
    override val closeFunction: () -> Unit,
    override val linkFunction: () -> Unit,
    override val prefsKey: String,
    override val emoji: String? = null
) : AnnouncementData

interface AnnouncementData {
    val title: Int
    val description: Int
    val link: Int
    val image: Int
    val closeFunction: () -> Unit
    val linkFunction: () -> Unit
    val prefsKey: String
    val emoji: String?
}
