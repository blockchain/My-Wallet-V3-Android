package piuk.blockchain.android.injection;

import dagger.Subcomponent;
import piuk.blockchain.android.ui.account.AccountEditViewModel;
import piuk.blockchain.android.ui.account.AccountViewModel;
import piuk.blockchain.android.ui.auth.PasswordRequiredViewModel;
import piuk.blockchain.android.ui.auth.PinEntryViewModel;
import piuk.blockchain.android.ui.backup.ConfirmFundsTransferViewModel;
import piuk.blockchain.android.ui.balance.BalanceViewModel;
import piuk.blockchain.android.ui.fingerprint.FingerprintDialogViewModel;
import piuk.blockchain.android.ui.home.MainViewModel;
import piuk.blockchain.android.ui.launcher.LauncherViewModel;
import piuk.blockchain.android.ui.pairing.ManualPairingViewModel;
import piuk.blockchain.android.ui.pairing.PairingViewModel;
import piuk.blockchain.android.ui.receive.ReceiveQrViewModel;
import piuk.blockchain.android.ui.receive.ReceiveViewModel;
import piuk.blockchain.android.ui.recover.RecoverFundsViewModel;
import piuk.blockchain.android.ui.send.SendViewModel;
import piuk.blockchain.android.ui.settings.SettingsViewModel;
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveViewModel;
import piuk.blockchain.android.ui.transactions.TransactionDetailViewModel;

/**
 * Subcomponents have access to all upstream objects in the graph but can have their own scope -
 * they don't need to explicitly state their dependencies as they have access anyway
 */
@SuppressWarnings("WeakerAccess")
@ViewModelScope
@Subcomponent(modules = DataManagerModule.class)
public interface DataManagerComponent {

    void inject(LauncherViewModel launcherViewModel);

    void inject(PasswordRequiredViewModel passwordRequiredViewModel);

    void inject(ManualPairingViewModel manualPairingViewModel);

    void inject(SendViewModel sendViewModel);

    void inject(PinEntryViewModel pinEntryViewModel);

    void inject(MainViewModel mainViewModel);

    void inject(BalanceViewModel balanceViewModel);

    void inject(PairingViewModel pairingViewModel);

    void inject(AccountEditViewModel accountEditViewModel);

    void inject(RecoverFundsViewModel recoverFundsViewModel);

    void inject(ReceiveViewModel receiveViewModel);

    void inject(TransactionDetailViewModel transactionDetailViewModel);

    void inject(ConfirmFundsTransferViewModel confirmFundsTransferViewModel);

    void inject(AccountViewModel accountViewModel);

    void inject(SettingsViewModel settingsViewModel);

    void inject(FingerprintDialogViewModel fingerprintDialogViewModel);

    void inject(ReceiveQrViewModel receiveQrViewModel);

    void inject(SwipeToReceiveViewModel swipeToReceiveViewModel);
}
