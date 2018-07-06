package piuk.blockchain.android.ui.backup.wordlist

import android.app.FragmentTransaction
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import kotlinx.android.synthetic.main.fragment_backup_word_list.*
import piuk.blockchain.android.R
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.backup.verify.BackupWalletVerifyFragment
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseFragment
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.invisible
import piuk.blockchain.androidcoreui.utils.extensions.visible
import javax.inject.Inject

class BackupWalletWordListFragment :
    BaseFragment<BackupWalletWordListView, BackupWalletWordListPresenter>(),
    BackupWalletWordListView {

    @Inject
    lateinit var backupWalletWordListPresenter: BackupWalletWordListPresenter

    private val animEnterFromRight: Animation by unsafeLazy {
        AnimationUtils.loadAnimation(
            activity,
            R.anim.enter_from_right
        )
    }
    private val animEnterFromLeft: Animation by unsafeLazy {
        AnimationUtils.loadAnimation(
            activity,
            R.anim.enter_from_left
        )
    }
    private val animExitToLeft: Animation by unsafeLazy {
        AnimationUtils.loadAnimation(
            activity,
            R.anim.exit_to_left
        )
    }
    private val animExitToRight: Animation by unsafeLazy {
        AnimationUtils.loadAnimation(
            activity,
            R.anim.exit_to_right
        )
    }
    private val word: String by unsafeLazy {
        getString(
            R.string.backup_word
        )
    }
    private val of: String by unsafeLazy {
        getString(
            R.string.backup_of
        )
    }

    var currentWordIndex = 0

    init {
        Injector.getInstance().presenterComponent.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = container?.inflate(R.layout.fragment_backup_word_list)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onViewReady()

        textview_word_counter.text = getFormattedPositionString()
        textview_current_word.text = presenter.getWordForIndex(currentWordIndex)

        button_next.setOnClickListener { onNextClicked() }
        button_previous.setOnClickListener { onPreviousClicked() }
    }

    override fun getPageBundle(): Bundle? = arguments

    override fun createPresenter() = backupWalletWordListPresenter

    override fun getMvpView() = this

    override fun finish() {
        activity?.finish()
    }

    private fun onNextClicked() {
        if (currentWordIndex >= 0) button_previous.visible() else button_previous.invisible()

        if (currentWordIndex < presenter.getMnemonicSize() - 1) {
            animExitToLeft.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {
                    textview_current_word.text = ""
                    textview_word_counter.text = getFormattedPositionString()
                }

                override fun onAnimationRepeat(animation: Animation) {
                    // No-op
                }

                override fun onAnimationEnd(animation: Animation) {
                    card_layout?.startAnimation(animEnterFromRight)
                    textview_current_word?.text = presenter.getWordForIndex(currentWordIndex)
                }
            })

            card_layout.startAnimation(animExitToLeft)
        }

        currentWordIndex++

        if (currentWordIndex == presenter.getMnemonicSize()) {
            currentWordIndex = 0
            launchVerifyFragment()
        } else {
            if (currentWordIndex == presenter.getMnemonicSize() - 1) {
                button_next.text = getString(R.string.backup_done)
            }
        }
    }

    private fun onPreviousClicked() {
        button_next.text = getString(R.string.backup_next_word)
        if (currentWordIndex == 1) {
            button_previous.invisible()
        }

        animExitToRight.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                textview_current_word.text = ""
                textview_word_counter.text = getFormattedPositionString()
            }

            override fun onAnimationRepeat(animation: Animation) {
                // No-op
            }

            override fun onAnimationEnd(animation: Animation) {
                card_layout?.startAnimation(animEnterFromLeft)
                textview_current_word?.text = presenter.getWordForIndex(currentWordIndex)
            }
        })

        card_layout.startAnimation(animExitToRight)
        currentWordIndex--
    }

    private fun launchVerifyFragment() {
        val fragment = BackupWalletVerifyFragment()
        if (presenter.secondPassword != null) {
            fragment.arguments = Bundle().apply {
                putString(ARGUMENT_SECOND_PASSWORD, presenter.secondPassword)
            }
        }

        fragmentManager?.run {
            beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.content_frame, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun getFormattedPositionString(): CharSequence? = "$word ${currentWordIndex + 1} $of 12"

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.run {
            val view = currentFocus
            if (view != null) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
            }
        }
    }

    companion object {
        const val ARGUMENT_SECOND_PASSWORD = "second_password"
    }
}