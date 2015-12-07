package info.blockchain.wallet;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.google.zxing.client.android.CaptureActivity;

import info.blockchain.wallet.pairing.PairingFactory;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.ToastCustom;
import piuk.blockchain.android.R;

public class PairOrCreateWalletActivity extends ActionBarActivity {

    public static final int PAIRING_QR = 2005;
    public static Fragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pair);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.blockchain_blue)));
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        findViewById(R.id.account_spinner).setVisibility(View.GONE);

        fragment = new CreateWalletFragment();
        if(getIntent().getIntExtra("starting_fragment", 1)==1)
            fragment = new PairWalletFragment();

        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                FragmentManager fragmentManager = getFragmentManager();

                if (fragmentManager.getBackStackEntryCount() > 0 )
                    fragmentManager.popBackStack();
                else
                {
                    Intent intent = new Intent(PairOrCreateWalletActivity.this, LandingActivity.class);
                    startActivity(intent);
                    finish();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {

        if(fragment != null && fragment instanceof ManualPairingFragment) {
            fragment = null;
            getFragmentManager().popBackStack();
        }
        else {
            AppUtil.getInstance(PairOrCreateWalletActivity.this).restartApp();
        }

    }

    public void setActionBarTitle(String title){
        getSupportActionBar().setTitle(title);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(resultCode == Activity.RESULT_OK && requestCode == PAIRING_QR)	{
            if(data != null && data.getStringExtra(CaptureActivity.SCAN_RESULT) != null)	{
                AppUtil.getInstance(this).clearCredentialsAndRestart();
                String strResult = data.getStringExtra(CaptureActivity.SCAN_RESULT);
                pairingThreadQR(strResult);
            }
        }
        else if(resultCode == Activity.RESULT_CANCELED && requestCode == PAIRING_QR)	{
            ;
        }
        else {
            ;
        }

    }

    private void pairingThreadQR(final String data) {

        final Handler handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                if(PairingFactory.getInstance(PairOrCreateWalletActivity.this).handleQRCode(data))	{
//                    ToastCustom.makeText(PairOrCreateWalletActivity.this, getString(R.string.pairing_success), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_OK);
                    Intent intent = new Intent(PairOrCreateWalletActivity.this, PinEntryActivity.class);
                    intent.putExtra(PairingFactory.KEY_EXTRA_IS_PAIRING, true);
                    startActivity(intent);
                }
                else	{
                    ToastCustom.makeText(PairOrCreateWalletActivity.this, getString(R.string.pairing_failed), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                    AppUtil.getInstance(PairOrCreateWalletActivity.this).clearCredentialsAndRestart();
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        ;
                    }
                });

                Looper.loop();
            }
        }).start();
    }
}
