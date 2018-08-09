package piuk.blockchain.androidcore.data.contacts.datastore;

import java.util.List;

import piuk.blockchain.androidcore.data.contacts.models.ContactTransactionModel;
import piuk.blockchain.androidcore.data.datastores.ListStore;

public class PendingTransactionListStore extends ListStore<ContactTransactionModel> {

    public void insertTransaction(ContactTransactionModel transaction) {
        insertObjectIntoList(transaction);
    }

    public void insertTransactions(List<ContactTransactionModel> transactions) {
        insertBulk(transactions);
    }

    public void removeTransaction(ContactTransactionModel object) {
        removeObjectFromList(object);
    }
}

