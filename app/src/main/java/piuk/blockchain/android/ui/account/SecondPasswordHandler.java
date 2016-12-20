package piuk.blockchain.android.ui.account;

import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.text.InputType;

import info.blockchain.wallet.payload.PayloadManager;

import piuk.blockchain.android.R;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.ViewUtils;

public class SecondPasswordHandler {

    private Context context;
    private PayloadManager payloadManager;

    public SecondPasswordHandler(Context context) {
        this.context = context;
        this.payloadManager = PayloadManager.getInstance();
    }

    public interface ResultListener {
        void onNoSecondPassword();

        void onSecondPasswordValidated(String validateSecondPassword);
    }

    public void validate(final ResultListener listener) {

        if (!payloadManager.getPayload().isDoubleEncrypted()) {
            listener.onNoSecondPassword();
        } else {

            final AppCompatEditText passwordField = new AppCompatEditText(context);
            passwordField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

            new AlertDialog.Builder(context, R.style.AlertDialogStyle)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.enter_double_encryption_pw)
                    .setView(ViewUtils.getAlertDialogEditTextLayout(context, passwordField))
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {

                        String secondPassword = passwordField.getText().toString();

                        // TODO: 18/11/2016 Make this asynchronous, validating the password can take
                        // a long time if the user has increased their PBKDF2 iterations
                        if (secondPassword.length() > 0 && payloadManager.validateSecondPassword(secondPassword)) {
                            listener.onSecondPasswordValidated(secondPassword);

                        } else {
                            ToastCustom.makeText(context, context.getString(R.string.double_encryption_password_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                        }
                    }).setNegativeButton(android.R.string.cancel, null).show();
        }
    }
}
