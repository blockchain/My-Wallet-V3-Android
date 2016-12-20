package piuk.blockchain.android.ui.fingerprint;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import info.blockchain.wallet.util.CharSequenceX;

import piuk.blockchain.android.R;
import piuk.blockchain.android.util.annotations.Thunk;

public class FingerprintDialog extends AppCompatDialogFragment
        implements FingerprintDialogViewModel.DataListener {

    public static final String TAG = "FingerprintDialog";
    public static final String KEY_BUNDLE_PIN_CODE = "pin_code";
    public static final String KEY_BUNDLE_STAGE = "stage";
    private static final long ERROR_TIMEOUT_MILLIS = 1500;
    private static final long SUCCESS_DELAY_MILLIS = 600;
    private static final long FATAL_ERROR_TIMEOUT_MILLIS = 3500;

    @Thunk ImageView fingerprintIcon;
    @Thunk TextView statusTextView;
    private TextView descriptionTextView;
    private Button cancelButton;
    private FingerprintAuthCallback authCallback;
    private FingerprintDialogViewModel viewModel;

    public static FingerprintDialog newInstance(CharSequenceX pin, String stage) {
        Bundle args = new Bundle();
        args.putString(KEY_BUNDLE_PIN_CODE, pin.toString());
        args.putString(KEY_BUNDLE_STAGE, stage);
        FingerprintDialog fragment = new FingerprintDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (authCallback == null) {
            throw new RuntimeException("Auth Callback is null, have you passed in into the dialog via setAuthCallback?");
        }
        setStyle(AppCompatDialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getDialog().setTitle(getString(R.string.fingerprint_login_title));
        getDialog().setCancelable(false);
        getDialog().setCanceledOnTouchOutside(false);
        getDialog().setOnKeyListener(new BackButtonListener(authCallback));

        viewModel = new FingerprintDialogViewModel(this);

        View view = inflater.inflate(R.layout.dialog_fingerprint, container, false);

        descriptionTextView = (TextView) view.findViewById(R.id.fingerprint_description);
        statusTextView = (TextView) view.findViewById(R.id.fingerprint_status);
        cancelButton = (Button) view.findViewById(R.id.action_cancel);
        cancelButton.setOnClickListener(v -> authCallback.onCanceled());

        fingerprintIcon = (ImageView) view.findViewById(R.id.icon_fingerprint);

        viewModel.onViewReady();

        return view;
    }

    public void setAuthCallback(FingerprintAuthCallback authCallback) {
        this.authCallback = authCallback;
    }

    @Override
    public void setCancelButtonText(@StringRes int text) {
        cancelButton.setText(text);
    }

    @Override
    public void setDescriptionText(@StringRes int text) {
        descriptionTextView.setText(text);
    }

    @Override
    public void setStatusText(@StringRes int text) {
        statusTextView.setText(text);
    }

    @Override
    public void setStatusText(String text) {
        statusTextView.setText(text);
    }

    @Override
    public void setStatusTextColor(@ColorRes int color) {
        statusTextView.setTextColor(ContextCompat.getColor(getContext(), color));
    }

    @Override
    public void setIcon(@DrawableRes int drawable) {
        fingerprintIcon.setImageResource(drawable);
    }


    @Override
    public Bundle getBundle() {
        return getArguments();
    }

    @Override
    public void onAuthenticated(@Nullable CharSequenceX data) {
        statusTextView.removeCallbacks(resetErrorTextRunnable);
        fingerprintIcon.postDelayed(() -> authCallback.onAuthenticated(data), SUCCESS_DELAY_MILLIS);
    }

    @Override
    public void onRecoverableError() {
        showErrorAnimation(ERROR_TIMEOUT_MILLIS);
    }

    @Override
    public void onFatalError() {
        showErrorAnimation(FATAL_ERROR_TIMEOUT_MILLIS);
        fingerprintIcon.postDelayed(() -> authCallback.onCanceled(), FATAL_ERROR_TIMEOUT_MILLIS);
    }

    @Override
    public void onCanceled() {
        authCallback.onCanceled();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        viewModel.destroy();
        super.onDismiss(dialog);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // This is to fix a long-standing bug in the Android framework
    }

    private Runnable resetErrorTextRunnable = new Runnable() {
        @Override
        public void run() {
            if (getContext() != null) {
                statusTextView.setTextColor(ContextCompat.getColor(getContext(), R.color.light_grey_text));
                statusTextView.setText(getString(R.string.fingerprint_hint));
                fingerprintIcon.setImageResource(R.drawable.ic_fingerprint_logo);
            }
        }
    };

    private void showErrorAnimation(long timeout) {
        Animation shake = AnimationUtils.loadAnimation(getContext(), R.anim.fingerprint_failed_shake);
        fingerprintIcon.setAnimation(shake);
        fingerprintIcon.animate();
        statusTextView.removeCallbacks(resetErrorTextRunnable);
        statusTextView.postDelayed(resetErrorTextRunnable, timeout);
    }

    /**
     * Indicate which stage of the auth process the user is currently at
     */
    public static class Stage {
        public static final String REGISTER_FINGERPRINT = "register_fingerprint";
        public static final String AUTHENTICATE = "authenticate";
    }

    public interface FingerprintAuthCallback {

        void onAuthenticated(CharSequenceX data);

        void onCanceled();
    }

    private static class BackButtonListener implements DialogInterface.OnKeyListener {

        private FingerprintAuthCallback fingerprintAuthCallback;

        BackButtonListener(FingerprintAuthCallback fingerprintAuthCallback) {
            this.fingerprintAuthCallback = fingerprintAuthCallback;
        }

        @Override
        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
            if ((keyCode == KeyEvent.KEYCODE_BACK)) {
                if (event.getAction() != KeyEvent.ACTION_DOWN) {
                    return true;
                } else {
                    fingerprintAuthCallback.onCanceled();
                    return true;
                }
            } else {
                return false;
            }
        }
    }
}
