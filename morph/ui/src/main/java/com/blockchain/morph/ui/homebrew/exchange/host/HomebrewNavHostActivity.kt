package com.blockchain.morph.ui.homebrew.exchange.host

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.app.Fragment
import android.support.v7.widget.Toolbar
import androidx.navigation.fragment.NavHostFragment.findNavController
import com.blockchain.morph.ui.R
import com.blockchain.morph.ui.homebrew.exchange.ExchangeFragment
import com.blockchain.morph.ui.homebrew.exchange.confirmation.ExchangeConfirmationFragment
import com.blockchain.morph.ui.homebrew.exchange.confirmation.ExchangeConfirmationModel
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseAuthActivity

class HomebrewNavHostActivity : BaseAuthActivity(), HomebrewHostActivityListener {

    private val toolbar by unsafeLazy { findViewById<Toolbar>(R.id.toolbar_general) }
    private val navHostFragment by unsafeLazy { supportFragmentManager.findFragmentById(R.id.nav_host) }
    private val navController by unsafeLazy { findNavController(navHostFragment) }
    private val currentFragment: Fragment?
        get() = navHostFragment.childFragmentManager.findFragmentById(R.id.nav_host)

    private val defaultCurrency by unsafeLazy { intent.getStringExtra(EXTRA_DEFAULT_CURRENCY) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homebrew_host)

        val args = ExchangeFragment.bundleArgs(defaultCurrency)
        navController.navigate(R.id.exchangeFragment, args)
    }

    override fun onSupportNavigateUp(): Boolean =
        if (currentFragment is ExchangeConfirmationFragment) {
            consume { navController.popBackStack() }
        } else {
            consume { finish() }
        }

    override fun setToolbarTitle(title: Int) {
        setupToolbar(toolbar, title)
    }

    override fun launchConfirmation(confirmationModel: ExchangeConfirmationModel) {
        val args = ExchangeConfirmationFragment.bundleArgs(confirmationModel)
        navController.navigate(R.id.exchangeConfirmationFragment, args)
    }

    companion object {

        private const val EXTRA_DEFAULT_CURRENCY =
            "com.blockchain.morph.ui.homebrew.exchange.EXTRA_DEFAULT_CURRENCY"

        @JvmStatic
        @JvmOverloads
        fun start(context: Context, defaultCurrency: String = "USD") {
            Intent(context, HomebrewNavHostActivity::class.java).apply {
                putExtra(EXTRA_DEFAULT_CURRENCY, defaultCurrency)
            }.run { context.startActivity(this) }
        }
    }
}

internal interface HomebrewHostActivityListener {

    fun setToolbarTitle(@StringRes title: Int)

    fun launchConfirmation(confirmationModel: ExchangeConfirmationModel)
}