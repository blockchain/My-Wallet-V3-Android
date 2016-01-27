package info.blockchain.ui;

import android.test.ActivityInstrumentationTestCase2;
import android.test.FlakyTest;
import android.widget.TextView;

import com.robotium.solo.Solo;

import info.blockchain.ui.util.UiUtil;
import info.blockchain.wallet.MainActivity;
import info.blockchain.wallet.PolicyActivity;
import info.blockchain.wallet.util.ExchangeRateFactory;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.PrefsUtil;

import junit.framework.TestCase;

import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.R;

public class SettingsScreenTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private static boolean loggedIn = false;
    private Solo solo = null;

    public SettingsScreenTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {

        super.setUp();
        solo = new Solo(getInstrumentation(), getActivity());

        if (!loggedIn) {
            UiUtil.getInstance(getActivity()).enterPin(solo, solo.getString(R.string.qa_test_pin1));
            try {
                solo.sleep(4000);
            } catch (Exception e) {
            }
            loggedIn = true;
        }

        UiUtil.getInstance(getActivity()).openNavigationDrawer(solo);
        try {
            solo.sleep(500);
        } catch (Exception e) {
        }
        solo.clickOnText(getActivity().getString(R.string.action_settings));
        try {
            solo.sleep(500);
        } catch (Exception e) {
        }
    }

    @Override
    public void tearDown() throws Exception {
        solo.finishOpenedActivities();
    }

    @FlakyTest(tolerance = 2)
    public void testA_CopyGUI() throws AssertionError {
        solo.clickOnText(getActivity().getString(R.string.options_id));
        solo.waitForText(getActivity().getString(R.string.copied_to_clipboard), 1, 200);
        solo.clickOnText(getActivity().getString(R.string.no));

        solo.clickOnText(getActivity().getString(R.string.options_id));
        solo.waitForText(getActivity().getString(R.string.copied_to_clipboard), 1, 200);
        solo.clickOnText(getActivity().getString(R.string.yes));

        assertTrue(UiUtil.getInstance(getActivity()).getClipboardText().equals(PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_GUID, "")));
    }

    public void testB_SelectBTCUnits() throws AssertionError {

        final CharSequence[] units = MonetaryUtil.getInstance().getBTCUnits();

        for (CharSequence unit : units) {

            PrefsUtil.getInstance(getActivity()).setValue(PrefsUtil.KEY_BTC_UNITS, 0);

            if (solo.searchText(getActivity().getString(R.string.options_units)))
                solo.clickOnText(getActivity().getString(R.string.options_units));

            solo.waitForText(getActivity().getString(R.string.select_units), 1, 500);
            solo.clickOnText(unit.toString());
            try {
                solo.sleep(500);
            } catch (Exception e) {
            }
            int sel = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_BTC_UNITS, 0);
            assertTrue("Found: '" + units[sel] + "' - Expected: '" + unit + "'", units[sel].equals(unit));

            solo.goBack();
            while (!solo.searchText(units[sel].toString())) {
                TextView balance = (TextView) solo.getView(R.id.balance1);
                solo.clickOnView(balance);
            }

            assertTrue(solo.searchText(units[sel].toString()));

            UiUtil.getInstance(getActivity()).openNavigationDrawer(solo);
            try {
                solo.sleep(500);
            } catch (Exception e) {
            }
            solo.clickOnText(getActivity().getString(R.string.action_settings));
            try {
                solo.sleep(500);
            } catch (Exception e) {
            }
        }
    }

    public void testC_SelectCurrency() throws AssertionError {

        final String[] currencies = ExchangeRateFactory.getInstance(getActivity()).getCurrencies();

        for (String currency : currencies) {

            if (solo.searchText(getActivity().getString(R.string.options_currency)))
                solo.clickOnText(getActivity().getString(R.string.options_currency), 1, true);

            solo.waitForText(getActivity().getString(R.string.select_currency), 1, 500, true);

            boolean down = true;
            while (!solo.searchText(currency.toString())) {
                if (down) {
                    solo.scrollDown();
                    down = !down;
                } else

                    solo.scrollUp();
            }

            solo.clickOnText(currency.toString(), 1, true);
            try {
                solo.sleep(500);
            } catch (Exception e) {
            }
            solo.goBack();
            while (!solo.searchText(currency)) {
                TextView balance = (TextView) solo.getView(R.id.balance1);
                solo.clickOnView(balance);
            }

            assertTrue(solo.searchText(currency));

            UiUtil.getInstance(getActivity()).openNavigationDrawer(solo);
            try {
                solo.sleep(500);
            } catch (Exception e) {
            }
            solo.clickOnText(getActivity().getString(R.string.action_settings));
            try {
                solo.sleep(500);
            } catch (Exception e) {
            }
        }
    }

    public void testD_AboutUs() throws AssertionError {
        solo.clickOnText(getActivity().getString(R.string.about_us));
        assertTrue(solo.waitForText(BuildConfig.VERSION_NAME));
        solo.goBack();
    }

    public void testE_Policies() throws AssertionError {
        solo.clickOnText(getActivity().getString(R.string.options_tos));
        TestCase.assertEquals(true, solo.waitForActivity(PolicyActivity.class));
        solo.goBack();
        solo.clickOnText(getActivity().getString(R.string.options_privacy));
        TestCase.assertEquals(true, solo.waitForActivity(PolicyActivity.class));

        UiUtil.getInstance(getActivity()).exitApp(solo);
    }
}
