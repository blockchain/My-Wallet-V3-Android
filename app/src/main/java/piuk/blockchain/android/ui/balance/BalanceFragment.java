package piuk.blockchain.android.ui.balance;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.TextView;

import com.getbase.floatingactionbutton.FloatingActionsMenu;

import info.blockchain.wallet.transaction.Tx;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.payload.PayloadBridge;
import piuk.blockchain.android.databinding.FragmentBalanceBinding;
import piuk.blockchain.android.ui.backup.BackupWalletActivity;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.home.MainActivity;
import piuk.blockchain.android.ui.home.SecurityPromptDialog;
import piuk.blockchain.android.ui.receive.ReceiveActivity;
import piuk.blockchain.android.ui.send.SendActivity;
import piuk.blockchain.android.ui.settings.SettingsActivity;
import piuk.blockchain.android.ui.settings.SettingsFragment;
import piuk.blockchain.android.ui.transactions.TransactionDetailActivity;
import piuk.blockchain.android.util.DateUtil;
import piuk.blockchain.android.util.ExchangeRateFactory;
import piuk.blockchain.android.util.ListUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.ViewUtils;
import piuk.blockchain.android.util.annotations.Thunk;

public class BalanceFragment extends Fragment implements BalanceViewModel.DataListener {

    private static final String TAG = "BalanceFragment";
    public static final String ACTION_INTENT = "info.blockchain.wallet.ui.BalanceFragment.REFRESH";
    public static final String KEY_SELECTED_ACCOUNT_POSITION = "selected_account_position";
    public static final String KEY_TRANSACTION_LIST_POSITION = "transaction_list_position";
    private static final int SHOW_BTC = 1;
    private static final int SHOW_FIAT = 2;
    private static int BALANCE_DISPLAY_STATE = SHOW_BTC;
    public int balanceBarHeight;
    private BalanceHeaderAdapter accountsAdapter;
    @Thunk Communicator comm;
    private double btc_fx = 319.13;//TODO remove hard coded when refactoring
    @Thunk boolean isBTC = true;
    // Accounts list
    @Thunk AppCompatSpinner accountSpinner;
    // Tx list
    @Thunk BalanceListAdapter transactionAdapter;
    private Activity context;
    private PrefsUtil prefsUtil;
    private DateUtil dateUtil;

    @Thunk FragmentBalanceBinding binding;
    @Thunk BalanceViewModel viewModel;
    @Thunk Toolbar toolbar;

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (ACTION_INTENT.equals(intent.getAction())) {
                binding.swipeContainer.setRefreshing(true);
                viewModel.updateAccountList();
                viewModel.updateBalanceAndTransactionList(intent, accountSpinner.getSelectedItemPosition(), isBTC);
                transactionAdapter.onTransactionsUpdated(viewModel.getTransactionList());
                binding.swipeContainer.setRefreshing(false);
                // Check backup status on receiving funds
                viewModel.onViewReady();
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = getActivity();
        prefsUtil = new PrefsUtil(context);
        dateUtil = new DateUtil(context);

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_balance, container, false);
        viewModel = new BalanceViewModel(context, this);
        binding.setViewModel(viewModel);

        setHasOptionsMenu(true);

        BALANCE_DISPLAY_STATE = prefsUtil.getValue(PrefsUtil.KEY_BALANCE_DISPLAY_STATE, SHOW_BTC);
        if (BALANCE_DISPLAY_STATE == SHOW_FIAT) {
            isBTC = false;
        }

        balanceBarHeight = (int) ViewUtils.convertDpToPixel(96, getActivity());

        setupViews();

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel.onViewReady();
    }

    private void setAccountSpinner() {
        toolbar = (Toolbar) context.findViewById(R.id.toolbar_general);
        ((AppCompatActivity) context).setSupportActionBar(toolbar);

        if (viewModel.getActiveAccountAndAddressList().size() > 1) {
            accountSpinner.setVisibility(View.VISIBLE);
        } else if (viewModel.getActiveAccountAndAddressList().size() > 0) {
            accountSpinner.setSelection(0);
            accountSpinner.setVisibility(View.GONE);
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser) {
            viewModel.updateBalanceAndTransactionList(null, accountSpinner.getSelectedItemPosition(), isBTC);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        comm.resetNavigationDrawer();

        IntentFilter filter = new IntentFilter(ACTION_INTENT);
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter);

        viewModel.startWebSocketService();
        viewModel.updateAccountList();
        viewModel.updateBalanceAndTransactionList(null, accountSpinner.getSelectedItemPosition(), isBTC);

        binding.rvTransactions.clearOnScrollListeners();
        binding.rvTransactions.addOnScrollListener(new CollapseActionbarScrollListener() {
            @Override
            public void onMoved(int distance) {
                setToolbarOffset(distance);
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver);
    }

    /**
     * Deprecated, but necessary to prevent casting issues on <API21
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        comm = (Communicator) activity;
    }

    @Override
    public void show2FaDialog() {
        SecurityPromptDialog securityPromptDialog = SecurityPromptDialog.newInstance(
                R.string.two_fa,
                getString(R.string.security_centre_two_fa_message),
                R.drawable.vector_mobile,
                R.string.enable,
                true,
                true
        );

        securityPromptDialog.setPositiveButtonListener(v -> {
            securityPromptDialog.dismiss();
            if (securityPromptDialog.isChecked()) {
                viewModel.neverPrompt2Fa();
            }
            Intent intent = new Intent(getActivity(), SettingsActivity.class);
            intent.putExtra(SettingsFragment.EXTRA_SHOW_TWO_FA_DIALOG, true);
            startActivity(intent);
        });

        securityPromptDialog.setNegativeButtonListener(v -> {
            securityPromptDialog.dismiss();
            if (securityPromptDialog.isChecked()) {
                viewModel.neverPrompt2Fa();
            }
        });

        if (getActivity() != null && !getActivity().isFinishing() && isAdded()) {
            securityPromptDialog.showDialog(((AppCompatActivity) getActivity()).getSupportFragmentManager());
        }
    }

    @Override
    public void showBackupPromptDialog(boolean showNeverAgain) {
        SecurityPromptDialog securityPromptDialog = SecurityPromptDialog.newInstance(
                R.string.security_centre_backup_title,
                getString(R.string.security_centre_backup_message),
                R.drawable.vector_padlock,
                R.string.security_centre_backup_positive_button,
                true,
                showNeverAgain
        );

        securityPromptDialog.setPositiveButtonListener(v -> {
            securityPromptDialog.dismiss();
            if (securityPromptDialog.isChecked()) {
                viewModel.neverPromptBackup();
            }
            Intent intent = new Intent(getActivity(), BackupWalletActivity.class);
            startActivity(intent);
        });

        securityPromptDialog.setNegativeButtonListener(v -> {
            securityPromptDialog.dismiss();
            if (securityPromptDialog.isChecked()) {
                viewModel.neverPromptBackup();
            }
        });

        if (getActivity() != null && !getActivity().isFinishing() && isAdded()) {
            securityPromptDialog.showDialog(((AppCompatActivity) getActivity()).getSupportFragmentManager());
        }
    }

    private void initFab() {

        //First icon when fab expands
        com.getbase.floatingactionbutton.FloatingActionButton actionA = new com.getbase.floatingactionbutton.FloatingActionButton(context);
        actionA.setColorNormal(ContextCompat.getColor(getActivity(), R.color.blockchain_send_red));
        actionA.setSize(com.getbase.floatingactionbutton.FloatingActionButton.SIZE_MINI);
        Drawable sendIcon = ContextCompat.getDrawable(getActivity(), R.drawable.icon_send);
        sendIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        actionA.setIconDrawable(sendIcon);
        actionA.setColorPressed(ContextCompat.getColor(getActivity(), R.color.blockchain_red_50));
        actionA.setTitle(getResources().getString(R.string.send_bitcoin));
        actionA.setOnClickListener(v -> sendClicked());

        //Second icon when fab expands
        com.getbase.floatingactionbutton.FloatingActionButton actionB = new com.getbase.floatingactionbutton.FloatingActionButton(context);
        actionB.setColorNormal(ContextCompat.getColor(getActivity(), R.color.blockchain_receive_green));
        actionB.setSize(com.getbase.floatingactionbutton.FloatingActionButton.SIZE_MINI);
        Drawable receiveIcon = ContextCompat.getDrawable(getActivity(), R.drawable.icon_receive);
        receiveIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        actionB.setIconDrawable(receiveIcon);
        actionB.setColorPressed(ContextCompat.getColor(getActivity(), R.color.blockchain_green_50));
        actionB.setTitle(getResources().getString(R.string.receive_bitcoin));
        actionB.setOnClickListener(v -> receiveClicked());

        //Add buttons to expanding fab
        binding.fab.addButton(actionA);
        binding.fab.addButton(actionB);

        binding.fab.setOnFloatingActionsMenuUpdateListener(new FloatingActionsMenu.OnFloatingActionsMenuUpdateListener() {
            @Override
            public void onMenuExpanded() {
                binding.balanceMainContentShadow.setVisibility(View.VISIBLE);
                comm.setNavigationDrawerToggleEnabled(false);
            }

            @Override
            public void onMenuCollapsed() {
                binding.fab.collapse();
                binding.balanceMainContentShadow.setVisibility(View.GONE);
                comm.setNavigationDrawerToggleEnabled(true);
            }
        });
    }

    /**
     * Only available for Dogfood/Debug build
     */
    private void initDebugFab() {

        if (BuildConfig.DOGFOOD || BuildConfig.DEBUG) {
            binding.fabDebug.setVisibility(View.VISIBLE);

            com.getbase.floatingactionbutton.FloatingActionButton actionA = new com.getbase.floatingactionbutton.FloatingActionButton(context);
            actionA.setColorNormal(ContextCompat.getColor(getActivity(), R.color.blockchain_receive_green));
            actionA.setSize(com.getbase.floatingactionbutton.FloatingActionButton.SIZE_MINI);
            Drawable debugIcon = ContextCompat.getDrawable(getActivity(), R.drawable.icon_news);
            debugIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
            actionA.setIconDrawable(debugIcon);
            actionA.setColorPressed(ContextCompat.getColor(getActivity(), R.color.blockchain_green_50));
            actionA.setTitle("Show Payload");
            actionA.setOnClickListener(v -> {
                AlertDialog dialog = null;
                try {
                    dialog = new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                            .setTitle("Payload")
                            .setMessage(new JSONObject(viewModel.getPayloadManager().getBciWallet().getPayload().getDecryptedPayload()).toString(4))
                            .show();
                } catch (JSONException e) {
                    Log.e(TAG, "initDebugFab: ", e);
                }
                TextView textView = (TextView) dialog.findViewById(android.R.id.message);
                textView.setTextSize(9);
            });

            com.getbase.floatingactionbutton.FloatingActionButton actionB = new com.getbase.floatingactionbutton.FloatingActionButton(context);
            actionB.setColorNormal(ContextCompat.getColor(getActivity(), R.color.blockchain_receive_green));
            actionB.setSize(com.getbase.floatingactionbutton.FloatingActionButton.SIZE_MINI);
            debugIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
            actionB.setIconDrawable(debugIcon);
            actionB.setColorPressed(ContextCompat.getColor(getActivity(), R.color.blockchain_green_50));
            actionB.setTitle("Show unparsed wallet data");
            actionB.setOnClickListener(v -> {
                AlertDialog dialog = null;
                try {
                    dialog = new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                            .setTitle("Unparsed wallet data")
                            .setMessage(new JSONObject(viewModel.getPayloadManager().getBciWallet().getUnparsedWalletData()).toString(4))
                            .show();
                } catch (JSONException e) {
                    Log.e(TAG, "initDebugFab: ", e);
                }
                TextView textView = (TextView) dialog.findViewById(android.R.id.message);
                textView.setTextSize(9);
            });

            com.getbase.floatingactionbutton.FloatingActionButton actionC = new com.getbase.floatingactionbutton.FloatingActionButton(context);
            actionC.setColorNormal(ContextCompat.getColor(getActivity(), R.color.blockchain_receive_green));
            actionC.setSize(com.getbase.floatingactionbutton.FloatingActionButton.SIZE_MINI);
            debugIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
            actionC.setIconDrawable(debugIcon);
            actionC.setColorPressed(ContextCompat.getColor(getActivity(), R.color.blockchain_green_50));
            actionC.setTitle("Show parsed wallet data");
            actionC.setOnClickListener(v -> {
                AlertDialog dialog = null;
                dialog = new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                        .setTitle("Parsed wallet data")
                        .setMessage(viewModel.getPayloadManager().getBciWallet().toString())
                        .show();
                TextView textView = (TextView) dialog.findViewById(android.R.id.message);
                textView.setTextSize(9);
            });

            binding.fabDebug.addButton(actionA);
            binding.fabDebug.addButton(actionB);
            binding.fabDebug.addButton(actionC);
        } else {
            binding.fabDebug.setVisibility(View.GONE);
        }
    }

    public boolean isFabExpanded() {
        return isAdded() && binding.fab != null && binding.fab.isExpanded();
    }

    public void collapseFab() {
        if (binding.fab != null) binding.fab.collapse();
    }

    private void sendClicked() {
        Intent intent = new Intent(getActivity(), SendActivity.class);
        intent.putExtra(KEY_SELECTED_ACCOUNT_POSITION, getSelectedAccountPosition());
        startActivity(intent);
        binding.fab.collapse();
    }

    private void receiveClicked() {
        Intent intent = new Intent(getActivity(), ReceiveActivity.class);
        intent.putExtra(KEY_SELECTED_ACCOUNT_POSITION, getSelectedAccountPosition());
        startActivity(intent);
        binding.fab.collapse();
    }

    /**
     * Position is offset to account for first item being "All Wallets". If returned result is -1,
     * {@link SendActivity} and {@link ReceiveActivity} can safely ignore and choose the defaults
     * instead.
     */
    private int getSelectedAccountPosition() {
        int position = accountSpinner.getSelectedItemPosition();
        if (position >= accountSpinner.getCount() - 1) {
            // End of list is imported addresses, ignore
            position = 0;
        }

        return position - 1;
    }

    private void setupViews() {
        initFab();
        initDebugFab();

        binding.noTransactionMessage.noTxMessage.setVisibility(View.GONE);

        binding.balance1.setOnTouchListener((v, event) -> {

            if (BALANCE_DISPLAY_STATE == SHOW_BTC) {
                BALANCE_DISPLAY_STATE = SHOW_FIAT;
                isBTC = false;
                viewModel.updateBalanceAndTransactionList(null, accountSpinner.getSelectedItemPosition(), isBTC);
            } else {
                BALANCE_DISPLAY_STATE = SHOW_BTC;
                isBTC = true;
                viewModel.updateBalanceAndTransactionList(null, accountSpinner.getSelectedItemPosition(), isBTC);
            }

            transactionAdapter.onViewFormatUpdated(isBTC);
            prefsUtil.setValue(PrefsUtil.KEY_BALANCE_DISPLAY_STATE, BALANCE_DISPLAY_STATE);

            return false;
        });

        accountSpinner = binding.accountsSpinner;
        viewModel.updateAccountList();
        accountsAdapter = new BalanceHeaderAdapter(context, R.layout.spinner_balance_header, viewModel.getActiveAccountAndAddressList());
        accountsAdapter.setDropDownViewResource(R.layout.item_balance_account_dropdown);
        accountSpinner.setAdapter(accountsAdapter);
        accountSpinner.setOnTouchListener((v, event) -> event.getAction() == MotionEvent.ACTION_UP && ((MainActivity) getActivity()).getDrawerOpen());
        accountSpinner.post(() -> accountSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                //Refresh balance header and tx list
                viewModel.updateBalanceAndTransactionList(null, accountSpinner.getSelectedItemPosition(), isBTC);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // No-op
            }
        }));

        transactionAdapter = new BalanceListAdapter(viewModel.getTransactionList(), prefsUtil, viewModel.getMonetaryUtil(), dateUtil, btc_fx, isBTC);
        transactionAdapter.setTxListClickListener(new BalanceListAdapter.TxListClickListener() {
            @Override
            public void onRowClicked(int position) {
                goToTransactionDetail(position);
            }

            @Override
            public void onValueClicked(boolean isBtc) {
                viewModel.updateBalanceAndTransactionList(null, accountSpinner.getSelectedItemPosition(), isBtc);
            }
        });
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        binding.rvTransactions.setHasFixedSize(true);
        binding.rvTransactions.setLayoutManager(layoutManager);
        binding.rvTransactions.setAdapter(transactionAdapter);

        // drawerTitle account now that wallet has been created
        if (prefsUtil.getValue(PrefsUtil.KEY_INITIAL_ACCOUNT_NAME, "").length() > 0) {
            viewModel.getPayloadManager().getPayload().getHdWallet().getAccounts().get(0).setLabel(prefsUtil.getValue(PrefsUtil.KEY_INITIAL_ACCOUNT_NAME, ""));
            prefsUtil.removeValue(PrefsUtil.KEY_INITIAL_ACCOUNT_NAME);
            PayloadBridge.getInstance().remoteSaveThread(new PayloadBridge.PayloadSaveListener() {
                @Override
                public void onSaveSuccess() {

                }

                @Override
                public void onSaveFail() {
                    ToastCustom.makeText(getActivity(), getActivity().getString(R.string.remote_save_ko), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                }
            });
            accountsAdapter.notifyDataSetChanged();
        }

        binding.balanceMainContentShadow.setOnClickListener(v -> binding.fab.collapse());

        binding.noTransactionMessage.noTxMessage.setOnClickListener(v -> {
            Animation bounce = AnimationUtils.loadAnimation(context, R.anim.jump);
            binding.fab.startAnimation(bounce);
        });

        binding.swipeContainer.setProgressViewEndTarget(false, (int) (getResources().getDisplayMetrics().density * (72 + 20)));
        binding.swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        binding.swipeContainer.setRefreshing(true);
                    }

                    @Override
                    protected Void doInBackground(Void... params) {
                        try {
                            viewModel.getPayloadManager().updateBalancesAndTransactions();
                        } catch (Exception e) {
                            Log.e(TAG, "doInBackground: ", e);
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);
                        viewModel.updateAccountList();
                        viewModel.updateBalanceAndTransactionList(null, accountSpinner.getSelectedItemPosition(), isBTC);
                        binding.swipeContainer.setRefreshing(false);
                    }

                }.execute();
            }
        });
        binding.swipeContainer.setColorSchemeResources(R.color.blockchain_receive_green,
                R.color.blockchain_blue,
                R.color.blockchain_send_red);
    }

    @Thunk
    void setToolbarOffset(int distance) {
        binding.balanceLayout.setTranslationY(-distance);
        if (distance > 1) {
            ViewUtils.setElevation(toolbar, ViewUtils.convertDpToPixel(5F, getActivity()));
        } else {
            ViewUtils.setElevation(toolbar, 0F);
        }
    }

    @Thunk
    void goToTransactionDetail(int position) {
        Intent intent = new Intent(getActivity(), TransactionDetailActivity.class);
        intent.putExtra(KEY_TRANSACTION_LIST_POSITION, position);
        startActivity(intent);
    }

    @Override
    public void onRefreshAccounts() {
        //TODO revise
        if (accountSpinner != null)
            setAccountSpinner();

        context.runOnUiThread(() -> {
            if (accountsAdapter != null) accountsAdapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onAccountSizeChange() {
        if (accountSpinner != null)
            accountSpinner.setSelection(0);
    }

    @Override
    public void onRefreshBalanceAndTransactions() {

        String strFiat = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        btc_fx = ExchangeRateFactory.getInstance().getLastPrice(strFiat);

        // Notify adapter of change, let DiffUtil work out what needs changing
        List<Tx> newTransactions = new ArrayList<>();
        ListUtil.addAllIfNotNull(newTransactions, viewModel.getTransactionList());
        transactionAdapter.onTransactionsUpdated(newTransactions);
        binding.rvTransactions.scrollToPosition(0);
        binding.balanceLayout.post(() -> setToolbarOffset(0));

        //Display help text to user if no transactionList on selected account/address
        if (viewModel.getTransactionList().size() > 0) {
            binding.rvTransactions.setVisibility(View.VISIBLE);
            binding.noTransactionMessage.noTxMessage.setVisibility(View.GONE);
        } else {
            binding.rvTransactions.setVisibility(View.GONE);
            binding.noTransactionMessage.noTxMessage.setVisibility(View.VISIBLE);
        }

        if (isAdded() && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            //Fix for padding bug related to Android 4.1
            float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 72, getResources().getDisplayMetrics());
            binding.balance1.setPadding((int) px, 0, 0, 0);
        }
    }

    public interface Communicator {

        void setNavigationDrawerToggleEnabled(boolean enabled);

        void resetNavigationDrawer();
    }

    abstract class CollapseActionbarScrollListener extends RecyclerView.OnScrollListener {

        private int mToolbarOffset = 0;

        CollapseActionbarScrollListener() {
            // Empty Constructor
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            if ((mToolbarOffset < balanceBarHeight && dy > 0) || (mToolbarOffset > 0 && dy < 0)) {
                mToolbarOffset += dy;
            }

            clipToolbarOffset();
            onMoved(mToolbarOffset);
        }

        private void clipToolbarOffset() {
            if (mToolbarOffset > balanceBarHeight) {
                mToolbarOffset = balanceBarHeight;
            } else if (mToolbarOffset < 0) {
                mToolbarOffset = 0;
            }
        }

        public abstract void onMoved(int distance);
    }
}