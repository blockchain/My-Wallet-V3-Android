package piuk.blockchain.android.ui.settings;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.StringRes;
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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mukesh.countrypicker.fragments.CountryPicker;
import com.mukesh.countrypicker.models.Country;

import info.blockchain.api.Settings;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.FormatsUtil;
import info.blockchain.wallet.util.PasswordUtil;

import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.R;
import piuk.blockchain.android.ui.auth.PinEntryActivity;
import piuk.blockchain.android.ui.balance.BalanceFragment;
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.fingerprint.FingerprintDialog;
import piuk.blockchain.android.util.AndroidUtils;
import piuk.blockchain.android.util.ExchangeRateFactory;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.RootUtil;
import piuk.blockchain.android.util.ViewUtils;
import piuk.blockchain.android.util.annotations.Thunk;

import static android.app.Activity.RESULT_OK;
import static piuk.blockchain.android.R.string.email;
import static piuk.blockchain.android.R.string.success;
import static piuk.blockchain.android.ui.auth.PinEntryActivity.KEY_VALIDATED_PIN;
import static piuk.blockchain.android.ui.auth.PinEntryActivity.KEY_VALIDATING_PIN_FOR_RESULT;
import static piuk.blockchain.android.ui.auth.PinEntryActivity.REQUEST_CODE_VALIDATE_PIN;

public class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener, SettingsViewModel.DataListener {

    public static final String EXTRA_SHOW_TWO_FA_DIALOG = "show_two_fa_dialog";
    public static final String EXTRA_SHOW_ADD_EMAIL_DIALOG = "show_add_email_dialog";
    public static final String URL_TOS_POLICY = "https://blockchain.com/terms";
    public static final String URL_PRIVACY_POLICY = "https://blockchain.com/privacy";
    public static final int REQUEST_CODE_VALIDATE_PIN_FOR_FINGERPRINT = 1984;

    // Profile
    private Preference guidPref;
    private Preference emailPref;
    private Preference smsPref;

    // Preferences
    private Preference unitsPref;
    private Preference fiatPref;
    private SwitchPreferenceCompat emailNotificationPref;
    private SwitchPreferenceCompat smsNotificationPref;

    // Security
    @Thunk SwitchPreferenceCompat fingerprintPref;
    private SwitchPreferenceCompat twoStepVerificationPref;
    private Preference passwordHint1Pref;
    private SwitchPreferenceCompat torPref;
    private SwitchPreferenceCompat launcherShortcutPrefs;

    @Thunk SettingsViewModel viewModel;
    private int pwStrength = 0;
    private MaterialProgressDialog progressDialog;

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (BalanceFragment.ACTION_INTENT.equals(intent.getAction())) {
                viewModel.onViewReady();
            }
        }
    };

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new SettingsViewModel(this);
        viewModel.onViewReady();
    }

    @SuppressLint("NewApi")
    @Override
    public void setUpUi() {
        PreferenceScreen prefScreen = getPreferenceScreen();
        if (prefScreen != null) prefScreen.removeAll();

        addPreferencesFromResource(R.xml.settings);

        // Profile
        guidPref = findPreference("guid");
        guidPref.setOnPreferenceClickListener(this);

        emailPref = findPreference("email");
        emailPref.setOnPreferenceClickListener(this);

        smsPref = findPreference("mobile");
        smsPref.setOnPreferenceClickListener(this);

        // Preferences
        unitsPref = findPreference("units");
        unitsPref.setOnPreferenceClickListener(this);

        fiatPref = findPreference("fiat");
        fiatPref.setOnPreferenceClickListener(this);

        emailNotificationPref = (SwitchPreferenceCompat) findPreference("email_notifications");
        emailNotificationPref.setOnPreferenceClickListener(this);

        smsNotificationPref = (SwitchPreferenceCompat) findPreference("sms_notifications");
        smsNotificationPref.setOnPreferenceClickListener(this);

        // Security
        fingerprintPref = (SwitchPreferenceCompat) findPreference("fingerprint");
        fingerprintPref.setOnPreferenceClickListener(this);

        Preference pinPref = findPreference("pin");
        pinPref.setOnPreferenceClickListener(this);

        twoStepVerificationPref = (SwitchPreferenceCompat) findPreference("2fa");
        twoStepVerificationPref.setOnPreferenceClickListener(this);

        passwordHint1Pref = findPreference("pw_hint1");
        passwordHint1Pref.setOnPreferenceClickListener(this);

        Preference changePasswordPref = findPreference("change_pw");
        changePasswordPref.setOnPreferenceClickListener(this);

        torPref = (SwitchPreferenceCompat) findPreference("tor");
        torPref.setOnPreferenceClickListener(this);

        launcherShortcutPrefs = (SwitchPreferenceCompat) findPreference("receive_shortcuts_enabled");
        launcherShortcutPrefs.setOnPreferenceClickListener(this);
        launcherShortcutPrefs.setOnPreferenceChangeListener((preference, newValue) -> {
            if (!((Boolean) newValue) && AndroidUtils.is25orHigher()) {
                getActivity().getSystemService(ShortcutManager.class).removeAllDynamicShortcuts();
            }
            return true;
        });

        // App
        Preference aboutPref = findPreference("about");
        aboutPref.setSummary("v" + BuildConfig.VERSION_NAME);
        aboutPref.setOnPreferenceClickListener(this);

        Preference tosPref = findPreference("tos");
        tosPref.setOnPreferenceClickListener(this);

        Preference privacyPref = findPreference("privacy");
        privacyPref.setOnPreferenceClickListener(this);

        Preference disableRootWarningPref = findPreference("disable_root_warning");
        if (disableRootWarningPref != null && !new RootUtil().isDeviceRooted()) {
            PreferenceCategory appCategory = (PreferenceCategory) findPreference("app");
            appCategory.removePreference(disableRootWarningPref);
        }

        // Check if referred from Security Centre dialog
        if (getActivity().getIntent() != null && getActivity().getIntent().hasExtra(EXTRA_SHOW_TWO_FA_DIALOG)) {
            showDialogTwoFA();
        } else if (getActivity().getIntent() != null && getActivity().getIntent().hasExtra(EXTRA_SHOW_ADD_EMAIL_DIALOG)) {
            showDialogEmail();
        }
    }

    @Override
    public void showProgressDialog(@StringRes int message) {
        progressDialog = new MaterialProgressDialog(getActivity());
        progressDialog.setCancelable(false);
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    @Override
    public void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    @Override
    public void showToast(@StringRes int message, @ToastCustom.ToastType String toastType) {
        ToastCustom.makeText(getActivity(), getString(message), ToastCustom.LENGTH_SHORT, toastType);
    }

    @Override
    public void showWarningDialog(@StringRes int message) {
        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(message)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, null)
                .setOnDismissListener(dialogInterface -> showDialogVerifySms())
                .create()
                .show();
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
        // No-op
    }

    @Override
    public void setGuidSummary(String summary) {
        guidPref.setSummary(summary);
    }

    @Override
    public void setEmailSummary(String summary) {
        emailPref.setSummary(summary);
    }

    @Override
    public void setSmsSummary(String summary) {
        smsPref.setSummary(summary);
    }

    @Override
    public void setUnitsSummary(String summary) {
        unitsPref.setSummary(summary);
    }

    @Override
    public void setFiatSummary(String summary) {
        fiatPref.setSummary(summary);
    }

    @Override
    public void setEmailNotificationsVisibility(boolean visible) {
        emailNotificationPref.setVisible(visible);
    }

    @Override
    public void setSmsNotificationsVisibility(boolean visible) {
        smsNotificationPref.setVisible(visible);
    }

    @Override
    public void setEmailNotificationPref(boolean enabled) {
        emailNotificationPref.setChecked(enabled);
    }

    @Override
    public void setSmsNotificationPref(boolean enabled) {
        smsNotificationPref.setChecked(enabled);
    }

    @Override
    public void setFingerprintVisibility(boolean visible) {
        fingerprintPref.setVisible(visible);
    }

    @Override
    public void setTwoFaPreference(boolean enabled) {
        twoStepVerificationPref.setChecked(enabled);
    }

    @Override
    public void setTwoFaSummary(String summary) {
        twoStepVerificationPref.setSummary(summary);
    }

    @Override
    public void setPasswordHintSummary(String summary) {
        passwordHint1Pref.setSummary(summary);
    }

    @Override
    public void setTorBlocked(boolean blocked) {
        torPref.setChecked(blocked);
    }

    @Override
    public void setLauncherShortcutVisibility(boolean visible) {
        launcherShortcutPrefs.setVisible(visible);
    }

    private void onFingerprintClicked() {
        viewModel.onFingerprintClicked();
    }

    @Override
    public void showDisableFingerprintDialog() {
        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.fingerprint_disable_message)
                .setCancelable(true)
                .setPositiveButton(R.string.yes, (dialog, which) -> viewModel.setFingerprintUnlockEnabled(false))
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> updateFingerprintPreferenceStatus())
                .show();
    }

    @Override
    public void showNoFingerprintsAddedDialog() {
        updateFingerprintPreferenceStatus();
        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.fingerprint_no_fingerprints_added)
                .setCancelable(true)
                .setPositiveButton(R.string.yes, (dialog, which) ->
                        startActivityForResult(new Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS), 0))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void updateFingerprintPreferenceStatus() {
        fingerprintPref.setChecked(viewModel.getIfFingerprintUnlockEnabled());
    }

    @Override
    public void verifyPinCode() {
        Intent intent = new Intent(getActivity(), PinEntryActivity.class);
        intent.putExtra(KEY_VALIDATING_PIN_FOR_RESULT, true);
        startActivityForResult(intent, REQUEST_CODE_VALIDATE_PIN_FOR_FINGERPRINT);
    }

    @Override
    public void showFingerprintDialog(CharSequenceX pincode) {
        FingerprintDialog dialog = FingerprintDialog.newInstance(pincode, FingerprintDialog.Stage.REGISTER_FINGERPRINT);
        dialog.setAuthCallback(new FingerprintDialog.FingerprintAuthCallback() {
            @Override
            public void onAuthenticated(CharSequenceX data) {
                dialog.dismiss();
                viewModel.setFingerprintUnlockEnabled(true);
            }

            @Override
            public void onCanceled() {
                dialog.dismiss();
                viewModel.setFingerprintUnlockEnabled(false);
                fingerprintPref.setChecked(viewModel.getIfFingerprintUnlockEnabled());
            }
        });
        dialog.show(getFragmentManager(), FingerprintDialog.TAG);
    }

    @Override
    public void showDialogSmsVerified() {
        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(success)
                .setMessage(R.string.sms_verified)
                .setPositiveButton(R.string.dialog_continue, (dialogInterface, i) -> showDialogTwoFA())
                .show();
    }

    @Override
    public void goToPinEntryPage() {
        Intent intent = new Intent(getActivity(), PinEntryActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
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
                showDialogGuid();
                break;
            case "units":
                showDialogBTCUnits();
                break;
            case "fiat":
                showDialogFiatUnits();
                break;
            case "fingerprint":
                onFingerprintClicked();
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
            case "receive_shortcuts_enabled":
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
                .setPositiveButton(R.string.block, (dialogInterface, i) -> viewModel.updateTor(true))
                .setNegativeButton(R.string.allow, (dialogInterface, i) -> viewModel.updateTor(false))
                .create()
                .show();
    }

    private void showDialogEmail() {
        AppCompatEditText editText = new AppCompatEditText(getActivity());
        editText.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        editText.setText(viewModel.getEmail());
        editText.setSelection(editText.getText().length());

        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(email)
                .setMessage(R.string.verify_email2)
                .setView(ViewUtils.getAlertDialogEditTextLayout(getActivity(), editText))
                .setCancelable(false)
                .setPositiveButton(R.string.update, (dialogInterface, i) -> {
                    String email = editText.getText().toString();

                    if (!FormatsUtil.getInstance().isValidEmailAddress(email)) {
                        ToastCustom.makeText(getActivity(), getString(R.string.invalid_email), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                    } else {
                        viewModel.updateEmail(email);
                    }
                })
                .setNeutralButton(R.string.resend, (dialogInterface, i) -> {
                    // Resend verification code
                    viewModel.updateEmail(viewModel.getEmail());
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .show();
    }

    @Override
    public void showDialogEmailVerification() {
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
        if (viewModel.getAuthType() != Settings.AUTH_TYPE_OFF) {
            new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                    .setTitle(R.string.warning)
                    .setMessage(R.string.disable_2fa_first)
                    .setPositiveButton(android.R.string.ok, null)
                    .create()
                    .show();
        } else {
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View smsPickerView = inflater.inflate(R.layout.include_sms_update, null);
            AppCompatEditText mobileNumber = (AppCompatEditText) smsPickerView.findViewById(R.id.etSms);
            TextView countryTextView = (TextView) smsPickerView.findViewById(R.id.tvCountry);
            TextView mobileNumberTextView = (TextView) smsPickerView.findViewById(R.id.tvSms);

            CountryPicker picker = CountryPicker.newInstance(getString(R.string.select_country));
            Country country = picker.getUserCountryInfo(getActivity());
            if (country.getDialCode().equals("+93")) {
                setCountryFlag(countryTextView, "+1", R.drawable.flag_us);
            } else {
                setCountryFlag(countryTextView, country.getDialCode(), country.getFlag());
            }

            countryTextView.setOnClickListener(v -> {
                picker.show(getFragmentManager(), "COUNTRY_PICKER");
                picker.setListener((name, code, dialCode, flagDrawableResID) -> {
                    setCountryFlag(countryTextView, dialCode, flagDrawableResID);
                    picker.dismiss();
                });
            });

            if (!viewModel.getSms().isEmpty()) {
                mobileNumberTextView.setText(viewModel.getSms());
                mobileNumberTextView.setVisibility(View.VISIBLE);
            }

            AlertDialog.Builder alertDialogSmsBuilder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                    .setTitle(R.string.mobile)
                    .setMessage(getString(R.string.mobile_description))
                    .setView(smsPickerView)
                    .setCancelable(false)
                    .setPositiveButton(R.string.update, null)
                    .setNegativeButton(android.R.string.cancel, null);

            if (!viewModel.isSmsVerified() && !viewModel.getSms().isEmpty()) {
                alertDialogSmsBuilder.setNeutralButton(R.string.verify, (dialogInterface, i) -> {
                    viewModel.updateSms(viewModel.getSms());
                });
            }

            AlertDialog dialog = alertDialogSmsBuilder.create();
            dialog.setOnShowListener(dialogInterface -> {
                Button positive = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                positive.setOnClickListener(view -> {
                    String sms = countryTextView.getText() + mobileNumber.getText().toString();

                    if (!FormatsUtil.getInstance().isValidMobileNumber(sms)) {
                        ToastCustom.makeText(getActivity(), getString(R.string.invalid_mobile), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                    } else {
                        viewModel.updateSms(sms);
                        dialog.dismiss();
                    }
                });
            });

            dialog.show();
        }
    }

    private void showDialogGuid() {
        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.guid_to_clipboard)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, (dialog, whichButton) -> {
                    ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = null;
                    clip = ClipData.newPlainText("guid", guidPref.getSummary());
                    clipboard.setPrimaryClip(clip);
                    ToastCustom.makeText(getActivity(), getString(R.string.copied_to_clipboard), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void showDialogBTCUnits() {
        CharSequence[] units = viewModel.getBtcUnits();
        int sel = viewModel.getBtcUnitsPosition();

        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.select_units)
                .setSingleChoiceItems(units, sel, (dialog, which) -> {
                    viewModel.updatePreferences(PrefsUtil.KEY_BTC_UNITS, which);
                    dialog.dismiss();
                })
                .show();
    }

    private void showDialogFiatUnits() {
        String[] currencies = ExchangeRateFactory.getInstance().getCurrencyLabels();
        String strCurrency = viewModel.getFiatUnits();
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
                    viewModel.updatePreferences(PrefsUtil.KEY_SELECTED_FIAT, currencies[which].substring(currencies[which].length() - 3));
                    dialog.dismiss();
                })
                .show();
    }

    @Override
    public void showDialogVerifySms() {
        AppCompatEditText editText = new AppCompatEditText(getActivity());
        editText.setSingleLine(true);

        AlertDialog dialog = new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.verify_mobile)
                .setMessage(R.string.verify_sms_summary)
                .setView(ViewUtils.getAlertDialogEditTextLayout(getActivity(), editText))
                .setCancelable(false)
                .setPositiveButton(R.string.verify, null)
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.resend, (dialogInterface, i) -> viewModel.updateSms(viewModel.getSms()))
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positive = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            positive.setOnClickListener(view -> {
                String codeS = editText.getText().toString();
                if (codeS.length() > 0) {
                    viewModel.verifySms(codeS);
                    dialog.dismiss();
                    ViewUtils.hideKeyboard(getActivity());
                }
            });
        });

        dialog.show();
    }

    private void showDialogPasswordHint() {
        AppCompatEditText editText = new AppCompatEditText(getActivity());
        editText.setText(viewModel.getPasswordHint());
        editText.setSelection(viewModel.getPasswordHint().length());
        editText.setSingleLine(true);
        editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.password_hint)
                .setMessage(R.string.password_hint_summary)
                .setView(ViewUtils.getAlertDialogEditTextLayout(getActivity(), editText))
                .setCancelable(false)
                .setPositiveButton(R.string.update, (dialogInterface, i) -> {
                    String hint = editText.getText().toString();
                    if (!hint.equals(viewModel.getTempPassword().toString())) {
                        viewModel.updatePasswordHint(hint);
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
            viewModel.pinCodeValidatedForChange();
        } else if (requestCode == REQUEST_CODE_VALIDATE_PIN_FOR_FINGERPRINT && resultCode == RESULT_OK) {
            viewModel.pinCodeValidatedForFingerprint(new CharSequenceX(data.getStringExtra(KEY_VALIDATED_PIN)));
        }
    }

    private void showDialogEmailNotifications() {
        AlertDialog dialog = new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.email_notifications)
                .setMessage(R.string.email_notifications_summary)
                .setPositiveButton(R.string.enable, (dialogInterface, i) ->
                        viewModel.updateNotification(Settings.NOTIFICATION_TYPE_EMAIL, true))
                .setNegativeButton(R.string.disable, (dialogInterface, i) ->
                        viewModel.updateNotification(Settings.NOTIFICATION_TYPE_EMAIL, false))
                .create();

        dialog.setOnCancelListener(dialogInterface -> emailNotificationPref.setChecked(!emailNotificationPref.isChecked()));
        dialog.show();
    }

    private void showDialogSmsNotifications() {
        AlertDialog dialog = new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.sms_notifications)
                .setMessage(R.string.sms_notifications_summary)
                .setPositiveButton(R.string.enable, (dialogInterface, i) ->
                        viewModel.updateNotification(Settings.NOTIFICATION_TYPE_SMS, true))
                .setNegativeButton(R.string.disable, (dialogInterface, i) ->
                        viewModel.updateNotification(Settings.NOTIFICATION_TYPE_SMS, false))
                .create();

        dialog.setOnCancelListener(dialogInterface -> smsNotificationPref.setChecked(!smsNotificationPref.isChecked()));
        dialog.show();
    }

    private void showDialogChangePasswordWarning() {
        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.warning)
                .setMessage(R.string.change_password_summary)
                .setPositiveButton(R.string.dialog_continue, (dialog, which) -> showDialogChangePassword())
                .show();
    }

    private void showDialogChangePassword() {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        LinearLayout pwLayout = (LinearLayout) inflater.inflate(R.layout.modal_change_password2, null);

        AppCompatEditText currentPassword = (AppCompatEditText) pwLayout.findViewById(R.id.current_password);
        AppCompatEditText newPassword = (AppCompatEditText) pwLayout.findViewById(R.id.new_password);
        AppCompatEditText newPasswordConfirmation = (AppCompatEditText) pwLayout.findViewById(R.id.confirm_password);

        LinearLayout entropyMeter = (LinearLayout) pwLayout.findViewById(R.id.entropy_meter);
        ProgressBar passStrengthBar = (ProgressBar) pwLayout.findViewById(R.id.pass_strength_bar);
        passStrengthBar.setMax(100);
        TextView passStrengthVerdict = (TextView) pwLayout.findViewById(R.id.pass_strength_verdict);

        newPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(final Editable editable) {
                newPassword.postDelayed(() -> {
                    if (getActivity() != null && !getActivity().isFinishing()) {
                        entropyMeter.setVisibility(View.VISIBLE);
                        setPasswordStrength(passStrengthVerdict, passStrengthBar, editable.toString());
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

                String currentPw = currentPassword.getText().toString();
                String newPw = newPassword.getText().toString();
                String newConfirmedPw = newPasswordConfirmation.getText().toString();
                CharSequenceX walletPassword = viewModel.getTempPassword();

                if (!currentPw.equals(newPw)) {
                    if (currentPw.equals(walletPassword.toString())) {
                        if (newPw.equals(newConfirmedPw)) {
                            if (newConfirmedPw.length() < 4 || newConfirmedPw.length() > 255) {
                                ToastCustom.makeText(getActivity(), getString(R.string.invalid_password), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                            } else if (newConfirmedPw.equals(viewModel.getPasswordHint())) {
                                ToastCustom.makeText(getActivity(), getString(R.string.hint_reveals_password_error), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                            } else if (pwStrength < 50) {
                                new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                                        .setTitle(R.string.app_name)
                                        .setMessage(R.string.weak_password)
                                        .setCancelable(false)
                                        .setPositiveButton(R.string.yes, (dialog1, which) -> {
                                            newPasswordConfirmation.setText("");
                                            newPasswordConfirmation.requestFocus();
                                            newPassword.setText("");
                                            newPassword.requestFocus();
                                        })
                                        .setNegativeButton(R.string.polite_no, (dialog1, which) -> {
                                            alertDialog.dismiss();
                                            viewModel.updatePassword(new CharSequenceX(newConfirmedPw), walletPassword);
                                        })
                                        .show();
                            } else {
                                alertDialog.dismiss();
                                viewModel.updatePassword(new CharSequenceX(newConfirmedPw), walletPassword);
                            }
                        } else {
                            newPasswordConfirmation.setText("");
                            newPasswordConfirmation.requestFocus();
                            ToastCustom.makeText(getActivity(), getString(R.string.password_mismatch_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                        }
                    } else {
                        currentPassword.setText("");
                        currentPassword.requestFocus();
                        ToastCustom.makeText(getActivity(), getString(R.string.invalid_password), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                    }
                } else {
                    newPassword.setText("");
                    newPasswordConfirmation.setText("");
                    newPassword.requestFocus();
                    ToastCustom.makeText(getActivity(), getString(R.string.change_password_new_matches_current), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                }
            });
        });
        alertDialog.show();
    }

    private void showDialogTwoFA() {
        if (!viewModel.isSmsVerified()) {
            twoStepVerificationPref.setChecked(false);
            showDialogMobile();
        } else {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                    .setTitle(R.string.two_fa)
                    .setMessage(R.string.two_fa_summary)
                    .setNeutralButton(android.R.string.cancel, (dialogInterface, i) ->
                            twoStepVerificationPref.setChecked(viewModel.getAuthType() != Settings.AUTH_TYPE_OFF));

            if (viewModel.getAuthType() != Settings.AUTH_TYPE_OFF) {
                alertDialogBuilder.setNegativeButton(R.string.disable, (dialogInterface, i) ->
                        viewModel.updateTwoFa(Settings.AUTH_TYPE_OFF));
            } else {
                alertDialogBuilder.setPositiveButton(R.string.enable, (dialogInterface, i) ->
                        viewModel.updateTwoFa(Settings.AUTH_TYPE_SMS));
            }
            alertDialogBuilder
                    .create()
                    .show();
        }
    }

    @Thunk
    void setPasswordStrength(TextView passStrengthVerdict, ProgressBar passStrengthBar, String pw) {
        if (getActivity() != null && !getActivity().isFinishing()) {
            int[] strengthVerdicts = {R.string.strength_weak, R.string.strength_medium, R.string.strength_normal, R.string.strength_strong};
            int[] strengthColors = {R.drawable.progress_red, R.drawable.progress_orange, R.drawable.progress_blue, R.drawable.progress_green};
            pwStrength = (int) Math.round(PasswordUtil.getInstance().getStrength(pw));

            if (pw.equals(viewModel.getEmail())) pwStrength = 0;

            // red
            int pwStrengthLevel = 0;

            if (pwStrength >= 75) {
                // green
                pwStrengthLevel = 3;
            } else if (pwStrength >= 50) {
                // green
                pwStrengthLevel = 2;
            } else if (pwStrength >= 25) {
                // orange
                pwStrengthLevel = 1;
            }

            passStrengthBar.setProgress(pwStrength);
            passStrengthBar.setProgressDrawable(ContextCompat.getDrawable(getActivity(), strengthColors[pwStrengthLevel]));
            passStrengthVerdict.setText(getResources().getString(strengthVerdicts[pwStrengthLevel]));
        }
    }

    private void setCountryFlag(TextView tvCountry, String dialCode, int flagResourceId) {
        tvCountry.setText(dialCode);
        Drawable drawable = ContextCompat.getDrawable(getActivity(), flagResourceId);
        drawable.setAlpha(30);

        int sdk = android.os.Build.VERSION.SDK_INT;
        if (sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            //noinspection deprecation
            tvCountry.setBackgroundDrawable(drawable);
        } else {
            tvCountry.setBackground(drawable);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        hideProgressDialog();
        viewModel.destroy();
    }
}
