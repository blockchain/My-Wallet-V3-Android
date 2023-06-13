package piuk.blockchain.android.ui.auth;

import android.support.test.espresso.ViewInteraction;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import piuk.blockchain.android.BaseEspressoTest;
import piuk.blockchain.android.R;
import piuk.blockchain.androidcore.utils.PrefsUtil;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static java.lang.Thread.sleep;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertTrue;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class LandingActivityTest extends BaseEspressoTest {

    private static final ViewInteraction BUTTON_LOGIN = onView(withId(R.id.login));
    private static final ViewInteraction BUTTON_CREATE = onView(withId(R.id.create));
    private static final ViewInteraction BUTTON_RECOVER = onView(withId(R.id.recoverFunds));

    @Before
    public void setUp() throws Exception {
        prefs.setValue("disable_root_warning", true);
    }

    @Rule
    public ActivityTestRule<LandingActivity> activityRule =
            new ActivityTestRule<>(LandingActivity.class);

    @Test
    public void isLaunched() throws Exception {
        assertNotNull(activityRule.getActivity());
    }

    @Test
    public void launchLoginPage() throws InterruptedException {
        BUTTON_LOGIN.perform(click());
        sleep(500);
        // Check pairing fragment launched
        onView(withText(R.string.pair_your_wallet)).check(matches(isDisplayed()));
    }

    @Test
    public void launchCreateWalletPage() throws InterruptedException {
        BUTTON_CREATE.perform(click());
        sleep(500);
        // Check create wallet fragment launched
        onView(withText(R.string.new_wallet)).check(matches(isDisplayed()));
    }

    @Test
    public void launchRecoverFundsPage() throws InterruptedException {
        BUTTON_RECOVER.perform(click());
        sleep(500);
        // Verify warning dialog showing
        onView(withText(R.string.recover_funds_warning_message))
                .inRoot(isDialog())
                .check(matches(isDisplayed()));
        // Click "Continue"
        onView(withId(android.R.id.button1)).perform(click());
        sleep(500);
        // Check recover funds activity launched
        onView(withText(R.string.recover_funds_instructions)).check(matches(isDisplayed()));
    }

}
