package piuk.blockchain.androidcoreui.utils.logging

import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.AddToCartEvent
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.AnswersEvent
import com.crashlytics.android.answers.ContentViewEvent
import com.crashlytics.android.answers.CustomEvent
import com.crashlytics.android.answers.LoginEvent
import com.crashlytics.android.answers.PurchaseEvent
import com.crashlytics.android.answers.ShareEvent
import com.crashlytics.android.answers.SignUpEvent
import com.crashlytics.android.answers.StartCheckoutEvent
import piuk.blockchain.androidcoreui.BuildConfig

/**
 * A singleton wrapper for the [Answers] client. All events will only be logged for release.
 *
 * Note: absolutely no identifying information should be included in an [AnswersEvent], ever.
 * These should be used to get a feel for how often features are used, but that's it.
 */
@Suppress("ConstantConditionIf")
object Logging {

    const val ITEM_TYPE_FIAT = "Fiat Currency"
    const val ITEM_TYPE_CRYPTO = "Cryptocurrency"

    private const val shouldLog = BuildConfig.USE_CRASHLYTICS

    fun logCustom(customEvent: CustomEvent) {
        if (shouldLog) Answers.getInstance().logCustom(customEvent)
    }

    fun logContentView(contentViewEvent: ContentViewEvent) {
        if (shouldLog) Answers.getInstance().logContentView(contentViewEvent)
    }

    fun logLogin(loginEvent: LoginEvent) {
        if (shouldLog) Answers.getInstance().logLogin(loginEvent)
    }

    fun logSignUp(signUpEvent: SignUpEvent) {
        if (shouldLog) Answers.getInstance().logSignUp(signUpEvent)
    }

    fun logShare(shareEvent: ShareEvent) {
        if (shouldLog) Answers.getInstance().logShare(shareEvent)
    }

    fun logPurchase(purchaseEvent: PurchaseEvent) {
        if (shouldLog) Answers.getInstance().logPurchase(purchaseEvent)
    }

    fun logAddToCart(addToCartEvent: AddToCartEvent) {
        if (shouldLog) Answers.getInstance().logAddToCart(addToCartEvent)
    }

    fun logStartCheckout(startCheckoutEvent: StartCheckoutEvent) {
        if (shouldLog) Answers.getInstance().logStartCheckout(startCheckoutEvent)
    }

    fun logException(throwable: Throwable) {
        if (shouldLog) Crashlytics.logException(throwable)
    }
}