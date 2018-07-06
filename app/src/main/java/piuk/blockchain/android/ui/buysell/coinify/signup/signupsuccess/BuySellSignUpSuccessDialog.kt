package piuk.blockchain.android.ui.buysell.coinify.signup.signupsuccess

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.dialog_fragment_coinify_signup_success.*
import nl.dionsegijn.konfetti.models.Shape
import nl.dionsegijn.konfetti.models.Size
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.buysell.coinify.signup.CoinifyFlowListener
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedColor
import timber.log.Timber
import java.util.concurrent.TimeUnit

class BuySellSignUpSuccessDialog : DialogFragment() {

    private var signUpListener: CoinifyFlowListener? = null
    private val compositeDisposable = CompositeDisposable()
    private val colors by unsafeLazy {
        arrayOf(
            getResolvedColor(R.color.secondary_yellow_medium),
            getResolvedColor(R.color.primary_blue_light),
            getResolvedColor(R.color.secondary_pink_light),
            getResolvedColor(R.color.secondary_red_light),
            getResolvedColor(R.color.product_green_medium)
        ).toIntArray()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(
        R.layout.dialog_fragment_coinify_signup_success,
        container,
        false
    ).apply {
        isFocusableInTouchMode = true
        requestFocus()
        isCancelable = false
        dialog.window.setWindowAnimations(R.style.DialogNoAnimations)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button_continue.setOnClickListener { closeDialogAndNavToKyc() }

        view_konfetti.post { streamFromTop(colors) }
        view_konfetti.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                consume { burstFromCenter(colors, event.x, event.y) }
            } else false
        }

        val messageFadeIn = Completable.fromCallable {
            AlphaAnimation(0.0f, 1.0f).apply {
                duration = 1000
                startOffset = 2000
                fillAfter = true
                button_continue.alpha = 1.0f
                textview_success_message.alpha = 1.0f
                button_continue.startAnimation(this)
                textview_success_message.startAnimation(this)
            }
        }

        val backgroundAnim = Completable.fromCallable {
            val drawable = view_konfetti.background.apply { alpha = 0 }
            ObjectAnimator.ofPropertyValuesHolder(
                drawable,
                PropertyValuesHolder.ofInt("alpha", 0, 255)
            ).apply {
                target = drawable
                duration = 1000
                start()
            }
        }

        val closeDialog = Completable.timer(10, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
            .doOnComplete { closeDialogAndNavToKyc() }

        compositeDisposable.add(
            Completable.mergeArray(messageFadeIn, backgroundAnim, closeDialog)
                .doOnError { Timber.e(it) }
                .subscribe()
        )
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is CoinifyFlowListener) {
            signUpListener = context
        } else {
            throw RuntimeException("$context must implement CoinifyFlowListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        signUpListener = null
    }

    private fun closeDialogAndNavToKyc() {
        dismiss()
        signUpListener?.requestStartLetsGetToKnowYou()
    }

    private fun streamFromTop(colors: IntArray) {
        view_konfetti.build()
            .addColors(*colors)
            .setDirection(0.0, 359.0)
            .setSpeed(4f, 7f)
            .setFadeOutEnabled(true)
            .setTimeToLive(2000)
            .addShapes(Shape.RECT, Shape.CIRCLE)
            .addSizes(Size(12), Size(16, 6f))
            .setPosition(-50f, view_konfetti.width + 50f, -50f, -50f)
            .streamFor(300, 5000L)
    }

    private fun burstFromCenter(colors: IntArray, x: Float, y: Float) {
        if (!canIHaveMoreConfetti()) return
        view_konfetti.build()
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

    private fun canIHaveMoreConfetti(): Boolean = view_konfetti.getActiveSystems().size <= 3

    companion object {

        internal const val SUCCESS_FRAGMENT_ID =
            "piuk.blockchain.android.ui.buysell.coinify.signup.signupsuccess.BuySellSignUpSuccessDialog"

        internal fun newInstance(): BuySellSignUpSuccessDialog {
            return BuySellSignUpSuccessDialog().apply {
                setStyle(DialogFragment.STYLE_NO_FRAME, R.style.FullscreenDialog)
            }
        }
    }
}