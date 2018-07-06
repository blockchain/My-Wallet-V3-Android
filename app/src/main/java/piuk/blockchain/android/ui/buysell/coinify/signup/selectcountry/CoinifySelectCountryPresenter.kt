package piuk.blockchain.android.ui.buysell.coinify.signup.selectcountry

import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidbuysell.datamanagers.BuyDataManager
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import java.util.Locale
import javax.inject.Inject

class CoinifySelectCountryPresenter @Inject constructor(
    private val buyDataManager: BuyDataManager
) : BasePresenter<CoinifySelectCountryView>() {

    private val countryCodeMap by unsafeLazy {
        Locale.getISOCountries().associateBy(
            { Locale("en", it).displayCountry },
            { it }
        ).toSortedMap()
    }

    override fun onViewReady() {
        setCountryCodeMap()

        buyDataManager.countryCode
            .applySchedulers()
            .addToCompositeDisposable(this)
            .subscribeBy(onNext = { autoSelectCountry(it) })
    }

    private fun setCountryCodeMap() {
        view.onSetCountryPickerData(countryCodeMap.keys.toList())
    }

    private fun autoSelectCountry(countryCode: String) {
        val countryName = countryCodeMap
            .filterValues { it == countryCode }.keys
            .firstOrNull() ?: ""

        if (countryName.isNotEmpty()) {
            view.onAutoSelectCountry(countryCodeMap.keys.indexOf(countryName))
        }
    }

    fun collectDataAndContinue(countryPosition: Int) {
        val countryName =
            countryCodeMap.keys.filterIndexed { index, _ -> index == countryPosition }.last()
        val countryCode = countryCodeMap[countryName]!!

        buyDataManager.isInCoinifyCountry(countryCode)
            .applySchedulers()
            .addToCompositeDisposable(this)
            .subscribeBy(
                onNext = { isAllowed ->
                    if (isAllowed) {
                        view.onStartVerifyEmail(countryCode)
                    } else {
                        view.onStartInvalidCountry()
                    }
                }
            )
    }
}