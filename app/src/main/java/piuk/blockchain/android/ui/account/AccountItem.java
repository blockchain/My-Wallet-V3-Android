package piuk.blockchain.android.ui.account;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

class AccountItem {

    private String label;
    private String address;
    private Drawable icon;
    private String amount;
    private boolean isArchived;
    private boolean isWatchOnly;
    private boolean isDefault;
    private Integer correctedPosition;

    AccountItem(@Nullable Integer correctedPosition, String title, String address, String amount, Drawable icon, boolean isArchived, boolean isWatchOnly, boolean isDefault) {
        this.correctedPosition = correctedPosition;
        this.label = title;
        this.address = address;
        this.amount = amount;
        this.icon = icon;
        this.isArchived = isArchived;
        this.isWatchOnly = isWatchOnly;
        this.isDefault = isDefault;
    }

    @NonNull
    public String getLabel() {
        return label != null ? label : "";
    }

    @NonNull
    public String getAddress() {
        return address != null ? address : "";
    }

    @Nullable
    public Drawable getIcon() {
        return icon;
    }

    @NonNull
    public String getAmount() {
        return amount != null ? amount : "";
    }

    public boolean isArchived() {
        return isArchived;
    }

    public void setIsArchived(boolean isArchived) {
        this.isArchived = isArchived;
    }

    public boolean isWatchOnly() {
        return isWatchOnly;
    }

    public void setIsWatchOnly(boolean isWatchOnly) {
        this.isWatchOnly = isWatchOnly;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    @NonNull
    Integer getCorrectPosition() {
        return correctedPosition != null ? correctedPosition : -1;
    }
}