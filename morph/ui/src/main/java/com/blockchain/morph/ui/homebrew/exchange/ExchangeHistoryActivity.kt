package com.blockchain.morph.ui.homebrew.exchange

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.Toolbar
import android.widget.Button
import com.blockchain.morph.ui.R

class ExchangeHistoryActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var newExchangeButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homebrew_trade_history)

        newExchangeButton = findViewById(R.id.button_new_exchange)

        newExchangeButton.setOnClickListener {
            startActivity(ExchangeActivity.intent(this, "GBP"))
        }

        // Set up toolbar
        toolbar = findViewById(R.id.toolbar)
        toolbar.navigationIcon = ContextCompat.getDrawable(this, R.drawable.vector_menu)
        toolbar.title = getString(R.string.exchange)
        setSupportActionBar(toolbar)
    }
}
