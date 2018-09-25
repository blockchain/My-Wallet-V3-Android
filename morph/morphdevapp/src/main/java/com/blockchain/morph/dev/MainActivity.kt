package com.blockchain.morph.dev

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.blockchain.morph.ui.homebrew.exchange.host.HomebrewNavHostActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun launchExchange(view: View) {
        HomebrewNavHostActivity.start(this, "USD")
    }
}
