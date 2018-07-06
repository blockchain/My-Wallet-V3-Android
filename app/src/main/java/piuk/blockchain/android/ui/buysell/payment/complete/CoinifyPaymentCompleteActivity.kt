package piuk.blockchain.android.ui.buysell.payment.complete

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.MotionEvent
import nl.dionsegijn.konfetti.models.Shape
import nl.dionsegijn.konfetti.models.Size
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.buysell.payment.card.PaymentState
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseAuthActivity
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedColor
import piuk.blockchain.androidcoreui.utils.extensions.invisibleIf
import kotlinx.android.synthetic.main.activity_card_payment_complete.button_close as buttonClose
import kotlinx.android.synthetic.main.activity_card_payment_complete.image_view_tick as imageViewTick
import kotlinx.android.synthetic.main.activity_card_payment_complete.konfetti_view_success as konfetti
import kotlinx.android.synthetic.main.activity_card_payment_complete.text_view_failure_message as textViewFailureMessage
import kotlinx.android.synthetic.main.activity_card_payment_complete.text_view_success_message as textViewSuccessMessage
import kotlinx.android.synthetic.main.activity_card_payment_complete.text_view_success_title as textViewSuccessTitle
import kotlinx.android.synthetic.main.toolbar_general.toolbar_general as toolBar

class CoinifyPaymentCompleteActivity : BaseAuthActivity() {

    private val state by unsafeLazy { intent.getSerializableExtra(EXTRA_PAYMENT_STATE) as PaymentState }
    private val colors by unsafeLazy {
        arrayOf(
            getResolvedColor(R.color.secondary_yellow_medium),
            getResolvedColor(R.color.primary_blue_light),
            getResolvedColor(R.color.secondary_pink_light),
            getResolvedColor(R.color.secondary_red_light),
            getResolvedColor(R.color.product_green_medium)
        ).toIntArray()
    }
    private val successViews by unsafeLazy {
        listOf(
            konfetti,
            textViewSuccessTitle,
            textViewSuccessMessage
        )
    }
    private val failureViews by unsafeLazy {
        listOf(textViewFailureMessage)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_payment_complete)

        buttonClose.setOnClickListener { onSupportNavigateUp() }

        when (state) {
            PaymentState.SUCCESS -> displaySuccess()
            PaymentState.CANCELLED -> displayCancelled()
            PaymentState.EXPIRED -> displayExpired()
            PaymentState.DECLINED, PaymentState.REJECTED, PaymentState.FAILED -> displayRejected()
            PaymentState.PENDING -> displayReviewing()
        }

        val title = when (state) {
            PaymentState.SUCCESS -> R.string.buy_sell_card_order_created_title
            PaymentState.CANCELLED -> R.string.buy_sell_card_order_cancelled_title
            PaymentState.EXPIRED -> R.string.buy_sell_card_order_expired_title
            PaymentState.DECLINED -> R.string.buy_sell_card_order_declined_title
            PaymentState.REJECTED -> R.string.buy_sell_card_order_rejected_title
            PaymentState.FAILED -> R.string.buy_sell_card_order_failed_title
            PaymentState.PENDING -> R.string.buy_sell_card_order_in_review_title
        }

        setupToolbar(toolBar, title)
    }

    private fun displaySuccess() {
        updateVisibility(true)

        konfetti.post { streamFromTop(colors) }
        konfetti.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                consume { burstFromCenter(colors, event.x, event.y) }
            } else false
        }
    }

    private fun updateVisibility(success: Boolean) {
        successViews.forEach { it.invisibleIf { !success } }
        failureViews.forEach { it.invisibleIf { success } }
    }

    private fun displayCancelled() {
        updateVisibility(false)
        textViewFailureMessage.setText(R.string.buy_sell_card_order_cancelled_message)
        imageViewTick.setImageDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.shapeshift_progress_failed
            )
        )
    }

    private fun displayExpired() {
        updateVisibility(false)
        textViewFailureMessage.setText(R.string.buy_sell_card_order_expired_message)
        imageViewTick.setImageDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.shapeshift_progress_failed
            )
        )
    }

    private fun displayRejected() {
        updateVisibility(false)
        textViewFailureMessage.setText(R.string.buy_sell_card_order_failed_message)
        imageViewTick.setImageDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.shapeshift_progress_failed
            )
        )
    }

    private fun displayReviewing() {
        updateVisibility(false)
        textViewFailureMessage.setText(R.string.buy_sell_card_order_reviewing_message)
        imageViewTick.setImageDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.shapeshift_drawable_in_progress
            )
        )
    }

    private fun streamFromTop(colors: IntArray) {
        konfetti.build()
            .addColors(*colors)
            .setDirection(0.0, 359.0)
            .setSpeed(4f, 7f)
            .setFadeOutEnabled(true)
            .setTimeToLive(2000)
            .addShapes(Shape.RECT, Shape.CIRCLE)
            .addSizes(Size(12), Size(16, 6f))
            .setPosition(-50f, konfetti.width + 50f, -50f, -50f)
            .streamFor(300, 5000L)
    }

    private fun burstFromCenter(colors: IntArray, x: Float, y: Float) {
        if (!canIHaveMoreConfetti()) return
        konfetti.build()
            .addColors(*colors)
            .setDirection(0.0, 359.0)
            .setSpeed(4f, 7f)
            .setFadeOutEnabled(true)
            .setTimeToLive(2000)
            .addShapes(Shape.RECT, Shape.CIRCLE)
            .addSizes(Size(12), Size(16, 6f))
            .setPosition(x, y)
            .burst(100)
    }

    private fun canIHaveMoreConfetti(): Boolean = konfetti.getActiveSystems().size <= 3

    override fun onSupportNavigateUp(): Boolean = consume { finish() }

    companion object {

        private const val EXTRA_PAYMENT_STATE =
            "piuk.blockchain.android.ui.buysell.payment.card.EXTRA_REDIRECT_URL"

        fun start(
            activity: Activity,
            paymentState: PaymentState
        ) {
            Intent(activity, CoinifyPaymentCompleteActivity::class.java).apply {
                putExtra(EXTRA_PAYMENT_STATE, paymentState)
            }.run { activity.startActivity(this) }
        }
    }
}