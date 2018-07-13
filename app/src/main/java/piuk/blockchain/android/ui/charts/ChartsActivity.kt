package piuk.blockchain.android.ui.charts

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_graphs.*
import piuk.blockchain.android.R
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.androidcore.data.charts.TimeSpan
import piuk.blockchain.androidcore.data.currency.CryptoCurrency
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseAuthActivity

class ChartsActivity : BaseAuthActivity(), TimeSpanUpdateListener {

    private val cryptoCurrency: CryptoCurrency by unsafeLazy {
        intent.getSerializableExtra(EXTRA_CRYPTOCURRENCY) as CryptoCurrency
    }
    private val bitcoin = ChartsFragment.newInstance(CryptoCurrency.BTC)
    private val ether = ChartsFragment.newInstance(CryptoCurrency.ETHER)
    private val bitcoinCash = ChartsFragment.newInstance(CryptoCurrency.BCH)

    init {
        Injector.getInstance().presenterComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graphs)
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val adapter = ChartsFragmentPagerAdapter(
            supportFragmentManager,
            bitcoin,
            ether,
            bitcoinCash
        )

        viewpager.run {
            offscreenPageLimit = 3
            setAdapter(adapter)
            indicator.setViewPager(viewpager)
        }

        when (cryptoCurrency) {
            CryptoCurrency.BTC -> viewpager.currentItem = 0
            CryptoCurrency.ETHER -> viewpager.currentItem = 1
            CryptoCurrency.BCH -> viewpager.currentItem = 2
        }

        button_close.setOnClickListener { finish() }
    }

    override fun lockScreenOrientation() = Unit

    override fun onTimeSpanUpdated(timeSpan: TimeSpan) {
        listOf(bitcoin, ether, bitcoinCash).forEach { it.onTimeSpanUpdated(timeSpan) }
    }

    companion object {

        private const val EXTRA_CRYPTOCURRENCY = "piuk.blockchain.android.EXTRA_CRYPTOCURRENCY"

        fun start(context: Context, cryptoCurrency: CryptoCurrency) {
            val intent = Intent(context, ChartsActivity::class.java).apply {
                putExtra(EXTRA_CRYPTOCURRENCY, cryptoCurrency)
            }
            context.startActivity(intent)
        }
    }

    private class ChartsFragmentPagerAdapter internal constructor(
        fragmentManager: FragmentManager,
        private val bitcoin: ChartsFragment,
        private val ether: ChartsFragment,
        private val bitcoinCash: ChartsFragment
    ) : FragmentPagerAdapter(fragmentManager) {

        override fun getItem(position: Int) = when (position) {
            0 -> bitcoin
            1 -> ether
            2 -> bitcoinCash
            else -> null
        }

        override fun getCount(): Int = NUM_ITEMS

        companion object {

            private val NUM_ITEMS = 3
        }
    }
}