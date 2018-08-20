package piuk.blockchain.android.ui.contacts.detail;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;

import piuk.blockchain.android.R;
import piuk.blockchain.androidcoreui.ui.base.BaseAuthActivity;
import piuk.blockchain.android.ui.home.MainActivity;
import piuk.blockchain.android.ui.send.SendFragment;
import piuk.blockchain.android.ui.transactions.TransactionDetailActivity;

import static piuk.blockchain.android.ui.balance.BalanceFragment.KEY_TRANSACTION_HASH;
import static piuk.blockchain.android.ui.contacts.list.ContactsListActivity.KEY_BUNDLE_CONTACT_ID;
import static piuk.blockchain.android.ui.home.MainActivity.EXTRA_FCTX_ID;
import static piuk.blockchain.android.ui.home.MainActivity.EXTRA_MDID;
import static piuk.blockchain.android.ui.home.MainActivity.EXTRA_RECIPIENT_ID;
import static piuk.blockchain.android.ui.home.MainActivity.EXTRA_URI;

public class ContactDetailActivity extends BaseAuthActivity implements
        ContactDetailFragment.OnFragmentInteractionListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_contact_detail);

        setupToolbar(findViewById(R.id.toolbar_general), "");
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (getIntent() != null && getIntent().hasExtra(KEY_BUNDLE_CONTACT_ID)) {
            submitFragmentTransaction(
                    ContactDetailFragment.newInstance(
                            getIntent().getStringExtra(KEY_BUNDLE_CONTACT_ID)));
        } else {
            finish();
        }
    }

    private void submitFragmentTransaction(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
        transaction.replace(R.id.content_frame, fragment)
                .commit();
    }

    public Toolbar getToolbar() {
        return findViewById(R.id.toolbar_general);
    }

    public static void start(Context context, @NonNull Bundle extras) {
        Intent starter = new Intent(context, ContactDetailActivity.class);
        starter.putExtras(extras);
        context.startActivity(starter);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    public void onFinishPageCalled() {
        finish();
    }

    @Override
    public void onBackPressed() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragmentById = fragmentManager.findFragmentById(R.id.content_frame);
        if (fragmentById != null && fragmentById instanceof SendFragment) {
            submitFragmentTransaction(
                    ContactDetailFragment.newInstance(
                            getIntent().getStringExtra(KEY_BUNDLE_CONTACT_ID)));
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onPaymentInitiated(String uri, String recipientId, String mdid, String fctxId) {
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_URI, uri);
        bundle.putString(EXTRA_RECIPIENT_ID, recipientId);
        bundle.putString(EXTRA_MDID, mdid);
        bundle.putString(EXTRA_FCTX_ID, fctxId);
        MainActivity.start(this, bundle);
    }

    @Override
    public void onShowTransactionDetailCalled(String hash) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_TRANSACTION_HASH, hash);
        TransactionDetailActivity.start(this, bundle);
    }

}
