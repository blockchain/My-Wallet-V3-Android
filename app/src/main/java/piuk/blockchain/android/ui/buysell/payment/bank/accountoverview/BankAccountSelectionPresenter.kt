package piuk.blockchain.android.ui.buysell.payment.bank.accountoverview

import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.ui.buysell.payment.bank.accountoverview.models.AddAccountButton
import piuk.blockchain.android.ui.buysell.payment.bank.accountoverview.models.BankAccountDisplayable
import piuk.blockchain.android.ui.buysell.payment.bank.accountoverview.models.BankAccountListObject
import piuk.blockchain.android.ui.buysell.payment.bank.accountoverview.models.BankAccountState
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager
import piuk.blockchain.androidbuysell.services.ExchangeService
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import timber.log.Timber
import javax.inject.Inject

class BankAccountSelectionPresenter @Inject constructor(
    private val exchangeService: ExchangeService,
    private val coinifyDataManager: CoinifyDataManager
) : BasePresenter<BankAccountSelectionView>() {

    private val tokenSingle: Single<String>
        get() = exchangeService.getExchangeMetaData()
            .addToCompositeDisposable(this)
            .applySchedulers()
            .singleOrError()
            .map { it.coinify!!.token }

    override fun onViewReady() {
        tokenSingle
            .addToCompositeDisposable(this)
            .applySchedulers()
            .flatMapObservable { fetchAccountsObservable(it) }
            .doOnError { Timber.e(it) }
            .subscribeBy(onNext = { view.renderUiState(it) })
    }

    internal fun deleteBankAccount(bankAccountId: Int) {
        tokenSingle
            .addToCompositeDisposable(this)
            .applySchedulers()
            .flatMapCompletable { coinifyDataManager.deleteBankAccount(it, bankAccountId) }
            .andThen(tokenSingle)
            .flatMapObservable { fetchAccountsObservable(it) }
            .doOnError { Timber.e(it) }
            .startWith(BankAccountState.Loading)
            .onErrorReturn { BankAccountState.DeleteAccountFailure }
            .subscribeBy(onNext = { view.renderUiState(it) })
    }

    private fun fetchAccountsObservable(token: String): Observable<BankAccountState> =
        coinifyDataManager.getBankAccounts(token)
            .flattenAsObservable { it }
            .map<BankAccountDisplayable> {
                BankAccountListObject(it.id!!, formatStringWithSpaces(it.account.number))
            }
            .toList()
            .toObservable()
            .map<BankAccountState> {
                it.add(AddAccountButton())
                return@map BankAccountState.Data(it)
            }
            .startWith(BankAccountState.Loading)
            .onErrorReturn { BankAccountState.Failure }

    private fun formatStringWithSpaces(original: String): String {
        val dashInterval = 4
        return if (dashInterval < original.length) {
            var formatted = original.substring(0, dashInterval)
            var i = dashInterval
            while (i < original.length && i + dashInterval < original.length) {
                formatted += " " + original.substring(i, i + dashInterval)
                i += dashInterval
            }
            formatted
        } else original
    }
}
