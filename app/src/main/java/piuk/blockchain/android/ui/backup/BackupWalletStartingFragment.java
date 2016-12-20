package piuk.blockchain.android.ui.backup;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import info.blockchain.wallet.payload.PayloadManager;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.FragmentBackupStartBinding;
import piuk.blockchain.android.ui.account.SecondPasswordHandler;
import piuk.blockchain.android.ui.auth.PinEntryActivity;
import piuk.blockchain.android.util.ViewUtils;
import piuk.blockchain.android.util.annotations.Thunk;

import static android.app.Activity.RESULT_OK;
import static piuk.blockchain.android.ui.auth.PinEntryFragment.KEY_VALIDATING_PIN_FOR_RESULT;
import static piuk.blockchain.android.ui.auth.PinEntryFragment.REQUEST_CODE_VALIDATE_PIN;

public class BackupWalletStartingFragment extends Fragment {

    public static final String TAG = BackupWalletStartingFragment.class.getSimpleName();

    private PayloadManager mPayloadManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentBackupStartBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_backup_start, container, false);

        ActionBar supportActionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && supportActionBar != null) {
            supportActionBar.setElevation(ViewUtils.convertDpToPixel(5F, getActivity()));
        }

        binding.backupWalletAction.setOnClickListener(v -> {

            mPayloadManager = PayloadManager.getInstance();

            // Wallet is double encrypted
            if (mPayloadManager.getPayload().isDoubleEncrypted()) {
                new SecondPasswordHandler(getActivity()).validate(new SecondPasswordHandler.ResultListener() {
                    @Override
                    public void onNoSecondPassword() {
                        // No-op
                    }

                    @Override
                    public void onSecondPasswordValidated(String validateSecondPassword) {
                        Fragment fragment = new BackupWalletWordListFragment();
                        Bundle args = new Bundle();
                        args.putString("second_password", validateSecondPassword);
                        fragment.setArguments(args);
                        loadFragment(fragment);
                    }
                });

            } else {
                Intent intent = new Intent(getActivity(), PinEntryActivity.class);
                intent.putExtra(KEY_VALIDATING_PIN_FOR_RESULT, true);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivityForResult(intent, REQUEST_CODE_VALIDATE_PIN);
            }
        });

        return binding.getRoot();
    }

    @Thunk
    void loadFragment(Fragment fragment) {
        getFragmentManager().beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_NONE)
                .replace(R.id.content_frame, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_VALIDATE_PIN && resultCode == RESULT_OK) {
            loadFragment(new BackupWalletWordListFragment());
        }
    }
}