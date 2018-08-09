package piuk.blockchain.android.injection;

import com.blockchain.koin.KoinDaggerModule;
import dagger.Module;
import dagger.Provides;
import piuk.blockchain.androidcore.data.settings.datastore.SettingsDataStore;

@Module
public class PersistentStoreModule extends KoinDaggerModule {

    @Provides
    SettingsDataStore provideSettingsDataStore() {
        return get(SettingsDataStore.class);
    }
}
