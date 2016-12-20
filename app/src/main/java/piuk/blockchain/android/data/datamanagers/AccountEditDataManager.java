package piuk.blockchain.android.data.datamanagers;

import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payment.Payment;
import info.blockchain.wallet.payment.data.SpendableUnspentOutputs;
import info.blockchain.wallet.payment.data.SweepBundle;
import info.blockchain.wallet.payment.data.UnspentOutputs;

import org.bitcoinj.core.ECKey;

import java.math.BigInteger;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import piuk.blockchain.android.data.cache.DynamicFeeCache;
import piuk.blockchain.android.data.rxjava.IgnorableDefaultObserver;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.services.PaymentService;
import piuk.blockchain.android.data.services.UnspentService;
import piuk.blockchain.android.ui.account.ItemAccount;
import piuk.blockchain.android.ui.send.PendingTransaction;

public class AccountEditDataManager {

    private UnspentService unspentService;
    private PaymentService paymentService;
    private PayloadManager payloadManager;

    public AccountEditDataManager(UnspentService unspentService, PaymentService paymentService, PayloadManager payloadManager) {
        this.unspentService = unspentService;
        this.paymentService = paymentService;
        this.payloadManager = payloadManager;
    }

    /**
     * Generates a {@link PendingTransaction} object for a given legacy address, where the output is
     * the default account in the user's wallet
     *
     * @param legacyAddress The {@link LegacyAddress} you wish to transfer funds from
     * @param payment       A new {@link Payment} object
     * @return An {@link Observable<PendingTransaction>}
     */
    public Observable<PendingTransaction> getPendingTransactionForLegacyAddress(LegacyAddress legacyAddress,
                                                                                Payment payment) {

        PendingTransaction pendingTransaction = new PendingTransaction();

        return getUnspentOutputs(legacyAddress, payment)
                .flatMap(unspentOutputs -> {
                    BigInteger suggestedFeePerKb = DynamicFeeCache.getInstance().getSuggestedFee().defaultFeePerKb;
                    SweepBundle sweepBundle = payment.getSweepBundle(unspentOutputs, suggestedFeePerKb);

                    // To default account
                    int defaultIndex = payloadManager.getPayload().getHdWallet().getDefaultIndex();
                    Account defaultAccount = payloadManager.getPayload().getHdWallet().getAccounts().get(defaultIndex);
                    pendingTransaction.sendingObject = new ItemAccount(legacyAddress.getLabel(), sweepBundle.getSweepAmount().toString(), "", sweepBundle.getSweepAmount().longValue(), legacyAddress);
                    pendingTransaction.receivingObject = new ItemAccount(defaultAccount.getLabel(), "", "", sweepBundle.getSweepAmount().longValue(), defaultAccount);
                    pendingTransaction.unspentOutputBundle = payment.getSpendableCoins(unspentOutputs, sweepBundle.getSweepAmount(), suggestedFeePerKb);
                    pendingTransaction.bigIntAmount = sweepBundle.getSweepAmount();
                    pendingTransaction.bigIntFee = pendingTransaction.unspentOutputBundle.getAbsoluteFee();

                    return getNextReceiveAddress(defaultIndex);
                })
                .map(receivingAddress -> {
                    pendingTransaction.receivingAddress = receivingAddress;
                    return pendingTransaction;
                })
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Submits a payment to a specified address and returns the transaction hash if successful
     *
     * @param unspentOutputBundle UXTO object
     * @param keys                A List of elliptic curve keys
     * @param toAddress           The address to send the funds to
     * @param changeAddress       A change address
     * @param bigIntFee           The specified fee amount
     * @param bigIntAmount        The actual transaction amount
     * @return An {@link Observable<String>} where the String is the transaction hash
     */
    public Observable<String> submitPayment(SpendableUnspentOutputs unspentOutputBundle,
                                            List<ECKey> keys,
                                            String toAddress,
                                            String changeAddress,
                                            BigInteger bigIntFee,
                                            BigInteger bigIntAmount) {

        return paymentService.submitPayment(
                unspentOutputBundle,
                keys,
                toAddress,
                changeAddress,
                bigIntFee,
                bigIntAmount)
                .compose(RxUtil.applySchedulersToObservable());
    }

    // TODO: 21/10/2016 Move all PayloadManager methods out of here
    public Observable<Boolean> syncPayloadWithServer() {
        return Observable.fromCallable(() -> payloadManager.savePayloadToServer())
                .compose(RxUtil.applySchedulersToObservable());
    }

    private Observable<String> getNextReceiveAddress(int defaultIndex) {
        return Observable.fromCallable(() -> payloadManager.getNextReceiveAddress(defaultIndex));
    }

    /**
     * Returns {@link Completable} which updates balances and transactions in the PayloadManager.
     * Completable returns no value, and is used to call functions that return void but have side
     * effects.
     *
     * @return {@link Completable}
     * @see {@link IgnorableDefaultObserver}
     */
    public Completable updateBalancesAndTransactions() {
        return Completable.fromCallable(() -> {
            payloadManager.updateBalancesAndTransactions();
            return Void.TYPE;
        }).subscribeOn(Schedulers.io());
    }

    private Observable<UnspentOutputs> getUnspentOutputs(LegacyAddress legacyAddress, Payment payment) {
        return unspentService.getUnspentOutputs(legacyAddress.getAddress(), payment);
    }
}
