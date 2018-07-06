package piuk.blockchain.android.ui.swipetoreceive

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AlertDialog
import android.support.v7.widget.AppCompatImageView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.fragment_swipe_to_receive.*
import piuk.blockchain.android.R
import piuk.blockchain.android.data.websocket.WebSocketService
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.androidcoreui.ui.base.BaseFragment
import piuk.blockchain.androidcoreui.ui.base.UiState
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.invisible
import piuk.blockchain.androidcoreui.utils.extensions.toast
import piuk.blockchain.androidcoreui.utils.extensions.visible
import piuk.blockchain.androidcoreui.utils.helperfunctions.setOnPageChangeListener
import javax.inject.Inject

@Suppress("MemberVisibilityCanPrivate")
class SwipeToReceiveFragment : BaseFragment<SwipeToReceiveView, SwipeToReceivePresenter>(),
    SwipeToReceiveView {

    @Suppress("MemberVisibilityCanBePrivate")
    @Inject
    lateinit var swipeToReceivePresenter: SwipeToReceivePresenter

    init {
        Injector.getInstance().presenterComponent.inject(this)
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == WebSocketService.ACTION_INTENT) {
                // Update UI with new Address + QR
                presenter?.currencyPosition = presenter?.currencyPosition ?: 0
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_swipe_to_receive)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listOf(imageview_qr, textview_address, textview_request_currency).forEach {
            it.setOnClickListener { showClipboardWarning() }
        }

        imageview_left_arrow.setOnClickListener {
            viewpager_icons.currentItem = viewpager_icons.currentItem - 1
        }
        imageview_right_arrow.setOnClickListener {
            viewpager_icons.currentItem = viewpager_icons.currentItem + 1
        }

        val adapter = ImageAdapter(
            context!!,
            listOf(
                R.drawable.vector_bitcoin,
                R.drawable.vector_eth,
                R.drawable.vector_bitcoin_cash
            )
        )

        viewpager_icons.run {
            offscreenPageLimit = 3
            setAdapter(adapter)
            indicator.setViewPager(this)
            setOnPageChangeListener {
                onPageSelected {
                    presenter.currencyPosition = it

                    when (it) {
                        0 -> imageview_left_arrow.invisible()
                        1 -> listOf(
                            imageview_left_arrow,
                            imageview_right_arrow
                        ).forEach { it.visible() }
                        2 -> imageview_right_arrow.invisible()
                    }
                }
            }
        }

        onViewReady()
    }

    override fun displayReceiveAddress(address: String) {
        textview_address.text = address
    }

    override fun displayReceiveAccount(accountName: String) {
        textview_account.text = accountName
    }

    override fun displayCoinType(requestString: String) {
        textview_request_currency.text = requestString
    }

    override fun setUiState(uiState: Int) {
        when (uiState) {
            UiState.LOADING -> displayLoading()
            UiState.CONTENT -> showContent()
            UiState.FAILURE -> showNoAddressesAvailable()
            UiState.EMPTY -> showNoAddressesAvailable()
        }
    }

    override fun displayQrCode(bitmap: Bitmap) {
        imageview_qr.setImageBitmap(bitmap)
    }

    override fun onStop() {
        super.onStop()
        context?.run {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
        }
    }

    override fun onStart() {
        super.onStart()
        context?.run {
            LocalBroadcastManager.getInstance(this).registerReceiver(
                broadcastReceiver,
                IntentFilter(WebSocketService.ACTION_INTENT)
            )
        }
    }

    override fun createPresenter() = swipeToReceivePresenter

    override fun getMvpView() = this

    private fun showContent() {
        layout_qr.visible()
        progress_bar.gone()
        imageview_qr.visible()
        textview_error.gone()
    }

    private fun displayLoading() {
        layout_qr.visible()
        progress_bar.visible()
        imageview_qr.invisible()
        textview_error.gone()
    }

    private fun showNoAddressesAvailable() {
        layout_qr.invisible()
        textview_error.visible()
        textview_address.text = ""
    }

    private fun showClipboardWarning() {
        val address = textview_address.text
        activity?.run {
            AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.receive_address_to_clipboard)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, { _, _ ->
                    val clipboard =
                        getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Send address", address)
                    toast(R.string.copied_to_clipboard)
                    clipboard.primaryClip = clip
                })
                .setNegativeButton(R.string.no, null)
                .show()
        }
    }

    companion object {

        @JvmStatic
        fun newInstance(): SwipeToReceiveFragment = SwipeToReceiveFragment()
    }

    private class ImageAdapter(var context: Context, var drawables: List<Int>) : PagerAdapter() {

        override fun getCount(): Int = drawables.size

        override fun isViewFromObject(view: View, any: Any): Boolean {
            return view === any as AppCompatImageView
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val imageView = container.inflate(R.layout.item_image_pager) as ImageView
            imageView.setImageDrawable(ContextCompat.getDrawable(context, drawables[position]))
            (container as ViewPager).addView(imageView, 0)
            return imageView
        }

        override fun destroyItem(container: ViewGroup, position: Int, any: Any) {
            container.removeView(any as LinearLayout)
        }
    }
}
