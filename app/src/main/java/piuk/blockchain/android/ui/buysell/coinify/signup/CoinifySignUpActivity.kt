package piuk.blockchain.android.ui.buysell.coinify.signup

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.view.animation.DecelerateInterpolator
import kotlinx.android.synthetic.main.activity_coinify_signup.*
import kotlinx.android.synthetic.main.include_buysell_signup_progress.*
import kotlinx.android.synthetic.main.toolbar_general.*
import piuk.blockchain.android.R
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.buysell.coinify.signup.createaccountcompleted.CoinifyCreateAccountCompletedFragment
import piuk.blockchain.android.ui.buysell.coinify.signup.createaccountstart.CoinifyCreateAccountStartFragment
import piuk.blockchain.android.ui.buysell.coinify.signup.identityinreview.CoinifyIdentityInReviewFragment
import piuk.blockchain.android.ui.buysell.coinify.signup.invalidcountry.CoinifyInvalidCountryFragment
import piuk.blockchain.android.ui.buysell.coinify.signup.kyc.CoinifyKycActivity
import piuk.blockchain.android.ui.buysell.coinify.signup.selectcountry.CoinifySelectCountryFragment
import piuk.blockchain.android.ui.buysell.coinify.signup.signupsuccess.BuySellSignUpSuccessDialog
import piuk.blockchain.android.ui.buysell.coinify.signup.verifyemail.CoinifyVerifyEmailFragment
import piuk.blockchain.android.ui.buysell.overview.CoinifyOverviewActivity
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedColor
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.toast
import piuk.blockchain.androidcoreui.utils.extensions.visible
import javax.inject.Inject

class CoinifySignUpActivity : BaseMvpActivity<CoinifySignupView, CoinifySignUpPresenter>(),
    CoinifySignupView,
    FragmentManager.OnBackStackChangedListener,
    CoinifyFlowListener {

    @Inject
    lateinit var presenter: CoinifySignUpPresenter

    init {
        Injector.getInstance().presenterComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coinify_signup)

        setupToolbar(R.string.buy_sell)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        supportFragmentManager.addOnBackStackChangedListener(this)

        buysellSignupProgressBar.max = 100 * 10

        onViewReady()
    }

    private fun setupToolbar(title: Int) {
        setupToolbar(toolbar_general, title)
    }

    override fun onBackStackChanged() {
        val currentFragment = supportFragmentManager.findFragmentByTag(CURRENT_FRAGMENT_TAG)
        val title = when (currentFragment) {
            is CoinifyCreateAccountStartFragment -> R.string.buy_sell
            is CoinifySelectCountryFragment -> R.string.buy_sell_create_account
            is CoinifyVerifyEmailFragment -> R.string.buy_sell_create_account
            is CoinifyCreateAccountCompletedFragment -> R.string.buy_sell_identification_verification
            else -> R.string.buy_sell
        }

        setupToolbar(title)
        onProgressUpdate(currentFragment)
    }

    override fun onProgressUpdate(currentFragment: Fragment) {
        when (currentFragment) {
            is CoinifyCreateAccountStartFragment -> progressBar(0)
            is CoinifySelectCountryFragment -> progressBar(1)
            is CoinifyVerifyEmailFragment -> progressBar(50)
            is CoinifyCreateAccountCompletedFragment -> progressBar(100)
            else -> progressBar(1)
        }
    }

    private fun progressBar(progress: Int) {
        val icon1Color: Int
        val icon2Color: Int
        val icon3Color: Int

        when (progress) {
            in 1..49 -> {
                signupProgressLayout.visible()
                icon1Color = R.color.primary_blue_accent
                icon2Color = R.color.primary_gray_light
                icon3Color = R.color.primary_gray_light
            }
            in 50..99 -> {
                signupProgressLayout.visible()
                icon1Color = R.color.primary_blue_accent
                icon2Color = R.color.primary_blue_accent
                icon3Color = R.color.primary_gray_light
            }
            100 -> {
                signupProgressLayout.visible()
                icon1Color = R.color.primary_blue_accent
                icon2Color = R.color.primary_blue_accent
                icon3Color = R.color.primary_blue_accent
            }
            else -> {
                signupProgressLayout.gone()
                icon1Color = R.color.primary_gray_light
                icon2Color = R.color.primary_gray_light
                icon3Color = R.color.primary_gray_light
            }
        }

        imageView1.setColorFilter(getResolvedColor(icon1Color))
        imageView2.setColorFilter(getResolvedColor(icon2Color))
        imageView3.setColorFilter(getResolvedColor(icon3Color))

        animateProgressBar(progress)
    }

    private fun animateProgressBar(progress: Int) {
        ObjectAnimator.ofInt(
            buysellSignupProgressBar,
            "progress",
            buysellSignupProgressBar.progress,
            progress * 10
        ).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    override fun onSupportNavigateUp(): Boolean = consume { onBackPressed() }

    override fun requestStartSelectCountry() {
        onStartSelectCountry()
    }

    override fun requestStartVerifyEmail(countryCode: String) {
        onStartVerifyEmail(countryCode)
    }

    override fun requestStartCreateAccount() {
        onStartCreateAccountCompleted()
    }

    override fun requestStartVerifyIdentification() {
        presenter.continueVerifyIdentification()
    }

    override fun requestStartOverview() {
        onStartOverview()
    }

    override fun requestStartInvalidCountry() {
        onStartInvalidCountry()
    }

    override fun requestStartSignUpSuccess() {
        onStartSignUpSuccess()
    }

    override fun onFinish() {
        finish()
    }

    override fun requestStartLetsGetToKnowYou() {
        onStartCreateAccountCompleted()
    }

    override fun requestFinish() {
        finish()
    }

    override fun onStartWelcome() {
        replaceFragment(CoinifyCreateAccountStartFragment.newInstance())
    }

    override fun onStartSelectCountry() {
        addFragmentToBackStack(CoinifySelectCountryFragment.newInstance())
    }

    override fun onStartVerifyEmail(countryCode: String) {
        addFragmentToBackStack(CoinifyVerifyEmailFragment.newInstance(countryCode))
    }

    override fun onStartCreateAccountCompleted() {
        progressBar(100)
        setupToolbar(R.string.buy_sell_identification_verification)
        replaceFragment(CoinifyCreateAccountCompletedFragment.newInstance())
    }

    override fun startLogoutTimer() {
        // No-op
    }

    override fun onStartVerifyIdentification(redirectUrl: String, externalKycId: String) {
        CoinifyKycActivity.startForResult(
            this,
            redirectUrl,
            externalKycId,
            REQUEST_CODE_COINIFY_KYC_WEB_VIEW
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_COINIFY_KYC_WEB_VIEW) {
            onStartReviewInProgress()
        } else super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onStartReviewInProgress() {
        setupToolbar(R.string.buy_sell_verification_in_review)
        progressBar(0)
        replaceFragment(CoinifyIdentityInReviewFragment.newInstance())
    }

    override fun onStartOverview() {
        CoinifyOverviewActivity.start(this)
        finish()
    }

    override fun onStartInvalidCountry() {
        replaceFragment(CoinifyInvalidCountryFragment.newInstance())
        progressBar(0)
        setupToolbar(R.string.buy_sell)
    }

    private fun onStartSignUpSuccess() {
        BuySellSignUpSuccessDialog.newInstance()
            .show(supportFragmentManager, BuySellSignUpSuccessDialog.SUCCESS_FRAGMENT_ID)
    }

    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.content_frame, fragment, CURRENT_FRAGMENT_TAG)
            .commitAllowingStateLoss()
    }

    private fun addFragmentToBackStack(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        fragmentManager.beginTransaction()
            .addToBackStack(fragment.javaClass.name)
            .add(R.id.content_frame, fragment, CURRENT_FRAGMENT_TAG)
            .commitAllowingStateLoss()
    }

    override fun onBackPressed() {
        when (supportFragmentManager.fragments.size) {
            1 -> finish()
            else -> super.onBackPressed()
        }
    }

    override fun showToast(errorDescription: String) {
        toast(errorDescription, ToastCustom.TYPE_ERROR)
    }

    override fun createPresenter() = presenter

    override fun getView() = this

    companion object {

        private const val CURRENT_FRAGMENT_TAG =
            "piuk.blockchain.android.ui.buysell.coinify.signup.CoinifySignUpActivity.CURRENT_FRAGMENT_TAG"
        private const val REQUEST_CODE_COINIFY_KYC_WEB_VIEW = 8765

        fun start(context: Context) {
            val intent = Intent(context, CoinifySignUpActivity::class.java)
            context.startActivity(intent)
        }
    }
}

interface CoinifyFlowListener {

    fun requestStartSelectCountry()

    fun requestStartVerifyEmail(countryCode: String)

    fun requestStartCreateAccount()

    fun requestStartLetsGetToKnowYou()

    fun requestStartVerifyIdentification()

    fun requestStartOverview()

    fun requestStartInvalidCountry()

    fun requestStartSignUpSuccess()

    fun requestFinish()
}