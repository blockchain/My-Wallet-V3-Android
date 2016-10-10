package piuk.blockchain.android.ui.balance;

import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;

import info.blockchain.wallet.payload.Tx;

import java.util.List;

class BalanceDiffUtil extends DiffUtil.Callback {

    private List<Tx> oldTransactions;
    private List<Tx> newTransactions;

    BalanceDiffUtil(List<Tx> oldTransactions, List<Tx> newTransactions) {
        this.oldTransactions = oldTransactions;
        this.newTransactions = newTransactions;
    }

    @Override
    public int getOldListSize() {
        return oldTransactions != null ? oldTransactions.size() : 0;
    }

    @Override
    public int getNewListSize() {
        return newTransactions != null ? newTransactions.size() : 0;
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldTransactions.get(oldItemPosition).getHash().equals(
                newTransactions.get(newItemPosition).getHash());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        Tx oldTransaction = oldTransactions.get(oldItemPosition);
        Tx newTransaction = newTransactions.get(newItemPosition);

        return oldTransaction.getDirection().equals(newTransaction.getDirection())
                && oldTransaction.getAmount() == newTransaction.getAmount()
                && oldTransaction.getConfirmations() == newTransaction.getConfirmations()
                && oldTransaction.getTS() == newTransaction.getTS()
                && oldTransaction.isWatchOnly() == newTransaction.isWatchOnly();
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        return super.getChangePayload(oldItemPosition, newItemPosition);
    }
}
