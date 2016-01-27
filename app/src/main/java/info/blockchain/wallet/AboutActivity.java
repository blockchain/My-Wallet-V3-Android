package info.blockchain.wallet;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import info.blockchain.wallet.util.AppUtil;

import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.R;

//import android.util.Log;

public class AboutActivity extends Activity {

    private TextView tvAbout = null;
    private TextView bRate = null;
    private TextView bDownload = null;
    private String strMerchantPackage = "info.blockchain.merchant";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.setContentView(R.layout.activity_about);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        tvAbout = (TextView) findViewById(R.id.about);
        tvAbout.setText(getString(R.string.about, BuildConfig.VERSION_NAME, "2015"));

        bRate = (TextView) findViewById(R.id.rate_us);
        bRate.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                String appPackageName = getPackageName();
                Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName));
                marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                startActivity(marketIntent);
            }
        });

        bDownload = (TextView) findViewById(R.id.free_wallet);
        if (hasWallet()) {
            bDownload.setVisibility(View.GONE);
        } else {
            bDownload.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
                    Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + strMerchantPackage));
                    startActivity(marketIntent);
                }
            });
        }

    }

    private boolean hasWallet() {
        PackageManager pm = this.getPackageManager();
        try {
            pm.getPackageInfo(strMerchantPackage, 0);
            return true;
        } catch (NameNotFoundException nnfe) {
            return false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppUtil.getInstance(this).stopLogoutTimer();
    }

    @Override
    protected void onPause() {
        AppUtil.getInstance(this).startLogoutTimer();
        super.onPause();
    }
}
