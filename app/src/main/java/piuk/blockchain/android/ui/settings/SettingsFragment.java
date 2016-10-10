package piuk.blockchain.android.ui.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.UiThread;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mukesh.countrypicker.fragments.CountryPicker;
import com.mukesh.countrypicker.models.Country;

import info.blockchain.api.Settings;
import info.blockchain.wallet.payload.Payload;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.FormatsUtil;
import info.blockchain.wallet.util.PasswordUtil;

import java.util.Timer;
import java.util.TimerTask;

import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.ui.auth.PinEntryActivity;
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.balance.BalanceFragment;
import piuk.blockchain.android.util.ExchangeRateFactory;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.RootUtil;
import piuk.blockchain.android.util.ViewUtils;
import piuk.blockchain.android.util.annotations.Thunk;

import static android.app.Activity.RESULT_OK;
import static piuk.blockchain.android.ui.auth.PinEntryActivity.KEY_VALIDATED_PIN;
import static piuk.blockchain.android.ui.auth.PinEntryActivity.KEY_VALIDATING_PIN_FOR_RESULT;
import static piuk.blockchain.android.ui.auth.PinEntryActivity.REQUEST_CODE_VALIDATE_PIN;

public class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener {

    public static final String EXTRA_SHOW_TWO_FA_DIALOG = "show_two_fa_dialog";
    public static final String URL_TOS_POLICY = "https://blockchain.com/terms";
    public static final String URL_PRIVACY_POLICY = "https://blockchain.com/privacy";

    //Profile
    private Preference guidPref;
    private Preference emailPref;
    private Preference smsPref;

    //Preferences
    private Preference unitsPref;
    private Preference fiatPref;
    private SwitchPreferenceCompat emailNotificationPref;
    private SwitchPreferenceCompat smsNotificationPref;

    //Security
    private Preference pinPref;
    private SwitchPreferenceCompat twoStepVerificationPref;
    private Preference passwordHint1Pref;
    private Preference changePasswordPref;
    private SwitchPreferenceCompat torPref;

    //App
    private Preference aboutPref;
    private Preference tosPref;
    private Preference privacyPref;
    private Preference disableRootWarningPref;

    @Thunk Settings settingsApi;
    private int pwStrength = 0;
    private PrefsUtil prefsUtil;
    private MonetaryUtil monetaryUtil;
    @Thunk PayloadManager payloadManager;
    // Flag for setting 2FA after phone confirmation
    @Thunk boolean show2FaAfterPhoneVerified = false;

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (BalanceFragment.ACTION_INTENT.equals(intent.getAction())) {
                fetchUpdatedSettings();
            }
        }
    };

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        payloadManager = PayloadManager.getInstance();
        prefsUtil = new PrefsUtil(getActivity());
        monetaryUtil = new MonetaryUtil(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));

        fetchUpdatedSettings();
    }

    @Thunk
    void fetchUpdatedSettings() {
        new AsyncTask<Void, Void, Void>() {

            MaterialProgressDialog progress;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progress = new MaterialProgressDialog(getActivity());
                progress.setMessage(getActivity().getResources().getString(R.string.please_wait));
                progress.show();
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (progress != null && progress.isShowing()) {
                    progress.dismiss();
                    progress = null;
                }
                if (settingsApi != null) {
                    refreshList();
                }

                super.onPostExecute(aVoid);
            }

            @Override
            protected Void doInBackground(Void... params) {
                Payload payload = payloadManager.getPayload();
                settingsApi = new Settings(payload.getGuid(), payload.getSharedKey());
                return null;
            }

        }.execute();
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(BalanceFragment.ACTION_INTENT);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, filter);
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
        super.onPause();
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
    }

    @UiThread
    @Thunk
    void refreshList() {
        if (isAdded() && getActivity() != null) {
            PreferenceScreen prefScreen = getPreferenceScreen();
            if (prefScreen != null) prefScreen.removeAll();
            addPreferencesFromResource(R.xml.settings);

            //Profile
            PreferenceCategory profileCategory = (PreferenceCategory) findPreference("profile");
            guidPref = findPreference("guid");
            guidPref.setSummary(payloadManager.getPayload().getGuid());
            guidPref.setOnPreferenceClickListener(SettingsFragment.this);

            emailPref = findPreference("email");

            String emailAndStatus = settingsApi.getEmail();
            if (emailAndStatus == null || emailAndStatus.isEmpty()) {
                emailAndStatus = getString(R.string.not_specified);
            } else if (settingsApi.isEmailVerified()) {
                emailAndStatus += "  (" + getString(R.string.verified) + ")";
            } else {
                emailAndStatus += "  (" + getString(R.string.unverified) + ")";
            }
            emailPref.setSummary(emailAndStatus);
            emailPref.setOnPreferenceClickListener(SettingsFragment.this);

            smsPref = findPreference("mobile");
            String smsAndStatus = settingsApi.getSms();
            if (smsAndStatus == null || smsAndStatus.isEmpty()) {
                smsAndStatus = getString(R.string.not_specified);
            } else if (settingsApi.isSmsVerified()) {
                smsAndStatus += "  (" + getString(R.string.verified) + ")";
            } else {
                smsAndStatus += "  (" + getString(R.string.unverified) + ")";
            }
            smsPref.setSummary(smsAndStatus);
            smsPref.setOnPreferenceClickListener(SettingsFragment.this);

            //Preferences
            PreferenceCategory preferencesCategory = (PreferenceCategory) findPreference("preferences");
            unitsPref = findPreference("units");
            unitsPref.setSummary(getDisplayUnits());
            unitsPref.setOnPreferenceClickListener(SettingsFragment.this);

            fiatPref = findPreference("fiat");
            fiatPref.setSummary(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY));
            fiatPref.setOnPreferenceClickListener(SettingsFragment.this);

            emailNotificationPref = (SwitchPreferenceCompat) findPreference("email_notifications");
            if (settingsApi.isEmailVerified()) {
                emailNotificationPref.setOnPreferenceClickListener(this);
            } else {
                preferencesCategory.removePreference(emailNotificationPref);
            }

            smsNotificationPref = (SwitchPreferenceCompat) findPreference("sms_notifications");
            if (settingsApi.isSmsVerified()) {
                smsNotificationPref.setOnPreferenceClickListener(this);
            } else {
                preferencesCategory.removePreference(smsNotificationPref);
            }

            emailNotificationPref.setChecked(false);
            smsNotificationPref.setChecked(false);

            if (settingsApi.isNotificationsOn() && settingsApi.getNotificationTypes().size() > 0) {
                for (int type : settingsApi.getNotificationTypes()) {
                    if (type == Settings.NOTIFICATION_TYPE_EMAIL) {
                        emailNotificationPref.setChecked(true);
                    }

                    if (type == Settings.NOTIFICATION_TYPE_SMS) {
                        smsNotificationPref.setChecked(true);
                    }
                }
            } else {
                emailNotificationPref.setChecked(false);
                smsNotificationPref.setChecked(false);
            }

            //Security
            PreferenceCategory securityCategory = (PreferenceCategory) findPreference("security");
            pinPref = findPreference("pin");
            pinPref.setOnPreferenceClickListener(this);

            twoStepVerificationPref = (SwitchPreferenceCompat) findPreference("2fa");
            twoStepVerificationPref.setOnPreferenceClickListener(this);
            twoStepVerificationPref.setChecked(settingsApi.getAuthType() != Settings.AUTH_TYPE_OFF);

            set2FASummary(settingsApi.getAuthType());

            passwordHint1Pref = findPreference("pw_hint1");
            if (settingsApi.getPasswordHint1() != null && !settingsApi.getPasswordHint1().isEmpty()) {
                passwordHint1Pref.setSummary(settingsApi.getPasswordHint1());
            } else {
                passwordHint1Pref.setSummary("");
            }
            passwordHint1Pref.setOnPreferenceClickListener(this);

            changePasswordPref = findPreference("change_pw");
            changePasswordPref.setOnPreferenceClickListener(this);

            torPref = (SwitchPreferenceCompat) findPreference("tor");
            torPref.setChecked(settingsApi.isTorBlocked());
            torPref.setOnPreferenceClickListener(this);

            //App
            aboutPref = findPreference("about");
            aboutPref.setSummary("v" + BuildConfig.VERSION_NAME);
            aboutPref.setOnPreferenceClickListener(this);

            tosPref = findPreference("tos");
            tosPref.setOnPreferenceClickListener(this);

            privacyPref = findPreference("privacy");
            privacyPref.setOnPreferenceClickListener(this);

            disableRootWarningPref = findPreference("disable_root_warning");
            if (disableRootWarningPref != null &&
                    !new RootUtil().isDeviceRooted()) {
                PreferenceCategory appCategory = (PreferenceCategory) findPreference("app");
                appCategory.removePreference(disableRootWarningPref);
            }

            // Check if referred from Security Centre dialog
            if (getActivity().getIntent() != null && getActivity().getIntent().hasExtra(EXTRA_SHOW_TWO_FA_DIALOG)) {
                showDialogTwoFA();
            }
        }
    }

    private void set2FASummary(int type){
        switch (type) {
            case Settings.AUTH_TYPE_GOOGLE_AUTHENTICATOR:
                twoStepVerificationPref.setSummary(getString(R.string.google_authenticator));
                break;
            case Settings.AUTH_TYPE_SMS:
                twoStepVerificationPref.setSummary(getString(R.string.sms));
                break;
            case Settings.AUTH_TYPE_YUBI_KEY:
                twoStepVerificationPref.setSummary(getString(R.string.yubikey));
                break;
            default:
                twoStepVerificationPref.setSummary("");
                break;
        }
    }

    private String getDisplayUnits() {
        return (String) monetaryUtil.getBTCUnits()[prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)];
    }

    interface ExecutorListener {
        void onSuccess();
    }

    @UiThread
    private void updateEmail(String email, ExecutorListener listener) {
        if (email == null || email.isEmpty()) {
            email = getString(R.string.not_specified);
            emailPref.setSummary(email);
        } else {
            final String finalEmail = email;
            Handler handler = new Handler(Looper.getMainLooper());
            new BackgroundExecutor(getActivity(),
                    () -> settingsApi.setEmail(finalEmail, new Settings.ResultListener() {
                        @Override
                        public void onSuccess() {
                            handler.post(() -> {
                                listener.onSuccess();
                                updateNotification(false, Settings.NOTIFICATION_TYPE_EMAIL);
                                refreshList();
                            });
                        }

                        @Override
                        public void onFail() {
                            ToastCustom.makeText(getActivity(), getString(R.string.update_failed), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                        }

                        @Override
                        public void onBadRequest() {

                        }
                    })).execute();
        }
    }

    @UiThread
    private void updateSms(String sms) {
        if (sms == null || sms.isEmpty()) {
            sms = getString(R.string.not_specified);
            smsPref.setSummary(sms);
        } else {
            final String finalSms = sms;
            Handler handler = new Handler(Looper.getMainLooper());
            new BackgroundExecutor(getActivity(),
                    () -> settingsApi.setSms(finalSms, new Settings.ResultListener() {
                        @Override
                        public void onSuccess() {
                            handler.post(() -> {
                                updateNotification(false, Settings.NOTIFICATION_TYPE_SMS);
                                refreshList();
                                showDialogVerifySms();
                            });
                        }

                        @Override
                        public void onFail() {
                            show2FaAfterPhoneVerified = false;
                            ToastCustom.makeText(getActivity(), getString(R.string.update_failed), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                        }

                        @Override
                        public void onBadRequest() {

                        }
                    })).execute();

        }
    }

    @UiThread
    private void verifySms(final String code) {
        Handler handler = new Handler(Looper.getMainLooper());
        new BackgroundExecutor(getActivity(),
                () -> settingsApi.verifySms(code, new Settings.ResultListener() {
                    @Override
                    public void onSuccess() {
                        handler.post(() -> {
                            refreshList();
                            new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                                    .setTitle(R.string.success)
                                    .setMessage(R.string.sms_verified)
                                    .setPositiveButton(R.string.dialog_continue, (dialogInterface, i) -> {
                                        if (show2FaAfterPhoneVerified) showDialogTwoFA();
                                    })
                                    .show();
                        });
                    }

                    @Override
                    public void onFail() {
                        show2FaAfterPhoneVerified = false;
                        ToastCustom.makeText(getActivity(), getString(R.string.verification_failed), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                    }

                    @Override
                    public void onBadRequest() {

                    }
                })).execute();
    }

    @UiThread
    private void updateTor(final boolean enabled) {
        Handler handler = new Handler(Looper.getMainLooper());
        new BackgroundExecutor(getActivity(),
                () -> settingsApi.setTorBlocked(enabled, new Settings.ResultListener() {
                    @Override
                    public void onSuccess() {
                        handler.post(() -> torPref.setChecked(enabled));
                    }

                    @Override
                    public void onFail() {
                        ToastCustom.makeText(getActivity(), getString(R.string.update_failed), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                    }

                    @Override
                    public void onBadRequest() {

                    }
                })).execute();
    }

    @UiThread
    private void updatePasswordHint(final String hint) {
        Handler handler = new Handler(Looper.getMainLooper());
        new BackgroundExecutor(getActivity(),
                () -> settingsApi.setPasswordHint1(hint, new Settings.ResultListener() {
                    @Override
                    public void onSuccess() {
                        handler.post(() -> passwordHint1Pref.setSummary(hint));
                    }

                    @Override
                    public void onFail() {
                        ToastCustom.makeText(getActivity(), getString(R.string.update_failed), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                    }

                    @Override
                    public void onBadRequest() {

                    }
                })).execute();
    }

    @UiThread
    private void updatePin(final String pin) {
        new AsyncTask<Void, Void, CharSequenceX>() {

            MaterialProgressDialog progress;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progress = new MaterialProgressDialog(getActivity());
                progress.setMessage(getActivity().getResources().getString(R.string.please_wait));
                progress.show();
            }

            @Override
            protected void onPostExecute(CharSequenceX params) {
                if (progress != null && progress.isShowing()) {
                    progress.dismiss();
                    progress = null;
                }
                if (params != null) {
                    prefsUtil.removeValue(PrefsUtil.KEY_PIN_FAILS);
                    prefsUtil.removeValue(PrefsUtil.KEY_PIN_IDENTIFIER);

                    Intent intent = new Intent(getActivity(), PinEntryActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } else {
                    ToastCustom.makeText(getActivity(), getString(R.string.invalid_pin), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                }

                super.onPostExecute(params);
            }

            @Override
            protected CharSequenceX doInBackground(Void... params) {
                try {
                    return AccessState.getInstance().validatePIN(pin);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }.execute();
    }

    @UiThread
    @Thunk
    void updateNotification(final boolean enabled, int notificationType) {

        if (enabled) {

            Handler handler = new Handler(Looper.getMainLooper());
            new BackgroundExecutor(getActivity(),
                    () -> settingsApi.enableNotification(notificationType, new Settings.ResultListener() {
                        @Override
                        public void onSuccess() {
                            if (notificationType == Settings.NOTIFICATION_TYPE_EMAIL) {
                                handler.post(() -> emailNotificationPref.setChecked(enabled));
                            } else if (notificationType == Settings.NOTIFICATION_TYPE_SMS) {
                                handler.post(() -> smsNotificationPref.setChecked(enabled));
                            }
                        }

                        @Override
                        public void onFail() {
                            ToastCustom.makeText(getActivity(), getString(R.string.update_failed), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                        }

                        @Override
                        public void onBadRequest() {

                        }
                    })).execute();

        } else {

            Handler handler = new Handler(Looper.getMainLooper());
            new BackgroundExecutor(getActivity(),
                    () -> settingsApi.disableNotification(notificationType, new Settings.ResultListener() {
                        @Override
                        public void onSuccess() {
                            if (notificationType == Settings.NOTIFICATION_TYPE_EMAIL) {
                                handler.post(() -> emailNotificationPref.setChecked(enabled));
                            } else if (notificationType == Settings.NOTIFICATION_TYPE_SMS) {
                                handler.post(() -> smsNotificationPref.setChecked(enabled));
                            }
                        }

                        @Override
                        public void onFail() {
                            ToastCustom.makeText(getActivity(), getString(R.string.update_failed), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                        }

                        @Override
                        public void onBadRequest() {

                        }
                    })).execute();
        }
    }

    @UiThread
    private void update2FA(final int type) {
        Handler handler = new Handler(Looper.getMainLooper());
        new BackgroundExecutor(getActivity(),
                () -> settingsApi.setAuthType(type, new Settings.ResultListener() {
                    @Override
                    public void onSuccess() {
                        handler.post(() -> {
                            twoStepVerificationPref.setChecked(type != Settings.AUTH_TYPE_OFF);
                            set2FASummary(type);
                        });
                    }

                    @Override
                    public void onFail() {
                        ToastCustom.makeText(getActivity(), getString(R.string.update_failed), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                        handler.post(() -> {
                            twoStepVerificationPref.setChecked(type != Settings.AUTH_TYPE_OFF);
                            set2FASummary(type);
                        });
                    }

                    @Override
                    public void onBadRequest() {

                    }
                })).execute();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {

        switch (preference.getKey()) {

            case "email":
                showDialogEmail();
                break;

            case "email_notifications":
                showDialogEmailNotifications();
                break;

            case "sms_notifications":
                showDialogSmsNotifications();
                break;

            case "mobile":
                showDialogMobile();
                break;

            case "verify_mobile":
                showDialogVerifySms();
                break;

            case "guid":
                showDialogGUI();
                break;

            case "units":
                showDialogBTCUnits();
                break;

            case "fiat":
                showDialogFiatUnits();
                break;

            case "2fa":
                showDialogTwoFA();
                break;

            case "pin":
                showDialogChangePin();
                break;

            case "pw_hint1":
                showDialogPasswordHint();
                break;

            case "change_pw":
                showDialogChangePasswordWarning();
                break;

            case "tor":
                showDialogTorEnable();
                break;

            case "about":
                DialogFragment aboutDialog = new AboutDialog();
                aboutDialog.show(getFragmentManager(), "ABOUT_DIALOG");
                break;

            case "tos":
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(URL_TOS_POLICY)));
                break;

            case "privacy":
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(URL_PRIVACY_POLICY)));
                break;

            case "disable_root_warning":
                break;
        }

        return true;
    }

    private void showDialogTorEnable() {
        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.tor_requests)
                .setMessage(R.string.tor_summary)
                .setCancelable(false)
                .setPositiveButton(R.string.block, (dialogInterface, i) -> updateTor(true))
                .setNegativeButton(R.string.allow, (dialogInterface, i) -> updateTor(false))
                .create()
                .show();
    }

    private void showDialogEmail() {

        final AppCompatEditText etEmail = new AppCompatEditText(getActivity());
        etEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        etEmail.setText(settingsApi.getEmail());
        etEmail.setSelection(etEmail.getText().length());

        FrameLayout frameLayout = new FrameLayout(getActivity());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int marginInPixels = (int) ViewUtils.convertDpToPixel(20, getActivity());
        params.setMargins(marginInPixels, 0, marginInPixels, 0);
        frameLayout.addView(etEmail, params);

        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.email)
                .setMessage(R.string.verify_email2)
                .setView(frameLayout)
                .setCancelable(false)
                .setPositiveButton(R.string.update, (dialogInterface, i) -> {
                    String email = etEmail.getText().toString();

                    if (!FormatsUtil.getInstance().isValidEmailAddress(email)) {
                        ToastCustom.makeText(getActivity(), getString(R.string.invalid_email), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                    } else {
                        updateEmail(email, this::showDialogEmailVerification);
                    }
                })
                .setNeutralButton(R.string.resend, (dialogInterface, i) -> {
                    //Resend verification code
                    updateEmail(settingsApi.getEmail(), this::showDialogEmailVerification);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .show();
    }

    private void showDialogEmailVerification() {
        // Slight delay to prevent UI blinking issues
        Handler handler = new Handler();
        handler.postDelayed(() -> new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.verify)
                .setMessage(R.string.verify_email_notice)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, null)
                .show(), 300);
    }

    private void showDialogMobile() {

        if (settingsApi.getAuthType() != Settings.AUTH_TYPE_OFF) {
            new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                    .setTitle(R.string.warning)
                    .setMessage(R.string.disable_2fa_first)
                    .setPositiveButton(android.R.string.ok, null)
                    .create().show();

        } else {

            LayoutInflater inflater = getActivity().getLayoutInflater();
            View smsPickerView = inflater.inflate(R.layout.include_sms_update, null);
            final AppCompatEditText etMobile = (AppCompatEditText) smsPickerView.findViewById(R.id.etSms);
            final TextView tvCountry = (TextView) smsPickerView.findViewById(R.id.tvCountry);
            final TextView tvSms = (TextView) smsPickerView.findViewById(R.id.tvSms);

            final CountryPicker picker = CountryPicker.newInstance(getString(R.string.select_country));
            final Country country = picker.getUserCountryInfo(getActivity());
            if (country.getDialCode().equals("93")) {
                setCountryFlag(tvCountry, "+1", R.drawable.flag_us);
            } else {
                setCountryFlag(tvCountry, country.getDialCode(), country.getFlag());
            }
            tvCountry.setOnClickListener(v -> {

                picker.show(getFragmentManager(), "COUNTRY_PICKER");
                picker.setListener((name, code, dialCode, flagDrawableResID) -> {

                    setCountryFlag(tvCountry, dialCode, flagDrawableResID);
                    picker.dismiss();
                });
            });

            if (!settingsApi.isSmsVerified() && settingsApi.getSms() != null && !settingsApi.getSms().isEmpty()) {
                tvSms.setText(settingsApi.getSms());
                tvSms.setVisibility(View.VISIBLE);
            } else {
                tvSms.setVisibility(View.GONE);
            }

            final AlertDialog.Builder alertDialogSmsBuilder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                    .setTitle(R.string.mobile)
                    .setMessage(getString(R.string.mobile_description))
                    .setView(smsPickerView)
                    .setCancelable(false)
                    .setPositiveButton(R.string.update, null)
                    .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> show2FaAfterPhoneVerified = false);

            if (!settingsApi.isSmsVerified() && settingsApi.getSms() != null && !settingsApi.getSms().isEmpty()) {
                alertDialogSmsBuilder.setNeutralButton(R.string.verify, (dialogInterface, i) -> showDialogVerifySms());
            }

            AlertDialog dialog = alertDialogSmsBuilder.create();
            dialog.setOnShowListener(dialogInterface -> {
                Button positive = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                positive.setOnClickListener(view -> {
                    final String sms = tvCountry.getText().toString() + etMobile.getText().toString();

                    if (!FormatsUtil.getInstance().isValidMobileNumber(sms)) {
                        ToastCustom.makeText(getActivity(), getString(R.string.invalid_mobile), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                    } else {
                        updateSms(sms);
                        dialog.dismiss();
                    }
                });
            });

            dialog.show();
        }
    }

    private void showDialogGUI() {
        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.guid_to_clipboard)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, (dialog, whichButton) -> {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getActivity().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = null;
                    clip = android.content.ClipData.newPlainText("guid", payloadManager.getPayload().getGuid());
                    clipboard.setPrimaryClip(clip);
                    ToastCustom.makeText(getActivity(), getString(R.string.copied_to_clipboard), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void showDialogBTCUnits() {
        final CharSequence[] units = monetaryUtil.getBTCUnits();
        final int sel = prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, 0);

        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.select_units)
                .setSingleChoiceItems(units, sel, (dialog, which) -> {
                    prefsUtil.setValue(PrefsUtil.KEY_BTC_UNITS, which);
                    unitsPref.setSummary(getDisplayUnits());
                    dialog.dismiss();
                })
                .show();
    }

    private void showDialogFiatUnits() {
        final String[] currencies = ExchangeRateFactory.getInstance().getCurrencyLabels();
        String strCurrency = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        int selected = 0;
        for (int i = 0; i < currencies.length; i++) {
            if (currencies[i].endsWith(strCurrency)) {
                selected = i;
                break;
            }
        }

        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.select_currency)
                .setSingleChoiceItems(currencies, selected, (dialog, which) -> {
                    prefsUtil.setValue(PrefsUtil.KEY_SELECTED_FIAT, currencies[which].substring(currencies[which].length() - 3));
                    fiatPref.setSummary(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY));
                    dialog.dismiss();
                })
                .show();
    }

    @Thunk
    void showDialogVerifySms() {

        final AppCompatEditText etSms = new AppCompatEditText(getActivity());
        etSms.setSingleLine(true);
        FrameLayout frameLayout = new FrameLayout(getActivity());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int marginInPixels = (int) ViewUtils.convertDpToPixel(20, getActivity());
        params.setMargins(marginInPixels, 0, marginInPixels, 0);
        frameLayout.addView(etSms, params);

        AlertDialog dialog = new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.verify_mobile)
                .setMessage(R.string.verify_sms_summary)
                .setView(frameLayout)
                .setCancelable(false)
                .setPositiveButton(R.string.verify, null)
                .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> show2FaAfterPhoneVerified = false)
                .setNeutralButton(R.string.resend, (dialogInterface, i) -> updateSms(settingsApi.getSms()))
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positive = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            positive.setOnClickListener(view -> {
                final String codeS = etSms.getText().toString();
                if (codeS.length() > 0) {
                    verifySms(codeS);
                    dialog.dismiss();
                }
            });
        });

        dialog.show();
    }

    private void showDialogPasswordHint() {
        final AppCompatEditText etPwHint1 = new AppCompatEditText(getActivity());
        etPwHint1.setText(settingsApi.getPasswordHint1());
        etPwHint1.setSelection(etPwHint1.getText().length());
        etPwHint1.setSingleLine(true);
        etPwHint1.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        FrameLayout frameLayout = new FrameLayout(getActivity());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int marginInPixels = (int) ViewUtils.convertDpToPixel(20, getActivity());
        params.setMargins(marginInPixels, 0, marginInPixels, 0);
        frameLayout.addView(etPwHint1, params);

        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.password_hint)
                .setMessage(R.string.password_hint_summary)
                .setView(frameLayout)
                .setCancelable(false)
                .setPositiveButton(R.string.update, (dialogInterface, i) -> {
                    String hint = etPwHint1.getText().toString();
                    if (!hint.equals(payloadManager.getTempPassword().toString())) {
                        updatePasswordHint(hint);
                    } else {
                        ToastCustom.makeText(getActivity(), getString(R.string.hint_reveals_password_error), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .show();
    }

    private void showDialogChangePin() {
        Intent intent = new Intent(getActivity(), PinEntryActivity.class);
        intent.putExtra(KEY_VALIDATING_PIN_FOR_RESULT, true);
        startActivityForResult(intent, REQUEST_CODE_VALIDATE_PIN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_VALIDATE_PIN && resultCode == RESULT_OK) {
            String pin = data.getStringExtra(KEY_VALIDATED_PIN);
            updatePin(pin);
        }
    }

    private void showDialogEmailNotifications() {
        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.email_notifications)
                .setMessage(R.string.email_notifications_summary)
                .setCancelable(false)
                .setPositiveButton(R.string.enable, (dialogInterface, i) -> updateNotification(true, Settings.NOTIFICATION_TYPE_EMAIL))
                .setNegativeButton(R.string.disable, (dialogInterface, i) -> updateNotification(false, Settings.NOTIFICATION_TYPE_EMAIL))
                .create()
                .show();
    }

    private void showDialogSmsNotifications() {
        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.sms_notifications)
                .setMessage(R.string.sms_notifications_summary)
                .setCancelable(false)
                .setPositiveButton(R.string.enable, (dialogInterface, i) -> updateNotification(true, Settings.NOTIFICATION_TYPE_SMS))
                .setNegativeButton(R.string.disable, (dialogInterface, i) -> updateNotification(false, Settings.NOTIFICATION_TYPE_SMS))
                .create()
                .show();
    }

    private void showDialogChangePasswordWarning() {
        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.warning)
                .setMessage(R.string.change_password_summary)
                .setPositiveButton(R.string.dialog_continue, (dialog, which) -> showDialogChangePassword())
                .show();
    }

    private void showDialogChangePassword() {

        LayoutInflater inflater = (LayoutInflater) getActivity().getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final LinearLayout pwLayout = (LinearLayout) inflater.inflate(R.layout.modal_change_password2, null);

        AppCompatEditText etCurrentPw = (AppCompatEditText) pwLayout.findViewById(R.id.current_password);
        AppCompatEditText etNewPw = (AppCompatEditText) pwLayout.findViewById(R.id.new_password);
        AppCompatEditText etNewConfirmedPw = (AppCompatEditText) pwLayout.findViewById(R.id.confirm_password);

        LinearLayout entropyMeter = (LinearLayout) pwLayout.findViewById(R.id.entropy_meter);
        ProgressBar passStrengthBar = (ProgressBar) pwLayout.findViewById(R.id.pass_strength_bar);
        passStrengthBar.setMax(100);
        TextView passStrengthVerdict = (TextView) pwLayout.findViewById(R.id.pass_strength_verdict);

        etNewPw.addTextChangedListener(new TextWatcher() {
            private Timer timer = new Timer();

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(final Editable editable) {
                timer.cancel();
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        getActivity().runOnUiThread(() -> {
                            entropyMeter.setVisibility(View.VISIBLE);
                            setPasswordStrength(passStrengthVerdict, passStrengthBar, editable.toString());
                        });
                    }
                }, 200);
            }
        });

        AlertDialog alertDialog = new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.change_password)
                .setCancelable(false)
                .setView(pwLayout)
                .setPositiveButton(R.string.update, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        alertDialog.setOnShowListener(dialog -> {
            Button buttonPositive = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonPositive.setOnClickListener(view -> {

                String currentPw = etCurrentPw.getText().toString();
                String newPw = etNewPw.getText().toString();
                String newConfirmedPw = etNewConfirmedPw.getText().toString();
                final CharSequenceX walletPassword = payloadManager.getTempPassword();

                if (!currentPw.equals(newPw)) {
                    if (currentPw.equals(walletPassword.toString())) {
                        if (newPw.equals(newConfirmedPw)) {
                            if (newConfirmedPw == null || newConfirmedPw.length() < 4 || newConfirmedPw.length() > 255) {
                                ToastCustom.makeText(getActivity(), getString(R.string.invalid_password), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                            } else if (newConfirmedPw.equals(settingsApi.getPasswordHint1())) {
                                ToastCustom.makeText(getActivity(), getString(R.string.hint_reveals_password_error), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                            } else if (pwStrength < 50) {
                                new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                                        .setTitle(R.string.app_name)
                                        .setMessage(R.string.weak_password)
                                        .setCancelable(false)
                                        .setPositiveButton(R.string.yes, (dialog1, which) -> {
                                            etNewConfirmedPw.setText("");
                                            etNewConfirmedPw.requestFocus();
                                            etNewPw.setText("");
                                            etNewPw.requestFocus();
                                        })
                                        .setNegativeButton(R.string.polite_no, (dialog1, which) ->
                                                updatePassword(alertDialog, new CharSequenceX(newConfirmedPw), walletPassword))
                                        .show();
                            } else {
                                updatePassword(alertDialog, new CharSequenceX(newConfirmedPw), walletPassword);
                            }
                        } else {
                            etNewConfirmedPw.setText("");
                            etNewConfirmedPw.requestFocus();
                            ToastCustom.makeText(getActivity(), getString(R.string.password_mismatch_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                        }
                    } else {
                        etCurrentPw.setText("");
                        etCurrentPw.requestFocus();
                        ToastCustom.makeText(getActivity(), getString(R.string.invalid_password), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                    }
                } else {
                    etNewPw.setText("");
                    etNewConfirmedPw.setText("");
                    etNewPw.requestFocus();
                    ToastCustom.makeText(getActivity(), getString(R.string.change_password_new_matches_current), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                }
            });
        });
        alertDialog.show();
    }

    @Thunk
    void showDialogTwoFA() {
        if (!settingsApi.isSmsVerified()) {
            twoStepVerificationPref.setChecked(false);
            show2FaAfterPhoneVerified = true;
            showDialogMobile();
        } else {
            show2FaAfterPhoneVerified = false;
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                    .setTitle(R.string.two_fa)
                    .setMessage(R.string.two_fa_summary)
                    .setNeutralButton(android.R.string.cancel, (dialogInterface, i) -> {
                        twoStepVerificationPref.setChecked(settingsApi.getAuthType() != Settings.AUTH_TYPE_OFF);
                        set2FASummary(settingsApi.getAuthType());
                    });

            if (settingsApi.getAuthType() != Settings.AUTH_TYPE_OFF) {
                alertDialogBuilder.setNegativeButton(R.string.disable, (dialogInterface, i) -> update2FA(Settings.AUTH_TYPE_OFF));
            } else {
                //TODO - Currently only SMS 2FA on android
                alertDialogBuilder.setPositiveButton(R.string.enable, (dialogInterface, i) -> update2FA(Settings.AUTH_TYPE_SMS));
            }
            alertDialogBuilder.create()
                    .show();
        }
    }

    private void updatePassword(AlertDialog alertDialog, final CharSequenceX updatedPassword, final CharSequenceX fallbackPassword) {
        MaterialProgressDialog progress = new MaterialProgressDialog(getActivity());
        progress.setMessage(getActivity().getResources().getString(R.string.please_wait));
        progress.setCancelable(false);
        progress.show();

        new Thread(() -> {
            Looper.prepare();

            payloadManager.setTempPassword(updatedPassword);

            if (AccessState.getInstance().createPIN(updatedPassword, AccessState.getInstance().getPIN())
                    && payloadManager.savePayloadToServer()) {

                ToastCustom.makeText(getActivity(), getString(R.string.password_changed), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_OK);
            } else {
                //Revert on fail
                payloadManager.setTempPassword(fallbackPassword);
                ToastCustom.makeText(getActivity(), getString(R.string.remote_save_ko), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                ToastCustom.makeText(getActivity(), getString(R.string.password_unchanged), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            }
            progress.dismiss();
            alertDialog.dismiss();
            Looper.loop();
        }).start();
    }

    @UiThread
    private void setPasswordStrength(TextView passStrengthVerdict, ProgressBar passStrengthBar, String pw) {
        int[] strengthVerdicts = {R.string.strength_weak, R.string.strength_medium, R.string.strength_normal, R.string.strength_strong};
        int[] strengthColors = {R.drawable.progress_red, R.drawable.progress_orange, R.drawable.progress_blue, R.drawable.progress_green};
        pwStrength = (int) Math.round(PasswordUtil.getInstance().getStrength(pw));

        if (pw.equals(prefsUtil.getValue(PrefsUtil.KEY_EMAIL, ""))) pwStrength = 0;

        int pwStrengthLevel = 0;//red
        if (pwStrength >= 75) pwStrengthLevel = 3;//green
        else if (pwStrength >= 50) pwStrengthLevel = 2;//green
        else if (pwStrength >= 25) pwStrengthLevel = 1;//orange

        passStrengthBar.setProgress(pwStrength);
        passStrengthBar.setProgressDrawable(ContextCompat.getDrawable(getActivity(), strengthColors[pwStrengthLevel]));
        passStrengthVerdict.setText(getResources().getString(strengthVerdicts[pwStrengthLevel]));
    }

    @UiThread
    private void setCountryFlag(TextView tvCountry, String dialCode, int flagResourceId) {
        tvCountry.setText(dialCode);
        Drawable drawable = getResources().getDrawable(flagResourceId);
        drawable.setAlpha(30);
        tvCountry.setBackgroundDrawable(drawable);
    }
}
