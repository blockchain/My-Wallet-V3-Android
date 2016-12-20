package piuk.blockchain.android.data.services;

import info.blockchain.api.TransactionDetails;
import info.blockchain.wallet.transaction.Transaction;

import piuk.blockchain.android.data.rxjava.RxUtil;
import io.reactivex.Observable;

public class TransactionDetailsService {

    private TransactionDetails transactionDetails;

    public TransactionDetailsService(TransactionDetails transactionDetails) {
        this.transactionDetails = transactionDetails;
    }

    /**
     * Get a specific {@link Transaction} from a hash
     *
     * @param hash The hash of the transaction to be returned
     * @return A Transaction object
     */
    public Observable<Transaction> getTransactionDetailsFromHash(String hash) {
        return Observable.fromCallable(() -> transactionDetails.getTransactionDetails(hash))
                .compose(RxUtil.applySchedulersToObservable());
    }
}
