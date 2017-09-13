package piuk.blockchain.android.ui.home;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;

import info.blockchain.wallet.api.WalletApi;
import info.blockchain.wallet.exceptions.InvalidCredentialsException;
import info.blockchain.wallet.payload.PayloadManager;

import java.math.BigInteger;
import java.util.Collections;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Observable;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.api.EnvironmentSettings;
import piuk.blockchain.android.data.auth.AuthService;
import piuk.blockchain.android.data.cache.DynamicFeeCache;
import piuk.blockchain.android.data.contacts.ContactsDataManager;
import piuk.blockchain.android.data.contacts.models.ContactsEvent;
import piuk.blockchain.android.data.datamanagers.FeeDataManager;
import piuk.blockchain.android.data.datamanagers.PromptManager;
import piuk.blockchain.android.data.ethereum.EthDataManager;
import piuk.blockchain.android.data.exchange.BuyDataManager;
import piuk.blockchain.android.data.notifications.models.NotificationPayload;
import piuk.blockchain.android.data.payload.PayloadDataManager;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.services.EventService;
import piuk.blockchain.android.data.settings.SettingsDataManager;
import piuk.blockchain.android.data.websocket.WebSocketService;
import piuk.blockchain.android.ui.base.BasePresenter;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.home.models.MetadataEvent;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.ExchangeRateFactory;
import piuk.blockchain.android.util.OSUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;
import timber.log.Timber;

public class MainPresenter extends BasePresenter<MainView> {

    private OSUtil osUtil;
    private Observable<NotificationPayload> notificationObservable;
    private PrefsUtil prefs;
    private AppUtil appUtil;
    private AccessState accessState;
    private PayloadManager payloadManager;
    private PayloadDataManager payloadDataManager;
    private ContactsDataManager contactsDataManager;
    private Context applicationContext;
    private StringUtils stringUtils;
    private SettingsDataManager settingsDataManager;
    private BuyDataManager buyDataManager;
    private DynamicFeeCache dynamicFeeCache;
    private ExchangeRateFactory exchangeRateFactory;
    private RxBus rxBus;
    private FeeDataManager feeDataManager;
    private EnvironmentSettings environmentSettings;
    private PromptManager promptManager;
    private EthDataManager ethDataManager;

    @Inject
    MainPresenter(PrefsUtil prefs,
                  AppUtil appUtil,
                  AccessState accessState,
                  PayloadManager payloadManager,
                  PayloadDataManager payloadDataManager,
                  ContactsDataManager contactsDataManager,
                  Context applicationContext,
                  StringUtils stringUtils,
                  SettingsDataManager settingsDataManager,
                  BuyDataManager buyDataManager,
                  DynamicFeeCache dynamicFeeCache,
                  ExchangeRateFactory exchangeRateFactory,
                  RxBus rxBus,
                  FeeDataManager feeDataManager,
                  EnvironmentSettings environmentSettings,
                  PromptManager promptManager,
                  EthDataManager ethDataManager) {

        this.prefs = prefs;
        this.appUtil = appUtil;
        this.accessState = accessState;
        this.payloadManager = payloadManager;
        this.payloadDataManager = payloadDataManager;
        this.contactsDataManager = contactsDataManager;
        this.applicationContext = applicationContext;
        this.stringUtils = stringUtils;
        this.settingsDataManager = settingsDataManager;
        this.buyDataManager = buyDataManager;
        this.dynamicFeeCache = dynamicFeeCache;
        this.exchangeRateFactory = exchangeRateFactory;
        this.rxBus = rxBus;
        this.feeDataManager = feeDataManager;
        this.environmentSettings = environmentSettings;
        this.promptManager = promptManager;
        this.ethDataManager = ethDataManager;
        osUtil = new OSUtil(applicationContext);
    }

    private void initPrompts(Context context) {
        settingsDataManager.getSettings()
                .flatMap(settings -> promptManager.getCustomPrompts(context, settings))
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .flatMap(Observable::fromIterable)
                .firstOrError()
                .subscribe(
                        getView()::showCustomPrompt,
                        Timber::e);
    }

    @Override
    public void onViewReady() {
        if (!accessState.isLoggedIn()) {
            // This should never happen, but handle the scenario anyway by starting the launcher
            // activity, which handles all login/auth/corruption scenarios itself
            getView().kickToLauncherPage();
        } else {

            startWebSocketService();
            logEvents();

            getView().showProgressDialog(R.string.please_wait);

            getCompositeDisposable().add(registerMetadataNodesCompletable()
                    .mergeWith(feesCompletable())
                    .andThen(ethWalletCompletable())
                    .doAfterTerminate(() -> {
                                getView().hideProgressDialog();

                                initPrompts(getView().getActivityContext());

                                rxBus.emitEvent(MetadataEvent.class, MetadataEvent.SETUP_COMPLETE);

                                if (!prefs.getValue(PrefsUtil.KEY_SCHEME_URL, "").isEmpty()) {
                                    String strUri = prefs.getValue(PrefsUtil.KEY_SCHEME_URL, "");
                                    prefs.removeValue(PrefsUtil.KEY_SCHEME_URL);
                                    getView().onScanInput(strUri);
                                }
                            }
                    )
                    .subscribe(() -> {
                        if (getView().isBuySellPermitted()) {
                            initBuyService();
                        } else {
                            getView().setBuySellEnabled(false);
                        }
                        initContactsService();
                    }, throwable -> {
                        //noinspection StatementWithEmptyBody
                        if (throwable instanceof InvalidCredentialsException) {
                            // Double encrypted and not previously set up, ignore error
                            getView().showSecondPasswordDialog();
                        } else {
                            getView().showMetadataNodeRegistrationFailure();
                        }
                    }));
        }
    }

    private Completable feesCompletable() {
        return feeDataManager.getBtcFeeOptions()
                .doOnNext(btcFeeOptions -> dynamicFeeCache.setBtcFeeOptions(btcFeeOptions))
                .flatMap(ignored -> feeDataManager.getEthFeeOptions())
                .doOnNext(ethFeeOptions -> dynamicFeeCache.setEthFeeOptions(ethFeeOptions))
                .compose(RxUtil.applySchedulersToObservable())
                .flatMapCompletable(feeOptions -> exchangeRateFactory.updateTickers());
    }

    void checkForMessages() {
        getCompositeDisposable().add(contactsDataManager.fetchContacts()
                .andThen(contactsDataManager.getContactList())
                .toList()
                .flatMapObservable(contacts -> {
                    if (!contacts.isEmpty()) {
                        return contactsDataManager.getMessages(true);
                    } else {
                        return Observable.just(Collections.emptyList());
                    }
                })
                .subscribe(messages -> {
                    // No-op
                }, Timber::e));
    }

    void unPair() {
        getView().clearAllDynamicShortcuts();
        payloadManager.wipe();
        accessState.logout(applicationContext);
        accessState.unpairWallet();
        appUtil.restartApp();
        accessState.setPIN(null);
        buyDataManager.wipe();
        ethDataManager.clearEthAccountDetails();
    }

    PayloadManager getPayloadManager() {
        return payloadManager;
    }

    private void subscribeToNotifications() {
        notificationObservable = rxBus.register(NotificationPayload.class);

        getCompositeDisposable().add(
                notificationObservable
                        .compose(RxUtil.applySchedulersToObservable())
                        .subscribe(
                                notificationPayload -> checkForMessages(),
                                Throwable::printStackTrace));
    }

    @Override
    public void onViewDestroyed() {
        super.onViewDestroyed();
        rxBus.unregister(NotificationPayload.class, notificationObservable);
        appUtil.deleteQR();
        dismissAnnouncementIfOnboardingCompleted();
    }

    void updateTicker() {
        getCompositeDisposable().add(
                exchangeRateFactory.updateTickers()
                        .subscribe(
                                () -> {
                                    // No-op
                                },
                                Throwable::printStackTrace));
    }

    private void startWebSocketService() {
        Intent intent = new Intent(applicationContext, WebSocketService.class);

        if (!osUtil.isServiceRunning(WebSocketService.class)) {
            applicationContext.startService(intent);
        } else {
            // Restarting this here ensures re-subscription after app restart - the service may remain
            // running, but the subscription to the WebSocket won't be restarted unless onCreate called
            applicationContext.stopService(intent);
            applicationContext.startService(intent);
        }
    }

    private void logEvents() {
        EventService handler = new EventService(prefs, new AuthService(new WalletApi()));
        handler.log2ndPwEvent(payloadManager.getPayload().isDoubleEncryption());
        handler.logBackupEvent(payloadManager.getPayload().getHdWallets().get(0).isMnemonicVerified());

        try {
            BigInteger importedAddressesBalance = payloadManager.getImportedAddressesBalance();
            if (importedAddressesBalance != null) {
                handler.logLegacyEvent(importedAddressesBalance.longValue() > 0L);
            }
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    String getCurrentServerUrl() {
        return environmentSettings.getExplorerUrl();
    }

    private Completable registerMetadataNodesCompletable() {
        return payloadDataManager.loadNodes()
                .doOnNext(aBoolean -> subscribeToNotifications())
                .flatMap(loaded -> {
                    if (loaded) {
                        return payloadDataManager.getMetadataNodeFactory();
                    } else {
                        if (!payloadManager.getPayload().isDoubleEncryption()) {
                            return payloadDataManager.generateNodes(null)
                                    .andThen(payloadDataManager.getMetadataNodeFactory());
                        } else {
                            throw new InvalidCredentialsException("Payload is double encrypted");
                        }
                    }
                })
                .flatMapCompletable(metadataNodeFactory -> contactsDataManager
                        .initContactsService(metadataNodeFactory.getMetadataNode(), metadataNodeFactory.getSharedMetadataNode()))
                .andThen(payloadDataManager.registerMdid()
                        .onErrorReturn(throwable -> {
                            Timber.e(throwable);
                            return ResponseBody.create(MediaType.parse("application/json"), "{}");
                        }))
                .flatMapCompletable(ignored -> contactsDataManager.publishXpub());
    }

    private void initContactsService() {
        String uri = null;
        boolean fromNotification = false;

        if (!prefs.getValue(PrefsUtil.KEY_METADATA_URI, "").isEmpty()) {
            uri = prefs.getValue(PrefsUtil.KEY_METADATA_URI, "");
            prefs.removeValue(PrefsUtil.KEY_METADATA_URI);
        }

        if (prefs.getValue(PrefsUtil.KEY_CONTACTS_NOTIFICATION, false)) {
            prefs.removeValue(PrefsUtil.KEY_CONTACTS_NOTIFICATION);
            fromNotification = true;
        }

        final String finalUri = uri;
        if (finalUri != null || fromNotification) {
            getView().showProgressDialog(R.string.please_wait);
        }

        rxBus.emitEvent(ContactsEvent.class, ContactsEvent.INIT);

        if (uri != null) {
            getView().onStartContactsActivity(uri);
        } else if (fromNotification) {
            getView().onStartContactsActivity(null);
        } else {
            checkForMessages();
        }
    }

    private void initBuyService() {
        getCompositeDisposable().add(
                buyDataManager.getCanBuy()
                        .subscribe(isEnabled -> {
                                    getView().setBuySellEnabled(isEnabled);
                                    if (isEnabled) {
                                        buyDataManager.watchPendingTrades()
                                                .compose(RxUtil.applySchedulersToObservable())
                                                .subscribe(getView()::onTradeCompleted, Throwable::printStackTrace);

                                        buyDataManager.getWebViewLoginDetails()
                                                .subscribe(getView()::setWebViewLoginDetails, Throwable::printStackTrace);
                                    }
                                },
                                Timber::e));
    }

    private void dismissAnnouncementIfOnboardingCompleted() {
        if (prefs.getValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, false)
                && prefs.getValue(PrefsUtil.KEY_LATEST_ANNOUNCEMENT_SEEN, false)) {
            prefs.setValue(PrefsUtil.KEY_LATEST_ANNOUNCEMENT_DISMISSED, true);
        }
    }

    /**
     * Creates or retrieves ethereum wallet.
     */
    private Completable ethWalletCompletable() {
        return ethDataManager.getEthereumWallet(stringUtils.getString(R.string.eth_default_account_label))
                .compose(RxUtil.applySchedulersToObservable())
                .flatMapCompletable(ethereumWallet -> Completable.complete());
    }

    void generateMetadataHDNodeAndEthereumWallet(@Nullable String secondPassword) {
        if (!payloadDataManager.validateSecondPassword(secondPassword)) {
            getView().showToast(R.string.invalid_password, ToastCustom.TYPE_ERROR);
            getView().showSecondPasswordDialog();
        } else {
            getCompositeDisposable().add(
                    payloadDataManager.generateNodes(secondPassword)
                            .andThen(ethWalletCompletable())
                            .subscribe(() -> {
                                //no-op
                            }, Throwable::printStackTrace));
        }
    }
}
