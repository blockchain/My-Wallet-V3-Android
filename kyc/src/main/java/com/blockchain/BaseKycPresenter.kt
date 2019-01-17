package com.blockchain

import com.blockchain.nabu.NabuToken
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.base.View

abstract class BaseKycPresenter<T : View>(
    private val nabuToken: NabuToken
) : BasePresenter<T>() {

    protected val fetchOfflineToken by unsafeLazy { nabuToken.fetchNabuToken() }
}
