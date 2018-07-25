package piuk.blockchain.android.util;

import android.content.Context;
import android.support.annotation.PluralsRes;
import android.support.annotation.StringRes;

import javax.inject.Inject;

public class StringUtils {

    private Context context;

    @Inject
    public StringUtils(Context context) {
        this.context = context;
    }

    public String getString(@StringRes int stringId) {
        return context.getString(stringId);
    }

    public String getQuantityString(@PluralsRes int pluralId, int size) {
        return context.getResources().getQuantityString(pluralId, size, size);
    }

    public String getFormattedString(@StringRes int stringId, Object... formatArgs) {
        return context.getResources().getString(stringId, formatArgs);
    }

}
