package piuk.blockchain.kycdemo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun launchKycFlow(view: View) {
        Toast.makeText(this, "Todo!", Toast.LENGTH_SHORT).show()
    }
}
