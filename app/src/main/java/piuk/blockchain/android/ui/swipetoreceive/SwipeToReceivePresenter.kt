package piuk.blockchain.android.ui.swipetoreceive

import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.data.datamanagers.QrCodeDataManager
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.base.UiState
import javax.inject.Inject
import kotlin.properties.Delegates

class SwipeToReceivePresenter @Inject constructor(
    private val dataManager: QrCodeDataManager,
    private val swipeToReceiveHelper: SwipeToReceiveHelper,
    private val stringUtils: StringUtils
) : BasePresenter<SwipeToReceiveView>() {

    internal var currencyPosition by Delegates.observable(0) { _, _, new ->
        check(new in 0 until currencyList.size) { "Invalid currency position" }
        onCurrencySelected(currencyList[new])
    }

    private val currencyList = listOf(
        CryptoCurrency.BTC,
        CryptoCurrency.ETHER,
        CryptoCurrency.BCH
    )

    private val bitcoinAddress: Single<String>
        get() = swipeToReceiveHelper.getNextAvailableBitcoinAddressSingle()
    private val ethereumAddress: Single<String>
        get() = swipeToReceiveHelper.getEthReceiveAddressSingle()
    private val bitcoinCashAddress: Single<String>
        get() = swipeToReceiveHelper.getNextAvailableBitcoinCashAddressSingle()

    override fun onViewReady() {
        currencyPosition = 0
    }

    private fun onCurrencySelected(cryptoCurrency: CryptoCurrency) {
        view.displayCoinType(
            stringUtils.getFormattedString(
                R.string.swipe_receive_request,
                cryptoCurrency.unit
            )
        )
        view.setUiState(UiState.LOADING)

        val accountName: String
        val single: Single<String>
        val hasAddresses: Boolean

        when (cryptoCurrency) {
            CryptoCurrency.BTC -> {
                accountName = swipeToReceiveHelper.getBitcoinAccountName()
                single = bitcoinAddress.map { "bitcoin:$it" }
                hasAddresses = !swipeToReceiveHelper.getBitcoinReceiveAddresses().isEmpty()
            }
            CryptoCurrency.ETHER -> {
                accountName = swipeToReceiveHelper.getEthAccountName()
                single = ethereumAddress
                hasAddresses = !swipeToReceiveHelper.getEthReceiveAddress().isNullOrEmpty()
            }
            else -> {
                accountName = swipeToReceiveHelper.getBitcoinCashAccountName()
                single = bitcoinCashAddress
                hasAddresses = !swipeToReceiveHelper.getBitcoinCashReceiveAddresses().isEmpty()
            }
        }

        view.displayReceiveAccount(accountName)

        // Check we actually have addresses stored
        if (!hasAddresses) {
            view.setUiState(UiState.EMPTY)
        } else {
            single.doOnSuccess { require(it.isNotEmpty()) { "Returned address is empty, no more addresses available" } }
                .doOnSuccess {
                    view.displayReceiveAddress(
                        it.replace("bitcoincash:", "")
                            .replace("bitcoin:", "")
                    )
                }
                .flatMapObservable { dataManager.generateQrCode(it, DIMENSION_QR_CODE) }
                .addToCompositeDisposable(this)
                .subscribe(
                    {
                        view.displayQrCode(it)
                        view.setUiState(UiState.CONTENT)
                    },
                    { _ -> view.setUiState(UiState.FAILURE) })
        }
    }

    companion object {

        private const val DIMENSION_QR_CODE = 600
    }
}
