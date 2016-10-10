package piuk.blockchain.android.ui.settings;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.R;

public class AboutDialog extends AppCompatDialogFragment {

    private static final String strMerchantPackage = "info.blockchain.merchant";

    public AboutDialog() {
        // No-op
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_about, null);

        TextView about = (TextView) view.findViewById(R.id.about);
        TextView licenses = (TextView) view.findViewById(R.id.licenses);
        TextView rateUs = (TextView) view.findViewById(R.id.rate_us);
        TextView freeWallet = (TextView) view.findViewById(R.id.free_wallet);

        about.setText(getString(R.string.about, BuildConfig.VERSION_NAME, "2015"));

        rateUs.setOnClickListener(v -> {
            try {
                String appPackageName = getActivity().getPackageName();
                Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName));
                marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                startActivity(marketIntent);
            } catch (ActivityNotFoundException e) {
                Log.e(AboutDialog.class.getSimpleName(), "Google Play Store not found", e);
            }
        });

        licenses.setOnClickListener(v -> {
            View layout = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_licenses, null);
            WebView webView = (WebView) layout.findViewById(R.id.webview);
            webView.loadUrl(("file:///android_asset/licenses.html"));
                    new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                    .setView(layout)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        });

        if (hasWallet()) {
            freeWallet.setVisibility(View.GONE);
        } else {
            freeWallet.setOnClickListener(v -> {
                try {
                    Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + strMerchantPackage));
                    startActivity(marketIntent);
                } catch (ActivityNotFoundException e) {
                    Log.e(AboutDialog.class.getSimpleName(), "Google Play Store not found", e);
                }
            });
        }

        return view;
    }

    private boolean hasWallet() {
        PackageManager pm = getActivity().getPackageManager();
        try {
            pm.getPackageInfo(strMerchantPackage, 0);
            return true;
        } catch (NameNotFoundException nnfe) {
            return false;
        }
    }


}
