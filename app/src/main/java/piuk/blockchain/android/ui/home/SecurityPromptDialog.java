package piuk.blockchain.android.ui.home;

import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import piuk.blockchain.android.R;

public class SecurityPromptDialog extends AppCompatDialogFragment {

    public static final String TAG = SecurityPromptDialog.class.getSimpleName();

    private static final String KEY_TITLE = "title";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_ICON = "icon";
    private static final String KEY_POSITIVE_BUTTON = "positive_button";
    private static final String KEY_SHOW_CHECKBOX = "show_checkbox";
    private static final String KEY_SHOW_NEGATIVE_BUTTON = "show_negative_button";

    private AppCompatImageView mIcon;
    private AppCompatTextView mTitle;
    private AppCompatTextView mMessage;
    private AppCompatButton mPositiveButton;
    private AppCompatButton mNegativeButton;
    private AppCompatCheckBox mCheckBox;

    private View.OnClickListener mPositiveButtonListener;
    private View.OnClickListener mNegativeButtonListener;

    public SecurityPromptDialog() {
        // Empty constructor
    }

    public static SecurityPromptDialog newInstance(@StringRes int title,
                                                   String message,
                                                   @DrawableRes int icon,
                                                   @StringRes int positiveButton) {

        Bundle args = new Bundle();
        args.putInt(KEY_TITLE, title);
        args.putString(KEY_MESSAGE, message);
        args.putInt(KEY_ICON, icon);
        args.putInt(KEY_POSITIVE_BUTTON, positiveButton);

        SecurityPromptDialog fragment = new SecurityPromptDialog();
        fragment.setArguments(args);
        return fragment;
    }

    public static SecurityPromptDialog newInstance(@StringRes int title,
                                                   String message,
                                                   @DrawableRes int icon,
                                                   @StringRes int positiveButton,
                                                   boolean showNegativeButton,
                                                   boolean showCheckbox) {

        Bundle args = new Bundle();
        args.putInt(KEY_TITLE, title);
        args.putString(KEY_MESSAGE, message);
        args.putInt(KEY_ICON, icon);
        args.putInt(KEY_POSITIVE_BUTTON, positiveButton);
        args.putBoolean(KEY_SHOW_NEGATIVE_BUTTON, showNegativeButton);
        args.putBoolean(KEY_SHOW_CHECKBOX, showCheckbox);

        SecurityPromptDialog fragment = new SecurityPromptDialog();
        fragment.setArguments(args);
        return fragment;
    }

    public void showDialog(FragmentManager manager) {
        show(manager, TAG);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_security_centre, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mIcon = (AppCompatImageView) view.findViewById(R.id.icon);
        mTitle = (AppCompatTextView) view.findViewById(R.id.title);
        mMessage = (AppCompatTextView) view.findViewById(R.id.message);
        mPositiveButton = (AppCompatButton) view.findViewById(R.id.button_positive);
        mNegativeButton = (AppCompatButton) view.findViewById(R.id.button_negative);
        mCheckBox = (AppCompatCheckBox) view.findViewById(R.id.checkbox);

        mPositiveButton.setOnClickListener(v -> {
            if (mPositiveButtonListener != null) mPositiveButtonListener.onClick(mPositiveButton);
        });

        mNegativeButton.setOnClickListener(v -> {
            if (mNegativeButtonListener != null) mNegativeButtonListener.onClick(mNegativeButton);
        });

        Bundle args = getArguments();
        if (args.containsKey(KEY_TITLE)) {
            setTitle(args.getInt(KEY_TITLE));
        }

        if (args.containsKey(KEY_MESSAGE)) {
            setMessage(args.getString(KEY_MESSAGE));
        }

        if (args.containsKey(KEY_ICON)) {
            setIcon(args.getInt(KEY_ICON));
        }

        if (args.containsKey(KEY_POSITIVE_BUTTON)) {
            setPositiveButton(args.getInt(KEY_POSITIVE_BUTTON));
        }

        if (args.getBoolean(KEY_SHOW_NEGATIVE_BUTTON)) {
            showNegativeButton();
        }

        if (args.getBoolean(KEY_SHOW_CHECKBOX)) {
            showCheckBox();
        }
    }

    private void setTitle(@StringRes int titleStringId) {
        mTitle.setText(titleStringId);
    }

    private void setMessage(String message) {
        mMessage.setText(message);
    }

    private void setIcon(@DrawableRes int icon) {
        mIcon.setImageResource(icon);
    }

    private void setPositiveButton(@StringRes int textStringId) {
        mPositiveButton.setText(textStringId);
    }

    private void showCheckBox() {
        mCheckBox.setVisibility(View.VISIBLE);
    }

    private void showNegativeButton() {
        mNegativeButton.setVisibility(View.VISIBLE);
    }

    public void setPositiveButtonListener(View.OnClickListener listener) {
        mPositiveButtonListener = listener;
    }

    public void setNegativeButtonListener(View.OnClickListener listener) {
        mNegativeButtonListener = listener;
    }

    public boolean isChecked() {
        return mCheckBox.isChecked();
    }
}
