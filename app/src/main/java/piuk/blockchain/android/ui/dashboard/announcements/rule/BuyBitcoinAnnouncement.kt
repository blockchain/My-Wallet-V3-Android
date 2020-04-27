package piuk.blockchain.android.ui.dashboard.announcements.rule

import androidx.annotation.VisibleForTesting
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.simplebuy.SimpleBuyAvailability
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.ui.dashboard.announcements.StandardAnnouncementCard

class BuyBitcoinAnnouncement(
    dismissRecorder: DismissRecorder,
    private val simpleBuyAvailability: SimpleBuyAvailability
) : AnnouncementRule(dismissRecorder) {

    private var cta: (AnnouncementHost) -> Unit = {}
    override val dismissKey = DISMISS_KEY

    override fun shouldShow(): Single<Boolean> {
        if (dismissEntry.isDismissed) {
            return Single.just(false)
        }

        return simpleBuyAvailability.isAvailable()
            .doOnSuccess { simpleBuyAvailable ->
                if (simpleBuyAvailable) {
                    cta = {
                        it.startSimpleBuy()
                    }
                }
            }
    }

    override fun show(host: AnnouncementHost) {
        host.showAnnouncementCard(
            card = StandardAnnouncementCard(
                name = name,
                dismissRule = DismissRule.CardPeriodic,
                dismissEntry = dismissEntry,
                titleText = R.string.buy_crypto_card_title,
                bodyText = R.string.buy_crypto_card_body,
                ctaText = R.string.buy_crypto_card_cta,
                iconImage = R.drawable.ic_announce_buy_btc,
                dismissFunction = {
                    host.dismissAnnouncementCard()
                },
                ctaFunction = {
                    host.dismissAnnouncementCard()
                    host.startSimpleBuy()
                }
            )
        )
    }

    override val name = "buy_btc"

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "BuyBitcoinAuthAnnouncement_DISMISSED"
    }
}
