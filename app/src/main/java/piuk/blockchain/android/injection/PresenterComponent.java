package piuk.blockchain.android.injection;

import org.jetbrains.annotations.NotNull;

import dagger.Subcomponent;
import piuk.blockchain.android.data.websocket.WebSocketService;
import piuk.blockchain.android.ui.account.AccountActivity;
import piuk.blockchain.android.ui.account.AccountEditActivity;
import piuk.blockchain.android.ui.auth.LandingActivity;
import piuk.blockchain.android.ui.auth.PasswordRequiredActivity;
import piuk.blockchain.android.ui.auth.PinEntryFragment;
import piuk.blockchain.android.ui.backup.completed.BackupWalletCompletedFragment;
import piuk.blockchain.android.ui.backup.start.BackupWalletStartingFragment;
import piuk.blockchain.android.ui.backup.transfer.ConfirmFundsTransferDialogFragment;
import piuk.blockchain.android.ui.backup.verify.BackupWalletVerifyFragment;
import piuk.blockchain.android.ui.backup.wordlist.BackupWalletWordListFragment;
import piuk.blockchain.android.ui.balance.BalanceFragment;
import piuk.blockchain.android.ui.buy.BuyActivity;
import piuk.blockchain.android.ui.chooser.AccountChooserActivity;
import piuk.blockchain.android.ui.confirm.ConfirmPaymentDialog;
import piuk.blockchain.android.ui.contacts.detail.ContactDetailFragment;
import piuk.blockchain.android.ui.contacts.list.ContactsListActivity;
import piuk.blockchain.android.ui.contacts.payments.ContactConfirmRequestFragment;
import piuk.blockchain.android.ui.createwallet.CreateWalletActivity;
import piuk.blockchain.android.ui.dashboard.DashboardFragment;
import piuk.blockchain.android.ui.fingerprint.FingerprintDialog;
import piuk.blockchain.android.ui.home.MainActivity;
import piuk.blockchain.android.ui.launcher.LauncherActivity;
import piuk.blockchain.android.ui.login.LoginActivity;
import piuk.blockchain.android.ui.login.ManualPairingActivity;
import piuk.blockchain.android.ui.onboarding.OnboardingActivity;
import piuk.blockchain.android.ui.pairing_code.PairingCodeActivity;
import piuk.blockchain.android.ui.receive.ReceiveFragment;
import piuk.blockchain.android.ui.receive.ReceiveQrActivity;
import piuk.blockchain.android.ui.recover.RecoverFundsActivity;
import piuk.blockchain.android.ui.send.SendFragmentNew;
import piuk.blockchain.android.ui.settings.SettingsFragment;
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveFragment;
import piuk.blockchain.android.ui.transactions.TransactionDetailActivity;
import piuk.blockchain.android.ui.upgrade.UpgradeWalletActivity;

/**
 * Subcomponents have access to all upstream objects in the graph but can have their own scope -
 * they don't need to explicitly state their dependencies as they have access anyway
 */
@SuppressWarnings("NullableProblems")
@PresenterScope
@Subcomponent(modules = DataManagerModule.class)
public interface PresenterComponent {

    // Requires access to DataManagers
    void inject(WebSocketService webSocketService);

    // Activity/Fragment injection
    void inject(@NotNull LauncherActivity launcherActivity);

    void inject(@NotNull LoginActivity loginActivity);

    void inject(@NotNull SwipeToReceiveFragment swipeToReceiveFragment);

    void inject(@NotNull UpgradeWalletActivity upgradeWalletActivity);

    void inject(@NotNull BalanceFragment balanceFragment);

    void inject(@NotNull CreateWalletActivity createWalletActivity);

    void inject(@NotNull BackupWalletStartingFragment backupWalletStartingFragment);

    void inject(@NotNull BackupWalletWordListFragment backupWalletWordListFragment);

    void inject(@NotNull ConfirmPaymentDialog confirmPaymentDialog);

    void inject(@NotNull BackupWalletCompletedFragment backupWalletCompletedFragment);

    void inject(@NotNull FingerprintDialog fingerprintDialog);

    void inject(@NotNull BackupWalletVerifyFragment backupWalletVerifyFragment);

    void inject(@NotNull ConfirmFundsTransferDialogFragment confirmFundsTransferDialogFragment);

    void inject(@NotNull TransactionDetailActivity transactionDetailActivity);

    void inject(@NotNull PasswordRequiredActivity passwordRequiredActivity);

    void inject(@NotNull ManualPairingActivity manualPairingActivity);

    void inject(@NotNull MainActivity mainActivity);

    void inject(@NotNull PinEntryFragment pinEntryFragment);

    void inject(@NotNull AccountEditActivity accountEditActivity);

    void inject(@NotNull RecoverFundsActivity recoverFundsActivity);

    void inject(@NotNull ReceiveFragment receiveFragment);

    void inject(@NotNull ContactsListActivity contactsListActivity);

    void inject(@NotNull ContactDetailFragment contactDetailFragment);

    void inject(@NotNull ContactConfirmRequestFragment contactConfirmRequestFragment);

    void inject(@NotNull AccountChooserActivity accountChooserActivity);

    void inject(@NotNull OnboardingActivity onboardingActivity);

    void inject(@NotNull AccountActivity accountActivity);

    void inject(@NotNull SettingsFragment settingsFragment);

    void inject(@NotNull ReceiveQrActivity receiveQrActivity);

    void inject(@NotNull BuyActivity buyActivity);

    void inject(@NotNull PairingCodeActivity pairingCodeActivity);

    void inject(@NotNull LandingActivity landingActivity);

    void inject(@NotNull SendFragmentNew sendFragment);

    void inject(@NotNull DashboardFragment dashboardFragment);
}
