package piuk.blockchain.android.ui.account;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class ItemAccount {

    public String label;
    public String displayBalance;
    public String tag;
    public Long absoluteBalance;

    public Object accountObject;

    public ItemAccount(@NonNull String label,
                       @NonNull String displayBalance,
                       @Nullable String tag,
                       @Nullable Long absoluteBalance,
                       @Nullable Object accountObject) {
        this.label = label;
        this.displayBalance = displayBalance;
        this.tag = tag;
        this.absoluteBalance = absoluteBalance;
        this.accountObject = accountObject;
    }
}
