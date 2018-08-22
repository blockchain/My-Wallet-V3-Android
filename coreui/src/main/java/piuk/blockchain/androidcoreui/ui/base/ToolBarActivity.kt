package piuk.blockchain.androidcoreui.ui.base

import android.support.annotation.IdRes
import android.support.annotation.StringRes
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar

abstract class ToolBarActivity : AppCompatActivity() {

    /**
     * Applies the title to the [Toolbar] which is then set as the Activity's
     * SupportActionBar.
     *
     * @param toolbar The [Toolbar] for the current activity
     * @param title The title for the page, as a StringRes
     */
    fun setupToolbar(toolbar: Toolbar, @StringRes title: Int) {
        setupToolbar(toolbar, getString(title))
    }

    fun setupToolbar(@IdRes toolbar: Int, @StringRes title: Int) {
        setupToolbar(findViewById<Toolbar>(toolbar), getString(title))
    }

    /**
     * Applies the title to the [Toolbar] which is then set as the Activity's
     * SupportActionBar.
     *
     * @param toolbar The [Toolbar] for the current activity
     * @param title The title for the page, as a String
     */
    fun setupToolbar(toolbar: Toolbar, title: String) {
        toolbar.title = title
        setSupportActionBar(toolbar)
    }

    /**
     * Applies the title to the Activity's [ActionBar]. This method is the fragment equivalent
     * of [.setupToolbar].
     *
     * @param actionBar The [ActionBar] for the current activity
     * @param title The title for the page, as a StringRes
     */
    fun setupToolbar(actionBar: ActionBar, @StringRes title: Int) {
        setupToolbar(actionBar, getString(title))
    }

    /**
     * Applies the title to the Activity's [ActionBar]. This method is the fragment equivalent
     * of [.setupToolbar].
     *
     * @param actionBar The [ActionBar] for the current activity
     * @param title The title for the page, as a String
     */
    fun setupToolbar(actionBar: ActionBar, title: String) {
        actionBar.title = title
    }
}