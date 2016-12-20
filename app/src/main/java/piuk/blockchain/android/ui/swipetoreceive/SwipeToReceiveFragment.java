package piuk.blockchain.android.ui.swipetoreceive;


import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.websocket.WebSocketService;
import piuk.blockchain.android.databinding.FragmentSwipeToReceiveBinding;
import piuk.blockchain.android.ui.balance.BalanceFragment;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.OSUtil;
import piuk.blockchain.android.util.annotations.Thunk;

public class SwipeToReceiveFragment extends Fragment implements SwipeToReceiveViewModel.DataListener {

    private FragmentSwipeToReceiveBinding binding;
    @Thunk SwipeToReceiveViewModel viewModel;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (intent.getAction().equals(BalanceFragment.ACTION_INTENT)) {
                if (viewModel != null) {
                    // Update UI with new Address + QR
                    viewModel.onViewReady();
                }
            }
        }
    };

    public SwipeToReceiveFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Listen for updates to stored addresses
        startWebSocketService();

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_swipe_to_receive, container, false);

        viewModel = new SwipeToReceiveViewModel(this);

        binding.qrCode.setOnClickListener(view -> showClipboardWarning());

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel.onViewReady();
    }

    @Override
    public void displayLoading() {
        binding.progressbar.setVisibility(View.VISIBLE);
        binding.qrCode.setVisibility(View.INVISIBLE);
    }

    @Override
    public void displayQrCode(Bitmap bitmap) {
        binding.progressbar.setVisibility(View.GONE);
        binding.qrCode.setVisibility(View.VISIBLE);
        binding.qrCode.setImageBitmap(bitmap);
    }

    @Override
    public void displayReceiveAddress(String address) {
        binding.receivingAddress.setText(address);

        // Register address as the one we're interested in via broadcast
        Intent intent = new Intent(WebSocketService.ACTION_INTENT);
        intent.putExtra("address", address);
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);

        // Listen for corresponding broadcasts
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(
                broadcastReceiver, new IntentFilter(BalanceFragment.ACTION_INTENT));
    }

    @Override
    public void displayReceiveAccount(String accountName) {
        binding.receivingAccount.setText(accountName);
    }

    @Override
    public void showNoAddressesAvailable() {
        binding.contentLayout.setVisibility(View.GONE);
        binding.errorLayout.setVisibility(View.VISIBLE);
    }

    private void showClipboardWarning() {
        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.receive_address_to_clipboard)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, (dialog, whichButton) -> {
                    ClipboardManager clipboard = (android.content.ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = android.content.ClipData.newPlainText("Send address", binding.receivingAddress.getText().toString());
                    ToastCustom.makeText(getActivity(), getString(R.string.copied_to_clipboard), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_GENERAL);
                    clipboard.setPrimaryClip(clip);
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        viewModel.destroy();
    }

    private void startWebSocketService() {
        Intent intent = new Intent(getContext(), WebSocketService.class);

        if (!new OSUtil(getActivity()).isServiceRunning(WebSocketService.class)) {
            getActivity().startService(intent);
        } else {
            // Restarting this here ensures re-subscription after app restart - the service may remain
            // running, but the subscription to the WebSocket won't be restarted unless onCreate called
            getActivity().stopService(intent);
            getActivity().startService(intent);
        }
    }
}
