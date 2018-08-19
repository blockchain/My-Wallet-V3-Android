package piuk.blockchain.android.ui.buysell.payment.bank.addaddress

import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.R
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidbuysell.datamanagers.BuyDataManager
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager
import piuk.blockchain.androidbuysell.models.coinify.Account
import piuk.blockchain.androidbuysell.models.coinify.Address
import piuk.blockchain.androidbuysell.models.coinify.Bank
import piuk.blockchain.androidbuysell.models.coinify.BankAccount
import piuk.blockchain.androidbuysell.models.coinify.Holder
import piuk.blockchain.androidbuysell.models.coinify.exceptions.CoinifyApiException
import piuk.blockchain.androidbuysell.services.ExchangeService
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

class AddAddressPresenter @Inject constructor(
    private val coinifyDataManager: CoinifyDataManager,
    private val exchangeService: ExchangeService,
    private val buyDataManager: BuyDataManager
) : BasePresenter<AddAddressView>() {

    val countryCodeMap by unsafeLazy {
        Locale.getISOCountries().associateBy(
            { Locale(view.locale.displayLanguage, it).displayCountry },
            { it }
        ).toSortedMap()
    }

    private var countryCode: String = "UK"

    private val tokenSingle: Single<String>
        get() = exchangeService.getExchangeMetaData()
            .addToCompositeDisposable(this)
            .applySchedulers()
            .singleOrError()
            .map { it.coinify!!.token }

    override fun onViewReady() {
        buyDataManager.countryCode
            .applySchedulers()
            .addToCompositeDisposable(this)
            .subscribeBy(onNext = { selectCountry(it) })
    }

    internal fun onCountryCodeChanged(code: String) {
        countryCode = code
        selectCountry(countryCode)
    }

    internal fun onConfirmClicked() {
        if (!isDataValid()) return

        tokenSingle.flatMap { token ->
            coinifyDataManager.getTrader(token)
                .flatMap {
                    coinifyDataManager.addBankAccount(
                        token,
                        BankAccount(
                            account = Account(
                                view.displayModel.currencyToReceive,
                                null,
                                view.bic,
                                view.iban
                            ),
                            bank = Bank(address = Address(countryCode = countryCode)),
                            holder = Holder(
                                view.accountHolderName, Address(
                                    street = view.streetAndNumber,
                                    zipcode = view.postCode,
                                    city = view.city,
                                    countryCode = countryCode
                                )
                            )
                        )
                    )
                }
        }.doOnSubscribe { view.showProgressDialog() }
            .doOnEvent { _, _ -> view.dismissProgressDialog() }
            .doOnError { Timber.e(it) }
            .subscribeBy(
                onSuccess = { view.goToConfirmation(it.id!!) },
                onError = {
                    if (it is CoinifyApiException) {
                        view.showErrorDialog(it.getErrorDescription())
                    } else {
                        view.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)
                    }
                }
            )
    }

    private fun isDataValid(): Boolean {
        require(!view.iban.isEmpty()) { }
        require(!view.bic.isEmpty()) { }

        if (view.accountHolderName.isEmpty()) {
            view.showToast(R.string.buy_sell_add_address_name_empty, ToastCustom.TYPE_ERROR)
            return false
        }

        if (view.streetAndNumber.isEmpty()) {
            view.showToast(R.string.buy_sell_add_address_street_empty, ToastCustom.TYPE_ERROR)
            return false
        }

        if (view.city.isEmpty()) {
            view.showToast(R.string.buy_sell_add_address_city_empty, ToastCustom.TYPE_ERROR)
            return false
        }

        if (view.postCode.isEmpty()) {
            view.showToast(R.string.buy_sell_add_address_postcode_empty, ToastCustom.TYPE_ERROR)
            return false
        }

        return true
    }

    private fun selectCountry(countryCode: String) {
        val countryName = countryCodeMap
            .filterValues { it == countryCode }.keys
            .firstOrNull() ?: "UK"

        view.showCountrySelected(countryName)
    }
}