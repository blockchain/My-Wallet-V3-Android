package piuk.blockchain.android.ui.auth

import android.content.Context
import io.reactivex.Observable
import piuk.blockchain.android.data.datamanagers.PromptManager
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import javax.inject.Inject

class LandingPresenter @Inject constructor(
    private val environmentSettings: EnvironmentConfig,
    private val promptManager: PromptManager
) : BasePresenter<LandingView>() {

    override fun onViewReady() {
        if (environmentSettings.shouldShowDebugMenu()) {
            with(view) {
                showToast(
                    "Current environment: ${environmentSettings.environment.getName()}",
                    ToastCustom.TYPE_GENERAL
                )
                showDebugMenu()
            }
        }
    }

    internal fun initPreLoginPrompts(context: Context) {
        promptManager.getPreLoginPrompts(context)
            .flatMap { Observable.fromIterable(it) }
            .forEach { view.showWarningPrompt(it) }
    }
}
