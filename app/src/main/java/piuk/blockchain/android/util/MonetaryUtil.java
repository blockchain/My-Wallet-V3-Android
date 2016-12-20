package piuk.blockchain.android.util;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

public class MonetaryUtil {

    public static final int UNIT_BTC = 0;
    public static final int MILLI_BTC = 1;
    public static final int MICRO_BTC = 2;
    private static final double MILLI_DOUBLE = 1000.0;
    private static final double MICRO_DOUBLE = 1000000.0;
    private static final long MILLI_LONG = 1000L;
    private static final long MICRO_LONG = 1000000L;
    private static final double BTC_DEC = 1e8;
    private CharSequence[] btcUnits = {"BTC", "mBTC", "bits"};
    private DecimalFormat btcFormat = null;
    private DecimalFormat fiatFormat = null;

    private int unit;

    public MonetaryUtil(int unit) {
        this.unit = unit;

        fiatFormat = (DecimalFormat) NumberFormat.getInstance(Locale.getDefault());
        fiatFormat.setMaximumFractionDigits(2);
        fiatFormat.setMinimumFractionDigits(2);
        DecimalFormatSymbols symbolsF = fiatFormat.getDecimalFormatSymbols();
        symbolsF.setGroupingSeparator(' ');
        fiatFormat.setDecimalFormatSymbols(symbolsF);

        btcFormat = (DecimalFormat) NumberFormat.getInstance(Locale.getDefault());
        btcFormat.setMaximumFractionDigits(8);
        btcFormat.setMinimumFractionDigits(1);
        DecimalFormatSymbols symbolsB = btcFormat.getDecimalFormatSymbols();
        symbolsB.setGroupingSeparator(' ');
        btcFormat.setDecimalFormatSymbols(symbolsB);
    }

    public void updateUnit(int unit) {
        this.unit = unit;
    }

    public NumberFormat getBTCFormat() {
        return btcFormat;
    }

    public NumberFormat getFiatFormat(String fiat) {
        fiatFormat.setCurrency(Currency.getInstance(fiat));
        return fiatFormat;
    }

    public CharSequence[] getBTCUnits() {
        return btcUnits.clone();
    }

    public String getBTCUnit(int unit) {
        return (String) btcUnits[unit];
    }

    public String getDisplayAmount(long value) {

        String strAmount;

        switch (unit) {
            case MonetaryUtil.MICRO_BTC:
                strAmount = Double.toString((((double) (value * MICRO_LONG)) / BTC_DEC));
                break;
            case MonetaryUtil.MILLI_BTC:
                strAmount = Double.toString((((double) (value * MILLI_LONG)) / BTC_DEC));
                break;
            default:
                strAmount = getBTCFormat().format(value / BTC_DEC);
                break;
        }

        return strAmount;
    }

    public BigInteger getUndenominatedAmount(long value) {

        BigInteger amount;

        switch (unit) {
            case MonetaryUtil.MICRO_BTC:
                amount = BigInteger.valueOf(value / MICRO_LONG);
                break;
            case MonetaryUtil.MILLI_BTC:
                amount = BigInteger.valueOf(value / MILLI_LONG);
                break;
            default:
                amount = BigInteger.valueOf(value);
                break;
        }

        return amount;
    }

    public double getUndenominatedAmount(double value) {

        double amount;

        switch (unit) {
            case MonetaryUtil.MICRO_BTC:
                amount = value / MICRO_DOUBLE;
                break;
            case MonetaryUtil.MILLI_BTC:
                amount = value / MILLI_DOUBLE;
                break;
            default:
                amount = value;
                break;
        }

        return amount;
    }

    public double getDenominatedAmount(double value) {

        double amount;

        switch (unit) {
            case MonetaryUtil.MICRO_BTC:
                amount = value * MICRO_DOUBLE;
                break;
            case MonetaryUtil.MILLI_BTC:
                amount = value * MILLI_DOUBLE;
                break;
            default:
                amount = value;
                break;
        }

        return amount;
    }

    public String getDisplayAmountWithFormatting(long value) {

        String strAmount;
        DecimalFormat df = new DecimalFormat("#");
        df.setMinimumIntegerDigits(1);
        df.setMinimumFractionDigits(1);
        df.setMaximumFractionDigits(8);

        switch (unit) {
            case MonetaryUtil.MICRO_BTC:
                strAmount = df.format(((double) (value * MICRO_LONG)) / BTC_DEC);
                break;
            case MonetaryUtil.MILLI_BTC:
                strAmount = df.format(((double) (value * MILLI_LONG)) / BTC_DEC);
                break;
            default:
                strAmount = getBTCFormat().format(value / BTC_DEC);
                break;
        }

        return strAmount;
    }

    public String getDisplayAmountWithFormatting(double value) {

        String strAmount;
        DecimalFormat df = new DecimalFormat("#");
        df.setMinimumIntegerDigits(1);
        df.setMinimumFractionDigits(1);
        df.setMaximumFractionDigits(8);

        switch (unit) {
            case MonetaryUtil.MICRO_BTC:
                strAmount = df.format((value * MICRO_DOUBLE) / BTC_DEC);
                break;
            case MonetaryUtil.MILLI_BTC:
                strAmount = df.format((value * MILLI_DOUBLE) / BTC_DEC);
                break;
            default:
                strAmount = getBTCFormat().format(value / BTC_DEC);
                break;
        }

        return strAmount;
    }

}
