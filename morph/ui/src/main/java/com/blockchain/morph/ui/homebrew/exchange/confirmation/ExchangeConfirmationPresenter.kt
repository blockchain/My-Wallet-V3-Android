package com.blockchain.morph.ui.homebrew.exchange.confirmation

import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.androidcoreui.ui.base.BasePresenter

class ExchangeConfirmationPresenter(
    // TODO:   
) : BasePresenter<ExchangeConfirmationView>() {

    override fun onViewReady() {
        compositeDisposable +=
            view.clickEvents
                .subscribeBy(
                    onNext = {
                        // TODO: This is obviously incomplete
                        view.continueToExchangeLocked()
                    },
                    onError = {
                    }
                )
    }
}