package piuk.blockchain.android.data.datamanagers;

import android.support.annotation.Nullable;

import info.blockchain.api.data.UnspentOutputs;
import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.LegacyAddress;
import info.blockchain.wallet.payment.Payment;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.bitcoinj.core.ECKey;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import piuk.blockchain.android.data.cache.DynamicFeeCache;
import piuk.blockchain.androidcore.data.payload.PayloadDataManager;
import piuk.blockchain.androidcore.data.payments.SendDataManager;
import piuk.blockchain.androidcore.utils.rxjava.IgnorableDefaultObserver;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.androidcore.injection.PresenterScope;
import piuk.blockchain.android.ui.account.ItemAccount;
import piuk.blockchain.android.ui.send.PendingTransaction;

@PresenterScope
public class TransferFundsDataManager {

    private PayloadDataManager payloadDataManager;
    private SendDataManager sendDataManager;
    private DynamicFeeCache dynamicFeeCache;

    @Inject
    public TransferFundsDataManager(PayloadDataManager payloadDataManager,
                                    SendDataManager sendDataManager,
                                    DynamicFeeCache dynamicFeeCache) {
        this.payloadDataManager = payloadDataManager;
        this.sendDataManager = sendDataManager;
        this.dynamicFeeCache = dynamicFeeCache;
    }

    /**
     * Check if there are any spendable legacy funds that need to be sent to a HD wallet. Constructs
     * a list of {@link PendingTransaction} objects with outputs set to an account defined by it's
     * index in the list of HD accounts.
     *
     * @param addressToReceiveIndex The index of the account to which you want to send the funds
     * @return Returns a Map which bundles together the List of {@link PendingTransaction} objects,
     * as well as a Pair which contains the total to send and the total fees, in that order.
     */
    public Observable<Triple<List<PendingTransaction>, Long, Long>> getTransferableFundTransactionList(int addressToReceiveIndex) {
        return Observable.fromCallable(() -> {

            BigInteger suggestedFeePerKb =
                    BigInteger.valueOf(dynamicFeeCache.getBtcFeeOptions().getRegularFee() * 1000);

            List<PendingTransaction> pendingTransactionList = new ArrayList<>();
            List<LegacyAddress> legacyAddresses = payloadDataManager.getWallet().getLegacyAddressList();

            long totalToSend = 0L;
            long totalFee = 0L;

            for (LegacyAddress legacyAddress : legacyAddresses) {

                if (!legacyAddress.isWatchOnly()
                        && payloadDataManager.getAddressBalance(legacyAddress.getAddress())
                        .compareTo(BigInteger.ZERO) == 1) {

                    UnspentOutputs unspentOutputs =
                            sendDataManager.getUnspentOutputs(legacyAddress.getAddress())
                                    .blockingFirst();
                    Pair<BigInteger, BigInteger> sweepableCoins =
                            sendDataManager.getMaximumAvailable(unspentOutputs, suggestedFeePerKb);
                    BigInteger sweepAmount = sweepableCoins.getLeft();

                    // Don't sweep if there are still unconfirmed funds in address
                    if (unspentOutputs.getNotice() == null && sweepAmount.compareTo(Payment.DUST) == 1) {

                        PendingTransaction pendingSpend = new PendingTransaction();
                        pendingSpend.unspentOutputBundle = sendDataManager
                                .getSpendableCoins(unspentOutputs, sweepAmount, suggestedFeePerKb);
                        pendingSpend.sendingObject = new ItemAccount(
                                legacyAddress.getLabel(),
                                "",
                                "",
                                null,
                                legacyAddress,
                                legacyAddress.getAddress());
                        pendingSpend.bigIntFee = pendingSpend.unspentOutputBundle.getAbsoluteFee();
                        pendingSpend.bigIntAmount = sweepAmount;
                        pendingSpend.addressToReceiveIndex = addressToReceiveIndex;
                        totalToSend += pendingSpend.bigIntAmount.longValue();
                        totalFee += pendingSpend.bigIntFee.longValue();
                        pendingTransactionList.add(pendingSpend);
                    }
                }
            }

            return Triple.of(pendingTransactionList, totalToSend, totalFee);
        }).compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Check if there are any spendable legacy funds that need to be sent to a HD wallet. Constructs
     * a list of {@link PendingTransaction} objects with outputs set to the default HD account.
     *
     * @return Returns a Triple object which bundles together the List of {@link PendingTransaction}
     * objects, as well as the total to send and the total fees, in that order.
     */
    public Observable<Triple<List<PendingTransaction>, Long, Long>> getTransferableFundTransactionListForDefaultAccount() {
        return getTransferableFundTransactionList(payloadDataManager.getDefaultAccountIndex());
    }

    /**
     * Takes a list of {@link PendingTransaction} objects and transfers them all. Emits a String
     * which is the Tx hash for each successful payment, and calls onCompleted when all
     * PendingTransactions have been finished successfully.
     *
     * @param pendingTransactions A list of {@link PendingTransaction} objects
     * @param secondPassword      The double encryption password if necessary
     * @return An {@link Observable<String>}
     */
    public Observable<String> sendPayment(List<PendingTransaction> pendingTransactions,
                                          @Nullable String secondPassword) {
        return getPaymentObservable(pendingTransactions, secondPassword)
                .compose(RxUtil.applySchedulersToObservable());
    }

    private Observable<String> getPaymentObservable(List<PendingTransaction> pendingTransactions, String secondPassword) {
        return Observable.create(subscriber -> {
            for (int i = 0; i < pendingTransactions.size(); i++) {
                PendingTransaction pendingTransaction = pendingTransactions.get(i);

                final int finalI = i;
                LegacyAddress legacyAddress = ((LegacyAddress) pendingTransaction.sendingObject.getAccountObject());
                String changeAddress = legacyAddress.getAddress();
                String receivingAddress =
                        payloadDataManager.getNextReceiveAddress(pendingTransaction.addressToReceiveIndex)
                                .blockingFirst();

                List<ECKey> keys = new ArrayList<>();
                keys.add(payloadDataManager.getAddressECKey(legacyAddress, secondPassword));

                sendDataManager.submitBtcPayment(
                        pendingTransaction.unspentOutputBundle,
                        keys,
                        receivingAddress,
                        changeAddress,
                        pendingTransaction.bigIntFee,
                        pendingTransaction.bigIntAmount)
                        .blockingSubscribe(s -> {
                            if (!subscriber.isDisposed()) {
                                subscriber.onNext(s);
                            }
                            // Increment index on receive chain
                            Account account = payloadDataManager.getWallet()
                                    .getHdWallets()
                                    .get(0)
                                    .getAccounts()
                                    .get(pendingTransaction.addressToReceiveIndex);
                            payloadDataManager.incrementReceiveAddress(account);

                            // Update Balances temporarily rather than wait for sync
                            long spentAmount = (pendingTransaction.bigIntAmount.longValue() + pendingTransaction.bigIntFee.longValue());
                            payloadDataManager.subtractAmountFromAddressBalance(legacyAddress.getAddress(), spentAmount);

                            if (finalI == pendingTransactions.size() - 1) {
                                // Sync once transactions are completed
                                payloadDataManager.syncPayloadWithServer()
                                        .subscribe(new IgnorableDefaultObserver<>());

                                if (!subscriber.isDisposed()) {
                                    subscriber.onComplete();
                                }
                            }

                        }, throwable -> {
                            if (!subscriber.isDisposed()) {
                                subscriber.onError(new Throwable(throwable));
                            }
                        });
            }
        });
    }

}
