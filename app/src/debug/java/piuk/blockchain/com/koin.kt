package piuk.blockchain.com

import com.blockchain.featureflags.InternalFeatureFlagApi
import org.koin.dsl.bind
import org.koin.dsl.module

val internalFeatureFlagsModule = module {
    single {
        InternalFeatureFlagDebugApiImpl(
            prefs = get()
        )
    }.bind(InternalFeatureFlagApi::class)
}
