package piuk.blockchain.android.ui.buysell.coinify.signup.selectcountry

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_coinify_select_country.*
import piuk.blockchain.android.R
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.buysell.coinify.signup.CoinifyFlowListener
import piuk.blockchain.androidcoreui.ui.base.BaseFragment
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class CoinifySelectCountryFragment :
    BaseFragment<CoinifySelectCountryView, CoinifySelectCountryPresenter>(),
    CoinifySelectCountryView {

    @Inject
    lateinit var presenter: CoinifySelectCountryPresenter
    private var signUpListener: CoinifyFlowListener? = null

    init {
        Injector.INSTANCE.presenterComponent.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_coinify_select_country)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        RxView.clicks(buyandsellChooseCountryContinueButton)
            .throttleFirst(1000, TimeUnit.MILLISECONDS)
            .subscribeBy(onNext = { presenter.collectDataAndContinue(countryPicker.currentItemPosition) })

        onViewReady()
    }

    override fun onStartVerifyEmail(countryCode: String) {
        signUpListener?.requestStartVerifyEmail(countryCode)
    }

    override fun createPresenter() = presenter

    override fun getMvpView() = this

    override fun onSetCountryPickerData(countryNameList: List<String>) {
        countryPicker.data = countryNameList
    }

    override fun onAutoSelectCountry(position: Int) {
        countryPicker.selectedItemPosition = position
    }

    override fun onStartInvalidCountry() {
        signUpListener?.requestStartInvalidCountry()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is CoinifyFlowListener) {
            signUpListener = context
        } else {
            throw RuntimeException("$context must implement CoinifyFlowListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        signUpListener = null
    }

    companion object {

        fun newInstance(): CoinifySelectCountryFragment = CoinifySelectCountryFragment()
    }
}