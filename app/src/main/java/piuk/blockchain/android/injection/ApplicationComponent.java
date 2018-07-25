package piuk.blockchain.android.injection;

import info.blockchain.wallet.util.PrivateKeyFactory;

import javax.inject.Singleton;

import dagger.Component;
import piuk.blockchain.android.BlockchainApplication;
import piuk.blockchain.android.data.notifications.FcmCallbackService;
import piuk.blockchain.android.data.notifications.InstanceIdService;
import piuk.blockchain.android.util.exceptions.LoggingExceptionHandler;
import piuk.blockchain.androidcore.data.contacts.ContactsDataManager;
import piuk.blockchain.androidcoreui.injector.ContextModule;

/**
 * Created by adambennett on 08/08/2016.
 */

@SuppressWarnings("WeakerAccess")
@Singleton
@Component(modules = {
        ApplicationModule.class,
        ApiModule.class,
        PersistentStoreModule.class,
        ServiceModule.class,
        ContextModule.class,
        KycModule.class,
})
public interface ApplicationComponent {

    // Subcomponent with its own scope (technically unscoped now that we're not deliberately
    // destroying a module between pages)
    PresenterComponent presenterComponent();

    void inject(LoggingExceptionHandler loggingExceptionHandler);

    void inject(PrivateKeyFactory privateKeyFactory);

    void inject(InstanceIdService instanceIdService);

    void inject(BlockchainApplication blockchainApplication);

    void inject(ContactsDataManager contactsDataManager);

    void inject(FcmCallbackService fcmCallbackService);
}
