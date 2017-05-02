package piuk.blockchain.android.injection;

import dagger.Subcomponent;
import piuk.blockchain.android.data.websocket.WebSocketService;
import piuk.blockchain.android.ui.account.AccountEditViewModel;
import piuk.blockchain.android.ui.account.AccountViewModel;
import piuk.blockchain.android.ui.auth.PasswordRequiredViewModel;
import piuk.blockchain.android.ui.auth.PinEntryViewModel;
import piuk.blockchain.android.ui.backup.BackupVerifyViewModel;
import piuk.blockchain.android.ui.backup.BackupWalletViewModel;
import piuk.blockchain.android.ui.backup.ConfirmFundsTransferViewModel;
import piuk.blockchain.android.ui.balance.BalanceViewModel;
import piuk.blockchain.android.ui.buy.BuyViewModel;
import piuk.blockchain.android.ui.chooser.AccountChooserViewModel;
import piuk.blockchain.android.ui.contacts.detail.ContactDetailViewModel;
import piuk.blockchain.android.ui.contacts.list.ContactsListViewModel;
import piuk.blockchain.android.ui.contacts.pairing.ContactPairingMethodViewModel;
import piuk.blockchain.android.ui.contacts.pairing.ContactsInvitationBuilderViewModel;
import piuk.blockchain.android.ui.contacts.pairing.ContactsQrViewModel;
import piuk.blockchain.android.ui.contacts.payments.ContactPaymentDialogViewModel;
import piuk.blockchain.android.ui.contacts.payments.ContactsPaymentRequestViewModel;
import piuk.blockchain.android.ui.fingerprint.FingerprintDialogViewModel;
import piuk.blockchain.android.ui.home.MainViewModel;
import piuk.blockchain.android.ui.launcher.LauncherViewModel;
import piuk.blockchain.android.ui.onboarding.OnboardingViewModel;
import piuk.blockchain.android.ui.pairing.ManualPairingViewModel;
import piuk.blockchain.android.ui.pairing.PairingViewModel;
import piuk.blockchain.android.ui.receive.ReceiveQrViewModel;
import piuk.blockchain.android.ui.receive.ReceiveViewModel;
import piuk.blockchain.android.ui.recover.RecoverFundsViewModel;
import piuk.blockchain.android.ui.send.SendViewModel;
import piuk.blockchain.android.ui.settings.SettingsViewModel;
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveViewModel;
import piuk.blockchain.android.ui.transactions.TransactionDetailViewModel;
import piuk.blockchain.android.ui.upgrade.UpgradeWalletViewModel;

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

    void inject(ContactsListViewModel contactsListViewModel);

    void inject(ContactPairingMethodViewModel contactPairingMethodViewModel);

    void inject(SwipeToReceiveViewModel swipeToReceiveViewModel);

    void inject(ContactsInvitationBuilderViewModel contactsInvitationBuilderViewModel);

    void inject(ContactsQrViewModel contactsQrViewModel);

    void inject(ContactDetailViewModel contactDetailViewModel);

    void inject(ContactsPaymentRequestViewModel contactsPaymentRequestViewModel);

    void inject(AccountChooserViewModel accountChooserViewModel);

    void inject(BackupWalletViewModel backupWalletViewModel);

    void inject(WebSocketService webSocketService);

    void inject(BackupVerifyViewModel backupVerifyViewModel);

    void inject(ContactPaymentDialogViewModel contactPaymentDialogViewModel);

    void inject(OnboardingViewModel onboardingViewModel);

    void inject(UpgradeWalletViewModel upgradeWalletViewModel);

    void inject(BuyViewModel buyViewModel);
}
