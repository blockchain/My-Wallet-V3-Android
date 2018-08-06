package piuk.blockchain.android.injection;

import android.content.Context;

import com.blockchain.injection.KycComponent;
import piuk.blockchain.androidcoreui.injector.ContextModule;

/**
 * Created by adambennett on 08/08/2016.
 */

public enum Injector {

    INSTANCE;

    private ApplicationComponent applicationComponent;
    private PresenterComponent presenterComponent;
    private KycComponent kycComponent;

    public static Injector getInstance() {
        return INSTANCE;
    }

    public void init(Context applicationContext) {
        ApplicationModule applicationModule = new ApplicationModule();
        ApiModule apiModule = new ApiModule();
        ContextModule contextModule = new ContextModule(applicationContext);
        KycModule kycModule = new KycModule();

        initAppComponent(applicationModule, apiModule, contextModule, kycModule);
    }

    protected void initAppComponent(ApplicationModule applicationModule,
                                    ApiModule apiModule,
                                    ContextModule contextModule,
                                    KycModule kycModule) {
        applicationComponent = DaggerApplicationComponent.builder()
                .contextModule(contextModule)
                .applicationModule(applicationModule)
                .apiModule(apiModule)
                .kycModule(kycModule)
                .build();

        getPresenterComponent();
    }

    public ApplicationComponent getAppComponent() {
        return applicationComponent;
    }

    public PresenterComponent getPresenterComponent() {
        if (presenterComponent == null) {
            presenterComponent = applicationComponent.presenterComponent();
        }
        return presenterComponent;
    }

    public KycComponent getKycComponent() {
        if (kycComponent == null) {
            kycComponent = applicationComponent.kycComponent();
        }
        return kycComponent;
    }

}
