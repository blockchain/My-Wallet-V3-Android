package piuk.blockchain.android.ui.buysell.coinify.signup.invalidcountry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_coinify_invalid_country.*
import piuk.blockchain.android.R
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.androidcoreui.ui.base.BaseFragment
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import javax.inject.Inject

class CoinifyInvalidCountryFragment : BaseFragment<CoinifyInvalidCountryView,
    CoinifyInvalidCountryPresenter>(), CoinifyInvalidCountryView {

    @Inject
    lateinit var presenter: CoinifyInvalidCountryPresenter

    init {
        Injector.INSTANCE.presenterComponent.inject(this)
    }

    override fun createPresenter() = presenter

    override fun getMvpView() = this

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_coinify_invalid_country)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buysellSubmitButton.setOnClickListener {
            presenter.requestEmailOnBuySellAvailability()
        }

        onViewReady()
    }

    override fun close() {
        activity?.finish()
    }

    companion object {

        fun newInstance(): CoinifyInvalidCountryFragment = CoinifyInvalidCountryFragment()
    }
}