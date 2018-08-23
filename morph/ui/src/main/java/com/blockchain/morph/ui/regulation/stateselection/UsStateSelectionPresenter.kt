package com.blockchain.morph.ui.regulation.stateselection

import android.app.Activity
import com.blockchain.morph.regulation.UsStatesDataManager
import com.blockchain.morph.regulation.americanStatesMap
import com.blockchain.morph.ui.R
import io.reactivex.Completable
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import timber.log.Timber

internal class UsStateSelectionPresenter(
    private val usStatesDataManager: UsStatesDataManager
) : BasePresenter<UsStateSelectionView>() {

    override fun onViewReady() {
        // No-op
    }

    internal fun updateAmericanState(state: String) {
        val usState = americanStatesMap[state]
        require(usState != null) { "State not found in map" }

        usStatesDataManager.isStateWhitelisted(usState!!)
            .addToCompositeDisposable(this)
            .flatMapCompletable { whitelisted ->
                if (whitelisted) {
                    usStatesDataManager.setState(usState)
                        .doOnComplete { view.finishActivityWithResult(Activity.RESULT_OK) }
                } else {
                    view.onError(R.string.morph_unavailable_in_state)
                    Completable.complete()
                }
            }
            .subscribe(
                { /* No-op */ },
                {
                    Timber.e(it)
                    view.finishActivityWithResult(Activity.RESULT_CANCELED)
                }
            )
    }
}
