package piuk.blockchain.android.util;

import android.content.Context;
import android.support.annotation.PluralsRes;
import android.support.annotation.StringRes;

import javax.inject.Inject;

public final class StringUtils {

    private final Context context;

    @Inject
    public StringUtils(Context context) {
        this.context = context;
    }

    public String getString(@StringRes int stringId) {
        return context.getString(stringId);
    }

    /**
     * @deprecated Hides warnings/errors about mismatched number of arguments TODO: Inline, AND-1297
     */
    public String getQuantityString(@PluralsRes int pluralId, int size) {
        return context.getResources().getQuantityString(pluralId, size, size);
    }

    /**
     * @deprecated Hides warnings/errors about mismatched number of arguments TODO: Inline, AND-1297
     */
    public String getFormattedString(@StringRes int stringId, Object... formatArgs) {
        return context.getResources().getString(stringId, formatArgs);
    }

}
