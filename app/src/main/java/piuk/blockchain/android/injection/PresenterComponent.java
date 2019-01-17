package piuk.blockchain.android.injection;

import com.blockchain.morph.ui.detail.TradeDetailActivity;
import dagger.Subcomponent;
import org.jetbrains.annotations.NotNull;
import piuk.blockchain.android.data.websocket.WebSocketService;
import piuk.blockchain.android.ui.account.AccountActivity;
import piuk.blockchain.android.ui.account.AccountEditActivity;
import piuk.blockchain.android.ui.auth.LandingActivity;
import piuk.blockchain.android.ui.auth.LogoutActivity;
import piuk.blockchain.android.ui.auth.PasswordRequiredActivity;
import piuk.blockchain.android.ui.auth.PinEntryActivity;
import piuk.blockchain.android.ui.auth.PinEntryFragment;
import piuk.blockchain.android.ui.backup.completed.BackupWalletCompletedFragment;
import piuk.blockchain.android.ui.backup.start.BackupWalletStartingFragment;
import piuk.blockchain.android.ui.backup.transfer.ConfirmFundsTransferDialogFragment;
import piuk.blockchain.android.ui.backup.verify.BackupWalletVerifyFragment;
import piuk.blockchain.android.ui.backup.wordlist.BackupWalletWordListFragment;
import piuk.blockchain.android.ui.balance.BalanceFragment;
import piuk.blockchain.android.ui.buy.BuyActivity;
import piuk.blockchain.android.ui.buysell.coinify.signup.CoinifySignUpActivity;
import piuk.blockchain.android.ui.buysell.coinify.signup.identityinreview.CoinifyIdentityInReviewFragment;
import piuk.blockchain.android.ui.buysell.coinify.signup.invalidcountry.CoinifyInvalidCountryFragment;
import piuk.blockchain.android.ui.buysell.coinify.signup.selectcountry.CoinifySelectCountryFragment;
import piuk.blockchain.android.ui.buysell.coinify.signup.verifyemail.CoinifyVerifyEmailFragment;
import piuk.blockchain.android.ui.buysell.confirmation.buy.CoinifyBuyConfirmationActivity;
import piuk.blockchain.android.ui.buysell.confirmation.sell.CoinifySellConfirmationActivity;
import piuk.blockchain.android.ui.buysell.createorder.BuySellBuildOrderActivity;
import piuk.blockchain.android.ui.buysell.details.awaitingtransfer.CoinifyAwaitingBankTransferActivity;
import piuk.blockchain.android.ui.buysell.details.trade.CoinifyTransactionDetailActivity;
import piuk.blockchain.android.ui.buysell.launcher.BuySellLauncherActivity;
import piuk.blockchain.android.ui.buysell.overview.CoinifyOverviewActivity;
import piuk.blockchain.android.ui.buysell.payment.bank.accountoverview.BankAccountSelectionActivity;
import piuk.blockchain.android.ui.buysell.payment.bank.addaccount.AddBankAccountActivity;
import piuk.blockchain.android.ui.buysell.payment.bank.addaddress.AddAddressActivity;
import piuk.blockchain.android.ui.charts.ChartsActivity;
import piuk.blockchain.android.ui.charts.ChartsFragment;
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
import piuk.blockchain.android.ui.pairingcode.PairingCodeActivity;
import piuk.blockchain.android.ui.receive.ReceiveFragment;
import piuk.blockchain.android.ui.receive.ReceiveQrActivity;
import piuk.blockchain.android.ui.recover.RecoverFundsActivity;
import piuk.blockchain.android.ui.send.SendFragment;
import piuk.blockchain.android.ui.settings.SettingsFragment;
import piuk.blockchain.android.ui.ssl.SSLVerifyActivity;
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveFragment;
import piuk.blockchain.android.ui.transactions.TransactionDetailActivity;
import piuk.blockchain.android.ui.upgrade.UpgradeWalletActivity;
import piuk.blockchain.androidcore.injection.PresenterScope;

/**
 * Subcomponents have access to all upstream objects in the graph but can have their own scope -
 * they don't need to explicitly state their dependencies as they have access anyway
 */
@PresenterScope
@Subcomponent
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

    void inject(@NotNull OnboardingActivity onboardingActivity);

    void inject(@NotNull AccountActivity accountActivity);

    void inject(@NotNull SettingsFragment settingsFragment);

    void inject(@NotNull ReceiveQrActivity receiveQrActivity);

    void inject(@NotNull BuyActivity buyActivity);

    void inject(@NotNull PairingCodeActivity pairingCodeActivity);

    void inject(@NotNull LandingActivity landingActivity);

    void inject(@NotNull SendFragment sendFragment);

    void inject(@NotNull DashboardFragment dashboardFragment);

    void inject(@NotNull TradeDetailActivity tradeDetailActivity);

    void inject(@NotNull ChartsActivity chartsActivity);

    void inject(@NotNull ChartsFragment chartsFragment);

    void inject(@NotNull LogoutActivity logoutActivity);

    void inject(@NotNull PinEntryActivity pinEntryActivity);

    void inject(@NotNull SSLVerifyActivity sslVerifyActivity);

    void inject(@NotNull BuySellLauncherActivity buySellLauncherActivity);

    void inject(@NotNull CoinifySignUpActivity coinifySignUpActivity);

    void inject(@NotNull CoinifyVerifyEmailFragment verifyEmailFragment);

    void inject(@NotNull CoinifySelectCountryFragment selectCountryFragment);

    void inject(@NotNull CoinifyInvalidCountryFragment coinifyInvalidCountryFragment);

    void inject(@NotNull CoinifyOverviewActivity coinifyOverviewActivity);

    void inject(@NotNull CoinifyIdentityInReviewFragment coinifyIdentityInReviewFragment);

    void inject(@NotNull BuySellBuildOrderActivity buySellBuildOrderActivity);

    void inject(@NotNull CoinifyBuyConfirmationActivity coinifyBuyConfirmationActivity);

    void inject(@NotNull CoinifyTransactionDetailActivity coinifyTransactionDetailActivity);

    void inject(@NotNull AddBankAccountActivity addBankAccountActivity);

    void inject(@NotNull AddAddressActivity addAddressActivity);

    void inject(@NotNull CoinifyAwaitingBankTransferActivity coinifyAwaitingBankTransferActivity);

    void inject(@NotNull BankAccountSelectionActivity bankAccountSelectionActivity);

    void inject(@NotNull CoinifySellConfirmationActivity coinifySellConfirmationActivity);
}
