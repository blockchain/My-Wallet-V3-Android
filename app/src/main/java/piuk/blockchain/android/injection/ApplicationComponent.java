package piuk.blockchain.android.injection;

import javax.inject.Singleton;

import dagger.Component;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.datamanagers.AuthDataManager;
import piuk.blockchain.android.ui.account.AccountEditViewModel;
import piuk.blockchain.android.ui.account.AccountViewModel;
import piuk.blockchain.android.ui.auth.PasswordRequiredViewModel;
import piuk.blockchain.android.ui.auth.PinEntryViewModel;
import piuk.blockchain.android.ui.backup.ConfirmFundsTransferViewModel;
import piuk.blockchain.android.ui.balance.BalanceViewModel;
import piuk.blockchain.android.ui.home.MainViewModel;
import piuk.blockchain.android.ui.launcher.LauncherViewModel;
import piuk.blockchain.android.ui.pairing.ManualPairingViewModel;
import piuk.blockchain.android.ui.pairing.PairingViewModel;
import piuk.blockchain.android.ui.receive.ReceiveCurrencyHelper;
import piuk.blockchain.android.ui.receive.ReceiveViewModel;
import piuk.blockchain.android.ui.receive.WalletAccountHelper;
import piuk.blockchain.android.ui.recover.RecoverFundsViewModel;
import piuk.blockchain.android.ui.send.SendViewModel;
import piuk.blockchain.android.ui.transactions.TransactionDetailViewModel;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.ExchangeRateFactory;
import piuk.blockchain.android.util.exceptions.LoggingExceptionHandler;

/**
 * Created by adambennett on 08/08/2016.
 */

@Singleton
@Component(modules = {ApplicationModule.class, ApiModule.class, DataManagerModule.class})
public interface ApplicationComponent {

    void inject(AccessState accessState);

    void inject(LauncherViewModel launcherViewModel);

    void inject(PasswordRequiredViewModel passwordRequiredViewModel);

    void inject(AppUtil appUtil);

    void inject(LoggingExceptionHandler loggingExceptionHandler);

    void inject(ManualPairingViewModel manualPairingViewModel);

    void inject(AuthDataManager authDataManager);

    void inject(SendViewModel sendViewModel);

    void inject(PinEntryViewModel pinEntryViewModel);

    void inject(MainViewModel mainViewModel);

    void inject(BalanceViewModel balanceViewModel);

    void inject(PairingViewModel pairingViewModel);

    void inject(AccountEditViewModel accountEditViewModel);

    void inject(RecoverFundsViewModel recoverFundsViewModel);

    void inject(ReceiveViewModel receiveViewModel);

    void inject(ExchangeRateFactory exchangeRateFactory);

    void inject(ReceiveCurrencyHelper receiveCurrencyHelper);

    void inject(WalletAccountHelper walletAccountHelper);

    void inject(TransactionDetailViewModel transactionDetailViewModel);

    void inject(ConfirmFundsTransferViewModel confirmFundsTransferViewModel);

    void inject(AccountViewModel accountViewModel);
}
