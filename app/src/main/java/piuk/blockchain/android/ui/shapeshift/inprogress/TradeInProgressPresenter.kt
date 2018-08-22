package piuk.blockchain.android.ui.shapeshift.inprogress

import info.blockchain.wallet.shapeshift.data.Trade
import io.reactivex.Observable
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.shapeshift.models.TradeProgressUiState
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidcore.data.shapeshift.ShapeShiftDataManager
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class TradeInProgressPresenter @Inject constructor(
    private val shapeShiftDataManager: ShapeShiftDataManager
) : BasePresenter<TradeInProgressView>() {

    override fun onViewReady() {
        // Set initial state
        onNoDeposit()

        // Poll for results
        Observable.interval(10, TimeUnit.SECONDS)
            .flatMap { shapeShiftDataManager.getTradeStatus(view.depositAddress) }
            .doOnNext { handleState(it.status) }
            .takeUntil { isInFinalState(it.status) }
            .applySchedulers()
            .addToCompositeDisposable(this)
            .subscribe(
                {
                    // Doesn't particularly matter if completion is interrupted here
                    with(it) {
                        updateMetadata(address, transaction, status)
                    }
                },
                { Timber.e(it) }
            )
    }

    private fun updateMetadata(
        address: String,
        hashOut: String?,
        status: Trade.STATUS
    ) {

        shapeShiftDataManager.findTrade(address)
            .map {
                it.apply {
                    this.status = status
                    this.hashOut = hashOut
                }
            }
            .flatMapCompletable { shapeShiftDataManager.updateTrade(it) }
            .addToCompositeDisposable(this)
            .subscribe(
                { Timber.d("Update metadata entry complete") },
                { Timber.e(it) }
            )
    }

    private fun handleState(status: Trade.STATUS) {
        when (status) {
            Trade.STATUS.NO_DEPOSITS -> onNoDeposit()
            Trade.STATUS.RECEIVED -> onReceived()
            Trade.STATUS.COMPLETE -> onComplete()
            Trade.STATUS.FAILED, Trade.STATUS.RESOLVED -> onFailed()
        }
    }

    private fun onNoDeposit() {
        val state = TradeProgressUiState(
            R.string.morph_sending_title,
            R.string.morph_in_progress_explanation,
            R.drawable.shapeshift_progress_airplane,
            true,
            1
        )
        view.updateUi(state)
    }

    private fun onReceived() {
        val state = TradeProgressUiState(
            R.string.morph_in_progress_title,
            R.string.morph_in_progress_explanation,
            R.drawable.shapeshift_progress_exchange,
            true,
            2
        )
        view.updateUi(state)
    }

    private fun onComplete() {
        val state = TradeProgressUiState(
            R.string.morph_complete_title,
            R.string.morph_in_progress_explanation,
            R.drawable.shapeshift_progress_complete,
            true,
            3
        )
        view.updateUi(state)
    }

    private fun onFailed() {
        val state = TradeProgressUiState(
            R.string.morph_failed_title,
            R.string.morph_failed_explanation,
            R.drawable.shapeshift_progress_failed,
            false,
            0
        )
        view.updateUi(state)
    }

    private fun isInFinalState(status: Trade.STATUS) = when (status) {
        Trade.STATUS.NO_DEPOSITS, Trade.STATUS.RECEIVED -> false
        Trade.STATUS.COMPLETE, Trade.STATUS.FAILED, Trade.STATUS.RESOLVED -> true
    }
}